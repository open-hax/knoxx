import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.quality_labels.js";
import "./knoxx.backend.text.js";
import "./shadow.esm.esm_import$$resvg$resvg_js.js";
import "./knoxx.backend.tools.actor_credentials.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.discord');
knoxx.backend.tools.discord.discord_gateway_manager = (function knoxx$backend$tools$discord$discord_gateway_manager(){
return knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();
});
knoxx.backend.tools.discord.discord_gateway_started_QMARK_ = (function knoxx$backend$tools$discord$discord_gateway_started_QMARK_(){
var manager = knoxx.backend.tools.discord.discord_gateway_manager();
if((!((manager == null)))){
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
knoxx.backend.tools.discord.discord_token_BANG_ = (function knoxx$backend$tools$discord$discord_token_BANG_(runtime){
return knoxx.backend.tools.actor_credentials.get_credential_BANG_(runtime,"discord_bot").then((function (credential){
var token = knoxx.backend.tools.actor_credentials.secret_value.cljs$core$IFn$_invoke$arity$variadic(credential,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"botToken","botToken",1995464313),new cljs.core.Keyword(null,"bot-token","bot-token",-851028031),new cljs.core.Keyword(null,"token","token",-1211463215)], 0));
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)))){
throw (new Error("Discord bot actor credential must include botToken."));
} else {
}

return token;
}));
});
knoxx.backend.tools.discord.discord_rest_headers = (function knoxx$backend$tools$discord$discord_rest_headers(token){
return ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)), "Content-Type": "application/json"});
});
knoxx.backend.tools.discord.discord_query_url = (function knoxx$backend$tools$discord$discord_query_url(base,params){
var search = (new URLSearchParams());
var seq__58944_59647 = cljs.core.seq(params);
var chunk__58945_59648 = null;
var count__58946_59649 = (0);
var i__58947_59650 = (0);
while(true){
if((i__58947_59650 < count__58946_59649)){
var vec__58959_59651 = chunk__58945_59648.cljs$core$IIndexed$_nth$arity$2(null,i__58947_59650);
var k_59652 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58959_59651,(0),null);
var v_59653 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58959_59651,(1),null);
if((((v_59653 == null)) || (((typeof v_59653 === 'string') && (clojure.string.blank_QMARK_(v_59653)))))){
} else {
search.append(cljs.core.name(k_59652),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v_59653)));
}


var G__59654 = seq__58944_59647;
var G__59655 = chunk__58945_59648;
var G__59656 = count__58946_59649;
var G__59657 = (i__58947_59650 + (1));
seq__58944_59647 = G__59654;
chunk__58945_59648 = G__59655;
count__58946_59649 = G__59656;
i__58947_59650 = G__59657;
continue;
} else {
var temp__5825__auto___59658 = cljs.core.seq(seq__58944_59647);
if(temp__5825__auto___59658){
var seq__58944_59659__$1 = temp__5825__auto___59658;
if(cljs.core.chunked_seq_QMARK_(seq__58944_59659__$1)){
var c__5673__auto___59663 = cljs.core.chunk_first(seq__58944_59659__$1);
var G__59664 = cljs.core.chunk_rest(seq__58944_59659__$1);
var G__59665 = c__5673__auto___59663;
var G__59666 = cljs.core.count(c__5673__auto___59663);
var G__59667 = (0);
seq__58944_59647 = G__59664;
chunk__58945_59648 = G__59665;
count__58946_59649 = G__59666;
i__58947_59650 = G__59667;
continue;
} else {
var vec__58965_59668 = cljs.core.first(seq__58944_59659__$1);
var k_59669 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58965_59668,(0),null);
var v_59670 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58965_59668,(1),null);
if((((v_59670 == null)) || (((typeof v_59670 === 'string') && (clojure.string.blank_QMARK_(v_59670)))))){
} else {
search.append(cljs.core.name(k_59669),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v_59670)));
}


var G__59672 = cljs.core.next(seq__58944_59659__$1);
var G__59673 = null;
var G__59674 = (0);
var G__59675 = (0);
seq__58944_59647 = G__59672;
chunk__58945_59648 = G__59673;
count__58946_59649 = G__59674;
i__58947_59650 = G__59675;
continue;
}
} else {
}
}
break;
}

