export const API_BASE_URL = '/api';

export interface ApiRequestError extends Error {
  status?: number;
}

export async function notImplementedApiCall<T>(operation: string): Promise<T> {
  throw new Error(`API operation "${operation}" is not implemented yet.`);
}
