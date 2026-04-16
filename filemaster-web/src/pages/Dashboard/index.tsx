import { useEffect } from 'react';
import { Row, Col, Card, Statistic, Typography, List, Avatar, Progress } from 'antd';
import {
  FileOutlined,
  CloudOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  RiseOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useFileStore, useAuthStore } from '@stores';
import { formatFileSize } from '@utils';
import './index.less';

const { Title, Text } = Typography;

export const Dashboard = () => {
  const { t } = useTranslation();
  const { user } = useAuthStore();
  const { files, fetchFiles } = useFileStore();

  useEffect(() => {
    fetchFiles({ size: 10, sortBy: 'updatedAt', sortOrder: 'desc' });
  }, [fetchFiles]);

  // Mock statistics - replace with real data
  const stats = [
    { title: t('Total Files'), value: 1234, icon: <FileOutlined />, color: '#1890ff' },
    { title: t('Storage Used'), value: '45.2 GB', icon: <CloudOutlined />, color: '#52c41a' },
    { title: t('Processing'), value: 3, icon: <ClockCircleOutlined />, color: '#faad14' },
    { title: t('Completed'), value: 567, icon: <CheckCircleOutlined />, color: '#722ed1' },
  ];

  // Recent files with mock data
  const recentFiles = files.slice(0, 5).map((file) => ({
    ...file,
    icon: getFileIcon(file.name),
  }));

  return (
    <div className="dashboard-page">
      <Title level={4}>{t('Welcome')}, {user?.username}</Title>
      <Text type="secondary">{t('Here is your file overview')}</Text>

      {/* Statistics Cards */}
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {stats.map((stat) => (
          <Col xs={24} sm={12} lg={6} key={stat.title}>
            <Card className="stat-card">
              <div className="stat-content" style={{ borderLeftColor: stat.color }}>
                <div className="stat-icon" style={{ backgroundColor: `${stat.color}20`, color: stat.color }}>
                  {stat.icon}
                </div>
                <div className="stat-info">
                  <Text type="secondary">{stat.title}</Text>
                  <Title level={3} style={{ margin: 0 }}>{stat.value}</Title>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Main Content */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {/* Recent Files */}
        <Col xs={24} lg={16}>
          <Card title={t('Recent Files')} extra={<a href="/files">{t('View All')}</a>}>
            <List
              dataSource={recentFiles}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Text type="secondary">{formatFileSize(item.size)}</Text>,
                    <Text type="secondary">{new Date(item.updatedAt).toLocaleDateString()}</Text>,
                  ]}
                >
                  <List.Item.Meta
                    avatar={<Avatar icon={item.icon} />}
                    title={<a href={`/preview/${item.id}`}>{item.name}</a>}
                    description={item.type}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

        {/* Storage Usage */}
        <Col xs={24} lg={8}>
          <Card title={t('Storage Usage')}>
            <Progress
              type="circle"
              percent={45}
              format={(percent) => (
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 24, fontWeight: 'bold' }}>{percent}%</div>
                  <div style={{ fontSize: 12, color: '#999' }}>{t('Used')}</div>
                </div>
              )}
            />
            <div className="storage-details" style={{ marginTop: 24 }}>
              <div className="storage-item">
                <FileImageOutlined style={{ color: '#52c41a' }} />
                <span>{t('Images')}</span>
                <span className="storage-size">15.2 GB</span>
              </div>
              <div className="storage-item">
                <FilePdfOutlined style={{ color: '#ff4d4f' }} />
                <span>{t('Documents')}</span>
                <span className="storage-size">12.8 GB</span>
              </div>
              <div className="storage-item">
                <VideoCameraOutlined style={{ color: '#722ed1' }} />
                <span>{t('Videos')}</span>
                <span className="storage-size">10.5 GB</span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// Helper function to get file icon
function getFileIcon(filename: string) {
  const ext = filename.split('.').pop()?.toLowerCase();
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext || '')) return <FileImageOutlined />;
  if (ext === 'pdf') return <FilePdfOutlined />;
  if (['doc', 'docx'].includes(ext || '')) return <FileWordOutlined />;
  return <FileOutlined />;
}
