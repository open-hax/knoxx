goog.provide('shadow.dom');
shadow.dom.transition_supported_QMARK_ = true;

/**
 * @interface
 */
shadow.dom.IElement = function(){};

var shadow$dom$IElement$_to_dom$dyn_12400 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (shadow.dom._to_dom[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5499__auto__.call(null,this$));
} else {
var m__5497__auto__ = (shadow.dom._to_dom["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5497__auto__.call(null,this$));
} else {
throw cljs.core.missing_protocol("IElement.-to-dom",this$);
}
}
});
shadow.dom._to_dom = (function shadow$dom$_to_dom(this$){
if((((!((this$ == null)))) && ((!((this$.shadow$dom$IElement$_to_dom$arity$1 == null)))))){
return this$.shadow$dom$IElement$_to_dom$arity$1(this$);
} else {
return shadow$dom$IElement$_to_dom$dyn_12400(this$);
}
});


/**
 * @interface
 */
shadow.dom.SVGElement = function(){};

var shadow$dom$SVGElement$_to_svg$dyn_12406 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (shadow.dom._to_svg[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5499__auto__.call(null,this$));
} else {
var m__5497__auto__ = (shadow.dom._to_svg["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5497__auto__.call(null,this$));
} else {
throw cljs.core.missing_protocol("SVGElement.-to-svg",this$);
}
}
});
shadow.dom._to_svg = (function shadow$dom$_to_svg(this$){
if((((!((this$ == null)))) && ((!((this$.shadow$dom$SVGElement$_to_svg$arity$1 == null)))))){
return this$.shadow$dom$SVGElement$_to_svg$arity$1(this$);
} else {
return shadow$dom$SVGElement$_to_svg$dyn_12406(this$);
}
});

shadow.dom.lazy_native_coll_seq = (function shadow$dom$lazy_native_coll_seq(coll,idx){
if((idx < coll.length)){
return (new cljs.core.LazySeq(null,(function (){
return cljs.core.cons((coll[idx]),(function (){var G__10198 = coll;
var G__10199 = (idx + (1));
return (shadow.dom.lazy_native_coll_seq.cljs$core$IFn$_invoke$arity$2 ? shadow.dom.lazy_native_coll_seq.cljs$core$IFn$_invoke$arity$2(G__10198,G__10199) : shadow.dom.lazy_native_coll_seq.call(null,G__10198,G__10199));
})());
}),null,null));
} else {
return null;
}
});

/**
* @constructor
 * @implements {cljs.core.IIndexed}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IDeref}
 * @implements {shadow.dom.IElement}
*/
shadow.dom.NativeColl = (function (coll){
this.coll = coll;
this.cljs$lang$protocol_mask$partition0$ = 8421394;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(shadow.dom.NativeColl.prototype.cljs$core$IDeref$_deref$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.coll;
}));

(shadow.dom.NativeColl.prototype.cljs$core$IIndexed$_nth$arity$2 = (function (this$,n){
var self__ = this;
var this$__$1 = this;
return (self__.coll[n]);
}));

(shadow.dom.NativeColl.prototype.cljs$core$IIndexed$_nth$arity$3 = (function (this$,n,not_found){
var self__ = this;
var this$__$1 = this;
var or__5142__auto__ = (self__.coll[n]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return not_found;
}
}));

(shadow.dom.NativeColl.prototype.cljs$core$ICounted$_count$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.coll.length;
}));

(shadow.dom.NativeColl.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return shadow.dom.lazy_native_coll_seq(self__.coll,(0));
}));

(shadow.dom.NativeColl.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(shadow.dom.NativeColl.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.coll;
}));

(shadow.dom.NativeColl.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null);
}));

(shadow.dom.NativeColl.cljs$lang$type = true);

(shadow.dom.NativeColl.cljs$lang$ctorStr = "shadow.dom/NativeColl");

(shadow.dom.NativeColl.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"shadow.dom/NativeColl");
}));

/**
 * Positional factory function for shadow.dom/NativeColl.
 */
shadow.dom.__GT_NativeColl = (function shadow$dom$__GT_NativeColl(coll){
return (new shadow.dom.NativeColl(coll));
});

