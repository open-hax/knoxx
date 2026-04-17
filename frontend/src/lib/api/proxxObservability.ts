import { request } from "./core";

export type ProxxUsageWindow = "daily" | "weekly" | "monthly";

export type ProxxUsageOverview = {
  window: ProxxUsageWindow;
  generatedAt: string;
  summary: {
    requests24h: number;
    tokens24h: number;
    promptTokens24h: number;
    completionTokens24h: number;
    cachedPromptTokens24h: number;
    costUsd24h: number;
    cacheHitRate24h: number;
    errorRate24h: number;
    topModel: string | null;
    topProvider: string | null;
    activeAccounts: number;
    routingRequests24h?: {
      local: number;
      federated: number;
      bridge: number;
      distinctPeers: number;
      topPeer: string | null;
    };
  };
};

export type ProxxRequestLogEntry = {
  id: string;
  timestamp: number;
  providerId: string;
  accountId: string;
  model: string;
  status: number;
  latencyMs: number;
  routeKind?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  cachedPromptTokens?: number;
  costUsd?: number;
  cacheHit?: boolean;
  promptCacheKeyUsed?: boolean;
  error?: string;
  upstreamErrorCode?: string;
  upstreamErrorMessage?: string;
};

export type ProxxRequestLogsResponse = {
  entries: ProxxRequestLogEntry[];
};

export type ProxxProviderModelAnalyticsRow = {
  providerId?: string;
  model?: string;
  requestCount: number;
  errorCount: number;
  errorRate: number;
  totalTokens: number;
  cachedPromptTokens: number;
  cacheHitRate: number;
  costUsd: number;
  avgTtftMs: number | null;
  avgDecodeTps: number | null;
  avgTps: number | null;
  avgEndToEndTps: number | null;
  firstSeenAt: string | null;
  lastSeenAt: string | null;
};

export type ProxxProviderModelAnalytics = {
  window: ProxxUsageWindow;
  generatedAt: string;
  models: ProxxProviderModelAnalyticsRow[];
  providers: ProxxProviderModelAnalyticsRow[];
  providerModels: ProxxProviderModelAnalyticsRow[];
};

export async function getProxxUsageOverview(window: ProxxUsageWindow = "daily"): Promise<ProxxUsageOverview> {
  return request<ProxxUsageOverview>(`/api/proxx/observability/dashboard/overview?window=${encodeURIComponent(window)}`);
}

export async function listProxxRequestLogs(params?: {
  limit?: number;
  providerId?: string;
  accountId?: string;
  before?: string;
}): Promise<ProxxRequestLogsResponse> {
  const query = new URLSearchParams();
  if (params?.limit) query.set("limit", String(params.limit));
  if (params?.providerId) query.set("providerId", params.providerId);
  if (params?.accountId) query.set("accountId", params.accountId);
  if (params?.before) query.set("before", params.before);
  const qs = query.toString();
  return request<ProxxRequestLogsResponse>(`/api/proxx/observability/request-logs${qs ? `?${qs}` : ""}`);
}

export async function getProxxProviderModelAnalytics(window: ProxxUsageWindow = "daily"): Promise<ProxxProviderModelAnalytics> {
  return request<ProxxProviderModelAnalytics>(`/api/proxx/observability/analytics/provider-model?window=${encodeURIComponent(window)}`);
}
