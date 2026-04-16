import { create } from 'zustand';
import api, { handleApiResponse } from '@api';
import { FileItem, FileQueryParams, FileListResponse, Folder } from '@types';

interface FileState {
  // State
  files: FileItem[];
  folders: Folder[];
  currentFolder: string | null;
  selectedFiles: string[];
  currentFile: FileItem | null;
  viewMode: 'list' | 'grid';
  loading: boolean;
  error: string | null;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  breadcrumbs: Folder[];

  // Actions
  fetchFiles: (params?: FileQueryParams) => Promise<void>;
  fetchFolders: (parentId?: string | null) => Promise<void>;
  fetchBreadcrumbs: (folderId?: string | null) => Promise<void>;
  setCurrentFolder: (folderId: string | null) => void;
  selectFile: (fileId: string, multiple?: boolean) => void;
  selectAll: (selected: boolean) => void;
  clearSelection: () => void;
  setViewMode: (mode: 'list' | 'grid') => void;
  deleteFile: (fileId: string, permanent?: boolean) => Promise<void>;
  renameFile: (fileId: string, newName: string) => Promise<void>;
  moveFile: (fileId: string, targetFolderId: string | null) => Promise<void>;
  toggleFavorite: (fileId: string) => Promise<void>;
  createFolder: (name: string, parentId?: string | null) => Promise<void>;
  setCurrentFile: (file: FileItem | null) => void;
  clearError: () => void;
}

export const useFileStore = create<FileState>((set, get) => ({
  files: [],
  folders: [],
  currentFolder: null,
  selectedFiles: [],
  currentFile: null,
  viewMode: 'list',
  loading: false,
  error: null,
  pagination: {
    current: 1,
    pageSize: 50,
    total: 0,
  },
  breadcrumbs: [],

  fetchFiles: async (params = {}) => {
    set({ loading: true, error: null });
    try {
      const queryParams = new URLSearchParams();
      if (params.folderId !== undefined) queryParams.set('folderId', params.folderId || '');
      if (params.type) queryParams.set('type', params.type);
      if (params.status) queryParams.set('status', params.status);
      if (params.isFavorite !== undefined) queryParams.set('isFavorite', String(params.isFavorite));
      if (params.keyword) queryParams.set('keyword', params.keyword);
      if (params.tags?.length) queryParams.set('tags', params.tags.join(','));
      if (params.page !== undefined) queryParams.set('page', String(params.page));
      if (params.size !== undefined) queryParams.set('size', String(params.size));
      if (params.sortBy) queryParams.set('sortBy', params.sortBy);
      if (params.sortOrder) queryParams.set('sortOrder', params.sortOrder);

      const response = await api.get(`/api/files?${queryParams.toString()}`);
      const data = await handleApiResponse<FileListResponse>(Promise.resolve(response));

      set({
        files: data.content,
        pagination: {
          current: data.number + 1,
          pageSize: data.size,
          total: data.totalElements,
        },
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch files',
        loading: false,
      });
    }
  },

  fetchFolders: async (parentId = null) => {
    try {
      const queryParams = new URLSearchParams();
      if (parentId) queryParams.set('parentId', parentId);

      const response = await api.get(`/api/folders?${queryParams.toString()}`);
      const data = await handleApiResponse<Folder[]>(Promise.resolve(response));

      set({ folders: data });
    } catch (error) {
      console.error('Failed to fetch folders:', error);
    }
  },

  fetchBreadcrumbs: async (folderId = null) => {
    if (!folderId) {
      set({ breadcrumbs: [] });
      return;
    }

    try {
      const response = await api.get(`/api/folders/${folderId}/path`);
      const data = await handleApiResponse<Folder[]>(Promise.resolve(response));
      set({ breadcrumbs: data });
    } catch (error) {
      console.error('Failed to fetch breadcrumbs:', error);
    }
  },

  setCurrentFolder: (folderId) => {
    set({ currentFolder: folderId, selectedFiles: [] });
    const { fetchFiles, fetchFolders, fetchBreadcrumbs } = get();
    fetchFiles({ folderId });
    fetchFolders(folderId);
    fetchBreadcrumbs(folderId);
  },

  selectFile: (fileId, multiple = false) => {
    const { selectedFiles } = get();
    if (multiple) {
      if (selectedFiles.includes(fileId)) {
        set({ selectedFiles: selectedFiles.filter((id) => id !== fileId) });
      } else {
        set({ selectedFiles: [...selectedFiles, fileId] });
      }
    } else {
      set({ selectedFiles: selectedFiles.includes(fileId) ? [] : [fileId] });
    }
  },

  selectAll: (selected) => {
    const { files } = get();
    if (selected) {
      set({ selectedFiles: files.map((f) => f.id) });
    } else {
      set({ selectedFiles: [] });
    }
  },

  clearSelection: () => set({ selectedFiles: [] }),

  setViewMode: (mode) => set({ viewMode: mode }),

  deleteFile: async (fileId, permanent = false) => {
    try {
      await api.delete(`/api/files/${fileId}?permanent=${permanent}`);
      const { files, selectedFiles } = get();
      set({
        files: files.filter((f) => f.id !== fileId),
        selectedFiles: selectedFiles.filter((id) => id !== fileId),
      });
    } catch (error) {
      throw error;
    }
  },

  renameFile: async (fileId, newName) => {
    try {
      await api.put(`/api/files/${fileId}/rename`, { name: newName });
      const { files } = get();
      set({
        files: files.map((f) =>
          f.id === fileId ? { ...f, name: newName } : f
        ),
      });
    } catch (error) {
      throw error;
    }
  },

  moveFile: async (fileId, targetFolderId) => {
    try {
      await api.put(`/api/files/${fileId}/move`, { folderId: targetFolderId });
      const { files, currentFolder } = get();
      // Remove from current view if moved to different folder
      if (targetFolderId !== currentFolder) {
        set({ files: files.filter((f) => f.id !== fileId) });
      }
    } catch (error) {
      throw error;
    }
  },

  toggleFavorite: async (fileId) => {
    try {
      const response = await api.post(`/api/files/${fileId}/favorite`);
      const data = await handleApiResponse<{ isFavorite: boolean }>(Promise.resolve(response));
      const { files } = get();
      set({
        files: files.map((f) =>
          f.id === fileId ? { ...f, isFavorite: data.isFavorite } : f
        ),
      });
    } catch (error) {
      throw error;
    }
  },

  createFolder: async (name, parentId = null) => {
    try {
      const response = await api.post('/api/folders', { name, parentId });
      const data = await handleApiResponse<Folder>(Promise.resolve(response));
      const { folders } = get();
      set({ folders: [...folders, data] });
    } catch (error) {
      throw error;
    }
  },

  setCurrentFile: (file) => set({ currentFile: file }),

  clearError: () => set({ error: null }),
}));
