import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { ThemeMode } from '@types';

interface ThemeState {
  mode: ThemeMode;
  isDark: boolean;
  setMode: (mode: ThemeMode) => void;
  toggleTheme: () => void;
}

const getSystemTheme = (): boolean => {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
};

const isDarkMode = (mode: ThemeMode): boolean => {
  if (mode === 'dark') return true;
  if (mode === 'light') return false;
  return getSystemTheme();
};

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      mode: 'light',
      isDark: false,

      setMode: (mode) => {
        const dark = isDarkMode(mode);
        set({ mode, isDark: dark });
        updateHtmlClass(dark);
      },

      toggleTheme: () => {
        const { mode } = get();
        const newMode = mode === 'light' ? 'dark' : 'light';
        const dark = isDarkMode(newMode);
        set({ mode: newMode, isDark: dark });
        updateHtmlClass(dark);
      },
    }),
    {
      name: 'theme-storage',
    }
  )
);

// Update HTML class for Tailwind/Ant Design dark mode
const updateHtmlClass = (isDark: boolean) => {
  if (typeof document === 'undefined') return;

  const html = document.documentElement;
  if (isDark) {
    html.classList.add('dark');
    html.setAttribute('data-theme', 'dark');
  } else {
    html.classList.remove('dark');
    html.setAttribute('data-theme', 'light');
  }
};

// Initialize theme on app load
export const initTheme = () => {
  const stored = localStorage.getItem('theme-storage');
  if (stored) {
    try {
      const { state } = JSON.parse(stored);
      const dark = isDarkMode(state.mode);
      updateHtmlClass(dark);
    } catch (error) {
      console.error('Failed to parse theme storage:', error);
    }
  }

  // Listen for system theme changes
  if (typeof window !== 'undefined') {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', (e) => {
      const { mode } = useThemeStore.getState();
      if (mode === 'auto') {
        const dark = e.matches;
        useThemeStore.setState({ isDark: dark });
        updateHtmlClass(dark);
      }
    });
  }
};
