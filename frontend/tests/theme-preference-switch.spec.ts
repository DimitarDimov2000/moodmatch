import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it } from 'vitest';

import ThemePreferenceSwitch from '@/components/common/ThemePreferenceSwitch.vue';
import { THEME_STORAGE_KEY, initializeTheme } from '@/composables/useTheme';
import { resetMatchMedia, setMatchMediaMatches } from '@/test/setup';

const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';

describe('ThemePreferenceSwitch', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.colorScheme = '';
    resetMatchMedia();
    setMatchMediaMatches(DARK_MODE_QUERY, false);
    initializeTheme();
  });

  it('renders the compact cycling button with the current preference label', () => {
    const wrapper = mount(ThemePreferenceSwitch);
    const button = wrapper.get('[data-testid="theme-switch"]');

    expect(button.text()).toContain('Dunkel');
    expect(button.attributes('aria-label')).toBe('Design: Dunkel. Klickt zu Hell.');
    expect(button.attributes('title')).toBe('Design: Dunkel. Klickt zu Hell.');
  });

  it('cycles dark to light to system and back to dark', async () => {
    const wrapper = mount(ThemePreferenceSwitch);
    const button = wrapper.get('[data-testid="theme-switch"]');

    expect(document.documentElement.dataset.theme).toBe('dark');

    await button.trigger('click');

    expect(button.text()).toContain('Hell');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(button.attributes('aria-label')).toBe('Design: Hell. Klickt zu System.');

    await button.trigger('click');

    expect(button.text()).toContain('System');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('system');
    expect(button.attributes('aria-label')).toBe(
      'Design: System. Folgt Hell. Klickt zu Dunkel.',
    );

    await button.trigger('click');

    expect(button.text()).toContain('Dunkel');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(button.attributes('aria-label')).toBe('Design: Dunkel. Klickt zu Hell.');
  });

  it('hydrates from the existing localStorage-backed theme preference', () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, 'light');
    initializeTheme();

    const wrapper = mount(ThemePreferenceSwitch);
    const button = wrapper.get('[data-testid="theme-switch"]');

    expect(button.text()).toContain('Hell');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(button.attributes('aria-label')).toBe('Design: Hell. Klickt zu System.');
  });
});
