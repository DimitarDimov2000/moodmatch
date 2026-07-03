import type { TagCategory, TagResponse } from '@/types/api';
import { i18n } from '@/i18n';

export function getTagCategoryLabel(category: TagCategory): string {
  return i18n.global.t(`labels.tagCategory.${category}`);
}

export function groupTagsByCategory(tags: TagResponse[]): Array<{
  category: TagCategory;
  label: string;
  tags: TagResponse[];
}> {
  const order: TagCategory[] = ['GENRE', 'THEME', 'SETTING', 'TONE', 'EXPERIENCE'];

  return order
    .map((category) => ({
      category,
      label: getTagCategoryLabel(category),
      tags: tags
        .filter((tag) => tag.category === category)
        .sort((left, right) => left.name.localeCompare(right.name)),
    }))
    .filter((group) => group.tags.length > 0);
}
