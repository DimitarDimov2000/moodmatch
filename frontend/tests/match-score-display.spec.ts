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
    expect(wrapper.text()).toContain("Verglichen mit dieser Auswahl");
    expect(wrapper.classes()).toContain("match-score-display--success");
  });

  it("maps low positive percentages to a warm low-score state", () => {
    const wrapper = mount(MatchScoreDisplay, {
      props: {
        score: "24",
      },
    });

    expect(wrapper.classes()).toContain("match-score-display--low");
  });

  it("renders a clear no-score state instead of zero", () => {
    const wrapper = mount(MatchScoreDisplay, {
      props: {
        score: null,
      },
    });

    expect(wrapper.text()).toContain("Noch offen");
    expect(wrapper.text()).toContain(
      "Noch ohne belastbare Prozentangabe.",
    );
    expect(wrapper.text()).not.toContain("0 %");
  });

  it("hides numeric values when score output is suppressed", () => {
    const wrapper = mount(MatchScoreDisplay, {
      props: {
        score: "82",
        suppressed: true,
      },
    });

    expect(wrapper.text()).toContain("Noch offen");
    expect(wrapper.text()).toContain(
      "Noch ohne belastbare Prozentangabe.",
    );
    expect(wrapper.text()).not.toContain("82");
  });
});
