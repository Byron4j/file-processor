import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api, { handleApiResponse } from '@api';
import { User, LoginCredentials, LoginResponse } from '@types';

interface AuthState {
  // State
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  fetchCurrentUser: () => Promise<void>;
  clearError: () => void;
  updateUser: (user: Partial<User>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      login: async (credentials) => {
        set({ isLoading: true, error: null });
        try {
          const response = await api.post('/api/auth/login', credentials);
          const result = await handleApiResponse<LoginResponse>(Promise.resolve(response));

          const { accessToken, refreshToken, user } = result;

          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', refreshToken);

          set({
            user,
            accessToken,
            refreshToken,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error) {
          set({
            error: error instanceof Error ? error.message : 'Login failed',
            isLoading: false,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      logout: async () => {
        try {
          await api.post('/api/auth/logout');
        } catch (error) {
          console.error('Logout error:', error);
        } finally {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
          });
        }
      },

      refresh: async () => {
        const { refreshToken: currentRefreshToken } = get();
        if (!currentRefreshToken) {
          throw new Error('No refresh token');
        }

        try {
          const response = await api.post('/api/auth/refresh', {
            refreshToken: currentRefreshToken,
          });
          const result = await handleApiResponse<LoginResponse>(Promise.resolve(response));

          const { accessToken, refreshToken } = result;
          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', refreshToken);

          set({
            accessToken,
            refreshToken,
            isAuthenticated: true,
          });
        } catch (error) {
          // Refresh failed, clear auth state
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      fetchCurrentUser: async () => {
        try {
          const response = await api.get('/api/auth/me');
          const user = await handleApiResponse<User>(Promise.resolve(response));
          set({ user, isAuthenticated: true });
        } catch (error) {
          console.error('Fetch user error:', error);
          set({ isAuthenticated: false });
        }
      },

      clearError: () => set({ error: null }),

      updateUser: (updates) => {
        const { user } = get();
        if (user) {
          set({ user: { ...user, ...updates } });
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
