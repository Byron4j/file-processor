package com.fileprocessor.task;

import com.fileprocessor.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付相关定时任务
 */
@Component
public class PaymentScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(PaymentScheduledTask.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * 关闭超时订单
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void closeExpiredOrders() {
        log.debug("开始执行关闭超时订单任务");
        try {
            paymentService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("关闭超时订单任务执行失败", e);
        }
    }
}
