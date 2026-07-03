import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it } from 'vitest';

import LanguagePreferenceSwitch from '@/components/common/LanguagePreferenceSwitch.vue';
import { initializeI18n, LANGUAGE_STORAGE_KEY, setAppLocale } from '@/i18n';

describe('LanguagePreferenceSwitch', () => {
  beforeEach(() => {
    window.localStorage.clear();
    setAppLocale('de');
  });

  it('renders the current locale label', () => {
    const wrapper = mount(LanguagePreferenceSwitch);
    const button = wrapper.get('[data-testid="language-switch"]');

    expect(button.text()).toContain('DE');
    expect(button.attributes('aria-label')).toBe('Sprache: Deutsch. Zu Englisch wechseln.');
  });

  it('cycles DE to EN and back to DE', async () => {
    const wrapper = mount(LanguagePreferenceSwitch);
    const button = wrapper.get('[data-testid="language-switch"]');

    await button.trigger('click');

    expect(button.text()).toContain('EN');
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en');
    expect(document.documentElement.lang).toBe('en');

    await button.trigger('click');

    expect(button.text()).toContain('DE');
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('de');
    expect(document.documentElement.lang).toBe('de');
  });

  it('hydrates the stored locale on a fresh render', () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'en');
    initializeI18n();

    const wrapper = mount(LanguagePreferenceSwitch);

    expect(wrapper.get('[data-testid="language-switch"]').text()).toContain('EN');
  });
});
