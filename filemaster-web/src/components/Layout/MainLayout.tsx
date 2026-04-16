import { useState } from 'react';
import { Layout } from 'antd';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import './index.less';

const { Content, Sider } = Layout;

export const MainLayout = () => {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout className="main-layout">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="light"
        width={240}
      >
        <Sidebar collapsed={collapsed} />
      </Sider>
      <Layout>
        <Header collapsed={collapsed} onCollapse={setCollapsed} />
        <Content>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};