var query = search.toString();
if(clojure.string.blank_QMARK_(query)){
return base;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query));
}
});
knoxx.backend.tools.discord.discord_fetch_json_BANG_ = (function knoxx$backend$tools$discord$discord_fetch_json_BANG_(url,options){
return fetch(url,options).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"Discord API error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.tools.discord.discord_attachments = (function knoxx$backend$tools$discord$discord_attachments(message){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (attachment){
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
})(),new cljs.core.Keyword(null,"contentType","contentType",-1462509576),(attachment["content_type"]),new cljs.core.Keyword(null,"size","size",1098693007),(function (){var or__5142__auto__ = (attachment["size"]);
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
}),(cljs.core.truth_(cljs.core.array_QMARK_((message["attachments"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((message["attachments"])):cljs.core.PersistentVector.EMPTY));
});
knoxx.backend.tools.discord.discord_embeds = (function knoxx$backend$tools$discord$discord_embeds(message){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (embed){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"title","title",636505583),(embed["title"]),new cljs.core.Keyword(null,"description","description",-1428560544),(embed["description"]),new cljs.core.Keyword(null,"url","url",276297046),(embed["url"])], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((message["embeds"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((message["embeds"])):cljs.core.PersistentVector.EMPTY));
});
knoxx.backend.tools.discord.discord_message__GT_map = (function knoxx$backend$tools$discord$discord_message__GT_map(message){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"embeds","embeds",833349080),new cljs.core.Keyword(null,"timestamp","timestamp",579478971)],[(function (){var or__5142__auto__ = (message["author"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (message["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (message["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),knoxx.backend.tools.discord.discord_attachments(message),(function (){var or__5142__auto__ = (message["author"]["username"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})(),cljs.core.boolean$((message["author"]["bot"])),(function (){var or__5142__auto__ = (message["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),knoxx.backend.tools.discord.discord_embeds(message),(function (){var or__5142__auto__ = (message["timestamp"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()]);
});
knoxx.backend.tools.discord.discord_message_line = (function knoxx$backend$tools$discord$discord_message_line(message){
var content = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var attachments = new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(message);
var attachment_text = ((cljs.core.seq(attachments))?(""+" attachments="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (attachment){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(temp__5825__auto__)){
var url = temp__5825__auto__;
return (""+" <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+">");
} else {
return null;
}
})()));
}),attachments)))):null);
var embeds = new cljs.core.Keyword(null,"embeds","embeds",833349080).cljs$core$IFn$_invoke$arity$1(message);
var embed_text = ((cljs.core.seq(embeds))?(""+" embeds="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (embed){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(embed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "embed";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(embed);
if(cljs.core.truth_(temp__5825__auto__)){
var url = temp__5825__auto__;
return (""+" <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+">");
} else {
return null;
}
})()));
}),embeds)))):null);
return (""+"<"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message))+" (id:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message))+")> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(content))?"[no text]":content))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = attachment_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = embed_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
});
knoxx.backend.tools.discord.discord_messages_text = (function knoxx$backend$tools$discord$discord_messages_text(heading,messages){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(heading)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.seq(messages))?(""+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.discord.discord_message_line,messages)))):"\n\nNo messages found.")));
});
knoxx.backend.tools.discord.discord_record_id = (function knoxx$backend$tools$discord$discord_record_id(message){
return (""+"discord:message:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(message))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message)));
});
knoxx.backend.tools.discord.discord_message_quality = (function knoxx$backend$tools$discord$discord_message_quality(message){
return knoxx.backend.quality_labels.quality_label(message);
});
knoxx.backend.tools.discord.drop_bad_discord_messages = (function knoxx$backend$tools$discord$drop_bad_discord_messages(messages){
return knoxx.backend.quality_labels.drop_bad(messages);
});
knoxx.backend.tools.discord.parse_hours = (function knoxx$backend$tools$discord$parse_hours(value,default_hours){
var n = Number(value);
if(((cljs.core.not(isNaN(n))) && ((n > (0))))){
return n;
} else {
return default_hours;
}
});
knoxx.backend.tools.discord.timestamp_ms = (function knoxx$backend$tools$discord$timestamp_ms(value){
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
knoxx.backend.tools.discord.within_hours_QMARK_ = (function knoxx$backend$tools$discord$within_hours_QMARK_(hours,message){
var temp__5823__auto__ = knoxx.backend.tools.discord.timestamp_ms(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(message));
if(cljs.core.truth_(temp__5823__auto__)){
var ms = temp__5823__auto__;
return (ms >= (Date.now() - (((hours * (60)) * (60)) * (1000))));
} else {
return true;
}
});
knoxx.backend.tools.discord.chronological_discord_messages = (function knoxx$backend$tools$discord$chronological_discord_messages(messages){
return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2((function (message){
var or__5142__auto__ = knoxx.backend.tools.discord.timestamp_ms(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(message));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
}),messages));
});
knoxx.backend.tools.discord.good_first_then_not_bad = (function knoxx$backend$tools$discord$good_first_then_not_bad(messages){
var non_bad = knoxx.backend.tools.discord.drop_bad_discord_messages(messages);
var good = knoxx.backend.tools.discord.chronological_discord_messages(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59075_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("good",knoxx.backend.tools.discord.discord_message_quality(p1__59075_SHARP_));
}),non_bad));
var not_bad = knoxx.backend.tools.discord.chronological_discord_messages(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__59076_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("good",knoxx.backend.tools.discord.discord_message_quality(p1__59076_SHARP_));
}),non_bad));
return cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(good,not_bad));
});
knoxx.backend.tools.discord.label_for_record_id = (function knoxx$backend$tools$discord$label_for_record_id(labels,record_id){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(labels,record_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(labels,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(record_id));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
});
knoxx.backend.tools.discord.attach_openplanner_labels_BANG_ = (function knoxx$backend$tools$discord$attach_openplanner_labels_BANG_(config,messages){
if(((cljs.core.empty_QMARK_(messages)) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))){
return Promise.resolve(knoxx.backend.tools.discord.good_first_then_not_bad(messages));
} else {
var ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.discord.discord_record_id,messages);
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/labels/records/lookup",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ids","ids",-998535796),ids], null)).then((function (response){
var labels = new cljs.core.Keyword(null,"labels","labels",-626734591).cljs$core$IFn$_invoke$arity$1(response);
return knoxx.backend.tools.discord.good_first_then_not_bad(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (message){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(message,new cljs.core.Keyword(null,"openplannerLabels","openplannerLabels",-1625330291),knoxx.backend.tools.discord.label_for_record_id(labels,knoxx.backend.tools.discord.discord_record_id(message)));
}),messages));
})).catch((function (error){
console.warn("[discord-tools] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad messages",error);

return cljs.core.PersistentVector.EMPTY;
}));
}
});
knoxx.backend.tools.discord.discord_fetch_channel_messages_BANG_ = (function knoxx$backend$tools$discord$discord_fetch_channel_messages_BANG_(runtime,config,channel_id,p__59087){
var map__59088 = p__59087;
var map__59088__$1 = cljs.core.__destructure_map(map__59088);
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59088__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var before = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59088__$1,new cljs.core.Keyword(null,"before","before",-1633692388));
var after = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59088__$1,new cljs.core.Keyword(null,"after","after",594996914));
var around = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59088__$1,new cljs.core.Keyword(null,"around","around",-265975553));
if(clojure.string.blank_QMARK_(channel_id)){
throw (new Error("channel_id is required"));
} else {
}

