import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.events.cron');
knoxx.backend.events.cron.default_cron_ticker_ms = (15000);
knoxx.backend.events.cron.cadence_label = (function knoxx$backend$events$cron$cadence_label(minutes){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(minutes,(1))){
return "Every minute";
} else {
if((minutes < (60))){
return (""+"Every "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(minutes)+" minutes");
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.mod(minutes,(60)),(0))){
return (""+"Every "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((minutes / (60)))+" hours");
} else {
return (""+"Every "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(minutes)+" minutes");

}
}
}
});
knoxx.backend.events.cron.cron_job_QMARK_ = (function knoxx$backend$events$cron$cron_job_QMARK_(job){
var and__5140__auto__ = new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cron",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)));
} else {
return and__5140__auto__;
}
});
knoxx.backend.events.cron.job_cadence_ms = (function knoxx$backend$events$cron$job_cadence_ms(job){
return (((60) * (1000)) * cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})()));
});
knoxx.backend.events.cron.initialize_cron_job_state_BANG_ = (function knoxx$backend$events$cron$initialize_cron_job_state_BANG_(p__523130,job){
var map__523131 = p__523130;
var map__523131__$1 = cljs.core.__destructure_map(map__523131);
var update_job_state_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523131__$1,new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980));
var normalize_job_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523131__$1,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147));
var job_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
var cadence_ms = knoxx.backend.events.cron.job_cadence_ms(job);
var now = Date.now();
var G__523139 = job_id;
var G__523140 = (function (state){
var state__$1 = (normalize_job_state.cljs$core$IFn$_invoke$arity$2 ? normalize_job_state.cljs$core$IFn$_invoke$arity$2(job_id,state) : normalize_job_state.call(null,job_id,state));
var running_QMARK_ = cljs.core.boolean$(new cljs.core.Keyword(null,"running","running",1554969103).cljs$core$IFn$_invoke$arity$1(state__$1));
var last_finished = new cljs.core.Keyword(null,"lastFinishedAt","lastFinishedAt",-1905527657).cljs$core$IFn$_invoke$arity$1(state__$1);
var next_run = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"nextRunAt","nextRunAt",-1914512613).cljs$core$IFn$_invoke$arity$1(state__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (cljs.core.truth_(last_finished)?(last_finished + cadence_ms):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return now;
}
}
})();
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(state__$1,new cljs.core.Keyword(null,"id","id",-1388402092),job_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"running","running",1554969103),running_QMARK_,new cljs.core.Keyword(null,"nextRunAt","nextRunAt",-1914512613),next_run], 0));
});
return (update_job_state_BANG_.cljs$core$IFn$_invoke$arity$2 ? update_job_state_BANG_.cljs$core$IFn$_invoke$arity$2(G__523139,G__523140) : update_job_state_BANG_.call(null,G__523139,G__523140));
});
knoxx.backend.events.cron.due_cron_job_QMARK_ = (function knoxx$backend$events$cron$due_cron_job_QMARK_(p__523161,now,job){
var map__523162 = p__523161;
var map__523162__$1 = cljs.core.__destructure_map(map__523162);
var job_state_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523162__$1,new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400));
var normalize_job_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523162__$1,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147));
var job_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
var state = (function (){var G__523169 = job_id;
var G__523170 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(job_state_STAR_),job_id);
return (normalize_job_state.cljs$core$IFn$_invoke$arity$2 ? normalize_job_state.cljs$core$IFn$_invoke$arity$2(G__523169,G__523170) : normalize_job_state.call(null,G__523169,G__523170));
})();
var and__5140__auto__ = knoxx.backend.events.cron.cron_job_QMARK_(job);
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.not(new cljs.core.Keyword(null,"running","running",1554969103).cljs$core$IFn$_invoke$arity$1(state))) && (((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"nextRunAt","nextRunAt",-1914512613).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})() <= now)));
} else {
return and__5140__auto__;
}
});
knoxx.backend.events.cron.trigger_due_cron_jobs_BANG_ = (function knoxx$backend$events$cron$trigger_due_cron_jobs_BANG_(p__523180,config){
var map__523184 = p__523180;
var map__523184__$1 = cljs.core.__destructure_map(map__523184);
var job_state_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400));
var running_QMARK__STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"running?*","running?*",-1352022575));
var control_config_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"control-config-fn","control-config-fn",-413447156));
var run_job_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"run-job!","run-job!",-1560119830));
var update_job_state_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980));
var normalize_job_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523184__$1,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147));
if(cljs.core.truth_(cljs.core.deref(running_QMARK__STAR_))){
var control = (control_config_fn.cljs$core$IFn$_invoke$arity$1 ? control_config_fn.cljs$core$IFn$_invoke$arity$1(config) : control_config_fn.call(null,config));
var now = Date.now();
var cron_jobs = cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.events.cron.cron_job_QMARK_,new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control));
var seq__523189_523355 = cljs.core.seq(cron_jobs);
var chunk__523190_523356 = null;
var count__523191_523357 = (0);
var i__523192_523358 = (0);
while(true){
if((i__523192_523358 < count__523191_523357)){
var job_523363 = chunk__523190_523356.cljs$core$IIndexed$_nth$arity$2(null,i__523192_523358);
knoxx.backend.events.cron.initialize_cron_job_state_BANG_(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980),update_job_state_BANG_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),normalize_job_state], null),job_523363);


