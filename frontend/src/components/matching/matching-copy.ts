import { i18n } from '@/i18n';
import type { InterestProfileResponse, MatchResultResponse } from '@/types/api';

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

export function getProfileReadinessSummary(profile: InterestProfileResponse): string {
  if (profile.isReadyForMatching) {
    return i18n.global.t('profile.readySummary');
  }

  return i18n.global.t('profile.buildingSummary', {
    current: profile.profileRelevantMediaCount,
    required: profile.requiredProfileRelevantMediaCount,
  });
}

export function getGeneratedMatchExplanation(result: MatchResultResponse): string {
  const matchingTagNames = result.matchingTags
    .map((entry) => entry.tag.name)
    .filter(Boolean)
    .slice(0, 2);
  const tags = joinLocalized(matchingTagNames);

  if (!result.candidate.isCompleteForMatching) {
    if (tags) {
      return i18n.global.t('matching.generated.incompleteWithTags', { tags });
    }

    return i18n.global.t('matching.generated.incompleteGeneric');
  }

  if (result.relativeScore !== null) {
    if (tags) {
      return i18n.global.t('matching.generated.readyWithTags', { tags });
    }

    return i18n.global.t('matching.generated.readyGeneric');
  }

  if (result.matchingTagCount > 0) {
    if (tags) {
      return i18n.global.t('matching.generated.cautiousWithTags', { tags });
    }

    return i18n.global.t('matching.generated.cautiousGeneric');
  }

  const extraTags = result.extraCandidateTags
    .map((tag) => tag.name)
    .filter(Boolean)
    .slice(0, 2);
  const extra = joinLocalized(extraTags);

  if (extra) {
    return i18n.global.t('matching.generated.exploratoryWithTags', { tags: extra });
  }

  return i18n.global.t('matching.generated.exploratoryGeneric');
}

export function getGeneratedCandidateTagNote(): string {
  return i18n.global.t('matching.generated.tagNote');
}
