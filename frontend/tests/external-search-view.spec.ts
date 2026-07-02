import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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

  it('renders the import-first state before a search starts and keeps YouTube out of the UI', () => {
    const wrapper = mount(ExternalSearchView, {
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

    expect(wrapper.text()).toContain('Externe Medien suchen und importieren');
    expect(wrapper.text()).toContain('Noch keine Suche gestartet');
    expect(wrapper.text()).not.toContain('YouTube');
  });

  function getImportButton(wrapper: ReturnType<typeof mount>) {
    return wrapper
      .findAll('button')
      .find((candidate) => candidate.text().includes('In Mediathek importieren'));
  }

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

    const wrapper = mount(ExternalSearchView, {
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

    await wrapper.get('input').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(searchExternalMock).toHaveBeenCalledWith({
      query: 'arrival',
      mediaType: 'FILM',
    });
    expect(wrapper.text()).toContain('1 Treffer aus DEMO');
    expect(wrapper.text()).toContain('Arrival');
    expect(wrapper.text()).toContain('DEMO-Fallback aktiv');
  });

  it('imports a result and shows the library link state', async () => {
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
    importExternalMediaMock.mockResolvedValue({
      created: true,
      message: 'Imported into your media library.',
      media: {
        id: 'media-1',
        title: 'Arrival',
        originalTitle: null,
        description: 'First contact changes everything.',
        mediaType: 'FILM',
        consumptionStatus: 'WANT_TO_CONSUME',
        isFavourite: false,
        rating: null,
        sourceType: 'EXTERNAL_SEARCH',
        sourceNote: 'Imported from TMDB',
        commitmentLevel: 'MEDIUM',
        releaseYear: 2016,
        coverUrl: null,
        externalSourceName: 'TMDB',
        externalSourceId: '11',
        externalSourceUrl: 'https://www.themoviedb.org/movie/11',
        metadataOrigin: 'IMPORTED',
        tags: [],
        externalReferences: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });

    const wrapper = mount(ExternalSearchView, {
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

    await wrapper.get('input').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();
    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(importExternalMediaMock).toHaveBeenCalledWith({
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
      attribution: 'Metadata from TMDB',
    });
    expect(wrapper.text()).toContain('Imported into your media library.');
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

    const wrapper = mount(ExternalSearchView, {
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

    await wrapper.get('input').setValue('missing');
    await wrapper.get('select').setValue('GAME');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Keine Ergebnisse gefunden');
  });

  it('renders an error state when the API call fails', async () => {
    searchExternalMock.mockRejectedValue(new Error('boom'));

    const wrapper = mount(ExternalSearchView, {
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

    await wrapper.get('input').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Suche konnte nicht abgeschlossen werden');
    expect(wrapper.text()).toContain('Die externe Suche konnte gerade nicht geladen werden.');
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

    const wrapper = mount(ExternalSearchView, {
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

    await wrapper.get('input').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();
    const importButton = getImportButton(wrapper);
    expect(importButton).toBeTruthy();
    await importButton!.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('Der Import konnte gerade nicht abgeschlossen werden.');
  });
});
