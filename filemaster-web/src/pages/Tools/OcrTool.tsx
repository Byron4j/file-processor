import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, Button, message, Spin, Input } from 'antd';
import { ScanOutlined, InboxOutlined } from '@ant-design/icons';
import { aiApi } from '@api';

const { Dragger } = Upload;
const { TextArea } = Input;

export const OcrTool = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string>('');
  const [fileId, setFileId] = useState<string | null>(null);

  const handleOcr = async () => {
    if (!fileId) {
      message.error('Please upload an image');
      return;
    }

    setLoading(true);
    try {
      // Using document classification API as placeholder for OCR
      const data = await aiApi.classify(fileId);
      setResult(JSON.stringify(data, null, 2));
      message.success('OCR completed');
    } catch (error) {
      message.error('OCR failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ocr-tool-page">
      <Card title={t('OCR Text Recognition')}>
        <Dragger
          name="file"
          action="/api/files/upload"
          accept="image/*"
          onChange={(info) => {
            if (info.file.status === 'done') {
              setFileId(info.file.response.data.fileId);
              message.success(`${info.file.name} uploaded successfully`);
            }
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p>{t('Drop an image here or click to upload')}</p>
          <p className="ant-upload-hint">{t('Supports JPG, PNG, TIFF formats')}</p>
        </Dragger>

        <Button
          type="primary"
          icon={<ScanOutlined />}
          onClick={handleOcr}
          loading={loading}
          block
          style={{ marginTop: 16 }}
        >
          {t('Start OCR')}
        </Button>

        {result && (
          <div style={{ marginTop: 24 }}>
            <h4>{t('Recognition Result')}</h4>
            <TextArea rows={10} value={result} readOnly />
          </div>
        )}
      </Card>
    </div>
  );
};
