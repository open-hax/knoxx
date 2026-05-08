import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.actor_mailbox.js";
import "./knoxx.backend.agent_context.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.actors');
knoxx.backend.tools.actors.send_message_params = new cljs.core.PersistentVector(null, 9, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"target","target",253001721),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Target address: parent, self, conversation:<id>, conversation-id:<id>, session:<id>, session-id:<id>, actor:<id>, actor-id:<id>, or a raw conversation/session id when target_type is provided."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Message content to deliver."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Delivery mode: message (default follow-up), steer, follow-up, or event."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"target_type","target_type",-66014761),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional hint for raw target: parent, self, conversation, session, actor, or event."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Explicit conversation id override."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Explicit session id override."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional target/current run id for audit linkage."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata_json","metadata_json",2009778443),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional JSON object with lineage/audit metadata, e.g. parentRunId or subAgentId."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.actors.self_headers = (function knoxx$backend$tools$actors$self_headers(config){
var api_key = new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154).cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.shared.live_config(config));
var G__58765 = ({"Content-Type": "application/json", "x-knoxx-user-email": "system-admin@open-hax.local"});
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key)))))){
return (G__58765["X-API-Key"] = api_key);
} else {
return G__58765;
}
});
knoxx.backend.tools.actors.api_base = (function knoxx$backend$tools$actors$api_base(config){
var or__5142__auto__ = new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.shared.live_config(config));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://127.0.0.1:8000";
}
});
knoxx.backend.tools.actors.fetch_json_BANG_ = (function knoxx$backend$tools$actors$fetch_json_BANG_(config,method,path,body){
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.actors.api_base(config))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)),({"method": method, "headers": knoxx.backend.tools.actors.self_headers(config), "body": (cljs.core.truth_(body)?JSON.stringify(cljs.core.clj__GT_js(body)):null)})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.tools.actors.normalize_mode = (function knoxx$backend$tools$actors$normalize_mode(mode){
var mode_STAR_ = (function (){var G__58793 = mode;
var G__58793__$1 = (((G__58793 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58793)));
var G__58793__$2 = (((G__58793__$1 == null))?null:clojure.string.trim(G__58793__$1));
var G__58793__$3 = (((G__58793__$2 == null))?null:clojure.string.lower_case(G__58793__$2));
if((G__58793__$3 == null)){
return null;
} else {
return clojure.string.replace(G__58793__$3,/_/,"-");
}
})();
var G__58797 = mode_STAR_;
switch (G__58797) {
case "steer":
return "steer";

break;
case "follow-up":
return "follow-up";

break;
case "followup":
return "follow-up";

break;
case "event":
return "event";

break;
case "message":
return "message";

break;
default:
return "message";

}
});
knoxx.backend.tools.actors.normalize_target_type = (function knoxx$backend$tools$actors$normalize_target_type(target_type){
var G__58798 = target_type;
var G__58798__$1 = (((G__58798 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58798)));
var G__58798__$2 = (((G__58798__$1 == null))?null:clojure.string.trim(G__58798__$1));
var G__58798__$3 = (((G__58798__$2 == null))?null:clojure.string.lower_case(G__58798__$2));
var G__58798__$4 = (((G__58798__$3 == null))?null:clojure.string.replace(G__58798__$3,/_/,"-"));
if((G__58798__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__58798__$4);
}
});
knoxx.backend.tools.actors.parse_metadata = (function knoxx$backend$tools$actors$parse_metadata(metadata_json){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(metadata_json)))){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return knoxx.backend.tools.shared.json_parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(metadata_json)));
}
});
knoxx.backend.tools.actors.nonblank = (function knoxx$backend$tools$actors$nonblank(value){
var G__58817 = value;
var G__58817__$1 = (((G__58817 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58817)));
var G__58817__$2 = (((G__58817__$1 == null))?null:clojure.string.trim(G__58817__$1));
if((G__58817__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__58817__$2);
}
});
knoxx.backend.tools.actors.prefixed_target = (function knoxx$backend$tools$actors$prefixed_target(target){
var t = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target)));
var idx = t.indexOf(":");
if((idx > (0))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),knoxx.backend.tools.actors.normalize_target_type(t.slice((0),idx)),new cljs.core.Keyword(null,"id","id",-1388402092),knoxx.backend.tools.actors.nonblank(t.slice((idx + (1))))], null);
} else {
return null;
}
});
knoxx.backend.tools.actors.current_context = (function knoxx$backend$tools$actors$current_context(){
var or__5142__auto__ = knoxx.backend.agent_context.get_context();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.tools.actors.parent_context = (function knoxx$backend$tools$actors$parent_context(ctx,metadata){
var agent_spec = new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(ctx);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentConversationId","parentConversationId",-64718550).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_conversation_id","parent_conversation_id",-882361166).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentConversationId","parentConversationId",-64718550).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_conversation_id","parent_conversation_id",-882361166).cljs$core$IFn$_invoke$arity$1(agent_spec));
}
}
}
}
}
})(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentSessionId","parentSessionId",1674230329).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_session_id","parent_session_id",-44051626).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentSessionId","parentSessionId",1674230329).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_session_id","parent_session_id",-44051626).cljs$core$IFn$_invoke$arity$1(agent_spec));
}
}
}
}
}
})(),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_run_id","parent_run_id",-2071531014).cljs$core$IFn$_invoke$arity$1(metadata));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return knoxx.backend.tools.actors.nonblank(new cljs.core.Keyword(null,"parent_run_id","parent_run_id",-2071531014).cljs$core$IFn$_invoke$arity$1(agent_spec));
}
}
}
}
}
})()], null);
});
knoxx.backend.tools.actors.resolve_target_sync = (function knoxx$backend$tools$actors$resolve_target_sync(params,metadata){
var ctx = knoxx.backend.tools.actors.current_context();
var target = knoxx.backend.tools.actors.nonblank((function (){var or__5142__auto__ = (params["target"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "self";
}
})());
var type_hint = knoxx.backend.tools.actors.normalize_target_type((params["target_type"]));
var parsed = knoxx.backend.tools.actors.prefixed_target(target);
var target_type = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return type_hint;
}
})();
var target_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.truth_((function (){var fexpr__58853 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["parent",null,"event",null,"self",null], null), null);
return (fexpr__58853.cljs$core$IFn$_invoke$arity$1 ? fexpr__58853.cljs$core$IFn$_invoke$arity$1(target) : fexpr__58853.call(null,target));
})())){
return null;
} else {
return target;
}
}
})();
var explicit_conversation_id = knoxx.backend.tools.actors.nonblank((function (){var or__5142__auto__ = (params["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["conversationId"]);
}
})());
var explicit_session_id = knoxx.backend.tools.actors.nonblank((function (){var or__5142__auto__ = (params["session_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["sessionId"]);
}
})());
var explicit_run_id = knoxx.backend.tools.actors.nonblank((function (){var or__5142__auto__ = (params["run_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["runId"]);
}
})());
var parent = knoxx.backend.tools.actors.parent_context(ctx,metadata);
var G__58858 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"target","target",253001721),target,new cljs.core.Keyword(null,"target-type","target-type",-1795727181),target_type,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),explicit_conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),explicit_session_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = explicit_run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(ctx);
}
})()], null);
var G__58858__$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(target,"self"))?cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = explicit_conversation_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(ctx);
}
})(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = explicit_session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(ctx);
}
})(),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = explicit_run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(ctx);
}
})()], null)], 0)):G__58858);
var G__58858__$2 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(target,"parent"))?cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858__$1,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = explicit_conversation_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(parent);
}
})(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = explicit_session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(parent);
}
})(),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = explicit_run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(parent);
}
})()], null)], 0)):G__58858__$1);
var G__58858__$3 = (cljs.core.truth_((function (){var fexpr__58884 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["conversation",null,"conversationid",null,"conversation-id",null], null), null);
return (fexpr__58884.cljs$core$IFn$_invoke$arity$1 ? fexpr__58884.cljs$core$IFn$_invoke$arity$1(target_type) : fexpr__58884.call(null,target_type));
})())?cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858__$2,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = explicit_conversation_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return target_id;
}
})()], null)], 0)):G__58858__$2);
var G__58858__$4 = (cljs.core.truth_((function (){var fexpr__58885 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["sessionid",null,"session-id",null,"session",null], null), null);
return (fexpr__58885.cljs$core$IFn$_invoke$arity$1 ? fexpr__58885.cljs$core$IFn$_invoke$arity$1(target_type) : fexpr__58885.call(null,target_type));
})())?cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858__$3,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = explicit_session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return target_id;
}
})()], null)], 0)):G__58858__$3);
var G__58858__$5 = (cljs.core.truth_((function (){var fexpr__58887 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["actorid",null,"actor-id",null,"actor",null], null), null);
return (fexpr__58887.cljs$core$IFn$_invoke$arity$1 ? fexpr__58887.cljs$core$IFn$_invoke$arity$1(target_type) : fexpr__58887.call(null,target_type));
})())?cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858__$4,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),target_id], null)], 0)):G__58858__$4);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",target_type)){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__58858__$5,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"event-target","event-target",1020690123),target_id], null)], 0));
} else {
return G__58858__$5;
}
});
knoxx.backend.tools.actors.resolve_session_conversation_BANG_ = (function knoxx$backend$tools$actors$resolve_session_conversation_BANG_(target){
var session_id = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target);
var conversation_id = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target);
if(cljs.core.truth_((function (){var or__5142__auto__ = conversation_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)));
}
})())){
return Promise.resolve(target);
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(target,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(session);
}
})());
}));
}
});
knoxx.backend.tools.actors.delivery_mode = (function knoxx$backend$tools$actors$delivery_mode(mode){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("message",mode)){
return "follow-up";
} else {
return mode;
}
});
knoxx.backend.tools.actors.control_path = (function knoxx$backend$tools$actors$control_path(mode){
var G__58890 = knoxx.backend.tools.actors.delivery_mode(mode);
switch (G__58890) {
case "steer":
return "/api/knoxx/steer";

break;
case "follow-up":
return "/api/knoxx/follow-up";

break;
default:
return "/api/knoxx/follow-up";

}
});
knoxx.backend.tools.actors.event_payload = (function knoxx$backend$tools$actors$event_payload(target,content,metadata,current,mailbox_id){
var G__58903 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"actor",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"actors.message",new cljs.core.Keyword(null,"payload","payload",-383036092),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"sourceRunId","sourceRunId",-334289727),new cljs.core.Keyword(null,"sourceConversationId","sourceConversationId",-1312528671),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"sourceSessionId","sourceSessionId",228673635),new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"mailboxId","mailboxId",-395830287),new cljs.core.Keyword(null,"runId","runId",505587730),new cljs.core.Keyword(null,"targetType","targetType",1097784409),new cljs.core.Keyword(null,"target","target",253001721),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),new cljs.core.Keyword(null,"metadata","metadata",1799301597)],[new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(current),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(current),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(current),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target),content,mailbox_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"target-type","target-type",-1795727181).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target),metadata])], null);
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mailbox_id)))))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58903,new cljs.core.Keyword(null,"id","id",-1388402092),knoxx.backend.actor_mailbox.mailbox_event_id(mailbox_id));
} else {
return G__58903;
}
});
knoxx.backend.tools.actors.send_event_BANG_ = (function knoxx$backend$tools$actors$send_event_BANG_(config,target,content,metadata,mailbox_id){
return knoxx.backend.tools.actors.fetch_json_BANG_(config,"POST","/api/admin/config/events/dispatch",knoxx.backend.tools.actors.event_payload(target,content,metadata,knoxx.backend.tools.actors.current_context(),mailbox_id));
});
knoxx.backend.tools.actors.send_control_BANG_ = (function knoxx$backend$tools$actors$send_control_BANG_(config,target,mode,content,metadata){
var conversation_id = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target);
var session_id = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target);
var run_id = new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)))){
return Promise.reject((new Error("conversation_id is required for steer/follow-up actor messages")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))){
return Promise.reject((new Error("session_id is required for steer/follow-up actor messages")));
} else {
return knoxx.backend.tools.actors.fetch_json_BANG_(config,"POST",knoxx.backend.tools.actors.control_path(mode),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"message","message",-406056002),content,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"metadata","metadata",1799301597),metadata], null));

}
}
});
knoxx.backend.tools.actors.resolve_actor_route_BANG_ = (function knoxx$backend$tools$actors$resolve_actor_route_BANG_(runtime,target){
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target);
if(cljs.core.truth_(and__5140__auto__)){
return ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target))))) && (clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target))))));
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.actor_mailbox.resolve_actor_session_BANG_(runtime,new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target)).then((function (route){
if(cljs.core.truth_(route)){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([target,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(route),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(route),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(route);
}
})(),new cljs.core.Keyword(null,"resolved-actor-route","resolved-actor-route",-1522711213),route], null)], 0));
} else {
return target;
}
}));
} else {
return Promise.resolve(target);
}
});
knoxx.backend.tools.actors.mailbox_delivery_mode = (function knoxx$backend$tools$actors$mailbox_delivery_mode(mode){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",mode)){
return "event";
} else {
return knoxx.backend.tools.actors.delivery_mode(mode);
}
});
knoxx.backend.tools.actors.create_mailbox_entry_BANG_ = (function knoxx$backend$tools$actors$create_mailbox_entry_BANG_(runtime,target,mode,content,metadata){
var current = knoxx.backend.tools.actors.current_context();
return knoxx.backend.actor_mailbox.create_entry_BANG_(runtime,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"kind","kind",-717265803),"actor-message",new cljs.core.Keyword(null,"status","status",-1997798413),"pending",new cljs.core.Keyword(null,"source","source",-433931539),knoxx.backend.actor_mailbox.source_from_context(current),new cljs.core.Keyword(null,"target","target",253001721),target,new cljs.core.Keyword(null,"delivery-mode","delivery-mode",2042238834),knoxx.backend.tools.actors.mailbox_delivery_mode(mode),new cljs.core.Keyword(null,"content-ref","content-ref",1710065788),(function (){var G__58983 = cljs.core.PersistentArrayMap.EMPTY;
var G__58983__$1 = (cljs.core.truth_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58983,new cljs.core.Keyword(null,"target-run-id","target-run-id",-218529148),new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target)):G__58983);
if(cljs.core.truth_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(current))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58983__$1,new cljs.core.Keyword(null,"source-run-id","source-run-id",-2000058256),new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(current));
} else {
return G__58983__$1;
}
})(),new cljs.core.Keyword(null,"metadata","metadata",1799301597),metadata,new cljs.core.Keyword(null,"preview","preview",451279890),content], null));
});
knoxx.backend.tools.actors.mailbox_id = (function knoxx$backend$tools$actors$mailbox_id(entry){
return new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344).cljs$core$IFn$_invoke$arity$1(entry);
});
knoxx.backend.tools.actors.delivery_content_ref = (function knoxx$backend$tools$actors$delivery_content_ref(mode,result,mailbox_id){
var G__58986 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"delivery-mode","delivery-mode",2042238834),knoxx.backend.tools.actors.mailbox_delivery_mode(mode)], null);
var G__58986__$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",mode))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58986,new cljs.core.Keyword(null,"event-id","event-id",2130210178),knoxx.backend.actor_mailbox.mailbox_event_id(mailbox_id)):G__58986);
if(cljs.core.map_QMARK_(result)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58986__$1,new cljs.core.Keyword(null,"result","result",1415092211),result);
} else {
return G__58986__$1;
}
});
knoxx.backend.tools.actors.mark_failed_and_rethrow_BANG_ = (function knoxx$backend$tools$actors$mark_failed_and_rethrow_BANG_(runtime,mailbox_id,err){
return knoxx.backend.actor_mailbox.mark_failed_BANG_(runtime,mailbox_id,err).catch((function (_){
return null;
})).then((function (_){
throw err;
}));
});
knoxx.backend.tools.actors.actors_send_message_execute = (function knoxx$backend$tools$actors$actors_send_message_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var content = (function (){var or__5142__auto__ = (params["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var mode = knoxx.backend.tools.actors.normalize_mode((params["mode"]));
var metadata = knoxx.backend.tools.actors.parse_metadata((function (){var or__5142__auto__ = (params["metadata_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["metadataJson"]);
}
})());
var target = knoxx.backend.tools.actors.resolve_target_sync(params,metadata);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content)))){
throw (new Error("content is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Sending actor message via "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mode)+"\u2026"));

return knoxx.backend.tools.actors.resolve_session_conversation_BANG_(target).then((function (session_target){
return knoxx.backend.tools.actors.resolve_actor_route_BANG_(runtime,session_target);
})).then((function (resolved_target){
return knoxx.backend.tools.actors.create_mailbox_entry_BANG_(runtime,resolved_target,mode,content,metadata).then((function (entry){
var mailbox_id = knoxx.backend.tools.actors.mailbox_id(entry);
var metadata_STAR_ = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(metadata,new cljs.core.Keyword(null,"mailboxId","mailboxId",-395830287),mailbox_id);
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",mode))?knoxx.backend.tools.actors.send_event_BANG_(config,resolved_target,content,metadata_STAR_,mailbox_id):knoxx.backend.tools.actors.send_control_BANG_(config,resolved_target,mode,content,metadata_STAR_)).then((function (result){
var result_STAR_ = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.actor_mailbox.mark_delivered_BANG_(runtime,mailbox_id,knoxx.backend.tools.actors.delivery_content_ref(mode,result_STAR_,mailbox_id)).catch((function (_){
return null;
})).then((function (_){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"entry","entry",505168823),entry,new cljs.core.Keyword(null,"target","target",253001721),resolved_target,new cljs.core.Keyword(null,"result","result",1415092211),result_STAR_], null);
}));
})).catch((function (err){
return knoxx.backend.tools.actors.mark_failed_and_rethrow_BANG_(runtime,mailbox_id,err);
}));
}));
})).then((function (p__59006){
var map__59007 = p__59006;
var map__59007__$1 = cljs.core.__destructure_map(map__59007);
var entry = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59007__$1,new cljs.core.Keyword(null,"entry","entry",505168823));
var target__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59007__$1,new cljs.core.Keyword(null,"target","target",253001721));
var result = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59007__$1,new cljs.core.Keyword(null,"result","result",1415092211));
var summary = (""+"Sent actor message to "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(target__$1))+" via "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("message",mode))?"follow-up":mode)));
return knoxx.backend.text.tool_text_result(summary,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"tool","tool",-1298696470),"actors.send-message",new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"mailbox_id","mailbox_id",1368174469),knoxx.backend.tools.actors.mailbox_id(entry),new cljs.core.Keyword(null,"mailbox_durable","mailbox_durable",-1111734957),cljs.core.boolean$(new cljs.core.Keyword("mailbox","durable?","mailbox/durable?",771149223).cljs$core$IFn$_invoke$arity$1(entry)),new cljs.core.Keyword(null,"target","target",253001721),target__$1,new cljs.core.Keyword(null,"result","result",1415092211),result], null));
}));
});
knoxx.backend.tools.actors.actors_send_message_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"actors.send-message","Actors Send Message","Send an actor-to-actor message to a parent, sibling, session, conversation, actor target, or event queue.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Route asynchronous child-agent progress or results to another actor/session as steer, follow-up, or event while preserving lineage metadata.",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use target=parent from sub-agents when parentConversationId/parentSessionId is present in metadata or agent context.","Use mode=follow-up for busy targets; use mode=steer only for immediate interruption/steering.","Use mode=event when no live session target is available but an auditable mailbox-style event should be emitted.","Pass metadata_json with parentRunId, subAgentId, resultKey, or other lineage labels for auditability."], null),knoxx.backend.tools.actors.send_message_params,knoxx.backend.tools.actors.actors_send_message_execute], 0));
knoxx.backend.tools.actors.create_actors_custom_tools = (function knoxx$backend$tools$actors$create_actors_custom_tools(var_args){
var G__59027 = arguments.length;
switch (G__59027) {
case 2:
return knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
if((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"actors.send-message")))){
return cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.tools.actors.actors_send_message_tool(runtime,config)], null));
} else {
return cljs.core.clj__GT_js(cljs.core.PersistentVector.EMPTY);
}
}));

(knoxx.backend.tools.actors.create_actors_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.actors.js.map
