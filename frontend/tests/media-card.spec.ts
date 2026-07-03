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
    expect(wrapper.text()).toContain('Cover fehlt');
    expect(wrapper.get('.media-artwork__fallback-copy').attributes('aria-label')).toBe('Buch Platzhalter');
  });
});
