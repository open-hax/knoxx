export function isExternalHref(href: string): boolean {
  return /^(?:[a-z][a-z0-9+.-]*:)?\/\//i.test(href)
    || href.startsWith('mailto:')
    || href.startsWith('tel:');
}

export function normalizeRelativeDocPath(input: string): string {
  const stack: string[] = [];

  for (const part of input.split('/')) {
    if (!part || part === '.') continue;
    if (part === '..') {
      stack.pop();
      continue;
    }
    stack.push(part);
  }

  return stack.join('/');
}

export function resolveDocumentHref(currentPath: string, href: string): string | null {
  const trimmed = href.trim();
  if (!trimmed || isExternalHref(trimmed) || trimmed.startsWith('#')) {
    return null;
  }

  const withoutHash = trimmed.split('#')[0] || '';
  const withoutQuery = withoutHash.split('?')[0] || '';
  if (!withoutQuery) return null;

  if (withoutQuery.startsWith('/')) {
    return normalizeRelativeDocPath(withoutQuery.replace(/^\/+/, ''));
  }

  const baseParts = currentPath.split('/').filter(Boolean);
  baseParts.pop();
  return normalizeRelativeDocPath([...baseParts, withoutQuery].join('/'));
}
