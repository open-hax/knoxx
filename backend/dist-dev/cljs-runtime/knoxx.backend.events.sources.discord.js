import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.quality_labels.js";
goog.provide('knoxx.backend.events.sources.discord');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.events !== 'undefined') && (typeof knoxx.backend.events.sources !== 'undefined') && (typeof knoxx.backend.events.sources.discord !== 'undefined') && (typeof knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_ !== 'undefined')){
} else {
knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.events.sources.discord.bot_token = (function knoxx$backend$events$sources$discord$bot_token(config){
return new cljs.core.Keyword(null,"discord-bot-token","discord-bot-token",1224757550).cljs$core$IFn$_invoke$arity$1(config);
});
knoxx.backend.events.sources.discord.manager_active_QMARK_ = (function knoxx$backend$events$sources$discord$manager_active_QMARK_(manager){
if(cljs.core.truth_(manager)){
var status = manager.status();
return cljs.core.boolean$((function (){var or__5142__auto__ = (status["ready"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (status["started"]);
}
})());
} else {
return null;
}
});
knoxx.backend.events.sources.discord.active_gateway_entries = (function knoxx$backend$events$sources$discord$active_gateway_entries(){
var actor_entries = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__51642){
var vec__51643 = p__51642;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51643,(0),null);
var manager = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51643,(1),null);
return knoxx.backend.events.sources.discord.manager_active_QMARK_(manager);
}),knoxx.backend.discord_gateway.gateway_managers()));
if(cljs.core.seq(actor_entries)){
return actor_entries;
} else {
var temp__5825__auto__ = knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
if(cljs.core.truth_(knoxx.backend.events.sources.discord.manager_active_QMARK_(manager))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,manager], null)], null);
} else {
return null;
}
} else {
return null;
}
}
});
knoxx.backend.events.sources.discord.active_QMARK_ = (function knoxx$backend$events$sources$discord$active_QMARK_(){
return cljs.core.boolean$(cljs.core.seq(knoxx.backend.events.sources.discord.active_gateway_entries()));
});
knoxx.backend.events.sources.discord.gateway_user_id = (function knoxx$backend$events$sources$discord$gateway_user_id(){
var or__5142__auto__ = cljs.core.some((function (p__51684){
var vec__51685 = p__51684;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51685,(0),null);
var manager = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51685,(1),null);
var status = manager.status();
var G__51692 = (status["userId"]);
var G__51692__$1 = (((G__51692 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51692)));
var G__51692__$2 = (((G__51692__$1 == null))?null:clojure.string.trim(G__51692__$1));
if((G__51692__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51692__$2);
}
}),knoxx.backend.events.sources.discord.active_gateway_entries());
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
var status = manager.status();
var G__51731 = (status["userId"]);
var G__51731__$1 = (((G__51731 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51731)));
var G__51731__$2 = (((G__51731__$1 == null))?null:clojure.string.trim(G__51731__$1));
if((G__51731__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51731__$2);
}
} else {
return null;
}
}
});
knoxx.backend.events.sources.discord.discord_headers = (function knoxx$backend$events$sources$discord$discord_headers(token){
return ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)), "Content-Type": "application/json"});
});
knoxx.backend.events.sources.discord.fetch_json_BANG_ = (function knoxx$backend$events$sources$discord$fetch_json_BANG_(url,options){
return fetch(url,options).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.events.sources.discord.map_message = (function knoxx$backend$events$sources$discord$map_message(msg){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"embeds","embeds",833349080),new cljs.core.Keyword(null,"timestamp","timestamp",579478971)],[(function (){var or__5142__auto__ = (msg["author"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (msg["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (msg["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (attachment){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(function (){var or__5142__auto__ = (attachment["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"filename","filename",-1428840783),(function (){var or__5142__auto__ = (attachment["filename"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"contentType","contentType",-1462509576),(function (){var or__5142__auto__ = (attachment["content_type"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (attachment["contentType"]);
}
})(),new cljs.core.Keyword(null,"size","size",1098693007),(function (){var or__5142__auto__ = (attachment["size"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"url","url",276297046),(function (){var or__5142__auto__ = (attachment["url"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((msg["attachments"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((msg["attachments"])):cljs.core.PersistentVector.EMPTY)),(function (){var or__5142__auto__ = (msg["author"]["username"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})(),cljs.core.boolean$((msg["author"]["bot"])),(msg["id"]),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (embed){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"title","title",636505583),(function (){var or__5142__auto__ = (embed["title"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = (embed["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),new cljs.core.Keyword(null,"url","url",276297046),(function (){var or__5142__auto__ = (embed["url"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((msg["embeds"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((msg["embeds"])):cljs.core.PersistentVector.EMPTY)),(function (){var or__5142__auto__ = (msg["timestamp"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()]);
});
knoxx.backend.events.sources.discord.sort_newest_first = (function knoxx$backend$events$sources$discord$sort_newest_first(messages){
return cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(function (p1__51803_SHARP_,p2__51802_SHARP_){
return cljs.core.compare(p2__51802_SHARP_,p1__51803_SHARP_);
}),messages);
});
knoxx.backend.events.sources.discord.record_id = (function knoxx$backend$events$sources$discord$record_id(message){
return (""+"discord:message:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(message))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message)));
});
knoxx.backend.events.sources.discord.label_for_record_id = (function knoxx$backend$events$sources$discord$label_for_record_id(labels,rid){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(labels,rid);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(labels,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(rid));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
});
knoxx.backend.events.sources.discord.attach_openplanner_labels_BANG_ = (function knoxx$backend$events$sources$discord$attach_openplanner_labels_BANG_(config,messages){
if(((cljs.core.empty_QMARK_(messages)) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))){
return Promise.resolve(knoxx.backend.quality_labels.good_first_then_not_bad(messages));
} else {
var ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.events.sources.discord.record_id,messages);
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/labels/records/lookup",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ids","ids",-998535796),ids], null)).then((function (response){
var labels = new cljs.core.Keyword(null,"labels","labels",-626734591).cljs$core$IFn$_invoke$arity$1(response);
return knoxx.backend.quality_labels.good_first_then_not_bad(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (message){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(message,new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291),knoxx.backend.events.sources.discord.label_for_record_id(labels,knoxx.backend.events.sources.discord.record_id(message)));
}),messages));
})).catch((function (error){
console.warn("[event-agents.discord] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad Discord context",error);

return cljs.core.PersistentVector.EMPTY;
}));
}
});
knoxx.backend.events.sources.discord.fetch_channel_from_gateway_entries_BANG_ = (function knoxx$backend$events$sources$discord$fetch_channel_from_gateway_entries_BANG_(entries,channel_id,opts){
var temp__5823__auto__ = cljs.core.first(entries);
if(cljs.core.truth_(temp__5823__auto__)){
var vec__51832 = temp__5823__auto__;
var _actor_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51832,(0),null);
var manager = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51832,(1),null);
return manager.fetchChannelMessages(channel_id,opts).catch((function (_){
var G__51835 = cljs.core.rest(entries);
var G__51836 = channel_id;
var G__51837 = opts;
return (knoxx.backend.events.sources.discord.fetch_channel_from_gateway_entries_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.events.sources.discord.fetch_channel_from_gateway_entries_BANG_.cljs$core$IFn$_invoke$arity$3(G__51835,G__51836,G__51837) : knoxx.backend.events.sources.discord.fetch_channel_from_gateway_entries_BANG_.call(null,G__51835,G__51836,G__51837));
}));
} else {
return Promise.reject((new Error((""+"No active Discord actor gateway can read channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)))));
}
});
knoxx.backend.events.sources.discord.read_channel_BANG_ = (function knoxx$backend$events$sources$discord$read_channel_BANG_(config,channel_id,limit){
var temp__5823__auto__ = cljs.core.seq(knoxx.backend.events.sources.discord.active_gateway_entries());
if(temp__5823__auto__){
var entries = temp__5823__auto__;
return knoxx.backend.events.sources.discord.fetch_channel_from_gateway_entries_BANG_(entries,channel_id,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"limit","limit",-1355822363),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (25);
}
})()))], null))).then((function (messages){
return cljs.core.vec(knoxx.backend.events.sources.discord.sort_newest_first(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(messages,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))));
})).then((function (messages){
return knoxx.backend.events.sources.discord.attach_openplanner_labels_BANG_(config,messages);
}));
} else {
var token = knoxx.backend.events.sources.discord.bot_token(config);
if(clojure.string.blank_QMARK_(token)){
return Promise.reject((new Error("Discord bot token not configured")));
} else {
return knoxx.backend.events.sources.discord.fetch_json_BANG_((""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages?limit="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (25);
}
})())))),({"method": "GET", "headers": knoxx.backend.events.sources.discord.discord_headers(token)})).then((function (payload){
return cljs.core.vec(knoxx.backend.events.sources.discord.sort_newest_first(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.events.sources.discord.map_message,(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY))));
})).then((function (messages){
return knoxx.backend.events.sources.discord.attach_openplanner_labels_BANG_(config,messages);
}));
}
}
});
knoxx.backend.events.sources.discord.list_channels_BANG_ = (function knoxx$backend$events$sources$discord$list_channels_BANG_(){
var entries = knoxx.backend.events.sources.discord.active_gateway_entries();
if(cljs.core.seq(entries)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__51866){
var vec__51867 = p__51866;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51867,(0),null);
var manager = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51867,(1),null);
return manager.listChannels(null).catch((function (___$1){
return [];
}));
}),entries))).then((function (results){
return cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.first,cljs.core.vals(cljs.core.group_by(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__51862_SHARP_){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(p1__51862_SHARP_,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__51861_SHARP_){
return Array.from(p1__51861_SHARP_);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([Array.from(results)], 0)))))));
}));
} else {
var or__5142__auto__ = knoxx.backend.discord_gateway.list_channels.cljs$core$IFn$_invoke$arity$0();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return Promise.resolve([]);
}
}
});
knoxx.backend.events.sources.discord.resolve_channel_ids_BANG_ = (function knoxx$backend$events$sources$discord$resolve_channel_ids_BANG_(p__51887){
var map__51889 = p__51887;
var map__51889__$1 = cljs.core.__destructure_map(map__51889);
var explicit_channels = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51889__$1,new cljs.core.Keyword(null,"explicit-channels","explicit-channels",1984056567));
var guild_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51889__$1,new cljs.core.Keyword(null,"guild-ids","guild-ids",914840032));
var publish_channels = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51889__$1,new cljs.core.Keyword(null,"publish-channels","publish-channels",1648651117));
if(cljs.core.seq(guild_ids)){
return knoxx.backend.events.sources.discord.list_channels_BANG_().then((function (channels){
var rows = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(channels,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var guild_id_set = cljs.core.set(guild_ids);
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (channel){
return cljs.core.contains_QMARK_(guild_id_set,new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel));
}),rows))));
}));
} else {
if(((cljs.core.empty_QMARK_(explicit_channels)) && (cljs.core.seq(publish_channels)))){
return knoxx.backend.events.sources.discord.list_channels_BANG_().then((function (channels){
var rows = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(channels,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var publish_channel_set = cljs.core.set(publish_channels);
var guilds = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"guildId","guildId",-559818490),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (channel){
return cljs.core.contains_QMARK_(publish_channel_set,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel));
}),rows))));
if(cljs.core.seq(guilds)){
var guild_set = cljs.core.set(guilds);
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (channel){
return cljs.core.contains_QMARK_(guild_set,new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel));
}),rows))));
} else {
return publish_channels;
}
}));
} else {
return Promise.resolve(explicit_channels);

}
}
});
knoxx.backend.events.sources.discord.message_match_kind = (function knoxx$backend$events$sources$discord$message_match_kind(p__51965){
var map__51966 = p__51965;
var map__51966__$1 = cljs.core.__destructure_map(map__51966);
var bot_user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51966__$1,new cljs.core.Keyword(null,"bot-user-id","bot-user-id",1703682999));
var content = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51966__$1,new cljs.core.Keyword(null,"content","content",15833224));
var keyword_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51966__$1,new cljs.core.Keyword(null,"keyword?","keyword?",277265542));
var created_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51966__$1,new cljs.core.Keyword(null,"created?","created?",850508195));
var match_all_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51966__$1,new cljs.core.Keyword(null,"match-all?","match-all?",-1394896600));
var text = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content)+""));
var mention_QMARK_ = (function (){var and__5140__auto__ = bot_user_id;
if(cljs.core.truth_(and__5140__auto__)){
return ((clojure.string.includes_QMARK_(text,(""+"<@"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bot_user_id)+">"))) || (clojure.string.includes_QMARK_(text,(""+"<@!"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bot_user_id)+">"))));
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(mention_QMARK_)){
return "discord.message.mention";
} else {
if(cljs.core.truth_(keyword_QMARK_)){
return "discord.message.keyword";
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = created_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return match_all_QMARK_;
} else {
return and__5140__auto__;
}
})())){
return "discord.message.created";
} else {
return null;

}
}
}
});
knoxx.backend.events.sources.discord.execute_patrol_BANG_ = (function knoxx$backend$events$sources$discord$execute_patrol_BANG_(p__51972){
var map__51973 = p__51972;
var map__51973__$1 = cljs.core.__destructure_map(map__51973);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"config","config",994861415));
var channel_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var unseen_messages = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"unseen-messages","unseen-messages",-938745644));
var remember_latest_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"remember-latest!","remember-latest!",-903750029));
var match_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"match-kind","match-kind",83311455));
var dispatch_message_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51973__$1,new cljs.core.Keyword(null,"dispatch-message!","dispatch-message!",-1354158307));
if(knoxx.backend.events.sources.discord.active_QMARK_()){
return Promise.resolve(null);
} else {
if(cljs.core.seq(channel_ids)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (channel_id){
return knoxx.backend.events.sources.discord.read_channel_BANG_(config,channel_id,limit).then((function (messages){
var fresh = (unseen_messages.cljs$core$IFn$_invoke$arity$2 ? unseen_messages.cljs$core$IFn$_invoke$arity$2(channel_id,messages) : unseen_messages.call(null,channel_id,messages));
var seq__51979_52546 = cljs.core.seq(fresh);
var chunk__51980_52547 = null;
var count__51981_52548 = (0);
var i__51982_52549 = (0);
while(true){
if((i__51982_52549 < count__51981_52548)){
var message_52550 = chunk__51980_52547.cljs$core$IIndexed$_nth$arity$2(null,i__51982_52549);
var temp__5825__auto___52551 = (match_kind.cljs$core$IFn$_invoke$arity$1 ? match_kind.cljs$core$IFn$_invoke$arity$1(message_52550) : match_kind.call(null,message_52550));
if(cljs.core.truth_(temp__5825__auto___52551)){
var kind_52552 = temp__5825__auto___52551;
(dispatch_message_BANG_.cljs$core$IFn$_invoke$arity$2 ? dispatch_message_BANG_.cljs$core$IFn$_invoke$arity$2(message_52550,kind_52552) : dispatch_message_BANG_.call(null,message_52550,kind_52552));
} else {
}


var G__52553 = seq__51979_52546;
var G__52554 = chunk__51980_52547;
var G__52555 = count__51981_52548;
var G__52556 = (i__51982_52549 + (1));
seq__51979_52546 = G__52553;
chunk__51980_52547 = G__52554;
count__51981_52548 = G__52555;
i__51982_52549 = G__52556;
continue;
} else {
var temp__5825__auto___52569 = cljs.core.seq(seq__51979_52546);
if(temp__5825__auto___52569){
var seq__51979_52570__$1 = temp__5825__auto___52569;
if(cljs.core.chunked_seq_QMARK_(seq__51979_52570__$1)){
var c__5673__auto___52571 = cljs.core.chunk_first(seq__51979_52570__$1);
var G__52572 = cljs.core.chunk_rest(seq__51979_52570__$1);
var G__52573 = c__5673__auto___52571;
var G__52574 = cljs.core.count(c__5673__auto___52571);
var G__52575 = (0);
seq__51979_52546 = G__52572;
chunk__51980_52547 = G__52573;
count__51981_52548 = G__52574;
i__51982_52549 = G__52575;
continue;
} else {
var message_52579 = cljs.core.first(seq__51979_52570__$1);
var temp__5825__auto___52580__$1 = (match_kind.cljs$core$IFn$_invoke$arity$1 ? match_kind.cljs$core$IFn$_invoke$arity$1(message_52579) : match_kind.call(null,message_52579));
if(cljs.core.truth_(temp__5825__auto___52580__$1)){
var kind_52581 = temp__5825__auto___52580__$1;
(dispatch_message_BANG_.cljs$core$IFn$_invoke$arity$2 ? dispatch_message_BANG_.cljs$core$IFn$_invoke$arity$2(message_52579,kind_52581) : dispatch_message_BANG_.call(null,message_52579,kind_52581));
} else {
}


var G__52587 = cljs.core.next(seq__51979_52570__$1);
var G__52588 = null;
var G__52589 = (0);
var G__52590 = (0);
seq__51979_52546 = G__52587;
chunk__51980_52547 = G__52588;
count__51981_52548 = G__52589;
i__51982_52549 = G__52590;
continue;
}
} else {
}
}
break;
}

(remember_latest_BANG_.cljs$core$IFn$_invoke$arity$2 ? remember_latest_BANG_.cljs$core$IFn$_invoke$arity$2(channel_id,messages) : remember_latest_BANG_.call(null,channel_id,messages));

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"fetched","fetched",1610343604),cljs.core.count(messages),new cljs.core.Keyword(null,"fresh","fresh",-1182453442),cljs.core.count(fresh)], null);
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents.discord] patrol failed for",channel_id,":",err.message], 0));

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"error","error",-978969032),true], null);
}));
}),channel_ids)));
} else {
return Promise.resolve(null);
}
}
});
knoxx.backend.events.sources.discord.summarize_channel = (function knoxx$backend$events$sources$discord$summarize_channel(channel_id,messages){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (message){
var attachments = new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(message);
var attachment_text = ((cljs.core.seq(attachments))?(""+" attachments="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (a){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(a))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(a))?(""+" <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(a))+">"):null)));
}),attachments)))):null);
return (""+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"] <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message))+" (id:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message))+")> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message),(0),cljs.core.min.cljs$core$IFn$_invoke$arity$2((180),cljs.core.count(new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message)))))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = attachment_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}),cljs.core.take.cljs$core$IFn$_invoke$arity$2((8),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121),messages))));
});
knoxx.backend.events.sources.discord.image_attachments = (function knoxx$backend$events$sources$discord$image_attachments(rows){
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((8),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (attachment){
var G__52085 = new cljs.core.Keyword(null,"contentType","contentType",-1462509576).cljs$core$IFn$_invoke$arity$1(attachment);
var G__52085__$1 = (((G__52085 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52085)));
var G__52085__$2 = (((G__52085__$1 == null))?null:clojure.string.lower_case(G__52085__$1));
if((G__52085__$2 == null)){
return null;
} else {
return clojure.string.starts_with_QMARK_(G__52085__$2,"image/");
}
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"url","url",276297046),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p__52130){
var map__52134 = p__52130;
var map__52134__$1 = cljs.core.__destructure_map(map__52134);
var messages = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52134__$1,new cljs.core.Keyword(null,"messages","messages",345434482));
return cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(new cljs.core.Keyword(null,"attachments","attachments",-1535547830),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([messages], 0));
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([rows], 0))))));
});
knoxx.backend.events.sources.discord.execute_synthesis_BANG_ = (function knoxx$backend$events$sources$discord$execute_synthesis_BANG_(p__52189){
var map__52190 = p__52189;
var map__52190__$1 = cljs.core.__destructure_map(map__52190);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52190__$1,new cljs.core.Keyword(null,"config","config",994861415));
var channel_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52190__$1,new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52190__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var dispatch_summary_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52190__$1,new cljs.core.Keyword(null,"dispatch-summary!","dispatch-summary!",455883021));
if(cljs.core.not(cljs.core.seq(channel_ids))){
return Promise.resolve(null);
} else {
var fetch_row_BANG_ = (function (channel_id){
return knoxx.backend.events.sources.discord.read_channel_BANG_(config,channel_id,limit).then((function (messages){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"messages","messages",345434482),messages], null);
})).catch((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"messages","messages",345434482),cljs.core.PersistentVector.EMPTY], null);
}));
});
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(fetch_row_BANG_,channel_ids))).then((function (results){
var G__52319 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return (dispatch_summary_BANG_.cljs$core$IFn$_invoke$arity$1 ? dispatch_summary_BANG_.cljs$core$IFn$_invoke$arity$1(G__52319) : dispatch_summary_BANG_.call(null,G__52319));
}));
}
});
knoxx.backend.events.sources.discord.bind_gateways_BANG_ = (function knoxx$backend$events$sources$discord$bind_gateways_BANG_(p__52323){
var map__52325 = p__52323;
var map__52325__$1 = cljs.core.__destructure_map(map__52325);
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52325__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var on_message_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52325__$1,new cljs.core.Keyword(null,"on-message!","on-message!",-607128138));
var on_voice_state_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52325__$1,new cljs.core.Keyword(null,"on-voice-state!","on-voice-state!",2001605581));
var temp__5825__auto___52610 = cljs.core.deref(knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_);
if(cljs.core.truth_(temp__5825__auto___52610)){
var unsubscribe_52611 = temp__5825__auto___52610;
(unsubscribe_52611.cljs$core$IFn$_invoke$arity$0 ? unsubscribe_52611.cljs$core$IFn$_invoke$arity$0() : unsubscribe_52611.call(null));

cljs.core.reset_BANG_(knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_,null);
} else {
}

