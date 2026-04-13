import React, { useEffect } from 'react';
import { Routes, Route, NavLink, Navigate } from 'react-router-dom';
import { opsRoutes, joinPath, OPS_BASE_PATH } from '../lib/app-routes';
import DashboardPage from './DashboardPage';
import SettingsPage from './SettingsPage';
import DocumentsPage from './DocumentsPage';
import VectorsPage from './VectorsPage';
import RawGraphExportPage from './RawGraphExportPage';
import SourceDocPage from './SourceDocPage';
import AgentsPage from './AgentsPage';
import AdminPage from './AdminPage';
import SidebarOpsStatus from '../components/SidebarOpsStatus';
import TranslationReviewPage from './TranslationReviewPage';

const navItems = [
  { label: 'Dashboard', path: opsRoutes.dashboard },
  { label: 'Lakes', path: opsRoutes.documents },
  { label: 'Agents', path: opsRoutes.agents },
  { label: 'Translations', path: joinPath(OPS_BASE_PATH, 'translations') },
  { label: 'Graph', path: opsRoutes.vectors },
  { label: 'Settings', path: opsRoutes.settings },
  { label: 'Admin', path: opsRoutes.admin },
];

export default function OpsRoot() {
  useEffect(() => {
    document.documentElement.classList.add('dark');
  }, []);

  return (
    <div className="flex h-[calc(100vh-80px)] w-full flex-col bg-slate-50 dark:bg-slate-900 overflow-hidden text-slate-900 dark:text-slate-100 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg transition-colors duration-200">
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar Nav */}
        <aside className="w-64 min-h-0 overflow-hidden flex flex-col bg-white dark:bg-slate-800 border-r border-slate-200 dark:border-slate-700 transition-colors duration-200 h-full">
          {/* Fixed Header */}
          <div className="shrink-0 p-4 border-b border-slate-100 dark:border-slate-700/50 flex items-center justify-between">
            <h2 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600 dark:from-blue-400 dark:to-indigo-400">Knoxx Ops</h2>
            <button 
              onClick={() => document.documentElement.classList.toggle('dark')}
              className="p-1.5 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 transition-colors"
              title="Toggle Dark Mode"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>
            </button>
          </div>
          {/* Scrollable Navigation */}
          <nav className="flex-1 overflow-y-auto py-4 min-h-0">
            <ul className="space-y-1 px-3">
              {navItems.map((item) => (
                <li key={item.path}>
                  <NavLink
                    to={item.path}
                    className={({ isActive }) =>
                      `block px-3 py-2 rounded-md font-medium text-sm transition-colors ${
                        isActive
                          ? 'bg-blue-50 text-blue-700 dark:bg-blue-500/10 dark:text-blue-400'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-700/50 dark:hover:text-slate-200'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
          {/* Fixed Status Panel */}
          <div className="min-h-0 shrink-0 overflow-y-auto max-h-72">
            <SidebarOpsStatus />
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 flex flex-col min-w-0 bg-slate-50/50 dark:bg-slate-900/50 relative overflow-y-auto p-0 transition-colors duration-200">
          <Routes>
            <Route path="/" element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="documents" element={<DocumentsPage />} />
            <Route path="docs/view" element={<SourceDocPage />} />
            <Route path="vectors" element={<VectorsPage />} />
            <Route path="agents" element={<AgentsPage />} />
            <Route path="translations" element={<TranslationReviewPage />} />
            <Route path="translations/:documentId/:targetLang" element={<TranslationReviewPage />} />
            <Route path="graph-export-debug" element={<RawGraphExportPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="*" element={<Navigate to={opsRoutes.dashboard} replace />} />
          </Routes>
        </main>
      </div>

      {/* Legacy Fallback Link Requirement */}
      <footer className="shrink-0 text-center py-3 bg-white dark:bg-slate-800 border-t border-slate-200 dark:border-slate-700 text-sm text-slate-500 dark:text-slate-400 transition-colors duration-200">
        Prefer the previous interface?{' '}
        <NavLink to="/" className="text-blue-600 dark:text-blue-400 hover:underline">
          Open Legacy UI
        </NavLink>
      </footer>
    </div>
  );
}
