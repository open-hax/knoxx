import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.contracts.loader.js";
import "./knoxx.backend.policy.protocol.js";
import "./knoxx.backend.tools.registry.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.policy.edn_adapter');

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
knoxx.backend.policy.edn_adapter.EdnPolicyStore = (function (contracts_dir,__meta,__extmap,__hash){
this.contracts_dir = contracts_dir;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k53025,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__53060 = k53025;
var G__53060__$1 = (((G__53060 instanceof cljs.core.Keyword))?G__53060.fqn:null);
switch (G__53060__$1) {
case "contracts-dir":
return self__.contracts_dir;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k53025,else__5451__auto__);

}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__53066){
var vec__53071 = p__53066;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53071,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53071,(1),null);
return (f__5469__auto__.cljs$core$IFn$_invoke$arity$3 ? f__5469__auto__.cljs$core$IFn$_invoke$arity$3(ret__5471__auto__,k__5472__auto__,v__5473__auto__) : f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__));
}),init__5470__auto__,this__5468__auto____$1);
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer(writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer(writer__5464__auto__,pr_pair__5466__auto__,"#knoxx.backend.policy.edn-adapter.EdnPolicyStore{",", ","}",opts__5465__auto__,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),self__.contracts_dir],null))], null),self__.__extmap));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__53024){
var self__ = this;
var G__53024__$1 = this;
return (new cljs.core.RecordIter((0),G__53024__$1,1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(self__.contracts_dir,self__.__meta,self__.__extmap,self__.__hash));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (1 + cljs.core.count(self__.__extmap));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (692672145 ^ cljs.core.hash_unordered_coll(coll__5445__auto__));
})(this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this53027,other53028){
var self__ = this;
var this53027__$1 = this;
return (((!((other53028 == null)))) && ((((this53027__$1.constructor === other53028.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this53027__$1.contracts_dir,other53028.contracts_dir)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this53027__$1.__extmap,other53028.__extmap)))))));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core._with_meta(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(self__.contracts_dir,self__.__meta,cljs.core.not_empty(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(self__.__extmap,k__5459__auto__)),null));
}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k53025){
var self__ = this;
var this__5455__auto____$1 = this;
var G__53127 = k53025;
var G__53127__$1 = (((G__53127 instanceof cljs.core.Keyword))?G__53127.fqn:null);
switch (G__53127__$1) {
case "contracts-dir":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k53025);

}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__53024){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__53128 = cljs.core.keyword_identical_QMARK_;
var expr__53129 = k__5457__auto__;
if(cljs.core.truth_((pred__53128.cljs$core$IFn$_invoke$arity$2 ? pred__53128.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),expr__53129) : pred__53128.call(null,new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),expr__53129)))){
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(G__53024,self__.__meta,self__.__extmap,null));
} else {
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(self__.contracts_dir,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__53024),null));
}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),self__.contracts_dir,null))], null),self__.__extmap));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__53024){
var self__ = this;
var this__5447__auto____$1 = this;
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(self__.contracts_dir,G__53024,self__.__extmap,self__.__hash));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_(entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth(entry__5454__auto__,(0)),cljs.core._nth(entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"contracts-dir","contracts-dir",1861267262,null)], null);
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.cljs$lang$type = true);

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"knoxx.backend.policy.edn-adapter/EdnPolicyStore",null,(1),null));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write(writer__5495__auto__,"knoxx.backend.policy.edn-adapter/EdnPolicyStore");
}));

/**
 * Positional factory function for knoxx.backend.policy.edn-adapter/EdnPolicyStore.
 */
knoxx.backend.policy.edn_adapter.__GT_EdnPolicyStore = (function knoxx$backend$policy$edn_adapter$__GT_EdnPolicyStore(contracts_dir){
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(contracts_dir,null,null,null));
});

/**
 * Factory function for knoxx.backend.policy.edn-adapter/EdnPolicyStore, taking a map of keywords to field values.
 */
knoxx.backend.policy.edn_adapter.map__GT_EdnPolicyStore = (function knoxx$backend$policy$edn_adapter$map__GT_EdnPolicyStore(G__53033){
var extmap__5490__auto__ = (function (){var G__53157 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__53033,new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735));
if(cljs.core.record_QMARK_(G__53033)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__53157);
} else {
return G__53157;
}
})();
return (new knoxx.backend.policy.edn_adapter.EdnPolicyStore(new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735).cljs$core$IFn$_invoke$arity$1(G__53033),null,cljs.core.not_empty(extmap__5490__auto__),null));
});

