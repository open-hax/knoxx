import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.runtime.contract_loader.js";
goog.provide('knoxx.backend.runtime.models');
knoxx.backend.runtime.models.default_model_prefix_allowlist = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, ["glm-5","gpt-5","qwen3","gemma4:","gemma3:","deepseek","kimi-k2","nemotron","cogito","devstral","minimax","ministral","mistral-large"], null);
knoxx.backend.runtime.models.thinking_levels = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 6, ["low",null,"high",null,"off",null,"xhigh",null,"minimal",null,"medium",null], null), null);
knoxx.backend.runtime.models.input_kinds = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["image",null,"video",null,"document",null,"text",null,"audio",null], null), null);
knoxx.backend.runtime.models.eta_mu_registry_input_kinds = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["image",null,"text",null,"audio",null], null), null);
knoxx.backend.runtime.models.parse_prefix_allowlist = (function knoxx$backend$runtime$models$parse_prefix_allowlist(raw){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (v){
var G__33044 = v;
var G__33044__$1 = (((G__33044 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33044)));
var G__33044__$2 = (((G__33044__$1 == null))?null:clojure.string.trim(G__33044__$1));
if((G__33044__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33044__$2);
}
}),clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = raw;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/,/))));
});
knoxx.backend.runtime.models.normalize_thinking_level = (function knoxx$backend$runtime$models$normalize_thinking_level(value){
var normalized = (function (){var G__33046 = value;
var G__33046__$1 = (((G__33046 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33046)));
var G__33046__$2 = (((G__33046__$1 == null))?null:clojure.string.trim(G__33046__$1));
var G__33046__$3 = (((G__33046__$2 == null))?null:clojure.string.lower_case(G__33046__$2));
if((G__33046__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__33046__$3);
}
})();
if(cljs.core.contains_QMARK_(knoxx.backend.runtime.models.thinking_levels,normalized)){
return normalized;
} else {
return null;
}
});
knoxx.backend.runtime.models.normalize_input_kind = (function knoxx$backend$runtime$models$normalize_input_kind(value){
var normalized = (((value instanceof cljs.core.Keyword))?(function (){var G__33052 = value;
var G__33052__$1 = (((G__33052 == null))?null:cljs.core.name(G__33052));
var G__33052__$2 = (((G__33052__$1 == null))?null:clojure.string.trim(G__33052__$1));
var G__33052__$3 = (((G__33052__$2 == null))?null:clojure.string.lower_case(G__33052__$2));
if((G__33052__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__33052__$3);
}
})():(function (){var G__33061 = value;
var G__33061__$1 = (((G__33061 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33061)));
var G__33061__$2 = (((G__33061__$1 == null))?null:clojure.string.trim(G__33061__$1));
var G__33061__$3 = (((G__33061__$2 == null))?null:clojure.string.lower_case(G__33061__$2));
if((G__33061__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__33061__$3);
}
})()
);
if(cljs.core.contains_QMARK_(knoxx.backend.runtime.models.input_kinds,normalized)){
return normalized;
} else {
return null;
}
});
knoxx.backend.runtime.models.normalize_boolean = (function knoxx$backend$runtime$models$normalize_boolean(value){
if(value === true){
return true;
} else {
if(value === false){
return false;
} else {
return null;

}
}
});
knoxx.backend.runtime.models.normalize_string_seq = (function knoxx$backend$runtime$models$normalize_string_seq(values){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
if((value instanceof cljs.core.Keyword)){
var G__33070 = value;
var G__33070__$1 = (((G__33070 == null))?null:cljs.core.name(G__33070));
var G__33070__$2 = (((G__33070__$1 == null))?null:clojure.string.trim(G__33070__$1));
if((G__33070__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33070__$2);
}
} else {
var G__33071 = value;
var G__33071__$1 = (((G__33071 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33071)));
var G__33071__$2 = (((G__33071__$1 == null))?null:clojure.string.trim(G__33071__$1));
if((G__33071__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33071__$2);
}

}
}),(function (){var or__5142__auto__ = values;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.runtime.models.normalize_thinking_level_seq = (function knoxx$backend$runtime$models$normalize_thinking_level_seq(values){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_thinking_level,(function (){var or__5142__auto__ = values;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.runtime.models.normalize_input_kind_seq = (function knoxx$backend$runtime$models$normalize_input_kind_seq(values){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_input_kind,(function (){var or__5142__auto__ = values;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.runtime.models.normalize_provider_id = (function knoxx$backend$runtime$models$normalize_provider_id(value){
if((value instanceof cljs.core.Keyword)){
var G__33077 = value;
var G__33077__$1 = (((G__33077 == null))?null:cljs.core.name(G__33077));
var G__33077__$2 = (((G__33077__$1 == null))?null:clojure.string.trim(G__33077__$1));
if((G__33077__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33077__$2);
}
} else {
var G__33087 = value;
var G__33087__$1 = (((G__33087 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33087)));
var G__33087__$2 = (((G__33087__$1 == null))?null:clojure.string.trim(G__33087__$1));
if((G__33087__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33087__$2);
}

}
});
knoxx.backend.runtime.models.normalize_model_family_contract = (function knoxx$backend$runtime$models$normalize_model_family_contract(contract){
var temp__5825__auto__ = (function (){var G__33099 = new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(contract);
var G__33099__$1 = (((G__33099 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33099)));
var G__33099__$2 = (((G__33099__$1 == null))?null:clojure.string.trim(G__33099__$1));
if((G__33099__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33099__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var family_id = temp__5825__auto__;
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"api","api",-899839580),new cljs.core.Keyword(null,"prefixes","prefixes",1192472197),new cljs.core.Keyword(null,"default-thinking","default-thinking",88361639),new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),new cljs.core.Keyword(null,"allowlisted","allowlisted",-880000589),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"compat","compat",719061591),new cljs.core.Keyword(null,"input","input",556931961),new cljs.core.Keyword(null,"max-tokens","max-tokens",-838881286),new cljs.core.Keyword(null,"thinking-levels","thinking-levels",-1054185637),new cljs.core.Keyword(null,"provider","provider",-302056900),new cljs.core.Keyword(null,"context-window","context-window",1352884956)],[(function (){var G__33100 = new cljs.core.Keyword("model-family","api","model-family/api",535977772).cljs$core$IFn$_invoke$arity$1(contract);
return (knoxx.backend.runtime.models.normalize_model_api.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.models.normalize_model_api.cljs$core$IFn$_invoke$arity$1(G__33100) : knoxx.backend.runtime.models.normalize_model_api.call(null,G__33100));
})(),knoxx.backend.runtime.models.normalize_string_seq(new cljs.core.Keyword("model-family","prefixes","model-family/prefixes",-1282880755).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_thinking_level(new cljs.core.Keyword("model-family","default-thinking","model-family/default-thinking",682603567).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_boolean(new cljs.core.Keyword("model-family","reasoning","model-family/reasoning",-648811405).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_boolean(new cljs.core.Keyword("model-family","allowlisted","model-family/allowlisted",823108667).cljs$core$IFn$_invoke$arity$1(contract)),family_id,((cljs.core.map_QMARK_(new cljs.core.Keyword("model-family","compat","model-family/compat",-1218453809).cljs$core$IFn$_invoke$arity$1(contract)))?new cljs.core.Keyword("model-family","compat","model-family/compat",-1218453809).cljs$core$IFn$_invoke$arity$1(contract):null),knoxx.backend.runtime.models.normalize_input_kind_seq(new cljs.core.Keyword("model-family","input","model-family/input",-599309055).cljs$core$IFn$_invoke$arity$1(contract)),((typeof new cljs.core.Keyword("model-family","max-tokens","model-family/max-tokens",1586676610).cljs$core$IFn$_invoke$arity$1(contract) === 'number')?new cljs.core.Keyword("model-family","max-tokens","model-family/max-tokens",1586676610).cljs$core$IFn$_invoke$arity$1(contract):null),knoxx.backend.runtime.models.normalize_thinking_level_seq(new cljs.core.Keyword("model-family","thinking-levels","model-family/thinking-levels",1413007843).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_provider_id(new cljs.core.Keyword("model-family","provider","model-family/provider",2039068372).cljs$core$IFn$_invoke$arity$1(contract)),((typeof new cljs.core.Keyword("model-family","context-window","model-family/context-window",-1374945948).cljs$core$IFn$_invoke$arity$1(contract) === 'number')?new cljs.core.Keyword("model-family","context-window","model-family/context-window",-1374945948).cljs$core$IFn$_invoke$arity$1(contract):null)]);
} else {
return null;
}
});
knoxx.backend.runtime.models.normalize_model_contract = (function knoxx$backend$runtime$models$normalize_model_contract(contract){
var temp__5825__auto__ = (function (){var G__33101 = new cljs.core.Keyword("model","id","model/id",-1274892501).cljs$core$IFn$_invoke$arity$1(contract);
var G__33101__$1 = (((G__33101 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33101)));
var G__33101__$2 = (((G__33101__$1 == null))?null:clojure.string.trim(G__33101__$1));
if((G__33101__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33101__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var model_id = temp__5825__auto__;
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"api","api",-899839580),new cljs.core.Keyword(null,"default-thinking","default-thinking",88361639),new cljs.core.Keyword(null,"default","default",-1987822328),new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),new cljs.core.Keyword(null,"family-id","family-id",-1606346483),new cljs.core.Keyword(null,"allowlisted","allowlisted",-880000589),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"compat","compat",719061591),new cljs.core.Keyword(null,"input","input",556931961),new cljs.core.Keyword(null,"max-tokens","max-tokens",-838881286),new cljs.core.Keyword(null,"thinking-levels","thinking-levels",-1054185637),new cljs.core.Keyword(null,"provider","provider",-302056900),new cljs.core.Keyword(null,"context-window","context-window",1352884956)],[(function (){var G__33102 = new cljs.core.Keyword("model","api","model/api",-1266057655).cljs$core$IFn$_invoke$arity$1(contract);
return (knoxx.backend.runtime.models.normalize_model_api.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.models.normalize_model_api.cljs$core$IFn$_invoke$arity$1(G__33102) : knoxx.backend.runtime.models.normalize_model_api.call(null,G__33102));
})(),knoxx.backend.runtime.models.normalize_thinking_level(new cljs.core.Keyword("model","default-thinking","model/default-thinking",453531472).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_boolean(new cljs.core.Keyword("model","default","model/default",-1891077679).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_boolean(new cljs.core.Keyword("model","reasoning","model/reasoning",1256885972).cljs$core$IFn$_invoke$arity$1(contract)),(function (){var G__33103 = new cljs.core.Keyword("model-family","id","model-family/id",969625548).cljs$core$IFn$_invoke$arity$1(contract);
var G__33103__$1 = (((G__33103 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33103)));
var G__33103__$2 = (((G__33103__$1 == null))?null:clojure.string.trim(G__33103__$1));
if((G__33103__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33103__$2);
}
})(),knoxx.backend.runtime.models.normalize_boolean(new cljs.core.Keyword("model","allowlisted","model/allowlisted",-1053276966).cljs$core$IFn$_invoke$arity$1(contract)),(function (){var G__33104 = new cljs.core.Keyword("model","label","model/label",-1776753831).cljs$core$IFn$_invoke$arity$1(contract);
var G__33104__$1 = (((G__33104 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33104)));
var G__33104__$2 = (((G__33104__$1 == null))?null:clojure.string.trim(G__33104__$1));
if((G__33104__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33104__$2);
}
})(),model_id,((cljs.core.map_QMARK_(new cljs.core.Keyword("model","compat","model/compat",688390000).cljs$core$IFn$_invoke$arity$1(contract)))?new cljs.core.Keyword("model","compat","model/compat",688390000).cljs$core$IFn$_invoke$arity$1(contract):null),knoxx.backend.runtime.models.normalize_input_kind_seq(new cljs.core.Keyword("model","input","model/input",720249828).cljs$core$IFn$_invoke$arity$1(contract)),((typeof new cljs.core.Keyword("model","max-tokens","model/max-tokens",-269245341).cljs$core$IFn$_invoke$arity$1(contract) === 'number')?new cljs.core.Keyword("model","max-tokens","model/max-tokens",-269245341).cljs$core$IFn$_invoke$arity$1(contract):null),knoxx.backend.runtime.models.normalize_thinking_level_seq(new cljs.core.Keyword("model","thinking-levels","model/thinking-levels",-1025086910).cljs$core$IFn$_invoke$arity$1(contract)),knoxx.backend.runtime.models.normalize_provider_id(new cljs.core.Keyword("model","provider","model/provider",-478996621).cljs$core$IFn$_invoke$arity$1(contract)),((typeof new cljs.core.Keyword("model","context-window","model/context-window",1517771715).cljs$core$IFn$_invoke$arity$1(contract) === 'number')?new cljs.core.Keyword("model","context-window","model/context-window",1517771715).cljs$core$IFn$_invoke$arity$1(contract):null)]);
} else {
return null;
}
});
knoxx.backend.runtime.models.model_family_contracts = (function knoxx$backend$runtime$models$model_family_contracts(config){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_model_family_contract,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contract","contract",798152745),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33105_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("model_families",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__33105_SHARP_));
}),(knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.load_all_contracts_sync.call(null,config)))))));
});
knoxx.backend.runtime.models.model_contracts = (function knoxx$backend$runtime$models$model_contracts(config){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_model_contract,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contract","contract",798152745),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33106_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("models",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__33106_SHARP_));
}),(knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.load_all_contracts_sync.call(null,config)))))));
});
knoxx.backend.runtime.models.resolve_model_family = (function knoxx$backend$runtime$models$resolve_model_family(config,model_id){
var id = (function (){var G__33107 = model_id;
var G__33107__$1 = (((G__33107 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33107)));
var G__33107__$2 = (((G__33107__$1 == null))?null:clojure.string.trim(G__33107__$1));
if((G__33107__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33107__$2);
}
})();
if(cljs.core.truth_(id)){
return cljs.core.first(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2((function (family){
return (- cljs.core.apply.cljs$core$IFn$_invoke$arity$3(cljs.core.max,(0),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.count,new cljs.core.Keyword(null,"prefixes","prefixes",1192472197).cljs$core$IFn$_invoke$arity$1(family))));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (family){
return cljs.core.some((function (prefix){
return clojure.string.starts_with_QMARK_(id,prefix);
}),new cljs.core.Keyword(null,"prefixes","prefixes",1192472197).cljs$core$IFn$_invoke$arity$1(family));
}),knoxx.backend.runtime.models.model_family_contracts(config))));
} else {
return null;
}
});
knoxx.backend.runtime.models.resolve_model_contract = (function knoxx$backend$runtime$models$resolve_model_contract(config,model_id){
var id = (function (){var G__33108 = model_id;
var G__33108__$1 = (((G__33108 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33108)));
var G__33108__$2 = (((G__33108__$1 == null))?null:clojure.string.trim(G__33108__$1));
if((G__33108__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33108__$2);
}
})();
var exact = (cljs.core.truth_(id)?cljs.core.some((function (contract){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract))){
return contract;
} else {
return null;
}
}),knoxx.backend.runtime.models.model_contracts(config)):null);
var family = (function (){var or__5142__auto__ = (function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"family-id","family-id",-1606346483).cljs$core$IFn$_invoke$arity$1(exact);
if(cljs.core.truth_(temp__5825__auto__)){
var family_id = temp__5825__auto__;
return cljs.core.some((function (contract){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(family_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract))){
return contract;
} else {
return null;
}
}),knoxx.backend.runtime.models.model_family_contracts(config));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.runtime.models.resolve_model_family(config,id);
}
})();
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([family,exact], 0));
});
knoxx.backend.runtime.models.fallback_prefix_allowlisted_QMARK_ = (function knoxx$backend$runtime$models$fallback_prefix_allowlisted_QMARK_(config,model_id){
var prefixes = (function (){var configured = cljs.core.seq(new cljs.core.Keyword(null,"model-prefix-allowlist","model-prefix-allowlist",-866915955).cljs$core$IFn$_invoke$arity$1(config));
var or__5142__auto__ = configured;
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.runtime.models.default_model_prefix_allowlist;
}
})();
var id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = model_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
return cljs.core.boolean$(cljs.core.some((function (prefix){
return clojure.string.starts_with_QMARK_(id,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)));
}),prefixes));
});
/**
 * Returns true if model-id should be visible/selectable in Knoxx.
 * 
 * Contract model overrides win. Fallback is env-configured prefix allowlist.
 */