return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return knoxx.backend.tools.discord.discord_fetch_json_BANG_(knoxx.backend.tools.discord.discord_query_url((""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages"),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"limit","limit",-1355822363),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})())),new cljs.core.Keyword(null,"before","before",-1633692388),before,new cljs.core.Keyword(null,"after","after",594996914),after,new cljs.core.Keyword(null,"around","around",-265975553),around], null)),({"method": "GET", "headers": knoxx.backend.tools.discord.discord_rest_headers(token)})).then((function (payload){
var messages = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.discord.discord_message__GT_map,(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY)));
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"messages","messages",345434482),messages,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(messages),new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id], null);
}));
}));
});
knoxx.backend.tools.discord.discord_scroll_channel_messages_BANG_ = (function knoxx$backend$tools$discord$discord_scroll_channel_messages_BANG_(runtime,config,channel_id,oldest_seen_id,limit){
return knoxx.backend.tools.discord.discord_fetch_channel_messages_BANG_(runtime,config,channel_id,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"before","before",-1633692388),oldest_seen_id], null)).then((function (result){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(result,new cljs.core.Keyword(null,"oldestSeenId","oldestSeenId",-1356766638),oldest_seen_id);
}));
});
knoxx.backend.tools.discord.discord_open_dm_channel_BANG_ = (function knoxx$backend$tools$discord$discord_open_dm_channel_BANG_(runtime,user_id){
if(clojure.string.blank_QMARK_(user_id)){
throw (new Error("user_id is required"));
} else {
}

return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return knoxx.backend.tools.discord.discord_fetch_json_BANG_("https://discord.com/api/v10/users/@me/channels",({"method": "POST", "headers": knoxx.backend.tools.discord.discord_rest_headers(token), "body": JSON.stringify(({"recipient_id": user_id}))}));
}));
});
knoxx.backend.tools.discord.discord_fetch_dm_messages_BANG_ = (function knoxx$backend$tools$discord$discord_fetch_dm_messages_BANG_(runtime,config,user_id,p__59100){
var map__59101 = p__59100;
var map__59101__$1 = cljs.core.__destructure_map(map__59101);
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59101__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var before = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59101__$1,new cljs.core.Keyword(null,"before","before",-1633692388));
return knoxx.backend.tools.discord.discord_open_dm_channel_BANG_(runtime,user_id).then((function (channel){
var channel_id = (function (){var or__5142__auto__ = (channel["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return knoxx.backend.tools.discord.discord_fetch_channel_messages_BANG_(runtime,config,channel_id,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"before","before",-1633692388),before], null)).then((function (result){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"dmChannelId","dmChannelId",1364468195),channel_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"userId","userId",575594135),user_id], 0));
}));
}));
});
knoxx.backend.tools.discord.discord_search_messages_BANG_ = (function knoxx$backend$tools$discord$discord_search_messages_BANG_(runtime,config,scope,p__59109){
var map__59110 = p__59109;
var map__59110__$1 = cljs.core.__destructure_map(map__59110);
var channel_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"channel-id","channel-id",138191095));
var user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"user-id","user-id",-206822291));
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var before = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"before","before",-1633692388));
var after = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"after","after",594996914));
var since_hours = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59110__$1,new cljs.core.Keyword(null,"since-hours","since-hours",124306716));
var scope__$1 = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = scope;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "channel";
}
})())));
var timeframe_hours = knoxx.backend.tools.discord.parse_hours(since_hours,(168));
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(scope__$1,"dm"))?knoxx.backend.tools.discord.discord_fetch_dm_messages_BANG_(runtime,config,user_id,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(100),new cljs.core.Keyword(null,"before","before",-1633692388),before], null)):knoxx.backend.tools.discord.discord_fetch_channel_messages_BANG_(runtime,config,channel_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(100),new cljs.core.Keyword(null,"before","before",-1633692388),before,new cljs.core.Keyword(null,"after","after",594996914),after], null))).then((function (result){
return knoxx.backend.tools.discord.attach_openplanner_labels_BANG_(config,new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(result)).then((function (labelled){
var needle = (function (){var G__59111 = query;
var G__59111__$1 = (((G__59111 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59111)));
if((G__59111__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__59111__$1);
}
})();
var author_id = (function (){var G__59113 = user_id;
var G__59113__$1 = (((G__59113 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59113)));
if((G__59113__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__59113__$1);
}
})();
var filtered = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})(),knoxx.backend.tools.discord.good_first_then_not_bad(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (message){
return ((((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = needle;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))) || (clojure.string.includes_QMARK_(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message)))),needle)))) && ((((author_id == null)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(author_id,new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message))))));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59105_SHARP_){
return knoxx.backend.tools.discord.within_hours_QMARK_(timeframe_hours,p1__59105_SHARP_);
}),labelled)))));
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"messages","messages",345434482),filtered,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(filtered),new cljs.core.Keyword(null,"scope","scope",-439358418),scope__$1,new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"dmChannelId","dmChannelId",1364468195),new cljs.core.Keyword(null,"dmChannelId","dmChannelId",1364468195).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"source","source",-433931539),"client_side_filter_openplanner_labels",new cljs.core.Keyword(null,"qualityOrder","qualityOrder",1888657940),"good_chronological_then_not_bad_chronological",new cljs.core.Keyword(null,"sinceHours","sinceHours",530839084),timeframe_hours], null);
}));
}));
});
knoxx.backend.tools.discord.infer_upload_filename = (function knoxx$backend$tools$discord$infer_upload_filename(url,idx){
var pathname = (function (){try{return (new URL((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)))).pathname;
}catch (e59127){var _ = e59127;
return "";
}})();
var candidate = (function (){var G__59128 = pathname;
var G__59128__$1 = (((G__59128 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__59128,/\//));
var G__59128__$2 = (((G__59128__$1 == null))?null:cljs.core.last(G__59128__$1));
var G__59128__$3 = (((G__59128__$2 == null))?null:clojure.string.trim(G__59128__$2));
if((G__59128__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__59128__$3);
}
})();
var or__5142__auto__ = candidate;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"attachment-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx)+".bin");
}
});
knoxx.backend.tools.discord.file_url__GT_path = (function knoxx$backend$tools$discord$file_url__GT_path(source){
try{return decodeURIComponent((new URL(source)).pathname);
}catch (e59132){var _ = e59132;
return cljs.core.subs.cljs$core$IFn$_invoke$arity$2(source,(("file://").length));
}});
/**
 * Render an SVG buffer to PNG using @resvg/resvg-js. Returns a promise.
 */
knoxx.backend.tools.discord.svg_buffer__GT_png_buffer_BANG_ = (function knoxx$backend$tools$discord$svg_buffer__GT_png_buffer_BANG_(svg_buffer){
return (new Promise((function (resolve,reject){
try{var Resvg = (function (){var or__5142__auto__ = shadow.esm.esm_import$$resvg$resvg_js.default.Resvg;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$$resvg$resvg_js.default;
}
})();
var svg_str = knoxx.backend.text.sanitize_svg_content(svg_buffer.toString("utf8"));
var renderer = (new Resvg(svg_str,({"logLevel": "off"})));
var png_data = renderer.render().asPng();
var G__59136 = Buffer.from(png_data);
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__59136) : resolve.call(null,G__59136));
}catch (e59135){var e = e59135;
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(e) : reject.call(null,e));
}})));
});
knoxx.backend.tools.discord.svg_code_block_pattern = /```(?:svg|image\/svg\+xml)\s*\n([\s\S]*?)\n```/is;
/**
 * Pull fenced ```svg code blocks out of message text so they can be rendered
 * and attached as PNGs instead of being sent as raw code.
 */
knoxx.backend.tools.discord.extract_inline_svg_code_blocks = (function knoxx$backend$tools$discord$extract_inline_svg_code_blocks(text){
var raw_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var matches = cljs.core.re_seq(knoxx.backend.tools.discord.svg_code_block_pattern,raw_text);
var svg_blocks = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59144_SHARP_){
return clojure.string.includes_QMARK_(clojure.string.lower_case(p1__59144_SHARP_),"<svg");
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__59143_SHARP_){
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__59143_SHARP_)));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.second,matches))));
var cleaned_text = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,p__59165){
var vec__59171 = p__59165;
var full_match = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59171,(0),null);
var _svg = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59171,(1),null);
return clojure.string.replace(acc,full_match,"[image]");
}),raw_text,matches);
var attachment_urls = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (svg){
return (""+"data:image/svg+xml;charset=utf-8,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(svg)));
}),svg_blocks);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"text","text",-1790561697),cleaned_text,new cljs.core.Keyword(null,"attachmentUrls","attachmentUrls",1857227267),attachment_urls], null);
});
/**
 * If the resolved attachment is an SVG, render it to PNG transparently.
 * On render failure, returns original attachment.
 */
