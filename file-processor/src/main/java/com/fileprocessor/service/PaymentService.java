package com.fileprocessor.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.PaymentOrder;
import com.fileprocessor.entity.RefundRecord;
import com.fileprocessor.entity.SubscriptionPlan;
import com.fileprocessor.entity.User;
import com.fileprocessor.entity.UserSubscription;
import com.fileprocessor.mapper.PaymentOrderMapper;
import com.fileprocessor.mapper.RefundRecordMapper;
import com.fileprocessor.mapper.SubscriptionPlanMapper;
import com.fileprocessor.mapper.UserMapper;
import com.fileprocessor.mapper.UserSubscriptionMapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.nativepay.model.SceneInfo;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务 - 支持支付宝和微信支付
 * 生产环境可用实现，包含完整的幂等性、签名验证等功能
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // 订单锁前缀
    private static final String ORDER_LOCK_PREFIX = "payment:order:lock:";
    // 回调幂等性前缀
    private static final String CALLBACK_IDEMPOTENT_PREFIX = "payment:callback:";
    // 订单锁过期时间（秒）
    private static final long ORDER_LOCK_EXPIRE = 30;
    // 回调幂等性过期时间（分钟）
    private static final long CALLBACK_IDEMPOTENT_EXPIRE = 10;
    // 订单超时时间（分钟）
    private static final long ORDER_TIMEOUT_MINUTES = 30;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private SubscriptionPlanMapper planMapper;

    @Autowired
    private UserSubscriptionMapper subscriptionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Alipay Config
    @Value("${payment.alipay.app-id:}")
    private String alipayAppId;

    @Value("${payment.alipay.private-key:}")
    private String alipayPrivateKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.alipay.server-url:https://openapi.alipay.com/gateway.do}")
    private String alipayServerUrl;

    @Value("${payment.alipay.notify-url:}")
    private String alipayNotifyUrl;

    @Value("${payment.alipay.return-url:}")
    private String alipayReturnUrl;

    // WeChat Pay Config
    @Value("${payment.wechat.mch-id:}")
    private String wechatMchId;

    @Value("${payment.wechat.app-id:}")
    private String wechatAppId;

    @Value("${payment.wechat.api-v3-key:}")
    private String wechatApiV3Key;

    @Value("${payment.wechat.mch-serial-no:}")
    private String wechatMchSerialNo;

    @Value("${payment.wechat.private-key-path:}")
    private String wechatPrivateKeyPath;

    @Value("${payment.wechat.notify-url:}")
    private String wechatNotifyUrl;

    // 微信支付配置（延迟初始化）
    private volatile Config wechatConfig;
    private volatile NativePayService wechatPayService;
    private volatile RefundService wechatRefundService;

    /**
     * 获取微信支付配置（线程安全）
     */
    private Config getWechatConfig() {
        if (wechatConfig == null) {
            synchronized (this) {
                if (wechatConfig == null) {
                    wechatConfig = new RSAAutoCertificateConfig.Builder()
                            .merchantId(wechatMchId)
                            .privateKeyFromPath(wechatPrivateKeyPath)
                            .merchantSerialNumber(wechatMchSerialNo)
                            .apiV3Key(wechatApiV3Key)
                            .build();
                }
            }
        }
        return wechatConfig;
    }

    /**
     * 获取微信支付服务（线程安全）
     */
    private NativePayService getWechatPayService() {
        if (wechatPayService == null) {
            synchronized (this) {
                if (wechatPayService == null) {
                    wechatPayService = new NativePayService.Builder()
                            .config(getWechatConfig())
                            .build();
                }
            }
        }
        return wechatPayService;
    }

    /**
     * 获取微信退款服务（线程安全）
     */
    private RefundService getWechatRefundService() {
        if (wechatRefundService == null) {
            synchronized (this) {
                if (wechatRefundService == null) {
                    wechatRefundService = new RefundService.Builder()
                            .config(getWechatConfig())
                            .build();
                }
            }
        }
        return wechatRefundService;
    }

    /**
     * 创建支付订单
     * 包含幂等性检查：同一用户同一套餐在短时间内只能创建一个待支付订单
     */
    @Transactional
    public FileResponse createPaymentOrder(Long userId, Long planId, String billingCycle, String paymentMethod) {
        // 参数校验
        if (userId == null || planId == null || billingCycle == null || paymentMethod == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("参数不能为空")
                    .build();
        }

        // 验证套餐
        SubscriptionPlan plan = planMapper.selectById(planId);
        if (plan == null || !Boolean.TRUE.equals(plan.getActive())) {
            log.warn("创建订单失败：套餐不存在或已下架，planId={}", planId);
            return FileResponse.builder()
                    .success(false)
                    .message("套餐不存在或已下架")
                    .build();
        }

        // 验证计费周期
        if (!"YEARLY".equals(billingCycle) && !"MONTHLY".equals(billingCycle)) {
            return FileResponse.builder()
                    .success(false)
                    .message("无效的计费周期")
                    .build();
        }

        // 验证支付方式
        if (!"ALIPAY".equals(paymentMethod) && !"WECHAT".equals(paymentMethod)) {
            return FileResponse.builder()
                    .success(false)
                    .message("不支持的支付方式")
                    .build();
        }

        // 检查是否已有待支付订单（幂等性）
        String idempotentKey = String.format("payment:create:%d:%d:%s:%s", userId, planId, billingCycle, paymentMethod);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("创建订单过于频繁，userId={}", userId);
            return FileResponse.builder()
                    .success(false)
                    .message("订单创建过于频繁，请稍后再试")
                    .build();
        }

        try {
            // 检查是否已有相同条件的待支付订单
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getUserId, userId)
                   .eq(PaymentOrder::getPlanId, planId)
                   .eq(PaymentOrder::getStatus, "PENDING")
                   .ge(PaymentOrder::getCreatedAt, LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES));

            PaymentOrder existingOrder = paymentOrderMapper.selectOne(wrapper);
            if (existingOrder != null) {
                log.info("存在未完成的订单，直接返回，orderNo={}", existingOrder.getOrderNo());
                return buildPaymentResponse(existingOrder, plan, paymentMethod);
            }

            // 创建新订单
            BigDecimal amount = "YEARLY".equals(billingCycle) ? plan.getYearlyPrice() : plan.getMonthlyPrice();
            String orderNo = generateOrderNo();

            PaymentOrder order = new PaymentOrder();
            order.setUserId(userId);
            order.setOrderNo(orderNo);
            order.setPlanId(planId);
            order.setOrderType("SUBSCRIPTION");
            order.setAmount(amount);
            order.setCurrency("CNY");
            order.setPaymentMethod(paymentMethod);
            order.setBillingCycle(billingCycle);
            order.setStatus("PENDING");
            order.setExpireAt(LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES));
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            paymentOrderMapper.insert(order);

            log.info("创建支付订单成功，orderNo={}, userId={}, planId={}, amount={}",
                    orderNo, userId, planId, amount);

            return buildPaymentResponse(order, plan, paymentMethod);

        } finally {
            redisTemplate.delete(idempotentKey);
        }
    }

    /**
     * 构建支付响应
     */
    private FileResponse buildPaymentResponse(PaymentOrder order, SubscriptionPlan plan, String paymentMethod) {
        if ("ALIPAY".equals(paymentMethod)) {
            return createAlipayPayment(order, plan.getName(), order.getBillingCycle());
        } else {
            return createWechatPayment(order, plan.getName());
        }
    }

    /**
     * 创建支付宝支付
     */
    private FileResponse createAlipayPayment(PaymentOrder order, String planName, String billingCycle) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayServerUrl, alipayAppId, alipayPrivateKey, "json", "UTF-8", alipayPublicKey, "RSA2");

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayNotifyUrl);
            request.setReturnUrl(alipayReturnUrl);

            String subject = planName + " - " + ("YEARLY".equals(billingCycle) ? "年付" : "月付");

            // 构建业务内容
            String content = String.format(
                "{" +
                    "\"out_trade_no\":\"%s\"," +
                    "\"total_amount\":\"%s\"," +
                    "\"subject\":\"%s\"," +
                    "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                    "\"timeout_express\":\"30m\"" +
                "}",
                order.getOrderNo(),
                order.getAmount().toString(),
                subject
            );

            request.setBizContent(content);
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

            if (response.isSuccess()) {
                log.info("支付宝订单创建成功，orderNo={}", order.getOrderNo());
                return FileResponse.builder()
                        .success(true)
                        .message("支付订单创建成功")
                        .data(Map.of(
                                "orderNo", order.getOrderNo(),
                                "amount", order.getAmount(),
                                "payUrl", response.getBody(),
                                "expireAt", order.getExpireAt()
                        ))
                        .build();
            } else {
                log.error("支付宝订单创建失败，orderNo={}, msg={}", order.getOrderNo(), response.getMsg());
                order.setStatus("FAILED");
                order.setErrorMessage("创建支付失败：" + response.getMsg());
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                return FileResponse.builder()
                        .success(false)
                        .message("创建支付订单失败：" + response.getMsg())
                        .build();
            }
        } catch (AlipayApiException e) {
            log.error("支付宝接口异常，orderNo={}", order.getOrderNo(), e);
            order.setStatus("FAILED");
            order.setErrorMessage("支付宝接口异常：" + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            paymentOrderMapper.updateById(order);

            return FileResponse.builder()
                    .success(false)
                    .message("支付系统异常，请稍后重试")
                    .build();
        }
    }

    /**
     * 创建微信支付
     */
    private FileResponse createWechatPayment(PaymentOrder order, String planName) {
        try {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(wechatAppId);
            request.setMchid(wechatMchId);
            request.setDescription(planName.length() > 32 ? planName.substring(0, 32) : planName);
            request.setOutTradeNo(order.getOrderNo());
            request.setNotifyUrl(wechatNotifyUrl);
            request.setTimeExpire(order.getExpireAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")));

            // 设置订单金额（转换为分）
            Amount amount = new Amount();
            amount.setTotal(order.getAmount().multiply(new BigDecimal(100)).intValue());
            request.setAmount(amount);

            // 设置场景信息
            SceneInfo sceneInfo = new SceneInfo();
            sceneInfo.setPayerClientIp("127.0.0.1");
            request.setSceneInfo(sceneInfo);

            PrepayResponse response = getWechatPayService().prepay(request);

            log.info("微信支付订单创建成功，orderNo={}", order.getOrderNo());
            return FileResponse.builder()
                    .success(true)
                    .message("支付订单创建成功")
                    .data(Map.of(
                            "orderNo", order.getOrderNo(),
                            "amount", order.getAmount(),
                            "codeUrl", response.getCodeUrl(),
                            "expireAt", order.getExpireAt()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("微信支付订单创建失败，orderNo={}", order.getOrderNo(), e);
            order.setStatus("FAILED");
            order.setErrorMessage("创建支付失败：" + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            paymentOrderMapper.updateById(order);

            return FileResponse.builder()
                    .success(false)
                    .message("创建支付订单失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 处理支付宝回调
     * 完整的幂等性处理和事务控制
     */
    @Transactional(rollbackFor = Exception.class)
    public FileResponse handleAlipayCallback(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String tradeNo = params.get("trade_no");
        String callbackId = params.get("notify_id");

        if (orderNo == null || tradeStatus == null) {
            log.error("支付宝回调参数缺失");
            return FileResponse.builder()
                    .success(false)
                    .message("回调参数缺失")
                    .build();
        }

        // 幂等性检查
        String idempotentKey = CALLBACK_IDEMPOTENT_PREFIX + "alipay:" + (callbackId != null ? callbackId : orderNo);
        Boolean processed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", CALLBACK_IDEMPOTENT_EXPIRE, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(processed)) {
            log.warn("支付宝回调重复通知，orderNo={}", orderNo);
            return FileResponse.builder()
                    .success(true)
                    .message("回调已处理")
                    .build();
        }

        // 获取订单锁
        String lockKey = ORDER_LOCK_PREFIX + orderNo;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ORDER_LOCK_EXPIRE, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("获取订单锁失败，orderNo={}", orderNo);
            return FileResponse.builder()
                    .success(false)
                    .message("系统繁忙，请稍后")
                    .build();
        }

        try {
            PaymentOrder order = paymentOrderMapper.selectOne(
                    new LambdaQueryWrapper<PaymentOrder>()
                            .eq(PaymentOrder::getOrderNo, orderNo)
            );

            if (order == null) {
                log.error("支付宝回调：订单不存在，orderNo={}", orderNo);
                return FileResponse.builder()
                        .success(false)
                        .message("订单不存在")
                        .build();
            }

            // 如果订单已处理，直接返回成功
            if ("PAID".equals(order.getStatus())) {
                log.info("支付宝回调：订单已支付，orderNo={}", orderNo);
                return FileResponse.builder()
                        .success(true)
                        .message("回调处理成功")
                        .build();
            }

            // 如果订单已关闭或失败，记录日志但不处理
            if ("CLOSED".equals(order.getStatus()) || "FAILED".equals(order.getStatus())) {
                log.warn("支付宝回调：订单已关闭或失败，orderNo={}", orderNo);
                return FileResponse.builder()
                        .success(true)
                        .message("订单已关闭")
                        .build();
            }

            // 处理支付成功状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 验证金额是否一致
                String totalAmount = params.get("total_amount");
                if (totalAmount != null) {
                    BigDecimal callbackAmount = new BigDecimal(totalAmount);
                    if (callbackAmount.compareTo(order.getAmount()) != 0) {
                        log.error("支付宝回调：金额不匹配，orderNo={}, expected={}, actual={}",
                                orderNo, order.getAmount(), callbackAmount);
                        return FileResponse.builder()
                                .success(false)
                                .message("金额不匹配")
                                .build();
                    }
                }

                // 更新订单状态
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setProviderTransactionId(tradeNo);
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                // 激活订阅
                activateSubscription(order);

                log.info("支付宝支付成功处理完成，orderNo={}", orderNo);
            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                order.setStatus("CLOSED");
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
                log.info("支付宝订单关闭，orderNo={}", orderNo);
            }

            return FileResponse.builder()
                    .success(true)
                    .message("回调处理成功")
                    .build();

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 处理微信支付回调
     * 完整的签名验证、解密和幂等性处理
     */
    @Transactional(rollbackFor = Exception.class)
    public FileResponse handleWechatCallback(String serialNumber, String nonce, String timestamp,
                                              String signature, String body) {
        try {
            log.info("处理微信支付回调");

            // 使用通知解析器解析并验证回调
            NotificationParser parser = new NotificationParser(getWechatNotificationConfig());

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber)
                    .nonce(nonce)
                    .timestamp(timestamp)
                    .signType("WECHATPAY2-SHA256-RSA2048")
                    .signature(signature)
                    .body(body)
                    .build();

            com.wechat.pay.java.service.payments.model.Transaction transaction =
                    parser.parse(requestParam, com.wechat.pay.java.service.payments.model.Transaction.class);

            String orderNo = transaction.getOutTradeNo();
            String tradeState = transaction.getTradeState() != null ? transaction.getTradeState().name() : null;
            String transactionId = transaction.getTransactionId();

            if (orderNo == null || tradeState == null) {
                log.error("微信支付回调参数缺失");
                return FileResponse.builder()
                        .success(false)
                        .message("回调参数缺失")
                        .build();
            }

            // 幂等性检查
            String idempotentKey = CALLBACK_IDEMPOTENT_PREFIX + "wechat:" + transactionId;
            Boolean processed = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", CALLBACK_IDEMPOTENT_EXPIRE, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(processed)) {
                log.warn("微信支付回调重复通知，orderNo={}", orderNo);
                return FileResponse.builder()
                        .success(true)
                        .message("回调已处理")
                        .build();
            }

            // 获取订单锁
            String lockKey = ORDER_LOCK_PREFIX + orderNo;
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ORDER_LOCK_EXPIRE, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(locked)) {
                log.warn("获取订单锁失败，orderNo={}", orderNo);
                redisTemplate.delete(idempotentKey);
                return FileResponse.builder()
                        .success(false)
                        .message("系统繁忙，请稍后")
                        .build();
            }

            try {
                PaymentOrder order = paymentOrderMapper.selectOne(
                        new LambdaQueryWrapper<PaymentOrder>()
                                .eq(PaymentOrder::getOrderNo, orderNo)
                );

                if (order == null) {
                    log.error("微信支付回调：订单不存在，orderNo={}", orderNo);
                    return FileResponse.builder()
                            .success(false)
                            .message("订单不存在")
                            .build();
                }

                // 如果订单已处理，直接返回成功
                if ("PAID".equals(order.getStatus())) {
                    log.info("微信支付回调：订单已支付，orderNo={}", orderNo);
                    return FileResponse.builder()
                            .success(true)
                            .message("回调处理成功")
                            .build();
                }

                // 处理支付成功
                if ("SUCCESS".equals(tradeState)) {
                    // 验证金额
                    if (transaction.getAmount() != null && transaction.getAmount().getTotal() != null) {
                        int callbackAmount = transaction.getAmount().getTotal();
                        int expectedAmount = order.getAmount().multiply(new BigDecimal(100)).intValue();
                        if (callbackAmount != expectedAmount) {
                            log.error("微信支付回调：金额不匹配，orderNo={}, expected={}, actual={}",
                                    orderNo, expectedAmount, callbackAmount);
                            return FileResponse.builder()
                                    .success(false)
                                    .message("金额不匹配")
                                    .build();
                        }
                    }

                    // 更新订单状态
                    order.setStatus("PAID");
                    order.setPaidAt(LocalDateTime.now());
                    order.setProviderTransactionId(transactionId);
                    order.setUpdatedAt(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);

                    // 激活订阅
                    activateSubscription(order);

                    log.info("微信支付成功处理完成，orderNo={}", orderNo);
                } else if ("CLOSED".equals(tradeState) || "REVOKED".equals(tradeState)) {
                    order.setStatus("CLOSED");
                    order.setUpdatedAt(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                    log.info("微信订单关闭，orderNo={}", orderNo);
                }

                return FileResponse.builder()
                        .success(true)
                        .message("回调处理成功")
                        .build();

            } finally {
                redisTemplate.delete(lockKey);
            }

        } catch (Exception e) {
            log.error("微信支付回调处理异常", e);
            return FileResponse.builder()
                    .success(false)
                    .message("回调处理失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取微信支付通知配置
     */
    private NotificationConfig getWechatNotificationConfig() {
        return (NotificationConfig) getWechatConfig();
    }

    /**
     * 查询订单状态
     */
    public FileResponse queryOrderStatus(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return FileResponse.builder()
                    .success(false)
                    .message("订单号不能为空")
                    .build();
        }

        PaymentOrder order = paymentOrderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderNo, orderNo)
        );

        if (order == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("订单不存在")
                    .build();
        }

        // 如果订单超时，标记为已关闭
        if ("PENDING".equals(order.getStatus()) && order.getExpireAt() != null
                && order.getExpireAt().isBefore(LocalDateTime.now())) {
            order.setStatus("CLOSED");
            order.setUpdatedAt(LocalDateTime.now());
            paymentOrderMapper.updateById(order);
            log.info("订单已超时关闭，orderNo={}", orderNo);
        }

        // 如果是待支付状态，主动查询第三方支付状态
        if ("PENDING".equals(order.getStatus())) {
            if ("WECHAT".equals(order.getPaymentMethod())) {
                return queryWechatOrderStatus(order);
            } else if ("ALIPAY".equals(order.getPaymentMethod())) {
                return queryAlipayOrderStatus(order);
            }
        }

        return buildOrderStatusResponse(order);
    }

    /**
     * 查询微信支付订单状态
     */
    private FileResponse queryWechatOrderStatus(PaymentOrder order) {
        try {
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(wechatMchId);
            request.setOutTradeNo(order.getOrderNo());

            com.wechat.pay.java.service.payments.model.Transaction transaction =
                    getWechatPayService().queryOrderByOutTradeNo(request);

            String tradeState = transaction.getTradeState() != null ? transaction.getTradeState().name() : null;

            // 如果支付成功但订单状态未更新，则更新订单
            if ("SUCCESS".equals(tradeState) && !"PAID".equals(order.getStatus())) {
                String lockKey = ORDER_LOCK_PREFIX + order.getOrderNo();
                Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ORDER_LOCK_EXPIRE, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(locked)) {
                    try {
                        order.setStatus("PAID");
                        order.setPaidAt(LocalDateTime.now());
                        order.setProviderTransactionId(transaction.getTransactionId());
                        order.setUpdatedAt(LocalDateTime.now());
                        paymentOrderMapper.updateById(order);

                        activateSubscription(order);

                        log.info("微信订单状态更新为已支付，orderNo={}", order.getOrderNo());
                    } finally {
                        redisTemplate.delete(lockKey);
                    }
                }
            } else if (("CLOSED".equals(tradeState) || "REVOKED".equals(tradeState)) && "PENDING".equals(order.getStatus())) {
                order.setStatus("CLOSED");
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
            }

            return FileResponse.builder()
                    .success(true)
                    .message("订单状态查询成功")
                    .data(Map.of(
                            "orderNo", order.getOrderNo(),
                            "status", order.getStatus(),
                            "tradeState", tradeState,
                            "amount", order.getAmount(),
                            "createdAt", order.getCreatedAt(),
                            "paidAt", order.getPaidAt(),
                            "transactionId", order.getProviderTransactionId()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("查询微信订单状态失败，orderNo={}", order.getOrderNo(), e);
            return buildOrderStatusResponse(order);
        }
    }

    /**
     * 查询支付宝订单状态
     */
    private FileResponse queryAlipayOrderStatus(PaymentOrder order) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayServerUrl, alipayAppId, alipayPrivateKey, "json", "UTF-8", alipayPublicKey, "RSA2");

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", order.getOrderNo()));

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();

                // 如果支付成功但订单状态未更新，则更新订单
                if (("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))
                        && !"PAID".equals(order.getStatus())) {
                    String lockKey = ORDER_LOCK_PREFIX + order.getOrderNo();
                    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ORDER_LOCK_EXPIRE, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(locked)) {
                        try {
                            order.setStatus("PAID");
                            order.setPaidAt(LocalDateTime.now());
                            order.setProviderTransactionId(response.getTradeNo());
                            order.setUpdatedAt(LocalDateTime.now());
                            paymentOrderMapper.updateById(order);

                            activateSubscription(order);

                            log.info("支付宝订单状态更新为已支付，orderNo={}", order.getOrderNo());
                        } finally {
                            redisTemplate.delete(lockKey);
                        }
                    }
                } else if ("TRADE_CLOSED".equals(tradeStatus) && "PENDING".equals(order.getStatus())) {
                    order.setStatus("CLOSED");
                    order.setUpdatedAt(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                }

                return FileResponse.builder()
                        .success(true)
                        .message("订单状态查询成功")
                        .data(Map.of(
                                "orderNo", order.getOrderNo(),
                                "status", order.getStatus(),
                                "tradeStatus", tradeStatus,
                                "amount", order.getAmount(),
                                "createdAt", order.getCreatedAt(),
                                "paidAt", order.getPaidAt(),
                                "transactionId", order.getProviderTransactionId()
                        ))
                        .build();
            } else {
                log.warn("查询支付宝订单失败，orderNo={}, msg={}", order.getOrderNo(), response.getMsg());
                return buildOrderStatusResponse(order);
            }

        } catch (AlipayApiException e) {
            log.error("查询支付宝订单状态失败，orderNo={}", order.getOrderNo(), e);
            return buildOrderStatusResponse(order);
        }
    }

    /**
     * 构建订单状态响应
     */
    private FileResponse buildOrderStatusResponse(PaymentOrder order) {
        return FileResponse.builder()
                .success(true)
                .message("订单状态查询成功")
                .data(Map.of(
                        "orderNo", order.getOrderNo(),
                        "status", order.getStatus(),
                        "amount", order.getAmount(),
                        "paymentMethod", order.getPaymentMethod(),
                        "createdAt", order.getCreatedAt(),
                        "paidAt", order.getPaidAt(),
                        "expireAt", order.getExpireAt(),
                        "transactionId", order.getProviderTransactionId() != null ? order.getProviderTransactionId() : ""
                ))
                .build();
    }

    /**
     * 申请退款
     */
    @Transactional(rollbackFor = Exception.class)
    public FileResponse applyRefund(Long userId, String orderNo, BigDecimal refundAmount, String reason) {
        try {
            PaymentOrder order = paymentOrderMapper.selectOne(
                    new LambdaQueryWrapper<PaymentOrder>()
                            .eq(PaymentOrder::getOrderNo, orderNo)
                            .eq(PaymentOrder::getUserId, userId)
            );

            if (order == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("订单不存在")
                        .build();
            }

            if (!"PAID".equals(order.getStatus())) {
                return FileResponse.builder()
                        .success(false)
                        .message("订单未支付，无法退款")
                        .build();
            }

            // 检查是否已全额退款
            if ("REFUNDED".equals(order.getStatus())) {
                return FileResponse.builder()
                        .success(false)
                        .message("订单已全额退款")
                        .build();
            }

            // 验证退款金额
            if (refundAmount.compareTo(order.getAmount()) > 0) {
                return FileResponse.builder()
                        .success(false)
                        .message("退款金额不能超过订单金额")
                        .build();
            }

            // 生成退款单号
            String refundNo = generateOrderNo();

            // 创建退款记录
            RefundRecord refundRecord = new RefundRecord();
            refundRecord.setOrderNo(orderNo);
            refundRecord.setRefundNo(refundNo);
            refundRecord.setUserId(userId);
            refundRecord.setRefundAmount(refundAmount);
            refundRecord.setReason(reason);
            refundRecord.setStatus("PROCESSING");
            refundRecord.setCreatedAt(LocalDateTime.now());
            refundRecord.setUpdatedAt(LocalDateTime.now());
            refundRecordMapper.insert(refundRecord);

            // 执行退款
            FileResponse refundResponse;
            if ("WECHAT".equals(order.getPaymentMethod())) {
                refundResponse = wechatRefund(order, refundNo, refundAmount, reason);
            } else {
                refundResponse = alipayRefund(order, refundNo, refundAmount, reason);
            }

            if (refundResponse.isSuccess()) {
                refundRecord.setStatus("SUCCESS");
                refundRecord.setUpdatedAt(LocalDateTime.now());
                refundRecordMapper.updateById(refundRecord);

                // 更新订单退款状态
                if (refundAmount.compareTo(order.getAmount()) == 0) {
                    order.setStatus("REFUNDED");
                } else {
                    order.setStatus("PARTIALLY_REFUNDED");
                }
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                // 停用订阅
                deactivateSubscription(order);

                log.info("退款成功，orderNo={}, refundNo={}, amount={}", orderNo, refundNo, refundAmount);

                return FileResponse.builder()
                        .success(true)
                        .message("退款申请成功")
                        .data(Map.of("refundNo", refundNo))
                        .build();
            } else {
                refundRecord.setStatus("FAILED");
                refundRecord.setErrorMessage(refundResponse.getMessage());
                refundRecord.setUpdatedAt(LocalDateTime.now());
                refundRecordMapper.updateById(refundRecord);

                return refundResponse;
            }

        } catch (Exception e) {
            log.error("申请退款失败，orderNo={}", orderNo, e);
            return FileResponse.builder()
                    .success(false)
                    .message("退款申请失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 微信退款 - 完整实现
     * 使用微信支付v3 API实现退款功能
     */
    private FileResponse wechatRefund(PaymentOrder order, String refundNo, BigDecimal refundAmount, String reason) {
        try {
            // 创建退款请求
            CreateRequest refundRequest = new CreateRequest();
            refundRequest.setOutTradeNo(order.getOrderNo());
            refundRequest.setOutRefundNo(refundNo);
            refundRequest.setReason(reason != null ? reason : "用户申请退款");

            // 设置退款金额（转换为分）
            AmountReq amountReq = new AmountReq();
            amountReq.setRefund(refundAmount.multiply(new BigDecimal(100)).longValue());
            amountReq.setTotal(order.getAmount().multiply(new BigDecimal(100)).longValue());
            amountReq.setCurrency("CNY");
            refundRequest.setAmount(amountReq);

            // 调用微信退款API
            Refund refundResponse = getWechatRefundService().create(refundRequest);

            // 检查退款结果
            if (refundResponse != null && refundResponse.getRefundId() != null) {
                log.info("微信退款申请成功，orderNo={}, refundNo={}, refundId={}",
                    order.getOrderNo(), refundNo, refundResponse.getRefundId());

                // 查询退款状态确保成功
                String refundStatus = refundResponse.getStatus() != null ? refundResponse.getStatus().name() : null;

                if ("SUCCESS".equals(refundStatus)) {
                    return FileResponse.builder()
                            .success(true)
                            .message("退款成功")
                            .data(Map.of(
                                "refundId", refundResponse.getRefundId(),
                                "refundNo", refundNo,
                                "status", "SUCCESS"
                            ))
                            .build();
                } else if ("PROCESSING".equals(refundStatus)) {
                    // 退款处理中，返回成功，后续可通过查询接口获取最终状态
                    return FileResponse.builder()
                            .success(true)
                            .message("退款申请已受理，处理中")
                            .data(Map.of(
                                "refundId", refundResponse.getRefundId(),
                                "refundNo", refundNo,
                                "status", "PROCESSING"
                            ))
                            .build();
                } else {
                    return FileResponse.builder()
                            .success(false)
                            .message("退款失败，状态：" + refundStatus)
                            .build();
                }
            } else {
                log.error("微信退款返回异常，orderNo={}, refundNo={}", order.getOrderNo(), refundNo);
                return FileResponse.builder()
                        .success(false)
                        .message("退款申请失败：接口返回异常")
                        .build();
            }

        } catch (com.wechat.pay.java.core.exception.ServiceException e) {
            // 处理微信支付业务异常
            log.error("微信退款业务异常，orderNo={}, errorCode={}, errorMessage={}",
                order.getOrderNo(), e.getErrorCode(), e.getErrorMessage(), e);
            return FileResponse.builder()
                    .success(false)
                    .message("微信退款失败：" + e.getErrorMessage())
                    .build();
        } catch (Exception e) {
            log.error("微信退款失败，orderNo={}", order.getOrderNo(), e);
            return FileResponse.builder()
                    .success(false)
                    .message("微信退款失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 支付宝退款
     */
    private FileResponse alipayRefund(PaymentOrder order, String refundNo, BigDecimal refundAmount, String reason) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayServerUrl, alipayAppId, alipayPrivateKey, "json", "UTF-8", alipayPublicKey, "RSA2");

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String content = String.format(
                "{" +
                    "\"out_trade_no\":\"%s\"," +
                    "\"refund_amount\":\"%s\"," +
                    "\"out_request_no\":\"%s\"," +
                    "\"refund_reason\":\"%s\"" +
                "}",
                order.getOrderNo(),
                refundAmount.toString(),
                refundNo,
                reason
            );
            request.setBizContent(content);

            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess() && "Y".equals(response.getFundChange())) {
                return FileResponse.builder()
                        .success(true)
                        .message("退款成功")
                        .build();
            } else {
                return FileResponse.builder()
                        .success(false)
                        .message("退款失败：" + response.getMsg())
                        .build();
            }

        } catch (AlipayApiException e) {
            log.error("支付宝退款失败，orderNo={}", order.getOrderNo(), e);
            return FileResponse.builder()
                    .success(false)
                    .message("支付宝退款失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 激活订阅
     */
    @Transactional
    public void activateSubscription(PaymentOrder order) {
        try {
            // 幂等性检查：检查是否已激活
            LambdaQueryWrapper<UserSubscription> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(UserSubscription::getOrderId, order.getId());
            if (subscriptionMapper.selectCount(checkWrapper) > 0) {
                log.info("订阅已激活，跳过，orderId={}", order.getId());
                return;
            }

            SubscriptionPlan plan = planMapper.selectById(order.getPlanId());
            if (plan == null) {
                log.error("激活订阅失败：套餐不存在，orderId={}", order.getId());
                return;
            }

            // 计算订阅周期
            int months = "YEARLY".equals(order.getBillingCycle()) ? 12 : 1;
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusMonths(months);

            UserSubscription subscription = new UserSubscription();
            subscription.setUserId(order.getUserId());
            subscription.setPlanId(plan.getId());
            subscription.setOrderId(order.getId());
            subscription.setStatus("ACTIVE");
            subscription.setCurrentPeriodStart(startDate);
            subscription.setCurrentPeriodEnd(endDate);
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionMapper.insert(subscription);

            // 更新用户套餐
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                user.setPlanId(plan.getId());
                user.setPlanExpireTime(endDate.atStartOfDay());
                userMapper.updateById(user);
            }

            // 更新订单关联订阅
            order.setSubscriptionId(subscription.getId());
            paymentOrderMapper.updateById(order);

            log.info("订阅激活成功，orderId={}, subscriptionId={}", order.getId(), subscription.getId());

        } catch (Exception e) {
            log.error("激活订阅失败，orderId={}", order.getId(), e);
            throw new RuntimeException("激活订阅失败", e);
        }
    }

    /**
     * 停用订阅（退款后）
     */
    @Transactional
    public void deactivateSubscription(PaymentOrder order) {
        try {
            if (order.getSubscriptionId() == null) {
                return;
            }

            UserSubscription subscription = subscriptionMapper.selectById(order.getSubscriptionId());
            if (subscription != null) {
                subscription.setStatus("CANCELLED");
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionMapper.updateById(subscription);

                // 更新用户为免费版
                User user = userMapper.selectById(order.getUserId());
                if (user != null) {
                    user.setPlanId(1L); // 假设1是免费版
                    user.setPlanExpireTime(null);
                    userMapper.updateById(user);
                }

                log.info("订阅已停用，orderId={}", order.getId());
            }
        } catch (Exception e) {
            log.error("停用订阅失败，orderId={}", order.getId(), e);
        }
    }

    /**
     * 关闭超时订单（定时任务调用）
     */
    @Transactional
    public void closeExpiredOrders() {
        try {
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getStatus, "PENDING")
                   .lt(PaymentOrder::getExpireAt, LocalDateTime.now());

            PaymentOrder orderToClose = new PaymentOrder();
            orderToClose.setStatus("CLOSED");
            orderToClose.setUpdatedAt(LocalDateTime.now());

            int count = paymentOrderMapper.update(orderToClose, wrapper);
            if (count > 0) {
                log.info("关闭超时订单数量：{}", count);
            }
        } catch (Exception e) {
            log.error("关闭超时订单失败", e);
        }
    }

    /**
     * 生成唯一订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "FM" + timestamp + uuid;
    }
}
