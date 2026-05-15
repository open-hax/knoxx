import { useState } from "react";
import { API_BASE } from "../lib/api/core";

interface SignupPageProps {
  error?: string | null;
  onSignupSuccess: () => void;
}

export default function SignupPage({ error, onSignupSuccess }: SignupPageProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [status, setStatus] = useState<"idle" | "submitting" | "success" | "error">("idle");
  const [signupError, setSignupError] = useState("");

  const handleSignup = async () => {
    if (!email.trim()) return;
    setStatus("submitting");
    setSignupError("");
    try {
      const resp = await fetch(`${API_BASE}/api/auth/signup`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
          displayName: displayName.trim() || email.trim(),
        }),
      });
      if (!resp.ok) {
        const body = await resp.json().catch(() => ({ error: "Signup failed" }));
        throw new Error(body.error || body.code || `${resp.status}`);
      }
      setStatus("success");
      setTimeout(() => onSignupSuccess(), 300);
    } catch (err) {
      setStatus("error");
      setSignupError(err instanceof Error ? err.message : "Signup failed");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950">
      <div className="w-full max-w-md space-y-8 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-white">Create your Knoxx account</h1>
          <p className="mt-2 text-sm text-slate-400">Testing-only onboarding for basic chat users.</p>
        </div>

        {error && error !== "Logged out" ? (
          <div className="rounded-lg border border-red-800 bg-red-900/30 p-3 text-sm text-red-300">
            {error}
          </div>
        ) : null}

        <div className="space-y-4">
          <div>
            <label htmlFor="signup-email" className="mb-1 block text-sm font-medium text-slate-300">Email</label>
            <input
              id="signup-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none"
            />
          </div>

          <div>
            <label htmlFor="signup-display-name" className="mb-1 block text-sm font-medium text-slate-300">Display name</label>
            <input
              id="signup-display-name"
              type="text"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="Optional"
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none"
            />
          </div>

          {signupError ? (
            <div className="rounded-lg border border-red-800 bg-red-900/30 p-3 text-sm text-red-300">
              {signupError}
            </div>
          ) : null}

          {status === "success" ? (
            <div className="rounded-lg border border-green-800 bg-green-900/30 p-3 text-sm text-green-300">
              Account created. Redirecting…
            </div>
          ) : null}

          <button
            onClick={handleSignup}
            disabled={status === "submitting" || !email.trim()}
            className="w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {status === "submitting" ? "Creating account…" : "Create basic account"}
          </button>
        </div>

        <div className="text-center text-xs text-slate-500">
          Already have an account? <a href="/login" className="text-blue-400 hover:text-blue-300">Sign in</a>
        </div>
      </div>
    </div>
  );
}
