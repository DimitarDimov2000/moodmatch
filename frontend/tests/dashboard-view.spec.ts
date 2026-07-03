import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import DashboardView from "@/views/DashboardView.vue";

const { listMediaMock, getProfileMock, listCandidatesMock, getMatchesMock } =
  vi.hoisted(() => ({
    listMediaMock: vi.fn(),
    getProfileMock: vi.fn(),
    listCandidatesMock: vi.fn(),
    getMatchesMock: vi.fn(),
  }));

vi.mock("vue-router", () => ({
  RouterLink: {
    name: "RouterLink",
    props: ["to"],
    template: '<a :data-to="JSON.stringify(to)"><slot /></a>',
  },
}));

vi.mock("@/api/media", () => ({
  listMedia: listMediaMock,
}));

vi.mock("@/api/profile", () => ({
  getProfile: getProfileMock,
}));

vi.mock("@/api/candidates", () => ({
  listCandidates: listCandidatesMock,
}));

vi.mock("@/api/matches", () => ({
  getMatches: getMatchesMock,
}));

describe("DashboardView", () => {
  beforeEach(() => {
    listMediaMock.mockReset();
    getProfileMock.mockReset();
    listCandidatesMock.mockReset();
    getMatchesMock.mockReset();

    listMediaMock.mockResolvedValue([
      {
        id: "media-1",
        title: "Arrival",
        originalTitle: null,
        description: null,
        mediaType: "FILM",
        consumptionStatus: "CONSUMED",
        isFavourite: false,
        rating: 5,
        sourceType: "MANUAL",
        sourceNote: null,
        commitmentLevel: "MEDIUM",
        releaseYear: 2016,
        coverUrl: null,
        externalSourceName: null,
        externalSourceId: null,
        externalSourceUrl: null,
        metadataOrigin: "MANUAL",
        tags: [],
        externalReferences: [],
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
      {
        id: "media-2",
        title: "Control",
        originalTitle: null,
        description: null,
        mediaType: "GAME",
        consumptionStatus: "WANT_TO_CONSUME",
        isFavourite: false,
        rating: null,
        sourceType: "MANUAL",
        sourceNote: null,
        commitmentLevel: "MEDIUM",
        releaseYear: 2019,
        coverUrl: null,
        externalSourceName: null,
        externalSourceId: null,
        externalSourceUrl: null,
        metadataOrigin: "MANUAL",
        tags: [],
        externalReferences: [],
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ]);

    getProfileMock.mockResolvedValue({
      isReadyForMatching: true,
      profileRelevantMediaCount: 3,
      requiredProfileRelevantMediaCount: 3,
      explanationMessage: "Profil ist bereit.",
      contributingMedia: [],
      weightedTags: [
        {
          tag: {
            id: "tag-1",
            name: "Staunen",
            slug: "staunen",
            category: "EXPERIENCE",
            description: null,
          },
          weight: "3.5",
        },
      ],
    });

    listCandidatesMock.mockResolvedValue({
      candidates: [
        {
          media: {
            id: "candidate-1",
            title: "Dune",
            mediaType: "FILM",
            consumptionStatus: "WANT_TO_CONSUME",
            commitmentLevel: "LONG",
            isFavourite: false,
            rating: null,
            releaseYear: 2021,
            coverUrl: null,
            tags: [],
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z",
          },
          isCompleteForMatching: true,
        },
        {
          media: {
            id: "candidate-2",
            title: "Severance",
            mediaType: "SERIES",
            consumptionStatus: "WANT_TO_CONSUME",
            commitmentLevel: "MEDIUM",
            isFavourite: false,
            rating: null,
            releaseYear: 2022,
            coverUrl: null,
            tags: [],
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z",
          },
          isCompleteForMatching: false,
        },
      ],
    });

    getMatchesMock.mockResolvedValue({
      interestProfile: {
        isReadyForMatching: true,
        profileRelevantMediaCount: 3,
        requiredProfileRelevantMediaCount: 3,
        explanationMessage: "Profil ist bereit.",
        contributingMedia: [],
        weightedTags: [],
      },
      scoresSuppressed: false,
      explanationMessage: "Matches sind verfuegbar.",
      scoringMethodNote: "Relativ bei genug Daten.",
      matches: [
        {
          candidate: {
            media: {
              id: "candidate-1",
              title: "Dune",
              mediaType: "FILM",
              consumptionStatus: "WANT_TO_CONSUME",
              commitmentLevel: "LONG",
              isFavourite: false,
              rating: null,
              releaseYear: 2021,
              coverUrl: null,
              tags: [],
              createdAt: "2026-01-01T00:00:00Z",
              updatedAt: "2026-01-01T00:00:00Z",
            },
            isCompleteForMatching: true,
          },
          candidateTagCount: 3,
          matchingTagCount: 2,
          matchingTags: [],
          extraCandidateTags: [],
          rawScore: "5",
          precisionFactor: "0.67",
          adjustedScore: "3.35",
          relativeScore: "82",
          explanationMessage: "Dune teilt mehrere starke Profil-Tags.",
          candidateTagsNote:
            "Kandidaten-Tags basieren auf deiner Einschaetzung.",
        },
      ],
    });
  });

  it("renders dashboard counts and route links from existing api calls", async () => {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          AppMessage: false,
          DashboardSummaryCard: false,
        },
      },
    });

    await flushPromises();

    expect(listMediaMock).toHaveBeenCalledTimes(1);
    expect(getProfileMock).toHaveBeenCalledTimes(1);
    expect(listCandidatesMock).toHaveBeenCalledTimes(1);
    expect(getMatchesMock).toHaveBeenCalledTimes(1);

    expect(wrapper.text()).toContain("Lokale Mediensammlung");
    expect(wrapper.text()).toContain("2");
    expect(wrapper.text()).toContain("3/3");
    expect(wrapper.text()).toContain("Vorschläge für später");
    expect(wrapper.text()).toContain("Erklärbare Vergleiche");
    expect(wrapper.text()).toContain("Dune führt aktuell deine Matches an.");
  });
});
