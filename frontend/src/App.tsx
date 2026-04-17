import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { getFrontendConfig } from "./lib/api";
import { opsRoutes, remapLegacyOpsPath } from "./lib/app-routes";
import AuthBoundary from "./pages/AuthContext";
import { useAuth } from "./pages/useAuth";
import ChatPage from "./pages/ChatPage";
import CmsPage from "./pages/CmsPage";
import ContractsPage from "./pages/ContractsPage";
import GardensPage from "./pages/GardensPage";
import OpsRoot from "./pages/OpsRoot";
import TranslationReviewPage from "./pages/TranslationReviewPage";


function App() {
  const [knoxxAdminUrl] = useState<string>("");
  let auth: ReturnType<typeof useAuth> | null = null;
  try { auth = useAuth(); } catch { /* not inside AuthBoundary yet */ }

  const navLinkClass = ({ isActive }: { isActive: boolean }): string =>
    `app-shell__nav-link${isActive ? " app-shell__nav-link--active" : ""}`;

  useEffect(() => {
    document.documentElement.classList.add("dark");
  }, []);

  return (
    <AuthBoundary>
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
            <NavLink to="/contracts" className={navLinkClass}>
              Contracts
            </NavLink>
            <NavLink to="/gardens" className={navLinkClass}>
              Gardens
            </NavLink>
            <NavLink to="/translations" className={navLinkClass}>
              Translations
            </NavLink>
            <NavLink to={opsRoutes.admin} className={navLinkClass}>
              Admin
            </NavLink>
          </nav>
          {/* User menu */}
          <UserMenu />
        </div>
      </header>

      {/* Main content area */}
      <main className="app-shell__main">
        <Routes>
          {/* Regular pages */}
          <Route path="/" element={<ChatPage />} />
          <Route path="/cms" element={<CmsPage />} />
          <Route path="/contracts" element={<ContractsPage />} />
          <Route path="/gardens" element={<GardensPage />} />
          <Route path="/translations" element={<TranslationReviewPage />} />
          <Route path="/translations/:documentId/:targetLang" element={<TranslationReviewPage />} />
          <Route path="/ops/*" element={<OpsRoot />} />
          <Route path="/next/*" element={<LegacyOpsRedirect />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
    </AuthBoundary>
  );
}

function LegacyOpsRedirect() {
  const location = useLocation();
  return <Navigate to={remapLegacyOpsPath(location.pathname, location.search, location.hash)} replace />;
}

function UserMenu() {
  const [open, setOpen] = useState(false);
  let auth: ReturnType<typeof useAuth> | null = null;
  try { auth = useAuth(); } catch { return null; }
  if (!auth?.user) return null;

  return (
    <div className="relative ml-4">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800 transition"
      >
        <span className="h-6 w-6 rounded-full bg-blue-600 flex items-center justify-center text-xs font-bold text-white">
          {(auth.user.displayName || auth.user.email)[0].toUpperCase()}
        </span>
        <span className="hidden md:inline">{auth.user.displayName || auth.user.email}</span>
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 top-full mt-1 z-20 w-56 rounded-lg border border-slate-700 bg-slate-900 py-1 shadow-xl">
            <div className="px-3 py-2 border-b border-slate-800">
              <p className="text-sm font-medium text-white truncate">{auth.user.displayName}</p>
              <p className="text-xs text-slate-400 truncate">{auth.user.email}</p>
              {auth.org && <p className="text-xs text-slate-500 mt-0.5">{auth.org.name}</p>}
            </div>
            <button
              onClick={async () => { setOpen(false); await auth!.logout(); }}
              className="w-full px-3 py-2 text-left text-sm text-red-400 hover:bg-slate-800 transition"
            >
              Sign out
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default App;
