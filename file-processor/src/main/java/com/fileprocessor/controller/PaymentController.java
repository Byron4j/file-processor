package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器 - 支持支付宝和微信支付
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    public FileResponse createPayment(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PaymentRequest request) {
        log.info("创建支付订单，userId={}, planId={}, billingCycle={}, paymentMethod={}",
                userId, request.getPlanId(), request.getBillingCycle(), request.getPaymentMethod());
        return paymentService.createPaymentOrder(
                userId,
                request.getPlanId(),
                request.getBillingCycle(),
                request.getPaymentMethod()
        );
    }

    /**
     * 支付宝回调通知
     * 支付宝以POST表单形式发送通知
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        log.info("接收到支付宝回调通知");
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });

        log.debug("支付宝回调参数: {}", params);

        FileResponse response = paymentService.handleAlipayCallback(params);
        // 支付宝要求返回字符串 "success" 或 "fail"
        return response.isSuccess() ? "success" : "fail";
    }

    /**
     * 微信支付回调通知
     * 微信支付v3以JSON格式发送通知，需要从请求头中获取签名信息
     */
    @PostMapping("/wechat/notify")
    public String wechatNotify(HttpServletRequest request) {
        log.info("接收到微信支付回调通知");

        // 从请求头获取签名信息
        String serialNumber = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");

        if (serialNumber == null || nonce == null || timestamp == null || signature == null) {
            log.error("微信支付回调缺少必要的签名头信息");
            return buildWechatResponse(false);
        }

        // 读取请求体
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } catch (Exception e) {
            log.error("读取微信支付回调请求体失败", e);
            return buildWechatResponse(false);
        }

        log.debug("微信支付回调请求体: {}", body);

        FileResponse response = paymentService.handleWechatCallback(
                serialNumber, nonce, timestamp, signature, body.toString()
        );

        return buildWechatResponse(response.isSuccess());
    }

    /**
     * 构建微信支付回调响应
     */
    private String buildWechatResponse(boolean success) {
        if (success) {
            return "{\"code\": \"SUCCESS\", \"message\": \"OK\"}";
        } else {
            return "{\"code\": \"FAIL\", \"message\": \"处理失败\"}";
        }
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/status/{orderNo}")
    public FileResponse queryOrderStatus(@PathVariable String orderNo) {
        log.info("查询订单状态，orderNo={}", orderNo);
        return paymentService.queryOrderStatus(orderNo);
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public FileResponse applyRefund(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody RefundRequest request) {
        log.info("申请退款，userId={}, orderNo={}, amount={}", userId, request.getOrderNo(), request.getRefundAmount());
        return paymentService.applyRefund(
                userId,
                request.getOrderNo(),
                request.getRefundAmount(),
                request.getReason()
        );
    }

    /**
     * 支付请求DTO
     */
    public static class PaymentRequest {
        @NotNull(message = "套餐ID不能为空")
        private Long planId;

        @NotBlank(message = "计费周期不能为空")
        private String billingCycle;

        @NotBlank(message = "支付方式不能为空")
        private String paymentMethod;

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        public String getBillingCycle() { return billingCycle; }
        public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    /**
     * 退款请求DTO
     */
    public static class RefundRequest {
        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        @NotNull(message = "退款金额不能为空")
        @Positive(message = "退款金额必须大于0")
        private BigDecimal refundAmount;

        @NotBlank(message = "退款原因不能为空")
        private String reason;

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