if(cljs.core.truth_(policy_db)){
return policy_db.listActorCredentials("discord_bot").then((function (result){
return knoxx.backend.discord_gateway.start_actor_gateways_BANG_((function (){var or__5142__auto__ = (result["credentials"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
})).then((function (_started){
var unsubscribes = cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p__52358){
var vec__52361 = p__52358;
var actor_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52361,(0),null);
var manager = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52361,(1),null);
var status = manager.status();
var bot_user_id = (function (){var G__52366 = (status["userId"]);
var G__52366__$1 = (((G__52366 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52366)));
var G__52366__$2 = (((G__52366__$1 == null))?null:clojure.string.trim(G__52366__$1));
if((G__52366__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__52366__$2);
}
})();
var msg_unsub = manager.onMessage((function (mapped,_raw){
var G__52377 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(mapped,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533),actor_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696),bot_user_id], 0));
return (on_message_BANG_.cljs$core$IFn$_invoke$arity$1 ? on_message_BANG_.cljs$core$IFn$_invoke$arity$1(G__52377) : on_message_BANG_.call(null,G__52377));
}));
var voice_unsub = manager.onVoiceStateUpdate((function (mapped,_old,_new){
var G__52385 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(mapped,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533),actor_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696),bot_user_id], 0));
return (on_voice_state_BANG_.cljs$core$IFn$_invoke$arity$1 ? on_voice_state_BANG_.cljs$core$IFn$_invoke$arity$1(G__52385) : on_voice_state_BANG_.call(null,G__52385));
}));
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [msg_unsub,voice_unsub], null);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.discord_gateway.gateway_managers()], 0)));
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] bound",(cljs.core.count(unsubscribes) / (2)),"Discord actor gateway(s)"], 0));

