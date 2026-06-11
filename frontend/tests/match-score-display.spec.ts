import { mount } from "@vue/test-utils";

import MatchScoreDisplay from "@/components/matching/MatchScoreDisplay.vue";

describe("MatchScoreDisplay", () => {
  it("renders a formatted relative score", () => {
    const wrapper = mount(MatchScoreDisplay, {
      props: {
        score: "87.5",
      },
    });

    expect(wrapper.text()).toContain("87,5 %");
    expect(wrapper.text()).toContain("Relativer Match-Score");
  });

  it("renders a clear no-score state instead of zero", () => {
    const wrapper = mount(MatchScoreDisplay, {
      props: {
        score: null,
      },
    });

    expect(wrapper.text()).toContain("Keine Prozentangabe");
    expect(wrapper.text()).toContain(
      "Prozentwerte erscheinen erst bei genug vergleichbaren Daten.",
    );
    expect(wrapper.text()).not.toContain("0 %");
  });
});
