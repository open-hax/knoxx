import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Markdown } from '@open-hax/uxx';
import { fetchDocumentContent } from '../lib/nextApi';
import { ForumThreadView, parseForumThread } from './source-doc-page/ForumThreadView';

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

function isExternalHref(href: string): boolean {
  return /^(?:[a-z][a-z0-9+.-]*:)?\/\//i.test(href)
    || href.startsWith('mailto:')
    || href.startsWith('tel:');
}

function normalizeRelativeDocPath(input: string): string {
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

function resolveDocumentHref(currentPath: string, href: string): string | null {
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

export default function SourceDocPage() {
  const navigate = useNavigate();
  const query = useQuery();
  const rawPath = query.get('path') || '';
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [content, setContent] = useState('');
  const isMarkdown = /\.(md|mdx)$/i.test(rawPath);

  useEffect(() => {
    const relativePath = rawPath.replace(/^\/+/, '');
    if (!relativePath) {
      setError('Missing document path');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    void fetchDocumentContent(relativePath)
      .then((data) => {
        setContent(data.content || '');
      })
      .catch((err) => {
        setError((err as Error).message || 'Failed to load document');
      })
      .finally(() => setLoading(false));
  }, [rawPath]);

  const forumThread = useMemo(() => parseForumThread(rawPath, content), [rawPath, content]);

  const handleMarkdownLink = (href: string) => {
    if (!href) return;

    if (href.startsWith('#')) {
      const targetId = decodeURIComponent(href.slice(1));
      document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }

    if (isExternalHref(href)) {
      window.open(href, '_blank', 'noopener,noreferrer');
      return;
    }

    const nextPath = resolveDocumentHref(rawPath, href);
    if (!nextPath) {
      window.open(href, '_blank', 'noopener,noreferrer');
      return;
    }

    navigate(`/next/docs/view?path=${encodeURIComponent(nextPath)}`);
  };

  return (
    <div className="mx-auto w-full max-w-6xl p-6">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-100">Document Viewer</h1>
          <p className="mt-1 font-mono text-xs text-slate-400">{rawPath || 'N/A'}</p>
        </div>
        <Link to="/next/chat" className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200 hover:bg-slate-800">
          Back to Chat
        </Link>
      </div>

      <section className="rounded-xl border border-slate-700 bg-slate-900/80 p-4 shadow-xl">
        {loading ? <p className="text-slate-300">Loading document...</p> : null}
        {error ? <p className="text-rose-300">{error}</p> : null}

        {!loading && !error ? (
            <div className="max-h-[78vh] overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-4">
              {forumThread ? (
                <ForumThreadView thread={forumThread} />
              ) : isMarkdown ? (
                <article className="text-slate-100">
                  <Markdown
                  content={content}
                  theme="dark"
                  variant="full"
                  linkTarget="_self"
                  onLinkClick={(href) => handleMarkdownLink(href)}
                />
              </article>
            ) : (
              <pre className="whitespace-pre-wrap text-sm leading-6 text-slate-100">{content}</pre>
            )}
          </div>
        ) : null}
      </section>
    </div>
  );
}
