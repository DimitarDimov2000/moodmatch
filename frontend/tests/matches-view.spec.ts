import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MatchesView from '@/views/MatchesView.vue';

const { getMatchesMock } = vi.hoisted(() => ({
  getMatchesMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :data-to="JSON.stringify(to)"><slot /></a>',
  },
}));

vi.mock('@/api/matches', () => ({
  getMatches: getMatchesMock,
}));

describe('MatchesView', () => {
  beforeEach(() => {
    getMatchesMock.mockReset();
  });

  it('renders the profile-incomplete empty state with a profile action', async () => {
    getMatchesMock.mockResolvedValue({
      interestProfile: {
        isReadyForMatching: false,
        profileRelevantMediaCount: 1,
        requiredProfileRelevantMediaCount: 3,
        explanationMessage: 'Mindestens drei bewertete konsumierte Medien werden benoetigt.',
        contributingMedia: [],
        weightedTags: [],
      },
      scoresSuppressed: true,
      explanationMessage: 'Noch keine belastbare Vergleichsbasis.',
      scoringMethodNote: 'Relativ nur bei genug Daten.',
      matches: [],
    });

    const wrapper = mount(MatchesView, {
      global: {
        stubs: {
          AppMessage: false,
          MatchScoreDisplay: false,
          MatchExplanation: false,
          CandidateSummaryCard: false,
          InterestProfileWeights: false,
        },
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('Dein Profil braucht noch mehr Signale');
    expect(wrapper.text()).toContain('Profil noch nicht bereit für Matches');
    expect(wrapper.text()).toContain('Aktuell zählen 1 von 3 relevanten Medien fürs Matching. Mehr starke Bewertungen und bestätigte Tags machen dein Profil klarer.');
    expect(wrapper.text()).toContain('Profil verbessern');
    expect(wrapper.find(`[data-to='{"name":"profile"}']`).exists()).toBe(true);
  });

  it('filters the already loaded matches by media type and tag without reloading', async () => {
    getMatchesMock.mockResolvedValue({
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
          candidate: {
            media: {
              id: 'candidate-film',
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
                  id: 'tag-sf',
                  name: 'Science-Fiction',
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
          candidateTagCount: 1,
          matchingTagCount: 1,
          matchingTags: [],
          extraCandidateTags: [],
          rawScore: '4',
          precisionFactor: '1',
          adjustedScore: '4',
          relativeScore: '82',
          explanationMessage: 'Dune passt gut.',
          candidateTagsNote: 'Tags basieren auf deiner Einschaetzung.',
        },
        {
          candidate: {
            media: {
              id: 'candidate-series',
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
                  id: 'tag-mystery',
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
          candidateTagCount: 1,
          matchingTagCount: 0,
          matchingTags: [],
          extraCandidateTags: [],
          rawScore: '0',
          precisionFactor: '0',
          adjustedScore: '0',
          relativeScore: null,
          explanationMessage: 'Silo ist noch offen.',
          candidateTagsNote: 'Tags basieren auf deiner Einschaetzung.',
        },
      ],
    });

    const wrapper = mount(MatchesView, {
      global: {
        stubs: {
          AppMessage: false,
          MatchScoreDisplay: false,
          MatchExplanation: false,
          CandidateSummaryCard: false,
          InterestProfileWeights: false,
        },
      },
    });

    await flushPromises();

    expect(getMatchesMock).toHaveBeenCalledTimes(1);
    expect(wrapper.findAll('.matches-view__highlight-item')).toHaveLength(2);
    expect(wrapper.findAll('.matches-view__highlight-rank').map((node) => node.text())).toEqual(['1', '2']);
    expect(wrapper.findAll('.matches-view__match-card')).toHaveLength(2);

    await wrapper.get('select[name="matchMediaTypeFilter"]').setValue('SERIES');

    expect(wrapper.findAll('.matches-view__match-card')).toHaveLength(1);
    expect(wrapper.text()).toContain('Silo');
    expect(wrapper.text()).toContain('Aktuell 1 von 2 Matches mit den gewählten Filtern.');

    await wrapper.get('select[name="matchTagFilter"]').setValue('tag-sf');

    expect(wrapper.findAll('.matches-view__match-card')).toHaveLength(0);
    expect(wrapper.text()).toContain('Keine Matches für die aktuellen Filter');
    expect(getMatchesMock).toHaveBeenCalledTimes(1);
  });
});
