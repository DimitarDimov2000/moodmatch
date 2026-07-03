import { beforeEach, describe, expect, it, vi } from 'vitest';

import { resetMatchMedia, setMatchMediaMatches } from '@/test/setup';

const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';

async function importThemeModule() {
  vi.resetModules();
  return import('@/composables/useTheme');
}

describe('useTheme', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.colorScheme = '';
    resetMatchMedia();
  });

  it('defaults to dark on first visit when no preference is stored', async () => {
    const { initializeTheme, useTheme } = await importThemeModule();

    initializeTheme();

    const theme = useTheme();

    expect(theme.preference.value).toBe('dark');
    expect(theme.resolvedTheme.value).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(document.documentElement.style.colorScheme).toBe('dark');
  });

  it('resolves the explicit system preference with prefers-color-scheme', async () => {
    const { THEME_STORAGE_KEY, initializeTheme, useTheme } = await importThemeModule();

    window.localStorage.setItem(THEME_STORAGE_KEY, 'system');
    setMatchMediaMatches(DARK_MODE_QUERY, true);
    initializeTheme();

    const theme = useTheme();

    expect(theme.preference.value).toBe('system');
    expect(theme.resolvedTheme.value).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('sets and persists dark mode explicitly', async () => {
    const { THEME_STORAGE_KEY, initializeTheme, useTheme } = await importThemeModule();

    initializeTheme();

    const theme = useTheme();
    theme.setThemePreference('dark');

    expect(theme.preference.value).toBe('dark');
    expect(theme.resolvedTheme.value).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });

  it('sets and persists light mode explicitly', async () => {
    const { THEME_STORAGE_KEY, initializeTheme, useTheme } = await importThemeModule();

    initializeTheme();

    const theme = useTheme();
    theme.setThemePreference('light');

    expect(theme.preference.value).toBe('light');
    expect(theme.resolvedTheme.value).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(document.documentElement.style.colorScheme).toBe('light');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });

  it('reacts to system theme changes while in system mode', async () => {
    const { THEME_STORAGE_KEY, initializeTheme, useTheme } = await importThemeModule();

    window.localStorage.setItem(THEME_STORAGE_KEY, 'system');
    setMatchMediaMatches(DARK_MODE_QUERY, true);
    initializeTheme();

    const theme = useTheme();
    expect(theme.resolvedTheme.value).toBe('dark');

    setMatchMediaMatches(DARK_MODE_QUERY, false);

    expect(theme.preference.value).toBe('system');
    expect(theme.resolvedTheme.value).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(document.documentElement.style.colorScheme).toBe('light');
  });
});
