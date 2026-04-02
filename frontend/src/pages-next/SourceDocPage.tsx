import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { fetchDocumentContent } from '../lib/nextApi';

interface ForumPost {
  postId?: string;
  username?: string;
  date?: string;
  rawDate?: string;
  contentFull?: string;
  content?: string;
  images?: string[];
  links?: string[];
}

interface ForumThread {
  threadId?: string;
  threadUrl?: string;
  threadTitle?: string;
  forumCategory?: string;
  forumPath?: string[];
  stats?: {
    totalPosts?: number;
    substantivePosts?: number;
    uniqueUsers?: number;
    totalImages?: number;
    totalLinks?: number;
  };
  participants?: string[];
  posts?: ForumPost[];
}

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

function formatPostDate(post: ForumPost): string {
  if (post.rawDate) return post.rawDate;
  if (!post.date) return 'Unknown date';
  const date = new Date(post.date);
  if (Number.isNaN(date.getTime())) return 'Unknown date';
  return date.toLocaleString();
}

function parseForumThread(path: string, content: string): ForumThread | null {
  if (!/\.json$/i.test(path)) return null;
  try {
    const parsed = JSON.parse(content) as ForumThread;
    if (!Array.isArray(parsed?.posts)) return null;
    return parsed;
  } catch {
    return null;
  }
}

