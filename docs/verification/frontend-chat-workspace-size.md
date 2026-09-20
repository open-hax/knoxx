# Chat session history and media input boundaries

The existing runtime panel now owns recent session history, while the workspace
sidebar keeps layout and explorer composition. The sidebar is 378 lines and
the runtime panel is 305, both below the unchanged 400-line warning threshold.
The live/open indicators, exact session callback, scroll pagination threshold
and busy/exhausted guards retain their original behavior. The scroll region now
has an accessible name for navigation and browser verification.

The existing media component owns MIME classification, preview creation and
size formatting. Attachment draft types remain at the existing chat type
boundary; attachment IDs retain their original generator in the existing ID
module. The file-input component keeps selection, drag/drop, validation,
attachment presentation and removal. Input/content/types/ID files are now
391/385/337/11 lines. No format, size default, accepted extension, identifier
format or error policy changed.

Six new regressions cover live session selection, guarded pagination, empty
history refresh, browser file metadata, oversized-file error drafts and image
preview/MIME handling. The workspace test file passes **7 tests total**. Scoped
size gates and complete typecheck pass, as does full isolated legacy emission.
The combined full Vitest run passes **45 files / 235 executed tests**; its
**41 existing TODO cases remain explicitly unexecuted**.

No new TypeScript paths or bridge exports were added. Production output stayed
frozen; these checks use source inputs and an ignored isolated emit directory.