knoxx.backend.runtime.models.allowlisted_model_id_QMARK_ = (function knoxx$backend$runtime$models$allowlisted_model_id_QMARK_(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
if((!((new cljs.core.Keyword(null,"allowlisted","allowlisted",-880000589).cljs$core$IFn$_invoke$arity$1(model_spec) == null)))){
return cljs.core.boolean$(new cljs.core.Keyword(null,"allowlisted","allowlisted",-880000589).cljs$core$IFn$_invoke$arity$1(model_spec));
} else {
return knoxx.backend.runtime.models.fallback_prefix_allowlisted_QMARK_(config,model_id);
}
});
knoxx.backend.runtime.models.model_supports_reasoning_QMARK_ = (function knoxx$backend$runtime$models$model_supports_reasoning_QMARK_(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
if((!((new cljs.core.Keyword(null,"reasoning","reasoning",1956143595).cljs$core$IFn$_invoke$arity$1(model_spec) == null)))){
return cljs.core.boolean$(new cljs.core.Keyword(null,"reasoning","reasoning",1956143595).cljs$core$IFn$_invoke$arity$1(model_spec));
} else {
var normalized_model = (function (){var G__33109 = model_id;
var G__33109__$1 = (((G__33109 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33109)));
var G__33109__$2 = (((G__33109__$1 == null))?null:clojure.string.trim(G__33109__$1));
if((G__33109__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__33109__$2);
}
})();
var prefixes = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"reasoning-model-prefixes","reasoning-model-prefixes",-1475383929).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),/,/)));
return cljs.core.boolean$((function (){var and__5140__auto__ = normalized_model;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.some((function (prefix){
var normalized_prefix = clojure.string.replace(clojure.string.lower_case(prefix),/\\*$/,"");
return clojure.string.starts_with_QMARK_(normalized_model,normalized_prefix);
}),prefixes);
} else {
return and__5140__auto__;
}
})());
}
});
knoxx.backend.runtime.models.normalize_model_api = (function knoxx$backend$runtime$models$normalize_model_api(value){
var normalized = (function (){var G__33110 = value;
var G__33110__$1 = (((G__33110 == null))?null:cljs.core.name(G__33110));
var G__33110__$2 = (((G__33110__$1 == null))?null:clojure.string.trim(G__33110__$1));
var G__33110__$3 = (((G__33110__$2 == null))?null:clojure.string.lower_case(G__33110__$2));
if((G__33110__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__33110__$3);
}
})();
var G__33111 = normalized;
switch (G__33111) {
case "openai-responses":
return "openai-responses";

break;
case "openai/completions":
return "openai-completions";

break;
case "openai-completions":
return "openai-completions";

break;
default:
return null;

}
});
knoxx.backend.runtime.models.model_prefers_responses_QMARK_ = (function knoxx$backend$runtime$models$model_prefers_responses_QMARK_(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var explicit_api = knoxx.backend.runtime.models.normalize_model_api(new cljs.core.Keyword(null,"api","api",-899839580).cljs$core$IFn$_invoke$arity$1(model_spec));
if(cljs.core.truth_(explicit_api)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(explicit_api,"openai-responses");
} else {
var normalized_model = (function (){var G__33112 = model_id;
var G__33112__$1 = (((G__33112 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33112)));
var G__33112__$2 = (((G__33112__$1 == null))?null:clojure.string.trim(G__33112__$1));
if((G__33112__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__33112__$2);
}
})();
var prefixes = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"responses-model-prefixes","responses-model-prefixes",1717925641).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),/,/)));
return cljs.core.boolean$((function (){var and__5140__auto__ = normalized_model;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.some((function (prefix){
var normalized_prefix = clojure.string.replace(clojure.string.lower_case(prefix),/\\*$/,"");
return clojure.string.starts_with_QMARK_(normalized_model,normalized_prefix);
}),prefixes);
} else {
return and__5140__auto__;
}
})());

}
});
knoxx.backend.runtime.models.effective_thinking_level = (function knoxx$backend$runtime$models$effective_thinking_level(config,model_id,requested_thinking_level){
var requested = knoxx.backend.runtime.models.normalize_thinking_level(requested_thinking_level);
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var allowed_levels = (function (){var contract_levels = cljs.core.seq(new cljs.core.Keyword(null,"thinking-levels","thinking-levels",-1054185637).cljs$core$IFn$_invoke$arity$1(model_spec));
if(contract_levels){
return cljs.core.set(contract_levels);
} else {
if(new cljs.core.Keyword(null,"reasoning","reasoning",1956143595).cljs$core$IFn$_invoke$arity$1(model_spec) === false){
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, ["off",null], null), null);
} else {
return knoxx.backend.runtime.models.thinking_levels;

}
}
})();
var default_level = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"default-thinking","default-thinking",88361639).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = requested;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(allowed_levels,requested);
} else {
return and__5140__auto__;
}
})())){
return requested;
} else {
return default_level;
}
});
knoxx.backend.runtime.models.model_thinking_format = (function knoxx$backend$runtime$models$model_thinking_format(model_id){
var normalized_model = (function (){var G__33123 = model_id;
var G__33123__$1 = (((G__33123 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33123)));
var G__33123__$2 = (((G__33123__$1 == null))?null:clojure.string.trim(G__33123__$1));
if((G__33123__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__33123__$2);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = normalized_model;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_(normalized_model,"glm-");
} else {
return and__5140__auto__;
}
})())){
return "zai";
} else {
return null;

}
});
knoxx.backend.runtime.models.model_input_modes = (function knoxx$backend$runtime$models$model_input_modes(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var inputs = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_input_kind,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"input","input",556931961).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.seq(inputs)){
return inputs;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["text"], null);
}
});
knoxx.backend.runtime.models.model_supports_input_QMARK_ = (function knoxx$backend$runtime$models$model_supports_input_QMARK_(config,model_id,input_kind){
var wanted = (function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_input_kind(input_kind);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "text";
}
})();
return cljs.core.boolean$(cljs.core.some((function (p1__33145_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wanted,p1__33145_SHARP_);
}),knoxx.backend.runtime.models.model_input_modes(config,model_id)));
});
knoxx.backend.runtime.models.tool_cost = (function knoxx$backend$runtime$models$tool_cost(){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"input","input",556931961),(0),new cljs.core.Keyword(null,"output","output",-1105869043),(0),new cljs.core.Keyword(null,"cacheRead","cacheRead",1934988063),(0),new cljs.core.Keyword(null,"cacheWrite","cacheWrite",-543722637),(0)], null);
});
knoxx.backend.runtime.models.provider_model_config = (function knoxx$backend$runtime$models$provider_model_config(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var reasoning_QMARK_ = knoxx.backend.runtime.models.model_supports_reasoning_QMARK_(config,model_id);
var api = (function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_model_api(new cljs.core.Keyword(null,"api","api",-899839580).cljs$core$IFn$_invoke$arity$1(model_spec));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(knoxx.backend.runtime.models.model_prefers_responses_QMARK_(config,model_id)){
return "openai-responses";
} else {
return "openai-completions";
}
}
})();
var registry_inputs = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.eta_mu_registry_input_kinds,knoxx.backend.runtime.models.model_input_modes(config,model_id))));
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),model_id,new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return model_id;
}
})(),new cljs.core.Keyword(null,"api","api",-899839580),api,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),reasoning_QMARK_,new cljs.core.Keyword(null,"input","input",556931961),((cljs.core.seq(registry_inputs))?registry_inputs:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["text"], null)),new cljs.core.Keyword(null,"contextWindow","contextWindow",154155169),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"context-window","context-window",1352884956).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (128000);
}
})(),new cljs.core.Keyword(null,"maxTokens","maxTokens",-960030465),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"max-tokens","max-tokens",-838881286).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8192);
}
})(),new cljs.core.Keyword(null,"cost","cost",-1094861735),knoxx.backend.runtime.models.tool_cost()], null);
});
knoxx.backend.runtime.models.provider_openai_base_url = (function knoxx$backend$runtime$models$provider_openai_base_url(base_url){
var base = (function (){var or__5142__auto__ = base_url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(base)){
return null;
} else {
if(clojure.string.ends_with_QMARK_(base,"/v1")){
return base;
} else {
if(clojure.string.ends_with_QMARK_(base,"/")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"v1");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/v1");

}
}
}
});
knoxx.backend.runtime.models.provider_settings_map = (function knoxx$backend$runtime$models$provider_settings_map(config){
var configured_base_urls = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"provider-base-urls","provider-base-urls",1933293909).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var configured_auth_tokens = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"provider-auth-tokens","provider-auth-tokens",1365293080).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var configured_auth_headers = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"provider-auth-headers","provider-auth-headers",2087960896).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var configured_provider_ids = cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.models.normalize_provider_id,cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.keys(configured_base_urls),cljs.core.keys(configured_auth_tokens),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.keys(configured_auth_headers)], 0)))));
var configured_providers = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,provider_id){
var base_url = knoxx.backend.runtime.models.provider_openai_base_url(cljs.core.get.cljs$core$IFn$_invoke$arity$2(configured_base_urls,provider_id));
var api_key = cljs.core.get.cljs$core$IFn$_invoke$arity$2(configured_auth_tokens,provider_id);
var auth_header_raw = (function (){var G__33201 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(configured_auth_headers,provider_id);
var G__33201__$1 = (((G__33201 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33201)));
var G__33201__$2 = (((G__33201__$1 == null))?null:clojure.string.trim(G__33201__$1));
if((G__33201__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__33201__$2);
}
})();
var auth_header_QMARK_ = (((!((auth_header_raw == null))))?cljs.core.not((function (){var fexpr__33207 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["off",null,"false",null,"0",null,"no",null], null), null);
return (fexpr__33207.cljs$core$IFn$_invoke$arity$1 ? fexpr__33207.cljs$core$IFn$_invoke$arity$1(auth_header_raw) : fexpr__33207.call(null,auth_header_raw));
})()):true);
if(cljs.core.truth_(base_url)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(acc,provider_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"baseUrl","baseUrl",122264109),base_url,new cljs.core.Keyword(null,"apiKey","apiKey",-942818613),api_key,new cljs.core.Keyword(null,"authHeader","authHeader",-648098402),auth_header_QMARK_], null));
} else {
return acc;
}
}),cljs.core.PersistentArrayMap.EMPTY,configured_provider_ids);
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, ["proxx",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"baseUrl","baseUrl",122264109),knoxx.backend.runtime.models.provider_openai_base_url(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config)),new cljs.core.Keyword(null,"apiKey","apiKey",-942818613),"PROXX_AUTH_TOKEN",new cljs.core.Keyword(null,"authHeader","authHeader",-648098402),true], null)], null),configured_providers], 0));
});
/**
 * Compute per-model compat so reasoning/thinking settings aren't
 * incorrectly shared across models that don't support them. Contract-declared
 * compat keys win over inferred defaults.
 */
