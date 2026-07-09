import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import MediaCard from '@/components/media/MediaCard.vue';

describe('MediaCard', () => {
  it('renders a branded fallback cover and friendly provider label when no cover exists', () => {
    const wrapper = mount(MediaCard, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
          TagChip: {
            props: ['tag'],
            template: '<span>{{ tag.name }}</span>',
          },
        },
      },
      props: {
        media: {
          id: 'media-1',
          title: 'The Left Hand of Darkness',
          originalTitle: null,
          description: 'A landmark science-fiction novel.',
          mediaType: 'BOOK',
          consumptionStatus: 'WANT_TO_CONSUME',
          isFavourite: false,
          rating: null,
          sourceType: 'EXTERNAL_SEARCH',
          sourceNote: null,
          commitmentLevel: 'MEDIUM',
          releaseYear: 1969,
          coverUrl: null,
          externalSourceName: 'OPEN_LIBRARY',
          externalSourceId: 'OL1',
          externalSourceUrl: 'https://openlibrary.org/works/OL1W',
          metadataOrigin: 'IMPORTED',
          tags: [],
          externalReferences: [],
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      },
    });

    expect(wrapper.text()).toContain('The Left Hand of Darkness');
    expect(wrapper.text()).toContain('Open Library');
    expect(wrapper.text()).not.toContain('OPEN_LIBRARY');
    expect(wrapper.text()).toContain('Buch');
    expect(wrapper.get('.media-artwork__fallback-copy').text()).not.toContain('The Left Hand of Darkness');
    expect(wrapper.find('.media-artwork__title').exists()).toBe(false);
    expect(wrapper.get('.media-artwork__fallback-copy').attributes('aria-label')).toBe(
      'Buch Platzhalter für The Left Hand of Darkness',
    );
  });

  it('decodes HTML entities in visible media copy', () => {
    const wrapper = mount(MediaCard, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
          TagChip: {
            props: ['tag'],
            template: '<span>{{ tag.name }}</span>',
          },
        },
      },
      props: {
        media: {
          id: 'media-2',
          title: 'Tusk Ventures CEO on OpenAI proposing a 5% stake to the government: &#39;It makes zero sense&#39;',
          originalTitle: null,
          description: 'Provider copy with &amp; and &#39;quotes&#39; should render cleanly.',
          mediaType: 'VIDEO',
          consumptionStatus: 'WANT_TO_CONSUME',
          isFavourite: false,
          rating: null,
          sourceType: 'EXTERNAL_SEARCH',
          sourceNote: null,
          commitmentLevel: 'SHORT',
          releaseYear: 2026,
          coverUrl: null,
          externalSourceName: 'YOUTUBE',
          externalSourceId: 'yt-1',
          externalSourceUrl: 'https://youtube.com/watch?v=1',
          metadataOrigin: 'IMPORTED',
          tags: [],
          externalReferences: [],
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      },
    });

    expect(wrapper.text()).toContain("government: 'It makes zero sense'");
    expect(wrapper.text()).toContain("copy with & and 'quotes'");
    expect(wrapper.text()).not.toContain('&#39;');
    expect(wrapper.text()).not.toContain('&amp;');
  });

  it('shows the designed fallback while a remote cover is still loading', () => {
    const wrapper = mount(MediaCard, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
          TagChip: {
            props: ['tag'],
            template: '<span>{{ tag.name }}</span>',
          },
        },
      },
      props: {
        media: {
          id: 'media-3',
          title: 'A Long Way to a Small, Angry Planet',
          originalTitle: null,
          description: null,
          mediaType: 'BOOK',
          consumptionStatus: 'WANT_TO_CONSUME',
          isFavourite: false,
          rating: null,
          sourceType: 'EXTERNAL_SEARCH',
          sourceNote: null,
          commitmentLevel: 'MEDIUM',
          releaseYear: 2014,
          coverUrl: 'https://covers.example.test/slow.jpg',
          externalSourceName: 'OPEN_LIBRARY',
          externalSourceId: 'OL2',
          externalSourceUrl: null,
          metadataOrigin: 'IMPORTED',
          tags: [],
          externalReferences: [],
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      },
    });

    expect(wrapper.find('.media-artwork__fallback-copy').exists()).toBe(true);
    expect(wrapper.find('.media-artwork__loading-shell').exists()).toBe(false);
    expect(wrapper.get('.media-artwork__fallback-copy').text()).toContain('Buch');
    expect(wrapper.find('.media-artwork__title').exists()).toBe(false);
  });
});
