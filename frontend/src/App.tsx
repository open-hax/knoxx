import { Navigate, Route, Routes } from "react-router-dom";
import { Shell } from "./shell/Shell";
import { opsRoutes, remapLegacyOpsPath } from "./lib/app-routes";
import ChatPage from "./pages/ChatPage";
import CmsPage from "./pages/CmsPage";
import GardensPage from "./pages/GardensPage";
import IngestionPage from "./pages/IngestionPage";
import OpsRoot from "./pages/OpsRoot";
import QueryPage from "./pages/QueryPage";
import RunsPage from "./pages/RunsPage";
import TranslationPage from "./pages/TranslationPage";
import WorkbenchPage from "./pages/WorkbenchPage";

function App() {
  return (
    <Routes>
      {/* Workbench routes - use new Shell layout */}
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

      {/* Legacy routes - keep existing layout */}
      <Route path="/" element={<ChatPage />} />
      <Route path="/cms" element={<CmsPage />} />
      <Route path="/ingestion" element={<IngestionPage />} />
      <Route path="/query" element={<QueryPage />} />
      <Route path="/gardens" element={<GardensPage />} />
      <Route path="/runs" element={<RunsPage />} />
      <Route path="/translations" element={<TranslationPage />} />
      <Route path="/ops/*" element={<OpsRoot />} />
      <Route path="/next/*" element={<LegacyOpsRedirect />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function LegacyOpsRedirect() {
  const location = window.location;
  const newPath = remapLegacyOpsPath(location.pathname, location.search, location.hash);
  return <Navigate to={newPath} replace />;
}

export default App;
