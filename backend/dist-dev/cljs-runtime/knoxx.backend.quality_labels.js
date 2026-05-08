import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.quality_labels');
knoxx.backend.quality_labels.good_reaction_tokens = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 10, ["checkmark",null,"quality:good",null,"\u2714\uFE0F",null,"\u2611",null,"good",null,"\u2705",null,"good output",null,"check",null,"\u2611\uFE0F",null,"\u2714",null], null), null);
knoxx.backend.quality_labels.bad_reaction_tokens = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 11, ["bad output",null,"cross",null,"x",null,"\u274E",null,"\u2716\uFE0F",null,"\u2717",null,"\u2718",null,"bad",null,"quality:bad",null,"\u2716",null,"\u274C",null], null), null);
knoxx.backend.quality_labels.normalize_token = (function knoxx$backend$quality_labels$normalize_token(value){
return clojure.string.lower_case(clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\uFE0F/,"")));
});
knoxx.backend.quality_labels.map_get_any = (function knoxx$backend$quality_labels$map_get_any(m,ks){
return cljs.core.some((function (k){
if(cljs.core.contains_QMARK_(m,k)){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,k);
} else {
return null;
}
}),ks);
});
knoxx.backend.quality_labels.seqish_QMARK_ = (function knoxx$backend$quality_labels$seqish_QMARK_(value){
return ((cljs.core.sequential_QMARK_(value)) && ((!(typeof value === 'string'))));
});
knoxx.backend.quality_labels.value_evidence = (function knoxx$backend$quality_labels$value_evidence(value){
if((value == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(cljs.core.map_QMARK_(value)){
return (knoxx.backend.quality_labels.quality_label_evidence.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.quality_labels.quality_label_evidence.cljs$core$IFn$_invoke$arity$1(value) : knoxx.backend.quality_labels.quality_label_evidence.call(null,value));
} else {
if(knoxx.backend.quality_labels.seqish_QMARK_(value)){
return cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.quality_labels.value_evidence,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([value], 0));
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value))], null);

}
}
}
});
knoxx.backend.quality_labels.nested_label_maps = (function knoxx$backend$quality_labels$nested_label_maps(m){
return cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 9, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291),new cljs.core.Keyword(null,"openplanner-labels","openplanner-labels",105838271),new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727)], null)),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291)], null)),cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.Keyword(null,"metadata","metadata",1799301597)),cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,new cljs.core.Keyword(null,"extra","extra",1612569067))], null));
});
knoxx.backend.quality_labels.quality_label_evidence = (function knoxx$backend$quality_labels$quality_label_evidence(m){
var direct = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"quality","quality",147850199),new cljs.core.Keyword(null,"quality_label","quality_label",-955573305),new cljs.core.Keyword(null,"quality-label","quality-label",-149786964)], null)),knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"explicit_meaning","explicit_meaning",1062627523),new cljs.core.Keyword(null,"explicit-meaning","explicit-meaning",-1157802951)], null)),knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"emoji","emoji",1031230144),new cljs.core.Keyword(null,"reaction_emoji","reaction_emoji",-821316473),new cljs.core.Keyword(null,"reaction-emoji","reaction-emoji",-1917209784)], null)),knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"label","label",1718410804)], null))], null);
var collections = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"labels","labels",-626734591)], null)),knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reaction_emojis","reaction_emojis",-53169004),new cljs.core.Keyword(null,"reaction-emojis","reaction-emojis",-855354117)], null)),knoxx.backend.quality_labels.map_get_any(m,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"history","history",-247395220)], null))], null);
var nested = knoxx.backend.quality_labels.nested_label_maps(m);
return cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.quality_labels.value_evidence,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([direct], 0)),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.quality_labels.value_evidence,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([collections], 0)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.quality_labels.value_evidence,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([nested], 0))], 0));
});
knoxx.backend.quality_labels.substring_candidate_QMARK_ = (function knoxx$backend$quality_labels$substring_candidate_QMARK_(candidate){
return (((!(clojure.string.blank_QMARK_(candidate)))) && (cljs.core.not(cljs.core.re_matches(/[a-z]+/,candidate))));
});
knoxx.backend.quality_labels.token_matches_QMARK_ = (function knoxx$backend$quality_labels$token_matches_QMARK_(token,candidates){
var normalized = knoxx.backend.quality_labels.normalize_token(token);
return cljs.core.boolean$((function (){var or__5142__auto__ = cljs.core.contains_QMARK_(candidates,normalized);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return cljs.core.some((function (p1__517661_SHARP_){
return ((knoxx.backend.quality_labels.substring_candidate_QMARK_(p1__517661_SHARP_)) && (clojure.string.includes_QMARK_(normalized,p1__517661_SHARP_)));
}),candidates);
}
})());
});
/**
 * Return "good", "bad", or nil for a record/message/hit.
 * 
 * Bad dominates good so a crossed-out record is excluded even if it also has
 * a checkmark.
 */
knoxx.backend.quality_labels.quality_label = (function knoxx$backend$quality_labels$quality_label(record){
var tokens = knoxx.backend.quality_labels.quality_label_evidence(record);
if(cljs.core.truth_(cljs.core.some((function (p1__517671_SHARP_){
return knoxx.backend.quality_labels.token_matches_QMARK_(p1__517671_SHARP_,knoxx.backend.quality_labels.bad_reaction_tokens);
}),tokens))){
return "bad";
} else {
if(cljs.core.truth_(cljs.core.some((function (p1__517672_SHARP_){
return knoxx.backend.quality_labels.token_matches_QMARK_(p1__517672_SHARP_,knoxx.backend.quality_labels.good_reaction_tokens);
}),tokens))){
return "good";
} else {
return null;

}
}
});
knoxx.backend.quality_labels.good_QMARK_ = (function knoxx$backend$quality_labels$good_QMARK_(record){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("good",knoxx.backend.quality_labels.quality_label(record));
});
knoxx.backend.quality_labels.bad_QMARK_ = (function knoxx$backend$quality_labels$bad_QMARK_(record){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("bad",knoxx.backend.quality_labels.quality_label(record));
});
knoxx.backend.quality_labels.not_bad_QMARK_ = (function knoxx$backend$quality_labels$not_bad_QMARK_(record){
return (!(knoxx.backend.quality_labels.bad_QMARK_(record)));
});
knoxx.backend.quality_labels.drop_bad = (function knoxx$backend$quality_labels$drop_bad(records){
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.quality_labels.not_bad_QMARK_,(function (){var or__5142__auto__ = records;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
});
/**
 * Return checkmarked records first, then unlabeled/non-bad records.
 * Existing relative order within each group is preserved.
 */
knoxx.backend.quality_labels.good_first_then_not_bad = (function knoxx$backend$quality_labels$good_first_then_not_bad(records){
var records__$1 = knoxx.backend.quality_labels.drop_bad(records);
var good = cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.quality_labels.good_QMARK_,records__$1);
var rest = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(knoxx.backend.quality_labels.good_QMARK_,records__$1);
return cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(good,rest));
});

//# sourceMappingURL=knoxx.backend.quality_labels.js.map
