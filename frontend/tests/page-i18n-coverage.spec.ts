import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '@/App.vue';
import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';
import { createAppRouter } from '@/router';
import { initializeI18n, LANGUAGE_STORAGE_KEY, setAppLocale } from '@/i18n';
import { useAuthStore } from '@/stores/auth';

const { getCurrentUserMock } = vi.hoisted(() => ({
  getCurrentUserMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  getCurrentUser: getCurrentUserMock,
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  register: vi.fn(),
}));

const { listMediaMock, getMediaByIdMock } = vi.hoisted(() => ({
  listMediaMock: vi.fn(),
  getMediaByIdMock: vi.fn(),
}));

vi.mock('@/api/media', () => ({
  listMedia: listMediaMock,
  getMediaById: getMediaByIdMock,
}));

const { listTagsMock } = vi.hoisted(() => ({
  listTagsMock: vi.fn(),
}));

vi.mock('@/api/tags', () => ({
  listTags: listTagsMock,
}));

const { getProfileMock } = vi.hoisted(() => ({
  getProfileMock: vi.fn(),
}));

vi.mock('@/api/profile', () => ({
  getProfile: getProfileMock,
}));

const { listCandidatesMock } = vi.hoisted(() => ({
  listCandidatesMock: vi.fn(),
}));

vi.mock('@/api/candidates', () => ({
  listCandidates: listCandidatesMock,
}));

const { getMatchesMock } = vi.hoisted(() => ({
  getMatchesMock: vi.fn(),
}));

vi.mock('@/api/matches', () => ({
  getMatches: getMatchesMock,
}));

