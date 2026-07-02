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
          source: 'OPEN_LIBRARY',
          externalId: 'OL82563W',
          mediaType: 'BOOK',
          title: 'Dune',
          originalTitle: null,
          creatorNames: ['Frank Herbert'],
          description: 'Book by Frank Herbert. First published in 1965.',
          releaseYear: 1965,
          coverUrl: 'https://covers.openlibrary.org/b/id/987654-M.jpg',
          sourceUrl: 'https://openlibrary.org/works/OL82563W',
          externalGenres: [],
          externalSubjects: ['Politics', 'Desert planets'],
          suggestedTags: [
            {
              tagId: 'tag-1',
              tagName: 'Politik',
              tagCategory: 'THEME',
              sourceValue: 'Politics',
              reason: 'Mapped from external subject value.',
              confidence: 'HIGH',
            },
          ],
          attribution: 'Metadata from Open Library',
          warnings: [],
        },
        importMessage: 'Imported into your media library.',
        importedMediaId: 'media-1',
      },
    });

    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('Open Library');
    expect(wrapper.text()).toContain('Autor:innen');
    expect(wrapper.text()).toContain('Frank Herbert');
    expect(wrapper.text()).toContain('Politics');
    expect(wrapper.text()).toContain('Politik - HIGH');
    expect(wrapper.text()).toContain('Metadata from Open Library');
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('In Mediathek ansehen');
    expect(wrapper.get('a').attributes('href')).toBe(
      'https://openlibrary.org/works/OL82563W',
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
          creatorNames: [],
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
