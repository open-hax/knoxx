import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.bootstrap.js";
goog.provide('knoxx.backend.entrypoint');
/**
 * shadow-cljs module init-fn. Starts the Knoxx backend.
 */
knoxx.backend.entrypoint.init = (function knoxx$backend$entrypoint$init(){
return knoxx.backend.bootstrap.bootstrap_BANG_();
});

//# sourceMappingURL=knoxx.backend.entrypoint.js.map
