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
    expect(wrapper.text()).toContain('Hörbuch');
    expect(wrapper.text()).toContain('Bereits vorhanden');
    expect(wrapper.text()).toContain('Autor:in & Stimme');
    expect(wrapper.text()).toContain('Author: Jane Austen');
    expect(wrapper.text()).toContain('Reader: Annie Coleman Rothenberg');
    expect(wrapper.text()).toContain('Romance');
    expect(wrapper.text()).toContain('English');
    expect(wrapper.text()).toContain('Provider');
    expect(wrapper.text()).toContain('Romantik');
    expect(wrapper.text()).not.toContain('HIGH');
    expect(wrapper.text()).not.toContain('Noch keine passenden Tag-Vorschlaege vorhanden.');
    expect(wrapper.text()).toContain('LibriVox public domain audiobook catalog');
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('Details');
    expect(wrapper.get('a').attributes('href')).toBe(
      'https://librivox.org/pride-and-prejudice-by-jane-austen/',
    );
    expect(wrapper.get('img').attributes('loading')).toBe('lazy');
    expect(wrapper.get('img').attributes('decoding')).toBe('async');
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

  it('renders suggested fallback tags when provided by the backend', () => {
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
          source: 'RAWG',
          externalId: '3498',
          mediaType: 'GAME',
          title: 'Elden Ring',
          originalTitle: null,
          creatorNames: [],
          description: null,
          releaseYear: 2022,
          coverUrl: null,
          sourceUrl: 'https://rawg.io/games/elden-ring',
          externalGenres: ['Action', 'RPG'],
          externalSubjects: ['Open World'],
          suggestedTags: [
            {
              tagId: 'fallback-action',
              tagName: 'Action',
              tagCategory: 'GENRE',
              sourceValue: 'Action',
              reason: 'Suggested from normalized external metadata.',
              confidence: 'LOW',
            },
            {
              tagId: 'fallback-open-world',
              tagName: 'Open World',
              tagCategory: 'THEME',
              sourceValue: 'Open World',
              reason: 'Suggested from normalized external metadata.',
              confidence: 'LOW',
            },
          ],
          attribution: 'Metadata from RAWG',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Action');
    expect(wrapper.text()).toContain('Open World');
    expect(wrapper.text()).not.toContain('LOW');
    expect(wrapper.text()).not.toContain('MEDIUM');
    expect(wrapper.text()).not.toContain('HIGH');
    expect(wrapper.text()).not.toContain('Fuer dieses Ergebnis liegen noch keine gemappten Tag-Vorschlaege vor.');
  });

  it('renders the empty suggested-tag message only when no suggestions exist', () => {
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
          title: 'Sparse Result',
          originalTitle: null,
          creatorNames: [],
          description: null,
          releaseYear: null,
          coverUrl: null,
          sourceUrl: null,
          externalGenres: [],
          externalSubjects: [],
          suggestedTags: [],
          attribution: 'Metadata from TMDB',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Noch keine passenden Tag-Vorschläge vorhanden.');
  });

  it('renders RAWG game metadata clearly', () => {
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
          source: 'RAWG',
          externalId: '3498',
          mediaType: 'GAME',
          title: 'Elden Ring',
          originalTitle: null,
          creatorNames: ['Developer: FromSoftware', 'Publisher: Bandai Namco Entertainment'],
          description: 'Rise, Tarnished, and be guided by grace.',
          releaseYear: 2022,
          coverUrl: 'https://media.rawg.io/media/games/elden-ring.jpg',
          sourceUrl: 'https://rawg.io/games/elden-ring',
          externalGenres: ['Action', 'RPG'],
          externalSubjects: ['PC', 'PlayStation 5', 'Open World'],
          suggestedTags: [],
          attribution: 'Metadata from RAWG. View source on RAWG for full provider details.',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Elden Ring');
    expect(wrapper.text()).toContain('RAWG');
    expect(wrapper.text()).toContain('Spiel');
    expect(wrapper.text()).toContain('2022');
    expect(wrapper.text()).toContain('Studio / Publisher');
    expect(wrapper.text()).toContain('Developer: FromSoftware');
    expect(wrapper.text()).toContain('Action');
    expect(wrapper.text()).toContain('Hinweise');
    expect(wrapper.text()).toContain('PlayStation 5');
    expect(wrapper.get('img').attributes('src')).toBe('https://media.rawg.io/media/games/elden-ring.jpg');
  });

  it('renders AniList anime movie metadata as a film result', () => {
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
          source: 'ANILIST',
          externalId: '199',
          mediaType: 'FILM',
          title: 'Sen to Chihiro no Kamikakushi',
          originalTitle: '千と千尋の神隠し',
          creatorNames: ['Studio Ghibli'],
          description: 'A young girl enters a world of spirits.',
          releaseYear: 2001,
          coverUrl: 'https://img.anilist.co/spirited-away.jpg',
          sourceUrl: 'https://anilist.co/anime/199',
          externalGenres: ['Adventure', 'Fantasy'],
          externalSubjects: ['Format: MOVIE', 'Status: FINISHED'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Sen to Chihiro no Kamikakushi');
    expect(wrapper.text()).toContain('AniList');
    expect(wrapper.text()).toContain('Anime-Film');
    expect(wrapper.text()).toContain('2001');
    expect(wrapper.text()).toContain('Film');
    expect(wrapper.text()).toContain('Originaltitel: 千と千尋の神隠し');
    expect(wrapper.text()).toContain('Studio');
    expect(wrapper.text()).toContain('Studio Ghibli');
    expect(wrapper.text()).toContain('Hinweise');
    expect(wrapper.text()).toContain('Status: Abgeschlossen');
  });

  it('renders AniList manga metadata as a book result', () => {
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
          source: 'ANILIST',
          externalId: '30002',
          mediaType: 'BOOK',
          title: 'Berserk',
          originalTitle: 'ベルセルク',
          creatorNames: ['Kentaro Miura'],
          description: 'A dark fantasy manga.',
          releaseYear: 1989,
          coverUrl: 'https://img.anilist.co/berserk.jpg',
          sourceUrl: 'https://anilist.co/manga/30002',
          externalGenres: ['Action', 'Fantasy'],
          externalSubjects: ['Format: MANGA', 'Status: RELEASING'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Berserk');
    expect(wrapper.text()).toContain('Manga');
    expect(wrapper.text()).toContain('1989');
    expect(wrapper.text()).toContain('Autor:innen');
    expect(wrapper.text()).toContain('Kentaro Miura');
    expect(wrapper.text()).toContain('Status: Laufend');
  });

  it('renders youtube video metadata with a landscape thumbnail', () => {
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
          source: 'YOUTUBE',
          externalId: 'abc123XYZ_0',
          mediaType: 'VIDEO',
          title: 'AI Tutorial for Builders',
          originalTitle: null,
          creatorNames: ['MoodMatch Dev'],
          description: 'Build better search imports with the official YouTube API.',
          releaseYear: 2024,
          coverUrl: 'https://img.youtube.test/high.jpg',
          sourceUrl: 'https://www.youtube.com/watch?v=abc123XYZ_0',
          externalGenres: [],
          externalSubjects: ['Channel: MoodMatch Dev'],
          suggestedTags: [],
          attribution: 'Metadata from YouTube',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('AI Tutorial for Builders');
    expect(wrapper.text()).toContain('YouTube');
    expect(wrapper.text()).toContain('Video');
    expect(wrapper.text()).toContain('Kanal');
    expect(wrapper.text()).toContain('MoodMatch Dev');
    expect(wrapper.get('img').attributes('src')).toBe('https://img.youtube.test/high.jpg');
    expect(wrapper.find('.media-artwork--landscape').exists()).toBe(true);
  });

  it('renders Podcast Index podcast metadata clearly', () => {
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
          source: 'PODCAST_INDEX',
          externalId: '75075',
          mediaType: 'PODCAST',
          title: 'Lex Fridman Podcast',
          originalTitle: null,
          creatorNames: ['Lex Fridman'],
          description:
            'Conversations about science, technology, history, philosophy, and the nature of intelligence.',
          releaseYear: 2024,
          coverUrl: 'https://image.simplecastcdn.com/images/lex-fridman.jpg',
          sourceUrl: 'https://lexfridman.com/podcast/',
          externalGenres: ['Technology', 'Science'],
          externalSubjects: ['Language: en', 'Explicit: No', 'Feed type: podcast'],
          suggestedTags: [],
          attribution: 'Metadata from Podcast Index',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Lex Fridman Podcast');
    expect(wrapper.text()).toContain('Podcast Index');
    expect(wrapper.text()).toContain('Podcast-Show');
    expect(wrapper.text()).toContain('2024');
    expect(wrapper.text()).toContain('Host / Creator');
    expect(wrapper.text()).toContain('Lex Fridman');
    expect(wrapper.text()).toContain('Technology');
    expect(wrapper.text()).toContain('Science');
    expect(wrapper.text()).toContain('Hinweise');
    expect(wrapper.text()).toContain('Sprache: EN');
    expect(wrapper.text()).toContain('Explizit: Nein');
    expect(wrapper.text()).toContain('Provider');
    expect(wrapper.get('img').attributes('src')).toBe(
      'https://image.simplecastcdn.com/images/lex-fridman.jpg',
    );
  });

  it('renders YouTube video metadata clearly', () => {
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
          source: 'YOUTUBE',
          externalId: 'abc123XYZ_0',
          mediaType: 'VIDEO',
          title: 'VueConf 2024 Keynote',
          originalTitle: null,
          creatorNames: ['MoodMatch Dev'],
          description: 'A practical keynote about resilient frontend systems.',
          releaseYear: 2024,
          coverUrl: 'https://img.youtube.test/maxres.jpg',
          sourceUrl: 'https://www.youtube.com/watch?v=abc123XYZ_0',
          externalGenres: ['Education'],
          externalSubjects: ['Vue 3', 'Tutorial', 'Channel: MoodMatch Dev', 'Category: Education'],
          suggestedTags: [],
          attribution: 'Metadata from YouTube',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('VueConf 2024 Keynote');
    expect(wrapper.text()).toContain('YouTube');
    expect(wrapper.text()).toContain('Video');
    expect(wrapper.text()).toContain('2024');
    expect(wrapper.text()).toContain('Kanal');
    expect(wrapper.text()).toContain('MoodMatch Dev');
    expect(wrapper.text()).toContain('Education');
    expect(wrapper.text()).toContain('Kategorie: Education');
    expect(wrapper.get('img').attributes('src')).toBe('https://img.youtube.test/maxres.jpg');
  });

  it('renders media-specific cover fallbacks, trimmed descriptions, and warnings', () => {
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
          externalId: 'OL12345W',
          mediaType: 'BOOK',
          title: 'Very Long Book',
          originalTitle: null,
          creatorNames: ['Author'],
          description: 'A'.repeat(320),
          releaseYear: 2020,
          coverUrl: null,
          sourceUrl: 'https://openlibrary.org/works/OL12345W',
          externalGenres: ['Fantasy'],
          externalSubjects: ['Dark fantasy'],
          suggestedTags: [],
          attribution: 'Metadata from Open Library',
          warnings: ['Cover unavailable from provider.'],
        },
      },
    });

    expect(wrapper.text()).toContain('Buch');
    expect(wrapper.text()).toContain('Cover fehlt');
    expect(wrapper.get('.media-artwork__fallback-copy').attributes('aria-label')).toBe(
      'Buch Platzhalter',
    );
    expect(wrapper.get('.external-result-card__description').text()).toContain('…');
    expect(wrapper.text()).toContain('Hinweis');
    expect(wrapper.text()).toContain('Cover unavailable from provider.');
  });
});
