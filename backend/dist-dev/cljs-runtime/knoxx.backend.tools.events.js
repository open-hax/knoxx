import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.tools.event_agents.js";
goog.provide('knoxx.backend.tools.events');
knoxx.backend.tools.events.create_events_custom_tools = (function knoxx$backend$tools$events$create_events_custom_tools(var_args){
var G__34196 = arguments.length;
switch (G__34196) {
case 2:
return knoxx.backend.tools.events.create_events_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.events.create_events_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.events.create_events_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$2(runtime,config);
}));

(knoxx.backend.tools.events.create_events_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return knoxx.backend.tools.event_agents.create_event_agent_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
}));

(knoxx.backend.tools.events.create_events_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.events.js.map
