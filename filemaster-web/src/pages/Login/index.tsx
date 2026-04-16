import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, Checkbox, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@stores';
import './index.less';

const { Title, Text } = Typography;

export const Login = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { login, isLoading } = useAuthStore();
  const [form] = Form.useForm();

  const handleSubmit = async (values: { username: string; password: string; remember?: boolean }) => {
    try {
      await login({
        username: values.username,
        password: values.password,
        remember: values.remember,
      });
      message.success(t('common.success'));
      navigate('/');
    } catch (error) {
      message.error(error instanceof Error ? error.message : t('common.error'));
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <Card className="login-card">
          <div className="login-header">
            <img src="/logo.svg" alt="FileMaster Pro" className="login-logo" />
            <Title level={3}>{t('app.name')}</Title>
            <Text type="secondary">{t('auth.loginSubtitle')}</Text>
          </div>

          <Form
            form={form}
            name="login"
            onFinish={handleSubmit}
            autoComplete="off"
            layout="vertical"
            size="large"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: t('auth.username') }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder={t('auth.username')}
                autoFocus
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: t('auth.password') }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder={t('auth.password')}
              />
            </Form.Item>

            <Form.Item>
              <div className="login-options">
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>{t('auth.rememberMe')}</Checkbox>
                </Form.Item>
                <a href="/forgot-password">{t('auth.forgotPassword')}</a>
              </div>
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={isLoading}
                block
              >
                {t('auth.login')}
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    </div>
  );
};
