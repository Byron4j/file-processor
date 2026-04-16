import { useLocation, useNavigate } from 'react-router-dom';
import { Menu, Typography } from 'antd';
import {
  DashboardOutlined,
  FileOutlined,
  CloudUploadOutlined,
  HistoryOutlined,
  StarOutlined,
  DeleteOutlined,
  ToolOutlined,
  FilePdfOutlined,
  ScanOutlined,
  CopyrightOutlined,
  PartitionOutlined,
  RobotOutlined,
  FileTextOutlined,
  QuestionCircleOutlined,
  TagsOutlined,
  TagOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  CrownOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { MenuProps } from 'antd';

const { Title } = Typography;

type MenuItem = Required<MenuProps>['items'][number];

interface SidebarProps {
  collapsed: boolean;
}

export const Sidebar = ({ collapsed }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();

  const items: MenuItem[] = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: t('Dashboard'),
    },
    {
      key: 'files',
      icon: <FileOutlined />,
      label: t('Files'),
      children: [
        { key: '/files', icon: <FileOutlined />, label: t('All Files') },
        { key: '/files/recent', icon: <HistoryOutlined />, label: t('Recent') },
        { key: '/files/favorites', icon: <StarOutlined />, label: t('Favorites') },
        { key: '/files/trash', icon: <DeleteOutlined />, label: t('Trash') },
      ],
    },
    {
      key: 'convert',
      icon: <CloudUploadOutlined />,
      label: t('Conversion'),
      children: [
        { key: '/convert/document', icon: <FileTextOutlined />, label: t('Document') },
        { key: '/convert/image', icon: <FileOutlined />, label: t('Image') },
        { key: '/convert/video', icon: <FileOutlined />, label: t('Video') },
        { key: '/convert/audio', icon: <FileOutlined />, label: t('Audio') },
      ],
    },
    {
      key: 'tools',
      icon: <ToolOutlined />,
      label: t('Tools'),
      children: [
        { key: '/tools/pdf', icon: <FilePdfOutlined />, label: t('PDF Tools') },
        { key: '/tools/ocr', icon: <ScanOutlined />, label: t('OCR') },
        { key: '/tools/watermark', icon: <CopyrightOutlined />, label: t('Watermark') },
        { key: '/tools/split-merge', icon: <PartitionOutlined />, label: t('Split & Merge') },
      ],
    },
    {
      key: 'ai',
      icon: <RobotOutlined />,
      label: 'AI',
      children: [
        { key: '/ai/summary', icon: <FileTextOutlined />, label: t('Summary') },
        { key: '/ai/qa', icon: <QuestionCircleOutlined />, label: 'Q&A' },
        { key: '/ai/classification', icon: <TagsOutlined />, label: t('Classification') },
        { key: '/ai/tags', icon: <TagOutlined />, label: t('Smart Tags') },
      ],
    },
    {
      key: '/tasks',
      icon: <ScheduleOutlined />,
      label: t('Tasks'),
    },
    {
      key: '/subscription',
      icon: <CrownOutlined />,
      label: t('Subscription'),
    },
    {
      key: '/admin',
      icon: <TeamOutlined />,
      label: t('Admin'),
    },
  ];

  const handleClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  const selectedKeys = [location.pathname];
  const openKeys = location.pathname.split('/')[1] || [];

  return (
    <div className="sidebar">
      <div className="sidebar-logo">
        <img src="/logo.svg" alt="FileMaster" />
        {!collapsed && <Title level={5}>FileMaster Pro</Title>}
      </div>
      <Menu
        mode="inline"
        selectedKeys={selectedKeys}
        defaultOpenKeys={['files', 'convert', 'tools', 'ai', 'subscription']}
        items={items}
        onClick={handleClick}
      />
    </div>
  );
};
