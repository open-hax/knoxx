import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.document_state.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.tools.openplanner');
knoxx.backend.tools.openplanner.memory_search_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Semantic memory search across prior Knoxx sessions and actions indexed in OpenPlanner."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"k","k",-2146297393),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of memory hits to return (default 7, max 12). Reasoning traces are excluded by default."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(12)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional conversation/session id to scope the search."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.openplanner.graph_query_params = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Search text for canonical graph nodes across OpenPlanner lakes."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"lake","lake",805586599),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional lake/project filter such as devel, web, bluesky, or knoxx-session."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"nodeType","nodeType",-639803451),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional node_type filter such as docs, code, visited, assistant_message, tool_result, or reasoning."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of graph nodes to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(20)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"edgeLimit","edgeLimit",-1348630001),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of incident edges to include."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),(60)], null)], null)], null)], null);
knoxx.backend.tools.openplanner.websearch_params = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Live web search query routed through Proxx websearch."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"numResults","numResults",-2106407448),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of results to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(20)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"searchContextSize","searchContextSize",1024145239),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Search context size: low, medium, or high."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"allowedDomains","allowedDomains",975388721),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional domain allowlist."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional Proxx/OpenAI model override for search."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.openplanner.web_read_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"url","url",276297046),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Web link or attachment URL to fetch and read."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maxChars","maxChars",-1468489647),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of characters to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(200),new cljs.core.Keyword(null,"max","max",61366548),(20000)], null)], null)], null)], null);
knoxx.backend.tools.openplanner.memory_session_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Knoxx conversation/session id stored in OpenPlanner."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.openplanner.translation_params = new cljs.core.PersistentVector(null, 9, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source_text","source_text",-1294343676),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Original source text"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"translated_text","translated_text",-108470289),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Translated text"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source_lang","source_lang",-931946297),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Source language code (e.g. 'en')"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"target_lang","target_lang",220363042),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Target language code (e.g. 'es')"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"document_id","document_id",-1715671349),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Document ID being translated"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"garden_id","garden_id",1092752211),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Garden ID"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Project name"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"segment_index","segment_index",1984801663),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"0-based segment index"], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null);
knoxx.backend.tools.openplanner.create_file_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Human-readable title for the new artifact."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"path","path",-188191168),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Relative path for the new file inside the active docs root."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Initial markdown content to write into the new file."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.openplanner.push_claim_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"claim","claim",-975359169),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"The proposition or claim to add to the knowledge graph."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"evidence","evidence",1765855722),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Supporting evidence or a chain of claims that lead to this conclusion."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"probability","probability",-1843961875),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Confidence score from 0.0 to 1.0."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),0.0,new cljs.core.Keyword(null,"max","max",61366548),1.0], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"The source of the claim (e.g. \"web-research\", \"llm-inference\")."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.openplanner.web_read_url_BANG_ = (function knoxx$backend$tools$openplanner$web_read_url_BANG_(url,max_chars){
return fetch(url,({"headers": ({"User-Agent": "Knoxx-Agent/1.0"})})).then((function (resp){
var content_type = (function (){var or__5142__auto__ = resp.headers.get("content-type");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "application/octet-stream";
}
})();
if(cljs.core.not(resp.ok)){
return resp.text().then((function (text){
throw (new Error((""+"web.read failed "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
} else {
if(((clojure.string.starts_with_QMARK_(content_type,"text/")) || (((clojure.string.includes_QMARK_(content_type,"json")) || (((clojure.string.includes_QMARK_(content_type,"xml")) || (clojure.string.includes_QMARK_(content_type,"html")))))))){
return resp.text().then((function (text){
var collapsed = clojure.string.trim(clojure.string.replace(clojure.string.replace(text,/<[^>]+>/," "),/\s+/," "));
var clipped = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(collapsed,(0),cljs.core.min.cljs$core$IFn$_invoke$arity$2(max_chars,((collapsed).length)));
return knoxx.backend.text.tool_text_result((""+"Read URL "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content_type)+"):\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clipped)),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"url","url",276297046),url,new cljs.core.Keyword(null,"contentType","contentType",-1462509576),content_type,new cljs.core.Keyword(null,"text","text",-1790561697),clipped], null));
}));
} else {
return Promise.resolve(knoxx.backend.text.tool_text_result((""+"Fetched URL "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+" with content-type "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content_type)+". Binary/image content is available at the URL for follow-up use."),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"url","url",276297046),url,new cljs.core.Keyword(null,"contentType","contentType",-1462509576),content_type,new cljs.core.Keyword(null,"binary","binary",-1802232288),true], null)));
}
}
}));
});
knoxx.backend.tools.openplanner.slugify = (function knoxx$backend$tools$openplanner$slugify(value){
var raw = clojure.string.replace(clojure.string.replace(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "untitled-canvas";
}
})()))),/[^a-z0-9]+/,"-"),/^-+|-+$/,"");
if(clojure.string.blank_QMARK_(raw)){
return "untitled-canvas";
} else {
return raw;
}
});
knoxx.backend.tools.openplanner.make_memory_search_execute = (function knoxx$backend$tools$openplanner$make_memory_search_execute(auth_context){
return (function (_runtime,config,_tool_call_id,params,a,b,c){
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
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var k = (params["k"]);
var session_id = (function (){var or__5142__auto__ = (params["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Searching Knoxx memory in OpenPlanner\u2026");

return knoxx.backend.openplanner_memory.openplanner_memory_search_BANG_(config,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"k","k",-2146297393),k,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id], null)).then((function (result){
return knoxx.backend.core_memory.filter_authorized_memory_hits_BANG_(config,auth_context,new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(result)).then((function (hits){
var filtered = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(result,new cljs.core.Keyword(null,"hits","hits",-2120002930),hits);
return knoxx.backend.text.tool_text_result(knoxx.backend.text.openplanner_memory_search_text(filtered),filtered);
}));
}));
});
});
knoxx.backend.tools.openplanner.make_memory_session_execute = (function knoxx$backend$tools$openplanner$make_memory_session_execute(auth_context){
return (function (_runtime,config,_tool_call_id,params,a,b,c){
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
var session_id = (function (){var or__5142__auto__ = (params["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Loading Knoxx session from OpenPlanner\u2026");

return knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_(config,session_id).then((function (rows){
if(knoxx.backend.core_memory.session_visible_QMARK_(auth_context,rows)){
} else {
throw knoxx.backend.http.http_error((403),"memory_scope_denied","OpenPlanner session is outside the current Knoxx scope");
}

var payload = (function (){var G__59476 = ({});
(G__59476["sessionId"] = session_id);

(G__59476["rows"] = cljs.core.clj__GT_js(rows));

return G__59476;
})();
return knoxx.backend.text.tool_text_result(knoxx.backend.text.openplanner_session_text(session_id,rows),payload);
}));
});
});
knoxx.backend.tools.openplanner.graph_query_execute = (function knoxx$backend$tools$openplanner$graph_query_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var lake = (function (){var or__5142__auto__ = (params["lake"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var node_type = (function (){var or__5142__auto__ = (params["nodeType"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var limit = (params["limit"]);
var edge_limit = (params["edgeLimit"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Querying canonical knowledge graph\u2026");

return knoxx.backend.openplanner_memory.openplanner_graph_query_BANG_(config,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"lake","lake",805586599),lake,new cljs.core.Keyword(null,"node-type","node-type",129492462),node_type,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"edge-limit","edge-limit",4816756),edge_limit], null)).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.text.graph_query_result_text(result),result);
}));
});
knoxx.backend.tools.openplanner.websearch_execute = (function knoxx$backend$tools$openplanner$websearch_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var num_results = (function (){var or__5142__auto__ = (params["numResults"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8);
}
})();
var search_context_size = (params["searchContextSize"]);
var allowed_domains = (function (){var or__5142__auto__ = (params["allowedDomains"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var model = (params["model"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Searching the live web through Proxx\u2026");

return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/api/tools/websearch"),({"method": "POST", "headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)), "body": JSON.stringify(({"query": query, "numResults": num_results, "searchContextSize": search_context_size, "allowedDomains": allowed_domains, "model": model}))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
var result = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.text.tool_text_result(knoxx.backend.text.websearch_result_text(result),result);
} else {
throw (new Error((""+"websearch failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], 0))))));
}
}));
});
knoxx.backend.tools.openplanner.web_read_execute = (function knoxx$backend$tools$openplanner$web_read_execute(_runtime,_config,_tool_call_id,params,a,b,c){
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
var url = (function (){var or__5142__auto__ = (params["url"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var max_chars = cljs.core.max.cljs$core$IFn$_invoke$arity$2((200),cljs.core.min.cljs$core$IFn$_invoke$arity$2((20000),(function (){var or__5142__auto__ = (params["maxChars"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (6000);
}
})()));
if(clojure.string.blank_QMARK_(clojure.string.trim(url))){
throw (new Error("url is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Fetching "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+"\u2026"));

return knoxx.backend.tools.openplanner.web_read_url_BANG_(url,max_chars);
});
knoxx.backend.tools.openplanner.make_create_new_file_execute = (function knoxx$backend$tools$openplanner$make_create_new_file_execute(auth_context){
return (function (runtime,config,_tool_call_id,params,a,b,c){
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
var title = (function (){var or__5142__auto__ = (params["title"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Untitled Canvas";
}
})();
var requested_path = (function (){var or__5142__auto__ = (params["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"notes/canvas/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.openplanner.slugify(title))+".md");
}
})();
var content = (function (){var or__5142__auto__ = (params["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"# "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(title)+"\n\n");
}
})();
var profile = knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var docs_path = new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile);
var rel_path = knoxx.backend.document_state.normalize_relative_path(requested_path);
var abs_path = knoxx.backend.tools.media.path_resolve.cljs$core$IFn$_invoke$arity$variadic(shadow.esm.esm_import$node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([docs_path,rel_path], 0));
var rel_to_root = knoxx.backend.tools.media.path_relative(shadow.esm.esm_import$node_path,docs_path,abs_path);
var parent = shadow.esm.esm_import$node_path.dirname(abs_path);
if(clojure.string.blank_QMARK_(rel_path)){
throw (new Error("path is required for create_new_file"));
} else {
}

if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_(rel_to_root,"..");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.tools.media.path_is_absolute_QMARK_(shadow.esm.esm_import$node_path,rel_to_root);
}
})())){
throw (new Error("Path escapes active docs root"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Creating canvas file "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(rel_path)+"\u2026"));

return knoxx.backend.tools.media.fs_mkdir_BANG_(shadow.esm.esm_import$node_fs$promises,parent,({"recursive": true})).then((function (){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$4(shadow.esm.esm_import$node_fs$promises,abs_path,content,"utf8");
})).then((function (){
return knoxx.backend.text.tool_text_result((""+"Created canvas file at "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(rel_path)),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"title","title",636505583),title,new cljs.core.Keyword(null,"content","content",15833224),content,new cljs.core.Keyword(null,"canvas","canvas",-1798817489),true], null));
}));
});
});
knoxx.backend.tools.openplanner.make_push_claim_execute = (function knoxx$backend$tools$openplanner$make_push_claim_execute(auth_context){
return (function (runtime,config,_tool_call_id,params,a,b,c){
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
var claim = (params["claim"]);
var evidence = (function (){var or__5142__auto__ = (params["evidence"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var p = (function (){var or__5142__auto__ = (params["probability"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return 0.5;
}
})();
var source = (function (){var or__5142__auto__ = (params["source"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "llm-investigation";
}
})();
var profile = knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var event = knoxx.backend.openplanner_memory.openplanner_event(config,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+"claim:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(crypto.randomUUID())),new cljs.core.Keyword(null,"kind","kind",-717265803),"knoxx.claim",new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"sessionId","sessionId",1640410629).cljs$core$IFn$_invoke$arity$1(auth_context),new cljs.core.Keyword(null,"role","role",-736691072),"assistant",new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(profile),new cljs.core.Keyword(null,"text","text",-1790561697),claim,new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"claim","claim",-975359169),claim,new cljs.core.Keyword(null,"evidence","evidence",1765855722),cljs.core.clj__GT_js(evidence),new cljs.core.Keyword(null,"p","p",151049309),p,new cljs.core.Keyword(null,"src","src",-1651076051),source], null)], null));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Pushing claim to graph: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(claim)+"\u2026"));

return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/events",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [event], null)], null)).then((function (resp){
return knoxx.backend.text.tool_text_result((""+"Successfully pushed claim to graph: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(claim)),resp);
}));
});
});
knoxx.backend.tools.openplanner.make_save_translation_execute = (function knoxx$backend$tools$openplanner$make_save_translation_execute(auth_context){
return (function (_runtime,config,_tool_call_id,params,a,b,c){
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
var resource_policies = new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364).cljs$core$IFn$_invoke$arity$1(auth_context);
var source_text = (params["source_text"]);
var translated_text = (params["translated_text"]);
var source_lang = (function (){var or__5142__auto__ = (params["source_lang"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"source_lang","source_lang",-931946297).cljs$core$IFn$_invoke$arity$1(resource_policies);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"source-lang","source-lang",250384495).cljs$core$IFn$_invoke$arity$1(resource_policies);
}
}
})();
var target_lang = (function (){var or__5142__auto__ = (params["target_lang"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"target_lang","target_lang",220363042).cljs$core$IFn$_invoke$arity$1(resource_policies);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"target-lang","target-lang",1237032111).cljs$core$IFn$_invoke$arity$1(resource_policies);
}
}
})();
var document_id = (function (){var or__5142__auto__ = (params["document_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"document_id","document_id",-1715671349).cljs$core$IFn$_invoke$arity$1(resource_policies);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"document-id","document-id",797275350).cljs$core$IFn$_invoke$arity$1(resource_policies);
}
}
})();
var garden_id = (function (){var or__5142__auto__ = (params["garden_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"garden_id","garden_id",1092752211).cljs$core$IFn$_invoke$arity$1(resource_policies);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"garden-id","garden-id",1609283320).cljs$core$IFn$_invoke$arity$1(resource_policies);
}
}
})();
var project = (function (){var or__5142__auto__ = (params["project"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"project","project",1124394579).cljs$core$IFn$_invoke$arity$1(resource_policies);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
}
}
})();
var segment_index = (params["segment_index"]);
var normalized_source = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = source_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var normalized_translated = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = translated_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var prose_like_QMARK_ = (((((normalized_source).length) > (24))) || (clojure.string.includes_QMARK_(normalized_source," ")));
var _ = (((((!(clojure.string.blank_QMARK_(source_lang)))) && ((((!(clojure.string.blank_QMARK_(target_lang)))) && (((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(source_lang,target_lang)) && (((prose_like_QMARK_) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized_source,normalized_translated))))))))))?(function(){throw (new Error((""+"translated_text matches source_text for segment "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment_index)+"; provide an actual "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_lang)+" translation")))})():null);
var ___$1 = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(document_id))))?(function(){throw (new Error("document_id is required for save_translation"))})():null);
var segment = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"target_lang","target_lang",220363042),new cljs.core.Keyword(null,"mt_model","mt_model",1498814884),new cljs.core.Keyword(null,"source_text","source_text",-1294343676),new cljs.core.Keyword(null,"source_lang","source_lang",-931946297),new cljs.core.Keyword(null,"document_id","document_id",-1715671349),new cljs.core.Keyword(null,"translated_text","translated_text",-108470289),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"garden_id","garden_id",1092752211),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"segment_index","segment_index",1984801663)],[target_lang,"translation-agent",source_text,source_lang,document_id,translated_text,project,garden_id,"pending",segment_index]);
var url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config))+"/v1/translations/segments");
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Saving translation segment "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment_index)+"\u2026"));

