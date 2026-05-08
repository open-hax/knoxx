import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.event_agents.js";
goog.provide('knoxx.backend.events.runtime');
knoxx.backend.events.runtime.status_snapshot = (function knoxx$backend$events$runtime$status_snapshot(config){
return knoxx.backend.event_agents.status_snapshot(config);
});
knoxx.backend.events.runtime.start_BANG_ = (function knoxx$backend$events$runtime$start_BANG_(config){
return knoxx.backend.event_agents.start_BANG_(config);
});
knoxx.backend.events.runtime.stop_BANG_ = (function knoxx$backend$events$runtime$stop_BANG_(){
return knoxx.backend.event_agents.stop_BANG_();
});
knoxx.backend.events.runtime.reload_BANG_ = (function knoxx$backend$events$runtime$reload_BANG_(){
return knoxx.backend.event_agents.reload_BANG_();
});
knoxx.backend.events.runtime.debounced_reload_BANG_ = (function knoxx$backend$events$runtime$debounced_reload_BANG_(){
return knoxx.backend.event_agents.debounced_reload_BANG_();
});
knoxx.backend.events.runtime.reset_runtime_BANG_ = (function knoxx$backend$events$runtime$reset_runtime_BANG_(config){
return knoxx.backend.event_agents.reset_runtime_BANG_(config);
});
knoxx.backend.events.runtime.run_job_BANG_ = (function knoxx$backend$events$runtime$run_job_BANG_(job_id){
return knoxx.backend.event_agents.run_job_BANG_(job_id);
});
knoxx.backend.events.runtime.upsert_job_BANG_ = (function knoxx$backend$events$runtime$upsert_job_BANG_(job_id,job_spec){
return knoxx.backend.event_agents.upsert_job_BANG_(job_id,job_spec);
});

//# sourceMappingURL=knoxx.backend.events.runtime.js.map
