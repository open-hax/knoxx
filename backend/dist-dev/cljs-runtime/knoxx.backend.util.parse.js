import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.util.parse');
knoxx.backend.util.parse.parse_positive_int = (function knoxx$backend$util$parse$parse_positive_int(value){
var n = ((typeof value === 'string')?(cljs.core.truth_(cljs.core.re_find(/\\./,value))?NaN:parseInt(value,(10))):((typeof value === 'number')?value:NaN
));
if(((typeof n === 'number') && (((cljs.core.not(isNaN(n))) && ((n > (0))))))){
return n;
} else {
return null;
}
});
knoxx.backend.util.parse.truthy_param_QMARK_ = (function knoxx$backend$util$parse$truthy_param_QMARK_(value){
if(value === true){
return true;
} else {
if(typeof value === 'number'){
return (value > (0));
} else {
if(typeof value === 'string'){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["force",null,"yes",null,"true",null,"on",null,"1",null], null), null),clojure.string.lower_case(clojure.string.trim(value)));
} else {
return false;

}
}
}
});

//# sourceMappingURL=knoxx.backend.util.parse.js.map
