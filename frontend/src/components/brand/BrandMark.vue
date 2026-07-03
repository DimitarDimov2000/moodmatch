<script setup lang="ts">
import { computed, type PropType } from 'vue';

import moodmatchIconWhiteUrl from '@/assets/brand/moodmatch_icon_white.svg';
import moodmatchLogoDarkUrl from '@/assets/brand/moodmatch_logo_dark.svg';
import { useTheme, type ResolvedTheme } from '@/composables/useTheme';

const props = defineProps({
  testId: {
    type: String,
    default: 'brand-mark',
  },
  theme: {
    type: String as PropType<ResolvedTheme>,
    default: undefined,
  },
});

const { resolvedTheme } = useTheme();
const activeTheme = computed(() => props.theme ?? resolvedTheme.value);

const brandMarkUrl = computed(() =>
  activeTheme.value === 'light'
    ? moodmatchIconWhiteUrl
    : moodmatchLogoDarkUrl,
);
</script>

<template>
  <img
    :data-testid="props.testId"
    class="brand-mark"
    :class="`brand-mark--${activeTheme}`"
    :src="brandMarkUrl"
    alt=""
    decoding="async"
    loading="eager"
  >
</template>

<style scoped>
.brand-mark {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
}

.brand-mark--light {
  transform: scale(1.2);
  transform-origin: center;
}
</style>
