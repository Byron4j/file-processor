import { useTranslation } from 'react-i18next';
import { Card, Tabs, Table, Button, Tag, Statistic, Row, Col } from 'antd';
import {
  UserOutlined,
  DashboardOutlined,
  FileOutlined,
  SettingOutlined,
  BarChartOutlined,
} from '@ant-design/icons';

const { TabPane } = Tabs;

export const Admin = () => {
  const { t } = useTranslation();

  const userColumns = [
    { title: 'Username', dataIndex: 'username' },
    { title: 'Email', dataIndex: 'email' },
    { title: 'Role', dataIndex: 'role', render: (role: string) => <Tag color="blue">{role}</Tag> },
    { title: 'Status', dataIndex: 'status', render: (status: string) => <Tag color="green">{status}</Tag> },
    { title: 'Actions', render: () => <Button type="link">Edit</Button> },
  ];

  const auditColumns = [
    { title: 'Time', dataIndex: 'time' },
    { title: 'User', dataIndex: 'user' },
    { title: 'Action', dataIndex: 'action' },
    { title: 'IP', dataIndex: 'ip' },
  ];

  return (
    <div className="admin-page">
      <Card title={t('Admin Dashboard')}>
        <Tabs defaultActiveKey="dashboard">
          <TabPane tab={t('Dashboard')} key="dashboard" icon={<DashboardOutlined />}>
            <Row gutter={16}>
              <Col span={6}>
                <Card><Statistic title="Total Users" value={123} /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Total Files" value={4567} /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Storage Used" value="1.2 TB" /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Active Tasks" value={12} /></Card>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab={t('Users')} key="users" icon={<UserOutlined />}>
            <Table columns={userColumns} dataSource={[]} />
          </TabPane>

          <TabPane tab={t('Files')} key="files" icon={<FileOutlined />}>
            <p>File management coming soon...</p>
          </TabPane>

          <TabPane tab={t('Audit Logs')} key="audit" icon={<BarChartOutlined />}>
            <Table columns={auditColumns} dataSource={[]} />
          </TabPane>

          <TabPane tab={t('Settings')} key="settings" icon={<SettingOutlined />}>
            <p>System settings coming soon...</p>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};
