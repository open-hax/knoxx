import { describe, expect, it } from 'vitest';
import { joinPath, opsRoutes, remapLegacyOpsPath } from './app-routes';

describe('app routes', () => {
  it('builds canonical ops routes without duplicate slashes', () => {
    expect(joinPath('/ops/', '/admin/')).toBe('/ops/admin');
    expect(joinPath('/ops', '')).toBe('/ops');
    expect(opsRoutes.documents).toBe('/ops/documents');
    expect(opsRoutes.docsView).toBe('/ops/docs/view');
  });

  it('remaps legacy next routes to ops routes', () => {
    expect(remapLegacyOpsPath('/next')).toBe('/ops');
    expect(remapLegacyOpsPath('/next/admin')).toBe('/ops/admin');
    expect(remapLegacyOpsPath('/next/docs/view', '?path=docs%2Freadme.md', '#L12')).toBe('/ops/docs/view?path=docs%2Freadme.md#L12');
  });

  it('leaves non-legacy routes untouched', () => {
    expect(remapLegacyOpsPath('/')).toBe('/');
    expect(remapLegacyOpsPath('/translations', '?q=test')).toBe('/translations?q=test');
  });
});
