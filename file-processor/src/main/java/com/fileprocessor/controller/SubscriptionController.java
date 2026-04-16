package com.fileprocessor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.SubscriptionPlan;
import com.fileprocessor.entity.User;
import com.fileprocessor.entity.UserSubscription;
import com.fileprocessor.mapper.SubscriptionPlanMapper;
import com.fileprocessor.mapper.UserMapper;
import com.fileprocessor.mapper.UserSubscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionPlanMapper planMapper;

    @Autowired
    private UserSubscriptionMapper subscriptionMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取所有可用套餐
     */
    @GetMapping("/plans")
    public FileResponse getAllPlans() {
        LambdaQueryWrapper<SubscriptionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlan::getActive, true);
        List<SubscriptionPlan> plans = planMapper.selectList(wrapper);

        return FileResponse.builder()
                .success(true)
                .message("Plans retrieved successfully")
                .data(plans)
                .build();
    }

    /**
     * 获取套餐详情
     */
    @GetMapping("/plans/{planId}")
    public FileResponse getPlanDetail(@PathVariable Long planId) {
        SubscriptionPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getActive()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Plan not found")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Plan retrieved successfully")
                .data(plan)
                .build();
    }

    /**
     * 获取用户当前订阅
     */
    @GetMapping("/my")
    public FileResponse getMySubscription(@RequestAttribute("userId") Long userId) {
        LambdaQueryWrapper<UserSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSubscription::getUserId, userId)
               .eq(UserSubscription::getStatus, "ACTIVE")
               .orderByDesc(UserSubscription::getCreatedAt)
               .last("LIMIT 1");

        UserSubscription subscription = subscriptionMapper.selectOne(wrapper);

        if (subscription == null) {
            // 返回免费套餐信息
            User user = userMapper.selectById(userId);
            return FileResponse.builder()
                    .success(true)
                    .message("No active subscription, using free plan")
                    .data(user)
                    .build();
        }

        // 获取套餐详情
        SubscriptionPlan plan = planMapper.selectById(subscription.getPlanId());

        return FileResponse.builder()
                .success(true)
                .message("Subscription retrieved successfully")
                .data(java.util.Map.of(
                        "subscription", subscription,
                        "plan", plan
                ))
                .build();
    }

    /**
     * 获取用户订阅历史
     */
    @GetMapping("/history")
    public FileResponse getSubscriptionHistory(@RequestAttribute("userId") Long userId) {
        LambdaQueryWrapper<UserSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSubscription::getUserId, userId)
               .orderByDesc(UserSubscription::getCreatedAt);

        List<UserSubscription> subscriptions = subscriptionMapper.selectList(wrapper);

        return FileResponse.builder()
                .success(true)
                .message("Subscription history retrieved successfully")
                .data(subscriptions)
                .build();
    }
}