shadow.dom.native_coll = (function shadow$dom$native_coll(coll){
return (new shadow.dom.NativeColl(coll));
});
shadow.dom.dom_node = (function shadow$dom$dom_node(el){
if((el == null)){
return null;
} else {
if((((!((el == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === el.shadow$dom$IElement$))))?true:false):false)){
return el.shadow$dom$IElement$_to_dom$arity$1(null);
} else {
if(typeof el === 'string'){
return document.createTextNode(el);
} else {
if(typeof el === 'number'){
return document.createTextNode((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(el)));
} else {
return el;

}
}
}
}
});
shadow.dom.query_one = (function shadow$dom$query_one(var_args){
var G__10230 = arguments.length;
switch (G__10230) {
case 1:
return shadow.dom.query_one.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.query_one.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.query_one.cljs$core$IFn$_invoke$arity$1 = (function (sel){
return document.querySelector(sel);
}));

(shadow.dom.query_one.cljs$core$IFn$_invoke$arity$2 = (function (sel,root){
return shadow.dom.dom_node(root).querySelector(sel);
}));

(shadow.dom.query_one.cljs$lang$maxFixedArity = 2);

shadow.dom.query = (function shadow$dom$query(var_args){
var G__10237 = arguments.length;
switch (G__10237) {
case 1:
return shadow.dom.query.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.query.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.query.cljs$core$IFn$_invoke$arity$1 = (function (sel){
return (new shadow.dom.NativeColl(document.querySelectorAll(sel)));
}));

(shadow.dom.query.cljs$core$IFn$_invoke$arity$2 = (function (sel,root){
return (new shadow.dom.NativeColl(shadow.dom.dom_node(root).querySelectorAll(sel)));
}));

(shadow.dom.query.cljs$lang$maxFixedArity = 2);

shadow.dom.by_id = (function shadow$dom$by_id(var_args){
var G__10247 = arguments.length;
switch (G__10247) {
case 2:
return shadow.dom.by_id.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 1:
return shadow.dom.by_id.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.by_id.cljs$core$IFn$_invoke$arity$2 = (function (id,el){
return shadow.dom.dom_node(el).getElementById(id);
}));

(shadow.dom.by_id.cljs$core$IFn$_invoke$arity$1 = (function (id){
return document.getElementById(id);
}));

(shadow.dom.by_id.cljs$lang$maxFixedArity = 2);

shadow.dom.build = shadow.dom.dom_node;
shadow.dom.ev_stop = (function shadow$dom$ev_stop(var_args){
var G__10254 = arguments.length;
switch (G__10254) {
case 1:
return shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$1 = (function (e){
if(cljs.core.truth_(e.stopPropagation)){
e.stopPropagation();

e.preventDefault();
} else {
(e.cancelBubble = true);

(e.returnValue = false);
}

return e;
}));

(shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$2 = (function (e,el){
shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$1(e);

return el;
}));

(shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$4 = (function (e,el,scope,owner){
shadow.dom.ev_stop.cljs$core$IFn$_invoke$arity$1(e);

return el;
}));

(shadow.dom.ev_stop.cljs$lang$maxFixedArity = 4);

/**
 * check wether a parent node (or the document) contains the child
 */
shadow.dom.contains_QMARK_ = (function shadow$dom$contains_QMARK_(var_args){
var G__10307 = arguments.length;
switch (G__10307) {
case 1:
return shadow.dom.contains_QMARK_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.contains_QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.contains_QMARK_.cljs$core$IFn$_invoke$arity$1 = (function (el){
return goog.dom.contains(document,shadow.dom.dom_node(el));
}));

(shadow.dom.contains_QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (parent,el){
return goog.dom.contains(shadow.dom.dom_node(parent),shadow.dom.dom_node(el));
}));

(shadow.dom.contains_QMARK_.cljs$lang$maxFixedArity = 2);

shadow.dom.add_class = (function shadow$dom$add_class(el,cls){
return goog.dom.classlist.add(shadow.dom.dom_node(el),cls);
});
shadow.dom.remove_class = (function shadow$dom$remove_class(el,cls){
return goog.dom.classlist.remove(shadow.dom.dom_node(el),cls);
});
shadow.dom.toggle_class = (function shadow$dom$toggle_class(var_args){
var G__10444 = arguments.length;
switch (G__10444) {
case 2:
return shadow.dom.toggle_class.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return shadow.dom.toggle_class.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.toggle_class.cljs$core$IFn$_invoke$arity$2 = (function (el,cls){
return goog.dom.classlist.toggle(shadow.dom.dom_node(el),cls);
}));

(shadow.dom.toggle_class.cljs$core$IFn$_invoke$arity$3 = (function (el,cls,v){
if(cljs.core.truth_(v)){
return shadow.dom.add_class(el,cls);
} else {
return shadow.dom.remove_class(el,cls);
}
}));

(shadow.dom.toggle_class.cljs$lang$maxFixedArity = 3);

shadow.dom.dom_listen = (cljs.core.truth_((function (){var or__5142__auto__ = (!((typeof document !== 'undefined')));
if(or__5142__auto__){
return or__5142__auto__;
} else {
return document.addEventListener;
}
})())?(function shadow$dom$dom_listen_good(el,ev,handler){
return el.addEventListener(ev,handler,false);
}):(function shadow$dom$dom_listen_ie(el,ev,handler){
try{return el.attachEvent((""+"on"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ev)),(function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
}));
}catch (e10675){if((e10675 instanceof Object)){
var e = e10675;
return console.log("didnt support attachEvent",el,e);
} else {
throw e10675;

}
}}));
shadow.dom.dom_listen_remove = (cljs.core.truth_((function (){var or__5142__auto__ = (!((typeof document !== 'undefined')));
if(or__5142__auto__){
return or__5142__auto__;
} else {
return document.removeEventListener;
}
})())?(function shadow$dom$dom_listen_remove_good(el,ev,handler){
return el.removeEventListener(ev,handler,false);
}):(function shadow$dom$dom_listen_remove_ie(el,ev,handler){
return el.detachEvent((""+"on"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ev)),handler);
}));
shadow.dom.on_query = (function shadow$dom$on_query(root_el,ev,selector,handler){
var seq__10766 = cljs.core.seq(shadow.dom.query.cljs$core$IFn$_invoke$arity$2(selector,root_el));
var chunk__10767 = null;
var count__10768 = (0);
var i__10769 = (0);
while(true){
if((i__10769 < count__10768)){
var el = chunk__10767.cljs$core$IIndexed$_nth$arity$2(null,i__10769);
var handler_12431__$1 = ((function (seq__10766,chunk__10767,count__10768,i__10769,el){
return (function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
});})(seq__10766,chunk__10767,count__10768,i__10769,el))
;
shadow.dom.dom_listen(el,cljs.core.name(ev),handler_12431__$1);


var G__12434 = seq__10766;
var G__12435 = chunk__10767;
var G__12436 = count__10768;
var G__12437 = (i__10769 + (1));
seq__10766 = G__12434;
chunk__10767 = G__12435;
count__10768 = G__12436;
i__10769 = G__12437;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__10766);
if(temp__5823__auto__){
var seq__10766__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__10766__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__10766__$1);
var G__12438 = cljs.core.chunk_rest(seq__10766__$1);
var G__12439 = c__5673__auto__;
var G__12440 = cljs.core.count(c__5673__auto__);
var G__12441 = (0);
seq__10766 = G__12438;
chunk__10767 = G__12439;
count__10768 = G__12440;
i__10769 = G__12441;
continue;
} else {
var el = cljs.core.first(seq__10766__$1);
var handler_12442__$1 = ((function (seq__10766,chunk__10767,count__10768,i__10769,el,seq__10766__$1,temp__5823__auto__){
return (function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
});})(seq__10766,chunk__10767,count__10768,i__10769,el,seq__10766__$1,temp__5823__auto__))
;
shadow.dom.dom_listen(el,cljs.core.name(ev),handler_12442__$1);


var G__12443 = cljs.core.next(seq__10766__$1);
var G__12444 = null;
var G__12445 = (0);
var G__12446 = (0);
seq__10766 = G__12443;
chunk__10767 = G__12444;
count__10768 = G__12445;
i__10769 = G__12446;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.dom.on = (function shadow$dom$on(var_args){
var G__10820 = arguments.length;
switch (G__10820) {
case 3:
return shadow.dom.on.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return shadow.dom.on.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.on.cljs$core$IFn$_invoke$arity$3 = (function (el,ev,handler){
return shadow.dom.on.cljs$core$IFn$_invoke$arity$4(el,ev,handler,false);
}));

(shadow.dom.on.cljs$core$IFn$_invoke$arity$4 = (function (el,ev,handler,capture){
if(cljs.core.vector_QMARK_(ev)){
return shadow.dom.on_query(el,cljs.core.first(ev),cljs.core.second(ev),handler);
} else {
var handler__$1 = (function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
});
return shadow.dom.dom_listen(shadow.dom.dom_node(el),cljs.core.name(ev),handler__$1);
}
}));

(shadow.dom.on.cljs$lang$maxFixedArity = 4);

shadow.dom.remove_event_handler = (function shadow$dom$remove_event_handler(el,ev,handler){
return shadow.dom.dom_listen_remove(shadow.dom.dom_node(el),cljs.core.name(ev),handler);
});
shadow.dom.add_event_listeners = (function shadow$dom$add_event_listeners(el,events){
var seq__10841 = cljs.core.seq(events);
var chunk__10842 = null;
var count__10843 = (0);
var i__10844 = (0);
while(true){
if((i__10844 < count__10843)){
var vec__10891 = chunk__10842.cljs$core$IIndexed$_nth$arity$2(null,i__10844);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10891,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10891,(1),null);
shadow.dom.on.cljs$core$IFn$_invoke$arity$3(el,k,v);


var G__12453 = seq__10841;
var G__12454 = chunk__10842;
var G__12455 = count__10843;
var G__12456 = (i__10844 + (1));
seq__10841 = G__12453;
chunk__10842 = G__12454;
count__10843 = G__12455;
i__10844 = G__12456;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__10841);
if(temp__5823__auto__){
var seq__10841__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__10841__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__10841__$1);
var G__12458 = cljs.core.chunk_rest(seq__10841__$1);
var G__12459 = c__5673__auto__;
var G__12460 = cljs.core.count(c__5673__auto__);
var G__12461 = (0);
seq__10841 = G__12458;
chunk__10842 = G__12459;
count__10843 = G__12460;
i__10844 = G__12461;
continue;
} else {
var vec__10907 = cljs.core.first(seq__10841__$1);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10907,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10907,(1),null);
shadow.dom.on.cljs$core$IFn$_invoke$arity$3(el,k,v);


var G__12468 = cljs.core.next(seq__10841__$1);
var G__12469 = null;
var G__12470 = (0);
var G__12471 = (0);
seq__10841 = G__12468;
chunk__10842 = G__12469;
count__10843 = G__12470;
i__10844 = G__12471;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.dom.set_style = (function shadow$dom$set_style(el,styles){
var dom = shadow.dom.dom_node(el);
var seq__10921 = cljs.core.seq(styles);
var chunk__10922 = null;
var count__10923 = (0);
var i__10924 = (0);
while(true){
if((i__10924 < count__10923)){
var vec__10941 = chunk__10922.cljs$core$IIndexed$_nth$arity$2(null,i__10924);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10941,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10941,(1),null);
goog.style.setStyle(dom,cljs.core.name(k),(((v == null))?"":v));


var G__12476 = seq__10921;
var G__12477 = chunk__10922;
var G__12478 = count__10923;
var G__12479 = (i__10924 + (1));
seq__10921 = G__12476;
chunk__10922 = G__12477;
count__10923 = G__12478;
i__10924 = G__12479;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__10921);
if(temp__5823__auto__){
var seq__10921__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__10921__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__10921__$1);
var G__12480 = cljs.core.chunk_rest(seq__10921__$1);
var G__12481 = c__5673__auto__;
var G__12482 = cljs.core.count(c__5673__auto__);
var G__12483 = (0);
seq__10921 = G__12480;
chunk__10922 = G__12481;
count__10923 = G__12482;
i__10924 = G__12483;
continue;
} else {
var vec__10954 = cljs.core.first(seq__10921__$1);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10954,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__10954,(1),null);
goog.style.setStyle(dom,cljs.core.name(k),(((v == null))?"":v));


var G__12484 = cljs.core.next(seq__10921__$1);
var G__12485 = null;
var G__12486 = (0);
var G__12487 = (0);
seq__10921 = G__12484;
chunk__10922 = G__12485;
count__10923 = G__12486;
i__10924 = G__12487;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.dom.set_attr_STAR_ = (function shadow$dom$set_attr_STAR_(el,key,value){
var G__10982_12488 = key;
var G__10982_12489__$1 = (((G__10982_12488 instanceof cljs.core.Keyword))?G__10982_12488.fqn:null);
switch (G__10982_12489__$1) {
case "id":
(el.id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));

break;
case "class":
(el.className = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));

break;
case "for":
(el.htmlFor = value);

break;
case "cellpadding":
el.setAttribute("cellPadding",value);

break;
case "cellspacing":
el.setAttribute("cellSpacing",value);

break;
case "colspan":
el.setAttribute("colSpan",value);

break;
case "frameborder":
el.setAttribute("frameBorder",value);

break;
case "height":
el.setAttribute("height",value);

break;
case "maxlength":
el.setAttribute("maxLength",value);

break;
case "role":
el.setAttribute("role",value);

break;
case "rowspan":
el.setAttribute("rowSpan",value);

break;
case "type":
el.setAttribute("type",value);

break;
case "usemap":
el.setAttribute("useMap",value);

break;
case "valign":
el.setAttribute("vAlign",value);

break;
case "width":
el.setAttribute("width",value);

break;
case "on":
shadow.dom.add_event_listeners(el,value);

break;
case "style":
if((value == null)){
} else {
if(typeof value === 'string'){
el.setAttribute("style",value);
} else {
if(cljs.core.map_QMARK_(value)){
shadow.dom.set_style(el,value);
} else {
goog.style.setStyle(el,value);

}
}
}

break;
default:
var ks_12494 = cljs.core.name(key);
if(cljs.core.truth_((function (){var or__5142__auto__ = goog.string.startsWith(ks_12494,"data-");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return goog.string.startsWith(ks_12494,"aria-");
}
})())){
el.setAttribute(ks_12494,value);
} else {
(el[ks_12494] = value);
}

}

return el;
});
shadow.dom.set_attrs = (function shadow$dom$set_attrs(el,attrs){
return cljs.core.reduce_kv((function (el__$1,key,value){
shadow.dom.set_attr_STAR_(el__$1,key,value);

return el__$1;
}),shadow.dom.dom_node(el),attrs);
});
shadow.dom.set_attr = (function shadow$dom$set_attr(el,key,value){
return shadow.dom.set_attr_STAR_(shadow.dom.dom_node(el),key,value);
});
shadow.dom.has_class_QMARK_ = (function shadow$dom$has_class_QMARK_(el,cls){
return goog.dom.classlist.contains(shadow.dom.dom_node(el),cls);
});
shadow.dom.merge_class_string = (function shadow$dom$merge_class_string(current,extra_class){
if(cljs.core.seq(current)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(current)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(extra_class));
} else {
return extra_class;
}
});
shadow.dom.parse_tag = (function shadow$dom$parse_tag(spec){
var spec__$1 = cljs.core.name(spec);
var fdot = spec__$1.indexOf(".");
var fhash = spec__$1.indexOf("#");
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((-1),fdot)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((-1),fhash)))){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [spec__$1,null,null], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((-1),fhash)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [spec__$1.substring((0),fdot),null,clojure.string.replace(spec__$1.substring((fdot + (1))),/\./," ")], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((-1),fdot)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [spec__$1.substring((0),fhash),spec__$1.substring((fhash + (1))),null], null);
} else {
if((fhash > fdot)){
throw (""+"cant have id after class?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(spec__$1));
} else {
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [spec__$1.substring((0),fhash),spec__$1.substring((fhash + (1)),fdot),clojure.string.replace(spec__$1.substring((fdot + (1))),/\./," ")], null);

}
}
}
}
});
shadow.dom.create_dom_node = (function shadow$dom$create_dom_node(tag_def,p__11020){
var map__11021 = p__11020;
var map__11021__$1 = cljs.core.__destructure_map(map__11021);
var props = map__11021__$1;
var class$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__11021__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var tag_props = ({});
var vec__11022 = shadow.dom.parse_tag(tag_def);
var tag_name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11022,(0),null);
var tag_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11022,(1),null);
var tag_classes = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11022,(2),null);
if(cljs.core.truth_(tag_id)){
(tag_props["id"] = tag_id);
} else {
}

if(cljs.core.truth_(tag_classes)){
(tag_props["class"] = shadow.dom.merge_class_string(class$,tag_classes));
} else {
}

var G__11034 = goog.dom.createDom(tag_name,tag_props);
shadow.dom.set_attrs(G__11034,cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(props,new cljs.core.Keyword(null,"class","class",-2030961996)));

return G__11034;
});
shadow.dom.append = (function shadow$dom$append(var_args){
var G__11041 = arguments.length;
switch (G__11041) {
case 1:
return shadow.dom.append.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.append.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.append.cljs$core$IFn$_invoke$arity$1 = (function (node){
if(cljs.core.truth_(node)){
var temp__5823__auto__ = shadow.dom.dom_node(node);
if(cljs.core.truth_(temp__5823__auto__)){
var n = temp__5823__auto__;
document.body.appendChild(n);

return n;
} else {
return null;
}
} else {
return null;
}
}));

(shadow.dom.append.cljs$core$IFn$_invoke$arity$2 = (function (el,node){
if(cljs.core.truth_(node)){
var temp__5823__auto__ = shadow.dom.dom_node(node);
if(cljs.core.truth_(temp__5823__auto__)){
var n = temp__5823__auto__;
shadow.dom.dom_node(el).appendChild(n);

return n;
} else {
return null;
}
} else {
return null;
}
}));

(shadow.dom.append.cljs$lang$maxFixedArity = 2);

shadow.dom.destructure_node = (function shadow$dom$destructure_node(create_fn,p__11049){
var vec__11053 = p__11049;
var seq__11054 = cljs.core.seq(vec__11053);
var first__11055 = cljs.core.first(seq__11054);
var seq__11054__$1 = cljs.core.next(seq__11054);
var nn = first__11055;
var first__11055__$1 = cljs.core.first(seq__11054__$1);
var seq__11054__$2 = cljs.core.next(seq__11054__$1);
var np = first__11055__$1;
var nc = seq__11054__$2;
var node = vec__11053;
if((nn instanceof cljs.core.Keyword)){
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("invalid dom node",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"node","node",581201198),node], null));
}

if((((np == null)) && ((nc == null)))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var G__11056 = nn;
var G__11057 = cljs.core.PersistentArrayMap.EMPTY;
return (create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(G__11056,G__11057) : create_fn.call(null,G__11056,G__11057));
})(),cljs.core.List.EMPTY], null);
} else {
if(cljs.core.map_QMARK_(np)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(nn,np) : create_fn.call(null,nn,np)),nc], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var G__11058 = nn;
var G__11059 = cljs.core.PersistentArrayMap.EMPTY;
return (create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(G__11058,G__11059) : create_fn.call(null,G__11058,G__11059));
})(),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(nc,np)], null);

}
}
});
shadow.dom.make_dom_node = (function shadow$dom$make_dom_node(structure){
var vec__11061 = shadow.dom.destructure_node(shadow.dom.create_dom_node,structure);
var node = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11061,(0),null);
var node_children = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11061,(1),null);
var seq__11066_12502 = cljs.core.seq(node_children);
var chunk__11067_12503 = null;
var count__11068_12504 = (0);
var i__11069_12505 = (0);
while(true){
if((i__11069_12505 < count__11068_12504)){
var child_struct_12506 = chunk__11067_12503.cljs$core$IIndexed$_nth$arity$2(null,i__11069_12505);
var children_12507 = shadow.dom.dom_node(child_struct_12506);
if(cljs.core.seq_QMARK_(children_12507)){
var seq__11219_12508 = cljs.core.seq(cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom.dom_node,children_12507));
var chunk__11221_12509 = null;
var count__11222_12510 = (0);
var i__11223_12511 = (0);
while(true){
if((i__11223_12511 < count__11222_12510)){
var child_12512 = chunk__11221_12509.cljs$core$IIndexed$_nth$arity$2(null,i__11223_12511);
if(cljs.core.truth_(child_12512)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_12512);


var G__12513 = seq__11219_12508;
var G__12514 = chunk__11221_12509;
var G__12515 = count__11222_12510;
var G__12516 = (i__11223_12511 + (1));
seq__11219_12508 = G__12513;
chunk__11221_12509 = G__12514;
count__11222_12510 = G__12515;
i__11223_12511 = G__12516;
continue;
} else {
var G__12517 = seq__11219_12508;
var G__12518 = chunk__11221_12509;
var G__12519 = count__11222_12510;
var G__12520 = (i__11223_12511 + (1));
seq__11219_12508 = G__12517;
chunk__11221_12509 = G__12518;
count__11222_12510 = G__12519;
i__11223_12511 = G__12520;
continue;
}
} else {
var temp__5823__auto___12521 = cljs.core.seq(seq__11219_12508);
if(temp__5823__auto___12521){
var seq__11219_12522__$1 = temp__5823__auto___12521;
if(cljs.core.chunked_seq_QMARK_(seq__11219_12522__$1)){
var c__5673__auto___12523 = cljs.core.chunk_first(seq__11219_12522__$1);
var G__12524 = cljs.core.chunk_rest(seq__11219_12522__$1);
var G__12525 = c__5673__auto___12523;
var G__12526 = cljs.core.count(c__5673__auto___12523);
var G__12527 = (0);
seq__11219_12508 = G__12524;
chunk__11221_12509 = G__12525;
count__11222_12510 = G__12526;
i__11223_12511 = G__12527;
continue;
} else {
var child_12528 = cljs.core.first(seq__11219_12522__$1);
if(cljs.core.truth_(child_12528)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_12528);


var G__12529 = cljs.core.next(seq__11219_12522__$1);
var G__12530 = null;
var G__12531 = (0);
var G__12532 = (0);
seq__11219_12508 = G__12529;
chunk__11221_12509 = G__12530;
count__11222_12510 = G__12531;
i__11223_12511 = G__12532;
continue;
} else {
var G__12533 = cljs.core.next(seq__11219_12522__$1);
var G__12534 = null;
var G__12535 = (0);
var G__12536 = (0);
seq__11219_12508 = G__12533;
chunk__11221_12509 = G__12534;
count__11222_12510 = G__12535;
i__11223_12511 = G__12536;
continue;
}
}
} else {
}
}
break;
}
} else {
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,children_12507);
}