knoxx.backend.runtime.models.per_model_compat = (function knoxx$backend$runtime$models$per_model_compat(config,model_id){
var model_spec = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var inferred = (function (){var G__33217 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"supportsDeveloperRole","supportsDeveloperRole",1359025571),false], null);
var G__33217__$1 = ((knoxx.backend.runtime.models.model_supports_reasoning_QMARK_(config,model_id))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33217,new cljs.core.Keyword(null,"supportsReasoningEffort","supportsReasoningEffort",-557667213),true):G__33217);
if((!((knoxx.backend.runtime.models.model_thinking_format(model_id) == null)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33217__$1,new cljs.core.Keyword(null,"thinkingFormat","thinkingFormat",615539134),knoxx.backend.runtime.models.model_thinking_format(model_id));
} else {
return G__33217__$1;
}
})();
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([inferred,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"compat","compat",719061591).cljs$core$IFn$_invoke$arity$1(model_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
});
/**
 * Model ids Knoxx may select from contracts/config even when Proxx /v1/models
 * is unavailable or omits a contract-backed local model. Eta-mu requires every
 * selected model to exist in models.json before a session can be created.
 */
knoxx.backend.runtime.models.configured_model_ids = (function knoxx$backend$runtime$models$configured_model_ids(config){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (m){
var G__33235 = m;
var G__33235__$1 = (((G__33235 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33235)));
var G__33235__$2 = (((G__33235__$1 == null))?null:clojure.string.trim(G__33235__$1));
if((G__33235__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33235__$2);
}
}),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config)], null),cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (contract){
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,new cljs.core.Keyword(null,"allowlisted","allowlisted",-880000589).cljs$core$IFn$_invoke$arity$1(contract))){
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract);
} else {
return null;
}
}),knoxx.backend.runtime.models.model_contracts(config)))))));
});
knoxx.backend.runtime.models.models_config = (function knoxx$backend$runtime$models$models_config(var_args){
var G__33264 = arguments.length;
switch (G__33264) {
case 1:
return knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$2(config,null);
}));

(knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$2 = (function (config,model_ids){
var normalized_models = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (m){
var G__33296 = m;
var G__33296__$1 = (((G__33296 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33296)));
var G__33296__$2 = (((G__33296__$1 == null))?null:clojure.string.trim(G__33296__$1));
if((G__33296__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33296__$2);
}
}),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(model_ids,knoxx.backend.runtime.models.configured_model_ids(config))))));
var models = ((cljs.core.seq(normalized_models))?normalized_models:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["glm-5"], null));
var base_compat = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"supportsDeveloperRole","supportsDeveloperRole",1359025571),false], null);
var provider_settings = knoxx.backend.runtime.models.provider_settings_map(config);
var models_by_provider = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,model_id){
var provider_id = (function (){var or__5142__auto__ = (function (){var G__33305 = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
var G__33305__$1 = (((G__33305 == null))?null:new cljs.core.Keyword(null,"provider","provider",-302056900).cljs$core$IFn$_invoke$arity$1(G__33305));
if((G__33305__$1 == null)){
return null;
} else {
return knoxx.backend.runtime.models.normalize_provider_id(G__33305__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "proxx";
}
})();
return cljs.core.update.cljs$core$IFn$_invoke$arity$4(acc,provider_id,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),model_id);
}),cljs.core.PersistentArrayMap.EMPTY,models);
var providers = cljs.core.reduce_kv((function (acc,provider_id,provider_model_ids){
var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(provider_settings,provider_id);
if(cljs.core.truth_(temp__5823__auto__)){
var settings = temp__5823__auto__;
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(acc,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(provider_id),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([settings,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"compat","compat",719061591),base_compat,new cljs.core.Keyword(null,"models","models",-1985455662),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (model_id){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.runtime.models.provider_model_config(config,model_id),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"compat","compat",719061591),knoxx.backend.runtime.models.per_model_compat(config,model_id)], null)], 0));
}),provider_model_ids)], null)], 0)));
} else {
return acc;
}
}),cljs.core.PersistentArrayMap.EMPTY,models_by_provider);
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"providers","providers",543153826),providers], null);
}));

