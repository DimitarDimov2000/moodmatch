import type { TagCategory, TagResponse } from '@/types/api';

export const tagCategoryLabels: Record<TagCategory, string> = {
  GENRE: 'Genre',
  THEME: 'Thema',
  SETTING: 'Setting',
  TONE: 'Ton',
  EXPERIENCE: 'Erlebnis',
};

export function groupTagsByCategory(tags: TagResponse[]): Array<{
  category: TagCategory;
  label: string;
  tags: TagResponse[];
}> {
  const order: TagCategory[] = ['GENRE', 'THEME', 'SETTING', 'TONE', 'EXPERIENCE'];

  return order
    .map((category) => ({
      category,
      label: tagCategoryLabels[category],
      tags: tags
        .filter((tag) => tag.category === category)
        .sort((left, right) => left.name.localeCompare(right.name)),
    }))
    .filter((group) => group.tags.length > 0);
}

