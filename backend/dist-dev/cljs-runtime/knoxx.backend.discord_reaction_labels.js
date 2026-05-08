import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.util.time.js";
goog.provide('knoxx.backend.discord_reaction_labels');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.discord_reaction_labels !== 'undefined') && (typeof knoxx.backend.discord_reaction_labels.reaction_unsubscribe_STAR_ !== 'undefined')){
} else {
knoxx.backend.discord_reaction_labels.reaction_unsubscribe_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.discord_reaction_labels.js_get = (function knoxx$backend$discord_reaction_labels$js_get(obj,key){
if(cljs.core.truth_(obj)){
return (obj[key]);
} else {
return null;
}
});
knoxx.backend.discord_reaction_labels.discord_record_id = (function knoxx$backend$discord_reaction_labels$discord_record_id(channel_id,message_id){
return (""+"discord:message:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id));
});
knoxx.backend.discord_reaction_labels.quality_from_emoji = (function knoxx$backend$discord_reaction_labels$quality_from_emoji(emoji){
var G__52702 = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(emoji)));
switch (G__52702) {
case "\u2705":
return "good";

break;
case "\u2611\uFE0F":
return "good";

break;
case "\u2714\uFE0F":
return "good";

break;
case "\u2714":
return "good";

break;
case "\u274C":
return "bad";

break;
case "\u2716\uFE0F":
return "bad";

break;
case "\u2716":
return "bad";

break;
case "\u274E":
return "bad";

break;
default:
return null;

}
});
knoxx.backend.discord_reaction_labels.message__GT_openplanner_event = (function knoxx$backend$discord_reaction_labels$message__GT_openplanner_event(config,message,emoji,user_id){
var channel_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"channelId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var message_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"id");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var author_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"authorId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var record_id = knoxx.backend.discord_reaction_labels.discord_record_id(channel_id,message_id);
var content = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"content");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var ts = (function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"timestamp");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.time.now_iso();
}
})();
var quality = knoxx.backend.discord_reaction_labels.quality_from_emoji(emoji);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"schema","schema",-1582001791),new cljs.core.Keyword(null,"meta","meta",1499536964),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"source_ref","source_ref",-1854699662),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"text","text",-1790561697)],["openplanner.event.v1",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"role","role",-736691072),"user",new cljs.core.Keyword(null,"author","author",2111686192),author_id,new cljs.core.Keyword(null,"tags","tags",1771418977),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord","message","reaction-corpus"], null)], null),(function (){var G__52706 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727),new cljs.core.Keyword(null,"reaction_ingested","reaction_ingested",1691225188),new cljs.core.Keyword(null,"author_id","author_id",1568127108),new cljs.core.Keyword(null,"reaction_emoji","reaction_emoji",-821316473),new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.Keyword(null,"reaction_user_id","reaction_user_id",1266745431),new cljs.core.Keyword(null,"author_is_bot","author_is_bot",-1802717545),new cljs.core.Keyword(null,"author_username","author_username",-1558121698)],[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"claim_system","claim_system",-320128383),"weak-reaction-v1",new cljs.core.Keyword(null,"reaction_emojis","reaction_emojis",-53169004),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [emoji], null),new cljs.core.Keyword(null,"labels","labels",-626734591),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(cljs.core.truth_(quality)?(""+"quality:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quality)):(""+"reaction:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(emoji)))], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso()], null),true,author_id,emoji,channel_id,message_id,user_id,cljs.core.boolean$(knoxx.backend.discord_reaction_labels.js_get(message,"authorIsBot")),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(message,"authorUsername");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})()))]);
var G__52706__$1 = (cljs.core.truth_(quality)?cljs.core.assoc_in(G__52706,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727),new cljs.core.Keyword(null,"quality","quality",147850199)], null),quality):G__52706);
var G__52706__$2 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(quality,"good"))?cljs.core.assoc_in(G__52706__$1,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727),new cljs.core.Keyword(null,"explicit_meaning","explicit_meaning",1062627523)], null),"good output"):G__52706__$1);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(quality,"bad")){
return cljs.core.assoc_in(G__52706__$2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727),new cljs.core.Keyword(null,"explicit_meaning","explicit_meaning",1062627523)], null),"bad output");
} else {
return G__52706__$2;
}
})(),"discord",ts,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"session","session",1008279103),channel_id,new cljs.core.Keyword(null,"message","message",-406056002),message_id], null),record_id,"discord.message",content]);
});
knoxx.backend.discord_reaction_labels.ingest_reaction_BANG_ = (function knoxx$backend$discord_reaction_labels$ingest_reaction_BANG_(config,reaction){
var message = knoxx.backend.discord_reaction_labels.js_get(reaction,"message");
var emoji = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(reaction,"emoji");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var user_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(reaction,"userId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var channel_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(reaction,"channelId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var message_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.discord_reaction_labels.js_get(reaction,"messageId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(((clojure.string.blank_QMARK_(emoji)) || (((clojure.string.blank_QMARK_(channel_id)) || (((clojure.string.blank_QMARK_(message_id)) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))))))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"skipped","skipped",-1144887090),true], null));
} else {
var record_id = knoxx.backend.discord_reaction_labels.discord_record_id(channel_id,message_id);
var event = knoxx.backend.discord_reaction_labels.message__GT_openplanner_event(config,message,emoji,user_id);
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/events",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [event], null)], null)).then((function (_){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST",(""+"/v1/labels/records/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(record_id))+"/reaction"),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"emoji","emoji",1031230144),emoji,new cljs.core.Keyword(null,"source","source",-433931539),"discord-gateway-reaction",new cljs.core.Keyword(null,"user_id","user_id",993497112),user_id], null));
})).catch((function (err){
console.warn("[discord-reaction-labels] failed to ingest reaction",err);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),err.message], null);
}));
}
});
knoxx.backend.discord_reaction_labels.bind_BANG_ = (function knoxx$backend$discord_reaction_labels$bind_BANG_(config){
var temp__5825__auto___52825 = cljs.core.deref(knoxx.backend.discord_reaction_labels.reaction_unsubscribe_STAR_);
if(cljs.core.truth_(temp__5825__auto___52825)){
var unsubscribe_52826 = temp__5825__auto___52825;
(unsubscribe_52826.cljs$core$IFn$_invoke$arity$0 ? unsubscribe_52826.cljs$core$IFn$_invoke$arity$0() : unsubscribe_52826.call(null));

cljs.core.reset_BANG_(knoxx.backend.discord_reaction_labels.reaction_unsubscribe_STAR_,null);
} else {
}

var temp__5825__auto__ = knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
if(cljs.core.fn_QMARK_((manager["onReaction"]))){
return cljs.core.reset_BANG_(knoxx.backend.discord_reaction_labels.reaction_unsubscribe_STAR_,manager.onReaction((function (mapped,_raw_reaction,_user){
return knoxx.backend.discord_reaction_labels.ingest_reaction_BANG_(config,mapped);
})));
} else {
return null;
}
} else {
return null;
}
});

//# sourceMappingURL=knoxx.backend.discord_reaction_labels.js.map