(knoxx.backend.runtime.models.models_config.cljs$lang$maxFixedArity = 2);

knoxx.backend.runtime.models.default_model_from_contracts = (function knoxx$backend$runtime$models$default_model_from_contracts(config){
var or__5142__auto__ = (function (){var G__33374 = knoxx.backend.runtime.models.model_contracts(config);
if((G__33374 == null)){
return null;
} else {
return cljs.core.some((function (contract){
if(cljs.core.truth_(new cljs.core.Keyword(null,"default","default",-1987822328).cljs$core$IFn$_invoke$arity$1(contract))){
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract);
} else {
return null;
}
}),G__33374);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__33382 = knoxx.backend.runtime.models.model_contracts(config);
var G__33382__$1 = (((G__33382 == null))?null:cljs.core.first(G__33382));
if((G__33382__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__33382__$1);
}
}
});
/**
 * Augment an env-only config map with derived model config fields.
 * 
 * Keeps knoxx.backend.runtime.config strictly env-only, while ensuring legacy
 * call sites continue to find these keys.
 */
knoxx.backend.runtime.models.enrich_config = (function knoxx$backend$runtime$models$enrich_config(config){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"model-prefix-allowlist","model-prefix-allowlist",-866915955),knoxx.backend.runtime.models.parse_prefix_allowlist((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model-prefix-allowlist","model-prefix-allowlist",-866915955).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["KNOXX_MODEL_PREFIX_ALLOWLIST"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "glm-5,gpt-5,qwen3,gemma4:,gemma3:,deepseek,kimi-k2,nemotron,cogito,devstral,minimax,ministral,mistral-large";
}
}
})()),new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.runtime.models.default_model_from_contracts(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "glm-5";
}
}
})(),new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030),(function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_thinking_level((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["KNOXX_THINKING_LEVEL"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})());
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "off";
}
})(),new cljs.core.Keyword(null,"reasoning-model-prefixes","reasoning-model-prefixes",-1475383929),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"reasoning-model-prefixes","reasoning-model-prefixes",-1475383929).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["KNOXX_REASONING_MODEL_PREFIXES"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "glm-";
}
}
})(),new cljs.core.Keyword(null,"responses-model-prefixes","responses-model-prefixes",1717925641),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"responses-model-prefixes","responses-model-prefixes",1717925641).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["KNOXX_RESPONSES_MODEL_PREFIXES"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "gpt-";
}
}
})()], null),config], 0));
});

//# sourceMappingURL=knoxx.backend.runtime.models.js.map
