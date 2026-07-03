import { mount } from '@vue/test-utils';

import MediaForm from '@/components/media/MediaForm.vue';
import type { TagResponse } from '@/types/api';

function mountMediaForm(availableTags: TagResponse[] = []) {
  return mount(MediaForm, {
    props: {
      mode: 'create',
      availableTags,
    },
  });
}

async function setBaseTitle(wrapper: ReturnType<typeof mountMediaForm>, title = 'Interstellar') {
  await wrapper.get('input[name="title"]').setValue(title);
}

async function setStatus(wrapper: ReturnType<typeof mountMediaForm>, status: string) {
  await wrapper.get('select[name="consumptionStatus"]').setValue(status);
}

async function setRating(wrapper: ReturnType<typeof mountMediaForm>, rating: string) {
  await wrapper.get('input[name="rating"]').setValue(rating);
}

describe('MediaForm', () => {
  it('consumed media with rating 5 enables submit', async () => {
    const wrapper = mountMediaForm();

    await setBaseTitle(wrapper);
    await setStatus(wrapper, 'CONSUMED');
    await setRating(wrapper, '5');

    expect(wrapper.text()).not.toContain('Bei konsumierten Medien ist eine Bewertung erforderlich.');
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined();
  });

  it('consumed media with rating 5 enables favourite', async () => {
    const wrapper = mountMediaForm();

    await setBaseTitle(wrapper);
    await setStatus(wrapper, 'CONSUMED');
    await setRating(wrapper, '5');

    expect(wrapper.get('input[name="isFavourite"]').attributes('disabled')).toBeUndefined();
  });

  it('consumed media without rating blocks submit', async () => {
    const wrapper = mountMediaForm();

    await setBaseTitle(wrapper);
    await setStatus(wrapper, 'CONSUMED');

    expect(wrapper.text()).toContain('Bei konsumierten Medien ist eine Bewertung erforderlich.');
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined();
  });

  it('non-consumed media can be submitted without rating and emits null rating', async () => {
    const wrapper = mountMediaForm();

    await setBaseTitle(wrapper);
    await setStatus(wrapper, 'WANT_TO_CONSUME');

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined();

    await wrapper.get('form').trigger('submit.prevent');

    expect(wrapper.emitted('submit')).toBeTruthy();
    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      media: expect.objectContaining({
        title: 'Interstellar',
        consumptionStatus: 'WANT_TO_CONSUME',
        rating: null,
        isFavourite: false,
      }),
      tagIds: [],
    });
  });

  it('shows the empty tag helper message without blocking form usage', async () => {
    const wrapper = mountMediaForm();

    expect(wrapper.text()).toContain(
      'Noch keine Tags verfügbar. Du kannst das Medium trotzdem jetzt speichern und Tags später hinzufügen.',
    );
  });
});
