import { FileOutlined, FileImageOutlined, FilePdfOutlined, FileWordOutlined, FileExcelOutlined, FilePptOutlined, VideoCameraOutlined, AudioOutlined, FileZipOutlined, FileTextOutlined } from '@ant-design/icons';
import { getFileIconClass, getFileType } from '@utils';

interface FileIconProps {
  filename: string;
  size?: number;
}

export const FileIcon = ({ filename, size = 24 }: FileIconProps) => {
  const type = getFileType(filename);
  const iconClass = getFileIconClass(filename);

  const iconStyle = { fontSize: size };

  switch (type) {
    case 'image':
      return <FileImageOutlined style={{ ...iconStyle, color: '#52c41a' }} />;
    case 'video':
      return <VideoCameraOutlined style={{ ...iconStyle, color: '#722ed1' }} />;
    case 'audio':
      return <AudioOutlined style={{ ...iconStyle, color: '#eb2f96' }} />;
    case 'document':
      if (filename.toLowerCase().endsWith('.pdf')) {
        return <FilePdfOutlined style={{ ...iconStyle, color: '#ff4d4f' }} />;
      }
      return <FileWordOutlined style={{ ...iconStyle, color: '#1890ff' }} />;
    case 'spreadsheet':
      return <FileExcelOutlined style={{ ...iconStyle, color: '#52c41a' }} />;
    case 'presentation':
      return <FilePptOutlined style={{ ...iconStyle, color: '#faad14' }} />;
    case 'archive':
      return <FileZipOutlined style={{ ...iconStyle, color: '#fa8c16' }} />;
    case 'code':
    case 'text':
      return <FileTextOutlined style={{ ...iconStyle, color: '#8c8c8c' }} />;
    default:
      return <FileOutlined style={iconStyle} />;
  }
};
