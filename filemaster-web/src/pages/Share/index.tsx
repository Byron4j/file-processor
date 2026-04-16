import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card, Button, Input, Form, message, Spin, Descriptions, Tag } from 'antd';
import { DownloadOutlined, LockOutlined, FileOutlined } from '@ant-design/icons';
import { fileApi } from '@api';
import { ShareInfo } from '@types';
import { formatFileSize, formatDate } from '@utils';

export const Share = () => {
  const { t } = useTranslation();
  const { shareId } = useParams<{ shareId: string }>();
  const [loading, setLoading] = useState(true);
  const [share, setShare] = useState<ShareInfo | null>(null);
  const [passwordRequired, setPasswordRequired] = useState(false);
  const [verified, setVerified] = useState(false);

  useEffect(() => {
    if (shareId) {
      loadShare();
    }
  }, [shareId]);

  const loadShare = async () => {
    try {
      setLoading(true);
      const data = await fileApi.getShare(shareId!);
      setShare(data);
      if (data.hasPassword && !verified) {
        setPasswordRequired(true);
      }
    } catch (error) {
      message.error('Failed to load share');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyPassword = async (values: { password: string }) => {
    try {
      await fileApi.verifySharePassword(shareId!, values.password);
      setVerified(true);
      setPasswordRequired(false);
      loadShare();
    } catch (error) {
      message.error('Invalid password');
    }
  };

  const handleDownload = async () => {
    if (!share) return;
    try {
      await fileApi.download(share.fileId, share.fileName);
    } catch (error) {
      message.error('Download failed');
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (passwordRequired) {
    return (
      <div style={{ maxWidth: 400, margin: '100px auto', padding: 24 }}>
        <Card title={t('Password Protected')}>
          <Form onFinish={handleVerifyPassword}>
            <Form.Item
              name="password"
              rules={[{ required: true, message: 'Please enter password' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder={t('Enter password')} />
            </Form.Item>
            <Button type="primary" htmlType="submit" block>
              {t('Unlock')}
            </Button>
          </Form>
        </Card>
      </div>
    );
  }

  if (!share) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <h2>{t('Share not found or expired')}</h2>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: '50px auto', padding: 24 }}>
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <FileOutlined style={{ fontSize: 24 }} />
            <span>{share.fileName}</span>
          </div>
        }
      >
        <Descriptions column={1} bordered>
          <Descriptions.Item label={t('Created')}>
            {formatDate(share.createdAt)}
          </Descriptions.Item>
          {share.expiresAt && (
            <Descriptions.Item label={t('Expires')}>
              {formatDate(share.expiresAt)}
            </Descriptions.Item>
          )}
          <Descriptions.Item label={t('Downloads')}>
            {share.downloadCount} {share.maxDownloads && `/ ${share.maxDownloads}`}
          </Descriptions.Item>
        </Descriptions>

        {share.allowDownload && (
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleDownload}
            block
            size="large"
            style={{ marginTop: 24 }}
          >
            {t('Download File')}
          </Button>
        )}
      </Card>
    </div>
  );
};