return cljs.core.reset_BANG_(knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_,(function (){
var seq__52391 = cljs.core.seq(unsubscribes);
var chunk__52392 = null;
var count__52393 = (0);
var i__52394 = (0);
while(true){
if((i__52394 < count__52393)){
var unsubscribe = chunk__52392.cljs$core$IIndexed$_nth$arity$2(null,i__52394);
try{(unsubscribe.cljs$core$IFn$_invoke$arity$0 ? unsubscribe.cljs$core$IFn$_invoke$arity$0() : unsubscribe.call(null));
}catch (e52412){if((e52412 instanceof Error)){
var __52615 = e52412;
} else {
throw e52412;

}
}

var G__52616 = seq__52391;
var G__52617 = chunk__52392;
var G__52618 = count__52393;
var G__52619 = (i__52394 + (1));
seq__52391 = G__52616;
chunk__52392 = G__52617;
count__52393 = G__52618;
i__52394 = G__52619;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__52391);
if(temp__5825__auto__){
var seq__52391__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__52391__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__52391__$1);
var G__52621 = cljs.core.chunk_rest(seq__52391__$1);
var G__52622 = c__5673__auto__;
var G__52623 = cljs.core.count(c__5673__auto__);
var G__52624 = (0);
seq__52391 = G__52621;
chunk__52392 = G__52622;
count__52393 = G__52623;
i__52394 = G__52624;
continue;
} else {
var unsubscribe = cljs.core.first(seq__52391__$1);
try{(unsubscribe.cljs$core$IFn$_invoke$arity$0 ? unsubscribe.cljs$core$IFn$_invoke$arity$0() : unsubscribe.call(null));
}catch (e52421){if((e52421 instanceof Error)){
var __52625 = e52421;
} else {
throw e52421;

}
}

var G__52626 = cljs.core.next(seq__52391__$1);
var G__52627 = null;
var G__52628 = (0);
var G__52629 = (0);
seq__52391 = G__52626;
chunk__52392 = G__52627;
count__52393 = G__52628;
i__52394 = G__52629;
continue;
}
} else {
return null;
}
}
break;
}
}));
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] discord actor gateway bind failed:",err.message], 0));

return null;
}));
} else {
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] policy DB unavailable; Discord actor gateways not bound"], 0));
}
});
knoxx.backend.events.sources.discord.stop_BANG_ = (function knoxx$backend$events$sources$discord$stop_BANG_(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var unsubscribe = temp__5825__auto__;
(unsubscribe.cljs$core$IFn$_invoke$arity$0 ? unsubscribe.cljs$core$IFn$_invoke$arity$0() : unsubscribe.call(null));

return cljs.core.reset_BANG_(knoxx.backend.events.sources.discord.gateway_unsubscribe_STAR_,null);
} else {
return null;
}
});

//# sourceMappingURL=knoxx.backend.events.sources.discord.js.map
