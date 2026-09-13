# Actor identity editors and canonical graph projection

The actor directory now delegates its profile and provider credential forms to
the existing IdentitySection owner. Its state, filtering, expansion and saves
remain in the directory, along with membership roles and tool policies. Existing
administration types own the exact draft shapes, and existing administration
helpers own descriptor selection and draft construction. Directory/identity/
helpers/types are 356/251/312/377 lines. Axxium-bound actor IDs and identity email
remain read-only; global status and credential writes retain their capability
checks. Three new interaction tests verify credential payloads, secret input type,
identity preservation, permission-hidden writes and draft retention on collapse.

The canonical graph page now delegates its pure projection to the existing graph
helper. Shared graph types own layout and render payloads. The projection retains
lake and dangling-edge filtering, degree ranking, node/edge limits, selection
indexes and deterministic layout. Three new tests exercise cross-lake ranking,
edge limits, stable layout, empty data and zero node budgets. The graph page,
helper and shared types are 349/379/322 lines.

An intermediate legacy compilation caught a mechanical extraction mistake:
helper names had also been inserted into the WebGL constructor options. Those
unintended option fields were removed, preserving the original render setup.
The completed combined gates pass full typecheck and isolated legacy emission,
plus **45 Vitest files / 264 executed tests / 0 failures**. The **41 existing TODO
cases remain unexecuted**. This combined evidence also includes the independent
Contracts controller work and the documented runtime API boundary extraction.
All seven source-file size checks pass without changing thresholds. Production
output stayed frozen throughout.
