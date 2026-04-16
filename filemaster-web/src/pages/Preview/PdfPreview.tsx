import { useEffect, useRef, useState } from 'react';
import * as pdfjs from 'pdfjs-dist';
import { Button, Space, InputNumber, message } from 'antd';
import {
  ZoomInOutlined,
  ZoomOutOutlined,
  RotateLeftOutlined,
  DownloadOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons';
import { previewApi } from '@api';

// PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

interface PdfPreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const PdfPreview = ({ fileId, downloadUrl }: PdfPreviewProps) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [pdf, setPdf] = useState<pdfjs.PDFDocumentProxy | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [scale, setScale] = useState(1.0);
  const [rotation, setRotation] = useState(0);

  useEffect(() => {
    loadPdf();
  }, [fileId]);

  useEffect(() => {
    if (pdf) {
      renderPage(currentPage);
    }
  }, [pdf, currentPage, scale, rotation]);

  const loadPdf = async () => {
    try {
      const url = previewApi.getPdfUrl(fileId);
      const loadingTask = pdfjs.getDocument(url);
      const pdfDoc = await loadingTask.promise;
      setPdf(pdfDoc);
      setTotalPages(pdfDoc.numPages);
    } catch (error) {
      message.error('Failed to load PDF');
    }
  };

  const renderPage = async (pageNum: number) => {
    if (!pdf || !canvasRef.current) return;

    try {
      const page = await pdf.getPage(pageNum);
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');

      if (!context) return;

      const viewport = page.getViewport({ scale, rotation });
      canvas.height = viewport.height;
      canvas.width = viewport.width;

      await page.render({
        canvasContext: context,
        viewport: viewport,
        canvas: canvas,
      }).promise;
    } catch (error) {
      message.error('Failed to render page');
    }
  };

  return (
    <div className="pdf-preview">
      <div className="pdf-toolbar">
        <Space>
          <Button.Group>
            <Button
              icon={<LeftOutlined />}
              disabled={currentPage <= 1}
              onClick={() => setCurrentPage(p => p - 1)}
            />
            <InputNumber
              min={1}
              max={totalPages}
              value={currentPage}
              onChange={(val) => val && setCurrentPage(val)}
              style={{ width: 60 }}
            />
            <span style={{ padding: '0 8px' }}>/ {totalPages}</span>
            <Button
              icon={<RightOutlined />}
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage(p => p + 1)}
            />
          </Button.Group>

          <Button.Group>
            <Button icon={<ZoomOutOutlined />} onClick={() => setScale(s => Math.max(0.25, s - 0.25))} />
            <span style={{ padding: '0 8px' }}>{Math.round(scale * 100)}%</span>
            <Button icon={<ZoomInOutlined />} onClick={() => setScale(s => Math.min(5, s + 0.25))} />
          </Button.Group>

          <Button icon={<RotateLeftOutlined />} onClick={() => setRotation(r => (r - 90) % 360)} />

          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="pdf-container">
        <canvas ref={canvasRef} />
      </div>
    </div>
  );
};
