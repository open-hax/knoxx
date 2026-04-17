import { useEffect, useState } from "react";
import { API_BASE } from "../lib/api/core";

// ---------------------------------------------------------------------------
// LoginPage component
// ---------------------------------------------------------------------------

interface LoginPageProps {
  error?: string | null;
  onLoginSuccess: () => void;
}

export default function LoginPage({ error, onLoginSuccess }: LoginPageProps) {
  const [githubEnabled, setGithubEnabled] = useState(false);
  const [loginUrl, setLoginUrl] = useState<string | null>(null);
  const [inviteCode, setInviteCode] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [redeemStatus, setRedeemStatus] = useState<"idle" | "submitting" | "success" | "error">("idle");
  const [redeemError, setRedeemError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const inviteParam = params.get("invite");
    const emailParam = params.get("email");
    const errorParam = params.get("error");
    if (inviteParam) setInviteCode(inviteParam);
    if (emailParam) setInviteEmail(emailParam);
    if (errorParam === "not_whitelisted") {
      setRedeemError("Your GitHub account is not on the allowlist. Enter an invite code below to gain access.");
    }
  }, []);

  useEffect(() => {
    fetch(`${API_BASE}/api/auth/config`)
      .then((r) => r.json())
      .then((cfg) => {
        setGithubEnabled(cfg.githubEnabled ?? false);
        setLoginUrl(cfg.loginUrl ?? null);
      })
      .catch(() => {});
  }, []);

  const handleGithubLogin = () => {
    if (loginUrl) {
      window.location.href = `${loginUrl}?redirect=${encodeURIComponent(window.location.pathname)}`;
    }
  };

  const handleRedeemInvite = async () => {
    if (!inviteCode.trim()) return;
    setRedeemStatus("submitting");
    setRedeemError("");
    try {
      const resp = await fetch(`${API_BASE}/api/auth/invite/redeem`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code: inviteCode.trim(), email: inviteEmail.trim() }),
      });
      if (!resp.ok) {
        const body = await resp.json().catch(() => ({ error: "Redemption failed" }));
        throw new Error(body.error || body.code || `${resp.status}`);
      }
      setRedeemStatus("success");
      setTimeout(() => onLoginSuccess(), 500);
    } catch (err) {
      setRedeemStatus("error");
      setRedeemError(err instanceof Error ? err.message : "Redemption failed");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950">
      <div className="w-full max-w-md space-y-8 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-white">Knoxx</h1>
          <p className="mt-2 text-sm text-slate-400">Knowledge operations platform</p>
        </div>

        {error && error !== "Logged out" && (
          <div className="rounded-lg bg-red-900/30 border border-red-800 p-3 text-sm text-red-300">
            {error}
          </div>
        )}

        {githubEnabled && (
          <button
            onClick={handleGithubLogin}
            className="flex w-full items-center justify-center gap-3 rounded-lg bg-slate-800 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-700 border border-slate-700"
          >
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
            Continue with GitHub
          </button>
        )}

        {!githubEnabled && (
          <div className="rounded-lg bg-amber-900/30 border border-amber-800 p-3 text-sm text-amber-300">
            GitHub OAuth is not configured. Contact your administrator.
          </div>
        )}

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-700" />
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="bg-slate-900 px-2 text-slate-500">or redeem an invite</span>
          </div>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
            <input
              type="email"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">Invite code</label>
            <input
              type="text"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
              placeholder="Enter your invite code"
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none font-mono"
            />
          </div>

          {redeemError && (
            <div className="rounded-lg bg-red-900/30 border border-red-800 p-3 text-sm text-red-300">
              {redeemError}
            </div>
          )}

          {redeemStatus === "success" && (
            <div className="rounded-lg bg-green-900/30 border border-green-800 p-3 text-sm text-green-300">
              Invite accepted! Redirecting…
            </div>
          )}

          <button
            onClick={handleRedeemInvite}
            disabled={redeemStatus === "submitting" || !inviteCode.trim()}
            className="w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {redeemStatus === "submitting" ? "Redeeming…" : "Redeem invite"}
          </button>
        </div>

        <p className="text-center text-xs text-slate-600">
          By signing in, you agree to the Knoxx terms of service.
        </p>
      </div>
    </div>
  );
}
