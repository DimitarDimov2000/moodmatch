import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiRequestError } from '@/api/client';
import SwipeView from '@/views/SwipeView.vue';
import type {
  CandidateSelectionResponse,
  MatchingResponse,
  MediaResponse,
} from '@/types/api';

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :data-to="JSON.stringify(to)"><slot /></a>',
  },
}));

const {
  listCandidatesMock,
  getMatchesMock,
  getMediaByIdMock,
} = vi.hoisted(() => ({
  listCandidatesMock: vi.fn(),
  getMatchesMock: vi.fn(),
  getMediaByIdMock: vi.fn(),
}));

vi.mock('@/api/candidates', () => ({
  listCandidates: listCandidatesMock,
}));

vi.mock('@/api/matches', () => ({
  getMatches: getMatchesMock,
}));

vi.mock('@/api/media', () => ({
  getMediaById: getMediaByIdMock,
}));

const candidateResponse: CandidateSelectionResponse = {
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
        coverUrl: 'https://example.com/dune.jpg',
        tags: [
          {
            id: 'tag-1',
            name: 'Science-Fiction',
            category: 'GENRE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 'tag-2',
            name: 'Abenteuer',
            category: 'THEME',
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
        tags: [
          {
            id: 'tag-3',
            name: 'Mystery',
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
  ],
};

const matchingResponse: MatchingResponse = {
  interestProfile: {
    isReadyForMatching: true,
    profileRelevantMediaCount: 3,
    requiredProfileRelevantMediaCount: 3,
    explanationMessage: 'Profil ist bereit.',
    contributingMedia: [],
    weightedTags: [],
  },
  scoresSuppressed: false,
  explanationMessage: 'Matches sind verfuegbar.',
  scoringMethodNote: 'Relativ bei genug Daten.',
  matches: [
    {
      candidate: candidateResponse.candidates[0],
      candidateTagCount: 2,
      matchingTagCount: 2,
      matchingTags: [
        {
          tag: candidateResponse.candidates[0].media.tags[0],
          profileWeight: '2.5',
        },
        {
          tag: candidateResponse.candidates[0].media.tags[1],
          profileWeight: '2.2',
        },
      ],
      extraCandidateTags: [],
      rawScore: '4',
      precisionFactor: '1',
      adjustedScore: '4',
      relativeScore: '82',
      explanationMessage: 'Dune passt gut zu deinem aktuellen Profil.',
      candidateTagsNote: 'Kandidaten-Tags basieren auf deiner Einschaetzung.',
    },
    {
      candidate: candidateResponse.candidates[1],
      candidateTagCount: 1,
      matchingTagCount: 0,
      matchingTags: [],
      extraCandidateTags: [candidateResponse.candidates[1].media.tags[0]],
      rawScore: '0',
      precisionFactor: '0',
      adjustedScore: '0',
      relativeScore: null,
      explanationMessage: 'Silo ist eher ein Entdeckungstipp.',
      candidateTagsNote: 'Kandidaten-Tags basieren auf deiner Einschaetzung.',
    },
  ],
};

const detailedMediaResponse: MediaResponse = {
  id: 'candidate-1',
  title: 'Dune',
  originalTitle: 'Dune',
  description: 'Science fiction desert epic about power, prophecy, and survival.',
  mediaType: 'FILM',
  consumptionStatus: 'WANT_TO_CONSUME',
  isFavourite: false,
  rating: null,
  sourceType: 'EXTERNAL_SEARCH',
  sourceNote: null,
  commitmentLevel: 'LONG',
  releaseYear: 2021,
  coverUrl: 'https://example.com/dune.jpg',
  externalSourceName: 'TMDB',
  externalSourceId: 'tmdb-1',
  externalSourceUrl: 'https://www.themoviedb.org/movie/438631',
  metadataOrigin: 'IMPORTED',
  tags: candidateResponse.candidates[0].media.tags,
  externalReferences: [
    {
      id: 'ref-1',
      sourceName: 'TMDB',
      externalId: '438631',
      externalUrl: 'https://www.themoviedb.org/movie/438631',
      attributionText: null,
      sourcePayloadHash: null,
      createdAt: '2026-01-01T00:00:00Z',
    },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

async function mountView() {
  const wrapper = mount(SwipeView);
  await flushPromises();
  return wrapper;
}

async function settleDecisionAnimation() {
  vi.advanceTimersByTime(260);
  await flushPromises();
}

function progressValues(wrapper: ReturnType<typeof mount>) {
  return wrapper.findAll('.swipe-progress__stats dd').map((node) => node.text());
}

describe('SwipeView', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    listCandidatesMock.mockReset();
    getMatchesMock.mockReset();
    getMediaByIdMock.mockReset();

    listCandidatesMock.mockResolvedValue(candidateResponse);
    getMatchesMock.mockResolvedValue(matchingResponse);
    getMediaByIdMock.mockResolvedValue(detailedMediaResponse);
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  it('renders the first candidate with match percentage, reason chips, and mobile card structure', async () => {
    const wrapper = await mountView();

    expect(listCandidatesMock).toHaveBeenCalledTimes(1);
    expect(getMatchesMock).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('82%');
    expect(wrapper.text()).toContain('Science-Fiction');
    expect(wrapper.text()).toContain('2/2 Tags passen');
    expect(wrapper.text()).toContain('Nicht jetzt');
    expect(wrapper.text()).not.toContain('Links fuer Nicht jetzt, Mitte fuer Details, rechts fuer Like.');
    expect(wrapper.text()).not.toContain('Skip');
    expect(wrapper.find('.swipe-view__card-column').exists()).toBe(true);
    expect(wrapper.find('.swipe-decision-controls__buttons').exists()).toBe(true);
  });

  it('like button advances the queue', async () => {
    const wrapper = await mountView();

    await wrapper.get('.swipe-decision-controls__button--like').trigger('click');
    await settleDecisionAnimation();

    expect(wrapper.text()).toContain('Silo');
    expect(progressValues(wrapper)).toEqual(['1', '1', '0']);
  });

  it('left action advances the queue as a local pass', async () => {
    const wrapper = await mountView();

    await wrapper.get('.swipe-decision-controls__button--skip').trigger('click');
    await settleDecisionAnimation();

    expect(wrapper.text()).toContain('Silo');
    expect(progressValues(wrapper)).toEqual(['1', '0', '1']);
  });

  it('does not use vertical swipes for actions or details anymore', async () => {
    const wrapper = await mountView();
    const surface = wrapper.get('.swipe-candidate-card__swipe-surface');

    await surface.trigger('pointerdown', {
      pointerId: 1,
      clientX: 0,
      clientY: 0,
    });
    await surface.trigger('pointermove', {
      pointerId: 1,
      clientX: 0,
      clientY: -160,
    });
    await surface.trigger('pointerup', {
      pointerId: 1,
      clientX: 0,
      clientY: -160,
    });
    await flushPromises();

    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).not.toContain('Warum dieser Titel auftaucht');

    await surface.trigger('pointerdown', {
      pointerId: 2,
      clientX: 0,
      clientY: 0,
    });
    await surface.trigger('pointermove', {
      pointerId: 2,
      clientX: 0,
      clientY: 160,
    });
    await surface.trigger('pointerup', {
      pointerId: 2,
      clientX: 0,
      clientY: 160,
    });
    await flushPromises();

    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).not.toContain('Warum dieser Titel auftaucht');
  });

  it('details can expand and collapse with lazy-loaded extra info', async () => {
    const wrapper = await mountView();

    expect(getMediaByIdMock).not.toHaveBeenCalled();

    await wrapper.get('.swipe-decision-controls__button--details').trigger('click');
    await flushPromises();

    expect(getMediaByIdMock).toHaveBeenCalledWith('candidate-1');
    expect(wrapper.text()).toContain('Warum dieser Titel auftaucht');
    expect(wrapper.text()).toContain('Science fiction desert epic');
    expect(wrapper.text()).toContain('TMDB');

    await wrapper.get('.swipe-decision-controls__button--details').trigger('click');
    await flushPromises();

    expect(wrapper.text()).not.toContain('Warum dieser Titel auftaucht');
  });

  it('renders the profile-not-ready empty state', async () => {
    listCandidatesMock.mockResolvedValue({ candidates: [] });
    getMatchesMock.mockResolvedValue({
      interestProfile: {
        isReadyForMatching: false,
        profileRelevantMediaCount: 2,
        requiredProfileRelevantMediaCount: 3,
        explanationMessage: 'Noch mehr starke Ratings helfen.',
        contributingMedia: [],
        weightedTags: [],
      },
      scoresSuppressed: true,
      explanationMessage: 'Noch keine sichere Vergleichsbasis.',
      scoringMethodNote: 'Noch keine Prozentwerte.',
      matches: [],
    });

    const wrapper = await mountView();

    expect(wrapper.text()).toContain('Profil noch nicht bereit');
    expect(wrapper.text()).toContain('Noch mehr starke Ratings helfen.');
    expect(wrapper.text()).toContain('Medien bewerten');
  });

  it('keeps the queue usable when match hints fail to load', async () => {
    getMatchesMock.mockRejectedValue(
      new ApiRequestError('Matches sind gerade nicht verfuegbar.', {
        status: 503,
        code: 'UNAVAILABLE',
      }),
    );

    const wrapper = await mountView();

    expect(wrapper.text()).toContain('Match-Hinweise fehlen gerade');
    expect(wrapper.text()).toContain('Matches sind gerade nicht verfuegbar.');
    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('Spaeter');
  });
});
