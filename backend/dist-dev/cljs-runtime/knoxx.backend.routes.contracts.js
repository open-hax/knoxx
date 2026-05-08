import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.set.js";
import "./clojure.string.js";
import "./cljs.reader.js";
import "./knoxx.backend.contracts.resolve.js";
import "./knoxx.backend.events.runtime.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.runtime.actor_scope.js";
import "./knoxx.backend.runtime.contract_loader.js";
import "./knoxx.backend.runtime.contract_validator.js";
import "./knoxx.backend.util.time.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
goog.provide('knoxx.backend.routes.contracts');
knoxx.backend.routes.contracts.contracts_index_key = "contracts:index";
knoxx.backend.routes.contracts.contract_watch_debounce_ms = (350);
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.routes !== 'undefined') && (typeof knoxx.backend.routes.contracts !== 'undefined') && (typeof knoxx.backend.routes.contracts.contract_watchers_STAR_ !== 'undefined')){
} else {
knoxx.backend.routes.contracts.contract_watchers_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.routes !== 'undefined') && (typeof knoxx.backend.routes.contracts !== 'undefined') && (typeof knoxx.backend.routes.contracts.contract_watch_timer_STAR_ !== 'undefined')){
} else {
knoxx.backend.routes.contracts.contract_watch_timer_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.routes !== 'undefined') && (typeof knoxx.backend.routes.contracts !== 'undefined') && (typeof knoxx.backend.routes.contracts.contract_watch_running_QMARK__STAR_ !== 'undefined')){
} else {
knoxx.backend.routes.contracts.contract_watch_running_QMARK__STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false);
}
knoxx.backend.routes.contracts.keyword__GT_slug = (function knoxx$backend$routes$contracts$keyword__GT_slug(value){
var base = cljs.core.last(clojure.string.split.cljs$core$IFn$_invoke$arity$2((((value instanceof cljs.core.Keyword))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))
),/\//));
var G__54325 = base;
var G__54325__$1 = (((G__54325 == null))?null:clojure.string.replace(G__54325,/^-+/,""));
var G__54325__$2 = (((G__54325__$1 == null))?null:clojure.string.replace(G__54325__$1,/-/,"_"));
var G__54325__$3 = (((G__54325__$2 == null))?null:clojure.string.trim(G__54325__$2));
if((G__54325__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__54325__$3);
}
});
knoxx.backend.routes.contracts.normalize_contract_class = (function knoxx$backend$routes$contracts$normalize_contract_class(raw){
return (knoxx.backend.runtime.contract_loader.normalize_contract_class.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.normalize_contract_class.cljs$core$IFn$_invoke$arity$1(raw) : knoxx.backend.runtime.contract_loader.normalize_contract_class.call(null,raw));
});
knoxx.backend.routes.contracts.contract_id__GT_index_key = (function knoxx$backend$routes$contracts$contract_id__GT_index_key(contract_class,contract_id){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.contracts.normalize_contract_class(contract_class))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id));
});
knoxx.backend.routes.contracts.model_id__GT_slug = (function knoxx$backend$routes$contracts$model_id__GT_slug(model_id){
var G__54326 = model_id;
var G__54326__$1 = (((G__54326 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54326)));
var G__54326__$2 = (((G__54326__$1 == null))?null:clojure.string.replace(G__54326__$1,/[^A-Za-z0-9._-]+/,"_"));
if((G__54326__$2 == null)){
return null;
} else {
return clojure.string.replace(G__54326__$2,/_+/,"_");
}
});
knoxx.backend.routes.contracts.parsed_record_id = (function knoxx$backend$routes$contracts$parsed_record_id(contract_class,value){
var G__54328 = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
switch (G__54328) {
case "agents":
var G__54329 = new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(value);
if((G__54329 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54329));
}

break;
case "policies":
var G__54330 = new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(value);
if((G__54330 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54330));
}

break;
case "actors":
var G__54331 = new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(value);
if((G__54331 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54331));
}

break;
case "roles":
var G__54332 = new cljs.core.Keyword("role","id","role/id",-1375589954).cljs$core$IFn$_invoke$arity$1(value);
if((G__54332 == null)){
return null;
} else {
return knoxx.backend.routes.contracts.keyword__GT_slug(G__54332);
}

break;
case "capabilities":
var G__54333 = new cljs.core.Keyword("cap","id","cap/id",-1388434846).cljs$core$IFn$_invoke$arity$1(value);
var G__54333__$1 = (((G__54333 == null))?null:knoxx.backend.routes.contracts.keyword__GT_slug(G__54333));
if((G__54333__$1 == null)){
return null;
} else {
var slug = G__54333__$1;
return (""+"cap_"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug));
}

break;
case "model_families":
var G__54335 = new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(value);
if((G__54335 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54335));
}

break;
case "models":
var G__54336 = new cljs.core.Keyword("model","id","model/id",-1274892501).cljs$core$IFn$_invoke$arity$1(value);
if((G__54336 == null)){
return null;
} else {
return knoxx.backend.routes.contracts.model_id__GT_slug(G__54336);
}

