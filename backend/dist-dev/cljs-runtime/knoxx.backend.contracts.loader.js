import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./cljs.reader.js";
import "./knoxx.backend.contracts.actor_scope.js";
import "./knoxx.backend.contracts.validator.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.contracts.loader');
knoxx.backend.contracts.loader.contract_class_order = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, ["agents","actors","roles","capabilities","policies","model_families","models","ingest_sources","actions","pipelines","triggers","sub_agents","cms"], null);
knoxx.backend.contracts.loader.contract_edn_filename_QMARK_ = (function knoxx$backend$contracts$loader$contract_edn_filename_QMARK_(filename){
return ((typeof filename === 'string') && (((clojure.string.ends_with_QMARK_(filename,".edn")) && ((!(clojure.string.starts_with_QMARK_(filename,".")))))));
});
knoxx.backend.contracts.loader.configured_contracts_dir = (function knoxx$backend$contracts$loader$configured_contracts_dir(config){
var G__50689 = new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735).cljs$core$IFn$_invoke$arity$1(config);
var G__50689__$1 = (((G__50689 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50689)));
var G__50689__$2 = (((G__50689__$1 == null))?null:clojure.string.trim(G__50689__$1));
if((G__50689__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50689__$2);
}
});
knoxx.backend.contracts.loader.default_configured_contracts_dir_QMARK_ = (function knoxx$backend$contracts$loader$default_configured_contracts_dir_QMARK_(value){
return (((value == null)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(value,"contracts")));
});
knoxx.backend.contracts.loader.contract_root_candidates = (function knoxx$backend$contracts$loader$contract_root_candidates(config){
var configured = knoxx.backend.contracts.loader.configured_contracts_dir(config);
if(knoxx.backend.contracts.loader.default_configured_contracts_dir_QMARK_(configured)){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["../contracts","contracts","packages/agents/knoxx/contracts","orgs/open-hax/openplanner/packages/agents/knoxx/contracts"], null);
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [configured], null);
}
});
knoxx.backend.contracts.loader.contract_root_paths = (function knoxx$backend$contracts$loader$contract_root_paths(config){
var cwd = process.cwd();
var resolved = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__50694_SHARP_){
return shadow.esm.esm_import$node_path.resolve(cwd,p1__50694_SHARP_);
}),knoxx.backend.contracts.loader.contract_root_candidates(config))));
var existing = cljs.core.filterv((function (p1__50695_SHARP_){
return shadow.esm.esm_import$node_fs.existsSync(p1__50695_SHARP_);
}),resolved);
if(cljs.core.seq(existing)){
return existing;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [shadow.esm.esm_import$node_path.resolve(cwd,(function (){var or__5142__auto__ = knoxx.backend.contracts.loader.configured_contracts_dir(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "../contracts";
}
})())], null);
}
});
/**
 * First existing contract root (legacy single-root compat).
 */