knoxx.backend.policy.edn_adapter.create_store = (function knoxx$backend$policy$edn_adapter$create_store(contracts_dir){
return knoxx.backend.policy.edn_adapter.__GT_EdnPolicyStore(contracts_dir);
});
knoxx.backend.policy.edn_adapter.safe_path_segment_BANG_ = (function knoxx$backend$policy$edn_adapter$safe_path_segment_BANG_(segment,kind){
var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment));
if(((clojure.string.blank_QMARK_(s)) || (cljs.core.not(cljs.core.re_matches(/[A-Za-z0-9._-]+/,s))))){
throw (new Error((""+"Invalid "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kind)+" segment: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment))));
} else {
}

return s;
});
knoxx.backend.policy.edn_adapter.loader_config = (function knoxx$backend$policy$edn_adapter$loader_config(store){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735).cljs$core$IFn$_invoke$arity$1(store)], null);
});
knoxx.backend.policy.edn_adapter.normalized_class = (function knoxx$backend$policy$edn_adapter$normalized_class(contract_class){
return knoxx.backend.contracts.loader.normalize_contract_class(contract_class);
});
knoxx.backend.policy.edn_adapter.contract_path = (function knoxx$backend$policy$edn_adapter$contract_path(store,contract_class,contract_id){
return knoxx.backend.contracts.loader.contract_file_path.cljs$core$IFn$_invoke$arity$3(knoxx.backend.policy.edn_adapter.loader_config(store),knoxx.backend.policy.edn_adapter.normalized_class(contract_class),knoxx.backend.policy.edn_adapter.safe_path_segment_BANG_(contract_id,"contract id"));
});
knoxx.backend.policy.edn_adapter.contract_id = (function knoxx$backend$policy$edn_adapter$contract_id(contract_class,contract){
var G__53177 = contract_class;
var G__53177__$1 = (((G__53177 instanceof cljs.core.Keyword))?G__53177.fqn:null);
switch (G__53177__$1) {
case "actors":
return new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(contract);

break;
case "actor":
return new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(contract);

break;
default:
var or__5142__auto__ = new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword("role","id","role/id",-1375589954).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword("cap","id","cap/id",-1388434846).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword("model","id","model/id",-1274892501).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(contract);
}
}
}
}
}

}
});
knoxx.backend.policy.edn_adapter.actor_contract_file_path = (function knoxx$backend$policy$edn_adapter$actor_contract_file_path(store,actor_id){
return knoxx.backend.policy.edn_adapter.contract_path(store,new cljs.core.Keyword(null,"actors","actors",-1845636398),actor_id);
});
knoxx.backend.policy.edn_adapter.normalize_actor_contract = (function knoxx$backend$policy$edn_adapter$normalize_actor_contract(actor){
var G__53188 = actor;
if(((cljs.core.map_QMARK_(actor)) && (cljs.core.not(new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(actor))))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53188,new cljs.core.Keyword("actor","kind","actor/kind",-1410102686),new cljs.core.Keyword(null,"agent","agent",-766455027));
} else {
return G__53188;
}
});
knoxx.backend.policy.edn_adapter.edn_file_paths_under_root = (function knoxx$backend$policy$edn_adapter$edn_file_paths_under_root(root){
try{return cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (ent){
if(cljs.core.truth_((function (){var and__5140__auto__ = ent.isFile();
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof ent.name === 'string') && (((clojure.string.ends_with_QMARK_(ent.name,".edn")) && ((!(clojure.string.starts_with_QMARK_(ent.name,".")))))));
} else {
return and__5140__auto__;
}
})())){
return shadow.esm.esm_import$node_path.join(ent.parentPath,ent.name);
} else {
return null;
}
}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(shadow.esm.esm_import$node_fs.readdirSync(root,({"withFileTypes": true, "recursive": true}))));
}catch (e53201){var _ = e53201;
return cljs.core.PersistentVector.EMPTY;
}});
knoxx.backend.policy.edn_adapter.load_all_contract_records_sync = (function knoxx$backend$policy$edn_adapter$load_all_contract_records_sync(store){
return knoxx.backend.contracts.loader.dedup_contracts(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (file_path){
try{return knoxx.backend.contracts.loader.parse_contract_file_BANG_(file_path,shadow.esm.esm_import$node_fs.readFileSync(file_path,"utf8"));
}catch (e53203){var _ = e53203;
return null;
}}),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.policy.edn_adapter.edn_file_paths_under_root,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.contracts.loader.contract_root_paths(knoxx.backend.policy.edn_adapter.loader_config(store))], 0)))));
});
knoxx.backend.policy.edn_adapter.validate_contract_for_class_BANG_ = (function knoxx$backend$policy$edn_adapter$validate_contract_for_class_BANG_(contract_class,contract){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(contract_class,new cljs.core.Keyword(null,"actors","actors",-1845636398))){
return knoxx.backend.policy.protocol.validate_actor_BANG_(knoxx.backend.policy.edn_adapter.normalize_actor_contract(contract));
} else {
return knoxx.backend.policy.protocol.validate_contract_BANG_(contract_class,contract);
}
});
knoxx.backend.policy.edn_adapter.contract_tool_ids = (function knoxx$backend$policy$edn_adapter$contract_tool_ids(store){
return cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__53208_SHARP_){
var or__5142__auto__ = new cljs.core.Keyword("cap","tools","cap/tools",-1241568196).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(p1__53208_SHARP_));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53207_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("capabilities",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__53207_SHARP_));
}),knoxx.backend.policy.edn_adapter.load_all_contract_records_sync(store))], 0))));
});
(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$ = cljs.core.PROTOCOL_SENTINEL);

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$list_contracts$arity$2 = (function (store,contract_class){
var store__$1 = this;
var klass = knoxx.backend.policy.edn_adapter.normalized_class(contract_class);
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__53210_SHARP_){
return knoxx.backend.policy.edn_adapter.validate_contract_for_class_BANG_(contract_class,new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(p1__53210_SHARP_));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53209_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__53209_SHARP_));
}),knoxx.backend.policy.edn_adapter.load_all_contract_records_sync(store__$1)));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$get_contract$arity$3 = (function (store,contract_class,contract_id){
var store__$1 = this;
var klass = knoxx.backend.policy.edn_adapter.normalized_class(contract_class);
var wanted_id = (function (){var G__53221 = contract_id;
var G__53221__$1 = (((G__53221 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53221)));
var G__53221__$2 = (((G__53221__$1 == null))?null:clojure.string.trim(G__53221__$1));
if((G__53221__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53221__$2);
}
})();
var G__53222 = knoxx.backend.policy.edn_adapter.load_all_contract_records_sync(store__$1);
var G__53222__$1 = (((G__53222 == null))?null:cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53211_SHARP_){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(klass,new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__53211_SHARP_))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wanted_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__53211_SHARP_))));
}),G__53222));
var G__53222__$2 = (((G__53222__$1 == null))?null:cljs.core.first(G__53222__$1));
var G__53222__$3 = (((G__53222__$2 == null))?null:new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(G__53222__$2));
if((G__53222__$3 == null)){
return null;
} else {
return knoxx.backend.policy.edn_adapter.validate_contract_for_class_BANG_(contract_class,G__53222__$3);
}
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$arity$3 = (function (store,contract_class,contract){
var store__$1 = this;
var validated = knoxx.backend.policy.edn_adapter.validate_contract_for_class_BANG_(contract_class,contract);
var id = knoxx.backend.policy.edn_adapter.contract_id(contract_class,validated);
var file_path = knoxx.backend.policy.edn_adapter.contract_path(store__$1,contract_class,id);
shadow.esm.esm_import$node_fs.mkdirSync(shadow.esm.esm_import$node_path.dirname(file_path),({"recursive": true}));

shadow.esm.esm_import$node_fs.writeFileSync(file_path,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([validated], 0)))+"\n"),"utf8");

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"contract","contract",798152745),validated,new cljs.core.Keyword(null,"path","path",-188191168),file_path], null));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$list_actors$arity$1 = (function (store){
var store__$1 = this;
return knoxx.backend.policy.protocol.list_contracts(store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398));
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$get_actor$arity$2 = (function (store,actor_id){
var store__$1 = this;
return knoxx.backend.policy.protocol.get_contract(store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398),actor_id);
}));

(knoxx.backend.policy.edn_adapter.EdnPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$arity$2 = (function (store,actor){
var store__$1 = this;
return knoxx.backend.policy.protocol.upsert_contract_BANG_(store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398),actor);
}));

//# sourceMappingURL=knoxx.backend.policy.edn_adapter.js.map