break;
default:
return null;

}
});
knoxx.backend.routes.contracts.parsed_kind_label = (function knoxx$backend$routes$contracts$parsed_kind_label(contract_class,value){
var G__54345 = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
switch (G__54345) {
case "agents":
var G__54346 = new cljs.core.Keyword("contract","kind","contract/kind",1929672067).cljs$core$IFn$_invoke$arity$1(value);
if((G__54346 == null)){
return null;
} else {
return cljs.core.name(G__54346);
}

break;
case "policies":
return "policy";

break;
case "actors":
var G__54347 = new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(value);
if((G__54347 == null)){
return null;
} else {
return cljs.core.name(G__54347);
}

break;
case "roles":
return "role";

break;
case "capabilities":
return "capability";

break;
case "model_families":
return "model-family";

break;
case "models":
return "model";

break;
default:
return "unknown";

}
});
knoxx.backend.routes.contracts.parsed_version = (function knoxx$backend$routes$contracts$parsed_version(contract_class,value){
var G__54348 = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
switch (G__54348) {
case "agents":
var or__5142__auto__ = new cljs.core.Keyword("contract","version","contract/version",-140246352).cljs$core$IFn$_invoke$arity$1(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}

break;
case "policies":
var or__5142__auto__ = new cljs.core.Keyword("contract","version","contract/version",-140246352).cljs$core$IFn$_invoke$arity$1(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}

break;
default:
return (1);

}
});
knoxx.backend.routes.contracts.parsed_enabled_QMARK_ = (function knoxx$backend$routes$contracts$parsed_enabled_QMARK_(contract_class,value){
var G__54352 = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
switch (G__54352) {
case "agents":
return (!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(value) === false));

break;
case "policies":
return (!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(value) === false));

break;
default:
return true;

}
});
knoxx.backend.routes.contracts.validate_contract_edn = (function knoxx$backend$routes$contracts$validate_contract_edn(contract_class,edn_text){
var trimmed = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text)));
if(clojure.string.blank_QMARK_(trimmed)){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"contract","contract",798152745),null,new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"message","message",-406056002),"EDN text is empty"], null)], null),new cljs.core.Keyword(null,"warnings","warnings",-735437651),cljs.core.PersistentVector.EMPTY], null);
} else {
try{var raw_contract = cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1(trimmed);
var contract = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.contracts.normalize_contract_class(contract_class),"agents"))?(knoxx.backend.runtime.actor_scope.normalize_agent_contract.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.actor_scope.normalize_agent_contract.cljs$core$IFn$_invoke$arity$1(raw_contract) : knoxx.backend.runtime.actor_scope.normalize_agent_contract.call(null,raw_contract)):raw_contract);
var base = (knoxx.backend.runtime.contract_validator.validate.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.contract_validator.validate.cljs$core$IFn$_invoke$arity$2(contract_class,contract) : knoxx.backend.runtime.contract_validator.validate.call(null,contract_class,contract));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(base),new cljs.core.Keyword(null,"contract","contract",798152745),contract,new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(base),new cljs.core.Keyword(null,"warnings","warnings",-735437651),cljs.core.PersistentVector.EMPTY], null);
}catch (e54354){var err = e54354;
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"contract","contract",798152745),null,new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"message","message",-406056002),(""+"EDN parse error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))], null)], null),new cljs.core.Keyword(null,"warnings","warnings",-735437651),cljs.core.PersistentVector.EMPTY], null);
}}
});
knoxx.backend.routes.contracts.safe_contract_id = (function knoxx$backend$routes$contracts$safe_contract_id(raw_id){
try{return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"id","id",-1388402092),(knoxx.backend.runtime.contract_loader.safe_path_segment_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.contract_loader.safe_path_segment_BANG_.cljs$core$IFn$_invoke$arity$2(raw_id,"contract-id") : knoxx.backend.runtime.contract_loader.safe_path_segment_BANG_.call(null,raw_id,"contract-id"))], null);
}catch (e54359){var err = e54359;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()], null);
}});
knoxx.backend.routes.contracts.safe_contract_class = (function knoxx$backend$routes$contracts$safe_contract_class(raw_class){
try{return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"class","class",-2030961996),knoxx.backend.routes.contracts.normalize_contract_class(raw_class)], null);
}catch (e54362){var err = e54362;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()], null);
}});
knoxx.backend.routes.contracts.update_id_in_edn_text = (function knoxx$backend$routes$contracts$update_id_in_edn_text(contract_class,edn_text,new_id){
var G__54363 = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
switch (G__54363) {
case "agents":
if(clojure.string.includes_QMARK_(edn_text,":contract/id")){
return clojure.string.replace(edn_text,/:contract\/id\s+\"[^\"]+\"/,(""+":contract/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\""));
} else {
return (""+":contract/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "policies":
if(clojure.string.includes_QMARK_(edn_text,":contract/id")){
return clojure.string.replace(edn_text,/:contract\/id\s+\"[^\"]+\"/,(""+":contract/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\""));
} else {
return (""+":contract/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "actors":
if(clojure.string.includes_QMARK_(edn_text,":actor/id")){
return clojure.string.replace(edn_text,/:actor\/id\s+\"[^\"]+\"/,(""+":actor/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\""));
} else {
return (""+":actor/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "roles":
var keyword_id = (""+":role/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.replace(new_id,/_/,"-")));
if(clojure.string.includes_QMARK_(edn_text,":role/id")){
return clojure.string.replace(edn_text,/:role\/id\s+:[^\s\]}]+/,(""+":role/id "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(keyword_id)));
} else {
return (""+":role/id "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(keyword_id)+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "capabilities":
var slug = clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)),/^cap_/,"");
var keyword_id = (""+":cap/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.replace(slug,/_/,"-")));
if(clojure.string.includes_QMARK_(edn_text,":cap/id")){
return clojure.string.replace(edn_text,/:cap\/id\s+:[^\s\]}]+/,(""+":cap/id "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(keyword_id)));
} else {
return (""+":cap/id "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(keyword_id)+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "model_families":
if(clojure.string.includes_QMARK_(edn_text,":model-family/id")){
return clojure.string.replace(edn_text,/:model-family\/id\s+\"[^\"]+\"/,(""+":model-family/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\""));
} else {
return (""+":model-family/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)+"\"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
case "models":
var model_id = clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new_id)),/_/,":");
if(clojure.string.includes_QMARK_(edn_text,":model/id")){
return clojure.string.replace(edn_text,/:model\/id\s+\"[^\"]+\"/,(""+":model/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+"\""));
} else {
return (""+":model/id \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+"\"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
}

break;
default:
return edn_text;

}
});
knoxx.backend.routes.contracts.contract_metadata_BANG_ = (function knoxx$backend$routes$contracts$contract_metadata_BANG_(config,contract_class,contract_id){
var file_path = (knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,contract_class,contract_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,contract_class,contract_id));
return shadow.esm.esm_import$node_fs$promises.stat(file_path).then((function (stats){
return shadow.esm.esm_import$node_fs$promises.readFile(file_path,"utf8").then((function (edn_text){
var parsed = (function (){try{return cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text)));
}catch (e54370){var _ = e54370;
return null;
}})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"path","path",-188191168),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"ednHash","ednHash",-2013451418),new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"version","version",425292698),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"compiledAt","compiledAt",-863938657)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.contracts.normalize_contract_class(contract_class))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id)+".edn"),(function (){var or__5142__auto__ = (function (){var G__54371 = stats;
var G__54371__$1 = (((G__54371 == null))?null:G__54371.mtime);
if((G__54371__$1 == null)){
return null;
} else {
return G__54371__$1.toISOString();
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.time.now_iso();
}
})(),cljs.core.hash((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text))),knoxx.backend.routes.contracts.normalize_contract_class(contract_class),contract_id,contract_id,knoxx.backend.routes.contracts.parsed_kind_label(contract_class,parsed),knoxx.backend.routes.contracts.parsed_version(contract_class,parsed),knoxx.backend.routes.contracts.parsed_enabled_QMARK_(contract_class,parsed),null]);
}));
}));
});
/**
 * Sync contracts/*.edn → Redis contracts:index set.
 * 
 * Redis is a cache + fast index; disk is canonical. Invalid contract files are
 * omitted by the loader and must not block backend startup or the repair UI.
 */
