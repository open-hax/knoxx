import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { getFrontendConfig } from "./lib/api";
import ChatLabPage from "./pages/ChatLabPage";
import CmsPage from "./pages/CmsPage";
import GardensPage from "./pages/GardensPage";
import IngestionPage from "./pages/IngestionPage";
import QueryPage from "./pages/QueryPage";
import RunsPage from "./pages/RunsPage";
import NextRoot from "./pages-next/NextRoot";

function resolveExternalUrl(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl);
    const localHosts = new Set(["localhost", "127.0.0.1", "::1"]);
    if (!localHosts.has(parsed.hostname)) {
      return rawUrl;
    }
    const next = new URL(rawUrl);
    next.hostname = window.location.hostname;
    next.protocol = window.location.protocol;
    return next.toString();
  } catch {
    return rawUrl;
  }
}

function App() {
  const [knoxxAdminUrl, setKnoxxAdminUrl] = useState<string>("");

  useEffect(() => {
    getFrontendConfig()
      .then((cfg) => setKnoxxAdminUrl(resolveExternalUrl(cfg.knoxx_admin_url)))
      .catch(() => setKnoxxAdminUrl(""));
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-100 via-slate-100 to-slate-200 dark:from-slate-900 dark:via-slate-900 dark:to-slate-800 text-slate-900 dark:text-slate-100 transition-colors duration-200">
      <header className="border-b border-slate-200 dark:border-slate-800 bg-white/85 dark:bg-slate-900/85 backdrop-blur">
        <div className="mx-auto flex max-w-[1500px] items-center justify-between px-4 py-3">
          <h1 className="text-lg font-semibold">Knoxx</h1>
          <nav className="flex gap-2 rounded-lg bg-slate-100 dark:bg-slate-800 p-1">
            <NavLink
              to="/"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              Chat Lab
            </NavLink>
            <NavLink
              to="/runs"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              Runs
            </NavLink>
            <NavLink
              to="/cms"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              CMS
            </NavLink>
            <NavLink
              to="/ingestion"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              Ingestion
            </NavLink>
            <NavLink
              to="/query"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              Query
            </NavLink>
            <NavLink
              to="/gardens"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm ${
                  isActive ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow" : "text-slate-600 dark:text-slate-300 hover:bg-white/50 dark:hover:bg-slate-700/50"
                }`
              }
            >
              Gardens
            </NavLink>
            <NavLink
              to="/next/dashboard"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-500/10 ${
                  isActive ? "bg-white dark:bg-slate-700 shadow" : ""
                }`
              }
            >
              Next UI ✨
            </NavLink>
            {knoxxAdminUrl ? (
              <a
                href={knoxxAdminUrl}
                target="_blank"
                rel="noreferrer"
                className="rounded-md px-3 py-1.5 text-sm text-slate-600 dark:text-slate-300 hover:bg-white dark:hover:bg-slate-700/50"
              >
                Knoxx Admin
              </a>
            ) : null}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-[1500px] p-4">
        <Routes>
          <Route path="/" element={<ChatLabPage />} />
          <Route path="/runs" element={<RunsPage />} />
          <Route path="/cms" element={<CmsPage />} />
          <Route path="/ingestion" element={<IngestionPage />} />
          <Route path="/query" element={<QueryPage />} />
          <Route path="/gardens" element={<GardensPage />} />
          <Route path="/next/*" element={<NextRoot />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
