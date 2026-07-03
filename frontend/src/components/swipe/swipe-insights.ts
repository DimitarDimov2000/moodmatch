import type { SwipeQueueItem } from '@/types/swipe';
import { i18n } from '@/i18n';

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
  const t = i18n.global.t;
  const matchingTagNames = item.match?.matchingTags.map((entry) => entry.tag.name) ?? [];
  const score = toNumber(item.match?.relativeScore ?? null);

  if (!options.matchInsightsAvailable) {
    return {
      valueLabel: t('swipeInsights.laterLabel'),
      valueCaption: t('swipeInsights.laterCaption'),
      valueTone: 'muted',
      headline: t('swipeInsights.laterHeadline'),
      supportingCopy: t('swipeInsights.laterSupport'),
      detailsIntro: t('swipeInsights.laterDetails'),
      reasonChips: buildReasonChips(item, options, [t('swipeInsights.laterChip')]),
    };
  }

  if (options.profileReady === false || options.scoresSuppressed) {
    return {
      valueLabel: t('swipeInsights.learningLabel'),
      valueCaption: t('swipeInsights.learningCaption'),
      valueTone: 'muted',
      headline: t('swipeInsights.learningHeadline'),
      supportingCopy: t('swipeInsights.learningSupport'),
      detailsIntro: t('swipeInsights.learningDetails'),
      reasonChips: buildReasonChips(item, options, [
        t('swipeInsights.earlyChip'),
        t('swipeInsights.ratingsChip'),
      ]),
    };
  }

  if (!item.candidate.isCompleteForMatching) {
    return {
      valueLabel: t('swipeInsights.incompleteLabel'),
      valueCaption: t('swipeInsights.incompleteCaption'),
      valueTone: 'muted',
      headline: t('swipeInsights.incompleteHeadline'),
      supportingCopy: t('swipeInsights.incompleteSupport'),
      detailsIntro: t('swipeInsights.incompleteDetails'),
      reasonChips: buildReasonChips(item, options, [
        t('swipeInsights.moreTagsChip'),
        t('swipeInsights.cautiousChip'),
      ]),
    };
  }

  if (score !== null) {
    const topTags = matchingTagNames.slice(0, 2);
    const hasStrongScore = score >= 80;
    const hasGoodScore = score >= 60;

    return {
      valueLabel: `${Math.round(score)}%`,
      valueCaption: hasStrongScore
        ? t('swipeInsights.strongCaption')
        : hasGoodScore
          ? t('swipeInsights.goodCaption')
          : t('swipeInsights.solidCaption'),
      valueTone: hasStrongScore ? 'success' : hasGoodScore ? 'accent' : 'muted',
      headline:
        topTags.length > 0
          ? t('swipeInsights.strongHeadlineWithTags', { tags: joinLocalized(topTags) })
          : t('swipeInsights.strongHeadlineGeneric'),
      supportingCopy:
        topTags.length > 0
          ? t('swipeInsights.strongSupportWithTags')
          : t('swipeInsights.strongSupportGeneric'),
      detailsIntro:
        topTags.length > 0
          ? t('swipeInsights.strongDetailsWithTags', { tags: joinLocalized(topTags) })
          : t('swipeInsights.strongDetailsGeneric'),
      reasonChips: buildReasonChips(item, options, [
        hasStrongScore ? t('swipeInsights.strongChip') : t('swipeInsights.fitsChip'),
      ]),
    };
  }

  if ((item.match?.matchingTagCount ?? 0) > 0) {
    return {
      valueLabel: t('swipeInsights.closeLabel'),
      valueCaption: t('swipeInsights.closeCaption'),
      valueTone: 'accent',
      headline:
        matchingTagNames.length > 0
          ? t('swipeInsights.closeHeadlineWithTags', {
            tags: joinLocalized(matchingTagNames.slice(0, 2)),
          })
          : t('swipeInsights.closeHeadlineGeneric'),
      supportingCopy: t('swipeInsights.closeSupport'),
      detailsIntro: t('swipeInsights.closeDetails'),
      reasonChips: buildReasonChips(item, options, [
        t('swipeInsights.goodSignalsChip'),
        t('swipeInsights.noPercentChip'),
      ]),
    };
  }

  return {
    valueLabel: t('swipeInsights.newLabel'),
    valueCaption: t('swipeInsights.newCaption'),
    valueTone: 'muted',
    headline: t('swipeInsights.newHeadline'),
    supportingCopy: t('swipeInsights.newSupport'),
    detailsIntro: t('swipeInsights.newDetails'),
    reasonChips: buildReasonChips(item, options, [
      t('swipeInsights.discoveryChip'),
      t('swipeInsights.lowOverlapChip'),
    ]),
  };
}

function buildReasonChips(
  item: SwipeQueueItem,
  options: SwipeInsightOptions,
  fallbackChips: string[],
): string[] {
  const t = i18n.global.t;
  const chips: string[] = [];
  const matchingTagNames = item.match?.matchingTags.map((entry) => entry.tag.name) ?? [];
  const candidateTagNames = item.candidate.media.tags.map((tag) => tag.name);

  chips.push(...matchingTagNames.slice(0, 2));

  if (chips.length < 2) {
    chips.push(...candidateTagNames.slice(0, 2));
  }

  if ((item.match?.matchingTagCount ?? 0) > 0 && item.match?.candidateTagCount) {
    chips.push(
      t('swipeInsights.matchingRatioChip', {
        matching: item.match.matchingTagCount,
        total: item.match.candidateTagCount,
      }),
    );
  }

  chips.push(...fallbackChips);

  if (!options.matchInsightsAvailable) {
    chips.push(t('swipeInsights.profileLaterChip'));
  }

  return [...new Set(chips.filter(Boolean))].slice(0, 4);
}

function joinLocalized(values: string[]): string {
  const conjunction = i18n.global.t('formatting.andWord');

  if (values.length <= 1) {
    return values[0] ?? '';
  }

  if (values.length === 2) {
    return `${values[0]} ${conjunction} ${values[1]}`;
  }

  return `${values.slice(0, -1).join(', ')} ${conjunction} ${values.at(-1)}`;
}

function toNumber(value: string | null): number | null {
  if (value === null) {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}
