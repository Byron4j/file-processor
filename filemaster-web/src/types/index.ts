// User types
export interface User {
  id: string;
  username: string;
  email?: string;
  avatar?: string;
  roles: string[];
  status: number;
  createdAt?: string;
}

export interface LoginCredentials {
  username: string;
  password: string;
  remember?: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// File types
export interface FileItem {
  id: string;
  name: string;
  originalName: string;
  type: string;
  mimeType: string;
  size: number;
  path?: string;
  folderId?: string | null;
  ownerId: string;
  tags?: string[];
  metadata?: Record<string, any>;
  status: 'active' | 'deleted' | 'archived';
  isFavorite: boolean;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface FileQueryParams {
  folderId?: string | null;
  type?: string;
  status?: string;
  isFavorite?: boolean;
  keyword?: string;
  tags?: string[];
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface FileListResponse {
  content: FileItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Folder types
export interface Folder {
  id: string;
  name: string;
  parentId?: string | null;
  ownerId: string;
  path: string;
  createdAt: string;
  updatedAt: string;
}

// Upload types
export interface UploadTask {
  id: string;
  file: File;
  fileName: string;
  fileSize: number;
  mimeType: string;
  progress: number;
  status: 'pending' | 'uploading' | 'paused' | 'completed' | 'error';
  uploadedChunks: number[];
  totalChunks: number;
  folderId?: string | null;
  error?: string;
  speed?: number;
  remainingTime?: number;
}

export interface UploadInitResponse {
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  alreadyUploadedChunks?: number[];
}

// Preview types
export type PreviewType = 'image' | 'pdf' | 'video' | 'audio' | 'office' | 'text' | 'code' | 'unsupported';

export interface PreviewInfo {
  fileId: string;
  type: PreviewType;
  mimeType: string;
  url?: string;
  downloadUrl: string;
  size: number;
  name: string;
  extension: string;
  supports: string[];
  metadata?: Record<string, any>;
}

export interface Subtitle {
  src: string;
  srclang: string;
  label: string;
  default?: boolean;
}

export interface VideoQuality {
  label: string;
  src: string;
  resolution: string;
}

// Task types
export type TaskStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
export type TaskType = 'CONVERT' | 'WATERMARK' | 'MERGE' | 'SPLIT' | 'EXTRACT' | 'TRANSCODE' | 'OCR' | 'AI_SUMMARY' | 'AI_TAGS';

export interface Task {
  id: string;
  type: TaskType;
  status: TaskStatus;
  progress: number;
  totalItems: number;
  processedItems: number;
  inputFiles: string[];
  outputPath?: string;
  errorMessage?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  createdBy: string;
}

export interface TaskQueryParams {
  type?: TaskType;
  status?: TaskStatus;
  page?: number;
  size?: number;
}

// Convert types
export interface ConvertOptions {
  targetFormat: string;
  quality?: number;
  width?: number;
  height?: number;
  keepAspectRatio?: boolean;
}

export interface DocumentConvertRequest {
  fileId: string;
  targetFormat: 'docx' | 'doc' | 'pdf' | 'txt' | 'html';
  preserveFormatting?: boolean;
}

export interface ImageConvertRequest {
  fileId: string;
  targetFormat: 'jpeg' | 'png' | 'gif' | 'bmp' | 'webp' | 'tiff';
  quality?: number;
  width?: number;
  height?: number;
  mode?: 'fit' | 'fill' | 'scale';
}

export interface VideoConvertRequest {
  fileId: string;
  targetFormat: 'mp4' | 'webm' | 'mov' | 'avi' | 'mkv';
  resolution?: string;
  videoCodec?: string;
  audioCodec?: string;
  bitrate?: string;
}

// PDF Tool types
export interface PdfMergeRequest {
  fileIds: string[];
  addBookmarks?: boolean;
  outputName?: string;
}

export interface PdfSplitRequest {
  fileId: string;
  mode: 'ranges' | 'every' | 'extract';
  ranges?: string;
  everyNPages?: number;
  pages?: number[];
}

export interface PdfRotateRequest {
  fileId: string;
  degrees: 90 | 180 | 270;
  pages?: number[];
}

export interface PdfDeletePagesRequest {
  fileId: string;
  pages: number[];
}

// Watermark types
export interface TextWatermarkRequest {
  fileId: string;
  text: string;
  fontSize?: number;
  color?: string;
  opacity?: number;
  rotation?: number;
  position?: 'center' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'tile';
}

export interface ImageWatermarkRequest {
  fileId: string;
  watermarkFileId: string;
  opacity?: number;
  scale?: number;
  position?: 'center' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'tile';
}

// Security types
export interface PdfEncryptRequest {
  fileId: string;
  userPassword?: string;
  ownerPassword: string;
  permissions?: {
    canPrint?: boolean;
    canModify?: boolean;
    canCopy?: boolean;
    canAnnotate?: boolean;
  };
  keyLength?: 128 | 256;
}

export interface PdfDecryptRequest {
  fileId: string;
  password: string;
}

// AI types
export interface AiSummaryRequest {
  fileId: string;
  maxLength?: number;
  style?: 'concise' | 'detailed' | 'bullet-points';
}

export interface AiTagsRequest {
  fileId: string;
  count?: number;
}

export interface AiAskRequest {
  fileId: string;
  question: string;
  context?: string;
}

export interface AiResponse {
  answer: string;
  confidence?: number;
  sources?: string[];
}

// Share types
export interface ShareCreateRequest {
  fileId: string;
  password?: string;
  expiresAt?: string;
  allowDownload?: boolean;
  maxDownloads?: number;
}

export interface ShareInfo {
  id: string;
  fileId: string;
  fileName: string;
  createdAt: string;
  expiresAt?: string;
  hasPassword: boolean;
  allowDownload: boolean;
  downloadCount: number;
  maxDownloads?: number;
}

// API Response types
export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  code?: string;
  data?: T;
  timestamp: string;
}

export interface FileResponse<T = any> {
  success: boolean;
  message: string;
  filePath?: string;
  fileSize?: number;
  data?: T;
}

// Theme types
export type ThemeMode = 'light' | 'dark' | 'auto';

// Stats types
export interface DashboardStats {
  totalFiles: number;
  totalSize: number;
  recentFiles: FileItem[];
  uploadStats: {
    date: string;
    count: number;
    size: number;
  }[];
  typeDistribution: {
    type: string;
    count: number;
    size: number;
  }[];
}

// Audit log types
export interface AuditLog {
  id: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  details?: Record<string, any>;
  ip?: string;
  userAgent?: string;
  createdAt: string;
}

// Notification types
export interface Notification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  link?: string;
}
