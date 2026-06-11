import { deleteRequest, getJson, patchJson, postJson, putJson } from './client';
import type {
  CreateMediaRequest,
  MediaResponse,
  ReplaceMediaTagsRequest,
  UpdateMediaConsumptionStatusRequest,
  UpdateMediaFavouriteRequest,
  UpdateMediaRequest,
} from '@/types/api';

const mediaPath = '/media';

export function listMedia(): Promise<MediaResponse[]> {
  return getJson<MediaResponse[]>(mediaPath);
}

export function getMediaById(id: string): Promise<MediaResponse> {
  return getJson<MediaResponse>(`${mediaPath}/${id}`);
}

export function createMedia(request: CreateMediaRequest): Promise<MediaResponse> {
  return postJson<MediaResponse, CreateMediaRequest>(mediaPath, request);
}

export function updateMedia(id: string, request: UpdateMediaRequest): Promise<MediaResponse> {
  return putJson<MediaResponse, UpdateMediaRequest>(`${mediaPath}/${id}`, request);
}

export function deleteMedia(id: string): Promise<void> {
  return deleteRequest(`${mediaPath}/${id}`);
}

export function updateMediaStatus(
  id: string,
  request: UpdateMediaConsumptionStatusRequest,
): Promise<MediaResponse> {
  return patchJson<MediaResponse, UpdateMediaConsumptionStatusRequest>(
    `${mediaPath}/${id}/status`,
    request,
  );
}

export function updateMediaFavourite(
  id: string,
  request: UpdateMediaFavouriteRequest,
): Promise<MediaResponse> {
  return patchJson<MediaResponse, UpdateMediaFavouriteRequest>(
    `${mediaPath}/${id}/favorite`,
    request,
  );
}

export function replaceMediaTags(
  id: string,
  request: ReplaceMediaTagsRequest,
): Promise<MediaResponse> {
  return putJson<MediaResponse, ReplaceMediaTagsRequest>(`${mediaPath}/${id}/tags`, request);
}
