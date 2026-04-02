import React, { useEffect } from 'react';
import { Routes, Route, NavLink, Navigate } from 'react-router-dom';
import DashboardPage from './DashboardPage';
import SettingsPage from './SettingsPage';
import DocumentsPage from './DocumentsPage';
import ChatPage from './ChatPage';
import VectorsPage from './VectorsPage';
import SourceDocPage from './SourceDocPage';
import SidebarOpsStatus from '../components-next/SidebarOpsStatus';

const navItems = [
  { label: 'Dashboard', path: '/next/dashboard' },
  { label: 'Chat', path: '/next/chat' },
  { label: 'Documents', path: '/next/documents' },
  { label: 'Vectors', path: '/next/vectors' },
  { label: 'Settings', path: '/next/settings' },
];

export default function NextRoot() {
  useEffect(() => {
    document.documentElement.classList.add('dark');
  }, []);

  return (
    <div className="flex h-[calc(100vh-80px)] w-full flex-col bg-slate-50 dark:bg-slate-900 overflow-hidden text-slate-900 dark:text-slate-100 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg transition-colors duration-200">
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar Nav */}
        <aside className="w-64 flex flex-col bg-white dark:bg-slate-800 border-r border-slate-200 dark:border-slate-700 transition-colors duration-200">
          <div className="p-4 border-b border-slate-100 dark:border-slate-700/50 flex items-center justify-between">
            <h2 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600 dark:from-blue-400 dark:to-indigo-400">Knoxx Ops</h2>
            <button 
              onClick={() => document.documentElement.classList.toggle('dark')}
              className="p-1.5 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 transition-colors"
              title="Toggle Dark Mode"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>
            </button>
          </div>
          <nav className="py-4">
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
          <SidebarOpsStatus />
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 flex flex-col min-w-0 bg-slate-50/50 dark:bg-slate-900/50 relative overflow-y-auto p-0 transition-colors duration-200">
          <Routes>
            <Route path="/" element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="chat" element={<ChatPage />} />
            <Route path="documents" element={<DocumentsPage />} />
            <Route path="docs/view" element={<SourceDocPage />} />
            <Route path="vectors" element={<VectorsPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="dashboard" replace />} />
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
