import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Import translations
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';
import jaJP from './locales/ja-JP.json';
import koKR from './locales/ko-KR.json';

const resources = {
  'zh-CN': { translation: zhCN },
  'en-US': { translation: enUS },
  'ja-JP': { translation: jaJP },
  'ko-KR': { translation: koKR },
};

// Get stored language or default
const getStoredLanguage = (): string => {
  if (typeof window === 'undefined') return 'zh-CN';
  return localStorage.getItem('i18nextLng') || navigator.language || 'zh-CN';
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: getStoredLanguage(),
    fallbackLng: 'zh-CN',
    supportedLngs: ['zh-CN', 'en-US', 'ja-JP', 'ko-KR'],
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
