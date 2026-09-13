# Frontend domain type ownership

The inherited `lib/types.ts` was 814 lines, exceeding the repository size gate's
800-line error limit. It now retains 273 lines and type-only compatibility
exports. Existing chat, administration and translation modules own their exact
original wire definitions, at 317, 187 and 173 lines respectively. No fields,
optionality, unions, API bodies or runtime behavior changed.

All four modules are below the unchanged 400-line warning threshold. Every
previous consumer can keep its existing import, and no new TypeScript source
path or bridge export was introduced. The type-only reexports emit no additional
runtime imports; the existing workspace/chat type relationship stays type-only.

Verification: complete `pnpm -C frontend typecheck` passes, and the complete
Vitest command passes **45 files / 219 executed tests**. Its **41 existing TODO
cases remain unexecuted**, explicitly reported by Vitest. These are separate
from the passing native ClojureScript suite. Production output remained frozen
throughout this source-only change for the real-stack browser tour.

The frontend-only size inventory is now **6 errors / 21 warnings**. This bounded
extraction resolves the shared type file; it does not waive the remaining live
legacy module debt or relax any migration or size gate.
