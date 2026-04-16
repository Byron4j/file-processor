import { create } from 'zustand';
import api, { handleApiResponse } from '@api';
import { Task, TaskQueryParams, TaskStatus, TaskType } from '@types';

interface TaskState {
  tasks: Task[];
  currentTask: Task | null;
  loading: boolean;
  error: string | null;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };

  fetchTasks: (params?: TaskQueryParams) => Promise<void>;
  fetchTask: (taskId: string) => Promise<void>;
  submitTask: (type: TaskType, params: Record<string, any>) => Promise<string>;
  cancelTask: (taskId: string) => Promise<void>;
  updateTaskProgress: (taskId: string, progress: number) => void;
  updateTaskStatus: (taskId: string, status: TaskStatus) => void;
  clearError: () => void;
}

export const useTaskStore = create<TaskState>((set, get) => ({
  tasks: [],
  currentTask: null,
  loading: false,
  error: null,
  pagination: {
    current: 1,
    pageSize: 20,
    total: 0,
  },

  fetchTasks: async (params = {}) => {
    set({ loading: true, error: null });
    try {
      const queryParams = new URLSearchParams();
      if (params.type) queryParams.set('type', params.type);
      if (params.status) queryParams.set('status', params.status);
      if (params.page !== undefined) queryParams.set('page', String(params.page));
      if (params.size !== undefined) queryParams.set('size', String(params.size));

      const response = await api.get(`/api/tasks?${queryParams.toString()}`);
      const data = await handleApiResponse<{
        content: Task[];
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
      }>(Promise.resolve(response));

      set({
        tasks: data.content,
        pagination: {
          current: data.number + 1,
          pageSize: data.size,
          total: data.totalElements,
        },
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch tasks',
        loading: false,
      });
    }
  },

  fetchTask: async (taskId) => {
    try {
      const response = await api.get(`/api/tasks/${taskId}/status`);
      const data = await handleApiResponse<Task>(Promise.resolve(response));
      set((state) => ({
        currentTask: data,
        tasks: state.tasks.map((t) => (t.id === taskId ? data : t)),
      }));
    } catch (error) {
      console.error('Failed to fetch task:', error);
    }
  },

  submitTask: async (type, params) => {
    const response = await api.post('/api/tasks/submit', {
      type,
      ...params,
    });
    const data = await handleApiResponse<{ taskId: string }>(Promise.resolve(response));
    return data.taskId;
  },

  cancelTask: async (taskId) => {
    await api.post(`/api/tasks/${taskId}/cancel`);
    set((state) => ({
      tasks: state.tasks.map((t) =>
        t.id === taskId ? { ...t, status: 'CANCELLED' as TaskStatus } : t
      ),
    }));
  },

  updateTaskProgress: (taskId, progress) => {
    set((state) => ({
      tasks: state.tasks.map((t) =>
        t.id === taskId ? { ...t, progress } : t
      ),
    }));
  },

  updateTaskStatus: (taskId, status) => {
    set((state) => ({
      tasks: state.tasks.map((t) =>
        t.id === taskId ? { ...t, status } : t
      ),
    }));
  },

  clearError: () => set({ error: null }),
}));
