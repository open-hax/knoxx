import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_net.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.tools.nrepl');
knoxx.backend.tools.nrepl.default_host = "127.0.0.1";
knoxx.backend.tools.nrepl.default_port = (4500);
knoxx.backend.tools.nrepl.nrepl_eval_params = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"target","target",253001721),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Eval target: cljs (default) forwards into shadow.cljs.devtools.api/cljs-eval; clj evaluates on the JVM nREPL host."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"build_id","build_id",193038620),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"shadow-cljs build id when target=cljs (default: server)."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"CLJS namespace string when target=cljs (default: cljs.user)."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code","code",1586293142),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Code to evaluate. For target=cljs, this must be CLJS code as a string."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"timeout_ms","timeout_ms",70221217),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Timeout in milliseconds (default 15000)."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),(1)], null)], null)], null)], null);
knoxx.backend.tools.nrepl.env = (function knoxx$backend$tools$nrepl$env(k){
try{return (process["env"][k]);
}catch (e58766){var _ = e58766;
return null;
}});
knoxx.backend.tools.nrepl.nrepl_host = (function knoxx$backend$tools$nrepl$nrepl_host(){
var or__5142__auto__ = knoxx.backend.tools.nrepl.env("KNOXX_NREPL_HOST");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.nrepl.default_host;
}
});
knoxx.backend.tools.nrepl.nrepl_port = (function knoxx$backend$tools$nrepl$nrepl_port(){
return parseInt((function (){var or__5142__auto__ = knoxx.backend.tools.nrepl.env("KNOXX_NREPL_PORT");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.nrepl.default_port));
}
})(),(10));
});
knoxx.backend.tools.nrepl.uuid_str = (function knoxx$backend$tools$nrepl$uuid_str(){
try{return shadow.esm.esm_import$node_crypto.randomUUID();
}catch (e58776){var _ = e58776;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.random_uuid()));
}});
knoxx.backend.tools.nrepl.bencode_bytes = (function knoxx$backend$tools$nrepl$bencode_bytes(s){
return Buffer.from((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)),"utf8");
});
knoxx.backend.tools.nrepl.bencode_encode = (function knoxx$backend$tools$nrepl$bencode_encode(v){
if(typeof v === 'string'){
var b = knoxx.backend.tools.nrepl.bencode_bytes(v);
return Buffer.concat(cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [Buffer.from((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b.length)+":"),"utf8"),b], null)));
} else {
if(typeof v === 'number'){
return Buffer.from((""+"i"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Math.trunc(v))+"e"),"utf8");
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = cljs.core.array_QMARK_(v);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ((cljs.core.vector_QMARK_(v)) || (cljs.core.seq_QMARK_(v)));
}
})())){
var items = cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.nrepl.bencode_encode,(cljs.core.truth_(cljs.core.array_QMARK_(v))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(v):v));
return Buffer.concat(cljs.core.clj__GT_js(cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [Buffer.from("l","utf8")], null),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(items,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [Buffer.from("e","utf8")], null)))));
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = cljs.core.map_QMARK_(v);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var and__5140__auto__ = v;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(goog.typeOf(v),"object");
} else {
return and__5140__auto__;
}
}
})())){
var m = ((cljs.core.map_QMARK_(v))?v:cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(v)
);
var entries = cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(cljs.core.first,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__58806){
var vec__58808 = p__58806;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58808,(0),null);
var vv = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58808,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k)),vv], null);
}),m));
var encoded = cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p__58818){
var vec__58819 = p__58818;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58819,(0),null);
var vv = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58819,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1(k) : knoxx.backend.tools.nrepl.bencode_encode.call(null,k)),(knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1(vv) : knoxx.backend.tools.nrepl.bencode_encode.call(null,vv))], null);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([entries], 0));
return Buffer.concat(cljs.core.clj__GT_js(cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [Buffer.from("d","utf8")], null),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(encoded,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [Buffer.from("e","utf8")], null)))));
} else {
if((v == null)){
return (knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1("") : knoxx.backend.tools.nrepl.bencode_encode.call(null,""));
} else {
var G__58832 = cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([v], 0));
return (knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.nrepl.bencode_encode.cljs$core$IFn$_invoke$arity$1(G__58832) : knoxx.backend.tools.nrepl.bencode_encode.call(null,G__58832));

}
}
}
}
}
});
knoxx.backend.tools.nrepl.parse_int = (function knoxx$backend$tools$nrepl$parse_int(buf,start,end){
return parseInt(buf.toString("utf8",start,end),(10));
});
knoxx.backend.tools.nrepl.bencode_decode_string_STAR_ = (function knoxx$backend$tools$nrepl$bencode_decode_string_STAR_(buf,idx){
var colon = buf.indexOf((":".charCodeAt((0)) | 0),idx);
if((colon < (0))){
throw (new Error("Invalid bencode string: missing ':'"));
} else {
}

var len = knoxx.backend.tools.nrepl.parse_int(buf,idx,colon);
var start = (colon + (1));
var end = (start + len);
var s = buf.toString("utf8",start,end);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [s,end], null);
});
knoxx.backend.tools.nrepl.bencode_decode_int_STAR_ = (function knoxx$backend$tools$nrepl$bencode_decode_int_STAR_(buf,idx){
var end = buf.indexOf(("e".charCodeAt((0)) | 0),idx);
if((end < (0))){
throw (new Error("Invalid bencode int: missing 'e'"));
} else {
}

var n = knoxx.backend.tools.nrepl.parse_int(buf,(idx + (1)),end);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [n,(end + (1))], null);
});
knoxx.backend.tools.nrepl.bencode_decode_list_STAR_ = (function knoxx$backend$tools$nrepl$bencode_decode_list_STAR_(buf,idx){
var pos = (idx + (1));
var acc = cljs.core.PersistentVector.EMPTY;
while(true){
if((pos >= buf.length)){
throw (new Error("Invalid bencode list: unexpected EOF"));
} else {
}

if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(buf.toString("utf8",pos,(pos + (1))),"e")){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [acc,(pos + (1))], null);
} else {
var vec__58863 = (knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2(buf,pos) : knoxx.backend.tools.nrepl.bencode_decode_STAR_.call(null,buf,pos));
var item = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58863,(0),null);
var next_pos = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58863,(1),null);
var G__59064 = next_pos;
var G__59065 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,item);
pos = G__59064;
acc = G__59065;
continue;
}
break;
}
});
knoxx.backend.tools.nrepl.bencode_decode_dict_STAR_ = (function knoxx$backend$tools$nrepl$bencode_decode_dict_STAR_(buf,idx){
var pos = (idx + (1));
var acc = cljs.core.PersistentArrayMap.EMPTY;
while(true){
if((pos >= buf.length)){
throw (new Error("Invalid bencode dict: unexpected EOF"));
} else {
}

if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(buf.toString("utf8",pos,(pos + (1))),"e")){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [acc,(pos + (1))], null);
} else {
var vec__58878 = (knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2(buf,pos) : knoxx.backend.tools.nrepl.bencode_decode_STAR_.call(null,buf,pos));
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58878,(0),null);
var next_pos = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58878,(1),null);
var vec__58881 = (knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.tools.nrepl.bencode_decode_STAR_.cljs$core$IFn$_invoke$arity$2(buf,next_pos) : knoxx.backend.tools.nrepl.bencode_decode_STAR_.call(null,buf,next_pos));
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58881,(0),null);
var next_pos2 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58881,(1),null);
var G__59068 = next_pos2;
var G__59069 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(acc,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k)),v);
pos = G__59068;
acc = G__59069;
continue;
}
break;
}
});
knoxx.backend.tools.nrepl.bencode_decode_STAR_ = (function knoxx$backend$tools$nrepl$bencode_decode_STAR_(buf,idx){
if((idx >= buf.length)){
throw (new Error("Invalid bencode: unexpected EOF"));
} else {
}

var ch = buf.toString("utf8",idx,(idx + (1)));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ch,"i")){
return knoxx.backend.tools.nrepl.bencode_decode_int_STAR_(buf,idx);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ch,"l")){
return knoxx.backend.tools.nrepl.bencode_decode_list_STAR_(buf,idx);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ch,"d")){
return knoxx.backend.tools.nrepl.bencode_decode_dict_STAR_(buf,idx);
} else {
if(cljs.core.truth_(cljs.core.re_matches(/[0-9]/,ch))){
return knoxx.backend.tools.nrepl.bencode_decode_string_STAR_(buf,idx);
} else {
throw (new Error((""+"Invalid bencode prefix: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch))));

}
}
}
}
});
knoxx.backend.tools.nrepl.bencode_decode_all = (function knoxx$backend$tools$nrepl$bencode_decode_all(buf){
var pos = (0);
var values = cljs.core.PersistentVector.EMPTY;
while(true){
if((pos >= buf.length)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [values,buf.subarray(pos)], null);
} else {
var decoded = (function (){try{return knoxx.backend.tools.nrepl.bencode_decode_STAR_(buf,pos);
}catch (e58897){var _ = e58897;
return null;
}})();
if((decoded == null)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [values,buf.subarray(pos)], null);
} else {
var vec__58905 = decoded;
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58905,(0),null);
var next_pos = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58905,(1),null);
var G__59071 = next_pos;
var G__59072 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(values,v);
pos = G__59071;
values = G__59072;
continue;
}
}
break;
}
});
knoxx.backend.tools.nrepl.connect_socket_BANG_ = (function knoxx$backend$tools$nrepl$connect_socket_BANG_(){
return (new Promise((function (resolve,reject){
var socket = shadow.esm.esm_import$node_net.createConnection(({"host": knoxx.backend.tools.nrepl.nrepl_host(), "port": knoxx.backend.tools.nrepl.nrepl_port()}));
socket.once("connect",(function (){
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(socket) : resolve.call(null,socket));
}));

return socket.once("error",reject);
})));
});
knoxx.backend.tools.nrepl.socket_write_BANG_ = (function knoxx$backend$tools$nrepl$socket_write_BANG_(socket,msg){
return socket.write(knoxx.backend.tools.nrepl.bencode_encode(msg));
});
knoxx.backend.tools.nrepl.collect_responses_BANG_ = (function knoxx$backend$tools$nrepl$collect_responses_BANG_(socket,id,timeout_ms){
return (new Promise((function (resolve,reject){
var timeout_ms__$1 = (function (){var or__5142__auto__ = timeout_ms;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (15000);
}
})();
var state = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"buf","buf",-213913340),Buffer.alloc((0)),new cljs.core.Keyword(null,"messages","messages",345434482),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"done?","done?",-1847001718),false], null));
var cleanup_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var cleanup = (function (){
var temp__5825__auto__ = cljs.core.deref(cleanup_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var f = temp__5825__auto__;
cljs.core.reset_BANG_(cleanup_STAR_,null);

return (f.cljs$core$IFn$_invoke$arity$0 ? f.cljs$core$IFn$_invoke$arity$0() : f.call(null));
} else {
return null;
}
});
var on_data = (function (chunk){
var buf = Buffer.concat(cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"buf","buf",-213913340).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state)),chunk], null)));
var vec__58941 = knoxx.backend.tools.nrepl.bencode_decode_all(buf);
var values = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58941,(0),null);
var remaining = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58941,(1),null);
var msgs = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (m){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get.cljs$core$IFn$_invoke$arity$3(m,"id",""))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.map_QMARK_,values)));
var statuses = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (m){
var st = cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,"status");
if((st == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(cljs.core.vector_QMARK_(st)){
return st;
} else {
if(cljs.core.seq_QMARK_(st)){
return cljs.core.vec(st);
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(st))){
return cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(st));
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [st], null);

}
}
}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msgs], 0))));
var done_QMARK_ = cljs.core.contains_QMARK_(statuses,"done");
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state,cljs.core.update,new cljs.core.Keyword(null,"messages","messages",345434482),cljs.core.into,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msgs], 0));

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state,cljs.core.assoc,new cljs.core.Keyword(null,"buf","buf",-213913340),remaining,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"done?","done?",-1847001718),done_QMARK_], 0));

