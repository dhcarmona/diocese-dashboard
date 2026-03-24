import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import en from './locales/en.json';
import es from './locales/es.json';

// Guard allows tests to initialize the singleton first with a fixed language.
if (!i18n.isInitialized) {
  void i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources: { en: { translation: en }, es: { translation: es } },
      fallbackLng: 'es',
      // Only consult localStorage so that first-time visitors get Spanish
      // (the fallback) rather than inheriting their browser's locale.
      detection: {
        order: ['localStorage'],
        caches: ['localStorage'],
      },
      interpolation: { escapeValue: false },
    });
}

export default i18n;
