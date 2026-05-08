import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.tools.openutau');
knoxx.backend.tools.openutau.default_ustx_version = "0.6";
knoxx.backend.tools.openutau.default_ticks_per_quarter = (480);
knoxx.backend.tools.openutau.default_renderer = "WORLDLINE-R";
knoxx.backend.tools.openutau.default_track_color = "Blue";
knoxx.backend.tools.openutau.slugify = (function knoxx$backend$tools$openutau$slugify(value){
var base = clojure.string.replace(clojure.string.replace(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "openutau-project";
}
})()))),/[^a-z0-9]+/,"-"),/^-+|-+$/,"");
if(clojure.string.blank_QMARK_(base)){
return "openutau-project";
} else {
return base;
}
});
knoxx.backend.tools.openutau.default_project_relative_path = (function knoxx$backend$tools$openutau$default_project_relative_path(project_name){
var slug = knoxx.backend.tools.openutau.slugify(project_name);
return (""+"orgs/open-hax/openplanner/packages/agents/knoxx/uploads/openutau/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)+".ustx");
});
knoxx.backend.tools.openutau.finite_number_QMARK_ = (function knoxx$backend$tools$openutau$finite_number_QMARK_(value){
var and__5140__auto__ = typeof value === 'number';
if(and__5140__auto__){
return Number.isFinite(value);
} else {
return and__5140__auto__;
}
});
knoxx.backend.tools.openutau.parse_number = (function knoxx$backend$tools$openutau$parse_number(value){
if(cljs.core.truth_(knoxx.backend.tools.openutau.finite_number_QMARK_(value))){
return value;
} else {
if(typeof value === 'string'){
var parsed = parseFloat(value);
if(cljs.core.truth_(Number.isFinite(parsed))){
return parsed;
} else {
return null;
}
} else {
return null;

}
}
});
knoxx.backend.tools.openutau.clamp_int = (function knoxx$backend$tools$openutau$clamp_int(value,fallback,min_value,max_value){
var n = knoxx.backend.tools.openutau.parse_number(value);
if((n == null)){
return fallback;
} else {
return cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2(Math.round(n),min_value),max_value);
}
});
knoxx.backend.tools.openutau.clamp_float = (function knoxx$backend$tools$openutau$clamp_float(value,fallback,min_value,max_value){
var n = knoxx.backend.tools.openutau.parse_number(value);
if((n == null)){
return fallback;
} else {
return cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2(n,min_value),max_value);
}
});
knoxx.backend.tools.openutau.lyric_text = (function knoxx$backend$tools$openutau$lyric_text(note){
var lyric = (function (){var G__521974 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"lyric","lyric",164436415).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(note);
}
})();
var G__521974__$1 = (((G__521974 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521974)));
var G__521974__$2 = (((G__521974__$1 == null))?null:clojure.string.trim(G__521974__$1));
if((G__521974__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__521974__$2);
}
})();
var phonetic_hint = (function (){var G__521976 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"phonetic_hint","phonetic_hint",1425882362).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"phonetic-hint","phonetic-hint",999705969).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"phoneticHint","phoneticHint",-2094145553).cljs$core$IFn$_invoke$arity$1(note);
}
}
})();
var G__521976__$1 = (((G__521976 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521976)));
var G__521976__$2 = (((G__521976__$1 == null))?null:clojure.string.trim(G__521976__$1));
if((G__521976__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__521976__$2);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = lyric;
if(cljs.core.truth_(and__5140__auto__)){
return phonetic_hint;
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(lyric)+" ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(phonetic_hint)+"]");
} else {
if(cljs.core.truth_(phonetic_hint)){
return (""+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(phonetic_hint)+"]");
} else {
if(cljs.core.truth_(lyric)){
return lyric;
} else {
return "a";

}
}
}
});
knoxx.backend.tools.openutau.normalize_notes = (function knoxx$backend$tools$openutau$normalize_notes(notes){
var remaining = cljs.core.seq(notes);
var cursor = (0);
var normalized = cljs.core.PersistentVector.EMPTY;
while(true){
if(cljs.core.not(remaining)){
return normalized;
} else {
var note = cljs.core.first(remaining);
var explicit_position = knoxx.backend.tools.openutau.parse_number((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"position","position",-2011731912).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"start_tick","start_tick",-2101810776).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"start-tick","start-tick",1709314339).cljs$core$IFn$_invoke$arity$1(note);
}
}
})());
var position = (((explicit_position == null))?cursor:cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),Math.round(explicit_position)));
var duration = knoxx.backend.tools.openutau.clamp_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"duration","duration",1444101068).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"duration_ticks","duration_ticks",1004912450).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"duration-ticks","duration-ticks",-468631450).cljs$core$IFn$_invoke$arity$1(note);
}
}
})(),knoxx.backend.tools.openutau.default_ticks_per_quarter,(10),((64) * knoxx.backend.tools.openutau.default_ticks_per_quarter));
var tone = knoxx.backend.tools.openutau.clamp_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tone","tone",-1422788785).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"midi","midi",1256960668).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"note","note",1426297904).cljs$core$IFn$_invoke$arity$1(note);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"pitch","pitch",1495126700).cljs$core$IFn$_invoke$arity$1(note);
}
}
}
})(),(60),(0),(127));
var normalized_note = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"position","position",-2011731912),position,new cljs.core.Keyword(null,"duration","duration",1444101068),duration,new cljs.core.Keyword(null,"tone","tone",-1422788785),tone,new cljs.core.Keyword(null,"lyric","lyric",164436415),knoxx.backend.tools.openutau.lyric_text(note)], null);
var G__522079 = cljs.core.next(remaining);
var G__522080 = (position + duration);
var G__522081 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(normalized,normalized_note);
remaining = G__522079;
cursor = G__522080;
normalized = G__522081;
continue;
}
break;
}
});
knoxx.backend.tools.openutau.voice_part_duration = (function knoxx$backend$tools$openutau$voice_part_duration(normalized_notes){
return (cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (max_end,p__521992){
var map__521993 = p__521992;
var map__521993__$1 = cljs.core.__destructure_map(map__521993);
var position = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521993__$1,new cljs.core.Keyword(null,"position","position",-2011731912));
var duration = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521993__$1,new cljs.core.Keyword(null,"duration","duration",1444101068));
return cljs.core.max.cljs$core$IFn$_invoke$arity$2(max_end,(position + duration));
}),(0),normalized_notes) + knoxx.backend.tools.openutau.default_ticks_per_quarter);
});
knoxx.backend.tools.openutau.build_project = (function knoxx$backend$tools$openutau$build_project(opts,normalized_notes){
var project_name = (function (){var or__5142__auto__ = (function (){var G__521994 = new cljs.core.Keyword(null,"project_name","project_name",-1535411620).cljs$core$IFn$_invoke$arity$1(opts);
var G__521994__$1 = (((G__521994 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521994)));
var G__521994__$2 = (((G__521994__$1 == null))?null:clojure.string.trim(G__521994__$1));
if((G__521994__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__521994__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__521996 = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(opts);
var G__521996__$1 = (((G__521996 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521996)));
var G__521996__$2 = (((G__521996__$1 == null))?null:clojure.string.trim(G__521996__$1));
if((G__521996__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__521996__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "Knoxx OpenUtau Project";
}
}
})();
var bpm = knoxx.backend.tools.openutau.clamp_float(new cljs.core.Keyword(null,"tempo","tempo",-1555208453).cljs$core$IFn$_invoke$arity$1(opts),(120),(20),(300));
var beat_per_bar = knoxx.backend.tools.openutau.clamp_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"beat_per_bar","beat_per_bar",-752938484).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"beat-per-bar","beat-per-bar",914529868).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),new cljs.core.Keyword(null,"beat_per_bar","beat_per_bar",-752938484)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),new cljs.core.Keyword(null,"beat-per-bar","beat-per-bar",914529868)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time-signature","time-signature",-1730387952),new cljs.core.Keyword(null,"beat_per_bar","beat_per_bar",-752938484)], null));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time-signature","time-signature",-1730387952),new cljs.core.Keyword(null,"beat-per-bar","beat-per-bar",914529868)], null));
}
}
}
}
}
})(),(4),(1),(32));
var beat_unit = knoxx.backend.tools.openutau.clamp_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"beat_unit","beat_unit",1360431781).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"beat-unit","beat-unit",-869271375).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),new cljs.core.Keyword(null,"beat_unit","beat_unit",1360431781)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),new cljs.core.Keyword(null,"beat-unit","beat-unit",-869271375)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time-signature","time-signature",-1730387952),new cljs.core.Keyword(null,"beat_unit","beat_unit",1360431781)], null));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(opts,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time-signature","time-signature",-1730387952),new cljs.core.Keyword(null,"beat-unit","beat-unit",-869271375)], null));
}
}
}
}
}
})(),(4),(1),(32));
var singer_id = (function (){var or__5142__auto__ = (function (){var G__521998 = new cljs.core.Keyword(null,"singer_id","singer_id",1456162645).cljs$core$IFn$_invoke$arity$1(opts);
var G__521998__$1 = (((G__521998 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521998)));
var G__521998__$2 = (((G__521998__$1 == null))?null:clojure.string.trim(G__521998__$1));
if((G__521998__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__521998__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__522000 = new cljs.core.Keyword(null,"singer-id","singer-id",705189264).cljs$core$IFn$_invoke$arity$1(opts);
var G__522000__$1 = (((G__522000 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522000)));
var G__522000__$2 = (((G__522000__$1 == null))?null:clojure.string.trim(G__522000__$1));
if((G__522000__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522000__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var phonemizer = (function (){var or__5142__auto__ = (function (){var G__522006 = new cljs.core.Keyword(null,"phonemizer","phonemizer",-1364007211).cljs$core$IFn$_invoke$arity$1(opts);
var G__522006__$1 = (((G__522006 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522006)));
var G__522006__$2 = (((G__522006__$1 == null))?null:clojure.string.trim(G__522006__$1));
if((G__522006__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522006__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var track_name = (function (){var or__5142__auto__ = (function (){var G__522010 = new cljs.core.Keyword(null,"track_name","track_name",1331132230).cljs$core$IFn$_invoke$arity$1(opts);
var G__522010__$1 = (((G__522010 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522010)));
var G__522010__$2 = (((G__522010__$1 == null))?null:clojure.string.trim(G__522010__$1));
if((G__522010__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522010__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__522016 = new cljs.core.Keyword(null,"track-name","track-name",2146044267).cljs$core$IFn$_invoke$arity$1(opts);
var G__522016__$1 = (((G__522016 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522016)));
var G__522016__$2 = (((G__522016__$1 == null))?null:clojure.string.trim(G__522016__$1));
if((G__522016__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522016__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "Vocal";
}
}
})();
var part_name = (function (){var or__5142__auto__ = (function (){var G__522018 = new cljs.core.Keyword(null,"part_name","part_name",-334556537).cljs$core$IFn$_invoke$arity$1(opts);
var G__522018__$1 = (((G__522018 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522018)));
var G__522018__$2 = (((G__522018__$1 == null))?null:clojure.string.trim(G__522018__$1));
if((G__522018__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522018__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__522023 = new cljs.core.Keyword(null,"part-name","part-name",-290002832).cljs$core$IFn$_invoke$arity$1(opts);
var G__522023__$1 = (((G__522023 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522023)));
var G__522023__$2 = (((G__522023__$1 == null))?null:clojure.string.trim(G__522023__$1));
if((G__522023__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522023__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "Main Part";
}
}
})();
var comment = (function (){var or__5142__auto__ = (function (){var G__522024 = new cljs.core.Keyword(null,"comment","comment",532206069).cljs$core$IFn$_invoke$arity$1(opts);
var G__522024__$1 = (((G__522024 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__522024)));
var G__522024__$2 = (((G__522024__$1 == null))?null:clojure.string.trim(G__522024__$1));
if((G__522024__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__522024__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Generated by Knoxx for OpenUtau. Open in OpenUtau and export audio from the UI.";
}
})();
return (new cljs.core.PersistentArrayMap(null,(12),["name",project_name,"comment",comment,"output_dir","Export","cache_dir","UCache","ustx_version",knoxx.backend.tools.openutau.default_ustx_version,"resolution",knoxx.backend.tools.openutau.default_ticks_per_quarter,"key",knoxx.backend.tools.openutau.clamp_int(new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(opts),(0),(0),(11)),"time_signatures",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentArrayMap(null,(3),["bar_position",(0),"beat_per_bar",beat_per_bar,"beat_unit",beat_unit],null))], null),"tempos",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentArrayMap(null,(2),["position",(0),"bpm",bpm],null))], null),"tracks",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentArrayMap(null,(10),["singer",singer_id,"phonemizer",phonemizer,"renderer_settings",(new cljs.core.PersistentArrayMap(null,(1),["renderer",knoxx.backend.tools.openutau.default_renderer],null)),"track_name",track_name,"track_color",knoxx.backend.tools.openutau.default_track_color,"mute",false,"solo",false,"volume",(0),"pan",(0),"voice_color_names",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [""], null)],null))], null),"voice_parts",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentArrayMap(null,(6),["duration",knoxx.backend.tools.openutau.voice_part_duration(normalized_notes),"name",part_name,"track_no",(0),"position",(0),"notes",cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__522043){
var map__522044 = p__522043;
var map__522044__$1 = cljs.core.__destructure_map(map__522044);
var position = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522044__$1,new cljs.core.Keyword(null,"position","position",-2011731912));
var duration = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522044__$1,new cljs.core.Keyword(null,"duration","duration",1444101068));
var tone = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522044__$1,new cljs.core.Keyword(null,"tone","tone",-1422788785));
var lyric = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522044__$1,new cljs.core.Keyword(null,"lyric","lyric",164436415));
return (new cljs.core.PersistentArrayMap(null,(8),["position",position,"duration",duration,"tone",tone,"lyric",lyric,"pitch",(new cljs.core.PersistentArrayMap(null,(2),["data",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentArrayMap(null,(3),["x",(-40),"y",(0),"shape","io"],null)),(new cljs.core.PersistentArrayMap(null,(3),["x",(40),"y",(0),"shape","io"],null))], null),"snap_first",true],null)),"vibrato",(new cljs.core.PersistentArrayMap(null,(8),["length",(0),"period",(175),"depth",(25),"in",(10),"out",(10),"shift",(0),"drift",(0),"vol_link",(0)],null)),"phoneme_expressions",cljs.core.PersistentVector.EMPTY,"phoneme_overrides",cljs.core.PersistentVector.EMPTY],null));
}),normalized_notes),"curves",cljs.core.PersistentVector.EMPTY],null))], null),"wave_parts",cljs.core.PersistentVector.EMPTY],null));
});
knoxx.backend.tools.openutau.yaml_scalar = (function knoxx$backend$tools$openutau$yaml_scalar(value){
if((value == null)){
return "null";
} else {
if(typeof value === 'string'){
return (""+"'"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.replace(value,/'/,"''"))+"'");
} else {
if((value instanceof cljs.core.Keyword)){
var G__522045 = cljs.core.name(value);
return (knoxx.backend.tools.openutau.yaml_scalar.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.openutau.yaml_scalar.cljs$core$IFn$_invoke$arity$1(G__522045) : knoxx.backend.tools.openutau.yaml_scalar.call(null,G__522045));
} else {
if(cljs.core.boolean_QMARK_(value)){
if(value){
return "true";
} else {
return "false";
}
} else {
if(typeof value === 'number'){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
} else {
var G__522046 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
return (knoxx.backend.tools.openutau.yaml_scalar.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.openutau.yaml_scalar.cljs$core$IFn$_invoke$arity$1(G__522046) : knoxx.backend.tools.openutau.yaml_scalar.call(null,G__522046));

}
}
}
}
}
});
knoxx.backend.tools.openutau.indent_str = (function knoxx$backend$tools$openutau$indent_str(n){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.repeat.cljs$core$IFn$_invoke$arity$2(n," "));
});
knoxx.backend.tools.openutau.emit_map_lines = (function knoxx$backend$tools$openutau$emit_map_lines(m,indent){
return cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p__522060){
var vec__522061 = p__522060;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522061,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522061,(1),null);
var key = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k));
var prefix = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.indent_str(indent))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key));
if(cljs.core.map_QMARK_(v)){
if(cljs.core.empty_QMARK_(v)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+": {}")], null);
} else {
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+":")], null),(function (){var G__522064 = v;
var G__522065 = (indent + (2));
return (knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2(G__522064,G__522065) : knoxx.backend.tools.openutau.emit_yaml_lines.call(null,G__522064,G__522065));
})());
}
} else {
if(cljs.core.vector_QMARK_(v)){
if(cljs.core.empty_QMARK_(v)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+": []")], null);
} else {
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+":")], null),(function (){var G__522066 = v;
var G__522067 = (indent + (2));
return (knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2(G__522066,G__522067) : knoxx.backend.tools.openutau.emit_yaml_lines.call(null,G__522066,G__522067));
})());
}
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.yaml_scalar(v)))], null);

}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([m], 0));
});
knoxx.backend.tools.openutau.emit_vector_lines = (function knoxx$backend$tools$openutau$emit_vector_lines(xs,indent){
return cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (item){
var prefix = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.indent_str(indent))+"-");
if(cljs.core.map_QMARK_(item)){
if(cljs.core.empty_QMARK_(item)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+" {}")], null);
} else {
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [prefix], null),(function (){var G__522068 = item;
var G__522069 = (indent + (2));
return (knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2(G__522068,G__522069) : knoxx.backend.tools.openutau.emit_yaml_lines.call(null,G__522068,G__522069));
})());
}
} else {
if(cljs.core.vector_QMARK_(item)){
if(cljs.core.empty_QMARK_(item)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+" []")], null);
} else {
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [prefix], null),(function (){var G__522070 = item;
var G__522071 = (indent + (2));
return (knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.openutau.emit_yaml_lines.cljs$core$IFn$_invoke$arity$2(G__522070,G__522071) : knoxx.backend.tools.openutau.emit_yaml_lines.call(null,G__522070,G__522071));
})());
}
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.yaml_scalar(item)))], null);

}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([xs], 0));
});
knoxx.backend.tools.openutau.emit_yaml_lines = (function knoxx$backend$tools$openutau$emit_yaml_lines(value,indent){
if(cljs.core.map_QMARK_(value)){
return knoxx.backend.tools.openutau.emit_map_lines(value,indent);
} else {
if(cljs.core.vector_QMARK_(value)){
return knoxx.backend.tools.openutau.emit_vector_lines(value,indent);
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.indent_str(indent))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.yaml_scalar(value)))], null);

}
}
});
knoxx.backend.tools.openutau.project__GT_ustx_yaml = (function knoxx$backend$tools$openutau$project__GT_ustx_yaml(project){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",knoxx.backend.tools.openutau.emit_yaml_lines(project,(0))))+"\n");
});
knoxx.backend.tools.openutau.readme_markdown = (function knoxx$backend$tools$openutau$readme_markdown(p__522072){
var map__522073 = p__522072;
var map__522073__$1 = cljs.core.__destructure_map(map__522073);
var project_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"project-name","project-name",1486861539));
var ustx_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"ustx-path","ustx-path",242803323));
var readme_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"readme-path","readme-path",205242972));
var note_count = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"note-count","note-count",-2010784834));
var tempo = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"tempo","tempo",-1555208453));
var singer_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"singer-id","singer-id",705189264));
var phonemizer = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522073__$1,new cljs.core.Keyword(null,"phonemizer","phonemizer",-1364007211));
return (""+"# "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(project_name)+"\n\n"+"Generated by Knoxx as an OpenUtau singing-project scaffold.\n\n"+"## Files\n"+"- `"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ustx_path)+"` \u2014 OpenUtau `.ustx` project\n"+"- `"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(readme_path)+"` \u2014 this workflow note\n\n"+"## Current settings\n"+"- Notes: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(note_count)+"\n"+"- Tempo: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tempo)+" BPM\n"+"- Renderer: `"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openutau.default_renderer)+"`\n"+"- Singer: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(singer_id))))?"_(choose in OpenUtau)_":(""+"`"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(singer_id)+"`")))+"\n"+"- Phonemizer: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(phonemizer))))?"_(choose in OpenUtau if needed)_":(""+"`"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(phonemizer)+"`")))+"\n\n"+"## Render workflow\n"+"1. Open `"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ustx_path)+"` in OpenUtau.\n"+"2. If singer or phonemizer is blank, select them on the vocal track.\n"+"3. Review lyrics, phonemes, and pitch/timing.\n"+"4. Export audio with `File > Export Audio > Export wav Files` or `Mixdown To Wav File`.\n\n"+"## Research note\n"+"OpenUtau currently documents UI-based export and does not expose a supported headless `.ustx -> .wav` workflow for backend automation, so Knoxx generates a ready-to-edit project instead of pretending it already rendered audio.\n");
});

//# sourceMappingURL=knoxx.backend.tools.openutau.js.map