knoxx.backend.contracts.loader.contracts_dir_path = (function knoxx$backend$contracts$loader$contracts_dir_path(config){
return cljs.core.first(knoxx.backend.contracts.loader.contract_root_paths(config));
});
knoxx.backend.contracts.loader.safe_path_segment_BANG_ = (function knoxx$backend$contracts$loader$safe_path_segment_BANG_(segment,kind){
var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment));
if(((clojure.string.blank_QMARK_(s)) || (cljs.core.not(cljs.core.re_matches(/[A-Za-z0-9._-]+/,s))))){
throw (new Error((""+"Invalid "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kind)+" segment: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment))));
} else {
}

return s;
});
knoxx.backend.contracts.loader.normalize_contract_class = (function knoxx$backend$contracts$loader$normalize_contract_class(value){
var raw = (function (){var G__50704 = value;
var G__50704__$1 = (((G__50704 == null))?null:(function (){var G__50705 = G__50704;
var G__50705__$1 = (((value instanceof cljs.core.Keyword))?cljs.core.name(G__50705):G__50705);
if((!((value instanceof cljs.core.Keyword)))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50705__$1));
} else {
return G__50705__$1;
}
})());
var G__50704__$2 = (((G__50704__$1 == null))?null:clojure.string.trim(G__50704__$1));
if((G__50704__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__50704__$2);
}
})();
var G__50706 = raw;
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(null,G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("",G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("pipelines",G__50706)){
return "pipelines";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("capabilities",G__50706)){
return "capabilities";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("role",G__50706)){
return "roles";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("sub_agents",G__50706)){
return "sub_agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model-families",G__50706)){
return "model_families";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("agent",G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model",G__50706)){
return "models";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("sub-agent",G__50706)){
return "sub_agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ingest-source",G__50706)){
return "ingest_sources";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ingest_source",G__50706)){
return "ingest_sources";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-configs",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("caps",G__50706)){
return "capabilities";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ingest-sources",G__50706)){
return "ingest_sources";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms_config",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("user",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("capability",G__50706)){
return "capabilities";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-block-registry",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-block-registries",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("agents",G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("sub_agent",G__50706)){
return "sub_agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-template",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-templates-registry",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("users",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms_configs",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model-family",G__50706)){
return "model_families";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-template-registries",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("action",G__50706)){
return "actions";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("sub-agents",G__50706)){
return "sub_agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("policies",G__50706)){
return "policies";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("policy",G__50706)){
return "policies";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("actors",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("pipeline",G__50706)){
return "pipelines";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model_family",G__50706)){
return "model_families";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("triggers",G__50706)){
return "triggers";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("contracts",G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("contract",G__50706)){
return "agents";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-templates",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model_families",G__50706)){
return "model_families";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("ingest_sources",G__50706)){
return "ingest_sources";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cap",G__50706)){
return "capabilities";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("models",G__50706)){
return "models";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("human",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("trigger",G__50706)){
return "triggers";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("humans",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-config",G__50706)){
return "cms";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("roles",G__50706)){
return "roles";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("actor",G__50706)){
return "actors";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("actions",G__50706)){
return "actions";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cms-template-registry",G__50706)){
return "cms";
} else {
throw (new Error((""+"Unknown contract class: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value))));

}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
knoxx.backend.contracts.loader.stderr_BANG_ = (function knoxx$backend$contracts$loader$stderr_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___50987 = arguments.length;
var i__5877__auto___50988 = (0);
while(true){
if((i__5877__auto___50988 < len__5876__auto___50987)){
args__5882__auto__.push((arguments[i__5877__auto___50988]));

var G__50989 = (i__5877__auto___50988 + (1));
i__5877__auto___50988 = G__50989;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (parts){
return process.stderr.write((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("",parts))+"\n"));
}));

(knoxx.backend.contracts.loader.stderr_BANG_.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(knoxx.backend.contracts.loader.stderr_BANG_.cljs$lang$applyTo = (function (seq50726){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq50726));
}));

if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.contracts !== 'undefined') && (typeof knoxx.backend.contracts.loader !== 'undefined') && (typeof knoxx.backend.contracts.loader.sync_contract_record_cache_STAR_ !== 'undefined')){
} else {
knoxx.backend.contracts.loader.sync_contract_record_cache_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.contracts.loader.sync_contract_record_cache_ttl_ms = (2000);
knoxx.backend.contracts.loader.now_ms = (function knoxx$backend$contracts$loader$now_ms(){
return Date.now();
});
knoxx.backend.contracts.loader.invalidate_sync_contract_cache_BANG_ = (function knoxx$backend$contracts$loader$invalidate_sync_contract_cache_BANG_(){
return cljs.core.reset_BANG_(knoxx.backend.contracts.loader.sync_contract_record_cache_STAR_,null);
});
/**
 * Returns absolute path if entry is a non-hidden .edn file, else nil.
 */
knoxx.backend.contracts.loader.entry__GT_file_path = (function knoxx$backend$contracts$loader$entry__GT_file_path(ent){
if(cljs.core.truth_((function (){var and__5140__auto__ = ent.isFile();
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.contracts.loader.contract_edn_filename_QMARK_(ent.name);
} else {
return and__5140__auto__;
}
})())){
return shadow.esm.esm_import$node_path.join(ent.parentPath,ent.name);
} else {
return null;
}
});
/**
 * Find all .edn files under root via recursive readdir. Returns Promise<vector<string>>.
 */
knoxx.backend.contracts.loader.discover_contract_files_BANG_ = (function knoxx$backend$contracts$loader$discover_contract_files_BANG_(root){
return shadow.esm.esm_import$node_fs$promises.readdir(root,({"withFileTypes": true, "recursive": true})).then((function (entries){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.loader.entry__GT_file_path,Array.from(entries)));
})).catch((function (err){
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] readdir failed: ",root," \u2014 ",err.message], 0));

return cljs.core.PersistentVector.EMPTY;
}));
});
knoxx.backend.contracts.loader.keyword__GT_str = (function knoxx$backend$contracts$loader$keyword__GT_str(v){
if((v instanceof cljs.core.Keyword)){
return cljs.core.name(v);
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v));
}
});
knoxx.backend.contracts.loader.extract_contract_identity = (function knoxx$backend$contracts$loader$extract_contract_identity(raw){
var kind = (function (){var G__50744 = (function (){var or__5142__auto__ = new cljs.core.Keyword("contract","kind","contract/kind",1929672067).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (cljs.core.truth_(new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(raw))?"actors":null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (cljs.core.truth_(new cljs.core.Keyword("role","id","role/id",-1375589954).cljs$core$IFn$_invoke$arity$1(raw))?"roles":null);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = (cljs.core.truth_(new cljs.core.Keyword("cap","id","cap/id",-1388434846).cljs$core$IFn$_invoke$arity$1(raw))?"capabilities":null);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = (cljs.core.truth_(new cljs.core.Keyword("model","id","model/id",-1274892501).cljs$core$IFn$_invoke$arity$1(raw))?"models":null);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
if(cljs.core.truth_(new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(raw))){
return "model_families";
} else {
return null;
}
}
}
}
}
}
}
})();
var G__50744__$1 = (((G__50744 == null))?null:knoxx.backend.contracts.loader.keyword__GT_str(G__50744));
var G__50744__$2 = (((G__50744__$1 == null))?null:clojure.string.trim(G__50744__$1));
if((G__50744__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50744__$2);
}
})();
var id = (function (){var G__50753 = (function (){var or__5142__auto__ = new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword("role","id","role/id",-1375589954).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword("cap","id","cap/id",-1388434846).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = new cljs.core.Keyword("model","id","model/id",-1274892501).cljs$core$IFn$_invoke$arity$1(raw);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
return new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(raw);
}
}
}
}
}
}
})();
var G__50753__$1 = (((G__50753 == null))?null:knoxx.backend.contracts.loader.keyword__GT_str(G__50753));
var G__50753__$2 = (((G__50753__$1 == null))?null:clojure.string.trim(G__50753__$1));
if((G__50753__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50753__$2);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = kind;
if(cljs.core.truth_(and__5140__auto__)){
return id;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [kind,id], null);
} else {
return null;
}
});
knoxx.backend.contracts.loader.validate_and_build = (function knoxx$backend$contracts$loader$validate_and_build(file_path,edn_text,raw){
var vec__50759 = knoxx.backend.contracts.loader.extract_contract_identity(raw);
var raw_kind = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50759,(0),null);
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50759,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = raw_kind;
if(cljs.core.truth_(and__5140__auto__)){
return id;
} else {
return and__5140__auto__;
}
})())){
} else {
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] missing :contract/id or :contract/kind: ",file_path], 0));

