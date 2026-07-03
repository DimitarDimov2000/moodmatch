import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import BrandMark from '@/components/brand/BrandMark.vue';

describe('BrandMark', () => {
  it('renders the exact dark export by default', () => {
    const wrapper = mount(BrandMark);

    const image = wrapper.get('[data-testid="brand-mark"]');

    expect(image.element.tagName).toBe('IMG');
    expect(image.attributes('src')).toContain('moodmatch_logo_dark.svg');
    expect(image.attributes('alt')).toBe('');
  });

  it('switches to the exact light export when requested', () => {
    const wrapper = mount(BrandMark, {
      props: {
        theme: 'light',
      },
    });

    expect(wrapper.get('[data-testid="brand-mark"]').attributes('src')).toContain('moodmatch_icon_white.svg');
  });

  it('supports a custom test id for secondary lockups', () => {
    const wrapper = mount(BrandMark, {
      props: {
        testId: 'login-brand-mark',
      },
    });

    expect(wrapper.get('[data-testid="login-brand-mark"]').attributes('data-testid')).toBe('login-brand-mark');
  });
});