knoxx.backend.tools.discord.maybe_render_svg_BANG_ = (function knoxx$backend$tools$discord$maybe_render_svg_BANG_(p__59182){
var map__59183 = p__59182;
var map__59183__$1 = cljs.core.__destructure_map(map__59183);
var attachment = map__59183__$1;
var name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59183__$1,new cljs.core.Keyword(null,"name","name",1843675177));
var mimeType = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59183__$1,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690));
var buffer = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59183__$1,new cljs.core.Keyword(null,"buffer","buffer",617295198));
if(cljs.core.truth_((function (){var or__5142__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mimeType,"image/svg+xml");
if(or__5142__auto__){
return or__5142__auto__;
} else {
var G__59186 = name;
var G__59186__$1 = (((G__59186 == null))?null:clojure.string.lower_case(G__59186));
if((G__59186__$1 == null)){
return null;
} else {
return clojure.string.ends_with_QMARK_(G__59186__$1,".svg");
}
}
})())){
return knoxx.backend.tools.discord.svg_buffer__GT_png_buffer_BANG_(buffer).then((function (png_buf){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),(cljs.core.truth_((function (){var G__59192 = name;
var G__59192__$1 = (((G__59192 == null))?null:clojure.string.lower_case(G__59192));
if((G__59192__$1 == null)){
return null;
} else {
return clojure.string.ends_with_QMARK_(G__59192__$1,".svg");
}
})())?clojure.string.replace(name,/\.svg$/i,".png"):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "attachment";
}
})())+".png")),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),"image/png",new cljs.core.Keyword(null,"buffer","buffer",617295198),png_buf], null);
})).catch((function (error){
console.warn("[discord-tools] SVG render failed; uploading original SVG",error);

return attachment;
}));
} else {
return Promise.resolve(attachment);
}
});
/**
 * Fetch an attachment from a URL, data URL, or local file path.
 * Returns a promise resolving to {:name :mimeType :buffer}.
 * SVG files are automatically rendered to PNG before upload.
 */
knoxx.backend.tools.discord.fetch_discord_upload_attachment_BANG_ = (function knoxx$backend$tools$discord$fetch_discord_upload_attachment_BANG_(runtime,config,url,idx){
var source = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(source)){
return Promise.reject((new Error("Empty attachment source")));
} else {
if(knoxx.backend.tools.media.source_data_url_QMARK_(source)){
var data_start = source.indexOf(",");
var metadata = (((data_start >= (0)))?cljs.core.subs.cljs$core$IFn$_invoke$arity$3(source,(5),data_start):null);
var payload = (((data_start >= (0)))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(source,(data_start + (1))):null);
if((((metadata == null)) || ((payload == null)))){
return Promise.reject((new Error("Invalid data URL attachment")));
} else {
var metadata_parts = clojure.string.split.cljs$core$IFn$_invoke$arity$2(metadata,/;/);
var mime_type = knoxx.backend.tools.media.sanitize_mime_type(cljs.core.first(metadata_parts),"application/octet-stream");
var base64_QMARK_ = cljs.core.boolean$(cljs.core.some(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, ["base64",null], null), null),cljs.core.rest(metadata_parts)));
var buffer = ((base64_QMARK_)?Buffer.from(payload,"base64"):Buffer.from(decodeURIComponent(payload),"utf8"));
knoxx.backend.tools.media.ensure_source_size_BANG_(buffer.length,knoxx.backend.tools.media.workspace_media_max_bytes,"Discord attachment");

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),(""+"attachment-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.media.mime_type__GT_extension(mime_type))),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type,new cljs.core.Keyword(null,"buffer","buffer",617295198),buffer], null));
}
} else {
if(knoxx.backend.tools.media.source_http_url_QMARK_(source)){
return fetch(source,({"headers": ({"User-Agent": "Knoxx-Agent/1.0"})})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.arrayBuffer().then((function (buf){
var buffer = Buffer.from(buf);
var mime_type = knoxx.backend.tools.media.sanitize_mime_type(resp.headers.get("content-type"),knoxx.backend.tools.media.workspace_media_mime_type(source));
knoxx.backend.tools.media.ensure_source_size_BANG_(buffer.length,knoxx.backend.tools.media.workspace_media_max_bytes,"Discord attachment");

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),knoxx.backend.tools.discord.infer_upload_filename(source,idx),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type,new cljs.core.Keyword(null,"buffer","buffer",617295198),buffer], null);
}));
} else {
return resp.text().then((function (text){
throw (new Error((""+"Attachment fetch failed "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
} else {
var raw_source = ((knoxx.backend.tools.media.source_file_url_QMARK_(source))?knoxx.backend.tools.discord.file_url__GT_path(source):source);
return knoxx.backend.tools.media.load_media_source_BANG_(runtime,config,raw_source,knoxx.backend.tools.media.workspace_media_max_bytes).then((function (loaded){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(loaded);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"attachment-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.media.mime_type__GT_extension(new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(loaded))));
}
})(),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),knoxx.backend.tools.media.sanitize_mime_type(new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(loaded),"application/octet-stream"),new cljs.core.Keyword(null,"buffer","buffer",617295198),new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(loaded)], null);
}));

}
}
}
});
knoxx.backend.tools.discord.discord_message_chunks = (function knoxx$backend$tools$discord$discord_message_chunks(normalized){
var chunk_size = (2000);
var base_text = ((clojure.string.blank_QMARK_(normalized))?"[attachment]":normalized);
var remaining = base_text;
var acc = cljs.core.PersistentVector.EMPTY;
while(true){
if((cljs.core.count(remaining) <= chunk_size)){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,remaining);
} else {
var slice = remaining.lastIndexOf("\n\n",chunk_size);
var split_at = (((slice > ((chunk_size * 0.5) | 0)))?slice:chunk_size);
var G__59715 = clojure.string.trim(cljs.core.subs.cljs$core$IFn$_invoke$arity$2(remaining,split_at));
var G__59716 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,clojure.string.trim(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(remaining,(0),split_at)));
remaining = G__59715;
acc = G__59716;
continue;
}
break;
}
});
knoxx.backend.tools.discord.discord_message_payload = (function knoxx$backend$tools$discord$discord_message_payload(chunk,reply_to,state){
var G__59245 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),chunk], null);
if(cljs.core.truth_((function (){var and__5140__auto__ = reply_to;
if(cljs.core.truth_(and__5140__auto__)){
return (new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(state) == null);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59245,new cljs.core.Keyword(null,"message_reference","message_reference",-2008207798),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"message_id","message_id",663757010),reply_to], null));
} else {
return G__59245;
}
});
knoxx.backend.tools.discord.append_discord_files_BANG_ = (function knoxx$backend$tools$discord$append_discord_files_BANG_(form,file_list){
var seq__59255_59721 = cljs.core.seq(cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2(cljs.core.vector,file_list));
var chunk__59256_59722 = null;
var count__59257_59723 = (0);
var i__59258_59724 = (0);
while(true){
if((i__59258_59724 < count__59257_59723)){
var vec__59281_59725 = chunk__59256_59722.cljs$core$IIndexed$_nth$arity$2(null,i__59258_59724);
var idx_59726 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59281_59725,(0),null);
var file_59727 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59281_59725,(1),null);
form.append((""+"files["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx_59726)+"]"),(new Blob([new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(file_59727)],({"type": new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(file_59727)}))),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(file_59727));


var G__59732 = seq__59255_59721;
var G__59733 = chunk__59256_59722;
var G__59734 = count__59257_59723;
var G__59735 = (i__59258_59724 + (1));
seq__59255_59721 = G__59732;
chunk__59256_59722 = G__59733;
count__59257_59723 = G__59734;
i__59258_59724 = G__59735;
continue;
} else {
var temp__5825__auto___59736 = cljs.core.seq(seq__59255_59721);
if(temp__5825__auto___59736){
var seq__59255_59737__$1 = temp__5825__auto___59736;
if(cljs.core.chunked_seq_QMARK_(seq__59255_59737__$1)){
var c__5673__auto___59738 = cljs.core.chunk_first(seq__59255_59737__$1);
var G__59739 = cljs.core.chunk_rest(seq__59255_59737__$1);
var G__59740 = c__5673__auto___59738;
var G__59741 = cljs.core.count(c__5673__auto___59738);
var G__59742 = (0);
seq__59255_59721 = G__59739;
chunk__59256_59722 = G__59740;
count__59257_59723 = G__59741;
i__59258_59724 = G__59742;
continue;
} else {
var vec__59291_59743 = cljs.core.first(seq__59255_59737__$1);
var idx_59744 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59291_59743,(0),null);
var file_59745 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59291_59743,(1),null);
form.append((""+"files["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx_59744)+"]"),(new Blob([new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(file_59745)],({"type": new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(file_59745)}))),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(file_59745));


