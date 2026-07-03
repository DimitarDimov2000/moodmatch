import type { SwipeQueueItem } from '@/types/swipe';

export interface SwipeInsightSummary {
  valueLabel: string;
  valueCaption: string;
  valueTone: 'success' | 'accent' | 'muted';
  headline: string;
  supportingCopy: string;
  detailsIntro: string;
  reasonChips: string[];
}

export interface SwipeInsightOptions {
  matchInsightsAvailable: boolean;
  profileReady: boolean | null;
  scoresSuppressed: boolean;
}

export function summarizeSwipeInsight(
  item: SwipeQueueItem,
  options: SwipeInsightOptions,
): SwipeInsightSummary {
  const matchingTagNames = item.match?.matchingTags.map((entry) => entry.tag.name) ?? [];
  const score = toNumber(item.match?.relativeScore ?? null);

  if (!options.matchInsightsAvailable) {
    return {
      valueLabel: 'Spaeter',
      valueCaption: 'Match-Hinweise laden noch',
      valueTone: 'muted',
      headline: 'Die persoenliche Einordnung wird noch geladen.',
      supportingCopy: 'Du kannst den Titel schon sortieren und spaeter mehr Details aufklappen.',
      detailsIntro: 'Sobald die Match-Hinweise bereit sind, zeigen wir hier genauer, warum der Titel zu dir passen koennte.',
      reasonChips: buildReasonChips(item, options, ['Match-Hinweise spaeter']),
    };
  }

  if (options.profileReady === false || options.scoresSuppressed) {
    return {
      valueLabel: 'Lernt',
      valueCaption: 'Mehr starke Ratings helfen',
      valueTone: 'muted',
      headline: 'Dein Profil baut sich noch auf, deshalb bleibt diese Empfehlung bewusst vorsichtig.',
      supportingCopy: 'Sobald du mehr konsumierte Medien hoch bewertest, werden Prozent und Begruendungen praeziser.',
      detailsIntro: 'Die vorhandenen Signale reichen fuer erste Vorschlaege, aber noch nicht fuer eine starke Sicherheit.',
      reasonChips: buildReasonChips(item, options, ['Frueher Vorschlag', 'Mehr Ratings helfen']),
    };
  }

  if (!item.candidate.isCompleteForMatching) {
    return {
      valueLabel: 'Offen',
      valueCaption: 'Mehr Tags helfen',
      valueTone: 'muted',
      headline: 'Der Titel wirkt spannend, braucht aber noch bestaetigte Tags fuer eine sichere Einordnung.',
      supportingCopy: 'Wir tun nicht so, als waeren wir sicher: mehr Tags und Bewertungen machen diese Empfehlung besser.',
      detailsIntro: 'Im Moment zeigen wir lieber einen ehrlichen Zwischenstand statt eine kuenstlich genaue Prozentzahl.',
      reasonChips: buildReasonChips(item, options, ['Mehr Tags helfen', 'Vorsichtige Empfehlung']),
    };
  }

  if (score !== null) {
    const topTags = matchingTagNames.slice(0, 2);
    const hasStrongScore = score >= 80;
    const hasGoodScore = score >= 60;

    return {
      valueLabel: `${Math.round(score)}%`,
      valueCaption: hasStrongScore ? 'Starker Fit' : hasGoodScore ? 'Gute Chance' : 'Solider Treffer',
      valueTone: hasStrongScore ? 'success' : hasGoodScore ? 'accent' : 'muted',
      headline:
        topTags.length > 0
          ? `Passt besonders wegen ${joinWithUnd(topTags)}.`
          : 'Mehrere Signale sprechen fuer diesen Titel.',
      supportingCopy:
        topTags.length > 0
          ? 'Die ueberschneidenden Tags machen den Vorschlag schnell greifbar, ohne die Karte zu ueberladen.'
          : 'Die Empfehlung basiert auf der aktuellen Profilueberschneidung und bleibt leicht erklaerbar.',
      detailsIntro:
        topTags.length > 0
          ? `Dein Profil und dieser Titel treffen sich vor allem bei ${joinWithUnd(topTags)}.`
          : 'Der Titel teilt mehrere Signale mit deinem aktuellen Profil.',
      reasonChips: buildReasonChips(item, options, [hasStrongScore ? 'Starker Treffer' : 'Passt gut']),
    };
  }

  if ((item.match?.matchingTagCount ?? 0) > 0) {
    return {
      valueLabel: 'Nahe',
      valueCaption: 'Gute Signale, noch ohne Prozent',
      valueTone: 'accent',
      headline:
        matchingTagNames.length > 0
          ? `Schon spannend wegen ${joinWithUnd(matchingTagNames.slice(0, 2))}.`
          : 'Es gibt bereits gute Signale, aber noch keine belastbare Prozentzahl.',
      supportingCopy: 'Wir zeigen lieber eine ehrliche Tendenz als eine Zahl, die zu sicher wirken wuerde.',
      detailsIntro: 'Einige Tags passen bereits gut, nur die Vergleichsbasis fuer eine Prozentzahl ist noch zu duenn.',
      reasonChips: buildReasonChips(item, options, ['Gute Signale', 'Noch ohne Prozent']),
    };
  }

  return {
    valueLabel: 'Neu',
    valueCaption: 'Eher ein Entdeckungstipp',
    valueTone: 'muted',
    headline: 'Der Titel liegt etwas ausserhalb deiner staerksten bisherigen Muster.',
    supportingCopy: 'Gerade deshalb kann er interessant sein, wenn du bewusst etwas Neues ausprobieren willst.',
    detailsIntro: 'Es gibt aktuell wenig direkte Ueberschneidung mit deinem Profil, deshalb zeigen wir ihn als ehrlichen Entdeckungstipp.',
    reasonChips: buildReasonChips(item, options, ['Entdeckungstipp', 'Wenig Ueberschneidung']),
  };
}

function buildReasonChips(
  item: SwipeQueueItem,
  options: SwipeInsightOptions,
  fallbackChips: string[],
): string[] {
  const chips: string[] = [];
  const matchingTagNames = item.match?.matchingTags.map((entry) => entry.tag.name) ?? [];
  const candidateTagNames = item.candidate.media.tags.map((tag) => tag.name);

  chips.push(...matchingTagNames.slice(0, 2));

  if (chips.length < 2) {
    chips.push(...candidateTagNames.slice(0, 2));
  }

  if ((item.match?.matchingTagCount ?? 0) > 0 && item.match?.candidateTagCount) {
    chips.push(`${item.match.matchingTagCount}/${item.match.candidateTagCount} Tags passen`);
  }

  chips.push(...fallbackChips);

  if (!options.matchInsightsAvailable) {
    chips.push('Profilsignal spaeter');
  }

  return [...new Set(chips.filter(Boolean))].slice(0, 4);
}

function joinWithUnd(values: string[]): string {
  if (values.length <= 1) {
    return values[0] ?? '';
  }

  if (values.length === 2) {
    return `${values[0]} und ${values[1]}`;
  }

  return `${values.slice(0, -1).join(', ')} und ${values.at(-1)}`;
}

function toNumber(value: string | null): number | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}
