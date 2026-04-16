import { Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from '@components/Layout';
import { Login } from '@pages/Login';
import { Dashboard } from '@pages/Dashboard';
import { AllFiles, Recent, Favorites, Trash } from '@pages/Files';
import { Preview } from '@pages/Preview';
import { Convert } from '@pages/Convert';
import { PdfTools, OcrTool, WatermarkTool, SplitMergeTool } from '@pages/Tools';
import { AiSummary, AiQa, AiClassification, AiTags } from '@pages/AI';
import { Tasks } from '@pages/Tasks';
import { Share } from '@pages/Share';
import { Subscription } from '@pages/Subscription';
import { Admin } from '@pages/Admin';

// Route definitions for reference
export const routeConfig = {
  public: [
    { path: '/login', component: Login },
    { path: '/share/:shareId', component: Share },
  ],
  protected: [
    { path: '/', component: Dashboard },
    { path: '/files', component: AllFiles },
    { path: '/files/recent', component: Recent },
    { path: '/files/favorites', component: Favorites },
    { path: '/files/trash', component: Trash },
    { path: '/preview/:fileId', component: Preview },
    { path: '/convert', component: Convert },
    { path: '/convert/document', component: Convert, props: { type: 'document' } },
    { path: '/convert/image', component: Convert, props: { type: 'image' } },
    { path: '/convert/video', component: Convert, props: { type: 'video' } },
    { path: '/convert/audio', component: Convert, props: { type: 'audio' } },
    { path: '/tools/pdf', component: PdfTools },
    { path: '/tools/ocr', component: OcrTool },
    { path: '/tools/watermark', component: WatermarkTool },
    { path: '/tools/split-merge', component: SplitMergeTool },
    { path: '/ai/summary', component: AiSummary },
    { path: '/ai/qa', component: AiQa },
    { path: '/ai/classification', component: AiClassification },
    { path: '/ai/tags', component: AiTags },
    { path: '/tasks', component: Tasks },
    { path: '/tasks/history', component: Tasks, props: { view: 'history' } },
    { path: '/subscription', component: Subscription },
    { path: '/admin/*', component: Admin },
  ],
};

// Breadcrumb name mapping
export const breadcrumbNameMap: Record<string, string> = {
  '/': 'Dashboard',
  '/files': 'All Files',
  '/files/recent': 'Recent',
  '/files/favorites': 'Favorites',
  '/files/trash': 'Trash',
  '/preview': 'Preview',
  '/convert': 'Format Convert',
  '/convert/document': 'Document Convert',
  '/convert/image': 'Image Convert',
  '/convert/video': 'Video Convert',
  '/convert/audio': 'Audio Convert',
  '/tools': 'Tools',
  '/tools/pdf': 'PDF Tools',
  '/tools/ocr': 'OCR',
  '/tools/watermark': 'Watermark',
  '/tools/split-merge': 'Split & Merge',
  '/ai': 'AI Assistant',
  '/ai/summary': 'Smart Summary',
  '/ai/qa': 'Document QA',
  '/ai/classification': 'Document Classification',
  '/ai/tags': 'Smart Tags',
  '/tasks': 'Task Center',
  '/tasks/history': 'Task History',
  '/subscription': 'Subscription',
  '/admin': 'Admin',
};

export default Routes;
