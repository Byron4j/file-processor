import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Tabs, Upload, Button, message, Form, Input, Select, InputNumber } from 'antd';
import { InboxOutlined, MergeCellsOutlined, SplitCellsOutlined, RotateLeftOutlined, DeleteOutlined, LockOutlined, UnlockOutlined } from '@ant-design/icons';
import { pdfApi } from '@api';

const { Dragger } = Upload;
const { TabPane } = Tabs;

export const PdfTools = () => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState('merge');
  const [loading, setLoading] = useState(false);
  const [fileIds, setFileIds] = useState<string[]>([]);

  const handleMerge = async () => {
    if (fileIds.length < 2) {
      message.error('Please upload at least 2 files');
      return;
    }

    setLoading(true);
    try {
      const result = await pdfApi.merge({ fileIds });
      message.success('Merged successfully');
      window.open(result.downloadUrl, '_blank');
    } catch (error) {
      message.error('Merge failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSplit = async (values: any) => {
    if (fileIds.length === 0) {
      message.error('Please upload a file');
      return;
    }

    setLoading(true);
    try {
      const result = await pdfApi.split({
        fileId: fileIds[0],
        mode: values.mode,
        ranges: values.ranges,
      });
      message.success('Split successfully');
      window.open(result.downloadUrl, '_blank');
    } catch (error) {
      message.error('Split failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pdf-tools-page">
      <Card title={t('PDF Tools')}>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab={t('Merge')} key="merge" icon={<MergeCellsOutlined />}>
            <Dragger
              name="files"
              multiple
              action="/api/files/upload"
              onChange={(info) => {
                const ids = info.fileList
                  .filter(f => f.status === 'done' && f.response?.data?.fileId)
                  .map(f => f.response.data.fileId);
                setFileIds(ids);
              }}
            >
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p>{t('Drop PDF files here to merge')}</p>
            </Dragger>
            <Button type="primary" onClick={handleMerge} loading={loading} block style={{ marginTop: 16 }}>
              {t('Merge PDFs')}
            </Button>
          </TabPane>

          <TabPane tab={t('Split')} key="split" icon={<SplitCellsOutlined />}>
            <Dragger name="file" action="/api/files/upload" onChange={(info) => {
              if (info.file.status === 'done') {
                setFileIds([info.file.response.data.fileId]);
              }
            }}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p>{t('Drop a PDF file to split')}</p>
            </Dragger>
            <Form onFinish={handleSplit} layout="vertical" style={{ marginTop: 16 }}>
              <Form.Item name="mode" label={t('Split Mode')} initialValue="ranges">
                <Select>
                  <Select.Option value="ranges">{t('By Page Ranges')}</Select.Option>
                  <Select.Option value="every">{t('Every N Pages')}</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item name="ranges" label={t('Page Ranges')}>
                <Input placeholder="e.g., 1-3, 5, 8-10" />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} block>
                {t('Split PDF')}
              </Button>
            </Form>
          </TabPane>

          <TabPane tab={t('Rotate')} key="rotate" icon={<RotateLeftOutlined />}>
            <p>{t('Rotate PDF pages')}</p>
          </TabPane>

          <TabPane tab={t('Delete Pages')} key="delete" icon={<DeleteOutlined />}>
            <p>{t('Delete specific pages from PDF')}</p>
          </TabPane>

          <TabPane tab={t('Encrypt')} key="encrypt" icon={<LockOutlined />}>
            <p>{t('Add password protection to PDF')}</p>
          </TabPane>

          <TabPane tab={t('Decrypt')} key="decrypt" icon={<UnlockOutlined />}>
            <p>{t('Remove password protection from PDF')}</p>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};
