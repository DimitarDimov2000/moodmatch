import { flushPromises, mount } from "@vue/test-utils";
import { createPinia } from "pinia";
import { createMemoryHistory, createRouter } from "vue-router";
import { vi } from "vitest";

import App from "@/App.vue";
import CandidatesView from "@/views/CandidatesView.vue";
import DashboardView from "@/views/DashboardView.vue";
import ExternalSearchView from "@/views/ExternalSearchView.vue";
import MediaCreateView from "@/views/MediaCreateView.vue";
import MediaDetailView from "@/views/MediaDetailView.vue";
import MediaLibraryView from "@/views/MediaLibraryView.vue";
import MatchesView from "@/views/MatchesView.vue";
import NotFoundView from "@/views/NotFoundView.vue";
import ProfileView from "@/views/ProfileView.vue";
import SwipeView from "@/views/SwipeView.vue";

vi.mock("@/api/media", () => ({
  listMedia: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/profile", () => ({
  getProfile: vi.fn().mockResolvedValue({
    isReadyForMatching: false,
    profileRelevantMediaCount: 0,
    requiredProfileRelevantMediaCount: 3,
    explanationMessage: "Noch zu wenig Daten.",
    contributingMedia: [],
    weightedTags: [],
  }),
}));

vi.mock("@/api/candidates", () => ({
  listCandidates: vi.fn().mockResolvedValue({
    candidates: [],
  }),
}));

vi.mock("@/api/matches", () => ({
  getMatches: vi.fn().mockResolvedValue({
    interestProfile: {
      isReadyForMatching: false,
      profileRelevantMediaCount: 0,
      requiredProfileRelevantMediaCount: 3,
      explanationMessage: "Noch zu wenig Daten.",
      contributingMedia: [],
      weightedTags: [],
    },
    scoresSuppressed: true,
    explanationMessage: "Noch keine Vergleichbarkeit.",
    scoringMethodNote: "Relativ nur bei genug Daten.",
    matches: [],
  }),
}));

async function mountApp() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/", name: "dashboard", component: DashboardView },
      { path: "/profile", name: "profile", component: ProfileView },
      { path: "/external-search", name: "external-search", component: ExternalSearchView },
      { path: "/media", name: "media-list", component: MediaLibraryView },
      { path: "/media/new", name: "media-create", component: MediaCreateView },
      { path: "/media/:id", name: "media-detail", component: MediaDetailView },
      { path: "/candidates", name: "candidates", component: CandidatesView },
      { path: "/matches", name: "matches", component: MatchesView },
      { path: "/swipe", name: "swipe", component: SwipeView },
      { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundView },
    ],
  });

  await router.push("/");
  await router.isReady();

  return mount(App, {
    global: {
      plugins: [createPinia(), router],
    },
  });
}

describe("App", () => {
  it("renders the application shell and dashboard route", async () => {
    const wrapper = await mountApp();
    await flushPromises();

    expect(wrapper.text()).toContain("MoodMatch");
    expect(wrapper.text()).toContain("Profil");
    expect(wrapper.text()).toContain("Suche");
    expect(wrapper.text()).toContain("Medien");
    expect(wrapper.text()).toContain("Kandidaten");
    expect(wrapper.text()).toContain("Matches");
    expect(wrapper.text()).toContain("Swipe");
    expect(wrapper.text()).toContain("Dashboard");
  });
});
