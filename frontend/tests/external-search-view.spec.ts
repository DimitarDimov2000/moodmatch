import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiRequestError } from '@/api/client';
import ExternalSearchView from '@/views/ExternalSearchView.vue';

const { searchExternalMock, importExternalMediaMock } = vi.hoisted(() => ({
  searchExternalMock: vi.fn(),
  importExternalMediaMock: vi.fn(),
}));

vi.mock('@/api/external', () => ({
  searchExternal: searchExternalMock,
  importExternalMedia: importExternalMediaMock,
}));

describe('ExternalSearchView', () => {
  beforeEach(() => {
    searchExternalMock.mockReset();
    importExternalMediaMock.mockReset();
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

  it('renders search results and the demo fallback message from the normalized response', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'arrival',
      mediaType: 'FILM',
      source: 'DEMO',
      warnings: ['TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Using DEMO fallback.'],
      results: [
        {
          source: 'DEMO',
          externalId: 'demo-film-arrival',
          mediaType: 'FILM',
          title: 'Arrival',
          originalTitle: null,
          creatorNames: [],
          description: 'A linguist races to understand visitors.',
          releaseYear: 2016,
          coverUrl: null,
          sourceUrl: 'https://demo.moodmatch.local/items/demo-film-arrival',
          externalGenres: ['Science-Fiction'],
          externalSubjects: ['Zeit'],
          suggestedTags: [],
          attribution: 'MoodMatch Demo Provider (offline)',
          warnings: [],
        },
      ],
    });

    const wrapper = mountView();

    await wrapper.get('input[name="query"]').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'arrival',
      mediaType: 'FILM',
      source: undefined,
    });
    expect(wrapper.text()).toContain('1 Treffer aus DEMO');
    expect(wrapper.text()).toContain('Arrival');
    expect(wrapper.text()).toContain('DEMO-Fallback aktiv');
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
