# Chat projection and voice ownership

The existing derived-state module now owns bounded tool receipt formatting,
while the receipt component owns rendering. Persisted transcript reconstruction
now lives with the existing session persistence suite; workspace utilities retain
compatible exports and own live trace deltas and browser file classification.
The moved bodies retain their limits, truncation markers, fence handling and
failed-run recovery behavior. The files are now 301 lines for the receipt view,
380 for derived state, 374 for persistence, 379 for workspace utilities and 354
for chat types, below the unchanged 400-line warning threshold.

The existing voice hook now owns conversation recording and its persisted
silence threshold. The main pane keeps scrolling and message presentation, and
its parent owns the composition prop contract. Main pane, parent and voice hook
are now 377, 209 and 240 lines. Microphone acquisition still occurs through the
existing recorder lifecycle, with unchanged defaults, clamping and playback
coordination.

Four new tests cover receipt null sentinels and human summaries, embedded fences
and collection/depth limits, the exact raw-output truncation boundary, and failed
run recovery with reasoning and tool receipt provenance. Focused projection tests
pass 20 cases; existing voice/chunk/pane tests pass 17 cases. The combined full
Vitest run passes **45 files / 239 executed tests / 0 failures**. The **41 existing
TODO cases remain unexecuted**. Complete typecheck, isolated legacy emission and
all eight source-file size checks pass without warnings.

No new TypeScript paths or bridge exports were introduced. The production tree
remained frozen throughout; legacy emission used the ignored verification output
directory. Formatting helper implementations were preserved individually where
similarly named functions have different established behavior.