knoxx.backend.routes.contracts.sync_contract_index_BANG_ = (function knoxx$backend$routes$contracts$sync_contract_index_BANG_(config){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return (knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.call(null,config)).then((function (records){
var ids = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (record){
return knoxx.backend.routes.contracts.contract_id__GT_index_key(new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(record),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record));
}),records))));
return knoxx.backend.redis_client.smembers(client,knoxx.backend.routes.contracts.contracts_index_key).then((function (existing){
var existing_set = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,Array.from((function (){var or__5142__auto__ = existing;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})())));
var desired_set = cljs.core.set(ids);
var to_add = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(clojure.set.difference.cljs$core$IFn$_invoke$arity$2(desired_set,existing_set)));
var to_remove = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(clojure.set.difference.cljs$core$IFn$_invoke$arity$2(existing_set,desired_set)));
var ops = cljs.core.concat.cljs$core$IFn$_invoke$arity$2((function (){var iter__5628__auto__ = (function knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54379(s__54380){
return (new cljs.core.LazySeq(null,(function (){
var s__54380__$1 = s__54380;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__54380__$1);
if(temp__5825__auto__){
var s__54380__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__54380__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__54380__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__54382 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__54381 = (0);
while(true){
if((i__54381 < size__5627__auto__)){
var id = cljs.core._nth(c__5626__auto__,i__54381);
cljs.core.chunk_append(b__54382,knoxx.backend.redis_client.sadd(client,knoxx.backend.routes.contracts.contracts_index_key,id));

var G__54886 = (i__54381 + (1));
i__54381 = G__54886;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__54382),knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54379(cljs.core.chunk_rest(s__54380__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__54382),null);
}
} else {
var id = cljs.core.first(s__54380__$2);
return cljs.core.cons(knoxx.backend.redis_client.sadd(client,knoxx.backend.routes.contracts.contracts_index_key,id),knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54379(cljs.core.rest(s__54380__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(to_add);
})(),(function (){var iter__5628__auto__ = (function knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54413(s__54414){
return (new cljs.core.LazySeq(null,(function (){
var s__54414__$1 = s__54414;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__54414__$1);
if(temp__5825__auto__){
var s__54414__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__54414__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__54414__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__54416 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__54415 = (0);
while(true){
if((i__54415 < size__5627__auto__)){
var id = cljs.core._nth(c__5626__auto__,i__54415);
cljs.core.chunk_append(b__54416,knoxx.backend.redis_client.srem(client,knoxx.backend.routes.contracts.contracts_index_key,id));

var G__54888 = (i__54415 + (1));
i__54415 = G__54888;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__54416),knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54413(cljs.core.chunk_rest(s__54414__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__54416),null);
}
} else {
var id = cljs.core.first(s__54414__$2);
return cljs.core.cons(knoxx.backend.redis_client.srem(client,knoxx.backend.routes.contracts.contracts_index_key,id),knoxx$backend$routes$contracts$sync_contract_index_BANG__$_iter__54413(cljs.core.rest(s__54414__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(to_remove);
})());
return Promise.all(cljs.core.clj__GT_js(ops)).then((function (_){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] synced Redis index; add=",cljs.core.count(to_add),"remove=",cljs.core.count(to_remove)], 0));

return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"added","added",2057651688),to_add,new cljs.core.Keyword(null,"removed","removed",609626430),to_remove,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(ids)], null);
}));
}));
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] sync-contract-index! failed; startup continuing:",err.message], 0));

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),err.message], null);
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Redis not connected"], null));
}
});
knoxx.backend.routes.contracts.clear_contract_watch_timer_BANG_ = (function knoxx$backend$routes$contracts$clear_contract_watch_timer_BANG_(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.routes.contracts.contract_watch_timer_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var timer = temp__5825__auto__;
clearTimeout(timer);

return cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watch_timer_STAR_,null);
} else {
return null;
}
});
knoxx.backend.routes.contracts.stop_contract_watcher_BANG_ = (function knoxx$backend$routes$contracts$stop_contract_watcher_BANG_(){
knoxx.backend.routes.contracts.clear_contract_watch_timer_BANG_();

var seq__54445_54902 = cljs.core.seq(cljs.core.deref(knoxx.backend.routes.contracts.contract_watchers_STAR_));
var chunk__54446_54903 = null;
var count__54447_54904 = (0);
var i__54448_54905 = (0);
while(true){
if((i__54448_54905 < count__54447_54904)){
var watcher_54906 = chunk__54446_54903.cljs$core$IIndexed$_nth$arity$2(null,i__54448_54905);
if(cljs.core.truth_(watcher_54906)){
try{watcher_54906.close();
}catch (e54459){var __54907 = e54459;
}} else {
}


var G__54908 = seq__54445_54902;
var G__54909 = chunk__54446_54903;
var G__54910 = count__54447_54904;
var G__54911 = (i__54448_54905 + (1));
seq__54445_54902 = G__54908;
chunk__54446_54903 = G__54909;
count__54447_54904 = G__54910;
i__54448_54905 = G__54911;
continue;
} else {
var temp__5825__auto___54912 = cljs.core.seq(seq__54445_54902);
if(temp__5825__auto___54912){
var seq__54445_54913__$1 = temp__5825__auto___54912;
if(cljs.core.chunked_seq_QMARK_(seq__54445_54913__$1)){
var c__5673__auto___54914 = cljs.core.chunk_first(seq__54445_54913__$1);
var G__54915 = cljs.core.chunk_rest(seq__54445_54913__$1);
var G__54916 = c__5673__auto___54914;
var G__54917 = cljs.core.count(c__5673__auto___54914);
var G__54918 = (0);
seq__54445_54902 = G__54915;
chunk__54446_54903 = G__54916;
count__54447_54904 = G__54917;
i__54448_54905 = G__54918;
continue;
} else {
var watcher_54919 = cljs.core.first(seq__54445_54913__$1);
if(cljs.core.truth_(watcher_54919)){
try{watcher_54919.close();
}catch (e54465){var __54920 = e54465;
}} else {
}


var G__54921 = cljs.core.next(seq__54445_54913__$1);
var G__54922 = null;
var G__54923 = (0);
var G__54924 = (0);
seq__54445_54902 = G__54921;
chunk__54446_54903 = G__54922;
count__54447_54904 = G__54923;
i__54448_54905 = G__54924;
continue;
}
} else {
}
}
break;
}

cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watchers_STAR_,cljs.core.PersistentVector.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watch_running_QMARK__STAR_,false);

return null;
});
knoxx.backend.routes.contracts.watchable_contract_change_QMARK_ = (function knoxx$backend$routes$contracts$watchable_contract_change_QMARK_(filename){
return (((filename == null)) || (clojure.string.ends_with_QMARK_(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename))),".edn")));
});
knoxx.backend.routes.contracts.schedule_contract_refresh_BANG_ = (function knoxx$backend$routes$contracts$schedule_contract_refresh_BANG_(config,reason){
knoxx.backend.routes.contracts.clear_contract_watch_timer_BANG_();

return cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watch_timer_STAR_,setTimeout((function (){
cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watch_timer_STAR_,null);

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] watcher refresh triggered by",reason], 0));

return knoxx.backend.routes.contracts.sync_contract_index_BANG_(config).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] watcher sync failed:",err.message], 0));

return null;
})).then((function (_){
knoxx.backend.events.runtime.debounced_reload_BANG_();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] event-agent runtime reload scheduled after contract change"], 0));

return null;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] watcher reload failed:",err.message], 0));

return null;
}));
}),knoxx.backend.routes.contracts.contract_watch_debounce_ms));
});
knoxx.backend.routes.contracts.start_contract_watcher_BANG_ = (function knoxx$backend$routes$contracts$start_contract_watcher_BANG_(config){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.routes.contracts.contract_watch_running_QMARK__STAR_))){
return null;
} else {
var roots = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__54480_SHARP_){
return shadow.esm.esm_import$node_fs.existsSync(p1__54480_SHARP_);
}),(knoxx.backend.runtime.contract_loader.contract_root_paths.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.contract_root_paths.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.contract_root_paths.call(null,config)))));
var watch_root = (function (root){
try{return shadow.esm.esm_import$node_fs.watch(root,({"recursive": true}),(function (event_type,filename){
var filename_str = (function (){var G__54498 = filename;
if((G__54498 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54498));
}
})();
if(knoxx.backend.routes.contracts.watchable_contract_change_QMARK_(filename_str)){
return knoxx.backend.routes.contracts.schedule_contract_refresh_BANG_(config,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(root)+" :: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_type)+" :: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = filename_str;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "<unknown>";
}
})())));
} else {
return null;
}
}));
}catch (e54494){var err = e54494;
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] failed to watch",root,":",err.message], 0));

