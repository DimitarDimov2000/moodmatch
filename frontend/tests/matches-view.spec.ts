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
    expect(wrapper.text()).toContain('Aktuell 1 von 3 relevanten Medien. Mehr bestätigte Tags machen deine Empfehlungen aussagekräftiger.');
    expect(wrapper.text()).toContain('Profil verbessern');
    expect(wrapper.find(`[data-to='{"name":"profile"}']`).exists()).toBe(true);
  });
});