var G__12537 = seq__11066_12502;
var G__12538 = chunk__11067_12503;
var G__12539 = count__11068_12504;
var G__12540 = (i__11069_12505 + (1));
seq__11066_12502 = G__12537;
chunk__11067_12503 = G__12538;
count__11068_12504 = G__12539;
i__11069_12505 = G__12540;
continue;
} else {
var temp__5823__auto___12541 = cljs.core.seq(seq__11066_12502);
if(temp__5823__auto___12541){
var seq__11066_12542__$1 = temp__5823__auto___12541;
if(cljs.core.chunked_seq_QMARK_(seq__11066_12542__$1)){
var c__5673__auto___12543 = cljs.core.chunk_first(seq__11066_12542__$1);
var G__12544 = cljs.core.chunk_rest(seq__11066_12542__$1);
var G__12545 = c__5673__auto___12543;
var G__12546 = cljs.core.count(c__5673__auto___12543);
var G__12547 = (0);
seq__11066_12502 = G__12544;
chunk__11067_12503 = G__12545;
count__11068_12504 = G__12546;
i__11069_12505 = G__12547;
continue;
} else {
var child_struct_12548 = cljs.core.first(seq__11066_12542__$1);
var children_12549 = shadow.dom.dom_node(child_struct_12548);
if(cljs.core.seq_QMARK_(children_12549)){
var seq__11247_12550 = cljs.core.seq(cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom.dom_node,children_12549));
var chunk__11249_12551 = null;
var count__11250_12552 = (0);
var i__11251_12553 = (0);
while(true){
if((i__11251_12553 < count__11250_12552)){
var child_12554 = chunk__11249_12551.cljs$core$IIndexed$_nth$arity$2(null,i__11251_12553);
if(cljs.core.truth_(child_12554)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_12554);


var G__12555 = seq__11247_12550;
var G__12556 = chunk__11249_12551;
var G__12557 = count__11250_12552;
var G__12558 = (i__11251_12553 + (1));
seq__11247_12550 = G__12555;
chunk__11249_12551 = G__12556;
count__11250_12552 = G__12557;
i__11251_12553 = G__12558;
continue;
} else {
var G__12559 = seq__11247_12550;
var G__12560 = chunk__11249_12551;
var G__12561 = count__11250_12552;
var G__12562 = (i__11251_12553 + (1));
seq__11247_12550 = G__12559;
chunk__11249_12551 = G__12560;
count__11250_12552 = G__12561;
i__11251_12553 = G__12562;
continue;
}
} else {
var temp__5823__auto___12563__$1 = cljs.core.seq(seq__11247_12550);
if(temp__5823__auto___12563__$1){
var seq__11247_12564__$1 = temp__5823__auto___12563__$1;
if(cljs.core.chunked_seq_QMARK_(seq__11247_12564__$1)){
var c__5673__auto___12565 = cljs.core.chunk_first(seq__11247_12564__$1);
var G__12566 = cljs.core.chunk_rest(seq__11247_12564__$1);
var G__12567 = c__5673__auto___12565;
var G__12568 = cljs.core.count(c__5673__auto___12565);
var G__12569 = (0);
seq__11247_12550 = G__12566;
chunk__11249_12551 = G__12567;
count__11250_12552 = G__12568;
i__11251_12553 = G__12569;
continue;
} else {
var child_12570 = cljs.core.first(seq__11247_12564__$1);
if(cljs.core.truth_(child_12570)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_12570);


var G__12571 = cljs.core.next(seq__11247_12564__$1);
var G__12572 = null;
var G__12573 = (0);
var G__12574 = (0);
seq__11247_12550 = G__12571;
chunk__11249_12551 = G__12572;
count__11250_12552 = G__12573;
i__11251_12553 = G__12574;
continue;
} else {
var G__12575 = cljs.core.next(seq__11247_12564__$1);
var G__12576 = null;
var G__12577 = (0);
var G__12578 = (0);
seq__11247_12550 = G__12575;
chunk__11249_12551 = G__12576;
count__11250_12552 = G__12577;
i__11251_12553 = G__12578;
continue;
}
}
} else {
}
}
break;
}
} else {
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,children_12549);
}


