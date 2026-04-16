import { getFileExtension } from './format';

// File type constants
export const FILE_TYPES = {
  IMAGE: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'tiff', 'svg'],
  VIDEO: ['mp4', 'webm', 'mov', 'avi', 'mkv', 'flv', 'wmv', 'm4v'],
  AUDIO: ['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a', 'wma'],
  DOCUMENT: ['doc', 'docx', 'pdf', 'txt', 'rtf', 'odt'],
  SPREADSHEET: ['xls', 'xlsx', 'csv', 'ods'],
  PRESENTATION: ['ppt', 'pptx', 'odp'],
  ARCHIVE: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'],
  CODE: ['js', 'ts', 'jsx', 'tsx', 'html', 'css', 'json', 'xml', 'py', 'java', 'cpp', 'c', 'go', 'rs', 'php', 'rb'],
};

/**
 * Get file type category
 */
export const getFileType = (filename: string): string => {
  const ext = getFileExtension(filename);

  if (FILE_TYPES.IMAGE.includes(ext)) return 'image';
  if (FILE_TYPES.VIDEO.includes(ext)) return 'video';
  if (FILE_TYPES.AUDIO.includes(ext)) return 'audio';
  if (FILE_TYPES.DOCUMENT.includes(ext)) return 'document';
  if (FILE_TYPES.SPREADSHEET.includes(ext)) return 'spreadsheet';
  if (FILE_TYPES.PRESENTATION.includes(ext)) return 'presentation';
  if (FILE_TYPES.ARCHIVE.includes(ext)) return 'archive';
  if (FILE_TYPES.CODE.includes(ext)) return 'code';

  return 'unknown';
};

/**
 * Get Ant Design icon type for file
 */
export const getFileIcon = (filename: string): string => {
  const type = getFileType(filename);

  switch (type) {
    case 'image': return 'FileImageOutlined';
    case 'video': return 'VideoCameraOutlined';
    case 'audio': return 'AudioOutlined';
    case 'document':
      return filename.toLowerCase().endsWith('.pdf') ? 'FilePdfOutlined' : 'FileWordOutlined';
    case 'spreadsheet': return 'FileExcelOutlined';
    case 'presentation': return 'FilePptOutlined';
    case 'archive': return 'FileZipOutlined';
    case 'code': return 'FileTextOutlined';
    default: return 'FileOutlined';
  }
};

/**
 * Get CSS class for file icon color
 */
export const getFileIconClass = (filename: string): string => {
  const ext = getFileExtension(filename).toLowerCase();

  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'tiff', 'svg'].includes(ext)) {
    return 'file-icon-image';
  }
  if (ext === 'pdf') return 'file-icon-pdf';
  if (['doc', 'docx'].includes(ext)) return 'file-icon-word';
  if (['xls', 'xlsx', 'csv'].includes(ext)) return 'file-icon-excel';
  if (['ppt', 'pptx'].includes(ext)) return 'file-icon-ppt';
  if (['mp4', 'webm', 'mov', 'avi', 'mkv'].includes(ext)) return 'file-icon-video';
  if (['mp3', 'wav', 'ogg', 'flac', 'aac'].includes(ext)) return 'file-icon-audio';
  if (['zip', 'rar', '7z', 'tar'].includes(ext)) return 'file-icon-archive';
  if (['js', 'ts', 'html', 'css', 'json', 'py', 'java'].includes(ext)) return 'file-icon-code';
  if (['txt', 'md', 'log'].includes(ext)) return 'file-icon-text';

  return 'file-icon-default';
};

/**
 * Check if file can be previewed
 */
export const canPreview = (filename: string): boolean => {
  const type = getFileType(filename);
  return ['image', 'video', 'audio', 'document', 'code', 'text'].includes(type);
};

/**
 * Get preview type for file
 */
export const getPreviewType = (filename: string): string => {
  const type = getFileType(filename);
  const ext = getFileExtension(filename).toLowerCase();

  if (type === 'image') return 'image';
  if (type === 'video') return 'video';
  if (type === 'audio') return 'audio';
  if (ext === 'pdf') return 'pdf';
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) return 'office';
  if (type === 'code' || type === 'text' || ext === 'txt' || ext === 'md') return 'text';

  return 'unsupported';
};

/**
 * Check if file is an image
 */
export const isImage = (filename: string): boolean => {
  return getFileType(filename) === 'image';
};

/**
 * Check if file is a video
 */
export const isVideo = (filename: string): boolean => {
  return getFileType(filename) === 'video';
};

/**
 * Check if file is an audio file
 */
export const isAudio = (filename: string): boolean => {
  return getFileType(filename) === 'audio';
};

/**
 * Accept types for file input
 */
export const getAcceptTypes = (type: string): string => {
  switch (type) {
    case 'image': return 'image/*';
    case 'video': return 'video/*';
    case 'audio': return 'audio/*';
    case 'document': return '.doc,.docx,.pdf,.txt,.rtf';
    case 'pdf': return '.pdf';
    case 'office': return '.doc,.docx,.xls,.xlsx,.ppt,.pptx';
    case 'archive': return '.zip,.rar,.7z,.tar,.gz';
    default: return '*';
  }
};
