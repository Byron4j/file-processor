import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Table, Tag, Button, Progress, Space, Badge } from 'antd';
import { ReloadOutlined, PauseCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useTaskStore } from '@stores';
import { taskApi } from '@api';
import { Task, TaskStatus } from '@types';

interface TasksProps {
  view?: 'running' | 'history';
}

const statusColors: Record<TaskStatus, string> = {
  PENDING: 'default',
  PROCESSING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
};

const statusLabels: Record<TaskStatus, string> = {
  PENDING: 'Pending',
  PROCESSING: 'Processing',
  SUCCESS: 'Success',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled',
};

export const Tasks = ({ view = 'running' }: TasksProps) => {
  const { t } = useTranslation();
  const { tasks, loading, fetchTasks } = useTaskStore();
  const [polling, setPolling] = useState(false);

  useEffect(() => {
    fetchTasks({ status: view === 'running' ? 'PROCESSING' : undefined });

    // Poll for updates
    if (view === 'running') {
      const interval = setInterval(() => {
        fetchTasks({ status: 'PROCESSING' });
      }, 5000);
      return () => clearInterval(interval);
    }
  }, [view, fetchTasks]);

  const columns = [
    {
      title: t('Task ID'),
      dataIndex: 'id',
      key: 'id',
      ellipsis: true,
    },
    {
      title: t('Type'),
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: t('Status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: TaskStatus) => (
        <Tag color={statusColors[status]}>{t(statusLabels[status])}</Tag>
      ),
    },
    {
      title: t('Progress'),
      dataIndex: 'progress',
      key: 'progress',
      render: (progress: number, record: Task) => (
        record.status === 'PROCESSING' ? (
          <Progress percent={progress} size="small" />
        ) : (
          <Progress percent={progress} size="small" status={record.status === 'FAILED' ? 'exception' : 'success'} />
        )
      ),
    },
    {
      title: t('Created'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleString(),
    },
    {
      title: t('Actions'),
      key: 'actions',
      render: (_: any, record: Task) => (
        <Space>
          {record.status === 'PROCESSING' && (
            <Button
              type="text"
              danger
              icon={<CloseCircleOutlined />}
              onClick={() => taskApi.cancel(record.id)}
            >
              {t('Cancel')}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="tasks-page">
      <Card
        title={view === 'running' ? t('Running Tasks') : t('Task History')}
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchTasks()}
            loading={loading}
          >
            {t('Refresh')}
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={tasks}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};
