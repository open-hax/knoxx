import React, { useCallback, useEffect, useState } from "react";
import { SectionCard, Badge } from "./common";
import { getDiscordConfig, updateDiscordConfig, type DiscordConfigStatus } from "../../lib/api/admin";

type Notice = { tone: "success" | "error"; text: string } | null;

export function DiscordSection({ canManage }: { canManage: boolean }) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");
  const [status, setStatus] = useState<DiscordConfigStatus | null>(null);
  const [draftToken, setDraftToken] = useState<string>("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotice(null);
    try {
      const config = await getDiscordConfig();
      setStatus(config);
      setDraftToken("");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = useCallback(async () => {
    if (!canManage) return;
    const normalized = draftToken.trim();
    if (!normalized) {
      setError("Bot token must not be blank");
      return;
    }
    setSaving(true);
    setError("");
    setNotice(null);
    try {
      const updated = await updateDiscordConfig(normalized);
      setStatus(updated);
      setDraftToken("");
      setNotice({ tone: "success", text: `Discord bot token saved. Preview: ${updated.tokenPreview}` });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, draftToken]);

  return (
    <SectionCard
      title="Discord integration"
      description="Configure the Discord bot token so Knoxx agents can publish messages to Discord channels."
    >
      {loading ? (
        <div className="text-sm text-slate-300">Loading Discord config…</div>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="text-sm text-slate-300">Status:</span>
            {status?.configured ? (
              <Badge tone="success">Configured</Badge>
            ) : (
              <Badge tone="warn">Not configured</Badge>
            )}
            {status?.configured && status.tokenPreview ? (
              <span className="font-mono text-xs text-slate-400">{status.tokenPreview}</span>
            ) : null}
          </div>

          <div className="grid gap-3 lg:grid-cols-[1fr_auto] lg:items-end">
            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Discord bot token
              </div>
              <input
                type="password"
                value={draftToken}
                onChange={(event) => setDraftToken(event.target.value)}
                disabled={!canManage || saving}
                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                placeholder={status?.configured ? "Enter new token to replace" : "Bot token from Discord Developer Portal"}
              />
              <div className="text-xs text-slate-500">
                {status?.configured
                  ? "Leave blank to keep the current token. Enter a new value to replace it."
                  : "Create a bot at discord.com/developers/applications and copy the Bot Token."}
              </div>
            </label>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => void load()}
                disabled={saving}
                className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
              >
                Refresh
              </button>
              <button
                type="button"
                onClick={() => void handleSave()}
                disabled={!canManage || saving || !draftToken.trim()}
                className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
              >
                {saving ? "Saving…" : "Save token"}
              </button>
            </div>
          </div>

          {!canManage ? (
            <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              You do not have <code className="font-mono">platform.org.create</code> permission to update the Discord bot token.
            </div>
          ) : null}

          {notice ? (
            <div
              className={
                notice.tone === "success"
                  ? "rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
                  : "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"
              }
            >
              {notice.text}
            </div>
          ) : null}

          {error ? (
            <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
              {error}
            </div>
          ) : null}

          <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-3 text-xs text-slate-400">
            <strong className="text-slate-300">How to set up:</strong>
            <ol className="mt-1 list-inside list-decimal space-y-1">
              <li>Go to <a href="https://discord.com/developers/applications" target="_blank" rel="noreferrer" className="text-sky-400 underline">Discord Developer Portal</a></li>
              <li>Create a new application → Bot → copy the Bot Token</li>
              <li>Enable <strong>Message Content Intent</strong> in Bot settings</li>
              <li>Use OAuth2 URL Generator with <code className="font-mono">bot</code> scope and <code className="font-mono">Send Messages</code> + <code className="font-mono">Read Message History</code> permissions</li>
              <li>Paste the token above and save</li>
            </ol>
            <p className="mt-2">
              Once configured, agents with the <code className="font-mono">discord.publish</code> tool policy can post messages to any channel the bot has access to.
            </p>
          </div>
        </div>
      )}
    </SectionCard>
  );
}
