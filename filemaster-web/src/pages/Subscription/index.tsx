import React, { useEffect, useState } from 'react';
import { Card, Button, Radio, message, QRCode, Space, Typography, Tag, List, Skeleton } from 'antd';
import { CheckCircleOutlined, AlipayCircleOutlined, WechatOutlined } from '@ant-design/icons';
import { subscriptionApi } from '../../api/subscription';
import { paymentApi } from '../../api/payment';
import './index.less';

const { Title, Text } = Typography;

interface Plan {
  id: number;
  name: string;
  description: string;
  monthlyPrice: number;
  yearlyPrice: number;
  storageQuota: number;
  features: string;
}

const SubscriptionPage: React.FC = () => {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<number | null>(null);
  const [billingCycle, setBillingCycle] = useState<'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [paymentMethod, setPaymentMethod] = useState<'ALIPAY' | 'WECHAT'>('WECHAT');
  const [payUrl, setPayUrl] = useState<string>('');
  const [codeUrl, setCodeUrl] = useState<string>('');
  const [orderNo, setOrderNo] = useState<string>('');
  const [showPayment, setShowPayment] = useState(false);
  const [polling, setPolling] = useState(false);

  useEffect(() => {
    fetchPlans();
  }, []);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (polling && orderNo) {
      interval = setInterval(() => {
        checkPaymentStatus();
      }, 3000);
    }
    return () => clearInterval(interval);
  }, [polling, orderNo]);

  const fetchPlans = async () => {
    setLoading(true);
    try {
      const response = await subscriptionApi.getAllPlans();
      if (response.data.success) {
        setPlans(response.data.data);
      }
    } catch (error) {
      message.error('获取套餐列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreatePayment = async () => {
    if (!selectedPlan) {
      message.warning('请先选择套餐');
      return;
    }

    try {
      const response = await paymentApi.createPayment({
        planId: selectedPlan,
        billingCycle,
        paymentMethod,
      });

      if (response.data.success) {
        setOrderNo(response.data.data.orderNo);
        if (paymentMethod === 'ALIPAY') {
          setPayUrl(response.data.data.payUrl || '');
          window.open(response.data.data.payUrl, '_blank');
        } else {
          setCodeUrl(response.data.data.codeUrl || '');
          setShowPayment(true);
          setPolling(true);
        }
        message.success('订单创建成功，请完成支付');
      } else {
        message.error(response.data.message);
      }
    } catch (error) {
      message.error('创建订单失败');
    }
  };

  const checkPaymentStatus = async () => {
    try {
      const response = await paymentApi.getOrderStatus(orderNo);
      if (response.data.success && response.data.data.status === 'PAID') {
        setPolling(false);
        message.success('支付成功！');
        setShowPayment(false);
        setCodeUrl('');
      }
    } catch (error) {
      console.error('查询支付状态失败', error);
    }
  };

  const formatStorage = (bytes: number) => {
    if (bytes >= 1024 * 1024 * 1024) {
      return `${(bytes / 1024 / 1024 / 1024).toFixed(0)}GB`;
    }
    return `${(bytes / 1024 / 1024).toFixed(0)}MB`;
  };

  const getFeaturesList = (features: string) => {
    return features.split(',').map(f => f.trim());
  };

  if (loading) {
    return <Skeleton active />;
  }

  return (
    <div className="subscription-page">
      <Title level={2}>订阅套餐</Title>
      <Text type="secondary">选择适合您的套餐，解锁更多功能</Text>

      <div className="billing-cycle">
        <Radio.Group
          value={billingCycle}
          onChange={(e) => setBillingCycle(e.target.value)}
          buttonStyle="solid"
        >
          <Radio.Button value="MONTHLY">月付</Radio.Button>
          <Radio.Button value="YEARLY">年付 (省20%)</Radio.Button>
        </Radio.Group>
      </div>

      <div className="plans-container">
        {plans.map((plan) => (
          <Card
            key={plan.id}
            className={`plan-card ${selectedPlan === plan.id ? 'selected' : ''}`}
            onClick={() => setSelectedPlan(plan.id)}
            hoverable
          >
            <div className="plan-header">
              <Title level={4}>{plan.name}</Title>
              <Text type="secondary">{plan.description}</Text>
            </div>

            <div className="plan-price">
              <span className="currency">¥</span>
              <span className="amount">
                {billingCycle === 'YEARLY' ? plan.yearlyPrice : plan.monthlyPrice}
              </span>
              <span className="period">/{billingCycle === 'YEARLY' ? '年' : '月'}</span>
            </div>

            {billingCycle === 'YEARLY' && (
              <Tag color="green">
                省 ¥{(plan.monthlyPrice * 12 - plan.yearlyPrice).toFixed(0)}
              </Tag>
            )}

            <div className="plan-storage">
              <Text>存储空间: {formatStorage(plan.storageQuota)}</Text>
            </div>

            <List
              size="small"
              dataSource={getFeaturesList(plan.features)}
              renderItem={(item) => (
                <List.Item>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                  {item}
                </List.Item>
              )}
            />

            {selectedPlan === plan.id && (
              <div className="selected-badge">已选择</div>
            )}
          </Card>
        ))}
      </div>

      {selectedPlan && (
        <Card className="payment-section" title="支付方式">
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Radio.Group
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
            >
              <Radio.Button value="ALIPAY">
                <AlipayCircleOutlined /> 支付宝
              </Radio.Button>
              <Radio.Button value="WECHAT">
                <WechatOutlined /> 微信支付
              </Radio.Button>
            </Radio.Group>

            <Button
              type="primary"
              size="large"
              onClick={handleCreatePayment}
              block
            >
              立即支付
            </Button>
          </Space>
        </Card>
      )}

      {showPayment && codeUrl && (
        <Card className="qrcode-section" title="微信扫码支付">
          <div className="qrcode-container">
            <QRCode value={codeUrl} size={200} />
            <Text type="secondary">请使用微信扫一扫完成支付</Text>
          </div>
        </Card>
      )}
    </div>
  );
};

export default SubscriptionPage;