return null;
}});
var watchers = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(watch_root,roots)));
if(cljs.core.seq(watchers)){
cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watchers_STAR_,watchers);

cljs.core.reset_BANG_(knoxx.backend.routes.contracts.contract_watch_running_QMARK__STAR_,true);

return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] watching",cljs.core.count(watchers),"contract roots for live reload"], 0));
} else {
return null;
}
}
});
/**
 * List all contracts, optionally filtered by contract-class.
 * Public so tests can call it directly.
 */
knoxx.backend.routes.contracts.handle_list_contracts = (function knoxx$backend$routes$contracts$handle_list_contracts(do_json,config,contract_class){
return (knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.load_all_contracts_BANG_.call(null,config)).then((function (all){
var contracts = (function (){var G__54512 = all;
var G__54512__$1 = (cljs.core.truth_(contract_class)?cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__54503_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__54503_SHARP_),knoxx.backend.routes.contracts.normalize_contract_class(contract_class));
}),G__54512):G__54512);
var G__54512__$2 = cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(cljs.core.juxt.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),new cljs.core.Keyword(null,"id","id",-1388402092)),G__54512__$1)
;
return cljs.core.vec(G__54512__$2);

})();
var G__54519 = (200);
var G__54520 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"contracts","contracts",905357673),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__54508_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(p1__54508_SHARP_,new cljs.core.Keyword(null,"contract","contract",798152745),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458),new cljs.core.Keyword(null,"file-path","file-path",-2005501162),new cljs.core.Keyword(null,"ok?","ok?",447310304)], 0));
}),contracts)], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54519,G__54520) : do_json.call(null,G__54519,G__54520));
})).catch((function (err){
var G__54522 = (500);
var G__54523 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to list contracts: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54522,G__54523) : do_json.call(null,G__54522,G__54523));
}));
});
knoxx.backend.routes.contracts.handle_get_contract = (function knoxx$backend$routes$contracts$handle_get_contract(do_json,config,contract_class,contract_id){
return shadow.esm.esm_import$node_fs$promises.readFile((knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,contract_class,contract_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,contract_class,contract_id)),"utf8").then((function (edn_text){
var validation = knoxx.backend.routes.contracts.validate_contract_edn(contract_class,(function (){var or__5142__auto__ = edn_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var G__54537 = (200);
var G__54538 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),knoxx.backend.routes.contracts.normalize_contract_class(contract_class),new cljs.core.Keyword(null,"ednText","ednText",-1371174003),(function (){var or__5142__auto__ = edn_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"contract","contract",798152745),new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(validation),new cljs.core.Keyword(null,"validation","validation",-2141396518),cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(validation,new cljs.core.Keyword(null,"contract","contract",798152745))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54537,G__54538) : do_json.call(null,G__54537,G__54538));
})).catch((function (err){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ENOENT",err.code)){
var G__54542 = (404);
var G__54543 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Contract not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54542,G__54543) : do_json.call(null,G__54542,G__54543));
} else {
var G__54544 = (500);
var G__54545 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to read contract: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54544,G__54545) : do_json.call(null,G__54544,G__54545));
}
}));
});
knoxx.backend.routes.contracts.handle_save_contract = (function knoxx$backend$routes$contracts$handle_save_contract(do_json,config,contract_class,contract_id,edn_text){
var klass = knoxx.backend.routes.contracts.normalize_contract_class(contract_class);
var validation = knoxx.backend.routes.contracts.validate_contract_edn(klass,edn_text);
var validation_out = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(validation,new cljs.core.Keyword(null,"contract","contract",798152745));
var parsed = new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(validation);
var parsed_id = knoxx.backend.routes.contracts.parsed_record_id(klass,parsed);
var route_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id));
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(validation))){
var G__54553 = (400);
var G__54554 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"Contract EDN failed validation",new cljs.core.Keyword(null,"validation","validation",-2141396518),validation_out], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54553,G__54554) : do_json.call(null,G__54553,G__54554));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = parsed_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(route_id,parsed_id);
} else {
return and__5140__auto__;
}
})())){
var G__54559 = (400);
var G__54560 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"Refusing to save contract: record id does not match route contractId",new cljs.core.Keyword(null,"routeContractId","routeContractId",-2079828927),route_id,new cljs.core.Keyword(null,"ednContractId","ednContractId",1462680876),parsed_id,new cljs.core.Keyword(null,"validation","validation",-2141396518),validation_out], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54559,G__54560) : do_json.call(null,G__54559,G__54560));
} else {
var file_path = (knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,klass,route_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,klass,route_id));
return (knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.cljs$core$IFn$_invoke$arity$2(file_path,edn_text) : knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.call(null,file_path,edn_text)).then((function (_){
return knoxx.backend.routes.contracts.sync_contract_index_BANG_(config);
})).then((function (_){
var G__54564 = (200);
var G__54565 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),klass,new cljs.core.Keyword(null,"ednText","ednText",-1371174003),edn_text,new cljs.core.Keyword(null,"contract","contract",798152745),parsed,new cljs.core.Keyword(null,"validation","validation",-2141396518),validation_out], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54564,G__54565) : do_json.call(null,G__54564,G__54565));
})).catch((function (err){
var G__54567 = (500);
var G__54568 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to save contract: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54567,G__54568) : do_json.call(null,G__54567,G__54568));
}));

}
}
});
knoxx.backend.routes.contracts.handle_copy_contract = (function knoxx$backend$routes$contracts$handle_copy_contract(do_json,config,contract_class,source_id,new_id){
return shadow.esm.esm_import$node_fs$promises.readFile((knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,contract_class,source_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,contract_class,source_id)),"utf8").then((function (source_edn){
var text = (function (){var or__5142__auto__ = source_edn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var cloned = knoxx.backend.routes.contracts.update_id_in_edn_text(contract_class,text,new_id);
return knoxx.backend.routes.contracts.handle_save_contract(do_json,config,contract_class,new_id,cloned);
})).catch((function (err){
var G__54573 = (500);
var G__54574 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to copy contract: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54573,G__54574) : do_json.call(null,G__54573,G__54574));
}));
});
knoxx.backend.routes.contracts.handle_validate_contract = (function knoxx$backend$routes$contracts$handle_validate_contract(do_json,contract_class,edn_text){
var G__54602 = (200);
var G__54603 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.contracts.validate_contract_edn(contract_class,edn_text),new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),knoxx.backend.routes.contracts.normalize_contract_class(contract_class));
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54602,G__54603) : do_json.call(null,G__54602,G__54603));
});
knoxx.backend.routes.contracts.handle_agent_list_contracts = (function knoxx$backend$routes$contracts$handle_agent_list_contracts(do_text,config){
return (knoxx.backend.runtime.contract_loader.list_agent_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.list_agent_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.list_agent_contract_ids_BANG_.call(null,config)).then((function (ids){
var G__54611 = (200);
var G__54612 = cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([ids], 0));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54611,G__54612) : do_text.call(null,G__54611,G__54612));
})).catch((function (err){
var G__54616 = (500);
var G__54617 = (""+";; Failed to list contracts: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54616,G__54617) : do_text.call(null,G__54616,G__54617));
}));
});
knoxx.backend.routes.contracts.handle_agent_get_contract_edn = (function knoxx$backend$routes$contracts$handle_agent_get_contract_edn(do_text,config,contract_id){
return shadow.esm.esm_import$node_fs$promises.readFile((knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"agents",contract_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,"agents",contract_id)),"utf8").then((function (edn_text){
var G__54622 = (200);
var G__54623 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54622,G__54623) : do_text.call(null,G__54622,G__54623));
})).catch((function (err){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ENOENT",err.code)){
var G__54624 = (404);
var G__54625 = (""+";; Contract not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54624,G__54625) : do_text.call(null,G__54624,G__54625));
} else {
var G__54628 = (500);
var G__54629 = (""+";; Failed to read contract: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54628,G__54629) : do_text.call(null,G__54628,G__54629));
}
}));
});
knoxx.backend.routes.contracts.handle_agent_put_contract_edn = (function knoxx$backend$routes$contracts$handle_agent_put_contract_edn(do_text,config,contract_id,edn_text){
var validation = knoxx.backend.routes.contracts.validate_contract_edn("agents",edn_text);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(validation))){
var G__54633 = (422);
var G__54634 = cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(validation)], null)], 0));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54633,G__54634) : do_text.call(null,G__54633,G__54634));
} else {
var parsed = new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(validation);
var parsed_id = knoxx.backend.routes.contracts.parsed_record_id("agents",parsed);
var route_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id));
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(route_id,parsed_id)){
var G__54635 = (400);
var G__54636 = cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"contract_id_mismatch",new cljs.core.Keyword(null,"routeContractId","routeContractId",-2079828927),route_id,new cljs.core.Keyword(null,"ednContractId","ednContractId",1462680876),parsed_id], null)], 0));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54635,G__54636) : do_text.call(null,G__54635,G__54636));
} else {
return (function (){var G__54637 = (knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.runtime.contract_loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"agents",route_id) : knoxx.backend.runtime.contract_loader.contract_file_path.call(null,config,"agents",route_id));
var G__54638 = edn_text;
return (knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.cljs$core$IFn$_invoke$arity$2(G__54637,G__54638) : knoxx.backend.runtime.contract_loader.write_edn_file_BANG_.call(null,G__54637,G__54638));
})().then((function (_){
return knoxx.backend.routes.contracts.sync_contract_index_BANG_(config);
})).then((function (_){
var G__54639 = (200);
var G__54640 = cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword("contract","id","contract/id",-872298206),route_id,new cljs.core.Keyword(null,"contract","contract",798152745),parsed], null)], 0));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54639,G__54640) : do_text.call(null,G__54639,G__54640));
})).catch((function (err){
var G__54641 = (500);
var G__54642 = (""+";; Failed to save contract: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message));
return (do_text.cljs$core$IFn$_invoke$arity$2 ? do_text.cljs$core$IFn$_invoke$arity$2(G__54641,G__54642) : do_text.call(null,G__54641,G__54642));
}));
}
}
});
knoxx.backend.routes.contracts.handle_ui_actions = (function knoxx$backend$routes$contracts$handle_ui_actions(do_json,config,actor_id,surface){
var resolved = knoxx.backend.contracts.resolve.ui_actions_for_actor(config,actor_id,surface);
var G__54654 = (200);
var G__54655 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(resolved),new cljs.core.Keyword(null,"surface","surface",699915646),new cljs.core.Keyword(null,"surface","surface",699915646).cljs$core$IFn$_invoke$arity$1(resolved),new cljs.core.Keyword(null,"default_agent_id","default_agent_id",-1139401460),new cljs.core.Keyword(null,"default-agent-id","default-agent-id",-2135472358).cljs$core$IFn$_invoke$arity$1(resolved),new cljs.core.Keyword(null,"actions","actions",-812656882),new cljs.core.Keyword(null,"actions","actions",-812656882).cljs$core$IFn$_invoke$arity$1(resolved)], null);
return (do_json.cljs$core$IFn$_invoke$arity$2 ? do_json.cljs$core$IFn$_invoke$arity$2(G__54654,G__54655) : do_json.call(null,G__54654,G__54655));
});
knoxx.backend.routes.contracts.register_agent_contract_routes_BANG_ = (function knoxx$backend$routes$contracts$register_agent_contract_routes_BANG_(app,runtime,config,helpers){
var do_route = new cljs.core.Keyword(null,"route!","route!",-1286958144).cljs$core$IFn$_invoke$arity$1(helpers);
var do_json = new cljs.core.Keyword(null,"json-response!","json-response!",103570476).cljs$core$IFn$_invoke$arity$1(helpers);
var do_err = new cljs.core.Keyword(null,"error-response!","error-response!",-856339341).cljs$core$IFn$_invoke$arity$1(helpers);
var do_ctx = new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046).cljs$core$IFn$_invoke$arity$1(helpers);
var do_perm = new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163).cljs$core$IFn$_invoke$arity$1(helpers);
var do_text = (function (reply,status,text){
return reply.end(reply.status(status),text,({"Content-Type": "text/plain; charset=utf-8"}));
});
var G__54657_54961 = app;
var G__54658_54962 = "GET";
var G__54659_54963 = "/api/contracts/ui-actions";
var G__54660_54964 = (function (request,reply){
var G__54661 = runtime;
var G__54662 = request;
var G__54663 = reply;
var G__54664 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

var actor_id = (function (){var or__5142__auto__ = (request["query"]["actor"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["actor_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (request["query"]["actorId"]);
}
}
})();
var surface = (function (){var or__5142__auto__ = (request["query"]["surface"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["surface_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (request["query"]["surfaceId"]);
}
}
})();
return knoxx.backend.routes.contracts.handle_ui_actions(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),config,actor_id,surface);
}catch (e54665){var err = e54665;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54661,G__54662,G__54663,G__54664) : do_ctx.call(null,G__54661,G__54662,G__54663,G__54664));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54657_54961,G__54658_54962,G__54659_54963,G__54660_54964) : do_route.call(null,G__54657_54961,G__54658_54962,G__54659_54963,G__54660_54964));

