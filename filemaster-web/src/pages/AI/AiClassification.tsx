import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, message, Tag, Descriptions } from 'antd';
import { TagsOutlined, InboxOutlined } from '@ant-design/icons';
import { aiApi } from '@api';

const { Dragger } = Upload;

export const AiClassification = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<any>(null);

  const handleClassify = async (fileId: string) => {
    setLoading(true);
    try {
      const data = await aiApi.classify(fileId);
      setResult(data);
      message.success('Classification completed');
    } catch (error) {
      message.error('Classification failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-classification-page">
      <Card title={t('Document Classification')}>
        <Dragger
          name="file"
          action="/api/files/upload"
          accept=".pdf,.doc,.docx,.txt"
          onChange={(info: any) => {
            if (info.file.status === 'done') {
              handleClassify(info.file.response.data.fileId);
            }
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p>{t('Drop a document here to classify')}</p>
        </Dragger>

        {result && (
          <Descriptions title={t('Classification Result')} bordered style={{ marginTop: 24 }}>
            <Descriptions.Item label={t('Document Type')} span={3}>
              <Tag color="blue">{result.documentType}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label={t('Confidence')} span={3}>
              {(result.confidence * 100).toFixed(1)}%
            </Descriptions.Item>
            <Descriptions.Item label={t('Keywords')} span={3}>
              {result.keywords.map((kw: string) => (
                <Tag key={kw}>{kw}</Tag>
              ))}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>
    </div>
  );
};
