goog.provide('knoxx.frontend.admin.event_agent_editor');

/**
 * Generic text input field.
 */
knoxx.frontend.admin.event_agent_editor.text_field = (function (){var G__12857 = (function knoxx$frontend$admin$event_agent_editor$text_field_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__12859 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__12862 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12859,(0),null);
var map__12862__$1 = cljs.core.__destructure_map(map__12862);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var value = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"value","value",305978217));
var on_change = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var disabled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"disabled","disabled",-1529784218));
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var placeholder = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12862__$1,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083));

var G__12873 = "label";
var G__12874 = (function (){var obj12878 = ({"className":"space-y-0.5 block","children":[(function (){var G__12881 = "div";
var G__12882 = (function (){var obj12884 = ({"className":"text-[10px] font-semibold uppercase tracking-wide text-slate-400","children":label});
return obj12884;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12881,G__12882) : helix.core.jsx.call(null,G__12881,G__12882));
})(),(function (){var G__12895 = "input";
var G__12896 = (function (){var obj12898 = ({"type":(function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "text";
}
})(),"value":helix.impl.props.or_undefined(value),"onChange":on_change,"disabled":disabled,"placeholder":placeholder,"className":(""+"w-full rounded-lg border border-slate-800 bg-slate-950/70 "+"px-2.5 py-1.5 text-xs text-slate-100 outline-none "+"focus:border-sky-500 disabled:opacity-60")});
return obj12898;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12895,G__12896) : helix.core.jsx.call(null,G__12895,G__12896));
})()]});
return obj12878;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12873,G__12874) : helix.core.jsxs.call(null,G__12873,G__12874));
});
if(goog.DEBUG === true){
var G__12906 = G__12857;
(G__12906.displayName = "knoxx.frontend.admin.event-agent-editor/text-field");

return G__12906;
} else {
return G__12857;
}
})();




/**
 * Generic select dropdown.
 */
knoxx.frontend.admin.event_agent_editor.select_field = (function (){var G__12913 = (function knoxx$frontend$admin$event_agent_editor$select_field_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__12917 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__12920 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12917,(0),null);
var map__12920__$1 = cljs.core.__destructure_map(map__12920);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12920__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var value = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12920__$1,new cljs.core.Keyword(null,"value","value",305978217));
var on_change = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12920__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var disabled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12920__$1,new cljs.core.Keyword(null,"disabled","disabled",-1529784218));
var options = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12920__$1,new cljs.core.Keyword(null,"options","options",99638489));

