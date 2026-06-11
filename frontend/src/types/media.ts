import type {
  CommitmentLevel,
  ConsumptionStatus,
  ExternalSourceName,
  MediaType,
  MetadataOrigin,
  SourceType,
} from './api-common';
import type { TagResponse } from './tag';

export interface ExternalReferenceResponse {
  id: string;
  sourceName: ExternalSourceName;
  externalId: string;
  externalUrl: string | null;
  attributionText: string | null;
  sourcePayloadHash: string | null;
  createdAt: string;
}

export interface MediaUpsertRequest {
  title: string;
  originalTitle: string | null;
  description: string | null;
  mediaType: MediaType;
  consumptionStatus: ConsumptionStatus;
  isFavourite: boolean;
  rating: number | null;
  sourceType: SourceType;
  sourceNote: string | null;
  commitmentLevel: CommitmentLevel;
  releaseYear: number | null;
  coverUrl: string | null;
  metadataOrigin: MetadataOrigin | null;
}

export type CreateMediaRequest = MediaUpsertRequest;

export type UpdateMediaRequest = MediaUpsertRequest;

export interface UpdateMediaConsumptionStatusRequest {
  consumptionStatus: ConsumptionStatus;
  rating: number | null;
  isFavourite: boolean;
  confirmDestructiveChange: boolean;
}

export interface UpdateMediaFavouriteRequest {
  isFavourite: boolean;
}

export interface ReplaceMediaTagsRequest {
  tagIds: string[];
}

export interface MediaResponse {
  id: string;
  title: string;
  originalTitle: string | null;
  description: string | null;
  mediaType: MediaType;
  consumptionStatus: ConsumptionStatus;
  isFavourite: boolean;
  rating: number | null;
  sourceType: SourceType;
  sourceNote: string | null;
  commitmentLevel: CommitmentLevel;
  releaseYear: number | null;
  coverUrl: string | null;
  externalSourceName: ExternalSourceName | null;
  externalSourceId: string | null;
  externalSourceUrl: string | null;
  metadataOrigin: MetadataOrigin | null;
  tags: TagResponse[];
  externalReferences: ExternalReferenceResponse[];
  createdAt: string;
  updatedAt: string;
}
