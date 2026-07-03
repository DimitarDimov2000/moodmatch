import type {
  CommitmentLevel,
  ConsumptionStatus,
  MediaResponse,
  MediaType,
  MetadataOrigin,
  SourceType,
} from '@/types/api';
import { i18n } from '@/i18n';

export interface OptionItem<T extends string> {
  value: T;
  label: string;
}

const t = i18n.global.t;

export function getMediaTypeLabel(value: MediaType): string {
  return t(`labels.mediaType.${value}`);
}

export function getConsumptionStatusLabel(value: ConsumptionStatus): string {
  return t(`labels.consumptionStatus.${value}`);
}

export function getSourceTypeLabel(value: SourceType): string {
  return t(`labels.sourceType.${value}`);
}

export function getCommitmentLevelLabel(value: CommitmentLevel): string {
  return t(`labels.commitmentLevel.${value}`);
}

export function getMetadataOriginLabel(value: MetadataOrigin): string {
  return t(`labels.metadataOrigin.${value}`);
}

export function getMediaTypeOptions(): OptionItem<MediaType>[] {
  return [
    { value: 'FILM', label: getMediaTypeLabel('FILM') },
    { value: 'SERIES', label: getMediaTypeLabel('SERIES') },
    { value: 'BOOK', label: getMediaTypeLabel('BOOK') },
    { value: 'GAME', label: getMediaTypeLabel('GAME') },
    { value: 'AUDIOBOOK', label: getMediaTypeLabel('AUDIOBOOK') },
    { value: 'PODCAST', label: getMediaTypeLabel('PODCAST') },
    { value: 'VIDEO', label: getMediaTypeLabel('VIDEO') },
  ];
}
export const mediaTypeOptions = getMediaTypeOptions();

export function getConsumptionStatusOptions(): OptionItem<ConsumptionStatus>[] {
  return [
    { value: 'CONSUMED', label: getConsumptionStatusLabel('CONSUMED') },
    { value: 'WANT_TO_CONSUME', label: getConsumptionStatusLabel('WANT_TO_CONSUME') },
    { value: 'ABANDONED', label: getConsumptionStatusLabel('ABANDONED') },
    { value: 'NOT_INTERESTED', label: getConsumptionStatusLabel('NOT_INTERESTED') },
  ];
}
export const consumptionStatusOptions = getConsumptionStatusOptions();

export function getSourceTypeOptions(): OptionItem<SourceType>[] {
  return [
    { value: 'MANUAL', label: getSourceTypeLabel('MANUAL') },
    { value: 'FRIEND', label: getSourceTypeLabel('FRIEND') },
    { value: 'SOCIAL_MEDIA', label: getSourceTypeLabel('SOCIAL_MEDIA') },
    { value: 'ARTICLE', label: getSourceTypeLabel('ARTICLE') },
    { value: 'PLATFORM', label: getSourceTypeLabel('PLATFORM') },
    { value: 'EXTERNAL_SEARCH', label: getSourceTypeLabel('EXTERNAL_SEARCH') },
    { value: 'UNKNOWN', label: getSourceTypeLabel('UNKNOWN') },
  ];
}
export const sourceTypeOptions = getSourceTypeOptions();

export function getCommitmentLevelOptions(): OptionItem<CommitmentLevel>[] {
  return [
    { value: 'SHORT', label: getCommitmentLevelLabel('SHORT') },
    { value: 'MEDIUM', label: getCommitmentLevelLabel('MEDIUM') },
    { value: 'LONG', label: getCommitmentLevelLabel('LONG') },
    { value: 'UNKNOWN', label: getCommitmentLevelLabel('UNKNOWN') },
  ];
}
export const commitmentLevelOptions = getCommitmentLevelOptions();

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

export const mediaTypeLabels = createDynamicLabelLookup(getMediaTypeLabel);
export const consumptionStatusLabels = createDynamicLabelLookup(getConsumptionStatusLabel);
export const sourceTypeLabels = createDynamicLabelLookup(getSourceTypeLabel);
export const commitmentLevelLabels = createDynamicLabelLookup(getCommitmentLevelLabel);
export const metadataOriginLabels = createDynamicLabelLookup(getMetadataOriginLabel);

export function canBeFavourite(
  status: ConsumptionStatus,
  rating: number | null,
): boolean {
  return status === 'CONSUMED' && rating !== null && rating >= 4;
}

export function getMediaSubtitle(media: MediaResponse): string {
  const parts = [
    getMediaTypeLabel(media.mediaType),
    getConsumptionStatusLabel(media.consumptionStatus),
  ];

  if (media.releaseYear) {
    parts.push(String(media.releaseYear));
  }

  return parts.join(' • ');
}
