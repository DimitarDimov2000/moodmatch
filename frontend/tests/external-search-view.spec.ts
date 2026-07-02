import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiRequestError } from '@/api/client';
import ExternalSearchView from '@/views/ExternalSearchView.vue';

const { searchExternalMock, importExternalMediaMock, resolveExternalUrlMock } = vi.hoisted(() => ({
  searchExternalMock: vi.fn(),
  importExternalMediaMock: vi.fn(),
  resolveExternalUrlMock: vi.fn(),
}));

vi.mock('@/api/external', () => ({
  searchExternal: searchExternalMock,
  importExternalMedia: importExternalMediaMock,
  resolveExternalUrl: resolveExternalUrlMock,
}));

describe('ExternalSearchView', () => {
  beforeEach(() => {
    searchExternalMock.mockReset();
    importExternalMediaMock.mockReset();
    resolveExternalUrlMock.mockReset();
  });

  function mountView() {
    return mount(ExternalSearchView, {
      global: {
        stubs: {
          AppMessage: false,
          ExternalSearchForm: false,
          ExternalSearchResultCard: false,
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    });
  }

  function getImportButton(wrapper: ReturnType<typeof mount>) {
    return wrapper
      .findAll('button')
      .find((candidate) => candidate.text().includes('In Mediathek importieren'));
  }

  it('enables the audiobook LibriVox option without introducing music UI', async () => {
    const wrapper = mountView();

    expect(wrapper.text()).toContain('Externe Medien suchen und importieren');
    expect(wrapper.text()).toContain('Noch keine Suche gestartet');

    await wrapper.get('select[name="mediaType"]').setValue('AUDIOBOOK');

    expect(wrapper.text()).toContain('LibriVox (Hoerbuecher)');
    expect(wrapper.get('option[value="LIBRIVOX"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.text()).not.toContain('Spotify');
  });

  it('enables the game RAWG option without introducing music UI', async () => {
    const wrapper = mountView();

    await wrapper.get('select[name="mediaType"]').setValue('GAME');

    expect(wrapper.text()).toContain('RAWG (Games)');
    expect(wrapper.get('option[value="RAWG"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.text()).not.toContain('Spotify');
    expect(wrapper.text()).not.toContain('Music');
  });

  it('enables Podcast Index for podcasts without introducing music UI', async () => {
    const wrapper = mountView();

    await wrapper.get('select[name="mediaType"]').setValue('PODCAST');

    expect(wrapper.text()).toContain('Automatisch (Podcast Index)');
    expect(wrapper.text()).toContain('Podcast Index (Podcast-Shows)');
    expect(wrapper.text()).toContain('Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: Podcast Index');
    expect(wrapper.get('option[value="PODCAST_INDEX"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.text()).not.toContain('Spotify');
    expect(wrapper.text()).not.toContain('Music');
  });

  it('enables normal youtube query search for videos while keeping the dedicated url import lane', async () => {
    const wrapper = mountView();

    expect(wrapper.text()).toContain('YouTube-Video per URL oder ID importieren');
    expect(wrapper.get('input[name="youtubeUrl"]').attributes('placeholder')).toContain('youtube.com/watch');

    await wrapper.get('select[name="mediaType"]').setValue('VIDEO');

    expect(wrapper.get('option[value="YOUTUBE"]').text()).toContain('offizielle Videosuche');
    expect((wrapper.get('select[name="source"]').element as HTMLSelectElement).value).toBe('YOUTUBE');
    expect(wrapper.text()).toContain('separate YouTube-URL-Import');
  });

  it('searches youtube videos through the normal external search form and renders thumbnails', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'ai tutorial',
      mediaType: 'VIDEO',
      source: 'YOUTUBE',
      warnings: [],
      results: [
        {
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
      ],
    });

    const wrapper = mountView();

    await wrapper.get('select[name="mediaType"]').setValue('VIDEO');
    await wrapper.get('input[name="query"]').setValue('ai tutorial');
    await wrapper.get('form.external-search-form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'ai tutorial',
      mediaType: 'VIDEO',
      source: 'YOUTUBE',
    });
    expect(wrapper.text()).toContain('AI Tutorial for Builders');
    expect(wrapper.text()).toContain('YouTube');
    expect(wrapper.text()).toContain('Video');
    expect(wrapper.text()).toContain('MoodMatch Dev');
    expect(wrapper.get('img').attributes('src')).toBe('https://img.youtube.test/high.jpg');
  });

  it('shows a clear warning when youtube query search is selected without a backend api key', async () => {
    searchExternalMock.mockRejectedValue(
      new ApiRequestError(
        'YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.',
        {
          status: 400,
          code: 'BUSINESS_RULE_VIOLATION',
        },
      ),
    );

    const wrapper = mountView();

    await wrapper.get('select[name="mediaType"]').setValue('VIDEO');
    await wrapper.get('input[name="query"]').setValue('AI tutorial');
    await wrapper.get('form.external-search-form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Provider-Hinweis');
    expect(wrapper.text()).toContain('MOODMATCH_YOUTUBE_API_KEY');
    expect(wrapper.text()).not.toContain('Suche konnte nicht abgeschlossen werden');
  });

  it('resolves a valid youtube url into a preview card with thumbnail metadata', async () => {
    resolveExternalUrlMock.mockResolvedValue({
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
      externalSubjects: ['Vue 3', 'Tutorial', 'Channel: MoodMatch Dev'],
      suggestedTags: [],
      attribution: 'Metadata from YouTube',
      warnings: [],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="youtubeUrl"]').setValue('https://youtu.be/abc123XYZ_0');
    await wrapper.get('form.youtube-url-import-form').trigger('submit');
    await flushPromises();

    expect(resolveExternalUrlMock).toHaveBeenCalledWith({
      source: 'YOUTUBE',
      url: 'https://youtu.be/abc123XYZ_0',
    });
    expect(wrapper.text()).toContain('VueConf 2024 Keynote');
    expect(wrapper.text()).toContain('YouTube');
    expect(wrapper.text()).toContain('Video · YouTube');
    expect(wrapper.text()).toContain('2024');
    expect(wrapper.text()).toContain('Channel');
    expect(wrapper.text()).toContain('MoodMatch Dev');
    expect(wrapper.get('img').attributes('src')).toBe('https://img.youtube.test/maxres.jpg');
  });

  it('shows a clear backend configuration message when the youtube api key is missing', async () => {
    resolveExternalUrlMock.mockRejectedValue(
      new ApiRequestError(
        'YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.',
        {
          status: 400,
          code: 'BUSINESS_RULE_VIOLATION',
        },
      ),
    );

    const wrapper = mountView();

    await wrapper.get('input[name="youtubeUrl"]').setValue('https://www.youtube.com/watch?v=abc123XYZ_0');
    await wrapper.get('form.youtube-url-import-form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('YouTube-Import konnte nicht vorbereitet werden');
    expect(wrapper.text()).toContain('MOODMATCH_YOUTUBE_API_KEY');
  });

  it('shows invalid youtube url errors without calling the import flow', async () => {
    resolveExternalUrlMock.mockRejectedValue(
      new ApiRequestError('Enter a valid YouTube URL or video ID.', {
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
      }),
    );

    const wrapper = mountView();

    await wrapper.get('input[name="youtubeUrl"]').setValue('https://example.com/watch?v=abc123XYZ_0');
    await wrapper.get('form.youtube-url-import-form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Enter a valid YouTube URL or video ID.');
    expect(importExternalMediaMock).not.toHaveBeenCalled();
  });

  it('imports a resolved youtube preview through the existing import flow', async () => {
    resolveExternalUrlMock.mockResolvedValue({
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
      externalSubjects: ['Vue 3', 'Tutorial', 'Channel: MoodMatch Dev'],
      suggestedTags: [],
      attribution: 'Metadata from YouTube',
      warnings: [],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'media-youtube-1',
        title: 'VueConf 2024 Keynote',
        sourceNote: 'Imported from YOUTUBE',
      },
    });

    const wrapper = mountView();

    await wrapper.get('input[name="youtubeUrl"]').setValue('abc123XYZ_0');
    await wrapper.get('form.youtube-url-import-form').trigger('submit');
    await flushPromises();

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeDefined();

    await importButton?.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      externalSubjects: ['Vue 3', 'Tutorial', 'Channel: MoodMatch Dev'],
      attribution: 'Metadata from YouTube',
    });
    expect(wrapper.text()).toContain('Imported into your media library.');
  });

  it('imports a searched youtube result through the existing import flow', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'ai tutorial',
      mediaType: 'VIDEO',
      source: 'YOUTUBE',
      warnings: [],
      results: [
        {
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
      ],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'media-youtube-search-1',
        title: 'AI Tutorial for Builders',
        sourceNote: 'Imported from YOUTUBE',
      },
    });

    const wrapper = mountView();

    await wrapper.get('select[name="mediaType"]').setValue('VIDEO');
    await wrapper.get('input[name="query"]').setValue('ai tutorial');
    await wrapper.get('form.external-search-form').trigger('submit');
    await flushPromises();

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeDefined();

    await importButton?.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      attribution: 'Metadata from YouTube',
    });
    expect(wrapper.text()).toContain('Imported into your media library.');
  });

  it('enables AniList for anime and manga without exposing anime or manga core media types', async () => {
    const wrapper = mountView();

    expect(wrapper.text()).toContain('Automatisch (TMDB + AniList)');
    expect(wrapper.text()).toContain('Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp');
    expect(wrapper.text()).toContain('Anime movie / AniList');
    expect(wrapper.get('option[value="ANILIST"]').attributes('disabled')).toBeUndefined();

    await wrapper.get('select[name="mediaType"]').setValue('SERIES');

    expect(wrapper.text()).toContain('Anime series / AniList');
    expect(wrapper.get('option[value="ANILIST"]').attributes('disabled')).toBeUndefined();

    await wrapper.get('select[name="mediaType"]').setValue('BOOK');

    expect(wrapper.text()).toContain('Manga / AniList');
    expect(wrapper.text()).toContain('Automatisch (Open Library + AniList)');
    expect(wrapper.get('option[value="ANILIST"]').attributes('disabled')).toBeUndefined();
    expect(wrapper.find('option[value="ANIME"]').exists()).toBe(false);
    expect(wrapper.find('option[value="MANGA"]').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('Spotify');
    expect(wrapper.text()).not.toContain('Music');
  });

  it('renders automatic search results and provider warnings from the normalized response', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'spirited away',
      mediaType: 'FILM',
      source: 'AUTOMATIC',
      warnings: ['TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Provider skipped in automatic search.'],
      results: [
        {
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
          externalGenres: ['Adventure'],
          externalSubjects: ['Format: MOVIE', 'Status: FINISHED'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('spirited away');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'spirited away',
      mediaType: 'FILM',
      source: undefined,
    });
    expect(wrapper.text()).toContain('1 Treffer aus passenden Quellen');
    expect(wrapper.text()).toContain('Sen to Chihiro no Kamikakushi');
    expect(wrapper.text()).toContain('AniList');
    expect(wrapper.text()).toContain('Film');
    expect(wrapper.text()).toContain('Anime movie');
    expect(wrapper.text()).toContain('Automatic searches all suitable providers for the selected media type.');
    expect(wrapper.text()).toContain('Provider-Hinweis');
    expect(wrapper.text()).toContain('TMDB provider is not configured');
  });

  it('renders mixed-source automatic results with provider badges', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'berserk',
      mediaType: 'BOOK',
      source: 'AUTOMATIC',
      warnings: [],
      results: [
        {
          source: 'OPEN_LIBRARY',
          externalId: 'OL12345W',
          mediaType: 'BOOK',
          title: 'Berserk Deluxe',
          originalTitle: null,
          creatorNames: ['Kentaro Miura'],
          description: 'Book metadata from Open Library.',
          releaseYear: 2019,
          coverUrl: null,
          sourceUrl: 'https://openlibrary.org/works/OL12345W',
          externalGenres: ['Fantasy'],
          externalSubjects: ['Dark fantasy'],
          suggestedTags: [],
          attribution: 'Metadata from Open Library',
          warnings: [],
        },
        {
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
          externalGenres: ['Action'],
          externalSubjects: ['Format: MANGA', 'Status: RELEASING'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('berserk');
    await wrapper.get('select[name="mediaType"]').setValue('BOOK');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('2 Treffer aus passenden Quellen');
    expect(wrapper.text()).toContain('Open Library');
    expect(wrapper.text()).toContain('AniList');
    expect(wrapper.text()).toContain('Buch');
    expect(wrapper.text()).toContain('Manga');
  });

  it('filters mixed automatic results by provider', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'berserk',
      mediaType: 'BOOK',
      source: 'AUTOMATIC',
      warnings: [],
      results: [
        {
          source: 'OPEN_LIBRARY',
          externalId: 'OL12345W',
          mediaType: 'BOOK',
          title: 'Berserk Deluxe',
          originalTitle: null,
          creatorNames: ['Kentaro Miura'],
          description: 'Book metadata from Open Library.',
          releaseYear: 2019,
          coverUrl: null,
          sourceUrl: 'https://openlibrary.org/works/OL12345W',
          externalGenres: ['Fantasy'],
          externalSubjects: ['Dark fantasy'],
          suggestedTags: [],
          attribution: 'Metadata from Open Library',
          warnings: [],
        },
        {
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
          externalGenres: ['Action'],
          externalSubjects: ['Format: MANGA', 'Status: RELEASING'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('berserk');
    await wrapper.get('select[name="mediaType"]').setValue('BOOK');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    await wrapper.get('select[name="resultProviderFilter"]').setValue('ANILIST');

    expect(wrapper.text()).toContain('1 Treffer aus passenden Quellen');
    expect(wrapper.text()).toContain('Berserk');
    expect(wrapper.text()).not.toContain('Berserk Deluxe');
  });

  it('passes the selected book source through the search request', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'dune',
      mediaType: 'BOOK',
      source: 'OPEN_LIBRARY',
      warnings: [],
      results: [],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('dune');
    await wrapper.get('select[name="mediaType"]').setValue('BOOK');
    await wrapper.get('select[name="source"]').setValue('OPEN_LIBRARY');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'dune',
      mediaType: 'BOOK',
      source: 'OPEN_LIBRARY',
    });
  });

  it('shows podcast provider warnings from automatic search when credentials are missing', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'lex fridman',
      mediaType: 'PODCAST',
      source: 'AUTOMATIC',
      warnings: [
        'Podcast Index provider is not configured. Set MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET. Provider skipped in automatic search.',
      ],
      results: [],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('lex fridman');
    await wrapper.get('select[name="mediaType"]').setValue('PODCAST');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'lex fridman',
      mediaType: 'PODCAST',
      source: undefined,
    });
    expect(wrapper.text()).toContain('Provider-Hinweis');
    expect(wrapper.text()).toContain('Podcast Index provider is not configured');
    expect(wrapper.text()).toContain('Keine Ergebnisse gefunden');
  });

  it('imports a podcast result and shows the library link state', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'lex fridman',
      mediaType: 'PODCAST',
      source: 'PODCAST_INDEX',
      warnings: [],
      results: [
        {
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
      ],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'media-podcast-1',
        title: 'Lex Fridman Podcast',
        originalTitle: null,
        description:
          'Conversations about science, technology, history, philosophy, and the nature of intelligence.',
        mediaType: 'PODCAST',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from PODCAST_INDEX',
        commitmentLevel: 'LONG',
        releaseYear: 2024,
        coverUrl: 'https://image.simplecastcdn.com/images/lex-fridman.jpg',
        externalSourceName: 'PODCAST_INDEX',
        externalSourceId: '75075',
        externalSourceUrl: 'https://lexfridman.com/podcast/',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('lex fridman');
    await wrapper.get('select[name="mediaType"]').setValue('PODCAST');
    await wrapper.get('select[name="source"]').setValue('PODCAST_INDEX');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeDefined();

    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      attribution: 'Metadata from Podcast Index',
    });
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('In Mediathek ansehen');
  });

  it('imports an audiobook result and shows the library link state', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'pride',
      mediaType: 'AUDIOBOOK',
      source: 'LIBRIVOX',
      warnings: [],
      results: [
        {
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
          suggestedTags: [],
          attribution: 'LibriVox public domain audiobook catalog',
          warnings: [],
        },
      ],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'media-1',
        title: 'Pride and Prejudice',
        originalTitle: null,
        description: 'Jane Austen classic narrated for the public domain catalog.',
        mediaType: 'AUDIOBOOK',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from LIBRIVOX',
        commitmentLevel: 'LONG',
        releaseYear: 1813,
        coverUrl: 'https://archive.org/covers/pride.jpg',
        externalSourceName: 'LIBRIVOX',
        externalSourceId: '253',
        externalSourceUrl: 'https://librivox.org/pride-and-prejudice-by-jane-austen/',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('pride');
    await wrapper.get('select[name="mediaType"]').setValue('AUDIOBOOK');
    await wrapper.get('select[name="source"]').setValue('LIBRIVOX');
    await wrapper.get('form').trigger('submit');
    await flushPromises();
    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      attribution: 'LibriVox public domain audiobook catalog',
    });
    expect(wrapper.text()).toContain('Imported into your media library.');
    expect(wrapper.text()).toContain('In Mediathek ansehen');
  });

  it('renders and imports a RAWG game result', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'elden ring',
      mediaType: 'GAME',
      source: 'RAWG',
      warnings: [],
      results: [
        {
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
      ],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'game-1',
        title: 'Elden Ring',
        originalTitle: null,
        description: 'Rise, Tarnished, and be guided by grace.',
        mediaType: 'GAME',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from RAWG',
        commitmentLevel: 'LONG',
        releaseYear: 2022,
        coverUrl: 'https://media.rawg.io/media/games/elden-ring.jpg',
        externalSourceName: 'RAWG',
        externalSourceId: '3498',
        externalSourceUrl: 'https://rawg.io/games/elden-ring',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('elden ring');
    await wrapper.get('select[name="mediaType"]').setValue('GAME');
    await wrapper.get('select[name="source"]').setValue('RAWG');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'elden ring',
      mediaType: 'GAME',
      source: 'RAWG',
    });
    expect(wrapper.text()).toContain('1 Treffer aus RAWG');
    expect(wrapper.text()).toContain('Elden Ring');
    expect(wrapper.text()).toContain('Platforms / Tags');
    expect(wrapper.text()).toContain('PlayStation 5');

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      attribution: 'Metadata from RAWG. View source on RAWG for full provider details.',
    });
    expect(wrapper.text()).toContain('In Mediathek ansehen');
  });

  it('renders and imports an AniList anime series as a series result', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'attack on titan',
      mediaType: 'SERIES',
      source: 'ANILIST',
      warnings: [],
      results: [
        {
          source: 'ANILIST',
          externalId: '16498',
          mediaType: 'SERIES',
          title: 'Shingeki no Kyojin',
          originalTitle: '進撃の巨人',
          creatorNames: ['Wit Studio'],
          description: 'Humanity fights titans beyond the walls.',
          releaseYear: 2013,
          coverUrl: 'https://img.anilist.co/aot-large.jpg',
          sourceUrl: 'https://anilist.co/anime/16498',
          externalGenres: ['Action', 'Drama'],
          externalSubjects: ['Format: TV', 'Status: FINISHED', 'Season: SPRING 2013'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'anime-series-1',
        title: 'Shingeki no Kyojin',
        originalTitle: '進撃の巨人',
        description: 'Humanity fights titans beyond the walls.',
        mediaType: 'SERIES',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from ANILIST',
        commitmentLevel: 'LONG',
        releaseYear: 2013,
        coverUrl: 'https://img.anilist.co/aot-large.jpg',
        externalSourceName: 'ANILIST',
        externalSourceId: '16498',
        externalSourceUrl: 'https://anilist.co/anime/16498',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('attack on titan');
    await wrapper.get('select[name="mediaType"]').setValue('SERIES');
    await wrapper.get('select[name="source"]').setValue('ANILIST');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'attack on titan',
      mediaType: 'SERIES',
      source: 'ANILIST',
    });
    expect(wrapper.text()).toContain('1 Treffer aus AniList');
    expect(wrapper.text()).toContain('Shingeki no Kyojin');
    expect(wrapper.text()).toContain('Serie · AniList · Anime series');
    expect(wrapper.text()).toContain('2013');
    expect(wrapper.text()).toContain('Originaltitel: 進撃の巨人');
    expect(wrapper.text()).toContain('Format / Status / Tags');

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
      source: 'ANILIST',
      externalId: '16498',
      mediaType: 'SERIES',
      title: 'Shingeki no Kyojin',
      originalTitle: '進撃の巨人',
      creatorNames: ['Wit Studio'],
      description: 'Humanity fights titans beyond the walls.',
      releaseYear: 2013,
      coverUrl: 'https://img.anilist.co/aot-large.jpg',
      sourceUrl: 'https://anilist.co/anime/16498',
      externalGenres: ['Action', 'Drama'],
      externalSubjects: ['Format: TV', 'Status: FINISHED', 'Season: SPRING 2013'],
      attribution: 'Metadata from AniList',
    });
    expect(wrapper.text()).toContain('In Mediathek ansehen');
  });

  it('renders AniList anime movie and manga mappings from normalized results', async () => {
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'anime-movie-1',
        title: 'Sen to Chihiro no Kamikakushi',
        originalTitle: '千と千尋の神隠し',
        description: 'A young girl enters a world of spirits.',
        mediaType: 'FILM',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from ANILIST',
        commitmentLevel: 'MEDIUM',
        releaseYear: 2001,
        coverUrl: 'https://img.anilist.co/spirited-away.jpg',
        externalSourceName: 'ANILIST',
        externalSourceId: '199',
        externalSourceUrl: 'https://anilist.co/anime/199',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
    searchExternalMock.mockResolvedValueOnce({
      query: 'spirited away',
      mediaType: 'FILM',
      source: 'ANILIST',
      warnings: [],
      results: [
        {
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
          externalGenres: ['Adventure'],
          externalSubjects: ['Format: MOVIE', 'Status: FINISHED'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('spirited away');
    await wrapper.get('select[name="source"]').setValue('ANILIST');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Sen to Chihiro no Kamikakushi');
    expect(wrapper.text()).toContain('Film · AniList · Anime movie');
    expect(wrapper.text()).toContain('2001');

    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      externalGenres: ['Adventure'],
      externalSubjects: ['Format: MOVIE', 'Status: FINISHED'],
      attribution: 'Metadata from AniList',
    });
    expect(wrapper.text()).toContain('In Mediathek ansehen');

    searchExternalMock.mockResolvedValueOnce({
      query: 'berserk',
      mediaType: 'BOOK',
      source: 'ANILIST',
      warnings: [],
      results: [
        {
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
          externalGenres: ['Action'],
          externalSubjects: ['Format: MANGA', 'Status: RELEASING'],
          suggestedTags: [],
          attribution: 'Metadata from AniList',
          warnings: [],
        },
      ],
    });

    await wrapper.get('input[name="query"]').setValue('berserk');
    await wrapper.get('select[name="mediaType"]').setValue('BOOK');
    await wrapper.get('select[name="source"]').setValue('ANILIST');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Berserk');
    expect(wrapper.text()).toContain('Buch · AniList · Manga');
    expect(wrapper.text()).toContain('1989');
    expect(wrapper.text()).toContain('Format: MANGA');
  });

  it('renders an empty state when the search succeeds without matches', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'missing',
      mediaType: 'GAME',
      source: 'DEMO',
      warnings: [],
      results: [],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('missing');
    await wrapper.get('select[name="mediaType"]').setValue('GAME');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Keine Ergebnisse gefunden');
  });

  it('renders an error state when the API call fails', async () => {
    searchExternalMock.mockRejectedValue(new Error('boom'));

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Suche konnte nicht abgeschlossen werden');
    expect(wrapper.text()).toContain('Die externe Suche konnte gerade nicht geladen werden.');
  });

  it('renders the RAWG missing-key message surfaced by the backend', async () => {
    searchExternalMock.mockRejectedValue(new ApiRequestError(
      'RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.',
      {
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
      },
    ));

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('zelda');
    await wrapper.get('select[name="mediaType"]').setValue('GAME');
    await wrapper.get('select[name="source"]').setValue('RAWG');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.');
  });

  it('renders an import error when saving fails', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'arrival',
      mediaType: 'FILM',
      source: 'TMDB',
      warnings: [],
      results: [
        {
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
      ],
    });
    importExternalMediaMock.mockRejectedValue(new Error('boom'));

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();
    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('Der Import konnte gerade nicht abgeschlossen werden.');
  });
});
