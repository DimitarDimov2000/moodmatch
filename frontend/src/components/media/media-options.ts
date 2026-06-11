import type {
  CommitmentLevel,
  ConsumptionStatus,
  MediaResponse,
  MediaType,
  MetadataOrigin,
  SourceType,
} from '@/types/api';

export interface OptionItem<T extends string> {
  value: T;
  label: string;
}

export const mediaTypeOptions: OptionItem<MediaType>[] = [
  { value: 'FILM', label: 'Film' },
  { value: 'SERIES', label: 'Serie' },
  { value: 'BOOK', label: 'Buch' },
  { value: 'GAME', label: 'Spiel' },
];

export const consumptionStatusOptions: OptionItem<ConsumptionStatus>[] = [
  { value: 'CONSUMED', label: 'Konsumiert' },
  { value: 'WANT_TO_CONSUME', label: 'Moechte ich konsumieren' },
  { value: 'ABANDONED', label: 'Abgebrochen' },
  { value: 'NOT_INTERESTED', label: 'Kein Interesse' },
];

export const sourceTypeOptions: OptionItem<SourceType>[] = [
  { value: 'MANUAL', label: 'Manuell' },
  { value: 'FRIEND', label: 'Freunde' },
  { value: 'SOCIAL_MEDIA', label: 'Social Media' },
  { value: 'ARTICLE', label: 'Artikel' },
  { value: 'PLATFORM', label: 'Plattform' },
  { value: 'EXTERNAL_SEARCH', label: 'Externe Suche' },
  { value: 'UNKNOWN', label: 'Unbekannt' },
];

export const commitmentLevelOptions: OptionItem<CommitmentLevel>[] = [
  { value: 'SHORT', label: 'Kurz' },
  { value: 'MEDIUM', label: 'Mittel' },
  { value: 'LONG', label: 'Lang' },
  { value: 'UNKNOWN', label: 'Unbekannt' },
];

export const metadataOriginLabels: Record<MetadataOrigin, string> = {
  MANUAL: 'Manuell gepflegt',
  IMPORTED: 'Importiert',
  IMPORTED_AND_EDITED: 'Importiert und bearbeitet',
};

export const mediaTypeLabels = createLabelLookup(mediaTypeOptions);
export const consumptionStatusLabels = createLabelLookup(consumptionStatusOptions);
export const sourceTypeLabels = createLabelLookup(sourceTypeOptions);
export const commitmentLevelLabels = createLabelLookup(commitmentLevelOptions);

function createLabelLookup<T extends string>(
  options: OptionItem<T>[],
): Record<T, string> {
  return Object.fromEntries(options.map((option) => [option.value, option.label])) as Record<T, string>;
}

export function canBeFavourite(
  status: ConsumptionStatus,
  rating: number | null,
): boolean {
  return status === 'CONSUMED' && rating !== null && rating >= 4;
}

export function getMediaSubtitle(media: MediaResponse): string {
  const parts = [
    mediaTypeLabels[media.mediaType],
    consumptionStatusLabels[media.consumptionStatus],
  ];

  if (media.releaseYear) {
    parts.push(String(media.releaseYear));
  }

  return parts.join(' • ');
}

