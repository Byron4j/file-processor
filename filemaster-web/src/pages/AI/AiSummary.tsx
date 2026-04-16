import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, Button, message, Form, Slider, Input, Spin, Select } from 'antd';
import { FileTextOutlined, InboxOutlined, RobotOutlined } from '@ant-design/icons';
import { aiApi } from '@api';

const { Dragger } = Upload;
const { TextArea } = Input;
const { Option } = Select;

export const AiSummary = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState('');
  const [fileId, setFileId] = useState<string | null>(null);

  const handleGenerateSummary = async (values: any) => {
    if (!fileId) {
      message.error('Please upload a document');
      return;
    }

    setLoading(true);
    try {
      const data = await aiApi.summarize({
        fileId,
        maxLength: values.maxLength,
        style: values.style,
      });
      setResult(data.answer);
      message.success('Summary generated');
    } catch (error) {
      message.error('Failed to generate summary');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-summary-page">
      <Card title={t('AI Smart Summary')}>
        <Dragger
          name="file"
          action="/api/files/upload"
          accept=".pdf,.doc,.docx,.txt"
          onChange={(info: any) => {
            if (info.file.status === 'done') {
              setFileId(info.file.response.data.fileId);
              message.success(`${info.file.name} uploaded successfully`);
            }
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p>{t('Drop a document here or click to upload')}</p>
          <p className="ant-upload-hint">{t('Supports PDF, DOC, DOCX, TXT formats')}</p>
        </Dragger>

        <Form layout="vertical" onFinish={handleGenerateSummary} style={{ marginTop: 16 }}>
          <Form.Item name="style" label={t('Summary Style')} initialValue="concise">
            <Select>
              <Option value="concise">{t('Concise')}</Option>
              <Option value="detailed">{t('Detailed')}</Option>
              <Option value="bullet-points">{t('Bullet Points')}</Option>
            </Select>
          </Form.Item>

          <Form.Item name="maxLength" label={t('Maximum Length (words)')} initialValue={300}>
            <Slider min={100} max={1000} step={50} marks={{ 100: '100', 500: '500', 1000: '1000' }} />
          </Form.Item>

          <Button type="primary" htmlType="submit" loading={loading} block icon={<RobotOutlined />}>
            {t('Generate Summary')}
          </Button>
        </Form>

        {result && (
          <div style={{ marginTop: 24 }}>
            <h4>{t('Summary Result')}</h4>
            <Card>
              <p>{result}</p>
            </Card>
          </div>
        )}
      </Card>
    </div>
  );
};
