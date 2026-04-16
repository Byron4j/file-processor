import { useEffect, useState } from 'react';
import { Spin, Button, Space, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { previewApi } from '@api';
import './preview.less';

interface TextPreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const TextPreview = ({ fileId, downloadUrl }: TextPreviewProps) => {
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState<string>('');

  useEffect(() => {
    loadContent();
  }, [fileId]);

  const loadContent = async () => {
    try {
      setLoading(true);
      const data = await previewApi.getPreviewContent(fileId);
      setContent(data.content);
    } catch (error) {
      message.error('Failed to load content');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="text-preview">
      <div className="text-toolbar">
        <Space>
          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="text-container">
        <Spin spinning={loading}>
          <pre className="text-content">{content}</pre>
        </Spin>
      </div>
    </div>
  );
};
