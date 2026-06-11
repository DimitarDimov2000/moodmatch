import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';

describe('ExternalSearchResultCard', () => {
  it('renders normalized demo result fields, attribution, and suggested tags', () => {
    const wrapper = mount(ExternalSearchResultCard, {
      props: {
        result: {
          source: 'DEMO',
          externalId: 'demo-book-dune',
          mediaType: 'BOOK',
          title: 'Dune',
          originalTitle: null,
          description: 'A sprawling desert saga.',
          releaseYear: 1965,
          coverUrl: 'https://demo.moodmatch.local/covers/dune.jpg',
          sourceUrl: 'https://demo.moodmatch.local/items/demo-book-dune',
          externalGenres: ['Science-Fiction', 'Adventure'],
          externalSubjects: ['Macht', 'Ueberleben'],
          suggestedTags: [
            {
              tagId: 'tag-1',
              tagName: 'Entdeckung',
              tagCategory: 'THEME',
              sourceValue: 'Macht',
              reason: 'Mapped from external subject value.',
              confidence: 'HIGH',
            },
          ],
          attribution: 'MoodMatch Demo Provider (offline)',
          warnings: [],
        },
      },
    });

    expect(wrapper.text()).toContain('Dune');
    expect(wrapper.text()).toContain('DEMO');
    expect(wrapper.text()).toContain('Science-Fiction');
    expect(wrapper.text()).toContain('Macht');
    expect(wrapper.text()).toContain('Entdeckung - HIGH');
    expect(wrapper.text()).toContain('MoodMatch Demo Provider (offline)');
    expect(wrapper.get('a').attributes('href')).toBe(
      'https://demo.moodmatch.local/items/demo-book-dune',
    );
  });
});