var G__12579 = cljs.core.next(seq__11066_12542__$1);
var G__12580 = null;
var G__12581 = (0);
var G__12582 = (0);
seq__11066_12502 = G__12579;
chunk__11067_12503 = G__12580;
count__11068_12504 = G__12581;
i__11069_12505 = G__12582;
continue;
}
} else {
}
}
break;
}

return node;
});
(cljs.core.Keyword.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.Keyword.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var this$__$1 = this;
return shadow.dom.make_dom_node(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [this$__$1], null));
}));

(cljs.core.PersistentVector.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.PersistentVector.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var this$__$1 = this;
return shadow.dom.make_dom_node(this$__$1);
}));

(cljs.core.LazySeq.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.LazySeq.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var this$__$1 = this;
return cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom._to_dom,this$__$1);
}));
if(cljs.core.truth_(((typeof HTMLElement) != 'undefined'))){
(HTMLElement.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(HTMLElement.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1;
}));
} else {
}
if(cljs.core.truth_(((typeof DocumentFragment) != 'undefined'))){
(DocumentFragment.prototype.shadow$dom$IElement$ = cljs.core.PROTOCOL_SENTINEL);

(DocumentFragment.prototype.shadow$dom$IElement$_to_dom$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1;
}));
} else {
}
/**
 * clear node children
 */