if(done_QMARK_){
cleanup();

var G__58958 = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state));
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__58958) : resolve.call(null,G__58958));
} else {
return null;
}
});
var on_error = (function (err){
cleanup();

return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(err) : reject.call(null,err));
});
var on_close = (function (){
cleanup();

var G__58962 = (new Error("nREPL socket closed before :done"));
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(G__58962) : reject.call(null,G__58962));
});
var timeout_handle = setTimeout((function (){
cleanup();

var G__58963 = (new Error((""+"nREPL timeout after "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(timeout_ms__$1)+"ms")));
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(G__58963) : reject.call(null,G__58963));
}),timeout_ms__$1);
cljs.core.reset_BANG_(cleanup_STAR_,(function (){
clearTimeout(timeout_handle);

socket.removeListener("data",on_data);

socket.removeListener("error",on_error);

return socket.removeListener("close",on_close);
}));

socket.on("data",on_data);

socket.on("error",on_error);

return socket.on("close",on_close);
})));
});
knoxx.backend.tools.nrepl.nrepl_clone_BANG_ = (function knoxx$backend$tools$nrepl$nrepl_clone_BANG_(socket,timeout_ms){
var id = knoxx.backend.tools.nrepl.uuid_str();
var msg = new cljs.core.PersistentArrayMap(null, 2, ["op","clone","id",id], null);
knoxx.backend.tools.nrepl.socket_write_BANG_(socket,msg);

return knoxx.backend.tools.nrepl.collect_responses_BANG_(socket,id,timeout_ms).then((function (responses){
var session = cljs.core.some((function (m){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,"new-session");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,"session");
}
}),responses);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
throw (new Error((""+"nREPL clone did not return a session: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([responses], 0))))));
} else {
}

return session;
}));
});
knoxx.backend.tools.nrepl.nrepl_eval_BANG_ = (function knoxx$backend$tools$nrepl$nrepl_eval_BANG_(socket,session,code,p__58971){
var map__58972 = p__58971;
var map__58972__$1 = cljs.core.__destructure_map(map__58972);
var timeout_ms = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58972__$1,new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406));
var id = knoxx.backend.tools.nrepl.uuid_str();
var msg = new cljs.core.PersistentArrayMap(null, 4, ["op","eval","id",id,"session",session,"code",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code))], null);
knoxx.backend.tools.nrepl.socket_write_BANG_(socket,msg);

