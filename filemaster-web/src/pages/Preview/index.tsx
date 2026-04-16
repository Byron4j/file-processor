import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import { previewApi } from '@api';
import { PreviewInfo, PreviewType } from '@types';
import { PdfPreview } from './PdfPreview';
import { ImagePreview } from './ImagePreview';
import { VideoPreview } from './VideoPreview';
import { AudioPreview } from './AudioPreview';
import { OfficePreview } from './OfficePreview';
import { TextPreview } from './TextPreview';
import './index.less';

export const Preview = () => {
  const { fileId } = useParams<{ fileId: string }>();
  const [loading, setLoading] = useState(true);
  const [previewInfo, setPreviewInfo] = useState<PreviewInfo | null>(null);

  useEffect(() => {
    if (fileId) {
      loadPreview();
    }
  }, [fileId]);

  const loadPreview = async () => {
    try {
      setLoading(true);
      const info = await previewApi.getPreviewInfo(fileId!);
      setPreviewInfo(info);
    } catch (error) {
      message.error('Failed to load preview');
    } finally {
      setLoading(false);
    }
  };

  const renderPreview = () => {
    if (!previewInfo) return null;

    switch (previewInfo.type) {
      case 'pdf':
        return <PdfPreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      case 'image':
        return <ImagePreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      case 'video':
        return <VideoPreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      case 'audio':
        return <AudioPreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      case 'office':
        return <OfficePreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      case 'text':
        return <TextPreview fileId={fileId!} downloadUrl={previewInfo.downloadUrl} />;
      default:
        return (
          <div className="preview-unsupported">
            <p>Preview not supported for this file format</p>
            <a href={previewInfo.downloadUrl} download>
              Download File
            </a>
          </div>
        );
    }
  };

  if (loading) {
    return (
      <div className="preview-loading">
        <Spin size="large" tip="Loading preview..." />
      </div>
    );
  }

  return (
    <div className="preview-page">
      <div className="preview-container">{renderPreview()}</div>
    </div>
  );
};