var G__54672_54971 = app;
var G__54673_54972 = "GET";
var G__54674_54973 = "/api/agent/contracts";
var G__54675_54974 = (function (request,reply){
var G__54676 = runtime;
var G__54677 = request;
var G__54678 = reply;
var G__54679 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

return knoxx.backend.routes.contracts.handle_agent_list_contracts(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_text,reply),config);
}catch (e54680){var err = e54680;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54676,G__54677,G__54678,G__54679) : do_ctx.call(null,G__54676,G__54677,G__54678,G__54679));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54672_54971,G__54673_54972,G__54674_54973,G__54675_54974) : do_route.call(null,G__54672_54971,G__54673_54972,G__54674_54973,G__54675_54974));

var G__54682_54975 = app;
var G__54683_54976 = "GET";
var G__54684_54977 = "/api/agent/contracts/:contractId";
var G__54685_54978 = (function (request,reply){
var G__54686 = runtime;
var G__54687 = request;
var G__54688 = reply;
var G__54689 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

var contract_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["contractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(contract_id)){
return do_text(reply,(400),";; contractId is required");
} else {
var safe = knoxx.backend.routes.contracts.safe_contract_id(contract_id);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe))){
return do_text(reply,(400),(""+";; Invalid contractId: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe))));
} else {
return knoxx.backend.routes.contracts.handle_agent_get_contract_edn(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_text,reply),config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe));
}
}
}catch (e54690){var err = e54690;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54686,G__54687,G__54688,G__54689) : do_ctx.call(null,G__54686,G__54687,G__54688,G__54689));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54682_54975,G__54683_54976,G__54684_54977,G__54685_54978) : do_route.call(null,G__54682_54975,G__54683_54976,G__54684_54977,G__54685_54978));

