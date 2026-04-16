import { useEffect, useRef, useState } from 'react';
import videojs from 'video.js';
import 'video.js/dist/video-js.css';
import { Button, Space } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { previewApi } from '@api';

interface VideoPreviewProps {
  fileId: string;
  downloadUrl: string;
}

export const VideoPreview = ({ fileId, downloadUrl }: VideoPreviewProps) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerRef = useRef<any>(null);

  useEffect(() => {
    if (!videoRef.current) return;

    const videoUrl = previewApi.getVideoUrl(fileId);

    playerRef.current = videojs(videoRef.current, {
      controls: true,
      fluid: true,
      html5: {
        vhs: {
          overrideNative: true,
        },
      },
      sources: [{
        src: videoUrl,
        type: 'video/mp4',
      }],
    });

    return () => {
      if (playerRef.current) {
        playerRef.current.dispose();
        playerRef.current = null;
      }
    };
  }, [fileId]);

  return (
    <div className="video-preview">
      <div className="video-toolbar">
        <Space>
          <Button icon={<DownloadOutlined />} href={downloadUrl}>
            Download
          </Button>
        </Space>
      </div>

      <div className="video-container">
        <video
          ref={videoRef}
          className="video-js vjs-big-play-centered"
          controls
          preload="auto"
        />
      </div>
    </div>
  );
};
