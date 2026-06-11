import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiRequestError } from '@/api/client';
import SwipeView from '@/views/SwipeView.vue';
import type { CandidateSelectionResponse, MatchingResponse } from '@/types/api';

const { pushSpy } = vi.hoisted(() => ({
  pushSpy: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :data-to="JSON.stringify(to)"><slot /></a>',
  },
  useRouter: () => ({
    push: pushSpy,
  }),
}));

const {
  listCandidatesMock,
  getMatchesMock,
  updateMediaStatusMock,
} = vi.hoisted(() => ({
  listCandidatesMock: vi.fn(),
  getMatchesMock: vi.fn(),
  updateMediaStatusMock: vi.fn(),
}));

vi.mock('@/api/candidates', () => ({
  listCandidates: listCandidatesMock,
}));

vi.mock('@/api/matches', () => ({
  getMatches: getMatchesMock,
}));

vi.mock('@/api/media', () => ({
  updateMediaStatus: updateMediaStatusMock,
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
        coverUrl: null,
        tags: [
          {
            id: 'tag-1',
            name: 'Zukunft',
            category: 'SETTING',
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
      candidateTagCount: 1,
      matchingTagCount: 1,
      matchingTags: [],
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
      candidateTagCount: 0,
      matchingTagCount: 0,
      matchingTags: [],
      extraCandidateTags: [],
      rawScore: null,
      precisionFactor: null,
      adjustedScore: null,
      relativeScore: null,
      explanationMessage: 'Silo braucht noch erwartete Tags fuer eine bessere Einordnung.',
      candidateTagsNote: 'Kandidaten-Tags basieren auf deiner Einschaetzung.',
    },
  ],
};

async function mountView() {
  const wrapper = mount(SwipeView);
  await flushPromises();
  return wrapper;
}

describe('SwipeView', () => {
  beforeEach(() => {
    pushSpy.mockReset();
    listCandidatesMock.mockReset();
    getMatchesMock.mockReset();
    updateMediaStatusMock.mockReset();

    listCandidatesMock.mockResolvedValue(candidateResponse);
    getMatchesMock.mockResolvedValue(matchingResponse);
    updateMediaStatusMock.mockResolvedValue(candidateResponse.candidates[0].media);
  });

  it('renders the first candidate with progress and match context', async () => {
    const wrapper = await mountView();

    expect(listCandidatesMock).toHaveBeenCalledTimes(1);
    expect(getMatchesMock).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('Karte 1 von 2');
    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('Relativer Match-Score verfuegbar');
    expect(wrapper.text()).toContain('Dune passt gut zu deinem aktuellen Profil.');
    expect(wrapper.text()).toContain('Liken und Ueberspringen bleiben in dieser Runde lokal.');
  });

  it('supports keyboard shortcuts for like, skip, and details', async () => {
    const wrapper = await mountView();

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    await flushPromises();

    expect(wrapper.text()).toContain('Karte 2 von 2');
    expect(wrapper.text()).toContain('Silo');
    expect(wrapper.text()).toContain('Geliket');
    expect(wrapper.text()).toContain('1');

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    await flushPromises();

    expect(pushSpy).toHaveBeenCalledWith({
      name: 'media-detail',
      params: {
        id: 'candidate-2',
      },
    });

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 's' }));
    await flushPromises();

    expect(wrapper.text()).toContain('Runde abgeschlossen');
    expect(wrapper.text()).toContain('1 Kandidaten lokal geliket, 0 abgelehnt und 1 uebersprungen');
  });

  it('persists reject as NOT_INTERESTED and advances the queue', async () => {
    const wrapper = await mountView();

    await wrapper.get('.swipe-decision-controls__button--reject').trigger('click');
    await flushPromises();

    expect(updateMediaStatusMock).toHaveBeenCalledWith('candidate-1', {
      consumptionStatus: 'NOT_INTERESTED',
      rating: null,
      isFavourite: false,
      confirmDestructiveChange: false,
    });
    expect(wrapper.text()).toContain('Silo');
    expect(wrapper.text()).toContain('Abgelehnt');
    expect(wrapper.text()).toContain('1');
  });

  it('keeps the queue usable when match hints fail to load', async () => {
    getMatchesMock.mockRejectedValue(
      new ApiRequestError('Matches sind gerade nicht verfuegbar.', {
        status: 503,
        code: 'UNAVAILABLE',
      }),
    );

    const wrapper = await mountView();

    expect(wrapper.text()).toContain('Match-Hinweise derzeit nicht verfuegbar');
    expect(wrapper.text()).toContain('Matches sind gerade nicht verfuegbar.');
    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('Match-Hinweise derzeit nicht verfuegbar');
  });
});