return knoxx.backend.tools.nrepl.collect_responses_BANG_(socket,id,timeout_ms);
});
knoxx.backend.tools.nrepl.summarize_eval = (function knoxx$backend$tools$nrepl$summarize_eval(responses){
var values = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__58973_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(p1__58973_SHARP_,"value");
}),responses)));
var out = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__58974_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(p1__58974_SHARP_,"out");
}),responses)));
var err = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__58975_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(p1__58975_SHARP_,"err");
}),responses)));
var ex = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__58976_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(p1__58976_SHARP_,"ex");
}),responses)));
var statuses = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (m){
var st = cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,"status");
if((st == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(cljs.core.vector_QMARK_(st)){
return st;
} else {
if(cljs.core.seq_QMARK_(st)){
return cljs.core.vec(st);
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(st))){
return cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(st));
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [st], null);

}
}
}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([responses], 0))))));
var value = cljs.core.last(values);
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"values","values",372645556),values,new cljs.core.Keyword(null,"out","out",-910545517),out,new cljs.core.Keyword(null,"err","err",-2089457205),err,new cljs.core.Keyword(null,"ex","ex",-1413771341),ex,new cljs.core.Keyword(null,"status","status",-1997798413),statuses,new cljs.core.Keyword(null,"response_count","response_count",1689185133),cljs.core.count(responses)], null);
});
knoxx.backend.tools.nrepl.escape_clj_string = (function knoxx$backend$tools$nrepl$escape_clj_string(s){
return cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))], 0));
});
knoxx.backend.tools.nrepl.shadow_cljs_eval_form = (function knoxx$backend$tools$nrepl$shadow_cljs_eval_form(p__58989){
var map__58990 = p__58989;
var map__58990__$1 = cljs.core.__destructure_map(map__58990);
var build_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58990__$1,new cljs.core.Keyword(null,"build-id","build-id",1642831089));
var ns = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58990__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var code = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58990__$1,new cljs.core.Keyword(null,"code","code",1586293142));
return (""+"(do "+"(require (quote [shadow.cljs.devtools.api :as api])) "+"(api/cljs-eval "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.keyword.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = build_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "server";
}
})())], 0)))+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.nrepl.escape_clj_string((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code))))+" "+"{:ns "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.nrepl.escape_clj_string((function (){var or__5142__auto__ = ns;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "cljs.user";
}
})()))+"})"+")");
});
knoxx.backend.tools.nrepl.nrepl_eval_execute = (function knoxx$backend$tools$nrepl$nrepl_eval_execute(_runtime,_config,_tool_call_id,params,a,b,c){
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
var target = (function (){var G__58994 = (function (){var or__5142__auto__ = (params["target"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "cljs";
}
})();
var G__58994__$1 = (((G__58994 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58994)));
if((G__58994__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__58994__$1);
}
})();
var build_id = (function (){var or__5142__auto__ = (params["build_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["buildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "server";
}
}
})();
var ns = (function (){var or__5142__auto__ = (params["ns"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "cljs.user";
}
})();
var code = (function (){var or__5142__auto__ = (params["code"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var timeout_ms = (function (){var t = (function (){var or__5142__auto__ = (params["timeout_ms"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["timeoutMs"]);
}
})();
if((!((t == null)))){
return Math.trunc(t);
} else {
return null;
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code)))){
throw (new Error("code is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Connecting to nREPL at "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.nrepl.nrepl_host())+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.nrepl.nrepl_port())+"\u2026"));

return knoxx.backend.tools.nrepl.connect_socket_BANG_().then((function (socket){
return knoxx.backend.tools.nrepl.nrepl_clone_BANG_(socket,timeout_ms).then((function (session){
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"nREPL session ready; evaluating "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target)+"\u2026"));

var eval_code = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(target,"clj"))?code:knoxx.backend.tools.nrepl.shadow_cljs_eval_form(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"build-id","build-id",1642831089),build_id,new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"code","code",1586293142),code], null)));
return knoxx.backend.tools.nrepl.nrepl_eval_BANG_(socket,session,eval_code,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406),timeout_ms], null)).then((function (responses){
socket.end();

var summary = knoxx.backend.tools.nrepl.summarize_eval(responses);
var vec__58996 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"out","out",-910545517).cljs$core$IFn$_invoke$arity$1(summary),(20000));
var clipped_out = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58996,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58996,(1),null);
var vec__58999 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"err","err",-2089457205).cljs$core$IFn$_invoke$arity$1(summary),(20000));
var clipped_err = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58999,(0),null);
var ___$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58999,(1),null);
var headline = (""+"nREPL "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target)+" eval"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(target,"cljs"))?(""+" build="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(build_id)+" ns="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)):null))+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(summary);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "(no value)";
}
})()));
return knoxx.backend.text.tool_text_result((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(headline)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(clipped_out))?null:(""+"\n\n[stdout]\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clipped_out))))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(clipped_err))?null:(""+"\n\n[stderr]\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clipped_err))))),cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(summary,new cljs.core.Keyword(null,"target","target",253001721),target,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"build_id","build_id",193038620),build_id,new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"code","code",1586293142),code], 0)));
})).catch((function (err){
socket.end();

throw err;
}));
})).catch((function (err){
socket.end();

throw err;
}));
}));
});
knoxx.backend.tools.nrepl.nrepl_eval_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"nrepl.eval","nREPL Eval","Evaluate CLJ or CLJS against the live shadow-cljs runtime via nREPL. Developer-only.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Evaluate code in the live Knoxx runtime via shadow-cljs nREPL (dangerous).",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use for live runtime experimentation against the running Knoxx backend.","Default target=cljs forwards to shadow.cljs.devtools.api/cljs-eval build=server.","Prefer small, reversible changes; avoid long-running loops.","After experimentation, persist changes into source files and commit them."], null),knoxx.backend.tools.nrepl.nrepl_eval_params,knoxx.backend.tools.nrepl.nrepl_eval_execute], 0));
knoxx.backend.tools.nrepl.create_nrepl_custom_tools = (function knoxx$backend$tools$nrepl$create_nrepl_custom_tools(var_args){
var G__59018 = arguments.length;
switch (G__59018) {
case 2:
return knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("nrepl.eval"))?knoxx.backend.tools.nrepl.nrepl_eval_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.nrepl.js.map
