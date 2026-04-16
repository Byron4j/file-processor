import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Select, Button, Upload, message, Form, Input, Radio } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { convertApi } from '@api';

const { Dragger } = Upload;
const { Option } = Select;

interface ConvertProps {
  type?: 'document' | 'image' | 'video' | 'audio';
}

const formatOptions = {
  document: [
    { value: 'docx', label: 'DOCX' },
    { value: 'pdf', label: 'PDF' },
    { value: 'txt', label: 'TXT' },
    { value: 'html', label: 'HTML' },
  ],
  image: [
    { value: 'jpeg', label: 'JPEG' },
    { value: 'png', label: 'PNG' },
    { value: 'gif', label: 'GIF' },
    { value: 'webp', label: 'WebP' },
    { value: 'tiff', label: 'TIFF' },
  ],
  video: [
    { value: 'mp4', label: 'MP4' },
    { value: 'webm', label: 'WebM' },
    { value: 'mov', label: 'MOV' },
    { value: 'avi', label: 'AVI' },
  ],
  audio: [
    { value: 'mp3', label: 'MP3' },
    { value: 'wav', label: 'WAV' },
    { value: 'ogg', label: 'OGG' },
    { value: 'flac', label: 'FLAC' },
  ],
};

export const Convert = ({ type = 'document' }: ConvertProps) => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileId, setFileId] = useState<string | null>(null);

  const handleConvert = async (values: any) => {
    if (!fileId) {
      message.error('Please upload a file first');
      return;
    }

    setLoading(true);
    try {
      let result;
      switch (type) {
        case 'document':
          result = await convertApi.convertDocument({
            fileId,
            targetFormat: values.targetFormat,
          });
          break;
        case 'image':
          result = await convertApi.convertImage({
            fileId,
            targetFormat: values.targetFormat,
            quality: values.quality,
          });
          break;
        case 'video':
          result = await convertApi.convertVideo({
            fileId,
            targetFormat: values.targetFormat,
            resolution: values.resolution,
          });
          break;
        case 'audio':
          result = await convertApi.convertAudio(fileId, values.targetFormat);
          break;
        default:
          throw new Error('Unsupported conversion type');
      }

      message.success('Conversion started successfully');
      // Download the converted file
      window.open(result.downloadUrl, '_blank');
    } catch (error) {
      message.error('Conversion failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="convert-page">
      <Card title={t(`${type.charAt(0).toUpperCase() + type.slice(1)} Conversion`)}>
        <Form form={form} layout="vertical" onFinish={handleConvert}>
          <Form.Item label={t('Upload File')} required>
            <Dragger
              name="file"
              action="/api/files/upload"
              onChange={(info) => {
                if (info.file.status === 'done') {
                  setFileId(info.file.response.data.fileId);
                  message.success(`${info.file.name} uploaded successfully`);
                }
              }}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">{t('Click or drag file to this area to upload')}</p>
            </Dragger>
          </Form.Item>

          <Form.Item
            name="targetFormat"
            label={t('Target Format')}
            rules={[{ required: true, message: 'Please select target format' }]}
          >
            <Select placeholder={t('Select format')}>
              {formatOptions[type].map((opt) => (
                <Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {type === 'image' && (
            <Form.Item name="quality" label={t('Quality')}>
              <Radio.Group>
                <Radio.Button value={0.9}>{t('High')}</Radio.Button>
                <Radio.Button value={0.7}>{t('Medium')}</Radio.Button>
                <Radio.Button value={0.5}>{t('Low')}</Radio.Button>
              </Radio.Group>
            </Form.Item>
          )}

          {type === 'video' && (
            <Form.Item name="resolution" label={t('Resolution')}>
              <Select placeholder={t('Select resolution')}>
                <Option value="1080p">1080p (Full HD)</Option>
                <Option value="720p">720p (HD)</Option>
                <Option value="480p">480p (SD)</Option>
              </Select>
            </Form.Item>
          )}

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              {t('Start Conversion')}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