return fetch(url,({"method": "POST", "headers": ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config)))}), "body": JSON.stringify(cljs.core.clj__GT_js(segment))})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
})).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Saved segment "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment_index)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(translated_text.substring((0),cljs.core.min.cljs$core$IFn$_invoke$arity$2((50),cljs.core.count(translated_text))))+"\u2026"),result);
}));
});
});
knoxx.backend.tools.openplanner.graph_query_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"graph_query","Graph Query","Query the canonical OpenPlanner knowledge graph across the devel, web, bluesky, and knoxx-session lakes.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search the canonical knowledge graph when you need entities or cross-lake links rather than plain transcript memory or semantic document snippets.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use graph_query when the question is about entities, paths, URLs, provenance across lakes, or graph connectivity.","Prefer graph_query over semantic_query when node/edge structure matters.","Use the lake filter to focus on devel, web, bluesky, or knoxx-session when the search space is obvious."], null),knoxx.backend.tools.openplanner.graph_query_params,knoxx.backend.tools.openplanner.graph_query_execute], 0));
knoxx.backend.tools.openplanner.websearch_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"websearch","Web Search","Search the live web through Proxx websearch and return cited results.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search the live web when the user needs fresh external information or wants to expand the web frontier.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use websearch when freshness matters or when the answer probably lives outside the current graph corpus.","Prefer allowedDomains when you know the likely source surface.","Use websearch to seed follow-up graph or semantic exploration, not as a substitute for graph_query when graph structure already exists."], null),knoxx.backend.tools.openplanner.websearch_params,knoxx.backend.tools.openplanner.websearch_execute], 0));
knoxx.backend.tools.openplanner.web_read_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"web.read","Web Read","Fetch a web link or attachment URL and extract readable text or metadata.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read a web page, text attachment, or direct file URL when you already have a concrete link.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use web.read for direct URLs from Discord messages, embeds, or attachments.","For image/binary URLs, web.read returns metadata so you can decide whether to forward the attachment or inspect it another way."], null),knoxx.backend.tools.openplanner.web_read_params,knoxx.backend.tools.openplanner.web_read_execute], 0));
knoxx.backend.tools.openplanner.memory_search_tool = (function knoxx$backend$tools$openplanner$memory_search_tool(auth_context){
return cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"memory_search","Memory Search","Search prior Knoxx sessions, answers, and tool/action receipts stored in OpenPlanner.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search Knoxx long-term memory in OpenPlanner when the user asks about earlier sessions, prior decisions, or the agent's own past actions.",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use memory_search when the user references previous sessions, past work, or asks you to remember what happened before.","Prefer memory_search over guessing about prior conversations or actions.","Reasoning traces are filtered out of memory_search by default; use memory_session only when exact transcript drill-down is required.","If one session looks relevant, follow with memory_session to inspect the full transcript slice."], null),knoxx.backend.tools.openplanner.memory_search_params,knoxx.backend.tools.openplanner.make_memory_search_execute(auth_context)], 0));
});
knoxx.backend.tools.openplanner.memory_session_tool = (function knoxx$backend$tools$openplanner$memory_session_tool(auth_context){
return cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"memory_session","Memory Session","Load the indexed transcript/events for a specific Knoxx session from OpenPlanner.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Load a specific Knoxx OpenPlanner session when you need the exact previous transcript or action trace.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use memory_session after memory_search identifies a promising session id.","memory_session is the exact transcript/action drill-down companion to memory_search."], null),knoxx.backend.tools.openplanner.memory_session_params,knoxx.backend.tools.openplanner.make_memory_session_execute(auth_context)], 0));
});
knoxx.backend.tools.openplanner.save_translation_tool = (function knoxx$backend$tools$openplanner$save_translation_tool(auth_context){
return cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"save_translation","Save Translation","Save a translated segment to the OpenPlanner translation database.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Save each translated segment after translating.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Call save_translation for each segment you translate.","Include the source_text, translated_text, language codes, document_id, and segment_index."], null),knoxx.backend.tools.openplanner.translation_params,knoxx.backend.tools.openplanner.make_save_translation_execute(auth_context)], 0));
});
knoxx.backend.tools.openplanner.create_new_file_tool = (function knoxx$backend$tools$openplanner$create_new_file_tool(auth_context){
return cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"create_new_file","Create New File","Create a new file-backed artifact for the Knoxx canvas editor.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Create a new concrete artifact file when the user is ready to draft a real document instead of continuing in freeform chat.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use create_new_file when the user wants to start an actual artifact or canvas-backed document.","Return a file path and initial markdown content so the chat canvas can open it immediately."], null),knoxx.backend.tools.openplanner.create_file_params,knoxx.backend.tools.openplanner.make_create_new_file_execute(auth_context)], 0));
});
knoxx.backend.tools.openplanner.push_claim_tool = (function knoxx$backend$tools$openplanner$push_claim_tool(auth_context){
return cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"push_claim","Push Claim","Add a new claim or proposition to the knowledge graph. Use this when you discover a fact or derive an inference during an investigation.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Push a discovered claim or derived inference into the knowledge graph. Treat the graph as an evolving tapestry of hypotheses; the act of recording a discovery is as valuable as the discovery itself.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use push_claim when you uncover a new fact or derive a logical inference during investigation.","Don't obsess over binary truth; prioritize the capture of the epistemic trail.","Include all relevant evidence and a confidence probability."], null),knoxx.backend.tools.openplanner.push_claim_params,knoxx.backend.tools.openplanner.make_push_claim_execute(auth_context)], 0));
});
knoxx.backend.tools.openplanner.create_openplanner_custom_tools = (function knoxx$backend$tools$openplanner$create_openplanner_custom_tools(var_args){
var G__59632 = arguments.length;
switch (G__59632) {
case 2:
return knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((((auth_context == null)) || (((knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"graph_query")) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"semantic_query"))))))?knoxx.backend.tools.openplanner.graph_query_tool(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"websearch"))))?knoxx.backend.tools.openplanner.websearch_tool(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"web.read"))))?knoxx.backend.tools.openplanner.web_read_tool(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"memory_search"))))?knoxx.backend.tools.openplanner.memory_search_tool(auth_context)(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"memory_session"))))?knoxx.backend.tools.openplanner.memory_session_tool(auth_context)(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"save_translation"))))?knoxx.backend.tools.openplanner.save_translation_tool(auth_context)(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"create_new_file"))))?knoxx.backend.tools.openplanner.create_new_file_tool(auth_context)(runtime,config):null),(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"push_claim"))))?knoxx.backend.tools.openplanner.push_claim_tool(auth_context)(runtime,config):null)], null))));
}));

(knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.openplanner.js.map
