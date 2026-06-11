import type { CandidateMediaResponse, MatchResultResponse } from './api';

export type SwipeDecisionAction = 'like' | 'reject' | 'skip';
export type SwipeGestureIntent = SwipeDecisionAction | 'preview' | 'none';

export interface SwipeQueueItem {
  candidate: CandidateMediaResponse;
  match: MatchResultResponse | null;
}

export interface SwipeQueueStats {
  liked: number;
  rejected: number;
  skipped: number;
}
