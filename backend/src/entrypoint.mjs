// Knoxx Node entrypoint. All app logic lives in CLJS (backend/src/cljs).
// Build with: npx shadow-cljs release app
// Run with:   node src/entrypoint.mjs
import { bootstrap } from "../dist/app.js";

await bootstrap();
