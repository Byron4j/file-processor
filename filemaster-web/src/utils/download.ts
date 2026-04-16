import api from '@api';

/**
 * Download file from URL
 */
export const downloadFromUrl = async (
  url: string,
  filename?: string,
  onProgress?: (progress: number) => void
): Promise<void> => {
  const response = await api.get(url, {
    responseType: 'blob',
    onDownloadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(progress);
      }
    },
  });

  const blob = new Blob([response.data]);
  const downloadUrl = window.URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = downloadUrl;
  link.download = filename || 'download';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  window.URL.revokeObjectURL(downloadUrl);
};

/**
 * Download file by file ID
 */
export const downloadFile = async (
  fileId: string,
  filename?: string,
  onProgress?: (progress: number) => void
): Promise<void> => {
  await downloadFromUrl(
    `/api/files/download/${fileId}`,
    filename,
    onProgress
  );
};

/**
 * Download multiple files as ZIP
 */
export const downloadMultipleFiles = async (
  fileIds: string[],
  zipName: string = 'download.zip'
): Promise<void> => {
  const response = await api.post(
    '/api/files/download/batch',
    { fileIds },
    { responseType: 'blob' }
  );

  const blob = new Blob([response.data], { type: 'application/zip' });
  const downloadUrl = window.URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = downloadUrl;
  link.download = zipName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  window.URL.revokeObjectURL(downloadUrl);
};

/**
 * Create object URL from blob
 */
export const createObjectUrl = (blob: Blob): string => {
  return window.URL.createObjectURL(blob);
};

/**
 * Revoke object URL
 */
export const revokeObjectUrl = (url: string): void => {
  window.URL.revokeObjectURL(url);
};
