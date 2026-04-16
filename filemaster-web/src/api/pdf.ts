import api, { handleApiResponse } from './index';
import {
  PdfMergeRequest,
  PdfSplitRequest,
  PdfRotateRequest,
  PdfDeletePagesRequest,
  TextWatermarkRequest,
  ImageWatermarkRequest,
  PdfEncryptRequest,
  PdfDecryptRequest,
} from '@types';

export const pdfApi = {
  // Merge PDFs
  merge: (data: PdfMergeRequest) =>
    api.post('/api/pdf/merge', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Split PDF
  split: (data: PdfSplitRequest) =>
    api.post('/api/pdf/split', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Extract pages
  extract: (fileId: string, pages: number[]) =>
    api.post('/api/pdf/extract', { fileId, pages })
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Rotate pages
  rotate: (data: PdfRotateRequest) =>
    api.post('/api/pdf/rotate', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Delete pages
  deletePages: (data: PdfDeletePagesRequest) =>
    api.post('/api/pdf/delete-pages', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Get PDF info
  getInfo: (fileId: string) =>
    api.get(`/api/pdf/info?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        pageCount: number;
        author?: string;
        title?: string;
        subject?: string;
        encrypted: boolean;
      }>(Promise.resolve(r))),

  // Watermark
  addTextWatermark: (data: TextWatermarkRequest) =>
    api.post('/api/watermark/pdf/text', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  addImageWatermark: (data: ImageWatermarkRequest) =>
    api.post('/api/watermark/pdf/image', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  // Security
  encrypt: (data: PdfEncryptRequest) =>
    api.post('/api/security/pdf/encrypt', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  decrypt: (data: PdfDecryptRequest) =>
    api.post('/api/security/pdf/decrypt', data)
      .then(r => handleApiResponse<{ fileId: string; downloadUrl: string }>(Promise.resolve(r))),

  checkEncryption: (fileId: string) =>
    api.get(`/api/security/pdf/check?fileId=${fileId}`)
      .then(r => handleApiResponse<{
        encrypted: boolean;
        canPrint?: boolean;
        canModify?: boolean;
        canCopy?: boolean;
        canAnnotate?: boolean;
      }>(Promise.resolve(r))),
};
