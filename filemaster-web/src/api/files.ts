import api, { handleApiResponse, downloadFile } from './index';
import {
  FileItem,
  FileQueryParams,
  FileListResponse,
  Folder,
  ShareCreateRequest,
  ShareInfo,
} from '@types';

export const fileApi = {
  // Get file list
  getFiles: (params?: FileQueryParams) => {
    const queryParams = new URLSearchParams();
    if (params?.folderId !== undefined) queryParams.set('folderId', params.folderId || '');
    if (params?.type) queryParams.set('type', params.type);
    if (params?.status) queryParams.set('status', params.status);
    if (params?.isFavorite !== undefined) queryParams.set('isFavorite', String(params.isFavorite));
    if (params?.keyword) queryParams.set('keyword', params.keyword);
    if (params?.tags?.length) queryParams.set('tags', params.tags.join(','));
    if (params?.page !== undefined) queryParams.set('page', String(params.page));
    if (params?.size !== undefined) queryParams.set('size', String(params.size));
    if (params?.sortBy) queryParams.set('sortBy', params.sortBy);
    if (params?.sortOrder) queryParams.set('sortOrder', params.sortOrder);

    return api.get(`/api/files?${queryParams.toString()}`)
      .then(r => handleApiResponse<FileListResponse>(Promise.resolve(r)));
  },

  // Get single file
  getFile: (fileId: string) =>
    api.get(`/api/files/${fileId}`)
      .then(r => handleApiResponse<FileItem>(Promise.resolve(r))),

  // Delete file
  deleteFile: (fileId: string, permanent = false) =>
    api.delete(`/api/files/${fileId}?permanent=${permanent}`),

  // Rename file
  renameFile: (fileId: string, name: string) =>
    api.put(`/api/files/${fileId}/rename`, { name })
      .then(r => handleApiResponse<FileItem>(Promise.resolve(r))),

  // Move file
  moveFile: (fileId: string, folderId: string | null) =>
    api.put(`/api/files/${fileId}/move`, { folderId }),

  // Copy file
  copyFile: (fileId: string, folderId: string | null) =>
    api.post(`/api/files/${fileId}/copy`, { folderId })
      .then(r => handleApiResponse<FileItem>(Promise.resolve(r))),

  // Toggle favorite
  toggleFavorite: (fileId: string) =>
    api.post(`/api/files/${fileId}/favorite`)
      .then(r => handleApiResponse<{ isFavorite: boolean }>(Promise.resolve(r))),

  // Update tags
  updateTags: (fileId: string, tags: string[]) =>
    api.post(`/api/files/${fileId}/tags`, { tags })
      .then(r => handleApiResponse<FileItem>(Promise.resolve(r))),

  // Download file
  download: (fileId: string, filename?: string, onProgress?: (progress: number) => void) =>
    downloadFile(`/api/files/download/${fileId}`, filename, onProgress),

  // Batch download
  batchDownload: (fileIds: string[], zipName = 'download.zip') =>
    api.post('/api/files/download/batch', { fileIds }, { responseType: 'blob' }),

  // Get file statistics
  getStatistics: () =>
    api.get('/api/files/statistics')
      .then(r => handleApiResponse<{
        totalFiles: number;
        totalSize: number;
        typeDistribution: { type: string; count: number; size: number }[];
      }>(Promise.resolve(r))),

  // Folder operations
  getFolders: (parentId?: string | null) => {
    const queryParams = new URLSearchParams();
    if (parentId) queryParams.set('parentId', parentId);
    return api.get(`/api/folders?${queryParams.toString()}`)
      .then(r => handleApiResponse<Folder[]>(Promise.resolve(r)));
  },

  createFolder: (name: string, parentId?: string | null) =>
    api.post('/api/folders', { name, parentId })
      .then(r => handleApiResponse<Folder>(Promise.resolve(r))),

  renameFolder: (folderId: string, name: string) =>
    api.put(`/api/folders/${folderId}/rename`, { name })
      .then(r => handleApiResponse<Folder>(Promise.resolve(r))),

  deleteFolder: (folderId: string) =>
    api.delete(`/api/folders/${folderId}`),

  getFolderPath: (folderId: string) =>
    api.get(`/api/folders/${folderId}/path`)
      .then(r => handleApiResponse<Folder[]>(Promise.resolve(r))),

  // Share operations
  createShare: (data: ShareCreateRequest) =>
    api.post('/api/share/create', data)
      .then(r => handleApiResponse<ShareInfo>(Promise.resolve(r))),

  getShare: (shareId: string) =>
    api.get(`/api/share/${shareId}`)
      .then(r => handleApiResponse<ShareInfo>(Promise.resolve(r))),

  verifySharePassword: (shareId: string, password: string) =>
    api.post(`/api/share/${shareId}/verify`, { password }),

  getMyShares: () =>
    api.get('/api/share/my')
      .then(r => handleApiResponse<ShareInfo[]>(Promise.resolve(r))),

  revokeShare: (shareId: string) =>
    api.delete(`/api/share/${shareId}`),
};
