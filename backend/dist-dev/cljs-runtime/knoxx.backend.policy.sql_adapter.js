import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./honey.sql.js";
import "./knoxx.backend.policy.protocol.js";
goog.provide('knoxx.backend.policy.sql_adapter');

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
knoxx.backend.policy.sql_adapter.SqlPolicyStore = (function (query_one_BANG_,query_BANG_,find_org_by_slug_BANG_,set_membership_roles_BANG_,primary_org,__meta,__extmap,__hash){
this.query_one_BANG_ = query_one_BANG_;
this.query_BANG_ = query_BANG_;
this.find_org_by_slug_BANG_ = find_org_by_slug_BANG_;
this.set_membership_roles_BANG_ = set_membership_roles_BANG_;
this.primary_org = primary_org;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k526842,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__526846 = k526842;
var G__526846__$1 = (((G__526846 instanceof cljs.core.Keyword))?G__526846.fqn:null);
switch (G__526846__$1) {
case "query-one!":
return self__.query_one_BANG_;

break;
case "query!":
return self__.query_BANG_;

break;
case "find-org-by-slug!":
return self__.find_org_by_slug_BANG_;

break;
case "set-membership-roles!":
return self__.set_membership_roles_BANG_;

break;
case "primary-org":
return self__.primary_org;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k526842,else__5451__auto__);

}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__526847){
var vec__526848 = p__526847;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__526848,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__526848,(1),null);
return (f__5469__auto__.cljs$core$IFn$_invoke$arity$3 ? f__5469__auto__.cljs$core$IFn$_invoke$arity$3(ret__5471__auto__,k__5472__auto__,v__5473__auto__) : f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__));
}),init__5470__auto__,this__5468__auto____$1);
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer(writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer(writer__5464__auto__,pr_pair__5466__auto__,"#knoxx.backend.policy.sql-adapter.SqlPolicyStore{",", ","}",opts__5465__auto__,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"query-one!","query-one!",373666417),self__.query_one_BANG_],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"query!","query!",1326722454),self__.query_BANG_],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),self__.find_org_by_slug_BANG_],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),self__.set_membership_roles_BANG_],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),self__.primary_org],null))], null),self__.__extmap));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__526841){
var self__ = this;
var G__526841__$1 = this;
return (new cljs.core.RecordIter((0),G__526841__$1,5,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query-one!","query-one!",373666417),new cljs.core.Keyword(null,"query!","query!",1326722454),new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),new cljs.core.Keyword(null,"primary-org","primary-org",-717687488)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,self__.__extmap,self__.__hash));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (5 + cljs.core.count(self__.__extmap));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (42653026 ^ cljs.core.hash_unordered_coll(coll__5445__auto__));
})(this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this526843,other526844){
var self__ = this;
var this526843__$1 = this;
return (((!((other526844 == null)))) && ((((this526843__$1.constructor === other526844.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.query_one_BANG_,other526844.query_one_BANG_)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.query_BANG_,other526844.query_BANG_)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.find_org_by_slug_BANG_,other526844.find_org_by_slug_BANG_)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.set_membership_roles_BANG_,other526844.set_membership_roles_BANG_)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.primary_org,other526844.primary_org)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this526843__$1.__extmap,other526844.__extmap)))))))))))))));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),null,new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),null,new cljs.core.Keyword(null,"query-one!","query-one!",373666417),null,new cljs.core.Keyword(null,"query!","query!",1326722454),null,new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core._with_meta(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,cljs.core.not_empty(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(self__.__extmap,k__5459__auto__)),null));
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k526842){
var self__ = this;
var this__5455__auto____$1 = this;
var G__526853 = k526842;
var G__526853__$1 = (((G__526853 instanceof cljs.core.Keyword))?G__526853.fqn:null);
switch (G__526853__$1) {
case "query-one!":
case "query!":
case "find-org-by-slug!":
case "set-membership-roles!":
case "primary-org":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k526842);

}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__526841){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__526854 = cljs.core.keyword_identical_QMARK_;
var expr__526855 = k__5457__auto__;
if(cljs.core.truth_((pred__526854.cljs$core$IFn$_invoke$arity$2 ? pred__526854.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"query-one!","query-one!",373666417),expr__526855) : pred__526854.call(null,new cljs.core.Keyword(null,"query-one!","query-one!",373666417),expr__526855)))){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(G__526841,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__526854.cljs$core$IFn$_invoke$arity$2 ? pred__526854.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"query!","query!",1326722454),expr__526855) : pred__526854.call(null,new cljs.core.Keyword(null,"query!","query!",1326722454),expr__526855)))){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,G__526841,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__526854.cljs$core$IFn$_invoke$arity$2 ? pred__526854.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),expr__526855) : pred__526854.call(null,new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),expr__526855)))){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,G__526841,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__526854.cljs$core$IFn$_invoke$arity$2 ? pred__526854.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),expr__526855) : pred__526854.call(null,new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),expr__526855)))){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,G__526841,self__.primary_org,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__526854.cljs$core$IFn$_invoke$arity$2 ? pred__526854.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),expr__526855) : pred__526854.call(null,new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),expr__526855)))){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,G__526841,self__.__meta,self__.__extmap,null));
} else {
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__526841),null));
}
}
}
}
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"query-one!","query-one!",373666417),self__.query_one_BANG_,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"query!","query!",1326722454),self__.query_BANG_,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),self__.find_org_by_slug_BANG_,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),self__.set_membership_roles_BANG_,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),self__.primary_org,null))], null),self__.__extmap));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__526841){
var self__ = this;
var this__5447__auto____$1 = this;
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(self__.query_one_BANG_,self__.query_BANG_,self__.find_org_by_slug_BANG_,self__.set_membership_roles_BANG_,self__.primary_org,G__526841,self__.__extmap,self__.__hash));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_(entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth(entry__5454__auto__,(0)),cljs.core._nth(entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.getBasis = (function (){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"query-one!","query-one!",2014197944,null),new cljs.core.Symbol(null,"query!","query!",-1327713315,null),new cljs.core.Symbol(null,"find-org-by-slug!","find-org-by-slug!",-531941440,null),new cljs.core.Symbol(null,"set-membership-roles!","set-membership-roles!",-1986158352,null),new cljs.core.Symbol(null,"primary-org","primary-org",922844039,null)], null);
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.cljs$lang$type = true);

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"knoxx.backend.policy.sql-adapter/SqlPolicyStore",null,(1),null));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write(writer__5495__auto__,"knoxx.backend.policy.sql-adapter/SqlPolicyStore");
}));

