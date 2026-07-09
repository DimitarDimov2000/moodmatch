<script setup lang="ts">
import { i18n } from '@/i18n';

const props = defineProps<{
  pending?: boolean;
  detailsExpanded?: boolean;
}>();

const emit = defineEmits<{
  like: [];
  skip: [];
  details: [];
}>();

const { t } = i18n.global;
</script>

<template>
  <section class="swipe-decision-controls page-card">
    <div
      class="swipe-decision-controls__buttons"
      role="group"
      :aria-label="t('swipeCards.actions')"
    >
      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--skip"
        type="button"
        :aria-label="t('swipeCards.rejectLabel')"
        :disabled="props.pending"
        @click="emit('skip')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >×</span>
        <span class="swipe-decision-controls__label">{{ t('common.actions.notNow') }}</span>
        <span class="swipe-decision-controls__hint">{{ t('swipeCards.left') }}</span>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--details"
        type="button"
        :aria-expanded="props.detailsExpanded ? 'true' : 'false'"
        :aria-label="t('swipeCards.detailsLabel')"
        :disabled="props.pending"
        @click="emit('details')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >⌄</span>
        <span class="swipe-decision-controls__label">
          {{ props.detailsExpanded ? t('common.actions.less') : t('common.actions.details') }}
        </span>
        <span class="swipe-decision-controls__hint">Enter</span>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--like"
        type="button"
        :aria-label="t('swipeCards.likeLabel')"
        :disabled="props.pending"
        @click="emit('like')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >→</span>
        <span class="swipe-decision-controls__label">{{ t('common.actions.like') }}</span>
        <span class="swipe-decision-controls__hint">{{ t('swipeCards.right') }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.swipe-decision-controls {
  padding: 0.72rem;
  border-color: var(--theme-swipe-panel-border);
  background: var(--theme-swipe-panel-background);
  backdrop-filter: blur(18px);
}

.swipe-decision-controls__buttons {
  display: grid;
  gap: 0.58rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.swipe-decision-controls__button {
  display: grid;
  justify-items: center;
  gap: 0.14rem;
  min-height: 3.75rem;
  padding: 0.55rem 0.5rem;
  border-radius: 1.4rem;
  border-width: 1px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.swipe-decision-controls__icon {
  font-size: 1.08rem;
  font-weight: 700;
  line-height: 1;
}

.swipe-decision-controls__label {
  font-weight: 700;
  font-size: 0.94rem;
}

.swipe-decision-controls__hint {
  color: var(--theme-swipe-panel-soft-text);
  font-size: 0.76rem;
}

.swipe-decision-controls__button--skip {
  background: var(--theme-swipe-skip-button-background);
  border-color: var(--theme-swipe-skip-button-border);
  color: var(--theme-swipe-skip-button-text);
}

.swipe-decision-controls__button--details {
  background: var(--theme-swipe-details-button-background);
  border-color: var(--theme-swipe-details-button-border);
  color: var(--theme-swipe-details-button-text);
}

.swipe-decision-controls__button--like {
  background: var(--theme-swipe-like-button-background);
  border-color: var(--theme-swipe-like-button-border);
  color: #fff;
  box-shadow: var(--theme-swipe-like-button-shadow);
}

@media (max-width: 640px) {
  .swipe-decision-controls {
    position: sticky;
    bottom: max(0.7rem, env(safe-area-inset-bottom));
    z-index: 2;
    box-shadow: var(--theme-swipe-sticky-shadow);
  }
}

@media (max-width: 380px) {
  .swipe-decision-controls__button {
    min-height: 3.8rem;
    padding-inline: 0.42rem;
  }

  .swipe-decision-controls__label {
    font-size: 0.88rem;
  }

  .swipe-decision-controls__hint {
    font-size: 0.72rem;
  }
}
</style>
