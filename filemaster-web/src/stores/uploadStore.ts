import { create } from 'zustand';
import api, { uploadFileWithProgress } from '@api';
import { UploadTask, UploadInitResponse } from '@types';

interface UploadState {
  uploads: UploadTask[];
  isUploading: boolean;
  globalSpeed: number;

  addUpload: (file: File, folderId?: string | null) => string;
  removeUpload: (id: string) => void;
  startUpload: (id: string) => Promise<void>;
  pauseUpload: (id: string) => void;
  resumeUpload: (id: string) => void;
  cancelUpload: (id: string) => void;
  clearCompleted: () => void;
  retryUpload: (id: string) => Promise<void>;
}

// Chunk size: 10MB
const CHUNK_SIZE = 10 * 1024 * 1024;
// Concurrent uploads
const CONCURRENT_CHUNKS = 3;

export const useUploadStore = create<UploadState>((set, get) => ({
  uploads: [],
  isUploading: false,
  globalSpeed: 0,

  addUpload: (file, folderId = null) => {
    const id = `${file.name}-${Date.now()}`;
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

    const task: UploadTask = {
      id,
      file,
      fileName: file.name,
      fileSize: file.size,
      mimeType: file.type,
      progress: 0,
      status: 'pending',
      uploadedChunks: [],
      totalChunks,
      folderId,
    };

    set((state) => ({
      uploads: [...state.uploads, task],
    }));

    // Auto-start upload
    setTimeout(() => {
      get().startUpload(id);
    }, 0);

    return id;
  },

  removeUpload: (id) => {
    const { uploads } = get();
    const task = uploads.find((u) => u.id === id);
    if (task?.status === 'uploading') {
      // Cancel ongoing upload
    }
    set((state) => ({
      uploads: state.uploads.filter((u) => u.id !== id),
    }));
  },

  startUpload: async (id) => {
    const { uploads } = get();
    const task = uploads.find((u) => u.id === id);
    if (!task || task.status === 'completed') return;

    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.id === id ? { ...u, status: 'uploading' } : u
      ),
      isUploading: true,
    }));

    try {
      // Step 1: Initialize upload
      const initResponse = await api.post('/api/files/upload/init', {
        fileName: task.fileName,
        fileSize: task.fileSize,
        mimeType: task.mimeType,
        folderId: task.folderId,
        chunkSize: CHUNK_SIZE,
        totalChunks: task.totalChunks,
      });

      const initData: UploadInitResponse = initResponse.data.data;
      const { uploadId, alreadyUploadedChunks = [] } = initData;

      // Update already uploaded chunks
      if (alreadyUploadedChunks.length > 0) {
        set((state) => ({
          uploads: state.uploads.map((u) =>
            u.id === id
              ? {
                  ...u,
                  uploadedChunks: alreadyUploadedChunks,
                  progress: Math.round(
                    (alreadyUploadedChunks.length / u.totalChunks) * 100
                  ),
                }
              : u
          ),
        }));
      }

      // Step 2: Upload chunks
      const chunksToUpload: number[] = [];
      for (let i = 0; i < task.totalChunks; i++) {
        if (!alreadyUploadedChunks.includes(i)) {
          chunksToUpload.push(i);
        }
      }

      // Upload chunks concurrently
      const uploadChunk = async (chunkNumber: number) => {
        const start = chunkNumber * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, task.fileSize);
        const chunk = task.file.slice(start, end);

        const formData = new FormData();
        formData.append('chunk', chunk);
        formData.append('chunkNumber', String(chunkNumber));

        await api.post(
          `/api/files/upload/chunk/${uploadId}/${chunkNumber}`,
          formData,
          {
            headers: { 'Content-Type': 'multipart/form-data' },
          }
        );

        set((state) => {
          const task = state.uploads.find((u) => u.id === id);
          if (!task) return state;

          const uploadedChunks = [...task.uploadedChunks, chunkNumber];
          const progress = Math.round(
            (uploadedChunks.length / task.totalChunks) * 100
          );

          return {
            uploads: state.uploads.map((u) =>
              u.id === id ? { ...u, uploadedChunks, progress } : u
            ),
          };
        });
      };

      // Process chunks in batches
      for (let i = 0; i < chunksToUpload.length; i += CONCURRENT_CHUNKS) {
        const batch = chunksToUpload.slice(i, i + CONCURRENT_CHUNKS);
        await Promise.all(batch.map(uploadChunk));
      }

      // Step 3: Complete upload
      await api.post('/api/files/upload/complete', { uploadId });

      set((state) => ({
        uploads: state.uploads.map((u) =>
          u.id === id
            ? { ...u, status: 'completed', progress: 100 }
            : u
        ),
        isUploading: state.uploads.some(
          (u) => u.id !== id && u.status === 'uploading'
        ),
      }));
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Upload failed';
      set((state) => ({
        uploads: state.uploads.map((u) =>
          u.id === id
            ? { ...u, status: 'error', error: errorMessage }
            : u
        ),
        isUploading: state.uploads.some(
          (u) => u.id !== id && u.status === 'uploading'
        ),
      }));
    }
  },

  pauseUpload: (id) => {
    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.id === id ? { ...u, status: 'paused' } : u
      ),
    }));
  },

  resumeUpload: (id) => {
    const { startUpload } = get();
    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.id === id ? { ...u, status: 'uploading' } : u
      ),
    }));
    startUpload(id);
  },

  cancelUpload: (id) => {
    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.id === id ? { ...u, status: 'error', error: 'Cancelled' } : u
      ),
    }));
  },

  clearCompleted: () => {
    set((state) => ({
      uploads: state.uploads.filter(
        (u) => u.status !== 'completed' && u.status !== 'error'
      ),
    }));
  },

  retryUpload: async (id) => {
    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.id === id
          ? { ...u, status: 'pending', error: undefined, progress: 0 }
          : u
      ),
    }));
    await get().startUpload(id);
  },
}));
