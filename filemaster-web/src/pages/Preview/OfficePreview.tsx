import { useEffect, useState } from 'react';
import { Spin, Button, Space, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import * as mammoth from 'mammoth';
import { extractApi } from '@api';

interface OfficePreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const OfficePreview = ({ fileId, downloadUrl }: OfficePreviewProps) => {
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState<string>('');

  useEffect(() => {
    loadContent();
  }, [fileId]);

  const loadContent = async () => {
    try {
      setLoading(true);
      // For DOCX files, we can use mammoth to convert to HTML
      // For other formats, show a message
      const response = await fetch(downloadUrl);
      const blob = await response.blob();
      const arrayBuffer = await blob.arrayBuffer();

      const result = await mammoth.convertToHtml({ arrayBuffer });
      setContent(result.value);
    } catch (error) {
      message.error('Failed to load document preview');
      setContent('<p>Unable to preview this document. Please download to view.</p>');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="office-preview">
      <div className="office-toolbar">
        <Space>
          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="office-container">
        <Spin spinning={loading}>
          <div
            className="office-content"
            dangerouslySetInnerHTML={{ __html: content }}
            style={{
              padding: '40px',
              maxWidth: '800px',
              margin: '0 auto',
              background: '#fff',
              minHeight: '600px',
            }}
          />
        </Spin>
      </div>
    </div>
  );
};
