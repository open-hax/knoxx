import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.routes.studio.discord_scan');
knoxx.backend.routes.studio.discord_scan.discord_scan_audio_extensions = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 12, [".flac",null,".mp4",null,".aiff",null,".webm",null,".mp3",null,".ogg",null,".m4a",null,".wma",null,".aif",null,".wav",null,".aac",null,".opus",null], null), null);
knoxx.backend.routes.studio.discord_scan.discord_scan_image_extensions = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 9, [".bmp",null,".svg",null,".webp",null,".png",null,".tiff",null,".tif",null,".jpg",null,".gif",null,".jpeg",null], null), null);
knoxx.backend.routes.studio.discord_scan.discord_bot_token = (function knoxx$backend$routes$studio$discord_scan$discord_bot_token(config){
var G__52357 = knoxx.backend.tools.shared.live_config(config);
var G__52357__$1 = (((G__52357 == null))?null:new cljs.core.Keyword(null,"discord-bot-token","discord-bot-token",1224757550).cljs$core$IFn$_invoke$arity$1(G__52357));
var G__52357__$2 = (((G__52357__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52357__$1)));
var G__52357__$3 = (((G__52357__$2 == null))?null:clojure.string.trim(G__52357__$2));
if((G__52357__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__52357__$3);
}
});
knoxx.backend.routes.studio.discord_scan.sleep_BANG_ = (function knoxx$backend$routes$studio$discord_scan$sleep_BANG_(ms){
return (new Promise((function (resolve,_reject){
return setTimeout(resolve,ms);
})));
});
knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_ = (function knoxx$backend$routes$studio$discord_scan$discord_fetch_json_BANG_(var_args){
var G__52360 = arguments.length;
switch (G__52360) {
case 2:
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (url,token){
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$3(url,token,(0));
}));

(knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (url,token,attempt){
return fetch(url,({"method": "GET", "headers": ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)), "Accept": "application/json", "User-Agent": "Knoxx-Studio/1.0"})})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((429),resp.status)) && ((attempt < (3))))){
var retry_after_ms = (function (){try{var parsed = JSON.parse(text);
var retry_after = (function (){var or__5142__auto__ = (parsed["retry_after"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
return cljs.core.long$(Math.ceil(((1000) * retry_after)));
}catch (e52378){var _ = e52378;
return (1000);
}})();
return knoxx.backend.routes.studio.discord_scan.sleep_BANG_(retry_after_ms).then((function (){
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$3(url,token,(attempt + (1)));
}));
} else {
throw (new Error((""+"Discord API error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}
}));
}
}));
}));

(knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.routes.studio.discord_scan.safe_path_segment = (function knoxx$backend$routes$studio$discord_scan$safe_path_segment(value){
var cleaned = (function (){var G__52380 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var G__52380__$1 = (((G__52380 == null))?null:clojure.string.trim(G__52380));
var G__52380__$2 = (((G__52380__$1 == null))?null:clojure.string.replace(G__52380__$1,/[^A-Za-z0-9._-]+/,"_"));
var G__52380__$3 = (((G__52380__$2 == null))?null:clojure.string.replace(G__52380__$2,/_+/,"_"));
var G__52380__$4 = (((G__52380__$3 == null))?null:clojure.string.replace(G__52380__$3,/^_+|_+$/,""));
if((G__52380__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__52380__$4);
}
})();
var or__5142__auto__ = cleaned;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
});
knoxx.backend.routes.studio.discord_scan.timestamp_token = (function knoxx$backend$routes$studio$discord_scan$timestamp_token(value){
var cleaned = (function (){var G__52386 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var G__52386__$1 = (((G__52386 == null))?null:clojure.string.replace(G__52386,/[^0-9T]+/,""));
if((G__52386__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__52386__$1);
}
})();
var or__5142__auto__ = cleaned;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-time";
}
});
knoxx.backend.routes.studio.discord_scan.timestamp_ms = (function knoxx$backend$routes$studio$discord_scan$timestamp_ms(value){
var ms = Date.parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(cljs.core.truth_(isNaN(ms))){
return null;
} else {
return ms;
}
});
knoxx.backend.routes.studio.discord_scan.recent_enough_QMARK_ = (function knoxx$backend$routes$studio$discord_scan$recent_enough_QMARK_(cutoff_ms,message){
var temp__5823__auto__ = knoxx.backend.routes.studio.discord_scan.timestamp_ms(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(message));
if(cljs.core.truth_(temp__5823__auto__)){
var ms = temp__5823__auto__;
return (ms >= cutoff_ms);
} else {
return true;
}
});
knoxx.backend.routes.studio.discord_scan.discord_audio_attachment_QMARK_ = (function knoxx$backend$routes$studio$discord_scan$discord_audio_attachment_QMARK_(attachment){
var filename = (function (){var G__52402 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment);
var G__52402__$1 = (((G__52402 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52402)));
if((G__52402__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52402__$1);
}
})();
var ext = (cljs.core.truth_(filename)?(function (){var G__52407 = cljs.core.re_find(/\.[^.]+$/,filename);
if((G__52407 == null)){
return null;
} else {
return clojure.string.lower_case(G__52407);
}
})():null);
var content_type = (function (){var G__52409 = new cljs.core.Keyword(null,"contentType","contentType",-1462509576).cljs$core$IFn$_invoke$arity$1(attachment);
var G__52409__$1 = (((G__52409 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52409)));
if((G__52409__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52409__$1);
}
})();
var or__5142__auto__ = (function (){var G__52410 = content_type;
if((G__52410 == null)){
return null;
} else {
return clojure.string.starts_with_QMARK_(G__52410,"audio/");
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.contains_QMARK_(knoxx.backend.routes.studio.discord_scan.discord_scan_audio_extensions,ext);
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
var and__5140__auto__ = (function (){var G__52411 = content_type;
if((G__52411 == null)){
return null;
} else {
return clojure.string.starts_with_QMARK_(G__52411,"video/");
}
})();
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [".mp4",null,".webm",null], null), null),ext);
} else {
return and__5140__auto__;
}
}
}
});
knoxx.backend.routes.studio.discord_scan.discord_image_attachment_QMARK_ = (function knoxx$backend$routes$studio$discord_scan$discord_image_attachment_QMARK_(attachment){
var filename = (function (){var G__52414 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment);
var G__52414__$1 = (((G__52414 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52414)));
if((G__52414__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52414__$1);
}
})();
var ext = (cljs.core.truth_(filename)?(function (){var G__52419 = cljs.core.re_find(/\.[^.]+$/,filename);
if((G__52419 == null)){
return null;
} else {
return clojure.string.lower_case(G__52419);
}
})():null);
var content_type = (function (){var G__52420 = new cljs.core.Keyword(null,"contentType","contentType",-1462509576).cljs$core$IFn$_invoke$arity$1(attachment);
var G__52420__$1 = (((G__52420 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52420)));
if((G__52420__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52420__$1);
}
})();
var or__5142__auto__ = (function (){var G__52422 = content_type;
if((G__52422 == null)){
return null;
} else {
return clojure.string.starts_with_QMARK_(G__52422,"image/");
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.contains_QMARK_(knoxx.backend.routes.studio.discord_scan.discord_scan_image_extensions,ext);
}
});
knoxx.backend.routes.studio.discord_scan.discord_list_guilds_BANG_ = (function knoxx$backend$routes$studio$discord_scan$discord_list_guilds_BANG_(token){
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2("https://discord.com/api/v10/users/@me/guilds",token).then((function (payload){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (guild){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"id","id",-1388402092),(function (){var or__5142__auto__ = (guild["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = (guild["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-guild";
}
})()], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY));
}));
});
knoxx.backend.routes.studio.discord_scan.discord_list_channels_BANG_ = (function knoxx$backend$routes$studio$discord_scan$discord_list_channels_BANG_(token,guild){
return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2((""+"https://discord.com/api/v10/guilds/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(guild))+"/channels"),token).then((function (payload){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (channel){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),(function (){var or__5142__auto__ = (channel["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = (channel["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-channel";
}
})(),new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(guild),new cljs.core.Keyword(null,"guildName","guildName",119399715),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(guild)], null);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (channel){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [(0),null,(12),null,(11),null,(5),null], null), null),(channel["type"]));
}),(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY)));
}));
});
knoxx.backend.routes.studio.discord_scan.discord_fetch_channel_messages_BANG_ = (function knoxx$backend$routes$studio$discord_scan$discord_fetch_channel_messages_BANG_(token,channel_id,before,limit){
var params = (new URLSearchParams());
params.set("limit",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (100);
}
})())))));

if((!((before == null)))){
params.set("before",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(before)));
} else {
}

