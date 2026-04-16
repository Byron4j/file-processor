import api, { handleApiResponse } from './index';

export const extractApi = {
  // Extract text from document
  extractText: (fileId: string) =>
    api.post('/api/file/extract/text', { fileId })
      .then(r => handleApiResponse<{ text: string; pages?: number }>(Promise.resolve(r))),

  // Extract text from Excel
  extractExcelText: (fileId: string, sheetIndex = 0) =>
    api.post('/api/excel/extract/text', { fileId, sheetIndex })
      .then(r => handleApiResponse<{
        sheets: { name: string; rows: number; columns: number }[];
        content: { sheetName: string; data: string[][] };
      }>(Promise.resolve(r))),

  // Get Excel info
  getExcelInfo: (fileId: string) =>
    api.get(`/api/excel/info?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        sheetCount: number;
        sheets: { index: number; name: string; rows: number; columns: number }[];
      }>(Promise.resolve(r))),

  // Get image info
  getImageInfo: (fileId: string) =>
    api.get(`/api/image/info?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        width: number;
        height: number;
        format: string;
        size: number;
        colorSpace?: string;
      }>(Promise.resolve(r))),

  // Get video info
  getVideoInfo: (fileId: string) =>
    api.get(`/api/media/video/info?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        duration: number;
        width: number;
        height: number;
        codec: string;
        bitrate: number;
        fps?: number;
      }>(Promise.resolve(r))),

  // Get audio info
  getAudioInfo: (fileId: string) =>
    api.get(`/api/media/audio/info?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        duration: number;
        codec: string;
        sampleRate: number;
        channels: number;
        bitrate: number;
      }>(Promise.resolve(r))),

  // Extract video thumbnail
  extractThumbnail: (fileId: string, timestamp: number, width?: number, height?: number) =>
    api.post('/api/media/video/thumbnail', { fileId, timestamp, width, height })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),
};