var G__59749 = cljs.core.next(seq__59255_59737__$1);
var G__59750 = null;
var G__59751 = (0);
var G__59752 = (0);
seq__59255_59721 = G__59749;
chunk__59256_59722 = G__59750;
count__59257_59723 = G__59751;
i__59258_59724 = G__59752;
continue;
}
} else {
}
}
break;
}

return form;
});
knoxx.backend.tools.discord.post_discord_message_chunk_BANG_ = (function knoxx$backend$tools$discord$post_discord_message_chunk_BANG_(token,channel_id,reply_to,file_list,chunk,state){
var form = (new FormData());
var payload = knoxx.backend.tools.discord.discord_message_payload(chunk,reply_to,state);
form.append("payload_json",JSON.stringify(cljs.core.clj__GT_js(payload)));

knoxx.backend.tools.discord.append_discord_files_BANG_(form,file_list);

return fetch((""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages"),({"method": "POST", "headers": ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token))}), "body": form})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"Discord API error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.tools.discord.post_discord_message_chunks_BANG_ = (function knoxx$backend$tools$discord$post_discord_message_chunks_BANG_(token,channel_id,reply_to,file_list,chunks){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (promise,chunk){
return promise.then((function (state){
return knoxx.backend.tools.discord.post_discord_message_chunk_BANG_(token,channel_id,reply_to,file_list,chunk,state);
}));
}),Promise.resolve(null),chunks);
});
knoxx.backend.tools.discord.discord_send_message_BANG_ = (function knoxx$backend$tools$discord$discord_send_message_BANG_(runtime,config,channel_id,text,reply_to,attachment_urls){
var raw_text = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var map__59337 = knoxx.backend.tools.discord.extract_inline_svg_code_blocks(raw_text);
var map__59337__$1 = cljs.core.__destructure_map(map__59337);
var text__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59337__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var attachmentUrls = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59337__$1,new cljs.core.Keyword(null,"attachmentUrls","attachmentUrls",1857227267));
var extracted_urls = (((((!(clojure.string.blank_QMARK_(text__$1)))) && (clojure.string.includes_QMARK_(text__$1,"data:image/"))))?cljs.core.vec(cljs.core.re_seq(/data:image\/[^;]+;base64,[A-Za-z0-9+\/=]+/,text__$1)):null);
var text_for_discord = ((cljs.core.seq(extracted_urls))?cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (txt,url){
return clojure.string.replace(txt,url,"[image]");
}),text__$1,extracted_urls):text__$1);
var normalized = clojure.string.trim(text_for_discord);
var all_urls = cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = attachment_urls;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(function (){var or__5142__auto__ = attachmentUrls;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([extracted_urls], 0)));
if(clojure.string.blank_QMARK_(channel_id)){
throw (new Error("channel_id is required"));
} else {
}

if(((clojure.string.blank_QMARK_(normalized)) && (cljs.core.empty_QMARK_(all_urls)))){
throw (new Error("text or attachment_urls is required"));
} else {
}

return Promise.all(cljs.core.clj__GT_js(cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,url){
return knoxx.backend.tools.discord.fetch_discord_upload_attachment_BANG_(runtime,config,url,idx).then(knoxx.backend.tools.discord.maybe_render_svg_BANG_);
}),all_urls))).then((function (files){
var file_list = cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(files));
return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
var chunks = knoxx.backend.tools.discord.discord_message_chunks(normalized);
return knoxx.backend.tools.discord.post_discord_message_chunks_BANG_(token,channel_id,reply_to,file_list,chunks).then((function (result){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"messageId","messageId",-260575736),(function (){var or__5142__auto__ = (result["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"sent","sent",-1537501490),true,new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(function (){var or__5142__auto__ = (result["timestamp"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),cljs.core.count(chunks),new cljs.core.Keyword(null,"attachmentCount","attachmentCount",2014220145),cljs.core.count(file_list)], null);
}));
}));
}));
});
/**
 * Add an emoji reaction to a Discord message.
 */