return knoxx.backend.routes.studio.discord_scan.discord_fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2((""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(params.toString())),token).then((function (payload){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (message){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"id","id",-1388402092),(function (){var or__5142__auto__ = (message["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"channelId","channelId",2082229448),(function (){var or__5142__auto__ = (message["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return channel_id;
}
})(),new cljs.core.Keyword(null,"content","content",15833224),(function (){var or__5142__auto__ = (message["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"authorId","authorId",-1664154012),(function (){var or__5142__auto__ = (message["author"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),(function (){var or__5142__auto__ = (message["author"]["username"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})(),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(function (){var or__5142__auto__ = (message["timestamp"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (attachment){
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
return "attachment.bin";
}
})(),new cljs.core.Keyword(null,"contentType","contentType",-1462509576),(function (){var or__5142__auto__ = (attachment["content_type"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
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
}),(cljs.core.truth_(cljs.core.array_QMARK_((message["attachments"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((message["attachments"])):cljs.core.PersistentVector.EMPTY))], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY));
}));
});
knoxx.backend.routes.studio.discord_scan.promise_reduce = (function knoxx$backend$routes$studio$discord_scan$promise_reduce(items,init,step_fn){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (promise,item){
return promise.then((function (state){
return (step_fn.cljs$core$IFn$_invoke$arity$2 ? step_fn.cljs$core$IFn$_invoke$arity$2(state,item) : step_fn.call(null,state,item));
}));
}),Promise.resolve(init),items);
});
knoxx.backend.routes.studio.discord_scan.collect_discord_scan_channels_BANG_ = (function knoxx$backend$routes$studio$discord_scan$collect_discord_scan_channels_BANG_(token,channel_ids,max_channels){
if(cljs.core.seq(channel_ids)){
return Promise.resolve(cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (channel_id){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)),new cljs.core.Keyword(null,"name","name",1843675177),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)),new cljs.core.Keyword(null,"guildId","guildId",-559818490),null,new cljs.core.Keyword(null,"guildName","guildName",119399715),"manual"], null);
}),channel_ids)));
} else {
return knoxx.backend.routes.studio.discord_scan.discord_list_guilds_BANG_(token).then((function (guilds){
return knoxx.backend.routes.studio.discord_scan.promise_reduce(guilds,cljs.core.PersistentVector.EMPTY,(function (acc,guild){
return knoxx.backend.routes.studio.discord_scan.discord_list_channels_BANG_(token,guild).then((function (channels){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(acc,channels);
}));
})).then((function (channels){
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(max_channels,channels));
}));
}));
}
});
knoxx.backend.routes.studio.discord_scan.scan_channel_audio_BANG_ = (function knoxx$backend$routes$studio$discord_scan$scan_channel_audio_BANG_(token,channel,p__52596){
var map__52597 = p__52596;
var map__52597__$1 = cljs.core.__destructure_map(map__52597);
var cutoff_ms = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52597__$1,new cljs.core.Keyword(null,"cutoff-ms","cutoff-ms",634249946));
var pages_per_channel = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52597__$1,new cljs.core.Keyword(null,"pages-per-channel","pages-per-channel",1637798552));
var limit_per_page = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52597__$1,new cljs.core.Keyword(null,"limit-per-page","limit-per-page",-1452552787));
var step = (function knoxx$backend$routes$studio$discord_scan$scan_channel_audio_BANG__$_step(before,page,messages_scanned,attachments){
if((page >= pages_per_channel)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),messages_scanned,new cljs.core.Keyword(null,"attachments","attachments",-1535547830),attachments], null));
} else {
return knoxx.backend.routes.studio.discord_scan.discord_fetch_channel_messages_BANG_(token,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel),before,limit_per_page).then((function (messages){
var message_vec = cljs.core.vec(messages);
var matching = cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (message){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (attachment){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(attachment,new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"guildName","guildName",119399715),new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"channelName","channelName",327631603),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"messageId","messageId",-260575736),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"messageUrl","messageUrl",-1125656742),(cljs.core.truth_(new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel))?(""+"https://discord.com/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message))):null),new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message)], 0));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.studio.discord_scan.discord_audio_attachment_QMARK_,new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(message)));
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__52528_SHARP_){
return knoxx.backend.routes.studio.discord_scan.recent_enough_QMARK_(cutoff_ms,p1__52528_SHARP_);
}),message_vec)], 0)));
var next_attachments = cljs.core.into.cljs$core$IFn$_invoke$arity$2(attachments,matching);
var total_scanned = (messages_scanned + cljs.core.count(message_vec));
var oldest_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(cljs.core.last(message_vec));
var stop_QMARK_ = ((cljs.core.empty_QMARK_(message_vec)) || ((((cljs.core.count(message_vec) < limit_per_page)) || (cljs.core.every_QMARK_((function (p1__52529_SHARP_){
return (!(knoxx.backend.routes.studio.discord_scan.recent_enough_QMARK_(cutoff_ms,p1__52529_SHARP_)));
}),message_vec)))));
if(((stop_QMARK_) || (clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(oldest_id)))))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),total_scanned,new cljs.core.Keyword(null,"attachments","attachments",-1535547830),next_attachments], null);
} else {
return knoxx$backend$routes$studio$discord_scan$scan_channel_audio_BANG__$_step(oldest_id,(page + (1)),total_scanned,next_attachments);
}
}));
}
});
return step(null,(0),(0),cljs.core.PersistentVector.EMPTY);
});
knoxx.backend.routes.studio.discord_scan.scan_channel_images_BANG_ = (function knoxx$backend$routes$studio$discord_scan$scan_channel_images_BANG_(token,channel,p__52666){
var map__52667 = p__52666;
var map__52667__$1 = cljs.core.__destructure_map(map__52667);
var cutoff_ms = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52667__$1,new cljs.core.Keyword(null,"cutoff-ms","cutoff-ms",634249946));
var limit_per_page = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52667__$1,new cljs.core.Keyword(null,"limit-per-page","limit-per-page",-1452552787));
return knoxx.backend.routes.studio.discord_scan.discord_fetch_channel_messages_BANG_(token,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel),null,limit_per_page).then((function (messages){
var message_vec = cljs.core.vec(messages);
var matching = cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (message){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (attachment){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(attachment,new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"guildName","guildName",119399715),new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"channelName","channelName",327631603),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(channel),new cljs.core.Keyword(null,"messageId","messageId",-260575736),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"messageUrl","messageUrl",-1125656742),(cljs.core.truth_(new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel))?(""+"https://discord.com/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message))):null),new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message)], 0));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.studio.discord_scan.discord_image_attachment_QMARK_,new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(message)));
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__52665_SHARP_){
return knoxx.backend.routes.studio.discord_scan.recent_enough_QMARK_(cutoff_ms,p1__52665_SHARP_);
}),message_vec)], 0)));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),cljs.core.count(message_vec),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),matching], null);
}));
});
knoxx.backend.routes.studio.discord_scan.fs_path_exists_BANG_ = (function knoxx$backend$routes$studio$discord_scan$fs_path_exists_BANG_(node_fs,path){
return node_fs.stat(path).then((function (_){
return true;
})).catch((function (_){
return false;
}));
});
knoxx.backend.routes.studio.discord_scan.discord_audio_import_result = (function knoxx$backend$routes$studio$discord_scan$discord_audio_import_result(var_args){
var G__52673 = arguments.length;
switch (G__52673) {
case 4:
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4 = (function (status,file_relative,meta_relative,attachment){
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$5(status,file_relative,meta_relative,attachment,null);
}));

(knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$5 = (function (status,file_relative,meta_relative,attachment,error){
var G__52675 = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"path","path",-188191168),file_relative,new cljs.core.Keyword(null,"metadata_path","metadata_path",-1336761797),meta_relative,new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"source_url","source_url",-357411357),new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment)], null);
if(cljs.core.truth_(error)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__52675,new cljs.core.Keyword(null,"error","error",-978969032),error);
} else {
return G__52675;
}
}));

(knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$lang$maxFixedArity = 5);

knoxx.backend.routes.studio.discord_scan.discord_audio_import_metadata = (function knoxx$backend$routes$studio$discord_scan$discord_audio_import_metadata(attachment,loaded,file_relative){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.Keyword(null,"source_url","source_url",-357411357),new cljs.core.Keyword(null,"author_id","author_id",1568127108),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"message_url","message_url",-924222674),new cljs.core.Keyword(null,"size","size",1098693007),new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.Keyword(null,"content_type","content_type",52159344),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.Keyword(null,"attachment_id","attachment_id",586931570),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"channel_name","channel_name",-2066447531),new cljs.core.Keyword(null,"guild_name","guild_name",1509069398),new cljs.core.Keyword(null,"saved_path","saved_path",-1004894409),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"imported_at","imported_at",558511420),new cljs.core.Keyword(null,"author_username","author_username",-1558121698)],[new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"messageUrl","messageUrl",-1125656742).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"size","size",1098693007).cljs$core$IFn$_invoke$arity$1(loaded),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(attachment),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contentType","contentType",-1462509576).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(loaded);
}
})(),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(attachment),"discord-audio-import",new cljs.core.Keyword(null,"channelName","channelName",327631603).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(attachment),file_relative,new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment),(new Date()).toISOString(),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(attachment)]);
});
knoxx.backend.routes.studio.discord_scan.discord_image_import_metadata = (function knoxx$backend$routes$studio$discord_scan$discord_image_import_metadata(attachment,loaded,file_relative){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.Keyword(null,"source_url","source_url",-357411357),new cljs.core.Keyword(null,"author_id","author_id",1568127108),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"message_url","message_url",-924222674),new cljs.core.Keyword(null,"size","size",1098693007),new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.Keyword(null,"content_type","content_type",52159344),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.Keyword(null,"attachment_id","attachment_id",586931570),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"channel_name","channel_name",-2066447531),new cljs.core.Keyword(null,"guild_name","guild_name",1509069398),new cljs.core.Keyword(null,"saved_path","saved_path",-1004894409),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"imported_at","imported_at",558511420),new cljs.core.Keyword(null,"author_username","author_username",-1558121698)],[new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"messageUrl","messageUrl",-1125656742).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"size","size",1098693007).cljs$core$IFn$_invoke$arity$1(loaded),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(attachment),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contentType","contentType",-1462509576).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(loaded);
}
})(),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(attachment),"discord-image-import",new cljs.core.Keyword(null,"channelName","channelName",327631603).cljs$core$IFn$_invoke$arity$1(attachment),new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(attachment),file_relative,new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment),(new Date()).toISOString(),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(attachment)]);
});
knoxx.backend.routes.studio.discord_scan.import_audio_attachment_BANG_ = (function knoxx$backend$routes$studio$discord_scan$import_audio_attachment_BANG_(runtime,config,import_root,attachment){
var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var day = (function (){var ts = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if((((ts).length) >= (10))){
return cljs.core.subs.cljs$core$IFn$_invoke$arity$3(ts,(0),(10));
} else {
return "unknown-date";
}
})();
var guild_segment = knoxx.backend.routes.studio.discord_scan.safe_path_segment((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "discord";
}
}
})());
var channel_segment = knoxx.backend.routes.studio.discord_scan.safe_path_segment((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"channelName","channelName",327631603).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "channel";
}
}
})());
var filename = knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment));
var file_token = knoxx.backend.routes.studio.discord_scan.timestamp_token(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment));
var target_name = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_token)+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(attachment)))+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(attachment)))+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename));
var dir_absolute = shadow.esm.esm_import$node_path.join(workspace_root,import_root,guild_segment,channel_segment,day);
var file_absolute = shadow.esm.esm_import$node_path.join(dir_absolute,target_name);
var file_relative = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(import_root)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_segment)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_segment)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_name));
var meta_absolute = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_absolute)+".json");
var meta_relative = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_relative)+".json");
return knoxx.backend.routes.studio.discord_scan.fs_path_exists_BANG_(shadow.esm.esm_import$node_fs$promises,file_absolute).then((function (exists_QMARK_){
if(cljs.core.truth_(exists_QMARK_)){
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4("skipped",file_relative,meta_relative,attachment);
} else {
return knoxx.backend.tools.media.load_media_source_BANG_(runtime,knoxx.backend.tools.shared.live_config(config),new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment),knoxx.backend.tools.media.audio_render_max_bytes).then((function (loaded){
var metadata = knoxx.backend.routes.studio.discord_scan.discord_audio_import_metadata(attachment,loaded,file_relative);
return knoxx.backend.tools.media.fs_mkdir_BANG_(shadow.esm.esm_import$node_fs$promises,dir_absolute,({"recursive": true})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$3(shadow.esm.esm_import$node_fs$promises,file_absolute,new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(loaded));
})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$4(shadow.esm.esm_import$node_fs$promises,meta_absolute,JSON.stringify(cljs.core.clj__GT_js(metadata),null,(2)),"utf8");
})).then((function (){
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4("imported",file_relative,meta_relative,attachment);
}));
}));
}
}));
});
knoxx.backend.routes.studio.discord_scan.import_image_attachment_BANG_ = (function knoxx$backend$routes$studio$discord_scan$import_image_attachment_BANG_(runtime,config,import_root,attachment){
var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var day = (function (){var ts = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if((((ts).length) >= (10))){
return cljs.core.subs.cljs$core$IFn$_invoke$arity$3(ts,(0),(10));
} else {
return "unknown-date";
}
})();
var guild_segment = knoxx.backend.routes.studio.discord_scan.safe_path_segment((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"guildName","guildName",119399715).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "discord";
}
}
})());
var channel_segment = knoxx.backend.routes.studio.discord_scan.safe_path_segment((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"channelName","channelName",327631603).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "channel";
}
}
})());
var filename = knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment));
var file_token = knoxx.backend.routes.studio.discord_scan.timestamp_token(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(attachment));
var target_name = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_token)+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(attachment)))+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.studio.discord_scan.safe_path_segment(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(attachment)))+"__"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename));
var dir_absolute = shadow.esm.esm_import$node_path.join(workspace_root,import_root,guild_segment,channel_segment,day);
var file_absolute = shadow.esm.esm_import$node_path.join(dir_absolute,target_name);
var file_relative = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(import_root)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_segment)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_segment)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_name));
var meta_absolute = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_absolute)+".json");
var meta_relative = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_relative)+".json");
return knoxx.backend.routes.studio.discord_scan.fs_path_exists_BANG_(shadow.esm.esm_import$node_fs$promises,file_absolute).then((function (exists_QMARK_){
if(cljs.core.truth_(exists_QMARK_)){
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4("skipped",file_relative,meta_relative,attachment);
} else {
return knoxx.backend.tools.media.load_media_source_BANG_(runtime,knoxx.backend.tools.shared.live_config(config),new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment),knoxx.backend.tools.media.multimodal_upload_max_bytes).then((function (loaded){
var metadata = knoxx.backend.routes.studio.discord_scan.discord_image_import_metadata(attachment,loaded,file_relative);
return knoxx.backend.tools.media.fs_mkdir_BANG_(shadow.esm.esm_import$node_fs$promises,dir_absolute,({"recursive": true})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$3(shadow.esm.esm_import$node_fs$promises,file_absolute,new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(loaded));
})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$4(shadow.esm.esm_import$node_fs$promises,meta_absolute,JSON.stringify(cljs.core.clj__GT_js(metadata),null,(2)),"utf8");
})).then((function (){
return knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$4("imported",file_relative,meta_relative,attachment);
}));
}));
}
}));
});
knoxx.backend.routes.studio.discord_scan.write_scan_manifest_BANG_ = (function knoxx$backend$routes$studio$discord_scan$write_scan_manifest_BANG_(_runtime,config,import_root,manifest){
var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var stamp = knoxx.backend.routes.studio.discord_scan.timestamp_token(new cljs.core.Keyword(null,"scanned_at","scanned_at",1147989499).cljs$core$IFn$_invoke$arity$1(manifest));
var dir_absolute = shadow.esm.esm_import$node_path.join(workspace_root,import_root,"_scan_logs");
var file_name = (""+"scan-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(stamp)+".json");
var file_absolute = shadow.esm.esm_import$node_path.join(dir_absolute,file_name);
var file_relative = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(import_root)+"/_scan_logs/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_name));
return knoxx.backend.tools.media.fs_mkdir_BANG_(shadow.esm.esm_import$node_fs$promises,dir_absolute,({"recursive": true})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$4(shadow.esm.esm_import$node_fs$promises,file_absolute,JSON.stringify(cljs.core.clj__GT_js(manifest),null,(2)),"utf8");
})).then((function (){
return file_relative;
}));
});
knoxx.backend.routes.studio.discord_scan.bounded_body_int = (function knoxx$backend$routes$studio$discord_scan$bounded_body_int(body,key_name,default$,min_value,max_value){
var value = parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body[key_name]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
})())),(10));
if(cljs.core.truth_(isNaN(value))){
return default$;
} else {
return cljs.core.max.cljs$core$IFn$_invoke$arity$2(min_value,cljs.core.min.cljs$core$IFn$_invoke$arity$2(value,max_value));
}
});
knoxx.backend.routes.studio.discord_scan.scan_request_options = (function knoxx$backend$routes$studio$discord_scan$scan_request_options(body,default_import_root){
var channel_ids = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["channel_ids"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()))));
var since_hours = knoxx.backend.routes.studio.discord_scan.bounded_body_int(body,"since_hours",(336),(1),(8760));
var pages_per_channel = knoxx.backend.routes.studio.discord_scan.bounded_body_int(body,"pages_per_channel",(2),(1),(20));
var limit_per_page = knoxx.backend.routes.studio.discord_scan.bounded_body_int(body,"limit_per_page",(100),(1),(100));
var max_channels = knoxx.backend.routes.studio.discord_scan.bounded_body_int(body,"max_channels",(50),(1),(500));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738),channel_ids,new cljs.core.Keyword(null,"max-channels","max-channels",1456729856),max_channels,new cljs.core.Keyword(null,"import-root","import-root",1996845422),(function (){var or__5142__auto__ = knoxx.backend.tools.media.normalize_tool_path_arg((body["import_root"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default_import_root;
}
})(),new cljs.core.Keyword(null,"scan-options","scan-options",-1735848242),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"cutoff-ms","cutoff-ms",634249946),(Date.now() - (((since_hours * (60)) * (60)) * (1000))),new cljs.core.Keyword(null,"pages-per-channel","pages-per-channel",1637798552),pages_per_channel,new cljs.core.Keyword(null,"limit-per-page","limit-per-page",-1452552787),limit_per_page], null)], null);
});
knoxx.backend.routes.studio.discord_scan.collect_attachments_BANG_ = (function knoxx$backend$routes$studio$discord_scan$collect_attachments_BANG_(token,channels,scan_channel_BANG_,scan_options){
return knoxx.backend.routes.studio.discord_scan.promise_reduce(channels,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),(0),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),cljs.core.PersistentVector.EMPTY], null),(function (state,channel){
return (scan_channel_BANG_.cljs$core$IFn$_invoke$arity$3 ? scan_channel_BANG_.cljs$core$IFn$_invoke$arity$3(token,channel,scan_options) : scan_channel_BANG_.call(null,token,channel,scan_options)).then((function (result){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),(new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068).cljs$core$IFn$_invoke$arity$1(state) + new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068).cljs$core$IFn$_invoke$arity$1(result)),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(result))], null);
}));
}));
});
knoxx.backend.routes.studio.discord_scan.import_attachment_results_BANG_ = (function knoxx$backend$routes$studio$discord_scan$import_attachment_results_BANG_(runtime,config,import_root,attachments,import_attachment_BANG_){
return knoxx.backend.routes.studio.discord_scan.promise_reduce(attachments,cljs.core.PersistentVector.EMPTY,(function (results,attachment){
return (import_attachment_BANG_.cljs$core$IFn$_invoke$arity$4 ? import_attachment_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,import_root,attachment) : import_attachment_BANG_.call(null,runtime,config,import_root,attachment)).then((function (result){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(results,result);
})).catch((function (error){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(results,knoxx.backend.routes.studio.discord_scan.discord_audio_import_result.cljs$core$IFn$_invoke$arity$5("failed","","",attachment,(function (){var or__5142__auto__ = error.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error));
}
})()));
}));
}));
});
knoxx.backend.routes.studio.discord_scan.scan_summary = (function knoxx$backend$routes$studio$discord_scan$scan_summary(import_root,channels,messages_scanned,attachments,results){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"skipped_count","skipped_count",617412737),new cljs.core.Keyword(null,"failed_count","failed_count",-2009747228),new cljs.core.Keyword(null,"channels","channels",1132759174),new cljs.core.Keyword(null,"imported_count","imported_count",484068756),new cljs.core.Keyword(null,"ok","ok",967785236),new cljs.core.Keyword(null,"import_root","import_root",-1753448747),new cljs.core.Keyword(null,"messages_scanned","messages_scanned",-243158219),new cljs.core.Keyword(null,"attachments_found","attachments_found",1061501240),new cljs.core.Keyword(null,"channels_scanned","channels_scanned",152781658),new cljs.core.Keyword(null,"scanned_at","scanned_at",1147989499),new cljs.core.Keyword(null,"results","results",-1134170113)],[cljs.core.count(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__52704_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("skipped",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__52704_SHARP_));
}),results)),cljs.core.count(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__52705_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("failed",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__52705_SHARP_));
}),results)),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (channel){
return cljs.core.select_keys(channel,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"guildName","guildName",119399715)], null));
}),cljs.core.take.cljs$core$IFn$_invoke$arity$2((50),channels)),cljs.core.count(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__52703_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("imported",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__52703_SHARP_));
}),results)),true,import_root,messages_scanned,cljs.core.count(attachments),cljs.core.count(channels),(new Date()).toISOString(),cljs.core.vec(results)]);
});
knoxx.backend.routes.studio.discord_scan.run_discord_media_scan_BANG_ = (function knoxx$backend$routes$studio$discord_scan$run_discord_media_scan_BANG_(runtime,config,p__52724,scan_channel_BANG_,import_attachment_BANG_){
var map__52729 = p__52724;
var map__52729__$1 = cljs.core.__destructure_map(map__52729);
var token = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52729__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var channel_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52729__$1,new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738));
var max_channels = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52729__$1,new cljs.core.Keyword(null,"max-channels","max-channels",1456729856));
var import_root = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52729__$1,new cljs.core.Keyword(null,"import-root","import-root",1996845422));
var scan_options = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52729__$1,new cljs.core.Keyword(null,"scan-options","scan-options",-1735848242));
return knoxx.backend.routes.studio.discord_scan.collect_discord_scan_channels_BANG_(token,channel_ids,max_channels).then((function (channels){
return knoxx.backend.routes.studio.discord_scan.collect_attachments_BANG_(token,channels,scan_channel_BANG_,scan_options).then((function (p__52733){
var map__52734 = p__52733;
var map__52734__$1 = cljs.core.__destructure_map(map__52734);
var messages_scanned = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52734__$1,new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068));
var attachments = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52734__$1,new cljs.core.Keyword(null,"attachments","attachments",-1535547830));
return knoxx.backend.routes.studio.discord_scan.import_attachment_results_BANG_(runtime,config,import_root,attachments,import_attachment_BANG_).then((function (results){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"channels","channels",1132759174),channels,new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068),messages_scanned,new cljs.core.Keyword(null,"attachments","attachments",-1535547830),attachments,new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
}));
}));
}));
});
knoxx.backend.routes.studio.discord_scan.handle_discord_media_scan_BANG_ = (function knoxx$backend$routes$studio$discord_scan$handle_discord_media_scan_BANG_(runtime,config,reply,body,default_import_root,scan_channel_BANG_,import_attachment_BANG_,failure_message){
var token = knoxx.backend.routes.studio.discord_scan.discord_bot_token(config);
var options = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.studio.discord_scan.scan_request_options(body,default_import_root),new cljs.core.Keyword(null,"token","token",-1211463215),token);
if(cljs.core.not(token)){
var G__52759 = reply;
var G__52760 = (503);
var G__52761 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Discord bot token is not configured"], null);
return (knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52759,G__52760,G__52761) : knoxx.backend.routes.studio.discord_scan.json_response_BANG_.call(null,G__52759,G__52760,G__52761));
} else {
return knoxx.backend.routes.studio.discord_scan.run_discord_media_scan_BANG_(runtime,config,options,scan_channel_BANG_,import_attachment_BANG_).then((function (p__52762){
var map__52763 = p__52762;
var map__52763__$1 = cljs.core.__destructure_map(map__52763);
var channels = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52763__$1,new cljs.core.Keyword(null,"channels","channels",1132759174));
var messages_scanned = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52763__$1,new cljs.core.Keyword(null,"messages-scanned","messages-scanned",805888068));
var attachments = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52763__$1,new cljs.core.Keyword(null,"attachments","attachments",-1535547830));
var results = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52763__$1,new cljs.core.Keyword(null,"results","results",-1134170113));
var summary = knoxx.backend.routes.studio.discord_scan.scan_summary(new cljs.core.Keyword(null,"import-root","import-root",1996845422).cljs$core$IFn$_invoke$arity$1(options),channels,messages_scanned,attachments,results);
return knoxx.backend.routes.studio.discord_scan.write_scan_manifest_BANG_(runtime,config,new cljs.core.Keyword(null,"import-root","import-root",1996845422).cljs$core$IFn$_invoke$arity$1(options),summary).then((function (manifest_path){
var G__52764 = reply;
var G__52765 = (200);
var G__52766 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(summary,new cljs.core.Keyword(null,"manifest_path","manifest_path",-2086066483),manifest_path);
return (knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52764,G__52765,G__52766) : knoxx.backend.routes.studio.discord_scan.json_response_BANG_.call(null,G__52764,G__52765,G__52766));
}));
})).catch((function (err){
var G__52769 = reply;
var G__52770 = (500);
var G__52771 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(failure_message)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.studio.discord_scan.json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52769,G__52770,G__52771) : knoxx.backend.routes.studio.discord_scan.json_response_BANG_.call(null,G__52769,G__52770,G__52771));
}));
}
});
knoxx.backend.routes.studio.discord_scan.studio_discord_audio_scan_BANG_ = (function knoxx$backend$routes$studio$discord_scan$studio_discord_audio_scan_BANG_(app,runtime,config,deps){
var map__52772 = deps;
var map__52772__$1 = cljs.core.__destructure_map(map__52772);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52772__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52773 = app;
var G__52774 = "POST";
var G__52775 = "/api/studio/discord-audio-scan";
var G__52776 = (function (request,reply){
var G__52777 = runtime;
var G__52778 = request;
var G__52779 = reply;
var G__52780 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

return knoxx.backend.routes.studio.discord_scan.handle_discord_media_scan_BANG_(runtime,config,reply,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),"Audio/discord-imports",knoxx.backend.routes.studio.discord_scan.scan_channel_audio_BANG_,knoxx.backend.routes.studio.discord_scan.import_audio_attachment_BANG_,"Discord audio scan failed: ");
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52777,G__52778,G__52779,G__52780) : with_request_context_BANG_.call(null,G__52777,G__52778,G__52779,G__52780));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52773,G__52774,G__52775,G__52776) : route_BANG_.call(null,G__52773,G__52774,G__52775,G__52776));
});
knoxx.backend.routes.studio.discord_scan.studio_discord_image_scan_BANG_ = (function knoxx$backend$routes$studio$discord_scan$studio_discord_image_scan_BANG_(app,runtime,config,deps){
var map__52781 = deps;
var map__52781__$1 = cljs.core.__destructure_map(map__52781);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52781__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52782 = app;
var G__52783 = "POST";
var G__52784 = "/api/studio/discord-image-scan";
var G__52785 = (function (request,reply){
var G__52786 = runtime;
var G__52787 = request;
var G__52788 = reply;
var G__52789 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

return knoxx.backend.routes.studio.discord_scan.handle_discord_media_scan_BANG_(runtime,config,reply,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),"discord/images",knoxx.backend.routes.studio.discord_scan.scan_channel_images_BANG_,knoxx.backend.routes.studio.discord_scan.import_image_attachment_BANG_,"Discord image scan failed: ");
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52786,G__52787,G__52788,G__52789) : with_request_context_BANG_.call(null,G__52786,G__52787,G__52788,G__52789));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52782,G__52783,G__52784,G__52785) : route_BANG_.call(null,G__52782,G__52783,G__52784,G__52785));
});

//# sourceMappingURL=knoxx.backend.routes.studio.discord_scan.js.map
