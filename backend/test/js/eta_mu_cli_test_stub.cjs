// Test-only stand-in for @open-hax/eta-mu-cli.
//
// The production shadow-cljs ESM builds import @open-hax/eta-mu-cli directly via
// :keep-as-import. The :node-test target is CommonJS, so it cannot require that
// import-only package while merely loading namespaces that do not exercise the SDK.
// Tests that need SDK behavior should replace this with explicit fakes at the call site.

module.exports = {};
