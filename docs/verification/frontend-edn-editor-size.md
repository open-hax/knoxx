# EDN editor size recovery

The existing `EdnEditor.tsx` exceeded the 400-line warning limit at 568 lines. Its EDN balance diagnostics and CodeMirror lint, highlighting, and theme construction now live in the existing `lib/edn.ts` boundary. The component retains editor ownership, React state, callbacks, read-only updates, toolbar, and status rendering. Function bodies and the existing visual-CMS parser/serializer were preserved.

No TypeScript or TSX path was added. The component is now **337 lines**, the existing EDN boundary **373 lines**, and its existing visual-CMS test file **153 lines**; the unchanged size gate reports **0 errors, 0 warnings** for all three.

Fresh verification:

- Advertised `pnpm typecheck`: exit **0**, including the final test imports.
- Two meaningful tests added to the existing visual-CMS suite check EDN comment/string-aware balance diagnostics and construct actual CodeMirror state with the extracted lint, highlighting, and palette extensions.
- Existing visual-CMS load/save tests exercise the parser/serializer consumer after the shared module acquired editor imports; existing contract page tests retain their editing/submission coverage.
- The intended narrow `pnpm test -- ...` invocation forwarded the delimiter to Vitest, which ran the full suite. Actual result: **45 files passed, 221 tests passed, 41 pre-existing todo, no failures**. No todo was counted as passed and no test was disabled for this change.

Self-review checked preserved helper bodies, imports, component lifecycle, public props, and the unchanged parser/serializer. Theme and editor construction remain inside functions; importing the shared module does not create an editor. Production output was frozen for the coordinating browser walkthrough and was not rebuilt or modified by this lane. Final combined builds, browser evidence, and PR reviews remain with that lane.
