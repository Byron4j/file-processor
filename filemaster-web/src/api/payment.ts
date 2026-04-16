import client from './client';

export interface PaymentRequest {
  planId: number;
  billingCycle: 'MONTHLY' | 'YEARLY';
  paymentMethod: 'ALIPAY' | 'WECHAT';
}

export interface PaymentResponse {
  success: boolean;
  message: string;
  data?: {
    orderNo: string;
    amount: number;
    payUrl?: string;
    codeUrl?: string;
  };
}

export interface OrderStatus {
  success: boolean;
  data?: {
    orderNo: string;
    status: 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';
    amount: number;
    createdAt: string;
    paidAt?: string;
  };
}

export interface RefundRequest {
  orderNo: string;
  refundAmount: number;
  reason: string;
}

export interface RefundResponse {
  success: boolean;
  message: string;
  data?: {
    refundNo: string;
  };
}

export const paymentApi = {
  /**
   * 创建支付订单
   */
  createPayment: (request: PaymentRequest) =>
    client.post<PaymentResponse>('/api/payment/create', request),

  /**
   * 查询订单状态
   */
  getOrderStatus: (orderNo: string) =>
    client.get<OrderStatus>(`/api/payment/status/${orderNo}`),

  /**
   * 申请退款
   */
  applyRefund: (request: RefundRequest) =>
    client.post<RefundResponse>('/api/payment/refund', request),
};
