import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.contracts');
knoxx.backend.tools.contracts.contract_write_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Contract ID to create or update."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"edn_text","edn_text",258296122),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Complete EDN contract text to save. Must be valid EDN with :contract/id matching contract_id."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.contracts.contract_validate_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"edn_text","edn_text",258296122),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"EDN contract text to validate. Returns :ok, :errors, :contract-class, and parsed :contract on success."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contract_class","contract_class",490905262),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Contract class hint: agents, roles, capabilities, actors, policies. Defaults to agents."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.contracts.contract_write_execute = (function knoxx$backend$tools$contracts$contract_write_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var contract_id = (function (){var or__5142__auto__ = (params["contract_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["contractId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var edn_text = (function (){var or__5142__auto__ = (params["edn_text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["ednText"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var base_url = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(contract_id)){
throw (new Error("contract_id is required"));
} else {
}

if(clojure.string.blank_QMARK_(edn_text)){
throw (new Error("edn_text is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Saving contract "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id)+"\u2026"));

return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_url)+"/api/agent/contracts/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(contract_id))),({"method": "PUT", "headers": ({"Content-Type": "text/plain; charset=utf-8"}), "body": edn_text})).then((function (resp){
return resp.text();
})).then((function (result_text){
return knoxx.backend.text.tool_text_result((""+"Contract save result for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id)+":\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(result_text)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),contract_id,new cljs.core.Keyword(null,"result","result",1415092211),result_text], null));
}));
});
knoxx.backend.tools.contracts.contract_validate_execute = (function knoxx$backend$tools$contracts$contract_validate_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var edn_text = (function (){var or__5142__auto__ = (params["edn_text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["ednText"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var klass = (function (){var or__5142__auto__ = (params["contract_class"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "agents";
}
})();
var base_url = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(edn_text)){
throw (new Error("edn_text is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Validating contract EDN\u2026");

return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_url)+"/api/admin/contracts/validate"),({"method": "POST", "headers": ({"Content-Type": "application/json"}), "body": JSON.stringify(({"edn_text": edn_text, "class": klass}))})).then((function (resp){
return resp.json();
})).then((function (result){
var r = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var ok_QMARK_ = new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(r);
var errors = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(r);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
return knoxx.backend.text.tool_text_result((cljs.core.truth_(ok_QMARK_)?(""+"\u2713 Contract valid"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(r,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contract","contract",798152745),new cljs.core.Keyword("contract","id","contract/id",-872298206)], null));
if(cljs.core.truth_(temp__5825__auto__)){
var cid = temp__5825__auto__;
return (""+" \u2014 :contract/id "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cid));
} else {
return null;
}
})())+"\nClass: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract-class","contract-class",-393992188).cljs$core$IFn$_invoke$arity$1(r);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return klass;
}
})())):(""+"\u2717 Validation failed:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (err){
var path = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(err);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "root";
}
})();
var msg = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(err);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Unknown error";
}
})();
return (""+"  \u2022 ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(" > ",path))+"]: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg));
}),errors))))),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
}));
});
knoxx.backend.tools.contracts.contract_write_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"contract.write","Contract Write","Create or update a contract by writing EDN text. Validates before saving. This is your ONLY write tool \u2014 use it to create and edit contracts.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Write or update a contract's EDN text.",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use contract.write to save contract EDN.","The EDN must have :contract/id matching the contract_id parameter.","The server validates before saving \u2014 if validation fails, fix the EDN and retry.","This is the ONLY write tool available to you. No bash, no discord, no general write.","Before saving, call contract.validate to catch parse errors without side effects."], null),knoxx.backend.tools.contracts.contract_write_params,knoxx.backend.tools.contracts.contract_write_execute], 0));
knoxx.backend.tools.contracts.contract_validate_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"contract.validate","Contract Validate","Parse and validate EDN contract text without saving. Use this BEFORE contract.write to catch errors early.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Validate contract EDN before saving.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Always validate before writing a contract.","If :ok is false, fix the errors and validate again before calling contract.write.","Use contract_class hint when you know the type (agents, roles, capabilities, actors, policies)."], null),knoxx.backend.tools.contracts.contract_validate_params,knoxx.backend.tools.contracts.contract_validate_execute], 0));
/**
 * Create contract tools for the contract librarian agent.
 * Only contract.write — the librarian reads context via general read/memory tools
 * and writes EDN contracts. Validation is implicit (the write endpoint validates).
 */
knoxx.backend.tools.contracts.create_contract_custom_tools = (function knoxx$backend$tools$contracts$create_contract_custom_tools(var_args){
var G__58837 = arguments.length;
switch (G__58837) {
case 2:
return knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("contract.write"))?knoxx.backend.tools.contracts.contract_write_tool(runtime,config):null),((allowed_QMARK_("contract.validate"))?knoxx.backend.tools.contracts.contract_validate_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$lang$maxFixedArity = 3);

/**
 * Create the full tool suite for the contract librarian agent.
 * Has all READ tools (memory, graph, semantic, websearch) plus contract.write.
 * This is the toolset used in the contract editor chat panel.
 */
knoxx.backend.tools.contracts.create_contract_librarian_tools = (function knoxx$backend$tools$contracts$create_contract_librarian_tools(var_args){
var G__58844 = arguments.length;
switch (G__58844) {
case 2:
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$4(runtime,config,auth_context,null);
}));

(knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,auth_context,allowed_tool_ids){
var contract_tools = knoxx.backend.tools.contracts.create_contract_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var read_tools = [];
var memory_tools = [];
return knoxx.backend.tools.shared.filter_custom_tools_by_allow_set(knoxx.backend.tools.shared.sanitize_custom_tools(contract_tools.concat(read_tools).concat(memory_tools)),allowed_tool_ids);
}));

(knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$lang$maxFixedArity = 4);


//# sourceMappingURL=knoxx.backend.tools.contracts.js.map