const defaultMediaItems = [
  {
    id: 'media-1',
    title: 'Arrival',
    originalTitle: null,
    description: 'A sci-fi arrival story.',
    mediaType: 'FILM',
    consumptionStatus: 'CONSUMED',
    isFavourite: true,
    rating: 5,
    sourceType: 'MANUAL',
    sourceNote: null,
    commitmentLevel: 'MEDIUM',
    releaseYear: 2016,
    coverUrl: null,
    externalSourceName: null,
    externalSourceId: null,
    externalSourceUrl: null,
    metadataOrigin: 'MANUAL',
    tags: [
      {
        id: 'tag-1',
        name: 'Wonder',
        category: 'EXPERIENCE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ],
    externalReferences: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'media-2',
    title: 'Pride and Prejudice',
    originalTitle: null,
    description: 'Classic provider text should stay unchanged.',
    mediaType: 'AUDIOBOOK',
    consumptionStatus: 'WANT_TO_CONSUME',
    isFavourite: false,
    rating: null,
    sourceType: 'EXTERNAL_SEARCH',
    sourceNote: null,
    commitmentLevel: 'LONG',
    releaseYear: 1813,
    coverUrl: null,
    externalSourceName: 'OPEN_LIBRARY',
    externalSourceId: 'ol-1',
    externalSourceUrl: 'https://openlibrary.org/works/OL1W',
    metadataOrigin: 'IMPORTED',
    tags: [],
    externalReferences: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  },
];

const defaultProfile = {
  isReadyForMatching: true,
  profileRelevantMediaCount: 3,
  requiredProfileRelevantMediaCount: 3,
  explanationMessage: 'Backend copy should not leak into UI copy checks.',
  contributingMedia: [
    {
      media: {
        id: 'media-1',
        title: 'Arrival',
        mediaType: 'FILM',
        consumptionStatus: 'CONSUMED',
        commitmentLevel: 'MEDIUM',
        isFavourite: true,
        rating: 5,
        releaseYear: 2016,
        coverUrl: null,
        tags: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      ratingWeight: '1.8',
      favouriteFactor: '1.2',
      tagContributions: [],
    },
  ],
  weightedTags: [
    {
      tag: {
        id: 'tag-1',
        name: 'Wonder',
        category: 'EXPERIENCE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      weight: '3.4',
    },
  ],
};

const defaultCandidates = {
  candidates: [
    {
      media: {
        id: 'candidate-1',
        title: 'Dune',
        mediaType: 'FILM',
        consumptionStatus: 'WANT_TO_CONSUME',
        commitmentLevel: 'LONG',
        isFavourite: false,
        rating: null,
        releaseYear: 2021,
        coverUrl: null,
        tags: [
          {
            id: 'tag-2',
            name: 'Science Fiction',
            category: 'GENRE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      isCompleteForMatching: true,
    },
    {
      media: {
        id: 'candidate-2',
        title: 'Silo',
        mediaType: 'SERIES',
        consumptionStatus: 'WANT_TO_CONSUME',
        commitmentLevel: 'MEDIUM',
        isFavourite: false,
        rating: null,
        releaseYear: 2023,
        coverUrl: null,
        tags: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      isCompleteForMatching: false,
    },
  ],
};

const defaultMatching = {
  interestProfile: defaultProfile,
  scoresSuppressed: false,
  explanationMessage: 'Backend matching copy should stay internal.',
  scoringMethodNote: 'Backend note should stay internal.',
  matches: [
    {
      candidate: defaultCandidates.candidates[0],
      candidateTagCount: 1,
      matchingTagCount: 1,
      matchingTags: [
        {
          tag: defaultCandidates.candidates[0].media.tags[0],
          profileWeight: '2.7',
        },
      ],
      extraCandidateTags: [],
      rawScore: '4.1',
      precisionFactor: '0.9',
      adjustedScore: '3.7',
      relativeScore: '82',
      explanationMessage: 'Raw backend match explanation.',
      candidateTagsNote: 'Raw backend candidate note.',
    },
  ],
};
const mountedWrappers: Array<{ unmount: () => void }> = [];

function resetApiMocks() {
  getCurrentUserMock.mockReset();
  listMediaMock.mockReset();
  getMediaByIdMock.mockReset();
  listTagsMock.mockReset();
  getProfileMock.mockReset();
  listCandidatesMock.mockReset();
  getMatchesMock.mockReset();

  getCurrentUserMock.mockResolvedValue(null);
  listMediaMock.mockResolvedValue(defaultMediaItems);
  getMediaByIdMock.mockResolvedValue(defaultMediaItems[1]);
  listTagsMock.mockResolvedValue([
    {
      id: 'tag-1',
      name: 'Wonder',
      category: 'EXPERIENCE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ]);
  getProfileMock.mockResolvedValue(defaultProfile);
  listCandidatesMock.mockResolvedValue(defaultCandidates);
  getMatchesMock.mockResolvedValue(defaultMatching);
}

async function mountAppAtRoute(path: string) {
  const pinia = createPinia();
  const authStore = useAuthStore(pinia);
  authStore.setAuthMode('local-demo');

  const router = createAppRouter({
    history: createMemoryHistory(),
    pinia,
  });

  await router.push(path);
  await router.isReady();

  const wrapper = mount(App, {
    global: {
      plugins: [pinia, router],
    },
  });
  mountedWrappers.push(wrapper);

  await flushPromises();

  return { wrapper, router };
}

async function expectRouteToSwitch(path: string, germanText: string, englishText: string) {
  setAppLocale('de');
  const { wrapper } = await mountAppAtRoute(path);

  expect(wrapper.text()).toContain(germanText);
  expect(document.documentElement.lang).toBe('de');

  setAppLocale('en');
  await flushPromises();

  expect(wrapper.text()).toContain(englishText);
  expect(document.documentElement.lang).toBe('en');

  return wrapper;
}

describe('page-level frontend i18n coverage', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.title = '';
    document.documentElement.lang = '';
    setAppLocale('de');
    resetApiMocks();
  });

  afterEach(() => {
    while (mountedWrappers.length > 0) {
      mountedWrappers.pop()?.unmount();
    }
  });

  it('switches the login page between German and English', async () => {
    await expectRouteToSwitch('/login', 'Melde dich an, um weiterzumachen', 'Log in to continue');
  });

  it('switches the authenticated app shell navigation between German and English', async () => {
    const wrapper = await expectRouteToSwitch('/profile', 'Profil', 'Profile');

    expect(wrapper.text()).toContain('Search');
    expect(wrapper.text()).toContain('Media');
    expect(wrapper.text()).not.toContain('Suche');
    expect(wrapper.text()).not.toContain('Medien');
  });

  it.each([
    ['/', 'führt aktuell deine Matches an.', 'is currently leading your matches.'],
    ['/profile', 'So versteht MoodMatch deinen Geschmack', 'How MoodMatch understands your taste'],
    ['/external-search', 'Medien suchen & importieren', 'Search & import media'],
    ['/media', 'Deine Mediathek', 'Your media library'],
    ['/candidates', 'Kandidaten fürs nächste Match', 'Next match candidates'],
    ['/swipe', 'Deine nächste Empfehlung', 'Your next recommendation'],
    ['/matches', 'Warum diese Titel zu dir passen', 'Why these titles fit you'],
  ])('switches %s between German and English', async (path, germanText, englishText) => {
    await expectRouteToSwitch(path, germanText, englishText);
  });

  it('keeps provider metadata unchanged while UI labels switch languages', async () => {
    setAppLocale('de');

    const wrapper = mount(ExternalSearchResultCard, {
      props: {
        result: {
          source: 'OPEN_LIBRARY',
          externalId: 'ol-1',
          mediaType: 'BOOK',
          title: 'Pride and Prejudice',
          originalTitle: '鬼滅の刃 1',
          creatorNames: ['Austen'],
          description: 'How To Create Cinematic AI Ads - Full AI Video Course',
          releaseYear: 1813,
          coverUrl: null,
          sourceUrl: 'https://openlibrary.org/works/OL1W',
          externalGenres: ['Classic'],
          externalSubjects: ['Open Library subject'],
          suggestedTags: [],
          attribution: 'Metadata from Open Library',
          warnings: [],
        },
      },
      global: {
        stubs: {
          RouterLink: {
            name: 'RouterLink',
            props: ['to'],
            template: '<a :data-to="JSON.stringify(to)"><slot /></a>',
          },
        },
      },
    });
    mountedWrappers.push(wrapper);

    expect(wrapper.text()).toContain('Importvorschau');
    expect(wrapper.text()).toContain('Pride and Prejudice');
    expect(wrapper.text()).toContain('鬼滅の刃 1');
    expect(wrapper.text()).toContain('Austen');
    expect(wrapper.text()).toContain('How To Create Cinematic AI Ads - Full AI Video Course');
    expect(wrapper.text()).toContain('Open Library subject');

    setAppLocale('en');
    await flushPromises();

    expect(wrapper.text()).toContain('Import preview');
    expect(wrapper.text()).toContain('Pride and Prejudice');
    expect(wrapper.text()).toContain('鬼滅の刃 1');
    expect(wrapper.text()).toContain('Austen');
    expect(wrapper.text()).toContain('How To Create Cinematic AI Ads - Full AI Video Course');
    expect(wrapper.text()).toContain('Open Library subject');
  });

  it('switches internal media type and status labels between German and English', async () => {
    const { wrapper } = await mountAppAtRoute('/media');

    expect(wrapper.text()).toContain('Hörbuch');
    expect(wrapper.text()).toContain('Möchte ich konsumieren');
    expect(wrapper.text()).toContain('Pride and Prejudice');

    setAppLocale('en');
    await flushPromises();

    expect(wrapper.text()).toContain('Audiobook');
    expect(wrapper.text()).toContain('Want to consume');
    expect(wrapper.text()).toContain('Pride and Prejudice');
  });

  it('updates document.title and html lang when the locale changes', async () => {
    await mountAppAtRoute('/media');

    expect(document.title).toBe('MoodMatch | Mediathek');
    expect(document.documentElement.lang).toBe('de');

    setAppLocale('en');
    await flushPromises();

    expect(document.title).toBe('MoodMatch | Media Library');
    expect(document.documentElement.lang).toBe('en');
  });

  it('restores the stored locale for the app shell on a fresh mount', async () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'en');
    initializeI18n();

    const { wrapper } = await mountAppAtRoute('/profile');

    expect(wrapper.text()).toContain('How MoodMatch understands your taste');
    expect(document.documentElement.lang).toBe('en');
  });
});