throw (new Error("missing identity"));
}

var kind = knoxx.backend.contracts.loader.normalize_contract_class(raw_kind);
var valid = knoxx.backend.contracts.validator.validate.cljs$core$IFn$_invoke$arity$2(kind,raw);
if(cljs.core.truth_(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(valid))){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok?","ok?",447310304),true,new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694),kind,new cljs.core.Keyword(null,"contract","contract",798152745),raw,new cljs.core.Keyword(null,"file-path","file-path",-2005501162),file_path,new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text))], null);
} else {
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] validation failed: ",file_path," \u2014 ",cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(valid)], 0))], 0));

return null;
}
});
/**
 * Parse + validate a single .edn file. Returns contract record or nil.
 */
knoxx.backend.contracts.loader.parse_contract_file_BANG_ = (function knoxx$backend$contracts$loader$parse_contract_file_BANG_(file_path,edn_text){
try{var raw = cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edn_text)));
return knoxx.backend.contracts.loader.validate_and_build(file_path,edn_text,raw);
}catch (e50768){var err = e50768;
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] parse error: ",file_path," \u2014 ",err.message], 0));

return null;
}});
knoxx.backend.contracts.loader.read_contract_file_BANG_ = (function knoxx$backend$contracts$loader$read_contract_file_BANG_(file_path){
return shadow.esm.esm_import$node_fs$promises.readFile(file_path,"utf8").then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.loader.parse_contract_file_BANG_,file_path)).catch((function (err){
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] read error: ",file_path," \u2014 ",err.message], 0));

return null;
}));
});
/**
 * First-wins dedup on [contractClass id]. Logs collisions to stderr.
 */