/**
 * Positional factory function for knoxx.backend.policy.sql-adapter/SqlPolicyStore.
 */
knoxx.backend.policy.sql_adapter.__GT_SqlPolicyStore = (function knoxx$backend$policy$sql_adapter$__GT_SqlPolicyStore(query_one_BANG_,query_BANG_,find_org_by_slug_BANG_,set_membership_roles_BANG_,primary_org){
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(query_one_BANG_,query_BANG_,find_org_by_slug_BANG_,set_membership_roles_BANG_,primary_org,null,null,null));
});

/**
 * Factory function for knoxx.backend.policy.sql-adapter/SqlPolicyStore, taking a map of keywords to field values.
 */
knoxx.backend.policy.sql_adapter.map__GT_SqlPolicyStore = (function knoxx$backend$policy$sql_adapter$map__GT_SqlPolicyStore(G__526845){
var extmap__5490__auto__ = (function (){var G__526858 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(G__526845,new cljs.core.Keyword(null,"query-one!","query-one!",373666417),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"query!","query!",1326722454),new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),new cljs.core.Keyword(null,"primary-org","primary-org",-717687488)], 0));
if(cljs.core.record_QMARK_(G__526845)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__526858);
} else {
return G__526858;
}
})();
return (new knoxx.backend.policy.sql_adapter.SqlPolicyStore(new cljs.core.Keyword(null,"query-one!","query-one!",373666417).cljs$core$IFn$_invoke$arity$1(G__526845),new cljs.core.Keyword(null,"query!","query!",1326722454).cljs$core$IFn$_invoke$arity$1(G__526845),new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329).cljs$core$IFn$_invoke$arity$1(G__526845),new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417).cljs$core$IFn$_invoke$arity$1(G__526845),new cljs.core.Keyword(null,"primary-org","primary-org",-717687488).cljs$core$IFn$_invoke$arity$1(G__526845),null,cljs.core.not_empty(extmap__5490__auto__),null));
});

