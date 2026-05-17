
shadow.cljs.devtools.client.env.module_loaded('app');

try { knoxx.frontend.core.init(); } catch (e) { console.error("An error occurred when calling (knoxx.frontend.core/init)"); console.error(e); }