knoxx.backend.contracts.loader.dedup_contracts = (function knoxx$backend$contracts$loader$dedup_contracts(records){
var seen = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentHashSet.EMPTY);
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,r){
var k = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(r),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(r)], null);
if(cljs.core.truth_((function (){var fexpr__50773 = cljs.core.deref(seen);
return (fexpr__50773.cljs$core$IFn$_invoke$arity$1 ? fexpr__50773.cljs$core$IFn$_invoke$arity$1(k) : fexpr__50773.call(null,k));
})())){
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] collision on ",cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([k], 0))," \u2014 keeping first, ignoring ",new cljs.core.Keyword(null,"file-path","file-path",-2005501162).cljs$core$IFn$_invoke$arity$1(r)], 0));

return acc;
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(seen,cljs.core.conj,k);

return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,r);
}
}),cljs.core.PersistentVector.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,records));
});
/**
 * Synchronously find all .edn files under root. Runtime sync consumers must
 * still parse the files through parse-contract-file! so identity comes from the
 * contract body, not from the directory or filename.
 */
knoxx.backend.contracts.loader.discover_contract_files_sync = (function knoxx$backend$contracts$loader$discover_contract_files_sync(root){
try{return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.loader.entry__GT_file_path,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(shadow.esm.esm_import$node_fs.readdirSync(root,({"withFileTypes": true, "recursive": true})))));
}catch (e50778){var err = e50778;
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] sync readdir failed: ",root," \u2014 ",err.message], 0));

return cljs.core.PersistentVector.EMPTY;
}});
knoxx.backend.contracts.loader.load_all_contracts_sync_uncached = (function knoxx$backend$contracts$loader$load_all_contracts_sync_uncached(config){
return knoxx.backend.contracts.loader.dedup_contracts(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (file_path){
try{return knoxx.backend.contracts.loader.parse_contract_file_BANG_(file_path,shadow.esm.esm_import$node_fs.readFileSync(file_path,"utf8"));
}catch (e50781){var err = e50781;
knoxx.backend.contracts.loader.stderr_BANG_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[contracts] sync read error: ",file_path," \u2014 ",err.message], 0));

return null;
}}),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.contracts.loader.discover_contract_files_sync,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.contracts.loader.contract_root_paths(config)], 0)))));
});
/**
 * Synchronously load all contract records through the same parser/validator and
 * [contractClass id] dedup path as load-all-contracts!. This is the escape hatch
 * for startup/runtime code that cannot await but still must not use filepath or
 * folder placement as contract truth.
 * 
 * A short process-local cache prevents startup/model resolution from reparsing
 * the entire contract tree dozens of times in the same tick. Invalid contracts
 * are still omitted; they must not pin the event loop or block HTTP startup.
 */