knoxx.backend.tools.discord.discord_react_BANG_ = (function knoxx$backend$tools$discord$discord_react_BANG_(runtime,channel_id,message_id,emoji){
if(clojure.string.blank_QMARK_(channel_id)){
throw (new Error("channel_id is required"));
} else {
}

if(clojure.string.blank_QMARK_(message_id)){
throw (new Error("message_id is required"));
} else {
}

if(clojure.string.blank_QMARK_(emoji)){
throw (new Error("emoji is required"));
} else {
}

var encoded_emoji = encodeURIComponent(emoji);
return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return fetch((""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)+"/reactions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encoded_emoji)+"/@me"),({"method": "PUT", "headers": ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token))})})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"messageId","messageId",-260575736),message_id,new cljs.core.Keyword(null,"emoji","emoji",1031230144),emoji,new cljs.core.Keyword(null,"reacted","reacted",523485502),true], null);
} else {
return resp.text().then((function (text){
throw (new Error((""+"Discord API error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
}));
});
/**
 * Create a thread in a channel or from a message.
 */
knoxx.backend.tools.discord.discord_thread_create_BANG_ = (function knoxx$backend$tools$discord$discord_thread_create_BANG_(runtime,channel_id,message_id,name,auto_archive_duration){
if(clojure.string.blank_QMARK_(channel_id)){
throw (new Error("channel_id is required"));
} else {
}

if(clojure.string.blank_QMARK_(name)){
throw (new Error("name is required"));
} else {
}

var url = ((clojure.string.blank_QMARK_(message_id))?(""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/threads"):(""+"https://discord.com/api/v10/channels/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"/messages/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)+"/threads"));
var body = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"auto_archive_duration","auto_archive_duration",-1199207961),(function (){var or__5142__auto__ = auto_archive_duration;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1440);
}
})(),new cljs.core.Keyword(null,"type","type",1174270348),(11)], null));
return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return fetch(url,({"method": "POST", "headers": knoxx.backend.tools.discord.discord_rest_headers(token), "body": JSON.stringify(body)}));
})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"Discord API error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
})).then((function (result){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"threadId","threadId",-440699805),(function (){var or__5142__auto__ = (result["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"messageId","messageId",-260575736),(function (){var or__5142__auto__ = message_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"created","created",-704993748),true], null);
}));
});
knoxx.backend.tools.discord.discord_list_guilds_BANG_ = (function knoxx$backend$tools$discord$discord_list_guilds_BANG_(runtime){
return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return knoxx.backend.tools.discord.discord_fetch_json_BANG_("https://discord.com/api/v10/users/@me/guilds",({"method": "GET", "headers": knoxx.backend.tools.discord.discord_rest_headers(token)}));
})).then((function (payload){
var servers = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (guild){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(function (){var or__5142__auto__ = (guild["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = (guild["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"memberCount","memberCount",-1747037792),(guild["approximate_member_count"])], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"servers","servers",1881102005),servers,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(servers)], null);
}));
});
knoxx.backend.tools.discord.text_channel_type_QMARK_ = (function knoxx$backend$tools$discord$text_channel_type_QMARK_(channel){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [(0),null,(12),null,(11),null,(5),null], null), null),(channel["type"]));
});
knoxx.backend.tools.discord.discord_list_guild_channels_BANG_ = (function knoxx$backend$tools$discord$discord_list_guild_channels_BANG_(runtime,guild_id){
return knoxx.backend.tools.discord.discord_token_BANG_(runtime).then((function (token){
return knoxx.backend.tools.discord.discord_fetch_json_BANG_((""+"https://discord.com/api/v10/guilds/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)+"/channels"),({"method": "GET", "headers": knoxx.backend.tools.discord.discord_rest_headers(token)}));
})).then((function (payload){
var channels = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (channel){
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
return "";
}
})(),new cljs.core.Keyword(null,"guildId","guildId",-559818490),guild_id,new cljs.core.Keyword(null,"type","type",1174270348),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (channel["type"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))], null);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.discord.text_channel_type_QMARK_,(cljs.core.truth_(cljs.core.array_QMARK_(payload))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(payload):cljs.core.PersistentVector.EMPTY)));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channels","channels",1132759174),channels,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(channels)], null);
}));
});
knoxx.backend.tools.discord.discord_list_channels_BANG_ = (function knoxx$backend$tools$discord$discord_list_channels_BANG_(runtime,guild_id){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = guild_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
return knoxx.backend.tools.discord.discord_list_guilds_BANG_(runtime).then((function (result){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (server){
return knoxx.backend.tools.discord.discord_list_guild_channels_BANG_(runtime,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(server));
}),new cljs.core.Keyword(null,"servers","servers",1881102005).cljs$core$IFn$_invoke$arity$1(result)))).then((function (payloads){
var channels = cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(new cljs.core.Keyword(null,"channels","channels",1132759174),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.http.js_array_seq(payloads)], 0)));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channels","channels",1132759174),channels,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(channels)], null);
}));
}));
} else {
return knoxx.backend.tools.discord.discord_list_guild_channels_BANG_(runtime,guild_id);
}
});
knoxx.backend.tools.discord.strip_path_delims = (function knoxx$backend$tools$discord$strip_path_delims(s){
return clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = s;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/<\\|\"|\"[|>]/,""));
});
knoxx.backend.tools.discord.discord_send_execute = (function knoxx$backend$tools$discord$discord_send_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var text = (function (){var or__5142__auto__ = (params["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["content"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var reply_to = (function (){var or__5142__auto__ = (params["reply_to"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["replyTo"]);
}
})();
var attachment_urls = cljs.core.clj__GT_js(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.discord.strip_path_delims,knoxx.backend.http.js_array_seq((function (){var or__5142__auto__ = (params["attachment_urls"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["attachmentUrls"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return [];
}
}
})()))));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Sending Discord message to "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\u2026"));