export default function SourceDocPage() {
  const query = useQuery();
  const rawPath = query.get('path') || '';
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [content, setContent] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [postsPerPage, setPostsPerPage] = useState(40);
  const [onlyPostsWithImages, setOnlyPostsWithImages] = useState(false);
  const [expandedImages, setExpandedImages] = useState<Record<string, boolean>>({});
  const [failedImages, setFailedImages] = useState<Record<string, boolean>>({});
  const [imageRetryNonce, setImageRetryNonce] = useState<Record<string, number>>({});
  const [zoomGallery, setZoomGallery] = useState<string[]>([]);
  const [zoomIndex, setZoomIndex] = useState<number>(-1);
  const [zoomFailed, setZoomFailed] = useState(false);
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
    setCurrentPage(1);
    setPostsPerPage(40);
    setOnlyPostsWithImages(false);
    setExpandedImages({});
    setFailedImages({});
    setImageRetryNonce({});
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

  const extractInlineImageUrls = (text: string): string[] => {
    if (!text) return [];

    const fromBbCode = Array.from(text.matchAll(/\[img\](https?:\/\/[^\[]+)\[\/img\]/gi)).map((m) => m[1]);
    const fromMarkdown = Array.from(text.matchAll(/!\[[^\]]*\]\((https?:\/\/[^\s)]+)\)/gi)).map((m) => m[1]);
    const fromPlain = Array.from(text.matchAll(/https?:\/\/[^\s"'<>]+\.(?:jpg|jpeg|png|gif|webp|avif)(?:\?[^\s"'<>]*)?/gi)).map((m) => m[0]);

    const seen = new Set<string>();
    const all = [...fromBbCode, ...fromMarkdown, ...fromPlain]
      .map((url) => url.trim())
      .filter((url) => {
        if (!url || seen.has(url)) return false;
        seen.add(url);
        return true;
      });
    return all;
  };

  const togglePostImages = (postKey: string) => {
    setExpandedImages((prev) => ({ ...prev, [postKey]: !prev[postKey] }));
  };

  const buildImageSrc = (url: string, key: string): string => {
    const nonce = imageRetryNonce[key] || 0;
    if (!nonce) return url;
    const joiner = url.includes('?') ? '&' : '?';
    return `${url}${joiner}retry=${nonce}`;
  };

  const retryImage = (key: string) => {
    setFailedImages((prev) => ({ ...prev, [key]: false }));
    setImageRetryNonce((prev) => ({ ...prev, [key]: (prev[key] || 0) + 1 }));
  };

  const preparedPosts = useMemo(() => {
    const posts = forumThread?.posts || [];
    return posts.map((post, index) => {
      const body = post.contentFull || post.content || '';
      const inlineImages = extractInlineImageUrls(body);
      const imageUrls = Array.from(new Set([...(Array.isArray(post.images) ? post.images : []), ...inlineImages]));
      return {
        post,
        index,
        body,
        postLabel: post.postId || `#${index + 1}`,
        postKey: String(post.postId || index),
        imageUrls,
      };
    });
  }, [forumThread]);

  const visiblePosts = useMemo(() => {
    if (!onlyPostsWithImages) return preparedPosts;
    return preparedPosts.filter((item) => item.imageUrls.length > 0);
  }, [onlyPostsWithImages, preparedPosts]);

  const totalPages = Math.max(1, Math.ceil(visiblePosts.length / Math.max(1, postsPerPage)));

  useEffect(() => {
    setCurrentPage((prev) => Math.min(Math.max(1, prev), totalPages));
  }, [totalPages]);

  const pagedPosts = useMemo(() => {
    const start = (currentPage - 1) * postsPerPage;
    return visiblePosts.slice(start, start + postsPerPage);
  }, [currentPage, postsPerPage, visiblePosts]);

  const renderForumPagination = () => (
    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
      <button type="button" onClick={() => setCurrentPage(1)} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">First</button>
      <button type="button" onClick={() => setCurrentPage((p) => Math.max(1, p - 10))} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">-10</button>
      <button type="button" onClick={() => setCurrentPage((p) => Math.max(1, p - 1))} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">Prev</button>
      <span className="px-2 text-slate-300">Page {currentPage} / {totalPages}</span>
      <button type="button" onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">Next</button>
      <button type="button" onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 10))} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">+10</button>
      <button type="button" onClick={() => setCurrentPage(totalPages)} className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800">Last</button>
      <label className="ml-2 flex items-center gap-2 text-slate-300">
        <span>Jump</span>
        <input
          type="number"
          min={1}
          max={totalPages}
          value={currentPage}
          onChange={(e) => {
            const n = Number(e.target.value || 1);
            if (!Number.isFinite(n)) return;
            setCurrentPage(Math.min(totalPages, Math.max(1, n)));
          }}
          className="w-20 rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs"
        />
      </label>
    </div>
  );

  const threadImageUrls = useMemo(() => {
    if (preparedPosts.length === 0) return [] as string[];
    const seen = new Set<string>();
    const urls: string[] = [];
    for (const item of preparedPosts) {
      for (const url of item.imageUrls) {
        if (!url || seen.has(url)) continue;
        seen.add(url);
        urls.push(url);
      }
    }
    return urls;
  }, [preparedPosts]);

  const zoomImageUrl = zoomIndex >= 0 && zoomIndex < zoomGallery.length ? zoomGallery[zoomIndex] : null;

  const openZoomForUrl = (url: string) => {
    const gallery = threadImageUrls.length > 0 ? threadImageUrls : [url];
    const idx = gallery.indexOf(url);
    setZoomGallery(gallery);
    setZoomIndex(idx >= 0 ? idx : 0);
    setZoomFailed(false);
  };

  const closeZoom = () => {
    setZoomGallery([]);
    setZoomIndex(-1);
    setZoomFailed(false);
  };

  const stepZoom = (delta: number) => {
    if (zoomGallery.length === 0) return;
    setZoomIndex((prev) => {
      if (prev < 0) return 0;
      const next = (prev + delta + zoomGallery.length) % zoomGallery.length;
      return next;
    });
    setZoomFailed(false);
  };

  useEffect(() => {
    if (!zoomImageUrl) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeZoom();
      } else if (event.key === 'ArrowRight') {
        stepZoom(1);
      } else if (event.key === 'ArrowLeft') {
        stepZoom(-1);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [zoomImageUrl, zoomGallery.length]);

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
              <div className="space-y-4 text-slate-100">
                <header className="rounded-lg border border-slate-700 bg-slate-900/70 p-4">
                  <h2 className="text-xl font-semibold text-cyan-200">{forumThread.threadTitle || `Thread ${forumThread.threadId || ''}`}</h2>
                  <div className="mt-2 grid grid-cols-2 gap-2 text-xs text-slate-300 md:grid-cols-4">
                    <div>Thread ID: {forumThread.threadId || 'N/A'}</div>
                    <div>Category: {forumThread.forumCategory || 'N/A'}</div>
                    <div>Total posts: {forumThread.stats?.totalPosts ?? forumThread.posts?.length ?? 0}</div>
                    <div>Participants: {forumThread.stats?.uniqueUsers ?? forumThread.participants?.length ?? 0}</div>
                  </div>
                  {forumThread.threadUrl ? (
                    <a
                      href={forumThread.threadUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-2 inline-block text-xs text-cyan-300 underline hover:text-cyan-200"
                    >
                      Open original thread
                    </a>
                  ) : null}
                </header>

                <div className="space-y-3">
                  <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-3">
                    <div className="flex flex-wrap items-center gap-3">
                      <label className="flex items-center gap-2 text-xs text-slate-300">
                        <input
                          type="checkbox"
                          checked={onlyPostsWithImages}
                          onChange={(e) => {
                            setOnlyPostsWithImages(e.target.checked);
                            setCurrentPage(1);
                          }}
                        />
                        Only posts with images
                      </label>
                      <label className="flex items-center gap-2 text-xs text-slate-300">
                        <span>Posts per page</span>
                        <select
                          value={postsPerPage}
                          onChange={(e) => {
                            setPostsPerPage(Number(e.target.value));
                            setCurrentPage(1);
                          }}
                          className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs"
                        >
                          <option value={20}>20</option>
                          <option value={40}>40</option>
                          <option value={80}>80</option>
                          <option value={120}>120</option>
                        </select>
                      </label>
                      <span className="text-xs text-slate-400">
                        Showing {visiblePosts.length} posts
                      </span>
                    </div>

                    {renderForumPagination()}
                  </div>

                  {pagedPosts.map(({ post, index, body, postLabel, postKey, imageUrls }) => {
                    const showImages = Boolean(expandedImages[postKey]);
                    return (
                      <article key={`${post.postId || index}`} className="rounded-lg border border-slate-800 bg-slate-900/60 p-3">
                        <div className="mb-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
                          <span className="font-semibold text-cyan-200">{post.username || 'Unknown user'}</span>
                          <span className="text-slate-400">{formatPostDate(post)}</span>
                          <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[11px] text-slate-300">Post {postLabel}</span>
                          {imageUrls.length > 0 ? (
                            <button
                              type="button"
                              onClick={() => togglePostImages(postKey)}
                              className="rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"
                            >
                              {showImages ? 'Hide in-post images' : `Load images in post (${imageUrls.length})`}
                            </button>
                          ) : null}
                        </div>
                        <pre className="whitespace-pre-wrap break-words text-sm leading-6 text-slate-100">{body}</pre>

                        {imageUrls.length > 0 ? (
                          <div className="mt-2 space-y-1">
                            <div className="flex items-center justify-between">
                              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Images</p>
                              <button
                                type="button"
                                onClick={() => togglePostImages(postKey)}
                                className="rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"
                              >
                                {showImages ? 'Hide in post' : 'Load in post'}
                              </button>
                            </div>
                            {imageUrls.slice(0, 12).map((imageUrl, imageIdx) => (
                              <a
                                key={`${postLabel}-img-${imageIdx}`}
                                href={imageUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="block truncate text-xs text-cyan-300 underline hover:text-cyan-200"
                              >
                                {imageUrl}
                              </a>
                            ))}
                            {imageUrls.length > 12 ? <p className="text-[11px] text-slate-500">+{imageUrls.length - 12} more images</p> : null}

                            {showImages ? (
                              <div className="grid gap-2 pt-1 sm:grid-cols-2">
                                {imageUrls.slice(0, 8).map((imageUrl, imageIdx) => (
                                  (() => {
                                    const imageKey = `${postKey}:${imageIdx}`;
                                    const failed = Boolean(failedImages[imageKey]);
                                    return (
                                  <div
                                    key={`${postLabel}-preview-${imageIdx}`}
                                    className="block overflow-hidden rounded border border-slate-700 bg-slate-950"
                                  >
                                    {failed ? (
                                      <div className="flex h-56 flex-col items-center justify-center gap-2 px-3 text-center text-xs text-slate-300">
                                        <p className="text-rose-300">Image load timed out or failed.</p>
                                        <div className="flex items-center gap-2">
                                          <button
                                            type="button"
                                            onClick={(e) => {
                                              e.preventDefault();
                                              retryImage(imageKey);
                                            }}
                                            className="rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-1 text-[11px] text-cyan-200 hover:bg-cyan-500/20"
                                          >
                                            Retry
                                          </button>
                                          <a
                                            href={imageUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="text-[11px] text-cyan-300 underline hover:text-cyan-200"
                                          >
                                            Open original
                                          </a>
                                        </div>
                                      </div>
                                    ) : (
                                      <button
                                        type="button"
                                        onClick={() => openZoomForUrl(buildImageSrc(imageUrl, imageKey))}
                                        className="block w-full"
                                      >
                                        <img
                                          src={buildImageSrc(imageUrl, imageKey)}
                                          alt={`Post ${postLabel} image ${imageIdx + 1}`}
                                          loading="lazy"
                                          referrerPolicy="no-referrer"
                                          onError={() => setFailedImages((prev) => ({ ...prev, [imageKey]: true }))}
                                          className="h-56 w-full object-contain bg-black"
                                        />
                                      </button>
                                    )}
                                  </div>
                                    );
                                  })()
                                ))}
                              </div>
                            ) : null}
                          </div>
                        ) : null}
                      </article>
                    );
                  })}

                  {renderForumPagination()}
                </div>
              </div>
            ) : isMarkdown ? (
              <article className="space-y-3 text-slate-100">
                <ReactMarkdown
                  components={{
                    h1: ({ children }) => <h1 className="text-3xl font-bold text-slate-50">{children}</h1>,
                    h2: ({ children }) => <h2 className="text-2xl font-semibold text-slate-100">{children}</h2>,
                    h3: ({ children }) => <h3 className="text-xl font-semibold text-slate-200">{children}</h3>,
                    p: ({ children }) => <p className="text-sm leading-7 text-slate-200">{children}</p>,
                    code: ({ children }) => <code className="rounded bg-slate-900 px-1 py-0.5 text-cyan-300">{children}</code>,
                    pre: ({ children }) => <pre className="overflow-auto rounded-md border border-slate-700 bg-slate-900 p-3">{children}</pre>,
                    ul: ({ children }) => <ul className="list-disc space-y-1 pl-6 text-slate-200">{children}</ul>,
                    ol: ({ children }) => <ol className="list-decimal space-y-1 pl-6 text-slate-200">{children}</ol>,
                    a: ({ href, children }) => (
                      <a href={href} target="_blank" rel="noreferrer" className="text-cyan-300 underline hover:text-cyan-200">
                        {children}
                      </a>
                    ),
                  }}
                >
                  {content}
                </ReactMarkdown>
              </article>
            ) : (
              <pre className="whitespace-pre-wrap text-sm leading-6 text-slate-100">{content}</pre>
            )}
          </div>
        ) : null}
      </section>

      {zoomImageUrl ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4"
          onClick={closeZoom}
        >
          {zoomFailed ? (
            <div className="rounded border border-slate-700 bg-slate-900 p-4 text-center text-slate-100" onClick={(e) => e.stopPropagation()}>
              <p className="text-rose-300">Failed to load zoomed image.</p>
              <a href={zoomImageUrl} target="_blank" rel="noreferrer" className="mt-2 inline-block text-cyan-300 underline">
                Open original image
              </a>
            </div>
          ) : (
            <img
              src={zoomImageUrl}
              alt="Zoomed forum image"
              referrerPolicy="no-referrer"
              className="max-h-[92vh] max-w-[92vw] object-contain"
              onError={() => setZoomFailed(true)}
              onClick={(e) => e.stopPropagation()}
            />
          )}
          {zoomGallery.length > 1 ? (
            <>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  stepZoom(-1);
                }}
                className="absolute left-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
              >
                Prev
              </button>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  stepZoom(1);
                }}
                className="absolute right-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
              >
                Next
              </button>
            </>
          ) : null}
          <button
            type="button"
            onClick={closeZoom}
            className="absolute right-4 top-4 rounded bg-slate-900/80 px-3 py-1 text-sm text-slate-100 hover:bg-slate-800"
          >
            Close
          </button>
          {zoomGallery.length > 1 ? (
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded bg-slate-900/80 px-3 py-1 text-xs text-slate-200">
              {zoomIndex + 1} / {zoomGallery.length}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
