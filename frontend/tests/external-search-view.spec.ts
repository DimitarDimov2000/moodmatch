import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ExternalSearchView from '@/views/ExternalSearchView.vue';

const { searchExternalMock } = vi.hoisted(() => ({
  searchExternalMock: vi.fn(),
}));

vi.mock('@/api/external', () => ({
  searchExternal: searchExternalMock,
}));

describe('ExternalSearchView', () => {
  beforeEach(() => {
    searchExternalMock.mockReset();
  });

  it('renders the preview state before a search starts', () => {
    const wrapper = mount(ExternalSearchView, {
      global: {
        stubs: {
          AppMessage: false,
          ExternalSearchForm: false,
          ExternalSearchResultCard: false,
        },
      },
    });

    expect(wrapper.text()).toContain('Provider-Preview ohne Live-API');
    expect(wrapper.text()).toContain('Noch keine Suche gestartet');
  });

  it('renders search results from the normalized demo response', async () => {
    searchExternalMock.mockResolvedValue({
      query: 'arrival',
      mediaType: 'FILM',
      source: 'DEMO',
      warnings: [],
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
        },
      },
    });

    await wrapper.get('input').setValue('missing');
    await wrapper.get('select').setValue('GAME');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Keine Demo-Ergebnisse gefunden');
  });

  it('renders an error state when the API call fails', async () => {
    searchExternalMock.mockRejectedValue(new Error('boom'));

    const wrapper = mount(ExternalSearchView, {
      global: {
        stubs: {
          AppMessage: false,
          ExternalSearchForm: false,
          ExternalSearchResultCard: false,
        },
      },
    });

    await wrapper.get('input').setValue('arrival');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('Suche konnte nicht abgeschlossen werden');
    expect(wrapper.text()).toContain(
      'Die externe Demo-Suche konnte gerade nicht geladen werden.',
    );
  });
});
