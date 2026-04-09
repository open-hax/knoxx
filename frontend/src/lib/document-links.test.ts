import { describe, expect, it } from 'vitest';
import { isExternalHref, normalizeRelativeDocPath, resolveDocumentHref } from './document-links';

describe('document links', () => {
  it('detects external hrefs', () => {
    expect(isExternalHref('https://example.com')).toBe(true);
    expect(isExternalHref('mailto:test@example.com')).toBe(true);
    expect(isExternalHref('docs/readme.md')).toBe(false);
  });

  it('normalizes relative document paths', () => {
    expect(normalizeRelativeDocPath('./docs/../guides/intro.md')).toBe('guides/intro.md');
    expect(normalizeRelativeDocPath('/docs//guides/./intro.md')).toBe('docs/guides/intro.md');
  });

  it('resolves relative and absolute document hrefs', () => {
    expect(resolveDocumentHref('docs/guides/intro.md', '../api/reference.md')).toBe('docs/api/reference.md');
    expect(resolveDocumentHref('docs/guides/intro.md', '/docs/index.md')).toBe('docs/index.md');
    expect(resolveDocumentHref('docs/guides/intro.md', '#overview')).toBeNull();
    expect(resolveDocumentHref('docs/guides/intro.md', 'https://example.com')).toBeNull();
  });
});
