import { beforeEach, describe, expect, it } from 'vitest';

import {
  getCurrentLocale,
  i18n,
  initializeI18n,
  LANGUAGE_STORAGE_KEY,
  setAppLocale,
} from '@/i18n';

describe('i18n locale state', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.lang = '';
    setAppLocale('de');
  });

  it('defaults to de when nothing is stored', () => {
    initializeI18n();

    expect(getCurrentLocale()).toBe('de');
    expect(document.documentElement.lang).toBe('de');
  });

  it('hydrates a stored locale', () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'en');

    initializeI18n();

    expect(getCurrentLocale()).toBe('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('cleans invalid stored locale values', () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'fr');

    initializeI18n();

    expect(getCurrentLocale()).toBe('de');
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBeNull();
  });

  it('persists locale changes and updates html lang', () => {
    setAppLocale('en');

    expect(getCurrentLocale()).toBe('en');
    expect(document.documentElement.lang).toBe('en');
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en');
  });

  it('updates document title translations through the active locale', () => {
    setAppLocale('en');

    expect(i18n.global.t('routes.matches.title')).toBe('Matches');

    setAppLocale('de');

    expect(i18n.global.t('routes.matches.title')).toBe('Matches');
    expect(i18n.global.t('routes.media.title')).toBe('Mediathek');
  });
});
