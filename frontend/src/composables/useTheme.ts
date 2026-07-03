import { readonly, ref } from 'vue';

export type ThemePreference = 'system' | 'dark' | 'light';
export type ResolvedTheme = 'dark' | 'light';

export const THEME_STORAGE_KEY = 'moodmatch-theme';

const DARK_MODE_MEDIA_QUERY = '(prefers-color-scheme: dark)';
const DEFAULT_THEME_PREFERENCE: ThemePreference = 'dark';
const DEFAULT_RESOLVED_THEME: ResolvedTheme = 'dark';

const preferenceState = ref<ThemePreference>(DEFAULT_THEME_PREFERENCE);
const resolvedThemeState = ref<ResolvedTheme>(DEFAULT_RESOLVED_THEME);

let systemThemeQuery: MediaQueryList | null = null;
let systemThemeListener: ((event: MediaQueryListEvent) => void) | null = null;

function isThemePreference(value: unknown): value is ThemePreference {
  return value === 'system' || value === 'dark' || value === 'light';
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

function readStoredThemePreference(): ThemePreference | null {
  const storedPreference = getStorage()?.getItem(THEME_STORAGE_KEY);

  if (!storedPreference) {
    return null;
  }

  if (isThemePreference(storedPreference)) {
    return storedPreference;
  }

  try {
    getStorage()?.removeItem(THEME_STORAGE_KEY);
  } catch {
    // Ignore cleanup failures and keep the in-memory fallback.
  }

  return null;
}

function persistThemePreference(preference: ThemePreference) {
  try {
    getStorage()?.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    // Ignore storage failures and keep the current tab theme in memory.
  }
}

function resolveSystemTheme(): ResolvedTheme {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return DEFAULT_RESOLVED_THEME;
  }

  return window.matchMedia(DARK_MODE_MEDIA_QUERY).matches ? 'dark' : 'light';
}

export function resolveThemePreference(
  preference: ThemePreference,
  systemTheme: ResolvedTheme = resolveSystemTheme(),
): ResolvedTheme {
  return preference === 'system' ? systemTheme : preference;
}

function applyResolvedTheme(theme: ResolvedTheme) {
  resolvedThemeState.value = theme;

  if (typeof document === 'undefined') {
    return;
  }

  const root = document.documentElement;
  root.dataset.theme = theme;
  root.style.colorScheme = theme;
}

function unsubscribeFromSystemTheme() {
  if (!systemThemeQuery || !systemThemeListener) {
    systemThemeQuery = null;
    systemThemeListener = null;
    return;
  }

  if (typeof systemThemeQuery.removeEventListener === 'function') {
    systemThemeQuery.removeEventListener('change', systemThemeListener);
  } else {
    systemThemeQuery.removeListener?.(systemThemeListener);
  }

  systemThemeQuery = null;
  systemThemeListener = null;
}

function subscribeToSystemTheme() {
  if (
    typeof window === 'undefined' ||
    typeof window.matchMedia !== 'function' ||
    preferenceState.value !== 'system'
  ) {
    unsubscribeFromSystemTheme();
    return;
  }

  const nextQuery = window.matchMedia(DARK_MODE_MEDIA_QUERY);

  if (systemThemeQuery === nextQuery && systemThemeListener) {
    return;
  }

  unsubscribeFromSystemTheme();
  systemThemeQuery = nextQuery;
  systemThemeListener = (event: MediaQueryListEvent) => {
    if (preferenceState.value !== 'system') {
      return;
    }

    applyResolvedTheme(event.matches ? 'dark' : 'light');
  };

  if (typeof systemThemeQuery.addEventListener === 'function') {
    systemThemeQuery.addEventListener('change', systemThemeListener);
  } else {
    systemThemeQuery.addListener?.(systemThemeListener);
  }
}

function syncResolvedTheme() {
  applyResolvedTheme(resolveThemePreference(preferenceState.value));

  if (preferenceState.value === 'system') {
    subscribeToSystemTheme();
    return;
  }

  unsubscribeFromSystemTheme();
}

export function initializeTheme() {
  preferenceState.value = readStoredThemePreference() ?? DEFAULT_THEME_PREFERENCE;
  syncResolvedTheme();
}

export function useTheme() {
  return {
    preference: readonly(preferenceState),
    resolvedTheme: readonly(resolvedThemeState),
    initializeTheme,
    setThemePreference(preference: ThemePreference) {
      preferenceState.value = preference;
      persistThemePreference(preference);
      syncResolvedTheme();
    },
  };
}
