import api, { handleApiResponse } from './index';
import { Task, TaskQueryParams, TaskType } from '@types';

export const taskApi = {
  // Submit task
  submit: (type: TaskType, params: Record<string, any>) =>
    api.post('/api/tasks/submit', { type, ...params })
      .then(r => handleApiResponse<{ taskId: string }>(Promise.resolve(r))),

  // Get task status
  getStatus: (taskId: string) =>
    api.get(`/api/tasks/${taskId}/status`)
      .then(r => handleApiResponse<Task>(Promise.resolve(r))),

  // Cancel task
  cancel: (taskId: string) =>
    api.post(`/api/tasks/${taskId}/cancel`),

  // List tasks
  list: (params?: TaskQueryParams) => {
    const queryParams = new URLSearchParams();
    if (params?.type) queryParams.set('type', params.type);
    if (params?.status) queryParams.set('status', params.status);
    if (params?.page !== undefined) queryParams.set('page', String(params.page));
    if (params?.size !== undefined) queryParams.set('size', String(params.size));

    return api.get(`/api/tasks?${queryParams.toString()}`)
      .then(r => handleApiResponse<{
        content: Task[];
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
      }>(Promise.resolve(r)));
  },

  // Batch operations
  batchConvert: (fileIds: string[], targetFormat: string, options?: Record<string, any>) =>
    api.post('/api/batch/convert', { fileIds, targetFormat, ...options })
      .then(r => handleApiResponse<{ taskId: string }>(Promise.resolve(r))),

  batchWatermark: (fileIds: string[], type: 'text' | 'image', config: Record<string, any>) =>
    api.post('/api/batch/watermark', { fileIds, type, ...config })
      .then(r => handleApiResponse<{ taskId: string }>(Promise.resolve(r))),

  batchExtract: (fileIds: string[]) =>
    api.post('/api/batch/extract', { fileIds })
      .then(r => handleApiResponse<{ taskId: string; results: { fileId: string; text: string }[] }>(Promise.resolve(r))),
};
