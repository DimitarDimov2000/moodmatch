import type { TagCategory } from './api-common';

export interface TagResponse {
  id: string;
  name: string;
  category: TagCategory;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTagRequest {
  name: string;
  category: TagCategory;
}
