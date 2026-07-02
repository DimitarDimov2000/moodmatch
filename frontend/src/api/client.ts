import { API_BASE_URL } from './config';
import type { ApiErrorResponse } from '@/types/api';

export interface ApiRequestOptions extends Omit<RequestInit, 'body' | 'headers'> {
  body?: BodyInit | null | object;
  headers?: HeadersInit;
  query?: Record<string, string | number | boolean | null | undefined>;
}

export interface ApiClientAuthConfig {
  getAccessToken?: () => string | null | undefined;
  onUnauthorized?: (error: ApiRequestError) => void;
}

export class ApiRequestError extends Error {
  status: number;
  code: string;
  details: ApiErrorResponse['details'];
  cause?: unknown;

  constructor(
    message: string,
    options: {
      status: number;
      code: string;
      details?: ApiErrorResponse['details'];
      cause?: unknown;
    },
  ) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = options.status;
    this.code = options.code;
    this.details = options.details ?? [];
    this.cause = options.cause;
  }
}

let apiClientAuthConfig: ApiClientAuthConfig = {};

export function configureApiClientAuth(config: ApiClientAuthConfig) {
  apiClientAuthConfig = config;
}

export function resetApiClientAuth() {
  apiClientAuthConfig = {};
}

export function buildUrl(
  path: string,
  query?: ApiRequestOptions['query'],
  baseUrl = API_BASE_URL,
): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = new URL(`${baseUrl}${normalizedPath}`, window.location.origin);

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined) {
        continue;
      }

      url.searchParams.set(key, String(value));
    }
  }

  return url.toString();
}

function isPlainObject(value: ApiRequestOptions['body']): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !(value instanceof FormData);
}

function createRequestHeaders(
  body: ApiRequestOptions['body'],
  headers: HeadersInit | undefined,
  accessToken: string | null | undefined,
): Headers {
  const requestHeaders = new Headers(headers);

  if (isPlainObject(body) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  if (!requestHeaders.has('Accept')) {
    requestHeaders.set('Accept', 'application/json');
  }

  if (accessToken && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${accessToken}`);
  }

  return requestHeaders;
}

function createRequestBody(body: ApiRequestOptions['body']): BodyInit | null | undefined {
  if (body === undefined) {
    return undefined;
  }

  if (body === null || typeof body === 'string' || body instanceof FormData) {
    return body;
  }

  return JSON.stringify(body);
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<ApiErrorResponse>;

  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    Array.isArray(candidate.details)
  );
}

async function parseResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('Content-Type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json();
  }

  const text = await response.text();
  return text ? text : null;
}

export async function apiRequest<T>(
  path: string,
  { body, headers, query, ...init }: ApiRequestOptions = {},
): Promise<T> {
  let response: Response;
  const accessToken = apiClientAuthConfig.getAccessToken?.() ?? null;

  try {
    response = await fetch(buildUrl(path, query), {
      ...init,
      body: createRequestBody(body),
      headers: createRequestHeaders(body, headers, accessToken),
    });
  } catch (error) {
    throw new ApiRequestError('Network request failed.', {
      status: 0,
      code: 'NETWORK_ERROR',
      cause: error,
    });
  }

  const responseBody = await parseResponseBody(response);

  if (!response.ok) {
    const apiError = isApiErrorResponse(responseBody) ? responseBody : null;
    const requestError = new ApiRequestError(
      apiError?.message ?? `Request failed with status ${response.status}.`,
      {
        status: response.status,
        code: apiError?.code ?? (response.status === 401 ? 'UNAUTHORIZED' : 'REQUEST_ERROR'),
        details: apiError?.details ?? [],
      },
    );

    if (response.status === 401) {
      apiClientAuthConfig.onUnauthorized?.(requestError);
    }

    throw requestError;
  }

  return responseBody as T;
}

export async function getJson<T>(
  path: string,
  options?: Omit<ApiRequestOptions, 'body' | 'method'>,
): Promise<T> {
  return apiRequest<T>(path, { ...options, method: 'GET' });
}

export async function postJson<TResponse, TRequest extends ApiRequestOptions['body']>(
  path: string,
  body: TRequest,
  options?: Omit<ApiRequestOptions, 'body' | 'method'>,
): Promise<TResponse> {
  return apiRequest<TResponse>(path, { ...options, method: 'POST', body });
}

export async function putJson<TResponse, TRequest extends ApiRequestOptions['body']>(
  path: string,
  body: TRequest,
  options?: Omit<ApiRequestOptions, 'body' | 'method'>,
): Promise<TResponse> {
  return apiRequest<TResponse>(path, { ...options, method: 'PUT', body });
}

export async function patchJson<TResponse, TRequest extends ApiRequestOptions['body']>(
  path: string,
  body: TRequest,
  options?: Omit<ApiRequestOptions, 'body' | 'method'>,
): Promise<TResponse> {
  return apiRequest<TResponse>(path, { ...options, method: 'PATCH', body });
}

export async function deleteRequest(
  path: string,
  options?: Omit<ApiRequestOptions, 'body' | 'method'>,
): Promise<void> {
  await apiRequest<null>(path, { ...options, method: 'DELETE' });
}
