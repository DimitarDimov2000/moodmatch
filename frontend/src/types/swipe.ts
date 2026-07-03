import type { CandidateMediaResponse, MatchResultResponse } from './api';

export type SwipeDecisionAction = 'like' | 'skip';
export type SwipeGestureIntent = SwipeDecisionAction | 'none';

export interface SwipeQueueItem {
  candidate: CandidateMediaResponse;
  match: MatchResultResponse | null;
}

export interface SwipeQueueStats {
  liked: number;
  skipped: number;
}