knoxx.backend.contracts.loader.load_all_contracts_sync = (function knoxx$backend$contracts$loader$load_all_contracts_sync(config){
var now = knoxx.backend.contracts.loader.now_ms();
var cached = cljs.core.deref(knoxx.backend.contracts.loader.sync_contract_record_cache_STAR_);
if(cljs.core.truth_((function (){var and__5140__auto__ = cached;
if(cljs.core.truth_(and__5140__auto__)){
return ((now - new cljs.core.Keyword(null,"ts","ts",1617209904).cljs$core$IFn$_invoke$arity$1(cached)) < knoxx.backend.contracts.loader.sync_contract_record_cache_ttl_ms);
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"records","records",1326822832).cljs$core$IFn$_invoke$arity$1(cached);
} else {
var records = knoxx.backend.contracts.loader.load_all_contracts_sync_uncached(config);
cljs.core.reset_BANG_(knoxx.backend.contracts.loader.sync_contract_record_cache_STAR_,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ts","ts",1617209904),now,new cljs.core.Keyword(null,"records","records",1326822832),records], null));

return records;
}
});
knoxx.backend.contracts.loader.find_contract_record_sync = (function knoxx$backend$contracts$loader$find_contract_record_sync(config,contract_class,contract_id){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
var wanted_id = (function (){var G__50788 = contract_id;
var G__50788__$1 = (((G__50788 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50788)));
var G__50788__$2 = (((G__50788__$1 == null))?null:clojure.string.trim(G__50788__$1));
if((G__50788__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50788__$2);
}
})();
return cljs.core.some((function (record){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(record))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wanted_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record))))){
return record;
} else {
return null;
}
}),knoxx.backend.contracts.loader.load_all_contracts_sync(config));
});
knoxx.backend.contracts.loader.contract_sync = (function knoxx$backend$contracts$loader$contract_sync(config,contract_class,contract_id){
var G__50792 = knoxx.backend.contracts.loader.find_contract_record_sync(config,contract_class,contract_id);
if((G__50792 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(G__50792);
}
});
/**
 * Discover all .edn files under all contract roots, parse+validate each,
 * deduplicate on [kind id]. Returns Promise<vector<contract-record>>.
 */
knoxx.backend.contracts.loader.load_all_contracts_BANG_ = (function knoxx$backend$contracts$loader$load_all_contracts_BANG_(config){
var roots = knoxx.backend.contracts.loader.contract_root_paths(config);
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.loader.discover_contract_files_BANG_,roots))).then((function (file_lists){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__50794_SHARP_){
return Array.from(p1__50794_SHARP_);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([Array.from(file_lists)], 0))));
})).then((function (files){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.loader.read_contract_file_BANG_,files)));
})).then((function (results){
return knoxx.backend.contracts.loader.dedup_contracts(Array.from(results));
}));
});
knoxx.backend.contracts.loader.list_contract_ids_BANG_ = (function knoxx$backend$contracts$loader$list_contract_ids_BANG_(var_args){
var G__50800 = arguments.length;
switch (G__50800) {
case 1:
return knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$2(config,"agents");
}));

(knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (config,contract_class){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
return knoxx.backend.contracts.loader.load_all_contracts_BANG_(config).then((function (all){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__50798_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__50798_SHARP_),klass);
}),all))));
}));
}));

(knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$lang$maxFixedArity = 2);

knoxx.backend.contracts.loader.list_agent_contract_ids_BANG_ = (function knoxx$backend$contracts$loader$list_agent_contract_ids_BANG_(config){
return knoxx.backend.contracts.loader.list_contract_ids_BANG_.cljs$core$IFn$_invoke$arity$2(config,"agents");
});
knoxx.backend.contracts.loader.resolve_contracts_dir = (function knoxx$backend$contracts$loader$resolve_contracts_dir(config){
var or__5142__auto__ = cljs.core.first(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__50807_SHARP_){
return shadow.esm.esm_import$node_fs.existsSync(p1__50807_SHARP_);
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__50808_SHARP_){
return shadow.esm.esm_import$node_path.resolve(process.cwd(),p1__50808_SHARP_);
}),knoxx.backend.contracts.loader.contract_root_candidates(config))));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_path.resolve(process.cwd(),(function (){var or__5142__auto____$1 = knoxx.backend.contracts.loader.configured_contracts_dir(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "../contracts";
}
})());
}
});
knoxx.backend.contracts.loader.contract_class_dir_paths = (function knoxx$backend$contracts$loader$contract_class_dir_paths(config,contract_class){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__50836_SHARP_){
return shadow.esm.esm_import$node_path.join(p1__50836_SHARP_,klass);
}),knoxx.backend.contracts.loader.contract_root_paths(config));
});
/**
 * Search for {id}.edn under {root}/{class} recursively.
 */
knoxx.backend.contracts.loader.find_contract_file_recursive = (function knoxx$backend$contracts$loader$find_contract_file_recursive(root,klass,filename){
try{var entries = shadow.esm.esm_import$node_fs.readdirSync(shadow.esm.esm_import$node_path.join(root,klass),({"withFileTypes": true, "recursive": true}));
return cljs.core.some((function (ent){
if(cljs.core.truth_((function (){var and__5140__auto__ = ent.isFile();
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ent.name,filename);
} else {
return and__5140__auto__;
}
})())){
return shadow.esm.esm_import$node_path.join(ent.parentPath,ent.name);
} else {
return null;
}
}),entries);
}catch (e50878){var _ = e50878;
return null;
}});
knoxx.backend.contracts.loader.contract_file_path = (function knoxx$backend$contracts$loader$contract_file_path(var_args){
var G__50897 = arguments.length;
switch (G__50897) {
case 2:
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$2 = (function (config,contract_id){
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"agents",contract_id);
}));

(knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3 = (function (config,contract_class,contract_id){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
var id = knoxx.backend.contracts.loader.safe_path_segment_BANG_(contract_id,"contract-id");
var filename = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)+".edn");
var existing = cljs.core.some((function (root){
return knoxx.backend.contracts.loader.find_contract_file_recursive(root,klass,filename);
}),knoxx.backend.contracts.loader.contract_root_paths(config));
var or__5142__auto__ = existing;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_path.join(knoxx.backend.contracts.loader.resolve_contracts_dir(config),klass,filename);
}
}));

