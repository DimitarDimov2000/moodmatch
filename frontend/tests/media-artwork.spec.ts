import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import MediaArtwork from '@/components/media/MediaArtwork.vue';

describe('MediaArtwork', () => {
  it('renders the real image when a usable artwork URL exists', () => {
    const wrapper = mount(MediaArtwork, {
      props: {
        title: 'Arrival',
        mediaType: 'FILM',
        coverUrl: 'https://images.example.test/arrival.jpg',
      },
    });

    const image = wrapper.get('img');

    expect(image.attributes('src')).toBe('https://images.example.test/arrival.jpg');
    expect(image.attributes('alt')).toBe('Cover von Arrival');
    expect(wrapper.find('.media-artwork__fallback-copy').exists()).toBe(true);
  });

  it('renders the fallback immediately when artwork is missing, empty, or whitespace', async () => {
    const wrapper = mount(MediaArtwork, {
      props: {
        title: 'Station Eleven',
        mediaType: 'SERIES',
        coverUrl: null,
      },
    });

    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.get('.media-artwork__fallback-copy').attributes('aria-label')).toBe(
      'Serie Platzhalter für Station Eleven',
    );

    await wrapper.setProps({ coverUrl: '' });
    expect(wrapper.find('img').exists()).toBe(false);

    await wrapper.setProps({ coverUrl: '   ' });
    expect(wrapper.find('img').exists()).toBe(false);
  });

  it('switches to the fallback when image loading fails and resets when the URL changes', async () => {
    const wrapper = mount(MediaArtwork, {
      props: {
        title: 'Piranesi',
        mediaType: 'BOOK',
        coverUrl: 'https://images.example.test/piranesi-a.jpg',
      },
    });

    await wrapper.get('img').trigger('error');

    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.get('.media-artwork__fallback-copy').attributes('aria-label')).toBe(
      'Buch Platzhalter für Piranesi',
    );

    await wrapper.setProps({ coverUrl: 'https://images.example.test/piranesi-b.jpg' });

    expect(wrapper.get('img').attributes('src')).toBe('https://images.example.test/piranesi-b.jpg');
  });

  it.each([
    ['FILM', 'Film'],
    ['SERIES', 'Serie'],
    ['BOOK', 'Buch'],
    ['GAME', 'Spiel'],
    ['AUDIOBOOK', 'Hörbuch'],
    ['PODCAST', 'Podcast'],
    ['VIDEO', 'Video'],
  ] as const)('renders the localized fallback label for %s', (mediaType, label) => {
    const wrapper = mount(MediaArtwork, {
      props: {
        title: 'Fallback Title',
        mediaType,
        coverUrl: null,
      },
    });

    expect(wrapper.get('.media-artwork__label').text()).toBe(label);
  });

  it('can hide the fallback title for card and swipe-style usage', () => {
    const wrapper = mount(MediaArtwork, {
      props: {
        title: 'Hidden Duplicate Title',
        mediaType: 'VIDEO',
        coverUrl: null,
        variant: 'hero',
        showFallbackTitle: false,
      },
    });

    expect(wrapper.find('.media-artwork__title').exists()).toBe(false);
    expect(wrapper.get('.media-artwork__label').text()).toBe('Video');
    expect(wrapper.find('.media-artwork__hint').exists()).toBe(false);
  });
});