var G__54695 = app;
var G__54696 = "PUT";
var G__54697 = "/api/agent/contracts/:contractId";
var G__54698 = (function (request,reply){
var G__54702 = runtime;
var G__54703 = request;
var G__54704 = reply;
var G__54705 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

var contract_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["contractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var edn_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(contract_id)){
return do_text(reply,(400),";; contractId is required");
} else {
var safe = knoxx.backend.routes.contracts.safe_contract_id(contract_id);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe))){
return do_text(reply,(400),(""+";; Invalid contractId: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe))));
} else {
return knoxx.backend.routes.contracts.handle_agent_put_contract_edn(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_text,reply),config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe),edn_text);
}
}
}catch (e54706){var err = e54706;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54702,G__54703,G__54704,G__54705) : do_ctx.call(null,G__54702,G__54703,G__54704,G__54705));
});
return (do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54695,G__54696,G__54697,G__54698) : do_route.call(null,G__54695,G__54696,G__54697,G__54698));
});
knoxx.backend.routes.contracts.register_admin_contract_routes_BANG_ = (function knoxx$backend$routes$contracts$register_admin_contract_routes_BANG_(app,runtime,config,helpers){
var do_route = new cljs.core.Keyword(null,"route!","route!",-1286958144).cljs$core$IFn$_invoke$arity$1(helpers);
var do_json = new cljs.core.Keyword(null,"json-response!","json-response!",103570476).cljs$core$IFn$_invoke$arity$1(helpers);
var do_err = new cljs.core.Keyword(null,"error-response!","error-response!",-856339341).cljs$core$IFn$_invoke$arity$1(helpers);
var do_ctx = new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046).cljs$core$IFn$_invoke$arity$1(helpers);
var do_perm = new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163).cljs$core$IFn$_invoke$arity$1(helpers);
var G__54731_54989 = app;
var G__54732_54990 = "GET";
var G__54733_54991 = "/api/admin/contracts";
var G__54734_54992 = (function (request,reply){
var G__54735 = runtime;
var G__54736 = request;
var G__54737 = reply;
var G__54738 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

var kind = (function (){var or__5142__auto__ = (request["query"]["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (request["query"]["class"]);
}
})();
var safe_kind = (cljs.core.truth_(kind)?knoxx.backend.routes.contracts.safe_contract_class(kind):new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"class","class",-2030961996),null], null));
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_kind))){
var G__54745 = reply;
var G__54746 = (400);
var G__54747 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contract class",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_kind)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54745,G__54746,G__54747) : do_json.call(null,G__54745,G__54746,G__54747));
} else {
return knoxx.backend.routes.contracts.handle_list_contracts(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),config,new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(safe_kind));
}
}catch (e54741){var err = e54741;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54735,G__54736,G__54737,G__54738) : do_ctx.call(null,G__54735,G__54736,G__54737,G__54738));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54731_54989,G__54732_54990,G__54733_54991,G__54734_54992) : do_route.call(null,G__54731_54989,G__54732_54990,G__54733_54991,G__54734_54992));