return knoxx.backend.tools.discord.discord_send_message_BANG_(runtime,config,channel_id,text,reply_to,attachment_urls).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Sent Discord message "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(result))+" to channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)),result);
}));
});
knoxx.backend.tools.discord.send_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to send the message to. Use discord.list.channels to discover IDs."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Message content to send. Long messages will be chunked automatically."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reply_to","reply_to",64284531),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional message ID to reply to."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"attachment_urls","attachment_urls",178655562),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null)], null);
knoxx.backend.tools.discord.discord_send_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.send","Discord Send","Send a message to a Discord channel, optionally as a reply to an existing message.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Send a Discord message or reply to a specific message id.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.publish or discord.send to share results in a Discord channel.","Provide channelId/channel_id and content/text.","Include attachmentUrls/attachment_urls to upload files, images, or generated assets.","Pass file paths as plain strings (e.g. Graphics/seal.svg or Voice/clip.mp3). Do NOT wrap them in <|\"| delimiters.","To mention a user, use <@user_id> in the text. Do NOT use @username \u2014 it will not ping."], null)),knoxx.backend.tools.discord.send_params,knoxx.backend.tools.discord.discord_send_execute], 0));
knoxx.backend.tools.discord.channel_messages_params = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to fetch messages from."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of messages to fetch (default 50, max 100)."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(100)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"before","before",-1633692388),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch messages before this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"after","after",594996914),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch messages after this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"around","around",-265975553),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch messages around this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord.channel_messages_execute = (function knoxx$backend$tools$discord$channel_messages_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Fetching Discord messages from channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\u2026"));

return knoxx.backend.tools.discord.discord_fetch_channel_messages_BANG_(runtime,config,channel_id,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(params["limit"]),new cljs.core.Keyword(null,"before","before",-1633692388),(params["before"]),new cljs.core.Keyword(null,"after","after",594996914),(params["after"]),new cljs.core.Keyword(null,"around","around",-265975553),(params["around"])], null)).then((function (result){
return knoxx.backend.tools.discord.attach_openplanner_labels_BANG_(config,new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(result)).then((function (messages){
var filtered = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"messages","messages",345434482),messages,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(messages)], 0));
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.discord.discord_messages_text((""+"Fetched "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"count","count",2139924085).cljs$core$IFn$_invoke$arity$1(filtered))+" non-bad messages from channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"."),messages),filtered);
}));
}));
});
knoxx.backend.tools.discord.channel_messages_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.channel.messages","Discord Channel Messages","Fetch messages from a Discord channel with before/after/around cursors.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Fetch channel messages from Discord with pagination cursors when you need exact transcript context.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this when you know the channel id and need exact message history.","Use before/after/around for precise pagination."], null)),knoxx.backend.tools.discord.channel_messages_params,knoxx.backend.tools.discord.channel_messages_execute], 0));
knoxx.backend.tools.discord.channel_scroll_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to fetch older messages from."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"oldest_seen_id","oldest_seen_id",-1348923865),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Oldest message ID already seen; fetch messages before this."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of older messages to fetch."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(100)], null)], null)], null)], null);
knoxx.backend.tools.discord.channel_scroll_execute = (function knoxx$backend$tools$discord$channel_scroll_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var oldest_seen_id = (function (){var or__5142__auto__ = (params["oldest_seen_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["oldestSeenId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Scrolling older Discord messages in channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\u2026"));

return knoxx.backend.tools.discord.discord_scroll_channel_messages_BANG_(runtime,config,channel_id,oldest_seen_id,(params["limit"])).then((function (result){
return knoxx.backend.tools.discord.attach_openplanner_labels_BANG_(config,new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(result)).then((function (messages){
var filtered = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"messages","messages",345434482),messages,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(messages)], 0));
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.discord.discord_messages_text((""+"Fetched older non-bad messages before "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(oldest_seen_id)+"."),messages),filtered);
}));
}));
});
knoxx.backend.tools.discord.channel_scroll_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.channel.scroll","Discord Channel Scroll","Scroll older channel messages by fetching messages before the oldest already-seen message id.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Scroll backwards in a Discord channel once you already know the oldest seen id.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id.","Useful for paging backward through long histories."], null)),knoxx.backend.tools.discord.channel_scroll_params,knoxx.backend.tools.discord.channel_scroll_execute], 0));
knoxx.backend.tools.discord.dm_messages_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord user ID whose DM channel should be read."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of DM messages to fetch."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(100)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"before","before",-1633692388),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch DM messages before this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord.dm_messages_execute = (function knoxx$backend$tools$discord$dm_messages_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var user_id = (function (){var or__5142__auto__ = (params["user_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["userId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Fetching Discord DM messages for user "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id)+"\u2026"));

return knoxx.backend.tools.discord.discord_fetch_dm_messages_BANG_(runtime,config,user_id,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(params["limit"]),new cljs.core.Keyword(null,"before","before",-1633692388),(params["before"])], null)).then((function (result){
return knoxx.backend.tools.discord.attach_openplanner_labels_BANG_(config,new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(result)).then((function (messages){
var filtered = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"messages","messages",345434482),messages,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(messages)], 0));
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.discord.discord_messages_text((""+"Fetched "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"count","count",2139924085).cljs$core$IFn$_invoke$arity$1(filtered))+" non-bad DM messages for user "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id)+"."),messages),filtered);
}));
}));
});
knoxx.backend.tools.discord.dm_messages_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.dm.messages","Discord DM Messages","Fetch messages from the DM channel shared with a Discord user.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read DM history with a Discord user by user id.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this when the relevant conversation is in DMs rather than a guild channel.","Provide the target user id."], null)),knoxx.backend.tools.discord.dm_messages_params,knoxx.backend.tools.discord.dm_messages_execute], 0));
knoxx.backend.tools.discord.search_params = new cljs.core.PersistentVector(null, 9, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"scope","scope",-439358418),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Search scope: channel or dm."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to search when scope=channel."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Discord user ID to search against when scope=dm or to filter author."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional substring query to filter messages by content."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of matching messages to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(100)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"before","before",-1633692388),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch messages before this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"after","after",594996914),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Fetch messages after this message ID."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"since_hours","since_hours",-3911670),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Prefer matching messages within this many hours (default 168); pass a larger value to override the timeframe."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),(1)], null)], null)], null)], null);
knoxx.backend.tools.discord.search_execute = (function knoxx$backend$tools$discord$search_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var scope = (function (){var or__5142__auto__ = (params["scope"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "channel";
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["channelId"]);
}
})();
var user_id = (function (){var or__5142__auto__ = (params["user_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["userId"]);
}
})();
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Searching Discord messages in scope "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(scope)+"\u2026"));

return knoxx.backend.tools.discord.discord_search_messages_BANG_(runtime,config,scope,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"channel-id","channel-id",138191095),channel_id,new cljs.core.Keyword(null,"user-id","user-id",-206822291),user_id,new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"limit","limit",-1355822363),(params["limit"]),new cljs.core.Keyword(null,"before","before",-1633692388),(params["before"]),new cljs.core.Keyword(null,"after","after",594996914),(params["after"]),new cljs.core.Keyword(null,"since-hours","since-hours",124306716),(function (){var or__5142__auto__ = (params["since_hours"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["sinceHours"]);
}
})()], null)).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.discord.discord_messages_text((""+"Found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"count","count",2139924085).cljs$core$IFn$_invoke$arity$1(result))+" matching Discord messages."),new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(result)),result);
}));
});
knoxx.backend.tools.discord.search_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.search","Discord Search","Search channel or DM messages by content and/or author using client-side filtering.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search Discord messages by text and scope to find relevant discussion quickly.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs.","Messages marked bad in OpenPlanner labels are never shown.","Matching good-marked messages are returned first in chronological order, then unbad messages chronologically.","The default timeframe is 168 hours; pass since_hours to override when needed.","This falls back to client-side filtering when native search is unavailable."], null)),knoxx.backend.tools.discord.search_params,knoxx.backend.tools.discord.search_execute], 0));
knoxx.backend.tools.discord.list_servers_params = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461)], null);
knoxx.backend.tools.discord.list_servers_execute = (function knoxx$backend$tools$discord$list_servers_execute(runtime,config,_tool_call_id,_params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Listing Discord servers\u2026");

return knoxx.backend.tools.discord.discord_list_guilds_BANG_(runtime).then((function (result){
var lines = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (server){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(server))+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(server))+")");
}),new cljs.core.Keyword(null,"servers","servers",1881102005).cljs$core$IFn$_invoke$arity$1(result)));
return knoxx.backend.text.tool_text_result((""+"Discord servers:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(lines)),result);
}));
});
knoxx.backend.tools.discord.list_servers_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.list.servers","Discord List Servers","List all Discord servers/guilds the bot can access.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["List Discord servers before choosing channels or replying into a guild.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this before discord.list.channels when you need discovery.","Do not guess guild ids."], null)),knoxx.backend.tools.discord.list_servers_params,knoxx.backend.tools.discord.list_servers_execute], 0));
knoxx.backend.tools.discord.guilds_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.guilds","Discord Guilds","List Discord guilds/servers the bot is in.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["List Discord guilds to discover available servers.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alias for discord.list.servers.","Use before listing channels or posting to a specific server."], null)),knoxx.backend.tools.discord.list_servers_params,knoxx.backend.tools.discord.list_servers_execute], 0));
knoxx.backend.tools.discord.list_channels_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional guild/server ID. If omitted, returns channels across all visible guilds."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord.list_channels_execute = (function knoxx$backend$tools$discord$list_channels_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var guild_id = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["guildId"]);
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Listing Discord channels\u2026");

