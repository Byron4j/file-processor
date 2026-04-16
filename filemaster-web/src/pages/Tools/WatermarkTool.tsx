import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, Button, message, Form, Input, Select, Slider } from 'antd';
import { InboxOutlined, CopyrightOutlined } from '@ant-design/icons';
import { pdfApi } from '@api';

const { Dragger } = Upload;
const { Option } = Select;

export const WatermarkTool = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [fileId, setFileId] = useState<string | null>(null);

  const handleAddWatermark = async (values: any) => {
    if (!fileId) {
      message.error('Please upload a file');
      return;
    }

    setLoading(true);
    try {
      const result = await pdfApi.addTextWatermark({
        fileId,
        text: values.text,
        opacity: values.opacity / 100,
        position: values.position,
      });
      message.success('Watermark added successfully');
      window.open(result.downloadUrl, '_blank');
    } catch (error) {
      message.error('Failed to add watermark');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="watermark-tool-page">
      <Card title={t('Watermark Tool')}>
        <Dragger
          name="file"
          action="/api/files/upload"
          accept=".pdf,.doc,.docx"
          onChange={(info) => {
            if (info.file.status === 'done') {
              setFileId(info.file.response.data.fileId);
              message.success(`${info.file.name} uploaded successfully`);
            }
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p>{t('Drop a file here or click to upload')}</p>
        </Dragger>

        <Form layout="vertical" onFinish={handleAddWatermark} style={{ marginTop: 16 }}>
          <Form.Item name="text" label={t('Watermark Text')} rules={[{ required: true }]}>
            <Input placeholder={t('Enter watermark text')} />
          </Form.Item>

          <Form.Item name="position" label={t('Position')} initialValue="center">
            <Select>
              <Option value="center">{t('Center')}</Option>
              <Option value="top-left">{t('Top Left')}</Option>
              <Option value="top-right">{t('Top Right')}</Option>
              <Option value="bottom-left">{t('Bottom Left')}</Option>
              <Option value="bottom-right">{t('Bottom Right')}</Option>
              <Option value="tile">{t('Tile')}</Option>
            </Select>
          </Form.Item>

          <Form.Item name="opacity" label={t('Opacity')} initialValue={50}>
            <Slider min={0} max={100} marks={{ 0: '0%', 50: '50%', 100: '100%' }} />
          </Form.Item>

          <Button type="primary" htmlType="submit" loading={loading} block icon={<CopyrightOutlined />}>
            {t('Add Watermark')}
          </Button>
        </Form>
      </Card>
    </div>
  );
};
