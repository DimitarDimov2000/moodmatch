import type { TagResponse } from './tag';
import type { CommitmentLevel, ConsumptionStatus, MediaType } from './api-common';

export interface MatchingMediaResponse {
  id: string;
  title: string;
  mediaType: MediaType;
  consumptionStatus: ConsumptionStatus;
  commitmentLevel: CommitmentLevel;
  isFavourite: boolean;
  rating: number | null;
  releaseYear: number | null;
  coverUrl: string | null;
  tags: TagResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CandidateMediaResponse {
  media: MatchingMediaResponse;
  isCompleteForMatching: boolean;
}

export interface CandidateSelectionResponse {
  candidates: CandidateMediaResponse[];
}

export interface InterestProfileTagContributionResponse {
  tag: TagResponse;
  contributionWeight: string;
}

export interface InterestProfileMediaContributionResponse {
  media: MatchingMediaResponse;
  ratingWeight: string;
  favouriteFactor: string;
  tagContributions: InterestProfileTagContributionResponse[];
}

export interface InterestProfileTagWeightResponse {
  tag: TagResponse;
  weight: string;
}

export interface InterestProfileResponse {
  isReadyForMatching: boolean;
  profileRelevantMediaCount: number;
  requiredProfileRelevantMediaCount: number;
  explanationMessage: string;
  contributingMedia: InterestProfileMediaContributionResponse[];
  weightedTags: InterestProfileTagWeightResponse[];
}

export interface MatchTagExplanationResponse {
  tag: TagResponse;
  profileWeight: string;
}

export interface MatchResultResponse {
  candidate: CandidateMediaResponse;
  candidateTagCount: number;
  matchingTagCount: number;
  matchingTags: MatchTagExplanationResponse[];
  extraCandidateTags: TagResponse[];
  rawScore: string | null;
  precisionFactor: string | null;
  adjustedScore: string | null;
  relativeScore: string | null;
  explanationMessage: string;
  candidateTagsNote: string;
}

export interface MatchingResponse {
  interestProfile: InterestProfileResponse;
  scoresSuppressed: boolean;
  explanationMessage: string;
  scoringMethodNote: string;
  matches: MatchResultResponse[];
}
