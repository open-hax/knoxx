// CommonJS shim that lazily loads the ESM-only @open-hax/eta-mu-cli package.
//
// Shadow-cljs :node-test outputs CJS bundles; requiring an ESM-only package that
// only defines an "import" export condition will throw ERR_PACKAGE_PATH_NOT_EXPORTED.
//
// This shim keeps require() happy and only performs a dynamic import() when/if
// Knoxx actually needs eta-mu functionality.

let sdkPromise = null;

async function load() {
  if (!sdkPromise) {
    sdkPromise = import('@open-hax/eta-mu-cli');
  }
  return sdkPromise;
}

module.exports = {
  __esModule: true,
  load,
};