(knoxx.backend.contracts.loader.contract_file_path.cljs$lang$maxFixedArity = 3);

knoxx.backend.contracts.loader.role_file_path = (function knoxx$backend$contracts$loader$role_file_path(config,slug){
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"roles",slug);
});
knoxx.backend.contracts.loader.capability_file_path = (function knoxx$backend$contracts$loader$capability_file_path(config,slug){
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"capabilities",slug);
});
knoxx.backend.contracts.loader.actor_file_path = (function knoxx$backend$contracts$loader$actor_file_path(config,actor_id){
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(config,"actors",actor_id);
});
knoxx.backend.contracts.loader.read_edn_file_BANG_ = (function knoxx$backend$contracts$loader$read_edn_file_BANG_(file_path){
return shadow.esm.esm_import$node_fs$promises.readFile(file_path,"utf8").then((function (text){
return cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)));
}));
});
knoxx.backend.contracts.loader.ensure_dir_BANG_ = (function knoxx$backend$contracts$loader$ensure_dir_BANG_(dir){
return shadow.esm.esm_import$node_fs$promises.mkdir(dir,({"recursive": true}));
});
knoxx.backend.contracts.loader.write_edn_file_BANG_ = (function knoxx$backend$contracts$loader$write_edn_file_BANG_(file_path,edn_text){
knoxx.backend.contracts.loader.invalidate_sync_contract_cache_BANG_();

var dir = shadow.esm.esm_import$node_path.dirname(file_path);
return knoxx.backend.contracts.loader.ensure_dir_BANG_(dir).then((function (){
return shadow.esm.esm_import$node_fs$promises.writeFile(file_path,edn_text,"utf8");
}));
});
knoxx.backend.contracts.loader.list_contract_ids_sync = (function knoxx$backend$contracts$loader$list_contract_ids_sync(config,contract_class){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__50953_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__50953_SHARP_));
}),knoxx.backend.contracts.loader.load_all_contracts_sync(config))))));
});
knoxx.backend.contracts.loader.load_contract_BANG_ = (function knoxx$backend$contracts$loader$load_contract_BANG_(var_args){
var G__50962 = arguments.length;
switch (G__50962) {
case 2:
return knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (config,contract_id){
return knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$3(config,"agents",contract_id);
}));

(knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (config,contract_class,contract_id){
var klass = knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
var wanted_id = (function (){var G__50968 = contract_id;
var G__50968__$1 = (((G__50968 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50968)));
var G__50968__$2 = (((G__50968__$1 == null))?null:clojure.string.trim(G__50968__$1));
if((G__50968__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50968__$2);
}
})();
return knoxx.backend.contracts.loader.load_all_contracts_BANG_(config).then((function (records){
var temp__5823__auto__ = cljs.core.some((function (candidate){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(candidate))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wanted_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(candidate))))){
return candidate;
} else {
return null;
}
}),records);
if(cljs.core.truth_(temp__5823__auto__)){
var record = temp__5823__auto__;
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok?","ok?",447310304),true,new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458),new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458).cljs$core$IFn$_invoke$arity$1(record),new cljs.core.Keyword(null,"contract","contract",798152745),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,"agents"))?knoxx.backend.contracts.actor_scope.normalize_agent_contract(new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record)):new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record)),new cljs.core.Keyword(null,"validation","validation",-2141396518),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"errors","errors",-908790718),cljs.core.PersistentVector.EMPTY], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok?","ok?",447310304),false,new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458),"",new cljs.core.Keyword(null,"contract","contract",798152745),null,new cljs.core.Keyword(null,"validation","validation",-2141396518),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"message","message",-406056002),"Contract not found"], null)], null)], null)], null);
}
}));
}));

(knoxx.backend.contracts.loader.load_contract_BANG_.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.contracts.loader.js.map
