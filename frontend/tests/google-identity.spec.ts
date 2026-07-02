import { describe, expect, it } from 'vitest';

import { googleIdentityProvider } from '@/auth/google-identity';

describe('google identity provider boundary', () => {
  it('stays inactive when no Google client id is configured', async () => {
    await expect(
      googleIdentityProvider.initialize({
        clientId: null,
        onCredential: () => undefined,
      }),
    ).resolves.toEqual({
      status: 'missing-client-id',
    });
  });
});