shadow.dom.reset = (function shadow$dom$reset(node){
return goog.dom.removeChildren(shadow.dom.dom_node(node));
});
shadow.dom.remove = (function shadow$dom$remove(node){
if((((!((node == null))))?(((((node.cljs$lang$protocol_mask$partition0$ & (8388608))) || ((cljs.core.PROTOCOL_SENTINEL === node.cljs$core$ISeqable$))))?true:false):false)){
var seq__11268 = cljs.core.seq(node);
var chunk__11269 = null;
var count__11270 = (0);
var i__11271 = (0);
while(true){
if((i__11271 < count__11270)){
var n = chunk__11269.cljs$core$IIndexed$_nth$arity$2(null,i__11271);
(shadow.dom.remove.cljs$core$IFn$_invoke$arity$1 ? shadow.dom.remove.cljs$core$IFn$_invoke$arity$1(n) : shadow.dom.remove.call(null,n));


var G__12589 = seq__11268;
var G__12590 = chunk__11269;
var G__12591 = count__11270;
var G__12592 = (i__11271 + (1));
seq__11268 = G__12589;
chunk__11269 = G__12590;
count__11270 = G__12591;
i__11271 = G__12592;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__11268);
if(temp__5823__auto__){
var seq__11268__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__11268__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__11268__$1);
var G__12593 = cljs.core.chunk_rest(seq__11268__$1);
var G__12594 = c__5673__auto__;
var G__12595 = cljs.core.count(c__5673__auto__);
var G__12596 = (0);
seq__11268 = G__12593;
chunk__11269 = G__12594;
count__11270 = G__12595;
i__11271 = G__12596;
continue;
} else {
var n = cljs.core.first(seq__11268__$1);
(shadow.dom.remove.cljs$core$IFn$_invoke$arity$1 ? shadow.dom.remove.cljs$core$IFn$_invoke$arity$1(n) : shadow.dom.remove.call(null,n));


var G__12597 = cljs.core.next(seq__11268__$1);
var G__12598 = null;
var G__12599 = (0);
var G__12600 = (0);
seq__11268 = G__12597;
chunk__11269 = G__12598;
count__11270 = G__12599;
i__11271 = G__12600;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return goog.dom.removeNode(node);
}
});
shadow.dom.replace_node = (function shadow$dom$replace_node(old,new$){
return goog.dom.replaceNode(shadow.dom.dom_node(new$),shadow.dom.dom_node(old));
});
shadow.dom.text = (function shadow$dom$text(var_args){
var G__11290 = arguments.length;
switch (G__11290) {
case 2:
return shadow.dom.text.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 1:
return shadow.dom.text.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.text.cljs$core$IFn$_invoke$arity$2 = (function (el,new_text){
return (shadow.dom.dom_node(el).innerText = new_text);
}));

(shadow.dom.text.cljs$core$IFn$_invoke$arity$1 = (function (el){
return shadow.dom.dom_node(el).innerText;
}));

(shadow.dom.text.cljs$lang$maxFixedArity = 2);

shadow.dom.check = (function shadow$dom$check(var_args){
var G__11295 = arguments.length;
switch (G__11295) {
case 1:
return shadow.dom.check.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.check.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.check.cljs$core$IFn$_invoke$arity$1 = (function (el){
return shadow.dom.check.cljs$core$IFn$_invoke$arity$2(el,true);
}));

(shadow.dom.check.cljs$core$IFn$_invoke$arity$2 = (function (el,checked){
return (shadow.dom.dom_node(el).checked = checked);
}));

(shadow.dom.check.cljs$lang$maxFixedArity = 2);

shadow.dom.checked_QMARK_ = (function shadow$dom$checked_QMARK_(el){
return shadow.dom.dom_node(el).checked;
});
shadow.dom.form_elements = (function shadow$dom$form_elements(el){
return (new shadow.dom.NativeColl(shadow.dom.dom_node(el).elements));
});
shadow.dom.children = (function shadow$dom$children(el){
return (new shadow.dom.NativeColl(shadow.dom.dom_node(el).children));
});
shadow.dom.child_nodes = (function shadow$dom$child_nodes(el){
return (new shadow.dom.NativeColl(shadow.dom.dom_node(el).childNodes));
});
shadow.dom.attr = (function shadow$dom$attr(var_args){
var G__11318 = arguments.length;
switch (G__11318) {
case 2:
return shadow.dom.attr.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return shadow.dom.attr.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.attr.cljs$core$IFn$_invoke$arity$2 = (function (el,key){
return shadow.dom.dom_node(el).getAttribute(cljs.core.name(key));
}));

(shadow.dom.attr.cljs$core$IFn$_invoke$arity$3 = (function (el,key,default$){
var or__5142__auto__ = shadow.dom.dom_node(el).getAttribute(cljs.core.name(key));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
}));

(shadow.dom.attr.cljs$lang$maxFixedArity = 3);

shadow.dom.del_attr = (function shadow$dom$del_attr(el,key){
return shadow.dom.dom_node(el).removeAttribute(cljs.core.name(key));
});
shadow.dom.data = (function shadow$dom$data(el,key){
return shadow.dom.dom_node(el).getAttribute((""+"data-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(key))));
});
shadow.dom.set_data = (function shadow$dom$set_data(el,key,value){
return shadow.dom.dom_node(el).setAttribute((""+"data-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(key))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));
});
shadow.dom.set_html = (function shadow$dom$set_html(node,text){
return (shadow.dom.dom_node(node).innerHTML = text);
});
shadow.dom.get_html = (function shadow$dom$get_html(node){
return shadow.dom.dom_node(node).innerHTML;
});
shadow.dom.fragment = (function shadow$dom$fragment(var_args){
var args__5882__auto__ = [];
var len__5876__auto___12607 = arguments.length;
var i__5877__auto___12608 = (0);
while(true){
if((i__5877__auto___12608 < len__5876__auto___12607)){
args__5882__auto__.push((arguments[i__5877__auto___12608]));

var G__12609 = (i__5877__auto___12608 + (1));
i__5877__auto___12608 = G__12609;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return shadow.dom.fragment.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(shadow.dom.fragment.cljs$core$IFn$_invoke$arity$variadic = (function (nodes){
var fragment = document.createDocumentFragment();
var seq__11343_12610 = cljs.core.seq(nodes);
var chunk__11344_12611 = null;
var count__11345_12612 = (0);
var i__11346_12613 = (0);
while(true){
if((i__11346_12613 < count__11345_12612)){
var node_12614 = chunk__11344_12611.cljs$core$IIndexed$_nth$arity$2(null,i__11346_12613);
fragment.appendChild(shadow.dom._to_dom(node_12614));


var G__12615 = seq__11343_12610;
var G__12616 = chunk__11344_12611;
var G__12617 = count__11345_12612;
var G__12618 = (i__11346_12613 + (1));
seq__11343_12610 = G__12615;
chunk__11344_12611 = G__12616;
count__11345_12612 = G__12617;
i__11346_12613 = G__12618;
continue;
} else {
var temp__5823__auto___12619 = cljs.core.seq(seq__11343_12610);
if(temp__5823__auto___12619){
var seq__11343_12620__$1 = temp__5823__auto___12619;
if(cljs.core.chunked_seq_QMARK_(seq__11343_12620__$1)){
var c__5673__auto___12621 = cljs.core.chunk_first(seq__11343_12620__$1);
var G__12623 = cljs.core.chunk_rest(seq__11343_12620__$1);
var G__12624 = c__5673__auto___12621;
var G__12625 = cljs.core.count(c__5673__auto___12621);
var G__12626 = (0);
seq__11343_12610 = G__12623;
chunk__11344_12611 = G__12624;
count__11345_12612 = G__12625;
i__11346_12613 = G__12626;
continue;
} else {
var node_12627 = cljs.core.first(seq__11343_12620__$1);
fragment.appendChild(shadow.dom._to_dom(node_12627));


var G__12628 = cljs.core.next(seq__11343_12620__$1);
var G__12629 = null;
var G__12630 = (0);
var G__12631 = (0);
seq__11343_12610 = G__12628;
chunk__11344_12611 = G__12629;
count__11345_12612 = G__12630;
i__11346_12613 = G__12631;
continue;
}
} else {
}
}
break;
}

return (new shadow.dom.NativeColl(fragment));
}));

(shadow.dom.fragment.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(shadow.dom.fragment.cljs$lang$applyTo = (function (seq11337){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq11337));
}));

/**
 * given a html string, eval all <script> tags and return the html without the scripts
 * don't do this for everything, only content you trust.
 */
shadow.dom.eval_scripts = (function shadow$dom$eval_scripts(s){
var scripts = cljs.core.re_seq(/<script[^>]*?>(.+?)<\/script>/,s);
var seq__11372_12632 = cljs.core.seq(scripts);
var chunk__11373_12633 = null;
var count__11374_12634 = (0);
var i__11375_12635 = (0);
while(true){
if((i__11375_12635 < count__11374_12634)){
var vec__11388_12636 = chunk__11373_12633.cljs$core$IIndexed$_nth$arity$2(null,i__11375_12635);
var script_tag_12637 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11388_12636,(0),null);
var script_body_12638 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11388_12636,(1),null);
eval(script_body_12638);


var G__12639 = seq__11372_12632;
var G__12640 = chunk__11373_12633;
var G__12641 = count__11374_12634;
var G__12642 = (i__11375_12635 + (1));
seq__11372_12632 = G__12639;
chunk__11373_12633 = G__12640;
count__11374_12634 = G__12641;
i__11375_12635 = G__12642;
continue;
} else {
var temp__5823__auto___12643 = cljs.core.seq(seq__11372_12632);
if(temp__5823__auto___12643){
var seq__11372_12644__$1 = temp__5823__auto___12643;
if(cljs.core.chunked_seq_QMARK_(seq__11372_12644__$1)){
var c__5673__auto___12645 = cljs.core.chunk_first(seq__11372_12644__$1);
var G__12646 = cljs.core.chunk_rest(seq__11372_12644__$1);
var G__12647 = c__5673__auto___12645;
var G__12648 = cljs.core.count(c__5673__auto___12645);
var G__12649 = (0);
seq__11372_12632 = G__12646;
chunk__11373_12633 = G__12647;
count__11374_12634 = G__12648;
i__11375_12635 = G__12649;
continue;
} else {
var vec__11401_12650 = cljs.core.first(seq__11372_12644__$1);
var script_tag_12651 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11401_12650,(0),null);
var script_body_12652 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11401_12650,(1),null);
eval(script_body_12652);


var G__12657 = cljs.core.next(seq__11372_12644__$1);
var G__12658 = null;
var G__12659 = (0);
var G__12660 = (0);
seq__11372_12632 = G__12657;
chunk__11373_12633 = G__12658;
count__11374_12634 = G__12659;
i__11375_12635 = G__12660;
continue;
}
} else {
}
}
break;
}

return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (s__$1,p__11405){
var vec__11406 = p__11405;
var script_tag = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11406,(0),null);
var script_body = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11406,(1),null);
return clojure.string.replace(s__$1,script_tag,"");
}),s,scripts);
});
shadow.dom.str__GT_fragment = (function shadow$dom$str__GT_fragment(s){
var el = document.createElement("div");
(el.innerHTML = s);

return (new shadow.dom.NativeColl(goog.dom.childrenToNode_(document,el)));
});
shadow.dom.node_name = (function shadow$dom$node_name(el){
return shadow.dom.dom_node(el).nodeName;
});
shadow.dom.ancestor_by_class = (function shadow$dom$ancestor_by_class(el,cls){
return goog.dom.getAncestorByClass(shadow.dom.dom_node(el),cls);
});
shadow.dom.ancestor_by_tag = (function shadow$dom$ancestor_by_tag(var_args){
var G__11421 = arguments.length;
switch (G__11421) {
case 2:
return shadow.dom.ancestor_by_tag.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return shadow.dom.ancestor_by_tag.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.ancestor_by_tag.cljs$core$IFn$_invoke$arity$2 = (function (el,tag){
return goog.dom.getAncestorByTagNameAndClass(shadow.dom.dom_node(el),cljs.core.name(tag));
}));

(shadow.dom.ancestor_by_tag.cljs$core$IFn$_invoke$arity$3 = (function (el,tag,cls){
return goog.dom.getAncestorByTagNameAndClass(shadow.dom.dom_node(el),cljs.core.name(tag),cljs.core.name(cls));
}));

(shadow.dom.ancestor_by_tag.cljs$lang$maxFixedArity = 3);

shadow.dom.get_value = (function shadow$dom$get_value(dom){
return goog.dom.forms.getValue(shadow.dom.dom_node(dom));
});
shadow.dom.set_value = (function shadow$dom$set_value(dom,value){
return goog.dom.forms.setValue(shadow.dom.dom_node(dom),value);
});
shadow.dom.px = (function shadow$dom$px(value){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((value | 0))+"px");
});
shadow.dom.pct = (function shadow$dom$pct(value){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)+"%");
});
shadow.dom.remove_style_STAR_ = (function shadow$dom$remove_style_STAR_(el,style){
return el.style.removeProperty(cljs.core.name(style));
});
shadow.dom.remove_style = (function shadow$dom$remove_style(el,style){
var el__$1 = shadow.dom.dom_node(el);
return shadow.dom.remove_style_STAR_(el__$1,style);
});
shadow.dom.remove_styles = (function shadow$dom$remove_styles(el,style_keys){
var el__$1 = shadow.dom.dom_node(el);
var seq__11456 = cljs.core.seq(style_keys);
var chunk__11457 = null;
var count__11458 = (0);
var i__11459 = (0);
while(true){
if((i__11459 < count__11458)){
var it = chunk__11457.cljs$core$IIndexed$_nth$arity$2(null,i__11459);
shadow.dom.remove_style_STAR_(el__$1,it);


var G__12666 = seq__11456;
var G__12667 = chunk__11457;
var G__12668 = count__11458;
var G__12669 = (i__11459 + (1));
seq__11456 = G__12666;
chunk__11457 = G__12667;
count__11458 = G__12668;
i__11459 = G__12669;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__11456);
if(temp__5823__auto__){
var seq__11456__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__11456__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__11456__$1);
var G__12670 = cljs.core.chunk_rest(seq__11456__$1);
var G__12671 = c__5673__auto__;
var G__12672 = cljs.core.count(c__5673__auto__);
var G__12673 = (0);
seq__11456 = G__12670;
chunk__11457 = G__12671;
count__11458 = G__12672;
i__11459 = G__12673;
continue;
} else {
var it = cljs.core.first(seq__11456__$1);
shadow.dom.remove_style_STAR_(el__$1,it);


var G__12674 = cljs.core.next(seq__11456__$1);
var G__12675 = null;
var G__12676 = (0);
var G__12677 = (0);
seq__11456 = G__12674;
chunk__11457 = G__12675;
count__11458 = G__12676;
i__11459 = G__12677;
continue;
}
} else {
return null;
}
}
break;
}
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
shadow.dom.Coordinate = (function (x,y,__meta,__extmap,__hash){
this.x = x;
this.y = y;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(shadow.dom.Coordinate.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(shadow.dom.Coordinate.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k11496,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__11521 = k11496;
var G__11521__$1 = (((G__11521 instanceof cljs.core.Keyword))?G__11521.fqn:null);
switch (G__11521__$1) {
case "x":
return self__.x;

break;
case "y":
return self__.y;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k11496,else__5451__auto__);

}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__11524){
var vec__11525 = p__11524;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11525,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11525,(1),null);
return (f__5469__auto__.cljs$core$IFn$_invoke$arity$3 ? f__5469__auto__.cljs$core$IFn$_invoke$arity$3(ret__5471__auto__,k__5472__auto__,v__5473__auto__) : f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__));
}),init__5470__auto__,this__5468__auto____$1);
}));

(shadow.dom.Coordinate.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer(writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer(writer__5464__auto__,pr_pair__5466__auto__,"#shadow.dom.Coordinate{",", ","}",opts__5465__auto__,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"x","x",2099068185),self__.x],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"y","y",-1757859776),self__.y],null))], null),self__.__extmap));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__11495){
var self__ = this;
var G__11495__$1 = this;
return (new cljs.core.RecordIter((0),G__11495__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"x","x",2099068185),new cljs.core.Keyword(null,"y","y",-1757859776)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(shadow.dom.Coordinate.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new shadow.dom.Coordinate(self__.x,self__.y,self__.__meta,self__.__extmap,self__.__hash));
}));

