import type { CommitmentLevel, ConsumptionStatus, MediaType } from '@/types/api';

const numberFormatter = new Intl.NumberFormat('de-DE', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

const percentFormatter = new Intl.NumberFormat('de-DE', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
});

export const mediaTypeLabels: Record<MediaType, string> = {
  FILM: 'Film',
  SERIES: 'Serie',
  BOOK: 'Buch',
  GAME: 'Spiel',
};

export const consumptionStatusLabels: Record<ConsumptionStatus, string> = {
  CONSUMED: 'Konsumiert',
  WANT_TO_CONSUME: 'Kandidat',
  NOT_INTERESTED: 'Kein Interesse',
  ABANDONED: 'Abgebrochen',
};

export const commitmentLevelLabels: Record<CommitmentLevel, string> = {
  SHORT: 'Kurz',
  MEDIUM: 'Mittel',
  LONG: 'Lang',
  UNKNOWN: 'Unbekannt',
};

export function formatDecimal(value: string | null): string | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);

  if (Number.isNaN(parsed)) {
    return value;
  }

  return numberFormatter.format(parsed);
}

export function formatPercentage(value: string | null): string | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);

  if (Number.isNaN(parsed)) {
    return `${value} %`;
  }

  return `${percentFormatter.format(parsed)} %`;
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