var G__523371 = seq__523189_523355;
var G__523372 = chunk__523190_523356;
var G__523373 = count__523191_523357;
var G__523374 = (i__523192_523358 + (1));
seq__523189_523355 = G__523371;
chunk__523190_523356 = G__523372;
count__523191_523357 = G__523373;
i__523192_523358 = G__523374;
continue;
} else {
var temp__5825__auto___523376 = cljs.core.seq(seq__523189_523355);
if(temp__5825__auto___523376){
var seq__523189_523380__$1 = temp__5825__auto___523376;
if(cljs.core.chunked_seq_QMARK_(seq__523189_523380__$1)){
var c__5673__auto___523381 = cljs.core.chunk_first(seq__523189_523380__$1);
var G__523385 = cljs.core.chunk_rest(seq__523189_523380__$1);
var G__523386 = c__5673__auto___523381;
var G__523387 = cljs.core.count(c__5673__auto___523381);
var G__523388 = (0);
seq__523189_523355 = G__523385;
chunk__523190_523356 = G__523386;
count__523191_523357 = G__523387;
i__523192_523358 = G__523388;
continue;
} else {
var job_523389 = cljs.core.first(seq__523189_523380__$1);
knoxx.backend.events.cron.initialize_cron_job_state_BANG_(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980),update_job_state_BANG_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),normalize_job_state], null),job_523389);


var G__523390 = cljs.core.next(seq__523189_523380__$1);
var G__523391 = null;
var G__523392 = (0);
var G__523394 = (0);
seq__523189_523355 = G__523390;
chunk__523190_523356 = G__523391;
count__523191_523357 = G__523392;
i__523192_523358 = G__523394;
continue;
}
} else {
}
}
break;
}

var seq__523198 = cljs.core.seq(cron_jobs);
var chunk__523199 = null;
var count__523200 = (0);
var i__523201 = (0);
while(true){
if((i__523201 < count__523200)){
var job = chunk__523199.cljs$core$IIndexed$_nth$arity$2(null,i__523201);
if(cljs.core.truth_(knoxx.backend.events.cron.due_cron_job_QMARK_(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400),job_state_STAR_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),normalize_job_state], null),now,job))){
(function (){var G__523236 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
return (run_job_BANG_.cljs$core$IFn$_invoke$arity$1 ? run_job_BANG_.cljs$core$IFn$_invoke$arity$1(G__523236) : run_job_BANG_.call(null,G__523236));
})().catch(((function (seq__523198,chunk__523199,count__523200,i__523201,job,control,now,cron_jobs,map__523184,map__523184__$1,job_state_STAR_,running_QMARK__STAR_,control_config_fn,run_job_BANG_,update_job_state_BANG_,normalize_job_state){
return (function (err){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[events.cron] cron ticker job failed for",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),":",err.message], 0));
});})(seq__523198,chunk__523199,count__523200,i__523201,job,control,now,cron_jobs,map__523184,map__523184__$1,job_state_STAR_,running_QMARK__STAR_,control_config_fn,run_job_BANG_,update_job_state_BANG_,normalize_job_state))
);
} else {
}


