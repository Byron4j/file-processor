import { useState } from 'react';
import { Button, Space, message } from 'antd';
import {
  ZoomInOutlined,
  ZoomOutOutlined,
  RotateLeftOutlined,
  RotateRightOutlined,
  DownloadOutlined,
  FullscreenOutlined,
} from '@ant-design/icons';
import { previewApi } from '@api';

interface ImagePreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const ImagePreview = ({ fileId, downloadUrl }: ImagePreviewProps) => {
  const [scale, setScale] = useState(1);
  const [rotation, setRotation] = useState(0);

  const imageUrl = previewApi.getImageUrl(fileId);

  return (
    <div className="image-preview">
      <div className="image-toolbar">
        <Space>
          <Button.Group>
            <Button icon={<ZoomOutOutlined />} onClick={() => setScale(s => Math.max(0.1, s - 0.1))} />
            <span style={{ padding: '0 8px' }}>{Math.round(scale * 100)}%</span>
            <Button icon={<ZoomInOutlined />} onClick={() => setScale(s => Math.min(5, s + 0.1))} />
          </Button.Group>

          <Button.Group>
            <Button icon={<RotateLeftOutlined />} onClick={() => setRotation(r => r - 90)} />
            <Button icon={<RotateRightOutlined />} onClick={() => setRotation(r => r + 90)} />
          </Button.Group>

          <Button icon={<FullscreenOutlined />} onClick={() => setScale(1)}>
            Fit
          </Button>

          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="image-container">
        <img
          src={imageUrl}
          alt="Preview"
          style={{
            transform: `scale(${scale}) rotate(${rotation}deg)`,
            maxWidth: '100%',
            maxHeight: '100%',
            transition: 'transform 0.2s',
          }}
        />
      </div>
    </div>
  );
};
