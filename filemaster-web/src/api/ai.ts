import api, { handleApiResponse } from './index';
import { AiSummaryRequest, AiTagsRequest, AiAskRequest, AiResponse } from '@types';

export const aiApi = {
  // AI Summary
  summarize: (data: AiSummaryRequest) =>
    api.post('/api/ai/summary', data)
      .then(r => handleApiResponse<AiResponse>(Promise.resolve(r))),

  // AI Tags
  generateTags: (data: AiTagsRequest) =>
    api.post('/api/ai/tags', data)
      .then(r => handleApiResponse<AiResponse>(Promise.resolve(r))),

  // AI Q&A
  ask: (data: AiAskRequest) =>
    api.post('/api/ai/ask', data)
      .then(r => handleApiResponse<AiResponse>(Promise.resolve(r))),

  // Document Classification
  classify: (fileId: string) =>
    api.post('/api/intelligence/classify', { fileId })
      .then(r => handleApiResponse<{
        documentType: string;
        confidence: number;
        keywords: string[];
      }>(Promise.resolve(r))),

  // Sensitive Info Detection
  detectSensitiveInfo: (fileId: string) =>
    api.post('/api/intelligence/sensitive-info', { fileId })
      .then(r => handleApiResponse<{
        findings: { type: string; value: string; position?: number }[];
      }>(Promise.resolve(r))),

  // Keyword Extraction
  extractKeywords: (fileId: string, count = 10) =>
    api.post('/api/intelligence/keywords', { fileId, count })
      .then(r => handleApiResponse<{ keywords: string[] }>(Promise.resolve(r))),

  // Text Summarization (non-AI)
  extractSummary: (fileId: string, maxLength = 500) =>
    api.post('/api/intelligence/summary', { fileId, maxLength })
      .then(r => handleApiResponse<{ summary: string }>(Promise.resolve(r))),
};
