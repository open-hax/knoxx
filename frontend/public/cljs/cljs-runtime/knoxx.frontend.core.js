goog.provide('knoxx.frontend.core');

knoxx.frontend.core.app = (function (){var G__7081 = (function knoxx$frontend$core$app_render(props__6273__auto__,maybe_ref__6274__auto__){
var vec__7082 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__6273__auto__),maybe_ref__6274__auto__], null);

var G__7085 = "div";
var G__7086 = (function (){var obj7088 = ({"className":"min-h-screen bg-slate-900 text-white p-8","children":[(function (){var G__7089 = "h1";
var G__7090 = (function (){var obj7092 = ({"className":"text-3xl font-bold mb-4","children":"Knoxx"});
return obj7092;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__7089,G__7090) : helix.core.jsx.call(null,G__7089,G__7090));
})(),(function (){var G__7093 = "p";
var G__7094 = (function (){var obj7096 = ({"children":"Hello from shadow-cljs + helix!"});
return obj7096;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__7093,G__7094) : helix.core.jsx.call(null,G__7093,G__7094));
})(),(function (){var G__7097 = module$dist$bridge$knoxx_frontend_bridge_es.EmptyState;
var G__7098 = (function (){var obj7100 = ({"title":"Nothing here yet","message":"This TS component is imported from shadow-cljs.","icon":"\uD83E\uDDEA"});
return obj7100;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__7097,G__7098) : helix.core.jsx.call(null,G__7097,G__7098));
})()]});
return obj7088;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__7085,G__7086) : helix.core.jsxs.call(null,G__7085,G__7086));
});
if(goog.DEBUG === true){
var G__7101 = G__7081;
(G__7101.displayName = "knoxx.frontend.core/app");

return G__7101;
} else {
return G__7081;
}
})();



knoxx.frontend.core.after_load = (function knoxx$frontend$core$after_load(){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[knoxx-frontend] hot reload"], 0));
});
knoxx.frontend.core.init = (function knoxx$frontend$core$init(){
var root_el = document.getElementById("root");
var root = shadow.esm.esm_import$react_dom$client.createRoot(root_el);
return root.render((function (){var G__7102 = knoxx.frontend.core.app;
var G__7103 = ({});
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__7102,G__7103) : helix.core.jsx.call(null,G__7102,G__7103));
})());
});

//# sourceMappingURL=knoxx.frontend.core.js.map
