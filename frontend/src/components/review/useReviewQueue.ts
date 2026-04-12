/**
 * Review Queue API Hook
 *
 * Fetches and manages review queue state from /v1/reviews API.
 */

import { useState, useEffect, useCallback } from "react";
import type { ReviewItem, ReviewStats, ReviewItemStatus } from "./review-types";

const OPENPLANNER_BASE = "/api/openplanner/v1";

interface ReviewQueueState {
  items: ReviewItem[];
  stats: ReviewStats | null;
  loading: boolean;
  error: string | null;
}

interface ReviewQueueActions {
  refresh: () => Promise<void>;
  approve: (docId: string, notes?: string) => Promise<void>;
  reject: (docId: string, reason?: string) => Promise<void>;
  flag: (docId: string, reason?: string) => Promise<void>;
  batchAction: (action: "approve" | "reject" | "flag", docIds: string[], reason?: string) => Promise<void>;
}

async function openplannerFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${OPENPLANNER_BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed: ${res.status}`);
  }

  return res.json();
}

export function useReviewQueue(): ReviewQueueState & ReviewQueueActions {
  const [state, setState] = useState<ReviewQueueState>({
    items: [],
    stats: null,
    loading: true,
    error: null,
  });

  const refresh = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));

    try {
      const [itemsRes, statsRes] = await Promise.all([
        openplannerFetch<{ items: ReviewItem[]; total: number }>(
          `/reviews?status=all&limit=200`
        ),
        openplannerFetch<ReviewStats>(`/reviews/stats`),
      ]);

      setState({
        items: itemsRes.items,
        stats: statsRes,
        loading: false,
        error: null,
      });
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: err instanceof Error ? err.message : "Failed to load review queue",
      }));
    }
  }, []);

  const approve = useCallback(async (docId: string, notes?: string) => {
    await openplannerFetch(`/reviews/${docId}/approve`, {
      method: "POST",
      body: JSON.stringify({ notes }),
    });
    await refresh();
  }, [refresh]);

  const reject = useCallback(async (docId: string, reason?: string) => {
    await openplannerFetch(`/reviews/${docId}/reject`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    });
    await refresh();
  }, [refresh]);

  const flag = useCallback(async (docId: string, reason?: string) => {
    await openplannerFetch(`/reviews/${docId}/flag`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    });
    await refresh();
  }, [refresh]);

  const batchAction = useCallback(
    async (action: "approve" | "reject" | "flag", docIds: string[], reason?: string) => {
      await openplannerFetch(`/reviews/batch`, {
        method: "POST",
        body: JSON.stringify({ action, doc_ids: docIds, reason }),
      });
      await refresh();
    },
    [refresh]
  );

  // Initial load
  useEffect(() => {
    refresh();
  }, [refresh]);

  return {
    ...state,
    refresh,
    approve,
    reject,
    flag,
    batchAction,
  };
}

/** Get display status for an item */
export function getItemStatus(item: ReviewItem): ReviewItemStatus {
  if (item.visibility === "public") return "approved";
  if (item.visibility === "internal") return "rejected";
  return "pending";
}