(shadow.dom.Coordinate.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (2 + cljs.core.count(self__.__extmap));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (145542109 ^ cljs.core.hash_unordered_coll(coll__5445__auto__));
})(this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this11497,other11498){
var self__ = this;
var this11497__$1 = this;
return (((!((other11498 == null)))) && ((((this11497__$1.constructor === other11498.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11497__$1.x,other11498.x)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11497__$1.y,other11498.y)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11497__$1.__extmap,other11498.__extmap)))))))));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"y","y",-1757859776),null,new cljs.core.Keyword(null,"x","x",2099068185),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core._with_meta(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new shadow.dom.Coordinate(self__.x,self__.y,self__.__meta,cljs.core.not_empty(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(self__.__extmap,k__5459__auto__)),null));
}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k11496){
var self__ = this;
var this__5455__auto____$1 = this;
var G__11556 = k11496;
var G__11556__$1 = (((G__11556 instanceof cljs.core.Keyword))?G__11556.fqn:null);
switch (G__11556__$1) {
case "x":
case "y":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k11496);

}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__11495){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__11558 = cljs.core.keyword_identical_QMARK_;
var expr__11559 = k__5457__auto__;
if(cljs.core.truth_((pred__11558.cljs$core$IFn$_invoke$arity$2 ? pred__11558.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"x","x",2099068185),expr__11559) : pred__11558.call(null,new cljs.core.Keyword(null,"x","x",2099068185),expr__11559)))){
return (new shadow.dom.Coordinate(G__11495,self__.y,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__11558.cljs$core$IFn$_invoke$arity$2 ? pred__11558.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"y","y",-1757859776),expr__11559) : pred__11558.call(null,new cljs.core.Keyword(null,"y","y",-1757859776),expr__11559)))){
return (new shadow.dom.Coordinate(self__.x,G__11495,self__.__meta,self__.__extmap,null));
} else {
return (new shadow.dom.Coordinate(self__.x,self__.y,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__11495),null));
}
}
}));

(shadow.dom.Coordinate.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"x","x",2099068185),self__.x,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"y","y",-1757859776),self__.y,null))], null),self__.__extmap));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__11495){
var self__ = this;
var this__5447__auto____$1 = this;
return (new shadow.dom.Coordinate(self__.x,self__.y,G__11495,self__.__extmap,self__.__hash));
}));

(shadow.dom.Coordinate.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_(entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth(entry__5454__auto__,(0)),cljs.core._nth(entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(shadow.dom.Coordinate.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"x","x",-555367584,null),new cljs.core.Symbol(null,"y","y",-117328249,null)], null);
}));

(shadow.dom.Coordinate.cljs$lang$type = true);

(shadow.dom.Coordinate.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"shadow.dom/Coordinate",null,(1),null));
}));

(shadow.dom.Coordinate.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write(writer__5495__auto__,"shadow.dom/Coordinate");
}));

/**
 * Positional factory function for shadow.dom/Coordinate.
 */
shadow.dom.__GT_Coordinate = (function shadow$dom$__GT_Coordinate(x,y){
return (new shadow.dom.Coordinate(x,y,null,null,null));
});

/**
 * Factory function for shadow.dom/Coordinate, taking a map of keywords to field values.
 */
shadow.dom.map__GT_Coordinate = (function shadow$dom$map__GT_Coordinate(G__11503){
var extmap__5490__auto__ = (function (){var G__11585 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(G__11503,new cljs.core.Keyword(null,"x","x",2099068185),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"y","y",-1757859776)], 0));
if(cljs.core.record_QMARK_(G__11503)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__11585);
} else {
return G__11585;
}
})();
return (new shadow.dom.Coordinate(new cljs.core.Keyword(null,"x","x",2099068185).cljs$core$IFn$_invoke$arity$1(G__11503),new cljs.core.Keyword(null,"y","y",-1757859776).cljs$core$IFn$_invoke$arity$1(G__11503),null,cljs.core.not_empty(extmap__5490__auto__),null));
});

shadow.dom.get_position = (function shadow$dom$get_position(el){
var pos = goog.style.getPosition(shadow.dom.dom_node(el));
return shadow.dom.__GT_Coordinate(pos.x,pos.y);
});
shadow.dom.get_client_position = (function shadow$dom$get_client_position(el){
var pos = goog.style.getClientPosition(shadow.dom.dom_node(el));
return shadow.dom.__GT_Coordinate(pos.x,pos.y);
});
shadow.dom.get_page_offset = (function shadow$dom$get_page_offset(el){
var pos = goog.style.getPageOffset(shadow.dom.dom_node(el));
return shadow.dom.__GT_Coordinate(pos.x,pos.y);
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
shadow.dom.Size = (function (w,h,__meta,__extmap,__hash){
this.w = w;
this.h = h;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(shadow.dom.Size.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(shadow.dom.Size.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k11713,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__11718 = k11713;
var G__11718__$1 = (((G__11718 instanceof cljs.core.Keyword))?G__11718.fqn:null);
switch (G__11718__$1) {
case "w":
return self__.w;

break;
case "h":
return self__.h;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k11713,else__5451__auto__);

}
}));

(shadow.dom.Size.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__11721){
var vec__11722 = p__11721;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11722,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__11722,(1),null);
return (f__5469__auto__.cljs$core$IFn$_invoke$arity$3 ? f__5469__auto__.cljs$core$IFn$_invoke$arity$3(ret__5471__auto__,k__5472__auto__,v__5473__auto__) : f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__));
}),init__5470__auto__,this__5468__auto____$1);
}));

(shadow.dom.Size.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer(writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer(writer__5464__auto__,pr_pair__5466__auto__,"#shadow.dom.Size{",", ","}",opts__5465__auto__,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"w","w",354169001),self__.w],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"h","h",1109658740),self__.h],null))], null),self__.__extmap));
}));

(shadow.dom.Size.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__11712){
var self__ = this;
var G__11712__$1 = this;
return (new cljs.core.RecordIter((0),G__11712__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"w","w",354169001),new cljs.core.Keyword(null,"h","h",1109658740)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
}));

(shadow.dom.Size.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(shadow.dom.Size.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new shadow.dom.Size(self__.w,self__.h,self__.__meta,self__.__extmap,self__.__hash));
}));

(shadow.dom.Size.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (2 + cljs.core.count(self__.__extmap));
}));

(shadow.dom.Size.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (-1228019642 ^ cljs.core.hash_unordered_coll(coll__5445__auto__));
})(this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(shadow.dom.Size.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this11714,other11715){
var self__ = this;
var this11714__$1 = this;
return (((!((other11715 == null)))) && ((((this11714__$1.constructor === other11715.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11714__$1.w,other11715.w)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11714__$1.h,other11715.h)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this11714__$1.__extmap,other11715.__extmap)))))))));
}));

(shadow.dom.Size.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"w","w",354169001),null,new cljs.core.Keyword(null,"h","h",1109658740),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core._with_meta(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new shadow.dom.Size(self__.w,self__.h,self__.__meta,cljs.core.not_empty(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(self__.__extmap,k__5459__auto__)),null));
}
}));

(shadow.dom.Size.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k11713){
var self__ = this;
var this__5455__auto____$1 = this;
var G__11837 = k11713;
var G__11837__$1 = (((G__11837 instanceof cljs.core.Keyword))?G__11837.fqn:null);
switch (G__11837__$1) {
case "w":
case "h":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k11713);

}
}));

(shadow.dom.Size.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__11712){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__11840 = cljs.core.keyword_identical_QMARK_;
var expr__11841 = k__5457__auto__;
if(cljs.core.truth_((pred__11840.cljs$core$IFn$_invoke$arity$2 ? pred__11840.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"w","w",354169001),expr__11841) : pred__11840.call(null,new cljs.core.Keyword(null,"w","w",354169001),expr__11841)))){
return (new shadow.dom.Size(G__11712,self__.h,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__11840.cljs$core$IFn$_invoke$arity$2 ? pred__11840.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"h","h",1109658740),expr__11841) : pred__11840.call(null,new cljs.core.Keyword(null,"h","h",1109658740),expr__11841)))){
return (new shadow.dom.Size(self__.w,G__11712,self__.__meta,self__.__extmap,null));
} else {
return (new shadow.dom.Size(self__.w,self__.h,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__11712),null));
}
}
}));

(shadow.dom.Size.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"w","w",354169001),self__.w,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"h","h",1109658740),self__.h,null))], null),self__.__extmap));
}));

(shadow.dom.Size.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__11712){
var self__ = this;
var this__5447__auto____$1 = this;
return (new shadow.dom.Size(self__.w,self__.h,G__11712,self__.__extmap,self__.__hash));
}));

(shadow.dom.Size.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_(entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth(entry__5454__auto__,(0)),cljs.core._nth(entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(shadow.dom.Size.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"w","w",1994700528,null),new cljs.core.Symbol(null,"h","h",-1544777029,null)], null);
}));

(shadow.dom.Size.cljs$lang$type = true);

(shadow.dom.Size.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"shadow.dom/Size",null,(1),null));
}));

(shadow.dom.Size.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write(writer__5495__auto__,"shadow.dom/Size");
}));

/**
 * Positional factory function for shadow.dom/Size.
 */
shadow.dom.__GT_Size = (function shadow$dom$__GT_Size(w,h){
return (new shadow.dom.Size(w,h,null,null,null));
});

/**
 * Factory function for shadow.dom/Size, taking a map of keywords to field values.
 */
shadow.dom.map__GT_Size = (function shadow$dom$map__GT_Size(G__11716){
var extmap__5490__auto__ = (function (){var G__11872 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(G__11716,new cljs.core.Keyword(null,"w","w",354169001),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"h","h",1109658740)], 0));
if(cljs.core.record_QMARK_(G__11716)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__11872);
} else {
return G__11872;
}
})();
return (new shadow.dom.Size(new cljs.core.Keyword(null,"w","w",354169001).cljs$core$IFn$_invoke$arity$1(G__11716),new cljs.core.Keyword(null,"h","h",1109658740).cljs$core$IFn$_invoke$arity$1(G__11716),null,cljs.core.not_empty(extmap__5490__auto__),null));
});