var G__54748_54997 = app;
var G__54749_54998 = "GET";
var G__54750_54999 = "/api/admin/contracts/:contractId";
var G__54751_55000 = (function (request,reply){
var G__54752 = runtime;
var G__54753 = request;
var G__54754 = reply;
var G__54755 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : do_perm.call(null,ctx,"agent.chat.use"));
} else {
}

var contract_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["contractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var kind = (function (){var or__5142__auto__ = (request["query"]["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["class"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "agents";
}
}
})();
if(clojure.string.blank_QMARK_(contract_id)){
var G__54757 = reply;
var G__54758 = (400);
var G__54759 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"contractId is required"], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54757,G__54758,G__54759) : do_json.call(null,G__54757,G__54758,G__54759));
} else {
var safe = knoxx.backend.routes.contracts.safe_contract_id(contract_id);
var safe_kind = knoxx.backend.routes.contracts.safe_contract_class(kind);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_kind))){
var G__54761 = reply;
var G__54762 = (400);
var G__54763 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contract class",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_kind)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54761,G__54762,G__54763) : do_json.call(null,G__54761,G__54762,G__54763));
} else {
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe))){
var G__54764 = reply;
var G__54765 = (400);
var G__54766 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contractId",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54764,G__54765,G__54766) : do_json.call(null,G__54764,G__54765,G__54766));
} else {
return knoxx.backend.routes.contracts.handle_get_contract(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),config,new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(safe_kind),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe));

}
}
}
}catch (e54756){var err = e54756;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54752,G__54753,G__54754,G__54755) : do_ctx.call(null,G__54752,G__54753,G__54754,G__54755));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54748_54997,G__54749_54998,G__54750_54999,G__54751_55000) : do_route.call(null,G__54748_54997,G__54749_54998,G__54750_54999,G__54751_55000));

