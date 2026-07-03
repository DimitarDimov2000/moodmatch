<script setup lang="ts">
import { computed } from "vue";
import { i18n } from "@/i18n";

const props = withDefaults(
  defineProps<{
    title: string;
    description: string;
    tone?: "neutral" | "error" | "warning" | "info";
  }>(),
  {
    tone: "neutral",
  },
);
const { t } = i18n.global;

const toneLabel = computed(() => {
  switch (props.tone) {
    case "error":
      return t("common.tone.error");
    case "warning":
      return t("common.tone.warning");
    case "info":
      return t("common.tone.status");
    default:
      return t("common.tone.info");
  }
});
</script>

<template>
  <div
    class="app-message page-card state-panel"
    :class="`app-message--${props.tone}`"
    :role="props.tone === 'error' ? 'alert' : 'status'"
  >
    <div class="app-message__content">
      <p class="app-message__eyebrow eyebrow">
        {{ toneLabel }}
      </p>
      <h2 class="app-message__title">
        {{ props.title }}
      </h2>
      <p class="app-message__description">
        {{ props.description }}
      </p>
    </div>

    <div
      v-if="$slots.default"
      class="app-message__actions"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
.app-message {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.9rem 1rem;
  padding: clamp(0.95rem, 2.2vw, 1.15rem) clamp(0.95rem, 2.2vw, 1.15rem)
    clamp(0.95rem, 2.2vw, 1.15rem) clamp(1.15rem, 2.5vw, 1.4rem);
}

.app-message::before {
  content: "";
  position: absolute;
  top: 0.95rem;
  bottom: 0.95rem;
  left: 1rem;
  width: 3px;
  border-radius: var(--radius-full);
  background: var(--theme-message-rail-background);
}

.app-message--neutral {
  background: var(--theme-message-neutral-background);
}

.app-message--neutral::before {
  background: color-mix(in srgb, var(--color-info) 60%, white 8%);
}

.app-message--error {
  background: var(--theme-message-error-background);
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
}

.app-message--error::before {
  background: var(--color-error);
}

.app-message--warning {
  background: var(--theme-message-warning-background);
  border-color: color-mix(
    in srgb,
    var(--color-warning) 30%,
    var(--color-border)
  );
}

.app-message--warning::before {
  background: var(--color-warning);
}

.app-message--info {
  background: var(--theme-message-info-background);
  border-color: color-mix(in srgb, var(--color-info) 30%, var(--color-border));
}

.app-message--info::before {
  background: var(--color-info);
}

.app-message__eyebrow,
.app-message__title {
  margin: 0;
}

.app-message__content {
  display: grid;
  gap: 0.4rem;
  min-width: 0;
  padding-left: 0.5rem;
}

.app-message__title {
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 1.3;
}

.app-message__description {
  max-width: 68ch;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.94rem;
  line-height: 1.55;
  white-space: pre-line;
}

.app-message__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  align-self: center;
  gap: 0.65rem;
  min-width: fit-content;
}

@media (max-width: 760px) {
  .app-message {
    grid-template-columns: 1fr;
  }

  .app-message__content {
    padding-left: 0.35rem;
  }

  .app-message__actions {
    justify-content: flex-start;
    align-self: start;
  }
}
</style>