shadow.dom.size__GT_clj = (function shadow$dom$size__GT_clj(size){
return (new shadow.dom.Size(size.width,size.height,null,null,null));
});
shadow.dom.get_size = (function shadow$dom$get_size(el){
return shadow.dom.size__GT_clj(goog.style.getSize(shadow.dom.dom_node(el)));
});
shadow.dom.get_height = (function shadow$dom$get_height(el){
return shadow.dom.get_size(el).h;
});
shadow.dom.get_viewport_size = (function shadow$dom$get_viewport_size(){
return shadow.dom.size__GT_clj(goog.dom.getViewportSize());
});
shadow.dom.first_child = (function shadow$dom$first_child(el){
return (shadow.dom.dom_node(el).children[(0)]);
});
shadow.dom.select_option_values = (function shadow$dom$select_option_values(el){
var native$ = shadow.dom.dom_node(el);
var opts = (native$["options"]);
var a__5738__auto__ = opts;
var l__5739__auto__ = a__5738__auto__.length;
var i = (0);
var ret = cljs.core.PersistentVector.EMPTY;
while(true){
if((i < l__5739__auto__)){
var G__12716 = (i + (1));
var G__12717 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(ret,(opts[i]["value"]));
i = G__12716;
ret = G__12717;
continue;
} else {
return ret;
}
break;
}
});
shadow.dom.build_url = (function shadow$dom$build_url(path,query_params){
if(cljs.core.empty_QMARK_(query_params)){
return path;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("&",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__12028){
var vec__12029 = p__12028;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12029,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12029,(1),null);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(k))+"="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v)))));
}),query_params))));
}
});
shadow.dom.redirect = (function shadow$dom$redirect(var_args){
var G__12067 = arguments.length;
switch (G__12067) {
case 1:
return shadow.dom.redirect.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return shadow.dom.redirect.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.dom.redirect.cljs$core$IFn$_invoke$arity$1 = (function (path){
return shadow.dom.redirect.cljs$core$IFn$_invoke$arity$2(path,cljs.core.PersistentArrayMap.EMPTY);
}));

(shadow.dom.redirect.cljs$core$IFn$_invoke$arity$2 = (function (path,query_params){
return (document["location"]["href"] = shadow.dom.build_url(path,query_params));
}));

(shadow.dom.redirect.cljs$lang$maxFixedArity = 2);

shadow.dom.reload_BANG_ = (function shadow$dom$reload_BANG_(){
return (document.location.href = document.location.href);
});
shadow.dom.tag_name = (function shadow$dom$tag_name(el){
var dom = shadow.dom.dom_node(el);
return dom.tagName;
});
shadow.dom.insert_after = (function shadow$dom$insert_after(ref,new$){
var new_node = shadow.dom.dom_node(new$);
goog.dom.insertSiblingAfter(new_node,shadow.dom.dom_node(ref));

return new_node;
});
shadow.dom.insert_before = (function shadow$dom$insert_before(ref,new$){
var new_node = shadow.dom.dom_node(new$);
goog.dom.insertSiblingBefore(new_node,shadow.dom.dom_node(ref));

return new_node;
});
shadow.dom.insert_first = (function shadow$dom$insert_first(ref,new$){
var temp__5821__auto__ = shadow.dom.dom_node(ref).firstChild;
if(cljs.core.truth_(temp__5821__auto__)){
var child = temp__5821__auto__;
return shadow.dom.insert_before(child,new$);
} else {
return shadow.dom.append.cljs$core$IFn$_invoke$arity$2(ref,new$);
}
});
shadow.dom.index_of = (function shadow$dom$index_of(el){
var el__$1 = shadow.dom.dom_node(el);
var i = (0);
while(true){
var ps = el__$1.previousSibling;
if((ps == null)){
return i;
} else {
var G__12720 = ps;
var G__12721 = (i + (1));
el__$1 = G__12720;
i = G__12721;
continue;
}
break;
}
});
shadow.dom.get_parent = (function shadow$dom$get_parent(el){
return goog.dom.getParentElement(shadow.dom.dom_node(el));
});
shadow.dom.parents = (function shadow$dom$parents(el){
var parent = shadow.dom.get_parent(el);
if(cljs.core.truth_(parent)){
return cljs.core.cons(parent,(new cljs.core.LazySeq(null,(function (){
return (shadow.dom.parents.cljs$core$IFn$_invoke$arity$1 ? shadow.dom.parents.cljs$core$IFn$_invoke$arity$1(parent) : shadow.dom.parents.call(null,parent));
}),null,null)));
} else {
return null;
}
});
shadow.dom.matches = (function shadow$dom$matches(el,sel){
return shadow.dom.dom_node(el).matches(sel);
});
shadow.dom.get_next_sibling = (function shadow$dom$get_next_sibling(el){
return goog.dom.getNextElementSibling(shadow.dom.dom_node(el));
});
shadow.dom.get_previous_sibling = (function shadow$dom$get_previous_sibling(el){
return goog.dom.getPreviousElementSibling(shadow.dom.dom_node(el));
});
shadow.dom.xmlns = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 2, ["svg","http://www.w3.org/2000/svg","xlink","http://www.w3.org/1999/xlink"], null));
shadow.dom.create_svg_node = (function shadow$dom$create_svg_node(tag_def,props){
var vec__12170 = shadow.dom.parse_tag(tag_def);
var tag_name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12170,(0),null);
var tag_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12170,(1),null);
var tag_classes = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12170,(2),null);
var el = document.createElementNS("http://www.w3.org/2000/svg",tag_name);
if(cljs.core.truth_(tag_id)){
el.setAttribute("id",tag_id);
} else {
}

if(cljs.core.truth_(tag_classes)){
el.setAttribute("class",shadow.dom.merge_class_string(new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(props),tag_classes));
} else {
}

var seq__12175_12723 = cljs.core.seq(props);
var chunk__12176_12724 = null;
var count__12177_12725 = (0);
var i__12178_12726 = (0);
while(true){
if((i__12178_12726 < count__12177_12725)){
var vec__12202_12727 = chunk__12176_12724.cljs$core$IIndexed$_nth$arity$2(null,i__12178_12726);
var k_12728 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12202_12727,(0),null);
var v_12729 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12202_12727,(1),null);
el.setAttributeNS((function (){var temp__5823__auto__ = cljs.core.namespace(k_12728);
if(cljs.core.truth_(temp__5823__auto__)){
var ns = temp__5823__auto__;
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(shadow.dom.xmlns),ns);
} else {
return null;
}
})(),cljs.core.name(k_12728),v_12729);


var G__12731 = seq__12175_12723;
var G__12732 = chunk__12176_12724;
var G__12733 = count__12177_12725;
var G__12734 = (i__12178_12726 + (1));
seq__12175_12723 = G__12731;
chunk__12176_12724 = G__12732;
count__12177_12725 = G__12733;
i__12178_12726 = G__12734;
continue;
} else {
var temp__5823__auto___12735 = cljs.core.seq(seq__12175_12723);
if(temp__5823__auto___12735){
var seq__12175_12736__$1 = temp__5823__auto___12735;
if(cljs.core.chunked_seq_QMARK_(seq__12175_12736__$1)){
var c__5673__auto___12737 = cljs.core.chunk_first(seq__12175_12736__$1);
var G__12738 = cljs.core.chunk_rest(seq__12175_12736__$1);
var G__12739 = c__5673__auto___12737;
var G__12740 = cljs.core.count(c__5673__auto___12737);
var G__12741 = (0);
seq__12175_12723 = G__12738;
chunk__12176_12724 = G__12739;
count__12177_12725 = G__12740;
i__12178_12726 = G__12741;
continue;
} else {
var vec__12207_12742 = cljs.core.first(seq__12175_12736__$1);
var k_12743 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12207_12742,(0),null);
var v_12744 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12207_12742,(1),null);
el.setAttributeNS((function (){var temp__5823__auto____$1 = cljs.core.namespace(k_12743);
if(cljs.core.truth_(temp__5823__auto____$1)){
var ns = temp__5823__auto____$1;
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(shadow.dom.xmlns),ns);
} else {
return null;
}
})(),cljs.core.name(k_12743),v_12744);


var G__12746 = cljs.core.next(seq__12175_12736__$1);
var G__12747 = null;
var G__12748 = (0);
var G__12749 = (0);
seq__12175_12723 = G__12746;
chunk__12176_12724 = G__12747;
count__12177_12725 = G__12748;
i__12178_12726 = G__12749;
continue;
}
} else {
}
}
break;
}

