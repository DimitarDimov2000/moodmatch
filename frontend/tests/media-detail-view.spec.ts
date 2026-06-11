import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiRequestError } from '@/api/client';
import MediaDetailView from '@/views/MediaDetailView.vue';
import type { MediaResponse } from '@/types/api';

const { pushSpy } = vi.hoisted(() => ({
  pushSpy: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    template: '<a><slot /></a>',
  },
  useRoute: () => ({
    params: {
      id: 'media-1',
    },
  }),
  useRouter: () => ({
    push: pushSpy,
  }),
}));

const {
  getMediaByIdMock,
  replaceMediaTagsMock,
  updateMediaMock,
  updateMediaFavouriteMock,
  updateMediaStatusMock,
  deleteMediaMock,
} = vi.hoisted(() => ({
  getMediaByIdMock: vi.fn(),
  replaceMediaTagsMock: vi.fn(),
  updateMediaMock: vi.fn(),
  updateMediaFavouriteMock: vi.fn(),
  updateMediaStatusMock: vi.fn(),
  deleteMediaMock: vi.fn(),
}));

vi.mock('@/api/media', () => ({
  getMediaById: getMediaByIdMock,
  replaceMediaTags: replaceMediaTagsMock,
  updateMedia: updateMediaMock,
  updateMediaFavourite: updateMediaFavouriteMock,
  updateMediaStatus: updateMediaStatusMock,
  deleteMedia: deleteMediaMock,
}));

const { listTagsMock } = vi.hoisted(() => ({
  listTagsMock: vi.fn(),
}));

vi.mock('@/api/tags', () => ({
  listTags: listTagsMock,
}));

const sampleMedia: MediaResponse = {
  id: 'media-1',
  title: 'Interstellar',
  originalTitle: 'Interstellar',
  description: 'Science fiction.',
  mediaType: 'FILM',
  consumptionStatus: 'CONSUMED',
  isFavourite: true,
  rating: 5,
  sourceType: 'MANUAL',
  sourceNote: null,
  commitmentLevel: 'LONG',
  releaseYear: 2014,
  coverUrl: null,
  externalSourceName: null,
  externalSourceId: null,
  externalSourceUrl: null,
  metadataOrigin: 'MANUAL',
  tags: [],
  externalReferences: [],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function createDeferredPromise() {
  let resolve!: () => void;
  let reject!: (reason?: unknown) => void;

  const promise = new Promise<void>((innerResolve, innerReject) => {
    resolve = innerResolve;
    reject = innerReject;
  });

  return { promise, resolve, reject };
}

async function mountView() {
  const wrapper = mount(MediaDetailView, {
    global: {
      stubs: {
        MediaForm: true,
        MediaStatusControls: true,
        TagCategoryList: true,
      },
    },
  });

  await flushPromises();
  return wrapper;
}

describe('MediaDetailView', () => {
  beforeEach(() => {
    pushSpy.mockReset();
    getMediaByIdMock.mockReset();
    replaceMediaTagsMock.mockReset();
    updateMediaMock.mockReset();
    updateMediaFavouriteMock.mockReset();
    updateMediaStatusMock.mockReset();
    deleteMediaMock.mockReset();
    listTagsMock.mockReset();

    getMediaByIdMock.mockResolvedValue(sampleMedia);
    listTagsMock.mockResolvedValue([]);
  });

  it('shows a confirmation step and navigates back after a successful delete', async () => {
    const deferredDelete = createDeferredPromise();
    deleteMediaMock.mockReturnValue(deferredDelete.promise);

    const wrapper = await mountView();

    await wrapper.get('button.button--danger').trigger('click');

    expect(wrapper.text()).toContain('Wirklich');
    expect(wrapper.text()).toContain('Interstellar');

    await wrapper.get('button.button--danger').trigger('click');

    expect(deleteMediaMock).toHaveBeenCalledWith('media-1');
    expect(wrapper.text()).toContain('Wird geloescht...');
    expect(wrapper.get('button.button--danger').attributes('disabled')).toBeDefined();

    deferredDelete.resolve();
    await flushPromises();

    expect(pushSpy).toHaveBeenCalledWith({ name: 'media-list' });
  });

  it('shows a structured error message when delete fails', async () => {
    deleteMediaMock.mockRejectedValue(
      new ApiRequestError('Das Medium wird noch verwendet.', {
        status: 409,
        code: 'CONFLICT',
      }),
    );

    const wrapper = await mountView();

    await wrapper.get('button.button--danger').trigger('click');
    await wrapper.get('button.button--danger').trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('Loeschen fehlgeschlagen');
    expect(wrapper.text()).toContain('Das Medium wird noch verwendet.');
    expect(pushSpy).not.toHaveBeenCalled();
  });
});
