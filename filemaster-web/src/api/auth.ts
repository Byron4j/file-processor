import api, { handleApiResponse } from './index';
import { LoginCredentials, LoginResponse, User } from '@types';

export const authApi = {
  // Login
  login: (credentials: LoginCredentials) =>
    api.post('/api/auth/login', credentials).then(r => handleApiResponse<LoginResponse>(Promise.resolve(r))),

  // Logout
  logout: () =>
    api.post('/api/auth/logout'),

  // Refresh token
  refresh: (refreshToken: string) =>
    api.post('/api/auth/refresh', { refreshToken }).then(r => handleApiResponse<LoginResponse>(Promise.resolve(r))),

  // Get current user
  getCurrentUser: () =>
    api.get('/api/auth/me').then(r => handleApiResponse<User>(Promise.resolve(r))),

  // Update profile
  updateProfile: (data: Partial<User>) =>
    api.put('/api/auth/profile', data).then(r => handleApiResponse<User>(Promise.resolve(r))),

  // Change password
  changePassword: (oldPassword: string, newPassword: string) =>
    api.post('/api/auth/change-password', { oldPassword, newPassword }),
};
