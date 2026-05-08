import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('shadow.esm');
/**
 * dynamic import(path) where path is relative to the :output-dir of the build
 */
shadow.esm.dynamic_import = (function shadow$esm$dynamic_import(what){
return shadow_esm_import(what);
});
shadow.esm.loadables = ({});
shadow.esm.loaded = ({});
shadow.esm.get_loaded = (function shadow$esm$get_loaded(the_name){
return (shadow.esm.loaded[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(the_name))]);
});
shadow.esm.load_by_name = (function shadow$esm$load_by_name(the_name){
var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(the_name));
var info = (shadow.esm.loadables[s]);
if(cljs.core.truth_(info)){
} else {
throw (new Error((""+"could not find loadable info for: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))));
}

var module__$1 = (info["module"]);
var accessor = (info["get"]);
return shadow.esm.dynamic_import((""+"./"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(module__$1)+".js")).then((function (module__$2){
(shadow.esm.loaded[s] = accessor);

return accessor;
}));
});
shadow.esm.add_loadable = (function shadow$esm$add_loadable(the_name,module,accessor){
return (shadow.esm.loadables[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(the_name))] = ({"module": module, "get": accessor}));
});

//# sourceMappingURL=shadow.esm.js.map
