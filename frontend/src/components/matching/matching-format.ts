import type { CommitmentLevel, ConsumptionStatus, MediaType } from '@/types/api';
import { getIntlLocale, i18n } from '@/i18n';

function createNumberFormatter(maximumFractionDigits: number): Intl.NumberFormat {
  return new Intl.NumberFormat(getIntlLocale(), {
    minimumFractionDigits: 0,
    maximumFractionDigits,
  });
}

export function getMatchingMediaTypeLabel(value: MediaType): string {
  return i18n.global.t(`labels.mediaType.${value}`);
}

export function getMatchingConsumptionStatusLabel(value: ConsumptionStatus): string {
  return i18n.global.t(`labels.consumptionStatus.${value}`);
}

export function getMatchingCommitmentLevelLabel(value: CommitmentLevel): string {
  return i18n.global.t(`labels.commitmentLevel.${value}`);
}

function createDynamicLabelLookup<T extends string>(
  resolveLabel: (value: T) => string,
): Record<T, string> {
  return new Proxy({} as Record<T, string>, {
    get: (_, property) => {
      if (typeof property !== 'string' || property.startsWith('__v_')) {
        return undefined;
      }

      return resolveLabel(property as T);
    },
  });
}

export const mediaTypeLabels = createDynamicLabelLookup(getMatchingMediaTypeLabel);
export const consumptionStatusLabels = createDynamicLabelLookup(getMatchingConsumptionStatusLabel);
export const commitmentLevelLabels = createDynamicLabelLookup(getMatchingCommitmentLevelLabel);

export function formatDecimal(value: string | null): string | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);

  if (Number.isNaN(parsed)) {
    return value;
  }

  return createNumberFormatter(2).format(parsed);
}

export function formatPercentage(value: string | null): string | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);

  if (Number.isNaN(parsed)) {
    return `${value}${i18n.global.t('formatting.percentSuffix')}`;
  }

  return `${createNumberFormatter(1).format(parsed)}${i18n.global.t('formatting.percentSuffix')}`;
}

export function getScoreTone(score: string | null): 'muted' | 'warning' | 'accent' | 'success' {
  if (score === null) {
    return 'muted';
  }

  const parsed = Number(score);

  if (Number.isNaN(parsed) || parsed <= 0) {
    return 'muted';
  }

  if (parsed >= 80) {
    return 'success';
  }

  if (parsed >= 60) {
    return 'accent';
  }

  return 'warning';
}