var G__12927 = "label";
var G__12928 = (function (){var obj12932 = ({"className":"space-y-0.5 block","children":[(function (){var G__12935 = "div";
var G__12936 = (function (){var obj12940 = ({"className":"text-[10px] font-semibold uppercase tracking-wide text-slate-400","children":label});
return obj12940;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12935,G__12936) : helix.core.jsx.call(null,G__12935,G__12936));
})(),(function (){var G__12943 = "select";
var G__12944 = (function (){var obj12950 = ({"value":helix.impl.props.or_undefined(value),"onChange":on_change,"disabled":disabled,"className":(""+"w-full rounded-lg border border-slate-800 bg-slate-950/70 "+"px-2.5 py-1.5 text-xs text-slate-100 outline-none "+"focus:border-sky-500 disabled:opacity-60"),"children":(function (){var iter__5628__auto__ = (function knoxx$frontend$admin$event_agent_editor$select_field_render_$_iter__12973(s__12974){
return (new cljs.core.LazySeq(null,(function (){
var s__12974__$1 = s__12974;
while(true){
var temp__5823__auto__ = cljs.core.seq(s__12974__$1);
if(temp__5823__auto__){
var s__12974__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(s__12974__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__12974__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__12980 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__12979 = (0);
while(true){
if((i__12979 < size__5627__auto__)){
var opt = cljs.core._nth(c__5626__auto__,i__12979);
cljs.core.chunk_append(b__12980,(function (){var G__13002 = "option";
var G__13003 = (function (){var obj13006 = ({"value":helix.impl.props.or_undefined(opt),"children":opt});
return obj13006;
})();
var G__13004 = opt;
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$3 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$3(G__13002,G__13003,G__13004) : helix.core.jsx.call(null,G__13002,G__13003,G__13004));
})());

var G__13280 = (i__12979 + (1));
i__12979 = G__13280;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__12980),knoxx$frontend$admin$event_agent_editor$select_field_render_$_iter__12973(cljs.core.chunk_rest(s__12974__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__12980),null);
}
} else {
var opt = cljs.core.first(s__12974__$2);
return cljs.core.cons((function (){var G__13009 = "option";
var G__13010 = (function (){var obj13013 = ({"value":helix.impl.props.or_undefined(opt),"children":opt});
return obj13013;
})();
var G__13011 = opt;
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$3 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$3(G__13009,G__13010,G__13011) : helix.core.jsx.call(null,G__13009,G__13010,G__13011));
})(),knoxx$frontend$admin$event_agent_editor$select_field_render_$_iter__12973(cljs.core.rest(s__12974__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(options);
})()});
return obj12950;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12943,G__12944) : helix.core.jsx.call(null,G__12943,G__12944));
})()]});
return obj12932;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12927,G__12928) : helix.core.jsxs.call(null,G__12927,G__12928));
});
if(goog.DEBUG === true){
var G__13014 = G__12913;
(G__13014.displayName = "knoxx.frontend.admin.event-agent-editor/select-field");

return G__13014;
} else {
return G__12913;
}
})();




/**
 * Generic checkbox field.
 */
knoxx.frontend.admin.event_agent_editor.checkbox_field = (function (){var G__13016 = (function knoxx$frontend$admin$event_agent_editor$checkbox_field_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13017 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13020 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13017,(0),null);
var map__13020__$1 = cljs.core.__destructure_map(map__13020);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13020__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var checked = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13020__$1,new cljs.core.Keyword(null,"checked","checked",-50955819));
var on_change = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13020__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var disabled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13020__$1,new cljs.core.Keyword(null,"disabled","disabled",-1529784218));

var G__13022 = "div";
var G__13023 = (function (){var obj13025 = ({"className":"space-y-0.5","children":[(function (){var G__13030 = "div";
var G__13031 = (function (){var obj13033 = ({"className":"text-[10px] font-semibold uppercase tracking-wide text-slate-400","children":label});
return obj13033;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13030,G__13031) : helix.core.jsx.call(null,G__13030,G__13031));
})(),(function (){var G__13034 = "label";
var G__13035 = (function (){var obj13037 = ({"className":"inline-flex w-full items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/70 px-2.5 py-1.5 text-xs text-slate-200","children":[(function (){var G__13038 = "input";
var G__13039 = (function (){var obj13043 = ({"type":"checkbox","checked":checked,"onChange":on_change,"disabled":disabled});
return obj13043;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13038,G__13039) : helix.core.jsx.call(null,G__13038,G__13039));
})(),"Active"]});
return obj13037;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13034,G__13035) : helix.core.jsxs.call(null,G__13034,G__13035));
})()]});
return obj13025;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13022,G__13023) : helix.core.jsxs.call(null,G__13022,G__13023));
});
if(goog.DEBUG === true){
var G__13048 = G__13016;
(G__13048.displayName = "knoxx.frontend.admin.event-agent-editor/checkbox-field");

return G__13048;
} else {
return G__13016;
}
})();




/**
 * The full job editing form.
 */
knoxx.frontend.admin.event_agent_editor.job_form = (function (){var G__13079 = (function knoxx$frontend$admin$event_agent_editor$job_form_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13080 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13083 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13080,(0),null);
var map__13083__$1 = cljs.core.__destructure_map(map__13083);
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13083__$1,new cljs.core.Keyword(null,"job","job",850873087));
var on_update = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13083__$1,new cljs.core.Keyword(null,"on-update","on-update",1680216496));
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13083__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var saving_control = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13083__$1,new cljs.core.Keyword(null,"saving-control","saving-control",1969586949));

var disabled = (function (){var or__5142__auto__ = cljs.core.not(can_manage);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return saving_control;
}
})();
var trigger = job.trigger;
var source = job.source;
var spec = job.agentSpec;
var G__13092 = "div";
var G__13093 = (function (){var obj13097 = ({"className":"space-y-3","children":[(function (){var G__13098 = "div";
var G__13099 = (function (){var obj13101 = ({"className":"grid gap-2 md:grid-cols-3","children":[(function (){var G__13104 = knoxx.frontend.admin.event_agent_editor.checkbox_field;
var G__13105 = (function (){var obj13107 = ({"label":"Enabled","checked":job.enabled,"on-change":(function (p1__13051_SHARP_){
var G__13108 = new cljs.core.Keyword(null,"enabled","enabled",1195909756);
var G__13109 = p1__13051_SHARP_.target.checked;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13108,G__13109) : on_update.call(null,G__13108,G__13109));
}),"disabled":disabled});
return obj13107;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13104,G__13105) : helix.core.jsx.call(null,G__13104,G__13105));
})(),(function (){var G__13110 = knoxx.frontend.admin.event_agent_editor.select_field;
var G__13111 = (function (){var obj13115 = ({"label":"Trigger kind","value":trigger.kind,"on-change":(function (p1__13052_SHARP_){
var G__13118 = new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783);
var G__13119 = p1__13052_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13118,G__13119) : on_update.call(null,G__13118,G__13119));
}),"disabled":disabled,"options":new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["cron","event"], null)});
return obj13115;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13110,G__13111) : helix.core.jsx.call(null,G__13110,G__13111));
})(),(function (){var G__13124 = knoxx.frontend.admin.event_agent_editor.select_field;
var G__13125 = (function (){var obj13131 = ({"label":"Source kind","value":source.kind,"on-change":(function (p1__13055_SHARP_){
var G__13132 = new cljs.core.Keyword(null,"source-kind","source-kind",-1955827566);
var G__13133 = p1__13055_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13132,G__13133) : on_update.call(null,G__13132,G__13133));
}),"disabled":disabled,"options":new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord","github","webhook"], null)});
return obj13131;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13124,G__13125) : helix.core.jsx.call(null,G__13124,G__13125));
})()]});
return obj13101;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13098,G__13099) : helix.core.jsxs.call(null,G__13098,G__13099));
})(),(function (){var G__13134 = "div";
var G__13137 = (function (){var obj13141 = ({"className":"grid gap-2 md:grid-cols-3","children":[(function (){var G__13144 = knoxx.frontend.admin.event_agent_editor.text_field;
var G__13145 = (function (){var obj13149 = ({"label":"Source mode","value":source.mode,"on-change":(function (p1__13056_SHARP_){
var G__13152 = new cljs.core.Keyword(null,"source-mode","source-mode",725702471);
var G__13153 = p1__13056_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13152,G__13153) : on_update.call(null,G__13152,G__13153));
}),"disabled":disabled});
return obj13149;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13144,G__13145) : helix.core.jsx.call(null,G__13144,G__13145));
})(),(function (){var G__13156 = knoxx.frontend.admin.event_agent_editor.text_field;
var G__13157 = (function (){var obj13159 = ({"label":"Cadence (min)","type":"number","value":trigger.cadenceMinutes,"on-change":(function (p1__13057_SHARP_){
var G__13160 = new cljs.core.Keyword(null,"cadence","cadence",821204241);
var G__13161 = parseInt(p1__13057_SHARP_.target.value,(10));
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13160,G__13161) : on_update.call(null,G__13160,G__13161));
}),"disabled":(function (){var or__5142__auto__ = disabled;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(trigger.kind,"cron");
}
})()});
return obj13159;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13156,G__13157) : helix.core.jsx.call(null,G__13156,G__13157));
})(),(function (){var G__13162 = knoxx.frontend.admin.event_agent_editor.text_field;
var G__13163 = (function (){var obj13165 = ({"label":"Event kinds","value":knoxx.frontend.admin.event_agent_utils.join_csv(trigger.eventKinds),"on-change":(function (p1__13058_SHARP_){
var G__13170 = new cljs.core.Keyword(null,"event-kinds","event-kinds",1770855958);
var G__13171 = knoxx.frontend.admin.event_agent_utils.split_csv(p1__13058_SHARP_.target.value);
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13170,G__13171) : on_update.call(null,G__13170,G__13171));
}),"disabled":(function (){var or__5142__auto__ = disabled;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(trigger.kind,"event");
}
})(),"placeholder":"mention, issues.opened"});
return obj13165;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13162,G__13163) : helix.core.jsx.call(null,G__13162,G__13163));
})()]});
return obj13141;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13134,G__13137) : helix.core.jsxs.call(null,G__13134,G__13137));
})(),(function (){var G__13174 = "div";
var G__13175 = (function (){var obj13178 = ({"className":"grid gap-2 md:grid-cols-3","children":[(function (){var G__13183 = knoxx.frontend.admin.event_agent_editor.select_field;
var G__13184 = (function (){var obj13186 = ({"label":"Role","value":spec.role,"on-change":(function (p1__13059_SHARP_){
var G__13189 = new cljs.core.Keyword(null,"role","role",-736691072);
var G__13190 = p1__13059_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13189,G__13190) : on_update.call(null,G__13189,G__13190));
}),"disabled":disabled,"options":new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["agent","reviewer","dispatcher"], null)});
return obj13186;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13183,G__13184) : helix.core.jsx.call(null,G__13183,G__13184));
})(),(function (){var G__13197 = knoxx.frontend.admin.event_agent_editor.text_field;
var G__13200 = (function (){var obj13202 = ({"label":"Model","value":spec.model,"on-change":(function (p1__13060_SHARP_){
var G__13205 = new cljs.core.Keyword(null,"model","model",331153215);
var G__13206 = p1__13060_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13205,G__13206) : on_update.call(null,G__13205,G__13206));
}),"disabled":disabled});
return obj13202;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13197,G__13200) : helix.core.jsx.call(null,G__13197,G__13200));
})(),(function (){var G__13213 = knoxx.frontend.admin.event_agent_editor.select_field;
var G__13214 = (function (){var obj13218 = ({"label":"Thinking","value":spec.thinkingLevel,"on-change":(function (p1__13061_SHARP_){
var G__13219 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953);
var G__13220 = p1__13061_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13219,G__13220) : on_update.call(null,G__13219,G__13220));
}),"disabled":disabled,"options":new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, ["off","minimal","low","medium","high","xhigh"], null)});
return obj13218;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13213,G__13214) : helix.core.jsx.call(null,G__13213,G__13214));
})()]});
return obj13178;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13174,G__13175) : helix.core.jsxs.call(null,G__13174,G__13175));
})(),(function (){var G__13227 = knoxx.frontend.admin.event_agent_editor.text_field;
var G__13228 = (function (){var obj13232 = ({"label":"Job description","value":(function (){var or__5142__auto__ = job.description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),"on-change":(function (p1__13062_SHARP_){
var G__13237 = new cljs.core.Keyword(null,"description","description",-1428560544);
var G__13238 = p1__13062_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13237,G__13238) : on_update.call(null,G__13237,G__13238));
}),"disabled":disabled});
return obj13232;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13227,G__13228) : helix.core.jsx.call(null,G__13227,G__13228));
})(),(function (){var G__13243 = "label";
var G__13244 = (function (){var obj13250 = ({"className":"space-y-0.5 block","children":[(function (){var G__13251 = "div";
var G__13252 = (function (){var obj13254 = ({"className":"text-[10px] font-semibold uppercase tracking-wide text-slate-400","children":"System prompt"});
return obj13254;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13251,G__13252) : helix.core.jsx.call(null,G__13251,G__13252));
})(),(function (){var G__13255 = "textarea";
var G__13256 = (function (){var obj13258 = ({"value":helix.impl.props.or_undefined(spec.systemPrompt),"onChange":(function (p1__13065_SHARP_){
var G__13265 = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429);
var G__13266 = p1__13065_SHARP_.target.value;
return (on_update.cljs$core$IFn$_invoke$arity$2 ? on_update.cljs$core$IFn$_invoke$arity$2(G__13265,G__13266) : on_update.call(null,G__13265,G__13266));
}),"disabled":disabled,"rows":(3),"className":(""+"w-full rounded-lg border border-slate-800 bg-slate-950/70 "+"px-2.5 py-1.5 text-xs text-slate-100 outline-none "+"focus:border-sky-500 disabled:opacity-60")});
return obj13258;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13255,G__13256) : helix.core.jsx.call(null,G__13255,G__13256));
})()]});
return obj13250;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13243,G__13244) : helix.core.jsxs.call(null,G__13243,G__13244));
})()]});
return obj13097;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13092,G__13093) : helix.core.jsxs.call(null,G__13092,G__13093));
});
if(goog.DEBUG === true){
var G__13269 = G__13079;
(G__13269.displayName = "knoxx.frontend.admin.event-agent-editor/job-form");

return G__13269;
} else {
return G__13079;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agent_editor.js.map
