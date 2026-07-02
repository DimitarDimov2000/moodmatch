export interface GoogleCredentialResponse {
  credential: string;
  select_by?: string;
}

export interface GoogleIdentityInitializeOptions {
  clientId: string | null;
  onCredential: (response: GoogleCredentialResponse) => void;
}

export type GoogleIdentityInitializeStatus =
  | 'ready'
  | 'missing-client-id'
  | 'unsupported'
  | 'error';

export interface GoogleIdentityInitializeResult {
  status: GoogleIdentityInitializeStatus;
  error?: string;
}

interface GoogleIdConfiguration {
  client_id: string;
  callback: (response: GoogleCredentialResponse) => void;
  cancel_on_tap_outside?: boolean;
}

interface GoogleAccountsIdApi {
  initialize: (configuration: GoogleIdConfiguration) => void;
  prompt: () => void;
  disableAutoSelect?: () => void;
}

interface GoogleNamespace {
  accounts?: {
    id?: GoogleAccountsIdApi;
  };
}

declare global {
  interface Window {
    google?: GoogleNamespace;
  }
}

const GOOGLE_IDENTITY_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

let googleIdentityScriptPromise: Promise<void> | null = null;

function canUseBrowserDom(): boolean {
  return typeof window !== 'undefined' && typeof document !== 'undefined';
}

function getGoogleAccountsIdApi(): GoogleAccountsIdApi | null {
  return window.google?.accounts?.id ?? null;
}

function createGoogleIdentityScriptPromise(): Promise<void> {
  return new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>(
      `script[src="${GOOGLE_IDENTITY_SCRIPT_SRC}"]`,
    );

    if (existingScript) {
      if (getGoogleAccountsIdApi()) {
        resolve();
        return;
      }

      existingScript.addEventListener('load', () => resolve(), { once: true });
      existingScript.addEventListener(
        'error',
        () => reject(new Error('Failed to load Google Identity Services.')),
        { once: true },
      );
      return;
    }

    const script = document.createElement('script');
    script.src = GOOGLE_IDENTITY_SCRIPT_SRC;
    script.async = true;
    script.defer = true;
    script.addEventListener('load', () => resolve(), { once: true });
    script.addEventListener(
      'error',
      () => reject(new Error('Failed to load Google Identity Services.')),
      { once: true },
    );
    document.head.appendChild(script);
  });
}

async function loadGoogleIdentityScript(): Promise<void> {
  if (!canUseBrowserDom()) {
    throw new Error('Google Identity Services requires a browser environment.');
  }

  if (getGoogleAccountsIdApi()) {
    return;
  }

  googleIdentityScriptPromise ??= createGoogleIdentityScriptPromise();
  await googleIdentityScriptPromise;
}

export const googleIdentityProvider = {
  async initialize(
    options: GoogleIdentityInitializeOptions,
  ): Promise<GoogleIdentityInitializeResult> {
    if (!options.clientId) {
      return {
        status: 'missing-client-id',
      };
    }

    if (!canUseBrowserDom()) {
      return {
        status: 'unsupported',
        error: 'Google Identity Services requires a browser environment.',
      };
    }

    try {
      await loadGoogleIdentityScript();
    } catch (error) {
      return {
        status: 'error',
        error:
          error instanceof Error
            ? error.message
            : 'Failed to load Google Identity Services.',
      };
    }

    const googleAccountsIdApi = getGoogleAccountsIdApi();

    if (!googleAccountsIdApi) {
      return {
        status: 'error',
        error: 'Google Identity Services did not expose the expected accounts client.',
      };
    }

    googleAccountsIdApi.initialize({
      client_id: options.clientId,
      callback: options.onCredential,
      cancel_on_tap_outside: true,
    });

    return {
      status: 'ready',
    };
  },
  prompt(): boolean {
    const googleAccountsIdApi = canUseBrowserDom() ? getGoogleAccountsIdApi() : null;

    if (!googleAccountsIdApi) {
      return false;
    }

    googleAccountsIdApi.prompt();
    return true;
  },
  logout() {
    const googleAccountsIdApi = canUseBrowserDom() ? getGoogleAccountsIdApi() : null;
    googleAccountsIdApi?.disableAutoSelect?.();
  },
};
