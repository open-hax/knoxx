import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { getFrontendConfig } from "./lib/api";
import { opsRoutes, remapLegacyOpsPath } from "./lib/app-routes";
import { Shell } from "./shell/Shell";
import ChatPage from "./pages/ChatPage";
import CmsPage from "./pages/CmsPage";
import GardensPage from "./pages/GardensPage";
import IngestionPage from "./pages/IngestionPage";
import OpsRoot from "./pages/OpsRoot";
import QueryPage from "./pages/QueryPage";
import RunsPage from "./pages/RunsPage";
import TranslationReviewPage from "./pages/TranslationReviewPage";
import WorkbenchPage from "./pages/WorkbenchPage";

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

  const navLinkClass = ({ isActive }: { isActive: boolean }): string =>
    `app-shell__nav-link${isActive ? " app-shell__nav-link--active" : ""}`;

  useEffect(() => {
    document.documentElement.classList.add("dark");
    getFrontendConfig()
      .then((cfg) => setKnoxxAdminUrl(resolveExternalUrl(cfg.knoxx_admin_url)))
      .catch(() => setKnoxxAdminUrl(""));
  }, []);

  return (
    <div className="app-shell">
      {/* Main navbar - always visible across all workplaces */}
      <header className="app-shell__header">
        <div className="app-shell__header-inner">
          <h1 className="app-shell__brand">Knoxx</h1>
          <nav className="app-shell__nav">
            <NavLink to="/" className={navLinkClass}>
              Chat
            </NavLink>
            <NavLink to="/cms" className={navLinkClass}>
              CMS
            </NavLink>
            <NavLink to="/ingestion" className={navLinkClass}>
              Ingestion
            </NavLink>
            <NavLink to="/query" className={navLinkClass}>
              Query
            </NavLink>
            <NavLink to="/gardens" className={navLinkClass}>
              Gardens
            </NavLink>
            <NavLink to="/runs" className={navLinkClass}>
              Runs
            </NavLink>
            <NavLink to="/translations" className={navLinkClass}>
              Translations
            </NavLink>
            <NavLink to="/workbench/dashboard" className={navLinkClass}>
              Workbench
            </NavLink>
            <NavLink to={opsRoutes.admin} className={navLinkClass}>
              Admin
            </NavLink>
            {knoxxAdminUrl ? (
              <a
                href={knoxxAdminUrl}
                target="_blank"
                rel="noreferrer"
                className="app-shell__nav-link"
              >
                Legacy Admin
              </a>
            ) : null}
          </nav>
        </div>
      </header>

      {/* Main content area - switches between regular pages and workbench Shell */}
      <main className="app-shell__main">
        <Routes>
          {/* Workbench routes - Shell provides left context bar, center canvas, right inspection panel, status bar */}
          <Route
            path="/workbench/*"
            element={
              <Shell>
                <Routes>
                  <Route index element={<Navigate to="/workbench/dashboard" replace />} />
                  <Route path="dashboard" element={<WorkbenchPage view="dashboard" />} />
                  <Route path="content" element={<WorkbenchPage view="content" />} />
                  <Route path="content/:docId" element={<WorkbenchPage view="content" />} />
                  <Route path="review" element={<WorkbenchPage view="review" />} />
                  <Route path="memory" element={<WorkbenchPage view="memory" />} />
                  <Route path="agents" element={<WorkbenchPage view="agents" />} />
                  <Route path="ops" element={<WorkbenchPage view="ops" />} />
                  <Route path="*" element={<Navigate to="/workbench/dashboard" replace />} />
                </Routes>
              </Shell>
            }
          />

          {/* Regular pages */}
          <Route path="/" element={<ChatPage />} />
          <Route path="/cms" element={<CmsPage />} />
          <Route path="/ingestion" element={<IngestionPage />} />
          <Route path="/query" element={<QueryPage />} />
          <Route path="/gardens" element={<GardensPage />} />
          <Route path="/runs" element={<RunsPage />} />
          <Route path="/translations" element={<TranslationReviewPage />} />
          <Route path="/translations/:documentId/:targetLang" element={<TranslationReviewPage />} />
          <Route path="/ops/*" element={<OpsRoot />} />
          <Route path="/next/*" element={<LegacyOpsRedirect />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function LegacyOpsRedirect() {
  const location = useLocation();
  return <Navigate to={remapLegacyOpsPath(location.pathname, location.search, location.hash)} replace />;
}

export default App;