var G__54767_55001 = app;
var G__54768_55002 = "PUT";
var G__54769_55003 = "/api/admin/contracts/:contractId";
var G__54770_55004 = (function (request,reply){
var G__54777 = runtime;
var G__54778 = request;
var G__54779 = reply;
var G__54780 = (function (ctx){
try{(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.create") : do_perm.call(null,ctx,"platform.org.create"));

var contract_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["contractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var kind = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (request["query"]["kind"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "agents";
}
}
}
})();
var edn_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ednText","ednText",-1371174003).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(contract_id)){
var G__54782 = reply;
var G__54783 = (400);
var G__54784 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"contractId is required"], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54782,G__54783,G__54784) : do_json.call(null,G__54782,G__54783,G__54784));
} else {
var safe = knoxx.backend.routes.contracts.safe_contract_id(contract_id);
var safe_kind = knoxx.backend.routes.contracts.safe_contract_class(kind);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_kind))){
var G__54786 = reply;
var G__54787 = (400);
var G__54788 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contract class",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_kind)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54786,G__54787,G__54788) : do_json.call(null,G__54786,G__54787,G__54788));
} else {
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe))){
var G__54789 = reply;
var G__54790 = (400);
var G__54791 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contractId",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54789,G__54790,G__54791) : do_json.call(null,G__54789,G__54790,G__54791));
} else {
return knoxx.backend.routes.contracts.handle_save_contract(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),config,new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(safe_kind),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe),edn_text);

}
}
}
}catch (e54781){var err = e54781;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54777,G__54778,G__54779,G__54780) : do_ctx.call(null,G__54777,G__54778,G__54779,G__54780));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54767_55001,G__54768_55002,G__54769_55003,G__54770_55004) : do_route.call(null,G__54767_55001,G__54768_55002,G__54769_55003,G__54770_55004));

var G__54796_55012 = app;
var G__54797_55013 = "POST";
var G__54798_55014 = "/api/admin/contracts/validate";
var G__54799_55015 = (function (request,reply){
var G__54800 = runtime;
var G__54801 = request;
var G__54802 = reply;
var G__54803 = (function (ctx){
try{(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.create") : do_perm.call(null,ctx,"platform.org.create"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var kind = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "agents";
}
}
})();
var safe_kind = knoxx.backend.routes.contracts.safe_contract_class(kind);
var edn_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ednText","ednText",-1371174003).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_kind))){
var G__54808 = reply;
var G__54809 = (400);
var G__54810 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contract class",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_kind)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54808,G__54809,G__54810) : do_json.call(null,G__54808,G__54809,G__54810));
} else {
return knoxx.backend.routes.contracts.handle_validate_contract(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(safe_kind),edn_text);
}
}catch (e54807){var err = e54807;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54800,G__54801,G__54802,G__54803) : do_ctx.call(null,G__54800,G__54801,G__54802,G__54803));
});
(do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54796_55012,G__54797_55013,G__54798_55014,G__54799_55015) : do_route.call(null,G__54796_55012,G__54797_55013,G__54798_55014,G__54799_55015));

var G__54811 = app;
var G__54812 = "POST";
var G__54813 = "/api/admin/contracts/:contractId/copy";
var G__54814 = (function (request,reply){
var G__54816 = runtime;
var G__54817 = request;
var G__54818 = reply;
var G__54819 = (function (ctx){
try{(do_perm.cljs$core$IFn$_invoke$arity$2 ? do_perm.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.create") : do_perm.call(null,ctx,"platform.org.create"));

var source_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["contractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var new_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"newId","newId",1699050104).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var kind = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "agents";
}
}
})();
var safe_kind = knoxx.backend.routes.contracts.safe_contract_class(kind);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_kind))){
var G__54821 = reply;
var G__54822 = (400);
var G__54823 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid contract class",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_kind)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54821,G__54822,G__54823) : do_json.call(null,G__54821,G__54822,G__54823));
} else {
if(((clojure.string.blank_QMARK_(source_id)) || (clojure.string.blank_QMARK_(new_id)))){
var G__54824 = reply;
var G__54825 = (400);
var G__54826 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"source contractId and newId are required"], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54824,G__54825,G__54826) : do_json.call(null,G__54824,G__54825,G__54826));
} else {
var safe_source = knoxx.backend.routes.contracts.safe_contract_id(source_id);
var safe_new = knoxx.backend.routes.contracts.safe_contract_id(new_id);
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_source))){
var G__54831 = reply;
var G__54833 = (400);
var G__54834 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid source contractId",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_source)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54831,G__54833,G__54834) : do_json.call(null,G__54831,G__54833,G__54834));
} else {
if(cljs.core.not(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(safe_new))){
var G__54836 = reply;
var G__54837 = (400);
var G__54838 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Invalid newId",new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(safe_new)], null);
return (do_json.cljs$core$IFn$_invoke$arity$3 ? do_json.cljs$core$IFn$_invoke$arity$3(G__54836,G__54837,G__54838) : do_json.call(null,G__54836,G__54837,G__54838));
} else {
return knoxx.backend.routes.contracts.handle_copy_contract(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(do_json,reply),config,new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(safe_kind),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe_source),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(safe_new));

}
}

}
}
}catch (e54820){var err = e54820;
return (do_err.cljs$core$IFn$_invoke$arity$2 ? do_err.cljs$core$IFn$_invoke$arity$2(reply,err) : do_err.call(null,reply,err));
}});
return (do_ctx.cljs$core$IFn$_invoke$arity$4 ? do_ctx.cljs$core$IFn$_invoke$arity$4(G__54816,G__54817,G__54818,G__54819) : do_ctx.call(null,G__54816,G__54817,G__54818,G__54819));
});
return (do_route.cljs$core$IFn$_invoke$arity$4 ? do_route.cljs$core$IFn$_invoke$arity$4(G__54811,G__54812,G__54813,G__54814) : do_route.call(null,G__54811,G__54812,G__54813,G__54814));
});
knoxx.backend.routes.contracts.register_contracts_routes_BANG_ = (function knoxx$backend$routes$contracts$register_contracts_routes_BANG_(app,runtime,config,helpers){
knoxx.backend.routes.contracts.register_agent_contract_routes_BANG_(app,runtime,config,helpers);

knoxx.backend.routes.contracts.register_admin_contract_routes_BANG_(app,runtime,config,helpers);

return null;
});

//# sourceMappingURL=knoxx.backend.routes.contracts.js.map
