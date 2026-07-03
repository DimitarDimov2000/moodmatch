import { config } from '@vue/test-utils';
import { vi } from 'vitest';

import { i18n, initializeI18n } from '@/i18n';

config.global.renderStubDefaultSlot = true;
config.global.plugins = [i18n];

window.scrollTo = () => {};
initializeI18n();

type MediaQueryChangeListener = (event: MediaQueryListEvent) => void;

interface MockMediaQueryList extends MediaQueryList {
  setMatches: (matches: boolean) => void;
}

const mediaQueryState = new Map<string, boolean>();
const mediaQueryLists = new Map<string, MockMediaQueryList>();

function toChangeListener(
  listener: EventListenerOrEventListenerObject | null | undefined,
): MediaQueryChangeListener | null {
  if (typeof listener === 'function') {
    return listener as MediaQueryChangeListener;
  }

  if (listener && typeof listener === 'object' && 'handleEvent' in listener) {
    return (event) => listener.handleEvent(event);
  }

  return null;
}

function createMediaQueryList(query: string): MockMediaQueryList {
  const listeners = new Set<MediaQueryChangeListener>();
  let matches = mediaQueryState.get(query) ?? false;

  const mediaQueryList: MockMediaQueryList = {
    get matches() {
      return matches;
    },
    media: query,
    onchange: null,
    addEventListener(type: string, listener: EventListenerOrEventListenerObject | null) {
      if (type !== 'change') {
        return;
      }

      const changeListener = toChangeListener(listener);
      if (changeListener) {
        listeners.add(changeListener);
      }
    },
    removeEventListener(type: string, listener: EventListenerOrEventListenerObject | null) {
      if (type !== 'change') {
        return;
      }

      const changeListener = toChangeListener(listener);
      if (changeListener) {
        listeners.delete(changeListener);
      }
    },
    addListener(listener: MediaQueryChangeListener | null) {
      if (listener) {
        listeners.add(listener);
      }
    },
    removeListener(listener: MediaQueryChangeListener | null) {
      if (listener) {
        listeners.delete(listener);
      }
    },
    dispatchEvent() {
      return true;
    },
    setMatches(nextMatches: boolean) {
      if (matches === nextMatches) {
        return;
      }

      matches = nextMatches;

      const event = {
        matches: nextMatches,
        media: query,
      } as MediaQueryListEvent;

      mediaQueryList.onchange?.call(mediaQueryList, event);

      for (const listener of listeners) {
        listener(event);
      }
    },
  };

  return mediaQueryList;
}

function getMediaQueryList(query: string): MockMediaQueryList {
  const existingList = mediaQueryLists.get(query);

  if (existingList) {
    return existingList;
  }

  const mediaQueryList = createMediaQueryList(query);
  mediaQueryLists.set(query, mediaQueryList);
  return mediaQueryList;
}

export function setMatchMediaMatches(query: string, matches: boolean) {
  mediaQueryState.set(query, matches);
  getMediaQueryList(query).setMatches(matches);
}

export function resetMatchMedia() {
  mediaQueryState.clear();
  mediaQueryLists.clear();
}

window.matchMedia = vi.fn((query: string) => getMediaQueryList(query));
