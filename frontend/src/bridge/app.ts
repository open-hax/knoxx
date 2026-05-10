// App bridge: stable exports for the shadow-cljs router to mount.
//
// IMPORTANT:
// - This file is built by Vite in library mode and imported by shadow-cljs.
// - Keep exports stable (no hashed chunk names).
// - React is bundled by shadow-cljs; this bridge must not attempt to provide its own React.

export { default as AuthBoundary } from "../pages/AuthContext";
export { useAuth } from "../pages/useAuth";

// TSX pages that remain implemented in React/TS while shadow-cljs owns routing.
// Keep these as stable named exports so CLJS can reference them.
export { default as ChatPage } from "../pages/ChatPage";
export { default as MailPage } from "../pages/MailPage";
export { default as BroadcastStudioPage } from "../pages/BroadcastStudioPage";
export { default as ContractsPage } from "../pages/ContractsPage";
export { default as DataPage } from "../pages/DataPage";
export { default as GardensPage } from "../pages/GardensPage";
export { default as TranslationReviewPage } from "../pages/TranslationReviewPage";
export { default as OpsRoot } from "../pages/OpsRoot";

// NOTE: CMS pages currently import CSS Modules; ensure the bridge build remains
// single-file + compatible with shadow-cljs file resolution when adding these.
export { default as CmsPage } from "../pages/CmsPage";
export { VisualCmsEditorPage } from "../pages/VisualCmsEditorPage";