knoxx.backend.policy.sql_adapter.create_store = (function knoxx$backend$policy$sql_adapter$create_store(p__526859){
var map__526860 = p__526859;
var map__526860__$1 = cljs.core.__destructure_map(map__526860);
var query_one_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526860__$1,new cljs.core.Keyword(null,"query-one!","query-one!",373666417));
var query_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526860__$1,new cljs.core.Keyword(null,"query!","query!",1326722454));
var find_org_by_slug_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526860__$1,new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329));
var set_membership_roles_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526860__$1,new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417));
var primary_org = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526860__$1,new cljs.core.Keyword(null,"primary-org","primary-org",-717687488));
return knoxx.backend.policy.sql_adapter.__GT_SqlPolicyStore(query_one_BANG_,query_BANG_,find_org_by_slug_BANG_,set_membership_roles_BANG_,primary_org);
});
knoxx.backend.policy.sql_adapter.format_sql = (function knoxx$backend$policy$sql_adapter$format_sql(query_map){
return honey.sql.format.cljs$core$IFn$_invoke$arity$2(query_map,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"numbered","numbered",-2119856269),true], null));
});
knoxx.backend.policy.sql_adapter.execute_one_BANG_ = (function knoxx$backend$policy$sql_adapter$execute_one_BANG_(store,query_map){
var vec__526861 = knoxx.backend.policy.sql_adapter.format_sql(query_map);
var seq__526862 = cljs.core.seq(vec__526861);
var first__526863 = cljs.core.first(seq__526862);
var seq__526862__$1 = cljs.core.next(seq__526862);
var sql_str = first__526863;
var params = seq__526862__$1;
var fexpr__526864 = new cljs.core.Keyword(null,"query-one!","query-one!",373666417).cljs$core$IFn$_invoke$arity$1(store);
return (fexpr__526864.cljs$core$IFn$_invoke$arity$2 ? fexpr__526864.cljs$core$IFn$_invoke$arity$2(sql_str,params) : fexpr__526864.call(null,sql_str,params));
});
knoxx.backend.policy.sql_adapter.normalize_email = (function knoxx$backend$policy$sql_adapter$normalize_email(value){
var G__526865 = value;
var G__526865__$1 = (((G__526865 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526865)));
var G__526865__$2 = (((G__526865__$1 == null))?null:clojure.string.trim(G__526865__$1));
var G__526865__$3 = (((G__526865__$2 == null))?null:clojure.string.lower_case(G__526865__$2));
if((G__526865__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__526865__$3);
}
});
knoxx.backend.policy.sql_adapter.actor_email_from_id = (function knoxx$backend$policy$sql_adapter$actor_email_from_id(actor_id){
var slug = clojure.string.replace(clojure.string.replace(clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))),/[^a-z0-9._+-]+/,"-"),/^[-.]+|[-.]+$/,"");
if(clojure.string.blank_QMARK_(slug)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)+"@actors.local");
}
});
knoxx.backend.policy.sql_adapter.actor_credentials_select_query = (function knoxx$backend$policy$sql_adapter$actor_credentials_select_query(provider){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"select","select",1147833503),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ac.*","ac.*",-1072937509),new cljs.core.Keyword(null,"m.actor_id","m.actor_id",2025150514),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.slug","o.slug",-675756042),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770)], null)], null),new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor_credentials","actor_credentials",774592373),new cljs.core.Keyword(null,"ac","ac",-1891632475)], null)], null),new cljs.core.Keyword(null,"join","join",-758861890),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"m","m",1632677161)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.Keyword(null,"ac.user_id","ac.user_id",231890635)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.Keyword(null,"ac.org_id","ac.org_id",342159231)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"orgs","orgs",155776628),new cljs.core.Keyword(null,"o","o",-1350007228)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"o.id","o.id",-1100310568),new cljs.core.Keyword(null,"ac.org_id","ac.org_id",342159231)], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"ac.provider","ac.provider",-422592119),provider], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"ac.status","ac.status",1463122517),"active"], null)], null),new cljs.core.Keyword(null,"order-by","order-by",1527318070),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.actor_id","m.actor_id",2025150514),new cljs.core.Keyword(null,"asc","asc",356854569)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ac.updated_at","ac.updated_at",-1204564901),new cljs.core.Keyword(null,"desc","desc",2093485764)], null)], null)], null);
});
knoxx.backend.policy.sql_adapter.actor_credential_select_query = (function knoxx$backend$policy$sql_adapter$actor_credential_select_query(actor_id,provider){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"select","select",1147833503),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ac.*","ac.*",-1072937509),new cljs.core.Keyword(null,"m.actor_id","m.actor_id",2025150514),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.slug","o.slug",-675756042),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770)], null)], null),new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor_credentials","actor_credentials",774592373),new cljs.core.Keyword(null,"ac","ac",-1891632475)], null)], null),new cljs.core.Keyword(null,"join","join",-758861890),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"m","m",1632677161)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.Keyword(null,"ac.user_id","ac.user_id",231890635)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.Keyword(null,"ac.org_id","ac.org_id",342159231)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"orgs","orgs",155776628),new cljs.core.Keyword(null,"o","o",-1350007228)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"o.id","o.id",-1100310568),new cljs.core.Keyword(null,"ac.org_id","ac.org_id",342159231)], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.actor_id","m.actor_id",2025150514),actor_id], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"ac.provider","ac.provider",-422592119),provider], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"ac.status","ac.status",1463122517),"active"], null)], null),new cljs.core.Keyword(null,"order-by","order-by",1527318070),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.is_default","m.is_default",-1592367580),new cljs.core.Keyword(null,"desc","desc",2093485764)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ac.updated_at","ac.updated_at",-1204564901),new cljs.core.Keyword(null,"desc","desc",2093485764)], null)], null),new cljs.core.Keyword(null,"limit","limit",-1355822363),(1)], null);
});
knoxx.backend.policy.sql_adapter.actor_membership_select_query = (function knoxx$backend$policy$sql_adapter$actor_membership_select_query(p__526870){
var map__526871 = p__526870;
var map__526871__$1 = cljs.core.__destructure_map(map__526871);
var actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526871__$1,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
var user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526871__$1,new cljs.core.Keyword(null,"user-id","user-id",-206822291));
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526871__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"select","select",1147833503),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.*","m.*",-1225802390),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.slug","o.slug",-675756042),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770)], null)], null),new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"m","m",1632677161)], null)], null),new cljs.core.Keyword(null,"join","join",-758861890),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"orgs","orgs",155776628),new cljs.core.Keyword(null,"o","o",-1350007228)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"o.id","o.id",-1100310568),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842)], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),(function (){var G__526872 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817)], null);
var G__526872__$1 = (cljs.core.truth_(actor_id)?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__526872,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.actor_id","m.actor_id",2025150514),actor_id], null)):G__526872);
var G__526872__$2 = (cljs.core.truth_(user_id)?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__526872__$1,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),user_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null)], null)):G__526872__$1);
if(cljs.core.truth_(org_id)){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__526872__$2,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),org_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null)], null));
} else {
return G__526872__$2;
}
})(),new cljs.core.Keyword(null,"order-by","order-by",1527318070),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.is_default","m.is_default",-1592367580),new cljs.core.Keyword(null,"desc","desc",2093485764)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.updated_at","m.updated_at",-204467831),new cljs.core.Keyword(null,"desc","desc",2093485764)], null)], null),new cljs.core.Keyword(null,"limit","limit",-1355822363),(1)], null);
});
knoxx.backend.policy.sql_adapter.actor_user_upsert_query = (function knoxx$backend$policy$sql_adapter$actor_user_upsert_query(p__526873){
var map__526874 = p__526873;
var map__526874__$1 = cljs.core.__destructure_map(map__526874);
var email = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526874__$1,new cljs.core.Keyword(null,"email","email",1415816706));
var display_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526874__$1,new cljs.core.Keyword(null,"display-name","display-name",694513143));
var auth_provider = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526874__$1,new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231));
var external_subject = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526874__$1,new cljs.core.Keyword(null,"external-subject","external-subject",-265707402));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526874__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"insert-into","insert-into",382212789),new cljs.core.Keyword(null,"users","users",-713552705),new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"email","email",1415816706),new cljs.core.Keyword(null,"display_name","display_name",-1494335013),new cljs.core.Keyword(null,"auth_provider","auth_provider",-1634726609),new cljs.core.Keyword(null,"external_subject","external_subject",-2123976135),new cljs.core.Keyword(null,"status","status",-1997798413)], null),new cljs.core.Keyword(null,"values","values",372645556),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [email,display_name,auth_provider,external_subject,(function (){var or__5142__auto__ = status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()], null)], null),new cljs.core.Keyword(null,"on-conflict","on-conflict",1595391642),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"email","email",1415816706)], null),new cljs.core.Keyword(null,"do-update-set","do-update-set",-2028298967),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"display_name","display_name",-1494335013),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.display_name"], null),new cljs.core.Keyword(null,"auth_provider","auth_provider",-1634726609),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.auth_provider"], null),new cljs.core.Keyword(null,"external_subject","external_subject",-2123976135),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.external_subject"], null),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.status"], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"NOW()"], null)], null),new cljs.core.Keyword(null,"returning","returning",-387623629),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"*","*",-1294732318)], null)], null);
});
knoxx.backend.policy.sql_adapter.actor_user_update_query = (function knoxx$backend$policy$sql_adapter$actor_user_update_query(user_id,p__526875){
var map__526876 = p__526875;
var map__526876__$1 = cljs.core.__destructure_map(map__526876);
var email = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526876__$1,new cljs.core.Keyword(null,"email","email",1415816706));
var display_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526876__$1,new cljs.core.Keyword(null,"display-name","display-name",694513143));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526876__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var auth_provider = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526876__$1,new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231));
var external_subject = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526876__$1,new cljs.core.Keyword(null,"external-subject","external-subject",-265707402));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.Keyword(null,"users","users",-713552705),new cljs.core.Keyword(null,"set","set",304602554),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"email","email",1415816706),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),email,new cljs.core.Keyword(null,"email","email",1415816706)], null),new cljs.core.Keyword(null,"display_name","display_name",-1494335013),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),display_name,new cljs.core.Keyword(null,"display_name","display_name",-1494335013)], null),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),status,new cljs.core.Keyword(null,"status","status",-1997798413)], null),new cljs.core.Keyword(null,"auth_provider","auth_provider",-1634726609),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),auth_provider,new cljs.core.Keyword(null,"auth_provider","auth_provider",-1634726609)], null),new cljs.core.Keyword(null,"external_subject","external_subject",-2123976135),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),external_subject,new cljs.core.Keyword(null,"external_subject","external_subject",-2123976135)], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"NOW()"], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),user_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null)], null),new cljs.core.Keyword(null,"returning","returning",-387623629),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"*","*",-1294732318)], null)], null);
});
knoxx.backend.policy.sql_adapter.actor_membership_upsert_query = (function knoxx$backend$policy$sql_adapter$actor_membership_upsert_query(p__526877){
var map__526878 = p__526877;
var map__526878__$1 = cljs.core.__destructure_map(map__526878);
var user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526878__$1,new cljs.core.Keyword(null,"user-id","user-id",-206822291));
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526878__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526878__$1,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526878__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var is_default = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526878__$1,new cljs.core.Keyword(null,"is-default","is-default",1401171070));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"insert-into","insert-into",382212789),new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"is_default","is_default",-922813238)], null),new cljs.core.Keyword(null,"values","values",372645556),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),user_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),org_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null),actor_id,(function (){var or__5142__auto__ = status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})(),cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(is_default,false)], null)], null),new cljs.core.Keyword(null,"on-conflict","on-conflict",1595391642),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"org_id","org_id",1380185385)], null),new cljs.core.Keyword(null,"do-update-set","do-update-set",-2028298967),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.actor_id"], null),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.status"], null),new cljs.core.Keyword(null,"is_default","is_default",-922813238),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.is_default"], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"NOW()"], null)], null),new cljs.core.Keyword(null,"returning","returning",-387623629),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"*","*",-1294732318)], null)], null);
});
knoxx.backend.policy.sql_adapter.actor_membership_actor_update_query = (function knoxx$backend$policy$sql_adapter$actor_membership_actor_update_query(membership_id,actor_id){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"set","set",304602554),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),actor_id,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"NOW()"], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),membership_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null)], null),new cljs.core.Keyword(null,"returning","returning",-387623629),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"*","*",-1294732318)], null)], null);
});
knoxx.backend.policy.sql_adapter.actor_credential_upsert_query = (function knoxx$backend$policy$sql_adapter$actor_credential_upsert_query(p__526881){
var map__526882 = p__526881;
var map__526882__$1 = cljs.core.__destructure_map(map__526882);
var user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"user-id","user-id",-206822291));
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var provider = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"provider","provider",-302056900));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
var account_identifier = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"account-identifier","account-identifier",258852778));
var secret_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"secret-json","secret-json",-436252008));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__526882__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"insert-into","insert-into",382212789),new cljs.core.Keyword(null,"actor_credentials","actor_credentials",774592373),new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"provider","provider",-302056900),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"account_identifier","account_identifier",-1949012161),new cljs.core.Keyword(null,"secret_json","secret_json",-724933577),new cljs.core.Keyword(null,"status","status",-1997798413)], null),new cljs.core.Keyword(null,"values","values",372645556),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),user_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),org_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null),provider,(function (){var or__5142__auto__ = kind;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "credential";
}
})(),account_identifier,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),secret_json,new cljs.core.Keyword(null,"jsonb","jsonb",-826402072)], null),(function (){var or__5142__auto__ = status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()], null)], null),new cljs.core.Keyword(null,"on-conflict","on-conflict",1595391642),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"provider","provider",-302056900),new cljs.core.Keyword(null,"kind","kind",-717265803)], null),new cljs.core.Keyword(null,"do-update-set","do-update-set",-2028298967),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"account_identifier","account_identifier",-1949012161),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coalesce","coalesce",654622029),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.account_identifier"], null),new cljs.core.Keyword(null,"actor_credentials.account_identifier","actor_credentials.account_identifier",1753404584)], null),new cljs.core.Keyword(null,"secret_json","secret_json",-724933577),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"||","||",-207700737),new cljs.core.Keyword(null,"actor_credentials.secret_json","actor_credentials.secret_json",2030768516),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.secret_json"], null)], null),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"EXCLUDED.status"], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"raw","raw",1604651272),"NOW()"], null)], null),new cljs.core.Keyword(null,"returning","returning",-387623629),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"*","*",-1294732318)], null)], null);
});
knoxx.backend.policy.sql_adapter.user_memberships_query = (function knoxx$backend$policy$sql_adapter$user_memberships_query(user_ids,org_id){
var G__526883 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"select","select",1147833503),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.*","m.*",-1225802390),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.name","o.name",83564940),new cljs.core.Keyword(null,"org_name","org_name",-1732897410)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.slug","o.slug",-675756042),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770)], null)], null),new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memberships","memberships",1865599157),new cljs.core.Keyword(null,"m","m",1632677161)], null)], null),new cljs.core.Keyword(null,"join","join",-758861890),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"orgs","orgs",155776628),new cljs.core.Keyword(null,"o","o",-1350007228)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"o.id","o.id",-1100310568),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842)], null)], null),new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(user_ids),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),cljs.core.PersistentVector.EMPTY], null)], null)], null),new cljs.core.Keyword(null,"order-by","order-by",1527318070),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"o.name","o.name",83564940),new cljs.core.Keyword(null,"asc","asc",356854569)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.created_at","m.created_at",1807502461),new cljs.core.Keyword(null,"asc","asc",356854569)], null)], null)], null);
if(cljs.core.truth_(org_id)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(G__526883,new cljs.core.Keyword(null,"where","where",-2044795965),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.user_id","m.user_id",-1368963548),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(user_ids),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),cljs.core.PersistentVector.EMPTY], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"m.org_id","m.org_id",-1666616842),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cast","cast",-1761029143),org_id,new cljs.core.Keyword(null,"uuid","uuid",-2145095719)], null)], null)], null),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"order-by","order-by",1527318070),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"m.created_at","m.created_at",1807502461),new cljs.core.Keyword(null,"asc","asc",356854569)], null)], null)], 0));
} else {
return G__526883;
}
});
knoxx.backend.policy.sql_adapter.row__GT_credential = (function knoxx$backend$policy$sql_adapter$row__GT_credential(row){
if(cljs.core.truth_(row)){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"userId","userId",575594135),new cljs.core.Keyword(null,"secretJson","secretJson",1807839704),new cljs.core.Keyword(null,"provider","provider",-302056900)],[(row["actor_id"]),(row["account_identifier"]),(row["updated_at"]),(row["org_id"]),(row["org_slug"]),(row["created_at"]),(row["status"]),(row["id"]),(row["kind"]),(row["user_id"]),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (row["secret_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),(row["provider"])]);
} else {
return null;
}
});
knoxx.backend.policy.sql_adapter.actor_role_slugs = (function knoxx$backend$policy$sql_adapter$actor_role_slugs(actor){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (role){
if((role instanceof cljs.core.Keyword)){
return clojure.string.replace(cljs.core.name(role),/_/,"-");
} else {
if(typeof role === 'string'){
return clojure.string.replace(clojure.string.trim(role),/_/,"-");
} else {
return null;

}
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword("actor","roles","actor/roles",186081855).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.policy.sql_adapter.project_actor_BANG_ = (function knoxx$backend$policy$sql_adapter$project_actor_BANG_(store,actor){
var validated = knoxx.backend.policy.protocol.validate_actor_BANG_(actor);
var actor_id = new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(validated);
var email = (function (){var or__5142__auto__ = knoxx.backend.policy.sql_adapter.normalize_email(new cljs.core.Keyword("actor","email","actor/email",1189986301).cljs$core$IFn$_invoke$arity$1(validated));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy.sql_adapter.actor_email_from_id(actor_id);
}
})();
var display_name = (function (){var or__5142__auto__ = (function (){var G__526889 = new cljs.core.Keyword("actor","label","actor/label",-1796720603).cljs$core$IFn$_invoke$arity$1(validated);
var G__526889__$1 = (((G__526889 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526889)));
var G__526889__$2 = (((G__526889__$1 == null))?null:clojure.string.trim(G__526889__$1));
if((G__526889__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526889__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = actor_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return email;
}
}
})();
var auth_provider = "actor-contract";
var role_slugs = knoxx.backend.policy.sql_adapter.actor_role_slugs(validated);
return knoxx.backend.policy.sql_adapter.execute_one_BANG_(store,knoxx.backend.policy.sql_adapter.actor_user_upsert_query(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"display-name","display-name",694513143),display_name,new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231),auth_provider,new cljs.core.Keyword(null,"external-subject","external-subject",-265707402),null,new cljs.core.Keyword(null,"status","status",-1997798413),"active"], null))).then((function (user){
var org_promise = (function (){var temp__5823__auto__ = (function (){var G__526890 = new cljs.core.Keyword("actor","org","actor/org",175993262).cljs$core$IFn$_invoke$arity$1(validated);
var G__526890__$1 = (((G__526890 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526890)));
var G__526890__$2 = (((G__526890__$1 == null))?null:clojure.string.trim(G__526890__$1));
if((G__526890__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526890__$2);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var org_slug = temp__5823__auto__;
var fexpr__526891 = new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329).cljs$core$IFn$_invoke$arity$1(store);
return (fexpr__526891.cljs$core$IFn$_invoke$arity$1 ? fexpr__526891.cljs$core$IFn$_invoke$arity$1(org_slug) : fexpr__526891.call(null,org_slug));
} else {
return Promise.resolve(null);
}
})();
return org_promise.then((function (org){
var target_org = (function (){var or__5142__auto__ = org;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"primary-org","primary-org",-717687488).cljs$core$IFn$_invoke$arity$1(store);
}
})();
if(cljs.core.truth_(target_org)){
} else {
throw (new Error("primary org is required for actor projection sync"));
}

return knoxx.backend.policy.sql_adapter.execute_one_BANG_(store,knoxx.backend.policy.sql_adapter.actor_membership_upsert_query(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"user-id","user-id",-206822291),(user["id"]),new cljs.core.Keyword(null,"org-id","org-id",1485182668),(target_org["id"]),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id,new cljs.core.Keyword(null,"status","status",-1997798413),"active",new cljs.core.Keyword(null,"is-default","is-default",1401171070),true], null)));
})).then((function (membership){
var temp__5823__auto__ = new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417).cljs$core$IFn$_invoke$arity$1(store);
if(cljs.core.truth_(temp__5823__auto__)){
var set_roles_BANG_ = temp__5823__auto__;
return (function (){var G__526894 = (membership["id"]);
var G__526895 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),(membership["org_id"]),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),role_slugs,new cljs.core.Keyword(null,"role-ids","role-ids",652985101),[],new cljs.core.Keyword(null,"replace","replace",-786587770),true,new cljs.core.Keyword(null,"contract-projection","contract-projection",-1495437365),true], null);
return (set_roles_BANG_.cljs$core$IFn$_invoke$arity$2 ? set_roles_BANG_.cljs$core$IFn$_invoke$arity$2(G__526894,G__526895) : set_roles_BANG_.call(null,G__526894,G__526895));
})().then((function (_){
return membership;
})).catch((function (err){
console.warn("[policy-sql] actor role projection failed; keeping actor membership",actor_id,err.message);

return membership;
}));
} else {
return membership;
}
}));
}));
});
(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$ = cljs.core.PROTOCOL_SENTINEL);

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$list_contracts$arity$2 = (function (_store,_contract_class){
var _store__$1 = this;
throw (new Error("SQL policy adapter is projection-only; read canonical contracts through the EDN adapter."));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$get_contract$arity$3 = (function (_store,_contract_class,_contract_id){
var _store__$1 = this;
throw (new Error("SQL policy adapter is projection-only; read canonical contracts through the EDN adapter."));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$arity$3 = (function (_store,_contract_class,_contract){
var _store__$1 = this;
throw (new Error("SQL policy adapter is projection-only; write canonical contracts through the EDN adapter."));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$list_actors$arity$1 = (function (_store){
var _store__$1 = this;
return knoxx.backend.policy.protocol.list_contracts(_store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398));
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$get_actor$arity$2 = (function (_store,actor_id){
var _store__$1 = this;
return knoxx.backend.policy.protocol.get_contract(_store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398),actor_id);
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$arity$2 = (function (_store,actor){
var _store__$1 = this;
return knoxx.backend.policy.protocol.upsert_contract_BANG_(_store__$1,new cljs.core.Keyword(null,"actors","actors",-1845636398),actor);
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorCredentialStore$ = cljs.core.PROTOCOL_SENTINEL);

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorCredentialStore$list_actor_credentials$arity$2 = (function (store,provider){
var store__$1 = this;
var provider__$1 = (function (){var G__526919 = provider;
var G__526919__$1 = (((G__526919 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526919)));
var G__526919__$2 = (((G__526919__$1 == null))?null:clojure.string.trim(G__526919__$1));
if((G__526919__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526919__$2);
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider__$1)))){
return Promise.reject((new Error("provider is required")));
} else {
var vec__526921 = knoxx.backend.policy.sql_adapter.format_sql(knoxx.backend.policy.sql_adapter.actor_credentials_select_query(provider__$1));
var seq__526922 = cljs.core.seq(vec__526921);
var first__526923 = cljs.core.first(seq__526922);
var seq__526922__$1 = cljs.core.next(seq__526922);
var sql_str = first__526923;
var params = seq__526922__$1;
return (function (){var fexpr__526924 = store__$1.query_BANG_;
return (fexpr__526924.cljs$core$IFn$_invoke$arity$2 ? fexpr__526924.cljs$core$IFn$_invoke$arity$2(sql_str,params) : fexpr__526924.call(null,sql_str,params));
})().then((function (result){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__526898_SHARP_){
return clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(p1__526898_SHARP_))));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy.sql_adapter.row__GT_credential,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((result["rows"])))));
}));
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorCredentialStore$get_actor_credential$arity$3 = (function (store,actor_id,provider){
var store__$1 = this;
var actor_id__$1 = (function (){var G__526926 = actor_id;
var G__526926__$1 = (((G__526926 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526926)));
var G__526926__$2 = (((G__526926__$1 == null))?null:clojure.string.trim(G__526926__$1));
if((G__526926__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526926__$2);
}
})();
var provider__$1 = (function (){var G__526927 = provider;
var G__526927__$1 = (((G__526927 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526927)));
var G__526927__$2 = (((G__526927__$1 == null))?null:clojure.string.trim(G__526927__$1));
if((G__526927__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526927__$2);
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id__$1)))){
return Promise.reject((new Error("actorId is required")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider__$1)))){
return Promise.reject((new Error("provider is required")));
} else {
return knoxx.backend.policy.sql_adapter.execute_one_BANG_(store__$1,knoxx.backend.policy.sql_adapter.actor_credential_select_query(actor_id__$1,provider__$1)).then(knoxx.backend.policy.sql_adapter.row__GT_credential);

}
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorCredentialStore$upsert_actor_credential_BANG_$arity$4 = (function (store,actor_id,provider,credential){
var store__$1 = this;
var actor_id__$1 = (function (){var G__526928 = actor_id;
var G__526928__$1 = (((G__526928 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526928)));
var G__526928__$2 = (((G__526928__$1 == null))?null:clojure.string.trim(G__526928__$1));
if((G__526928__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526928__$2);
}
})();
var provider__$1 = (function (){var G__526929 = provider;
var G__526929__$1 = (((G__526929 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526929)));
var G__526929__$2 = (((G__526929__$1 == null))?null:clojure.string.trim(G__526929__$1));
if((G__526929__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526929__$2);
}
})();
var user_id = (function (){var G__526930 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"user-id","user-id",-206822291).cljs$core$IFn$_invoke$arity$1(credential);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(credential);
}
})();
var G__526930__$1 = (((G__526930 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526930)));
var G__526930__$2 = (((G__526930__$1 == null))?null:clojure.string.trim(G__526930__$1));
if((G__526930__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526930__$2);
}
})();
var org_id = (function (){var G__526931 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-id","org-id",1485182668).cljs$core$IFn$_invoke$arity$1(credential);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(credential);
}
})();
var G__526931__$1 = (((G__526931 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__526931)));
var G__526931__$2 = (((G__526931__$1 == null))?null:clojure.string.trim(G__526931__$1));
if((G__526931__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__526931__$2);
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id__$1)))){
return Promise.reject((new Error("actorId is required")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider__$1)))){
return Promise.reject((new Error("provider is required")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id)))){
return Promise.reject((new Error("userId is required")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
return Promise.reject((new Error("orgId is required")));
} else {
return knoxx.backend.policy.sql_adapter.execute_one_BANG_(store__$1,knoxx.backend.policy.sql_adapter.actor_membership_select_query(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id__$1,new cljs.core.Keyword(null,"user-id","user-id",-206822291),user_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id], null))).then((function (membership){
if(cljs.core.not(membership)){
return Promise.reject((new Error("actor membership not found")));
} else {
return knoxx.backend.policy.sql_adapter.execute_one_BANG_(store__$1,knoxx.backend.policy.sql_adapter.actor_credential_upsert_query(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"user-id","user-id",-206822291),user_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"provider","provider",-302056900),provider__$1,new cljs.core.Keyword(null,"kind","kind",-717265803),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(credential);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "credential";
}
})(),new cljs.core.Keyword(null,"account-identifier","account-identifier",258852778),new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"secret-json","secret-json",-436252008),JSON.stringify(cljs.core.clj__GT_js((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"secretJson","secretJson",1807839704).cljs$core$IFn$_invoke$arity$1(credential);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})())),new cljs.core.Keyword(null,"status","status",-1997798413),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(credential);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()], null)));
}
})).then(knoxx.backend.policy.sql_adapter.row__GT_credential);

}
}
}
}
}));

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorProjectionStore$ = cljs.core.PROTOCOL_SENTINEL);

(knoxx.backend.policy.sql_adapter.SqlPolicyStore.prototype.knoxx$backend$policy$protocol$ActorProjectionStore$sync_actor_projections_BANG_$arity$2 = (function (store,actors){
var store__$1 = this;
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__526899_SHARP_){
return knoxx.backend.policy.sql_adapter.project_actor_BANG_(store__$1,p1__526899_SHARP_);
}),actors))).then((function (_){
return null;
}));
}));

//# sourceMappingURL=knoxx.backend.policy.sql_adapter.js.map
