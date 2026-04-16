import { Button, Space } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { previewApi } from '@api';

interface AudioPreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const AudioPreview = ({ fileId, downloadUrl }: AudioPreviewProps) => {
  const audioUrl = previewApi.getAudioUrl(fileId);

  return (
    <div className="audio-preview">
      <div className="audio-toolbar">
        <Space>
          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="audio-container">
        <audio controls preload="auto" style={{ width: '100%' }}>
          <source src={audioUrl} />
          Your browser does not support the audio element.
        </audio>
      </div>
    </div>
  );
};
