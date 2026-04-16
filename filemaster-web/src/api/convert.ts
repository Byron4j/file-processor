import api, { handleApiResponse } from './index';
import { DocumentConvertRequest, ImageConvertRequest, VideoConvertRequest } from '@types';

export const convertApi = {
  // Document conversion
  convertDocToDocx: (fileId: string) =>
    api.post('/api/file/convert/doc-to-docx', { fileId })
      .then(r => handleApiResponse<{ outputPath: string }>(Promise.resolve(r))),

  convertDocument: (data: DocumentConvertRequest) =>
    api.post('/api/convert/document', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Image conversion
  convertImage: (data: ImageConvertRequest) =>
    api.post('/api/image/convert', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  createThumbnail: (fileId: string, width: number, height: number, mode: 'fit' | 'fill' | 'scale' = 'fit') =>
    api.post('/api/image/thumbnail', { fileId, width, height, mode })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  compressImage: (fileId: string, quality: number, maxWidth?: number, maxHeight?: number) =>
    api.post('/api/image/compress', { fileId, quality, maxWidth, maxHeight })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Video conversion
  convertVideo: (data: VideoConvertRequest) =>
    api.post('/api/media/video/transcode', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Audio conversion
  convertAudio: (fileId: string, targetFormat: string, codec?: string) =>
    api.post('/api/media/audio/transcode', { fileId, targetFormat, codec })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Excel conversion
  excelToCsv: (fileId: string, sheetIndex = 0, delimiter = ',') =>
    api.post('/api/excel/convert/csv', { fileId, sheetIndex, delimiter })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  excelToJson: (fileId: string, sheetIndex = 0, headerRow = 0) =>
    api.post('/api/excel/convert/json', { fileId, sheetIndex, headerRow })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),
};
