import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Upload, Button, message, Form, Input, List, Avatar } from 'antd';
import { QuestionCircleOutlined, InboxOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons';
import { aiApi } from '@api';

const { Dragger } = Upload;
const { TextArea } = Input;

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
}

export const AiQa = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [fileId, setFileId] = useState<string | null>(null);

  const handleAsk = async (values: any) => {
    if (!fileId) {
      message.error('Please upload a document');
      return;
    }

    const userMessage: ChatMessage = { role: 'user', content: values.question };
    setMessages(prev => [...prev, userMessage]);

    setLoading(true);
    try {
      const data = await aiApi.ask({
        fileId,
        question: values.question,
      });
      const aiMessage: ChatMessage = { role: 'ai', content: data.answer };
      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      message.error('Failed to get answer');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-qa-page">
      <Card title={t('Document Q&A')}>
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
          <p>{t('Upload a document to ask questions')}</p>
        </Dragger>

        <div style={{ marginTop: 24, maxHeight: 400, overflow: 'auto' }}>
          <List
            dataSource={messages}
            renderItem={(item) => (
              <List.Item style={{ justifyContent: item.role === 'user' ? 'flex-end' : 'flex-start' }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 8,
                  flexDirection: item.role === 'user' ? 'row-reverse' : 'row'
                }}>
                  <Avatar icon={item.role === 'user' ? <UserOutlined /> : <RobotOutlined />} />
                  <div style={{
                    background: item.role === 'user' ? '#1890ff' : '#f0f0f0',
                    color: item.role === 'user' ? '#fff' : '#000',
                    padding: '8px 12px',
                    borderRadius: 8,
                    maxWidth: 400
                  }}>
                    {item.content}
                  </div>
                </div>
              </List.Item>
            )}
          />
        </div>

        <Form layout="inline" onFinish={handleAsk} style={{ marginTop: 16 }}>
          <Form.Item name="question" style={{ flex: 1 }} rules={[{ required: true }]}>
            <Input placeholder={t('Ask a question about the document...')} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} icon={<QuestionCircleOutlined />}>
              {t('Ask')}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
