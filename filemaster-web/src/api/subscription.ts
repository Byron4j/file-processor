import client from './client';

export interface SubscriptionPlan {
  id: number;
  name: string;
  code: string;
  description: string;
  monthlyPrice: number;
  yearlyPrice: number;
  storageQuota: number;
  features: string;
  active: boolean;
}

export interface UserSubscription {
  id: number;
  userId: number;
  planId: number;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  currentPeriodStart: string;
  currentPeriodEnd: string;
}

export const subscriptionApi = {
  /**
   * 获取所有套餐
   */
  getAllPlans: () =>
    client.get<{ success: boolean; data: SubscriptionPlan[] }>('/api/subscription/plans'),

  /**
   * 获取套餐详情
   */
  getPlanDetail: (planId: number) =>
    client.get<{ success: boolean; data: SubscriptionPlan }>(`/api/subscription/plans/${planId}`),

  /**
   * 获取当前用户订阅
   */
  getMySubscription: () =>
    client.get<{ success: boolean; data: { subscription: UserSubscription; plan: SubscriptionPlan } }>('/api/subscription/my'),

  /**
   * 获取订阅历史
   */
  getSubscriptionHistory: () =>
    client.get<{ success: boolean; data: UserSubscription[] }>('/api/subscription/history'),
};
