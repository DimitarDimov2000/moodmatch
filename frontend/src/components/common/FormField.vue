<script setup lang="ts">
defineProps<{
  label: string;
  hint?: string;
  error?: string;
  required?: boolean;
}>();
</script>

<template>
  <label class="form-field">
    <span class="form-field__label">
      {{ label }}
      <span
        v-if="required"
        class="form-field__required"
        aria-hidden="true"
      >*</span>
    </span>
    <span
      v-if="hint"
      class="form-field__hint"
    >
      {{ hint }}
    </span>
    <slot />
    <span
      v-if="error"
      class="form-field__error"
    >
      {{ error }}
    </span>
  </label>
</template>

<style scoped>
.form-field {
  display: grid;
  gap: 0.5rem;
}

.form-field__label {
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-field__required {
  color: var(--color-error);
}

.form-field__hint {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.form-field__error {
  color: var(--color-error);
  font-size: 0.875rem;
}
</style>

