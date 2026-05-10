// CommonJS shim that lazily loads the ESM-only @open-hax/eta-mu-cli package.
//
// shadow-cljs :node-test emits CJS bundles; requiring an ESM-only package that
// only defines an "import" export condition will throw ERR_PACKAGE_PATH_NOT_EXPORTED.
//
// This shim keeps require() happy and only performs a dynamic import() when/if
// Knoxx actually needs eta-mu functionality.

let sdkPromise = null;

async function load() {
  if (!sdkPromise) {
    // Avoid ClosureCompiler trying (and failing) to transpile a dynamic import
    // expression by constructing it indirectly.
    const importer = new Function('specifier', 'return import(specifier)');
    sdkPromise = importer('@open-hax/eta-mu-cli');
  }
  return sdkPromise;
}

module.exports = {
  __esModule: true,
  load,
};
