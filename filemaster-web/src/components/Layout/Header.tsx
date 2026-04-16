import { useNavigate } from 'react-router-dom';
import {
  Layout,
  Button,
  Badge,
  Dropdown,
  Avatar,
  Tooltip,
  Space,
  Switch,
  Menu,
} from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  MoonOutlined,
  SunOutlined,
  GlobalOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore, useThemeStore } from '@stores';
import './index.less';

const { Header: AntHeader } = Layout;

interface HeaderProps {
  collapsed: boolean;
  onCollapse: (collapsed: boolean) => void;
}

export const Header = ({ collapsed, onCollapse }: HeaderProps) => {
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { user, logout } = useAuthStore();
  const { isDark, toggleTheme } = useThemeStore();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  const languageMenu = (
    <Menu
      items={[
        { key: 'zh-CN', label: '中文', onClick: () => changeLanguage('zh-CN') },
        { key: 'en-US', label: 'English', onClick: () => changeLanguage('en-US') },
        { key: 'ja-JP', label: '日本語', onClick: () => changeLanguage('ja-JP') },
        { key: 'ko-KR', label: '한국어', onClick: () => changeLanguage('ko-KR') },
      ]}
    />
  );

  const userMenu = (
    <Menu
      items={[
        { key: 'profile', icon: <UserOutlined />, label: t('Profile') },
        { key: 'settings', icon: <SettingOutlined />, label: t('Settings') },
        { type: 'divider' },
        { key: 'logout', icon: <LogoutOutlined />, label: t('Logout'), danger: true, onClick: handleLogout },
      ]}
    />
  );

  return (
    <AntHeader className="main-header">
      <div className="header-left">
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={() => onCollapse(!collapsed)}
        />
      </div>

      <div className="header-right">
        <Space size={16}>
          {/* Theme Toggle */}
          <Tooltip title={isDark ? t('Light Mode') : t('Dark Mode')}>
            <Switch
              checked={isDark}
              onChange={toggleTheme}
              checkedChildren={<MoonOutlined />}
              unCheckedChildren={<SunOutlined />}
            />
          </Tooltip>

          {/* Language Selector */}
          <Dropdown overlay={languageMenu} placement="bottomRight">
            <Button type="text" icon={<GlobalOutlined />}>
              {i18n.language.toUpperCase()}
            </Button>
          </Dropdown>

          {/* Notifications */}
          <Badge count={5} size="small">
            <Button type="text" icon={<BellOutlined />} />
          </Badge>

          {/* User Menu */}
          <Dropdown overlay={userMenu} placement="bottomRight">
            <Space className="user-info" style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} src={user?.avatar} />
              <span className="username">{user?.username}</span>
            </Space>
          </Dropdown>
        </Space>
      </div>
    </AntHeader>
  );
};
