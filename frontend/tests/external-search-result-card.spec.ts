import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';

describe('ExternalSearchResultCard', () => {
  it('renders normalized audiobook result fields and import actions', async () => {
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
          source: 'LIBRIVOX',
          externalId: '253',
          mediaType: 'AUDIOBOOK',
          title: 'Pride and Prejudice',
          originalTitle: null,
          creatorNames: ['Author: Jane Austen', 'Reader: Annie Coleman Rothenberg'],
          description: 'Jane Austen classic narrated for the public domain catalog.',
          releaseYear: 1813,
          coverUrl: 'https://archive.org/covers/pride.jpg',
          sourceUrl: 'https://librivox.org/pride-and-prejudice-by-jane-austen/',
          externalGenres: ['Romance'],
          externalSubjects: ['English'],
          suggestedTags: [
            {
              tagId: 'tag-1',
              tagName: 'Romantik',
              tagCategory: 'GENRE',
              sourceValue: 'Romance',
              reason: 'Mapped from external genre value.',
              confidence: 'HIGH',
            },
          ],
          attribution: 'LibriVox public domain audiobook catalog',
          warnings: [],
        },
        importMessage: 'Imported into your media library.',
        importedMediaId: 'media-1',
      },
    });

    expect(wrapper.text()).toContain('Pride and Prejudice');
    expect(wrapper.text()).toContain('LibriVox');
    expect(wrapper.text()).toContain('Autor:in / Sprecher:in');
    expect(wrapper.text()).toContain('Author: Jane Austen');
    expect(wrapper.text()).toContain('Reader: Annie Coleman Rothenberg');
    expect(wrapper.text()).toContain('Romance');
    expect(wrapper.text()).toContain('English');
    expect(wrapper.text()).toContain('Quelle');
    expect(wrapper.text()).toContain('LibriVox Audiobook Catalog');
    expect(wrapper.text()).toContain('Romantik - HIGH');
    expect(wrapper.text()).toContain('LibriVox public domain audiobook catalog');
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('In Mediathek ansehen');
    expect(wrapper.get('a').attributes('href')).toBe(
      'https://librivox.org/pride-and-prejudice-by-jane-austen/',
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