var G__523410 = seq__523198;
var G__523412 = chunk__523199;
var G__523413 = count__523200;
var G__523415 = (i__523201 + (1));
seq__523198 = G__523410;
chunk__523199 = G__523412;
count__523200 = G__523413;
i__523201 = G__523415;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__523198);
if(temp__5825__auto__){
var seq__523198__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__523198__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__523198__$1);
var G__523424 = cljs.core.chunk_rest(seq__523198__$1);
var G__523425 = c__5673__auto__;
var G__523426 = cljs.core.count(c__5673__auto__);
var G__523427 = (0);
seq__523198 = G__523424;
chunk__523199 = G__523425;
count__523200 = G__523426;
i__523201 = G__523427;
continue;
} else {
var job = cljs.core.first(seq__523198__$1);
if(cljs.core.truth_(knoxx.backend.events.cron.due_cron_job_QMARK_(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400),job_state_STAR_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),normalize_job_state], null),now,job))){
(function (){var G__523252 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
return (run_job_BANG_.cljs$core$IFn$_invoke$arity$1 ? run_job_BANG_.cljs$core$IFn$_invoke$arity$1(G__523252) : run_job_BANG_.call(null,G__523252));
})().catch(((function (seq__523198,chunk__523199,count__523200,i__523201,job,seq__523198__$1,temp__5825__auto__,control,now,cron_jobs,map__523184,map__523184__$1,job_state_STAR_,running_QMARK__STAR_,control_config_fn,run_job_BANG_,update_job_state_BANG_,normalize_job_state){
return (function (err){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[events.cron] cron ticker job failed for",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),":",err.message], 0));
});})(seq__523198,chunk__523199,count__523200,i__523201,job,seq__523198__$1,temp__5825__auto__,control,now,cron_jobs,map__523184,map__523184__$1,job_state_STAR_,running_QMARK__STAR_,control_config_fn,run_job_BANG_,update_job_state_BANG_,normalize_job_state))
);
} else {
}


var G__523437 = cljs.core.next(seq__523198__$1);
var G__523438 = null;
var G__523439 = (0);
var G__523440 = (0);
seq__523198 = G__523437;
chunk__523199 = G__523438;
count__523200 = G__523439;
i__523201 = G__523440;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
});
knoxx.backend.events.cron.schedule_cron_ticker_BANG_ = (function knoxx$backend$events$cron$schedule_cron_ticker_BANG_(p__523277,config){
var map__523278 = p__523277;
var map__523278__$1 = cljs.core.__destructure_map(map__523278);
var scheduled_tasks_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__523278__$1,new cljs.core.Keyword(null,"scheduled-tasks*","scheduled-tasks*",461432845),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY));
var job_state_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__523278__$1,new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY));
var running_QMARK__STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523278__$1,new cljs.core.Keyword(null,"running?*","running?*",-1352022575));
var control_config_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523278__$1,new cljs.core.Keyword(null,"control-config-fn","control-config-fn",-413447156));
var run_job_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523278__$1,new cljs.core.Keyword(null,"run-job!","run-job!",-1560119830));
var update_job_state_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523278__$1,new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980));
var normalize_job_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523278__$1,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147));
if(cljs.core.contains_QMARK_(cljs.core.deref(scheduled_tasks_STAR_),new cljs.core.Keyword(null,"cron-ticker","cron-ticker",1560192313))){
return null;
} else {
var tick_env = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400),job_state_STAR_,new cljs.core.Keyword(null,"running?*","running?*",-1352022575),running_QMARK__STAR_,new cljs.core.Keyword(null,"control-config-fn","control-config-fn",-413447156),control_config_fn,new cljs.core.Keyword(null,"run-job!","run-job!",-1560119830),run_job_BANG_,new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980),update_job_state_BANG_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),normalize_job_state], null);
var tick_BANG_ = (function (){
return knoxx.backend.events.cron.trigger_due_cron_jobs_BANG_(tick_env,config);
});
var id = (function (){var G__523295 = setInterval(tick_BANG_,knoxx.backend.events.cron.default_cron_ticker_ms);
G__523295.unref();

return G__523295;
})();
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(scheduled_tasks_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"cron-ticker","cron-ticker",1560192313),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"interval","interval",1708495417),new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"everyMs","everyMs",1558845283),knoxx.backend.events.cron.default_cron_ticker_ms], null));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[events.cron] scheduled single cron ticker every",knoxx.backend.events.cron.default_cron_ticker_ms,"ms"], 0));

return tick_BANG_();
}
});

//# sourceMappingURL=knoxx.backend.events.cron.js.map
