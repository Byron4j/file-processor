import { useEffect } from 'react';
import { useFileStore } from '@stores';
import { FileManager } from '@components/FileManager';

export const AllFiles = () => {
  const { fetchFiles, setCurrentFolder } = useFileStore();

  useEffect(() => {
    setCurrentFolder(null);
  }, [setCurrentFolder]);

  return <FileManager title="All Files" />;
};

export const Recent = () => {
  return <FileManager title="Recent Files" filter="recent" />;
};

export const Favorites = () => {
  return <FileManager title="Favorites" filter="favorites" />;
};

export const Trash = () => {
  return <FileManager title="Trash" filter="trash" />;
};
