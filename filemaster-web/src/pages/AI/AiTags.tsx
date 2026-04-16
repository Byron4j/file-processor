import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, Button, message, Tag, Spin } from 'antd';
import { TagOutlined, InboxOutlined } from '@ant-design/icons';
import { aiApi } from '@api';

const { Dragger } = Upload;

export const AiTags = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [tags, setTags] = useState<string[]>([]);

  const handleGenerateTags = async (fileId: string) => {
    setLoading(true);
    try {
      const data = await aiApi.generateTags({ fileId, count: 10 });
      // Parse the response to extract tags
      const extractedTags = data.answer.split(/[,\n]/).map((t: string) => t.trim()).filter(Boolean);
      setTags(extractedTags);
      message.success('Tags generated');
    } catch (error) {
      message.error('Failed to generate tags');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-tags-page">
      <Card title={t('AI Smart Tags')}>
        <Dragger
          name="file"
          action="/api/files/upload"
          accept=".pdf,.doc,.docx,.txt"
          onChange={(info: any) => {
            if (info.file.status === 'done') {
              handleGenerateTags(info.file.response.data.fileId);
            }
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p>{t('Upload a document to generate smart tags')}</p>
        </Dragger>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <Spin tip={t('Generating tags...')} />
          </div>
        ) : tags.length > 0 ? (
          <div style={{ marginTop: 24 }}>
            <h4>{t('Generated Tags')}</h4>
            <div>
              {tags.map((tag) => (
                <Tag key={tag} color="blue" style={{ margin: 4 }}>
                  {tag}
                </Tag>
              ))}
            </div>
          </div>
        ) : null}
      </Card>
    </div>
  );
};