return knoxx.backend.tools.discord.discord_list_channels_BANG_(runtime,guild_id).then((function (result){
var lines = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (channel){
return (""+"#"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(channel))+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(channel))+") guild="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(channel)));
}),new cljs.core.Keyword(null,"channels","channels",1132759174).cljs$core$IFn$_invoke$arity$1(result)));
return knoxx.backend.text.tool_text_result((""+"Discord channels:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(lines)),result);
}));
});
knoxx.backend.tools.discord.list_channels_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.list.channels","Discord List Channels","List channels in one Discord guild or across all visible guilds.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["List Discord channels to discover readable/postable targets.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["If guild_id is omitted, returns channels across all visible guilds.","Use returned channel ids with discord.channel.messages or discord.send."], null)),knoxx.backend.tools.discord.list_channels_params,knoxx.backend.tools.discord.list_channels_execute], 0));
knoxx.backend.tools.discord.channels_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord guild ID to list channels for."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord.channels_execute = (function knoxx$backend$tools$discord$channels_execute(runtime,config,tool_call_id,params,a,b,c){
return knoxx.backend.tools.discord.list_channels_execute(runtime,config,tool_call_id,({"guild_id": (params["guildId"])}),a,b,c);
});
knoxx.backend.tools.discord.channels_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.channels","Discord Channels","List channels in a Discord guild.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["List channels in a Discord guild to find the right channel for reading or posting.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alias for discord.list.channels.","Use guildId/guild_id when you already know the server."], null)),knoxx.backend.tools.discord.channels_params,knoxx.backend.tools.discord.channels_execute], 0));
knoxx.backend.tools.discord.react_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID containing the message to react to."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord message ID to react to."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"emoji","emoji",1031230144),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Emoji to react with (e.g. \uD83D\uDC4D, \uD83C\uDF89, \uD83D\uDC80)."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord.react_execute = (function knoxx$backend$tools$discord$react_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var message_id = (function (){var or__5142__auto__ = (params["message_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["messageId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var emoji = (function (){var or__5142__auto__ = (params["emoji"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Reacting to message "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)+" with "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(emoji)+"\u2026"));

return knoxx.backend.tools.discord.discord_react_BANG_(runtime,channel_id,message_id,emoji).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Reacted with "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(emoji)+" to message "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)),result);
}));
});
knoxx.backend.tools.discord.react_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.react","Discord React","Add an emoji reaction to a Discord message.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["React to a Discord message with an emoji.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.react to add emoji reactions to messages.","Provide channel_id, message_id, and an emoji (e.g. \uD83D\uDC4D, \uD83C\uDF89, \uD83D\uDC80)."], null)),knoxx.backend.tools.discord.react_params,knoxx.backend.tools.discord.react_execute], 0));
knoxx.backend.tools.discord.thread_create_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to create the thread in."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"message_id","message_id",663757010),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional message ID to create a thread from. If omitted, creates a standalone thread in the channel."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Name of the thread (max 100 chars)."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auto_archive_duration","auto_archive_duration",-1199207961),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Auto-archive duration in minutes: 60, 1440 (default), 4320, or 10080."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null);
knoxx.backend.tools.discord.thread_create_execute = (function knoxx$backend$tools$discord$thread_create_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var channel_id = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var message_id = (function (){var or__5142__auto__ = (params["message_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["messageId"]);
}
})();
var name = (function (){var or__5142__auto__ = (params["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var auto_archive = (params["auto_archive_duration"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Creating thread '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"' in channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\u2026"));

return knoxx.backend.tools.discord.discord_thread_create_BANG_(runtime,channel_id,message_id,name,auto_archive).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Created thread "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"threadId","threadId",-440699805).cljs$core$IFn$_invoke$arity$1(result))+" named '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"'"),result);
}));
});
knoxx.backend.tools.discord.thread_create_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.thread.create","Discord Thread Create","Create a Discord thread from a message or in a channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Create a thread to spin off a conversation.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.thread.create to start a thread from a message or in a channel.","Provide channel_id and a name. Optionally pass message_id to create a thread from that message.","After creating a thread, use the returned threadId as channel_id with discord.send to post in it."], null)),knoxx.backend.tools.discord.thread_create_params,knoxx.backend.tools.discord.thread_create_execute], 0));
knoxx.backend.tools.discord.publish_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to post the message to."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Message content to post to the Discord channel."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"attachment_urls","attachment_urls",178655562),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null)], null);
knoxx.backend.tools.discord.publish_execute = (function knoxx$backend$tools$discord$publish_execute(runtime,config,tool_call_id,params,a,b,c){
return knoxx.backend.tools.discord.discord_send_execute(runtime,config,tool_call_id,({"channel_id": (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(), "text": (function (){var or__5142__auto__ = (params["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["text"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(), "attachment_urls": (function (){var or__5142__auto__ = (params["attachment_urls"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["attachmentUrls"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return [];
}
}
})()}),a,b,c);
});
knoxx.backend.tools.discord.publish_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.publish","Discord Publish","Post a message to a Discord channel using the configured Knoxx Discord bot.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Post updates, summaries, or notifications to Discord channels.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.publish or discord.send to share results in a Discord channel.","Provide channelId/channel_id and content/text.","Include attachmentUrls/attachment_urls to upload files, images, or generated assets.","To mention a user, use <@user_id> in the text. Do NOT use @username \u2014 it will not ping."], null)),knoxx.backend.tools.discord.publish_params,knoxx.backend.tools.discord.publish_execute], 0));
knoxx.backend.tools.discord.read_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord channel ID to read messages from."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of messages to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(100)], null)], null)], null)], null);
knoxx.backend.tools.discord.read_execute = (function knoxx$backend$tools$discord$read_execute(runtime,config,tool_call_id,params,a,b,c){
return knoxx.backend.tools.discord.channel_messages_execute(runtime,config,tool_call_id,({"channel_id": (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(), "limit": (params["limit"])}),a,b,c);
});
knoxx.backend.tools.discord.read_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.read","Discord Read","Read recent messages from a Discord channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read recent messages from a Discord channel to understand context.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.read as a simple alias for discord.channel.messages.","For pagination or cursors, use discord.channel.messages or discord.channel.scroll directly."], null)),knoxx.backend.tools.discord.read_params,knoxx.backend.tools.discord.read_execute], 0));
knoxx.backend.tools.discord.create_discord_custom_tools = (function knoxx$backend$tools$discord$create_discord_custom_tools(var_args){
var G__59635 = arguments.length;
switch (G__59635) {
case 2:
return knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("discord.publish"))?knoxx.backend.tools.discord.publish_tool(runtime,config):null),((allowed_QMARK_("discord.send"))?knoxx.backend.tools.discord.discord_send_tool(runtime,config):null),((allowed_QMARK_("discord.react"))?knoxx.backend.tools.discord.react_tool(runtime,config):null),((allowed_QMARK_("discord.thread.create"))?knoxx.backend.tools.discord.thread_create_tool(runtime,config):null),((allowed_QMARK_("discord.read"))?knoxx.backend.tools.discord.read_tool(runtime,config):null),((allowed_QMARK_("discord.channel.messages"))?knoxx.backend.tools.discord.channel_messages_tool(runtime,config):null),((allowed_QMARK_("discord.channel.scroll"))?knoxx.backend.tools.discord.channel_scroll_tool(runtime,config):null),((allowed_QMARK_("discord.dm.messages"))?knoxx.backend.tools.discord.dm_messages_tool(runtime,config):null),((allowed_QMARK_("discord.search"))?knoxx.backend.tools.discord.search_tool(runtime,config):null),((allowed_QMARK_("discord.guilds"))?knoxx.backend.tools.discord.guilds_tool(runtime,config):null),((allowed_QMARK_("discord.list.servers"))?knoxx.backend.tools.discord.list_servers_tool(runtime,config):null),((allowed_QMARK_("discord.channels"))?knoxx.backend.tools.discord.channels_tool(runtime,config):null),((allowed_QMARK_("discord.list.channels"))?knoxx.backend.tools.discord.list_channels_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.discord.create_discord_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.discord.js.map
