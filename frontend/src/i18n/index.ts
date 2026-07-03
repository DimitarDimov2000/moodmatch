import { computed, readonly, ref, watch } from 'vue';
import { createI18n } from 'vue-i18n';

import { deMessages } from './messages/de';
import { enMessages } from './messages/en';

export const LANGUAGE_STORAGE_KEY = 'moodmatch-language';
export const supportedLocales = ['de', 'en'] as const;

export type AppLocale = (typeof supportedLocales)[number];

const DEFAULT_LOCALE: AppLocale = 'de';
const FALLBACK_LOCALE: AppLocale = 'en';

const localeState = ref<AppLocale>(DEFAULT_LOCALE);

export const i18n = createI18n({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: FALLBACK_LOCALE,
  messages: {
    de: deMessages,
    en: enMessages,
  },
});

function isSupportedLocale(value: unknown): value is AppLocale {
  return typeof value === 'string' && supportedLocales.includes(value as AppLocale);
}

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function persistLocale(locale: AppLocale) {
  try {
    getStorage()?.setItem(LANGUAGE_STORAGE_KEY, locale);
  } catch {
    // Ignore storage failures and keep the in-memory locale.
  }
}

function clearStoredLocale() {
  try {
    getStorage()?.removeItem(LANGUAGE_STORAGE_KEY);
  } catch {
    // Ignore storage cleanup failures.
  }
}

export function readStoredLocale(): AppLocale | null {
  const storedLocale = getStorage()?.getItem(LANGUAGE_STORAGE_KEY);

  if (!storedLocale) {
    return null;
  }

  if (isSupportedLocale(storedLocale)) {
    return storedLocale;
  }

  clearStoredLocale();
  return null;
}

function applyLocale(locale: AppLocale, persist = true) {
  localeState.value = locale;
  i18n.global.locale.value = locale;

  if (typeof document !== 'undefined') {
    document.documentElement.lang = locale;
  }

  if (persist) {
    persistLocale(locale);
  }
}

export function initializeI18n() {
  applyLocale(readStoredLocale() ?? DEFAULT_LOCALE, false);

  watch(
    localeState,
    (locale) => {
      if (typeof document !== 'undefined') {
        document.documentElement.lang = locale;
      }
    },
    { immediate: true },
  );
}

export function setAppLocale(locale: AppLocale) {
  applyLocale(locale);
}

export function getCurrentLocale(): AppLocale {
  return localeState.value;
}

export function getIntlLocale(locale: AppLocale = getCurrentLocale()): string {
  return locale === 'de' ? 'de-DE' : 'en-US';
}

export function useLocale() {
  const nextLocale = computed<AppLocale>(() => (localeState.value === 'de' ? 'en' : 'de'));

  return {
    locale: readonly(localeState),
    nextLocale,
    setLocale: setAppLocale,
  };
}
