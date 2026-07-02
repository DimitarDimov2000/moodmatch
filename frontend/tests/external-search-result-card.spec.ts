import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';

describe('ExternalSearchResultCard', () => {
  it('renders normalized result fields, warnings, and import actions', async () => {
    const wrapper = mount(ExternalSearchResultCard, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
      props: {
        result: {
          source: 'DEMO',
          externalId: 'demo-book-dune',
          mediaType: 'BOOK',
          title: 'Dune',
          originalTitle: null,
          description: 'A sprawling desert saga.',
          releaseYear: 1965,
          coverUrl: 'https://demo.moodmatch.local/covers/dune.jpg',
          sourceUrl: 'https://demo.moodmatch.local/items/demo-book-dune',
          externalGenres: ['Science-Fiction', 'Adventure'],
          externalSubjects: ['Macht', 'Ueberleben'],
          suggestedTags: [
            {
              tagId: 'tag-1',
              tagName: 'Entdeckung',
              tagCategory: 'THEME',
              sourceValue: 'Macht',
              reason: 'Mapped from external subject value.',
              confidence: 'HIGH',
            },
          ],
          attribution: 'MoodMatch Demo Provider (offline)',
          warnings: ['Demo fallback active.'],
        },
        importMessage: 'Imported into your media library.',
        importedMediaId: 'media-1',
      },
    });

    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('DEMO');
    expect(wrapper.text()).toContain('Science-Fiction');
    expect(wrapper.text()).toContain('Macht');
    expect(wrapper.text()).toContain('Entdeckung - HIGH');
    expect(wrapper.text()).toContain('MoodMatch Demo Provider (offline)');
    expect(wrapper.text()).toContain('Demo fallback active.');
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('In Mediathek ansehen');
    expect(wrapper.get('a').attributes('href')).toBe(
      'https://demo.moodmatch.local/items/demo-book-dune',
    );
  });

  it('emits an import event when the import button is clicked', async () => {
    const wrapper = mount(ExternalSearchResultCard, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
      props: {
        result: {
          source: 'TMDB',
          externalId: '11',
          mediaType: 'FILM',
          title: 'Arrival',
          originalTitle: null,
          description: 'First contact changes everything.',
          releaseYear: 2016,
          coverUrl: null,
          sourceUrl: 'https://www.themoviedb.org/movie/11',
          externalGenres: ['Science Fiction'],
          externalSubjects: [],
          suggestedTags: [],
          attribution: 'Metadata from TMDB',
          warnings: [],
        },
      },
    });

    await wrapper.get('button.button--primary').trigger('click');

    expect(wrapper.emitted('import')).toHaveLength(1);
  });
});