return el;
});
shadow.dom.svg_node = (function shadow$dom$svg_node(el){
if((el == null)){
return null;
} else {
if((((!((el == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === el.shadow$dom$SVGElement$))))?true:false):false)){
return el.shadow$dom$SVGElement$_to_svg$arity$1(null);
} else {
return el;

}
}
});
shadow.dom.make_svg_node = (function shadow$dom$make_svg_node(structure){
var vec__12230 = shadow.dom.destructure_node(shadow.dom.create_svg_node,structure);
var node = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12230,(0),null);
var node_children = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12230,(1),null);
var seq__12233_12751 = cljs.core.seq(node_children);
var chunk__12235_12752 = null;
var count__12236_12753 = (0);
var i__12237_12754 = (0);
while(true){
if((i__12237_12754 < count__12236_12753)){
var child_struct_12756 = chunk__12235_12752.cljs$core$IIndexed$_nth$arity$2(null,i__12237_12754);
if((!((child_struct_12756 == null)))){
if(typeof child_struct_12756 === 'string'){
var text_12757 = (node["textContent"]);
(node["textContent"] = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text_12757)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_struct_12756)));
} else {
var children_12758 = shadow.dom.svg_node(child_struct_12756);
if(cljs.core.seq_QMARK_(children_12758)){
var seq__12292_12759 = cljs.core.seq(children_12758);
var chunk__12294_12760 = null;
var count__12295_12761 = (0);
var i__12296_12762 = (0);
while(true){
if((i__12296_12762 < count__12295_12761)){
var child_12763 = chunk__12294_12760.cljs$core$IIndexed$_nth$arity$2(null,i__12296_12762);
if(cljs.core.truth_(child_12763)){
node.appendChild(child_12763);


var G__12764 = seq__12292_12759;
var G__12765 = chunk__12294_12760;
var G__12766 = count__12295_12761;
var G__12767 = (i__12296_12762 + (1));
seq__12292_12759 = G__12764;
chunk__12294_12760 = G__12765;
count__12295_12761 = G__12766;
i__12296_12762 = G__12767;
continue;
} else {
var G__12770 = seq__12292_12759;
var G__12771 = chunk__12294_12760;
var G__12772 = count__12295_12761;
var G__12773 = (i__12296_12762 + (1));
seq__12292_12759 = G__12770;
chunk__12294_12760 = G__12771;
count__12295_12761 = G__12772;
i__12296_12762 = G__12773;
continue;
}
} else {
var temp__5823__auto___12774 = cljs.core.seq(seq__12292_12759);
if(temp__5823__auto___12774){
var seq__12292_12775__$1 = temp__5823__auto___12774;
if(cljs.core.chunked_seq_QMARK_(seq__12292_12775__$1)){
var c__5673__auto___12776 = cljs.core.chunk_first(seq__12292_12775__$1);
var G__12777 = cljs.core.chunk_rest(seq__12292_12775__$1);
var G__12778 = c__5673__auto___12776;
var G__12779 = cljs.core.count(c__5673__auto___12776);
var G__12780 = (0);
seq__12292_12759 = G__12777;
chunk__12294_12760 = G__12778;
count__12295_12761 = G__12779;
i__12296_12762 = G__12780;
continue;
} else {
var child_12782 = cljs.core.first(seq__12292_12775__$1);
if(cljs.core.truth_(child_12782)){
node.appendChild(child_12782);


var G__12783 = cljs.core.next(seq__12292_12775__$1);
var G__12784 = null;
var G__12785 = (0);
var G__12786 = (0);
seq__12292_12759 = G__12783;
chunk__12294_12760 = G__12784;
count__12295_12761 = G__12785;
i__12296_12762 = G__12786;
continue;
} else {
var G__12787 = cljs.core.next(seq__12292_12775__$1);
var G__12788 = null;
var G__12789 = (0);
var G__12790 = (0);
seq__12292_12759 = G__12787;
chunk__12294_12760 = G__12788;
count__12295_12761 = G__12789;
i__12296_12762 = G__12790;
continue;
}
}
} else {
}
}
break;
}
} else {
node.appendChild(children_12758);
}
}


var G__12793 = seq__12233_12751;
var G__12794 = chunk__12235_12752;
var G__12795 = count__12236_12753;
var G__12796 = (i__12237_12754 + (1));
seq__12233_12751 = G__12793;
chunk__12235_12752 = G__12794;
count__12236_12753 = G__12795;
i__12237_12754 = G__12796;
continue;
} else {
var G__12798 = seq__12233_12751;
var G__12799 = chunk__12235_12752;
var G__12800 = count__12236_12753;
var G__12801 = (i__12237_12754 + (1));
seq__12233_12751 = G__12798;
chunk__12235_12752 = G__12799;
count__12236_12753 = G__12800;
i__12237_12754 = G__12801;
continue;
}
} else {
var temp__5823__auto___12802 = cljs.core.seq(seq__12233_12751);
if(temp__5823__auto___12802){
var seq__12233_12803__$1 = temp__5823__auto___12802;
if(cljs.core.chunked_seq_QMARK_(seq__12233_12803__$1)){
var c__5673__auto___12804 = cljs.core.chunk_first(seq__12233_12803__$1);
var G__12805 = cljs.core.chunk_rest(seq__12233_12803__$1);
var G__12806 = c__5673__auto___12804;
var G__12807 = cljs.core.count(c__5673__auto___12804);
var G__12808 = (0);
seq__12233_12751 = G__12805;
chunk__12235_12752 = G__12806;
count__12236_12753 = G__12807;
i__12237_12754 = G__12808;
continue;
} else {
var child_struct_12809 = cljs.core.first(seq__12233_12803__$1);
if((!((child_struct_12809 == null)))){
if(typeof child_struct_12809 === 'string'){
var text_12810 = (node["textContent"]);
(node["textContent"] = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text_12810)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_struct_12809)));
} else {
var children_12811 = shadow.dom.svg_node(child_struct_12809);
if(cljs.core.seq_QMARK_(children_12811)){
var seq__12326_12812 = cljs.core.seq(children_12811);
var chunk__12328_12813 = null;
var count__12329_12814 = (0);
var i__12330_12815 = (0);
while(true){
if((i__12330_12815 < count__12329_12814)){
var child_12816 = chunk__12328_12813.cljs$core$IIndexed$_nth$arity$2(null,i__12330_12815);
if(cljs.core.truth_(child_12816)){
node.appendChild(child_12816);


var G__12817 = seq__12326_12812;
var G__12818 = chunk__12328_12813;
var G__12819 = count__12329_12814;
var G__12820 = (i__12330_12815 + (1));
seq__12326_12812 = G__12817;
chunk__12328_12813 = G__12818;
count__12329_12814 = G__12819;
i__12330_12815 = G__12820;
continue;
} else {
var G__12821 = seq__12326_12812;
var G__12822 = chunk__12328_12813;
var G__12823 = count__12329_12814;
var G__12824 = (i__12330_12815 + (1));
seq__12326_12812 = G__12821;
chunk__12328_12813 = G__12822;
count__12329_12814 = G__12823;
i__12330_12815 = G__12824;
continue;
}
} else {
var temp__5823__auto___12825__$1 = cljs.core.seq(seq__12326_12812);
if(temp__5823__auto___12825__$1){
var seq__12326_12826__$1 = temp__5823__auto___12825__$1;
if(cljs.core.chunked_seq_QMARK_(seq__12326_12826__$1)){
var c__5673__auto___12827 = cljs.core.chunk_first(seq__12326_12826__$1);
var G__12829 = cljs.core.chunk_rest(seq__12326_12826__$1);
var G__12830 = c__5673__auto___12827;
var G__12831 = cljs.core.count(c__5673__auto___12827);
var G__12832 = (0);
seq__12326_12812 = G__12829;
chunk__12328_12813 = G__12830;
count__12329_12814 = G__12831;
i__12330_12815 = G__12832;
continue;
} else {
var child_12833 = cljs.core.first(seq__12326_12826__$1);
if(cljs.core.truth_(child_12833)){
node.appendChild(child_12833);


var G__12834 = cljs.core.next(seq__12326_12826__$1);
var G__12835 = null;
var G__12836 = (0);
var G__12837 = (0);
seq__12326_12812 = G__12834;
chunk__12328_12813 = G__12835;
count__12329_12814 = G__12836;
i__12330_12815 = G__12837;
continue;
} else {
var G__12838 = cljs.core.next(seq__12326_12826__$1);
var G__12839 = null;
var G__12840 = (0);
var G__12841 = (0);
seq__12326_12812 = G__12838;
chunk__12328_12813 = G__12839;
count__12329_12814 = G__12840;
i__12330_12815 = G__12841;
continue;
}
}
} else {
}
}
break;
}
} else {
node.appendChild(children_12811);
}
}


var G__12842 = cljs.core.next(seq__12233_12803__$1);
var G__12843 = null;
var G__12844 = (0);
var G__12845 = (0);
seq__12233_12751 = G__12842;
chunk__12235_12752 = G__12843;
count__12236_12753 = G__12844;
i__12237_12754 = G__12845;
continue;
} else {
var G__12847 = cljs.core.next(seq__12233_12803__$1);
var G__12848 = null;
var G__12849 = (0);
var G__12850 = (0);
seq__12233_12751 = G__12847;
chunk__12235_12752 = G__12848;
count__12236_12753 = G__12849;
i__12237_12754 = G__12850;
continue;
}
}
} else {
}
}
break;
}

return node;
});
(shadow.dom.SVGElement["string"] = true);

(shadow.dom._to_svg["string"] = (function (this$){
if((this$ instanceof cljs.core.Keyword)){
return shadow.dom.make_svg_node(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [this$], null));
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("strings cannot be in svgs",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"this","this",-611633625),this$], null));
}
}));

(cljs.core.PersistentVector.prototype.shadow$dom$SVGElement$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.PersistentVector.prototype.shadow$dom$SVGElement$_to_svg$arity$1 = (function (this$){
var this$__$1 = this;
return shadow.dom.make_svg_node(this$__$1);
}));

(cljs.core.LazySeq.prototype.shadow$dom$SVGElement$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.LazySeq.prototype.shadow$dom$SVGElement$_to_svg$arity$1 = (function (this$){
var this$__$1 = this;
return cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom._to_svg,this$__$1);
}));

(shadow.dom.SVGElement["null"] = true);

(shadow.dom._to_svg["null"] = (function (_){
return null;
}));
shadow.dom.svg = (function shadow$dom$svg(var_args){
var args__5882__auto__ = [];
var len__5876__auto___12853 = arguments.length;
var i__5877__auto___12854 = (0);
while(true){
if((i__5877__auto___12854 < len__5876__auto___12853)){
args__5882__auto__.push((arguments[i__5877__auto___12854]));

var G__12856 = (i__5877__auto___12854 + (1));
i__5877__auto___12854 = G__12856;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return shadow.dom.svg.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(shadow.dom.svg.cljs$core$IFn$_invoke$arity$variadic = (function (attrs,children){
return shadow.dom._to_svg(cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"svg","svg",856789142),attrs], null),children)));
}));

(shadow.dom.svg.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(shadow.dom.svg.cljs$lang$applyTo = (function (seq12377){
var G__12378 = cljs.core.first(seq12377);
var seq12377__$1 = cljs.core.next(seq12377);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__12378,seq12377__$1);
}));


//# sourceMappingURL=shadow.dom.js.map
