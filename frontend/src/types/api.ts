export type MediaType = 'FILM' | 'SERIES' | 'BOOK' | 'GAME';

export type ConsumptionStatus =
  | 'CONSUMED'
  | 'WANT_TO_CONSUME'
  | 'IN_PROGRESS'
  | 'NOT_INTERESTED';

export interface ApiErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  details: ApiErrorDetail[];
}
