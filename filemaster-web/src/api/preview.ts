import api, { handleApiResponse } from './index';
import { PreviewInfo } from '@types';

export const previewApi = {
  // Get preview info
  getPreviewInfo: (fileId: string) =>
    api.get(`/api/preview/${fileId}`)
      .then(r => handleApiResponse<PreviewInfo>(Promise.resolve(r))),

  // Get preview content (for text/code files)
  getPreviewContent: (fileId: string) =>
    api.get(`/api/preview/${fileId}/content`)
      .then(r => handleApiResponse<{ content: string; encoding: string }>(Promise.resolve(r))),

  // Get thumbnail URL
  getThumbnailUrl: (fileId: string, width?: number, height?: number) => {
    const params = new URLSearchParams();
    if (width) params.set('width', String(width));
    if (height) params.set('height', String(height));
    return `/api/preview/${fileId}/thumbnail?${params.toString()}`;
  },

  // Get PDF preview URL
  getPdfUrl: (fileId: string) => `/api/preview/${fileId}/pdf`,

  // Get video stream URL
  getVideoUrl: (fileId: string) => `/api/preview/${fileId}/stream`,

  // Get audio stream URL
  getAudioUrl: (fileId: string) => `/api/preview/${fileId}/audio`,

  // Get image preview URL
  getImageUrl: (fileId: string, maxWidth?: number) => {
    const params = new URLSearchParams();
    if (maxWidth) params.set('maxWidth', String(maxWidth));
    return `/api/preview/${fileId}/image?${params.toString()}`;
  },
};
