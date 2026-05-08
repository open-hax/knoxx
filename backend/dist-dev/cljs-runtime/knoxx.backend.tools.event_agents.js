import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.agent_templates.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.event_agents');
knoxx.backend.tools.event_agents.status_params = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461)], null);
knoxx.backend.tools.event_agents.run_job_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"job_id","job_id",-911607906),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Event-agent job id to run immediately."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.event_agents.dispatch_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source_kind","source_kind",-1857411768),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Event source kind such as manual, discord, github, or cron."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"event_kind","event_kind",1009075217),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Event kind string such as manual.note or discord.message.keyword."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload_json","payload_json",1533789905),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional JSON object payload for the event."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.event_agents.upsert_job_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"job_id","job_id",-911607906),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Unique event-agent job id."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"job_json","job_json",-1516738882),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"JSON object describing the event-agent job. Fields may include name, description, enabled, trigger, source, filters, and agentSpec."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.event_agents.spawn_agent_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"User message or task to give the one-off agent."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional model id override."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent_spec_json","agent_spec_json",-968990915),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional JSON object with direct-start style agent_spec fields such as role, contract_id, actor_id, system_prompt, task_prompt, thinking_level, and tool_policies."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.event_agents.self_headers = (function knoxx$backend$tools$event_agents$self_headers(config){
var api_key = new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154).cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.shared.live_config(config));
var G__58769 = ({"Content-Type": "application/json", "x-knoxx-user-email": "system-admin@open-hax.local"});
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key)))))){
return (G__58769["X-API-Key"] = api_key);
} else {
return G__58769;
}
});
knoxx.backend.tools.event_agents.api_base = (function knoxx$backend$tools$event_agents$api_base(config){
var or__5142__auto__ = new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.shared.live_config(config));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://127.0.0.1:8000";
}
});
knoxx.backend.tools.event_agents.fetch_json_BANG_ = (function knoxx$backend$tools$event_agents$fetch_json_BANG_(config,method,path,body){
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.event_agents.api_base(config))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)),({"method": method, "headers": knoxx.backend.tools.event_agents.self_headers(config), "body": (cljs.core.truth_(body)?JSON.stringify(cljs.core.clj__GT_js(body)):null)})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.tools.event_agents.event_agent_status_BANG_ = (function knoxx$backend$tools$event_agents$event_agent_status_BANG_(config){
return knoxx.backend.tools.event_agents.fetch_json_BANG_(config,"GET","/api/admin/config/events",null).then((function (result){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}));
});
knoxx.backend.tools.event_agents.event_agent_dispatch_BANG_ = (function knoxx$backend$tools$event_agents$event_agent_dispatch_BANG_(config,source_kind,event_kind,payload){
return knoxx.backend.tools.event_agents.fetch_json_BANG_(config,"POST","/api/admin/config/events/dispatch",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),source_kind,new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),event_kind,new cljs.core.Keyword(null,"payload","payload",-383036092),payload], null)).then((function (result){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}));
});
knoxx.backend.tools.event_agents.event_agent_upsert_job_BANG_ = (function knoxx$backend$tools$event_agents$event_agent_upsert_job_BANG_(config,job_id,job_patch){
var template_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"templateId","templateId",613248985).cljs$core$IFn$_invoke$arity$1(job_patch);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"template-id","template-id",1952916477).cljs$core$IFn$_invoke$arity$1(job_patch);
}
})();
var next_job = (cljs.core.truth_(template_id)?(function (){var trigger = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(job_patch);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"event",new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),(5),new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),cljs.core.PersistentVector.EMPTY], null);
}
})();
var source = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(job_patch);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"manual",new cljs.core.Keyword(null,"mode","mode",654403691),"respond",new cljs.core.Keyword(null,"config","config",994861415),cljs.core.PersistentArrayMap.EMPTY], null);
}
})();
var filters = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"filters","filters",974726919).cljs$core$IFn$_invoke$arity$1(job_patch);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channels","channels",1132759174),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"keywords","keywords",1526959054),cljs.core.PersistentVector.EMPTY], null);
}
})();
var overrides = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(job_patch,new cljs.core.Keyword(null,"templateId","templateId",613248985),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"template-id","template-id",1952916477),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"filters","filters",974726919)], 0));
return knoxx.backend.agent_templates.instantiate_job.cljs$core$IFn$_invoke$arity$variadic(template_id,job_id,trigger,source,filters,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([overrides], 0));
})():cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([job_patch,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),job_id], null)], 0)));
var normalized_job = knoxx.backend.agent_templates.normalize_job_for_persistence(next_job);
return knoxx.backend.tools.event_agents.event_agent_status_BANG_(config).then((function (status){
var current_control = new cljs.core.Keyword(null,"control","control",1892578036).cljs$core$IFn$_invoke$arity$1(status);
var jobs = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(current_control);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var existing = cljs.core.some((function (p1__58799_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__58799_SHARP_),job_id)){
return p1__58799_SHARP_;
} else {
return null;
}
}),jobs);
var merged_job = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([existing,normalized_job], 0));
var next_jobs = cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [merged_job], null),cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__58801_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__58801_SHARP_),job_id);
}),jobs)));
var next_control = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(current_control,new cljs.core.Keyword(null,"jobs","jobs",-313607120),next_jobs);
return knoxx.backend.tools.event_agents.fetch_json_BANG_(config,"PUT","/api/admin/config/events",next_control).then((function (_){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"job","job",850873087),merged_job,new cljs.core.Keyword(null,"message","message",-406056002),(""+"Upserted job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)),new cljs.core.Keyword(null,"templateId","templateId",613248985),template_id,new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(merged_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429)], null))], null);
}));
}));
});
knoxx.backend.tools.event_agents.event_agent_status_execute = (function knoxx$backend$tools$event_agents$event_agent_status_execute(_runtime,config,_tool_call_id,_params,a,b,c){
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
var result_promise = knoxx.backend.tools.event_agents.event_agent_status_BANG_(config);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Reading event-agent runtime state\u2026");

return result_promise.then((function (result){
var control = new cljs.core.Keyword(null,"control","control",1892578036).cljs$core$IFn$_invoke$arity$1(result);
var runtime_state = new cljs.core.Keyword(null,"runtime","runtime",-1331573996).cljs$core$IFn$_invoke$arity$1(result);
return knoxx.backend.text.tool_text_result((""+"Event-agent runtime running="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"running","running",1554969103).cljs$core$IFn$_invoke$arity$1(runtime_state))+", jobs="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(runtime_state)))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (job){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job))+" :: trigger="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))+" cadence="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405)], null)))+" enabled="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job)));
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control))))),result);
}));
});
knoxx.backend.tools.event_agents.event_agent_run_job_execute = (function knoxx$backend$tools$event_agents$event_agent_run_job_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var job_id = (function (){var or__5142__auto__ = (params["job_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["jobId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(job_id)){
throw (new Error("job_id is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Running event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+"\u2026"));

return knoxx.backend.tools.event_agents.fetch_json_BANG_(config,"POST",(""+"/api/admin/config/events/jobs/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(job_id))+"/run"),null).then((function (_){
return knoxx.backend.text.tool_text_result((""+"Triggered event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"jobId","jobId",1965699355),job_id,new cljs.core.Keyword(null,"ok","ok",967785236),true], null));
}));
});
knoxx.backend.tools.event_agents.event_agent_dispatch_execute = (function knoxx$backend$tools$event_agents$event_agent_dispatch_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var source_kind = (function (){var or__5142__auto__ = (params["source_kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["sourceKind"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "manual";
}
}
})();
var event_kind = (function (){var or__5142__auto__ = (params["event_kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["eventKind"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "manual.event";
}
}
})();
var payload_json = (function (){var or__5142__auto__ = (params["payload_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["payloadJson"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var payload = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(payload_json))))?cljs.core.PersistentArrayMap.EMPTY:knoxx.backend.tools.shared.json_parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(payload_json))));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Dispatching event "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)+"\u2026"));

return knoxx.backend.tools.event_agents.event_agent_dispatch_BANG_(config,source_kind,event_kind,payload).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Dispatched event "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)+" matched jobs: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822).cljs$core$IFn$_invoke$arity$1(result)))),result);
}));
});
knoxx.backend.tools.event_agents.agent_spawn_execute = (function knoxx$backend$tools$event_agents$agent_spawn_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var message = (function (){var or__5142__auto__ = (params["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var model = (function (){var or__5142__auto__ = (params["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})();
var agent_spec_json = (function (){var or__5142__auto__ = (params["agent_spec_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["agentSpecJson"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var agent_spec = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(agent_spec_json))))?cljs.core.PersistentArrayMap.EMPTY:knoxx.backend.tools.shared.json_parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(agent_spec_json))));
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message)))){
throw (new Error("message is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Spawning one-off agent run\u2026");

return knoxx.backend.tools.event_agents.fetch_json_BANG_(config,"POST","/api/knoxx/direct/start",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"message","message",-406056002),message,new cljs.core.Keyword(null,"model","model",331153215),model,new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),agent_spec], null)).then((function (result){
var result_STAR_ = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.text.tool_text_result((""+"Spawned one-off agent run "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(result_STAR_))),result_STAR_);
}));
});
knoxx.backend.tools.event_agents.event_agent_upsert_job_execute = (function knoxx$backend$tools$event_agents$event_agent_upsert_job_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var job_id = (function (){var or__5142__auto__ = (params["job_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["jobId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var job_json = (function (){var or__5142__auto__ = (params["job_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["jobJson"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(job_id)){
throw (new Error("job_id is required"));
} else {
}

if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_json)))){
throw (new Error("job_json is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Upserting event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+"\u2026"));

return knoxx.backend.tools.event_agents.event_agent_upsert_job_BANG_(config,job_id,knoxx.backend.tools.shared.json_parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_json)))).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Upserted event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)),result);
}));
});
knoxx.backend.tools.event_agents.schedule_event_agent_execute = (function knoxx$backend$tools$event_agents$schedule_event_agent_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var job_id = (function (){var or__5142__auto__ = (params["job_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["jobId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var job_json = (function (){var or__5142__auto__ = (params["job_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["jobJson"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(job_id)){
throw (new Error("job_id is required"));
} else {
}

if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_json)))){
throw (new Error("job_json is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Scheduling event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+"\u2026"));

return knoxx.backend.tools.event_agents.event_agent_upsert_job_BANG_(config,job_id,knoxx.backend.tools.shared.json_parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_json)))).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Scheduled event-agent job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)),result);
}));
});
knoxx.backend.tools.event_agents.event_agent_status_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"event_agents.status","Event Agent Status","Inspect the current scheduled event-agent runtime configuration and live state.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Inspect event-agent jobs, sources, and runtime state before changing schedules or dispatching events.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this before mutating jobs so you understand the current state.","Helpful for debugging cron/event behavior or checking recent events."], null),knoxx.backend.tools.event_agents.status_params,knoxx.backend.tools.event_agents.event_agent_status_execute], 0));
knoxx.backend.tools.event_agents.event_agent_dispatch_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"event_agents.dispatch","Event Agent Dispatch","Dispatch a structured event into the generic event-agent runtime.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Dispatch manual or synthetic events so matching event-agent jobs can react immediately.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use source_kind/manual for synthetic triggers you want to test immediately.","Put complex payload fields into payload_json as a JSON object string."], null),knoxx.backend.tools.event_agents.dispatch_params,knoxx.backend.tools.event_agents.event_agent_dispatch_execute], 0));
knoxx.backend.tools.event_agents.events_status_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"events.status","Events Status","Inspect the current generic events runtime state and trigger configuration.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Inspect event sources, triggers, and runtime state before dispatching or resetting events.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this before dispatching events or resetting the scheduler.","This is the preferred replacement vocabulary for event_agents.status."], null),knoxx.backend.tools.event_agents.status_params,knoxx.backend.tools.event_agents.event_agent_status_execute], 0));
knoxx.backend.tools.event_agents.events_dispatch_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"events.dispatch","Events Dispatch","Dispatch a normalized event onto the generic events runtime.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Publish a manual or synthetic event so matching triggers can react immediately.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this instead of event_agents.dispatch for the new events vocabulary.","Put complex payload fields into payload_json as a JSON object string."], null),knoxx.backend.tools.event_agents.dispatch_params,knoxx.backend.tools.event_agents.event_agent_dispatch_execute], 0));
knoxx.backend.tools.event_agents.agents_spawn_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"agents.spawn","Agents Spawn","Launch a one-off Knoxx agent run without creating or mutating an event trigger job.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Spawn a normal Knoxx agent directly through the shared agent runtime.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this for one-off agent execution.","Pass direct-start style agent overrides in agent_spec_json when you need a specific role, contract, actor, or tool policy surface."], null),knoxx.backend.tools.event_agents.spawn_agent_params,knoxx.backend.tools.event_agents.agent_spawn_execute], 0));
knoxx.backend.tools.event_agents.event_agent_run_job_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"event_agents.run_job","Event Agent Run Job","Run a configured event-agent job immediately without waiting for its schedule.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Trigger an event-agent job now.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this for manual patrol/synthesis/response runs after inspecting status.","Provide the exact job id."], null),knoxx.backend.tools.event_agents.run_job_params,knoxx.backend.tools.event_agents.event_agent_run_job_execute], 0));
knoxx.backend.tools.event_agents.event_agent_upsert_job_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"event_agents.upsert_job","Event Agent Upsert Job","Create or update a scheduled event-agent job, then reload the runtime.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Create or update a generic scheduled event-agent job using JSON job config.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this to create new cron/event-driven agents or revise existing jobs.","Pass a full JSON job object in job_json; include trigger, source, filters, and agentSpec when you need precise control."], null),knoxx.backend.tools.event_agents.upsert_job_params,knoxx.backend.tools.event_agents.event_agent_upsert_job_execute], 0));
knoxx.backend.tools.event_agents.schedule_event_agent_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"schedule_event_agent","Schedule Event Agent","Create or update a scheduled event-agent job with explicit trigger, source, prompts, and tool policies.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Schedule an event-driven agent job that can react to Discord, GitHub, cron, or manual events.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use this when the user wants to create or revise an event-based agent from conversation.","Provide a full job object in job_json, including trigger, source, filters, and agentSpec.","Use role slugs like translator, system_admin, or executive and include explicit toolPolicies so the scheduled agent has exactly the tools it needs."], null),knoxx.backend.tools.event_agents.upsert_job_params,knoxx.backend.tools.event_agents.schedule_event_agent_execute], 0));
knoxx.backend.tools.event_agents.create_event_agent_custom_tools = (function knoxx$backend$tools$event_agents$create_event_agent_custom_tools(var_args){
var G__58937 = arguments.length;
switch (G__58937) {
case 2:
return knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_any_QMARK_ = (function() { 
var G__59070__delegate = function (tool_ids){
return (((auth_context == null)) || (cljs.core.boolean$(cljs.core.some((function (p1__58924_SHARP_){
return knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,p1__58924_SHARP_);
}),tool_ids))));
};
var G__59070 = function (var_args){
var tool_ids = null;
if (arguments.length > 0) {
var G__59073__i = 0, G__59073__a = new Array(arguments.length -  0);
while (G__59073__i < G__59073__a.length) {G__59073__a[G__59073__i] = arguments[G__59073__i + 0]; ++G__59073__i;}
  tool_ids = new cljs.core.IndexedSeq(G__59073__a,0,null);
} 
return G__59070__delegate.call(this,tool_ids);};
G__59070.cljs$lang$maxFixedArity = 0;
G__59070.cljs$lang$applyTo = (function (arglist__59074){
var tool_ids = cljs.core.seq(arglist__59074);
return G__59070__delegate(tool_ids);
});
G__59070.cljs$core$IFn$_invoke$arity$variadic = G__59070__delegate;
return G__59070;
})()
;
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_any_QMARK_("event_agents.status","events.status"))?knoxx.backend.tools.event_agents.event_agent_status_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.status","events.status"))?knoxx.backend.tools.event_agents.events_status_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.dispatch","events.dispatch"))?knoxx.backend.tools.event_agents.event_agent_dispatch_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.dispatch","events.dispatch"))?knoxx.backend.tools.event_agents.events_dispatch_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.run_job","agents.spawn"))?knoxx.backend.tools.event_agents.event_agent_run_job_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.run_job","agents.spawn"))?knoxx.backend.tools.event_agents.agents_spawn_tool(runtime,config):null),((allowed_any_QMARK_("event_agents.upsert_job"))?knoxx.backend.tools.event_agents.event_agent_upsert_job_tool(runtime,config):null),((allowed_any_QMARK_("schedule_event_agent"))?knoxx.backend.tools.event_agents.schedule_event_agent_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.event_agents.js.map
