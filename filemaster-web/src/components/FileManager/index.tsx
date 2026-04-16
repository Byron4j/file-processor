import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table,
  Button,
  Space,
  Input,
  Dropdown,
  Modal,
  message,
  Empty,
  Spin,
  Upload,
  Breadcrumb,
  Tooltip,
  Checkbox,
} from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import {
  UploadOutlined,
  FolderAddOutlined,
  SearchOutlined,
  UnorderedListOutlined,
  AppstoreOutlined,
  MoreOutlined,
  EditOutlined,
  CopyOutlined,
  ScissorOutlined,
  StarOutlined,
  StarFilled,
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useFileStore, useUploadStore } from '@stores';
import { fileApi } from '@api';
import { FileItem } from '@types';
import { formatFileSize, formatDate, getFileIconClass, canPreview } from '@utils';
import { FileIcon } from '../common/Icon';
import './index.less';

interface FileManagerProps {
  title?: string;
  filter?: 'all' | 'recent' | 'favorites' | 'trash';
  folderId?: string | null;
}

export const FileManager = ({ title, filter = 'all' }: FileManagerProps) => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const {
    files,
    folders,
    currentFolder,
    selectedFiles,
    viewMode,
    loading,
    pagination,
    breadcrumbs,
    fetchFiles,
    fetchFolders,
    setCurrentFolder,
    selectFile,
    selectAll,
    setViewMode,
    deleteFile,
    renameFile,
    toggleFavorite,
    createFolder,
  } = useFileStore();

  const { addUpload } = useUploadStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [isCreateFolderModalOpen, setIsCreateFolderModalOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [isRenameModalOpen, setIsRenameModalOpen] = useState(false);
  const [renameValue, setRenameValue] = useState('');
  const [renameFileId, setRenameFileId] = useState<string | null>(null);

  useEffect(() => {
    const params: any = { page: pagination.current - 1, size: pagination.pageSize };

    if (filter === 'favorites') {
      params.isFavorite = true;
    } else if (filter === 'trash') {
      params.status = 'deleted';
    } else if (filter === 'recent') {
      params.sortBy = 'updatedAt';
      params.sortOrder = 'desc';
    }

    if (searchQuery) {
      params.keyword = searchQuery;
    }

    fetchFiles(params);
  }, [filter, searchQuery, pagination.current, pagination.pageSize, currentFolder]);

  const handleUpload = (info: any) => {
    if (info.file.status === 'done') {
      message.success(`${info.file.name} uploaded successfully`);
      fetchFiles({ folderId: currentFolder });
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} upload failed`);
    }
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    try {
      await createFolder(newFolderName, currentFolder);
      message.success('Folder created successfully');
      setNewFolderName('');
      setIsCreateFolderModalOpen(false);
    } catch (error) {
      message.error('Failed to create folder');
    }
  };

  const handleRename = async () => {
    if (!renameValue.trim() || !renameFileId) return;

    try {
      await renameFile(renameFileId, renameValue);
      message.success('Renamed successfully');
      setRenameValue('');
      setRenameFileId(null);
      setIsRenameModalOpen(false);
    } catch (error) {
      message.error('Failed to rename');
    }
  };

  const handleDelete = async (fileId: string, permanent = false) => {
    Modal.confirm({
      title: permanent ? 'Permanently Delete?' : 'Move to Trash?',
      content: permanent
        ? 'This action cannot be undone.'
        : 'The file will be moved to trash.',
      okText: permanent ? 'Delete' : 'Move to Trash',
      okType: 'danger',
      onOk: async () => {
        try {
          await deleteFile(fileId, permanent);
          message.success(permanent ? 'File deleted' : 'Moved to trash');
        } catch (error) {
          message.error('Failed to delete');
        }
      },
    });
  };

  const handleDownload = async (file: FileItem) => {
    try {
      await fileApi.download(file.id, file.name);
    } catch (error) {
      message.error('Download failed');
    }
  };

  const openRenameModal = (file: FileItem) => {
    setRenameFileId(file.id);
    setRenameValue(file.name);
    setIsRenameModalOpen(true);
  };

  const columns: TableColumnsType<FileItem> = [
    {
      title: (
        <Checkbox
          checked={selectedFiles.length === files.length && files.length > 0}
          indeterminate={selectedFiles.length > 0 && selectedFiles.length < files.length}
          onChange={(e) => selectAll(e.target.checked)}
        />
      ),
      width: 50,
      render: (_, record) => (
        <Checkbox
          checked={selectedFiles.includes(record.id)}
          onChange={() => selectFile(record.id)}
        />
      ),
    },
    {
      title: t('Name'),
      dataIndex: 'name',
      key: 'name',
      render: (_, record) => (
        <Space>
          <FileIcon filename={record.name} size={24} />
          <a onClick={() => record.type === 'folder' ? setCurrentFolder(record.id) : navigate(`/preview/${record.id}`)}>
            {record.name}
          </a>
        </Space>
      ),
    },
    {
      title: t('Size'),
      dataIndex: 'size',
      key: 'size',
      width: 120,
      render: (size) => formatFileSize(size),
    },
    {
      title: t('Modified'),
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (date) => formatDate(date),
    },
    {
      title: t('Actions'),
      key: 'actions',
      width: 150,
      render: (_, record) => {
        const items: MenuProps['items'] = [
          {
            key: 'preview',
            icon: <EyeOutlined />,
            label: t('Preview'),
            disabled: !canPreview(record.name),
            onClick: () => navigate(`/preview/${record.id}`),
          },
          {
            key: 'download',
            icon: <DownloadOutlined />,
            label: t('Download'),
            onClick: () => handleDownload(record),
          },
          {
            key: 'rename',
            icon: <EditOutlined />,
            label: t('Rename'),
            onClick: () => openRenameModal(record),
          },
          {
            key: 'favorite',
            icon: record.isFavorite ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />,
            label: record.isFavorite ? t('Remove from Favorites') : t('Add to Favorites'),
            onClick: () => toggleFavorite(record.id),
          },
          { type: 'divider' },
          {
            key: 'delete',
            icon: <DeleteOutlined />,
            label: t('Delete'),
            danger: true,
            onClick: () => handleDelete(record.id, filter === 'trash'),
          },
        ];

        return (
          <Space>
            {canPreview(record.name) && (
              <Tooltip title={t('Preview')}>
                <Button type="text" icon={<EyeOutlined />} onClick={() => navigate(`/preview/${record.id}`)} />
              </Tooltip>
            )}
            <Tooltip title={t('Download')}>
              <Button type="text" icon={<DownloadOutlined />} onClick={() => handleDownload(record)} />
            </Tooltip>
            <Dropdown menu={{ items }} placement="bottomRight">
              <Button type="text" icon={<MoreOutlined />} />
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  return (
    <div className="file-manager">
      {/* Toolbar */}
      <div className="file-manager-toolbar">
        <Space>
          <Upload
            customRequest={({ file }) => addUpload(file as File, currentFolder)}
            showUploadList={false}
            multiple
          >
            <Button type="primary" icon={<UploadOutlined />}>
              {t('Upload')}
            </Button>
          </Upload>

          <Button icon={<FolderAddOutlined />} onClick={() => setIsCreateFolderModalOpen(true)}>
            {t('New Folder')}
          </Button>

          {selectedFiles.length > 0 && (
            <>
              <Button icon={<DownloadOutlined />}>{t('Download')}</Button>
              <Button icon={<DeleteOutlined />} danger>
                {t('Delete')}
              </Button>
            </>
          )}
        </Space>

        <Space>
          <Input.Search
            placeholder={t('Search files')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ width: 250 }}
          />

          <Button.Group>
            <Button
              type={viewMode === 'list' ? 'primary' : 'default'}
              icon={<UnorderedListOutlined />}
              onClick={() => setViewMode('list')}
            />
            <Button
              type={viewMode === 'grid' ? 'primary' : 'default'}
              icon={<AppstoreOutlined />}
              onClick={() => setViewMode('grid')}
            />
          </Button.Group>
        </Space>
      </div>

      {/* Breadcrumb */}
      <Breadcrumb className="file-manager-breadcrumb">
        <Breadcrumb.Item onClick={() => setCurrentFolder(null)}>
          <a>{t('All Files')}</a>
        </Breadcrumb.Item>
        {breadcrumbs.map((folder) => (
          <Breadcrumb.Item key={folder.id} onClick={() => setCurrentFolder(folder.id)}>
            <a>{folder.name}</a>
          </Breadcrumb.Item>
        ))}
      </Breadcrumb>

      {/* File List */}
      <Spin spinning={loading}>
        {viewMode === 'list' ? (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={files}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showTotal: (total) => `${total} items`,
            }}
          />
        ) : (
          <div className="file-grid">
            {files.length === 0 ? (
              <Empty description={t('No files')} />
            ) : (
              files.map((file) => (
                <div
                  key={file.id}
                  className={`file-grid-item ${selectedFiles.includes(file.id) ? 'selected' : ''}`}
                  onClick={() => selectFile(file.id)}
                  onDoubleClick={() => navigate(`/preview/${file.id}`)}
                >
                  <FileIcon filename={file.name} size={48} />
                  <div className="file-name">{file.name}</div>
                  <div className="file-size">{formatFileSize(file.size)}</div>
                </div>
              ))
            )}
          </div>
        )}
      </Spin>

      {/* Create Folder Modal */}
      <Modal
        title={t('New Folder')}
        open={isCreateFolderModalOpen}
        onOk={handleCreateFolder}
        onCancel={() => setIsCreateFolderModalOpen(false)}
      >
        <Input
          placeholder={t('Folder name')}
          value={newFolderName}
          onChange={(e) => setNewFolderName(e.target.value)}
          onPressEnter={handleCreateFolder}
        />
      </Modal>

      {/* Rename Modal */}
      <Modal
        title={t('Rename')}
        open={isRenameModalOpen}
        onOk={handleRename}
        onCancel={() => setIsRenameModalOpen(false)}
      >
        <Input
          value={renameValue}
          onChange={(e) => setRenameValue(e.target.value)}
          onPressEnter={handleRename}
        />
      </Modal>
    </div>
  );
};
