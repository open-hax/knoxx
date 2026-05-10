goog.provide('shadow.dom');
shadow.dom.transition_supported_QMARK_ = true;

/**
 * @interface
 */
shadow.dom.IElement = function(){};

var shadow$dom$IElement$_to_dom$dyn_26760 = (function (this$){
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
return shadow$dom$IElement$_to_dom$dyn_26760(this$);
}
});


/**
 * @interface
 */
shadow.dom.SVGElement = function(){};

var shadow$dom$SVGElement$_to_svg$dyn_26765 = (function (this$){
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
return shadow$dom$SVGElement$_to_svg$dyn_26765(this$);
}
});

shadow.dom.lazy_native_coll_seq = (function shadow$dom$lazy_native_coll_seq(coll,idx){
if((idx < coll.length)){
return (new cljs.core.LazySeq(null,(function (){
return cljs.core.cons((coll[idx]),(function (){var G__25377 = coll;
var G__25378 = (idx + (1));
return (shadow.dom.lazy_native_coll_seq.cljs$core$IFn$_invoke$arity$2 ? shadow.dom.lazy_native_coll_seq.cljs$core$IFn$_invoke$arity$2(G__25377,G__25378) : shadow.dom.lazy_native_coll_seq.call(null,G__25377,G__25378));
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
var G__25408 = arguments.length;
switch (G__25408) {
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
var G__25417 = arguments.length;
switch (G__25417) {
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
var G__25436 = arguments.length;
switch (G__25436) {
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
var G__25441 = arguments.length;
switch (G__25441) {
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
var G__25452 = arguments.length;
switch (G__25452) {
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
var G__25549 = arguments.length;
switch (G__25549) {
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
}catch (e25558){if((e25558 instanceof Object)){
var e = e25558;
return console.log("didnt support attachEvent",el,e);
} else {
throw e25558;

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
var seq__25562 = cljs.core.seq(shadow.dom.query.cljs$core$IFn$_invoke$arity$2(selector,root_el));
var chunk__25563 = null;
var count__25564 = (0);
var i__25565 = (0);
while(true){
if((i__25565 < count__25564)){
var el = chunk__25563.cljs$core$IIndexed$_nth$arity$2(null,i__25565);
var handler_26814__$1 = ((function (seq__25562,chunk__25563,count__25564,i__25565,el){
return (function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
});})(seq__25562,chunk__25563,count__25564,i__25565,el))
;
shadow.dom.dom_listen(el,cljs.core.name(ev),handler_26814__$1);


var G__26823 = seq__25562;
var G__26824 = chunk__25563;
var G__26825 = count__25564;
var G__26826 = (i__25565 + (1));
seq__25562 = G__26823;
chunk__25563 = G__26824;
count__25564 = G__26825;
i__25565 = G__26826;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__25562);
if(temp__5823__auto__){
var seq__25562__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__25562__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__25562__$1);
var G__26831 = cljs.core.chunk_rest(seq__25562__$1);
var G__26832 = c__5673__auto__;
var G__26833 = cljs.core.count(c__5673__auto__);
var G__26834 = (0);
seq__25562 = G__26831;
chunk__25563 = G__26832;
count__25564 = G__26833;
i__25565 = G__26834;
continue;
} else {
var el = cljs.core.first(seq__25562__$1);
var handler_26839__$1 = ((function (seq__25562,chunk__25563,count__25564,i__25565,el,seq__25562__$1,temp__5823__auto__){
return (function (e){
return (handler.cljs$core$IFn$_invoke$arity$2 ? handler.cljs$core$IFn$_invoke$arity$2(e,el) : handler.call(null,e,el));
});})(seq__25562,chunk__25563,count__25564,i__25565,el,seq__25562__$1,temp__5823__auto__))
;
shadow.dom.dom_listen(el,cljs.core.name(ev),handler_26839__$1);


var G__26840 = cljs.core.next(seq__25562__$1);
var G__26841 = null;
var G__26842 = (0);
var G__26843 = (0);
seq__25562 = G__26840;
chunk__25563 = G__26841;
count__25564 = G__26842;
i__25565 = G__26843;
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
var G__25575 = arguments.length;
switch (G__25575) {
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
var seq__25604 = cljs.core.seq(events);
var chunk__25605 = null;
var count__25606 = (0);
var i__25607 = (0);
while(true){
if((i__25607 < count__25606)){
var vec__25615 = chunk__25605.cljs$core$IIndexed$_nth$arity$2(null,i__25607);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25615,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25615,(1),null);
shadow.dom.on.cljs$core$IFn$_invoke$arity$3(el,k,v);


var G__26846 = seq__25604;
var G__26847 = chunk__25605;
var G__26848 = count__25606;
var G__26849 = (i__25607 + (1));
seq__25604 = G__26846;
chunk__25605 = G__26847;
count__25606 = G__26848;
i__25607 = G__26849;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__25604);
if(temp__5823__auto__){
var seq__25604__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__25604__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__25604__$1);
var G__26850 = cljs.core.chunk_rest(seq__25604__$1);
var G__26851 = c__5673__auto__;
var G__26852 = cljs.core.count(c__5673__auto__);
var G__26853 = (0);
seq__25604 = G__26850;
chunk__25605 = G__26851;
count__25606 = G__26852;
i__25607 = G__26853;
continue;
} else {
var vec__25618 = cljs.core.first(seq__25604__$1);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25618,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25618,(1),null);
shadow.dom.on.cljs$core$IFn$_invoke$arity$3(el,k,v);


var G__26854 = cljs.core.next(seq__25604__$1);
var G__26855 = null;
var G__26856 = (0);
var G__26857 = (0);
seq__25604 = G__26854;
chunk__25605 = G__26855;
count__25606 = G__26856;
i__25607 = G__26857;
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
var seq__25623 = cljs.core.seq(styles);
var chunk__25624 = null;
var count__25625 = (0);
var i__25626 = (0);
while(true){
if((i__25626 < count__25625)){
var vec__25643 = chunk__25624.cljs$core$IIndexed$_nth$arity$2(null,i__25626);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25643,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25643,(1),null);
goog.style.setStyle(dom,cljs.core.name(k),(((v == null))?"":v));


var G__26858 = seq__25623;
var G__26859 = chunk__25624;
var G__26860 = count__25625;
var G__26861 = (i__25626 + (1));
seq__25623 = G__26858;
chunk__25624 = G__26859;
count__25625 = G__26860;
i__25626 = G__26861;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__25623);
if(temp__5823__auto__){
var seq__25623__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__25623__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__25623__$1);
var G__26862 = cljs.core.chunk_rest(seq__25623__$1);
var G__26863 = c__5673__auto__;
var G__26864 = cljs.core.count(c__5673__auto__);
var G__26865 = (0);
seq__25623 = G__26862;
chunk__25624 = G__26863;
count__25625 = G__26864;
i__25626 = G__26865;
continue;
} else {
var vec__25654 = cljs.core.first(seq__25623__$1);
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25654,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25654,(1),null);
goog.style.setStyle(dom,cljs.core.name(k),(((v == null))?"":v));


var G__26867 = cljs.core.next(seq__25623__$1);
var G__26868 = null;
var G__26869 = (0);
var G__26870 = (0);
seq__25623 = G__26867;
chunk__25624 = G__26868;
count__25625 = G__26869;
i__25626 = G__26870;
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
var G__25659_26871 = key;
var G__25659_26872__$1 = (((G__25659_26871 instanceof cljs.core.Keyword))?G__25659_26871.fqn:null);
switch (G__25659_26872__$1) {
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
var ks_26880 = cljs.core.name(key);
if(cljs.core.truth_((function (){var or__5142__auto__ = goog.string.startsWith(ks_26880,"data-");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return goog.string.startsWith(ks_26880,"aria-");
}
})())){
el.setAttribute(ks_26880,value);
} else {
(el[ks_26880] = value);
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
shadow.dom.create_dom_node = (function shadow$dom$create_dom_node(tag_def,p__25685){
var map__25686 = p__25685;
var map__25686__$1 = cljs.core.__destructure_map(map__25686);
var props = map__25686__$1;
var class$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25686__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var tag_props = ({});
var vec__25690 = shadow.dom.parse_tag(tag_def);
var tag_name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25690,(0),null);
var tag_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25690,(1),null);
var tag_classes = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25690,(2),null);
if(cljs.core.truth_(tag_id)){
(tag_props["id"] = tag_id);
} else {
}

if(cljs.core.truth_(tag_classes)){
(tag_props["class"] = shadow.dom.merge_class_string(class$,tag_classes));
} else {
}

var G__25693 = goog.dom.createDom(tag_name,tag_props);
shadow.dom.set_attrs(G__25693,cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(props,new cljs.core.Keyword(null,"class","class",-2030961996)));

return G__25693;
});
shadow.dom.append = (function shadow$dom$append(var_args){
var G__25738 = arguments.length;
switch (G__25738) {
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

shadow.dom.destructure_node = (function shadow$dom$destructure_node(create_fn,p__25783){
var vec__25784 = p__25783;
var seq__25785 = cljs.core.seq(vec__25784);
var first__25786 = cljs.core.first(seq__25785);
var seq__25785__$1 = cljs.core.next(seq__25785);
var nn = first__25786;
var first__25786__$1 = cljs.core.first(seq__25785__$1);
var seq__25785__$2 = cljs.core.next(seq__25785__$1);
var np = first__25786__$1;
var nc = seq__25785__$2;
var node = vec__25784;
if((nn instanceof cljs.core.Keyword)){
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("invalid dom node",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"node","node",581201198),node], null));
}

if((((np == null)) && ((nc == null)))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var G__25795 = nn;
var G__25796 = cljs.core.PersistentArrayMap.EMPTY;
return (create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(G__25795,G__25796) : create_fn.call(null,G__25795,G__25796));
})(),cljs.core.List.EMPTY], null);
} else {
if(cljs.core.map_QMARK_(np)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(nn,np) : create_fn.call(null,nn,np)),nc], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var G__25798 = nn;
var G__25799 = cljs.core.PersistentArrayMap.EMPTY;
return (create_fn.cljs$core$IFn$_invoke$arity$2 ? create_fn.cljs$core$IFn$_invoke$arity$2(G__25798,G__25799) : create_fn.call(null,G__25798,G__25799));
})(),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(nc,np)], null);

}
}
});
shadow.dom.make_dom_node = (function shadow$dom$make_dom_node(structure){
var vec__25805 = shadow.dom.destructure_node(shadow.dom.create_dom_node,structure);
var node = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25805,(0),null);
var node_children = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25805,(1),null);
var seq__25814_26925 = cljs.core.seq(node_children);
var chunk__25815_26926 = null;
var count__25816_26927 = (0);
var i__25817_26928 = (0);
while(true){
if((i__25817_26928 < count__25816_26927)){
var child_struct_26929 = chunk__25815_26926.cljs$core$IIndexed$_nth$arity$2(null,i__25817_26928);
var children_26932 = shadow.dom.dom_node(child_struct_26929);
if(cljs.core.seq_QMARK_(children_26932)){
var seq__25961_26934 = cljs.core.seq(cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom.dom_node,children_26932));
var chunk__25963_26935 = null;
var count__25964_26936 = (0);
var i__25965_26937 = (0);
while(true){
if((i__25965_26937 < count__25964_26936)){
var child_26938 = chunk__25963_26935.cljs$core$IIndexed$_nth$arity$2(null,i__25965_26937);
if(cljs.core.truth_(child_26938)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_26938);


var G__26940 = seq__25961_26934;
var G__26941 = chunk__25963_26935;
var G__26942 = count__25964_26936;
var G__26943 = (i__25965_26937 + (1));
seq__25961_26934 = G__26940;
chunk__25963_26935 = G__26941;
count__25964_26936 = G__26942;
i__25965_26937 = G__26943;
continue;
} else {
var G__26944 = seq__25961_26934;
var G__26945 = chunk__25963_26935;
var G__26946 = count__25964_26936;
var G__26947 = (i__25965_26937 + (1));
seq__25961_26934 = G__26944;
chunk__25963_26935 = G__26945;
count__25964_26936 = G__26946;
i__25965_26937 = G__26947;
continue;
}
} else {
var temp__5823__auto___26948 = cljs.core.seq(seq__25961_26934);
if(temp__5823__auto___26948){
var seq__25961_26952__$1 = temp__5823__auto___26948;
if(cljs.core.chunked_seq_QMARK_(seq__25961_26952__$1)){
var c__5673__auto___26953 = cljs.core.chunk_first(seq__25961_26952__$1);
var G__26955 = cljs.core.chunk_rest(seq__25961_26952__$1);
var G__26956 = c__5673__auto___26953;
var G__26957 = cljs.core.count(c__5673__auto___26953);
var G__26958 = (0);
seq__25961_26934 = G__26955;
chunk__25963_26935 = G__26956;
count__25964_26936 = G__26957;
i__25965_26937 = G__26958;
continue;
} else {
var child_26961 = cljs.core.first(seq__25961_26952__$1);
if(cljs.core.truth_(child_26961)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_26961);


var G__26963 = cljs.core.next(seq__25961_26952__$1);
var G__26964 = null;
var G__26965 = (0);
var G__26966 = (0);
seq__25961_26934 = G__26963;
chunk__25963_26935 = G__26964;
count__25964_26936 = G__26965;
i__25965_26937 = G__26966;
continue;
} else {
var G__26967 = cljs.core.next(seq__25961_26952__$1);
var G__26968 = null;
var G__26969 = (0);
var G__26970 = (0);
seq__25961_26934 = G__26967;
chunk__25963_26935 = G__26968;
count__25964_26936 = G__26969;
i__25965_26937 = G__26970;
continue;
}
}
} else {
}
}
break;
}
} else {
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,children_26932);
}


var G__26971 = seq__25814_26925;
var G__26972 = chunk__25815_26926;
var G__26973 = count__25816_26927;
var G__26974 = (i__25817_26928 + (1));
seq__25814_26925 = G__26971;
chunk__25815_26926 = G__26972;
count__25816_26927 = G__26973;
i__25817_26928 = G__26974;
continue;
} else {
var temp__5823__auto___26975 = cljs.core.seq(seq__25814_26925);
if(temp__5823__auto___26975){
var seq__25814_26976__$1 = temp__5823__auto___26975;
if(cljs.core.chunked_seq_QMARK_(seq__25814_26976__$1)){
var c__5673__auto___26979 = cljs.core.chunk_first(seq__25814_26976__$1);
var G__26981 = cljs.core.chunk_rest(seq__25814_26976__$1);
var G__26982 = c__5673__auto___26979;
var G__26983 = cljs.core.count(c__5673__auto___26979);
var G__26984 = (0);
seq__25814_26925 = G__26981;
chunk__25815_26926 = G__26982;
count__25816_26927 = G__26983;
i__25817_26928 = G__26984;
continue;
} else {
var child_struct_26985 = cljs.core.first(seq__25814_26976__$1);
var children_26986 = shadow.dom.dom_node(child_struct_26985);
if(cljs.core.seq_QMARK_(children_26986)){
var seq__25980_26987 = cljs.core.seq(cljs.core.map.cljs$core$IFn$_invoke$arity$2(shadow.dom.dom_node,children_26986));
var chunk__25982_26988 = null;
var count__25983_26989 = (0);
var i__25984_26990 = (0);
while(true){
if((i__25984_26990 < count__25983_26989)){
var child_26993 = chunk__25982_26988.cljs$core$IIndexed$_nth$arity$2(null,i__25984_26990);
if(cljs.core.truth_(child_26993)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_26993);


var G__26994 = seq__25980_26987;
var G__26995 = chunk__25982_26988;
var G__26996 = count__25983_26989;
var G__26997 = (i__25984_26990 + (1));
seq__25980_26987 = G__26994;
chunk__25982_26988 = G__26995;
count__25983_26989 = G__26996;
i__25984_26990 = G__26997;
continue;
} else {
var G__26999 = seq__25980_26987;
var G__27000 = chunk__25982_26988;
var G__27001 = count__25983_26989;
var G__27002 = (i__25984_26990 + (1));
seq__25980_26987 = G__26999;
chunk__25982_26988 = G__27000;
count__25983_26989 = G__27001;
i__25984_26990 = G__27002;
continue;
}
} else {
var temp__5823__auto___27003__$1 = cljs.core.seq(seq__25980_26987);
if(temp__5823__auto___27003__$1){
var seq__25980_27004__$1 = temp__5823__auto___27003__$1;
if(cljs.core.chunked_seq_QMARK_(seq__25980_27004__$1)){
var c__5673__auto___27005 = cljs.core.chunk_first(seq__25980_27004__$1);
var G__27007 = cljs.core.chunk_rest(seq__25980_27004__$1);
var G__27008 = c__5673__auto___27005;
var G__27009 = cljs.core.count(c__5673__auto___27005);
var G__27010 = (0);
seq__25980_26987 = G__27007;
chunk__25982_26988 = G__27008;
count__25983_26989 = G__27009;
i__25984_26990 = G__27010;
continue;
} else {
var child_27011 = cljs.core.first(seq__25980_27004__$1);
if(cljs.core.truth_(child_27011)){
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,child_27011);


var G__27012 = cljs.core.next(seq__25980_27004__$1);
var G__27013 = null;
var G__27014 = (0);
var G__27015 = (0);
seq__25980_26987 = G__27012;
chunk__25982_26988 = G__27013;
count__25983_26989 = G__27014;
i__25984_26990 = G__27015;
continue;
} else {
var G__27018 = cljs.core.next(seq__25980_27004__$1);
var G__27019 = null;
var G__27020 = (0);
var G__27021 = (0);
seq__25980_26987 = G__27018;
chunk__25982_26988 = G__27019;
count__25983_26989 = G__27020;
i__25984_26990 = G__27021;
continue;
}
}
} else {
}
}
break;
}
} else {
shadow.dom.append.cljs$core$IFn$_invoke$arity$2(node,children_26986);
}


var G__27023 = cljs.core.next(seq__25814_26976__$1);
var G__27024 = null;
var G__27025 = (0);
var G__27026 = (0);
seq__25814_26925 = G__27023;
chunk__25815_26926 = G__27024;
count__25816_26927 = G__27025;
i__25817_26928 = G__27026;
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
var seq__26029 = cljs.core.seq(node);
var chunk__26034 = null;
var count__26035 = (0);
var i__26036 = (0);
while(true){
if((i__26036 < count__26035)){
var n = chunk__26034.cljs$core$IIndexed$_nth$arity$2(null,i__26036);
(shadow.dom.remove.cljs$core$IFn$_invoke$arity$1 ? shadow.dom.remove.cljs$core$IFn$_invoke$arity$1(n) : shadow.dom.remove.call(null,n));


var G__27040 = seq__26029;
var G__27041 = chunk__26034;
var G__27042 = count__26035;
var G__27043 = (i__26036 + (1));
seq__26029 = G__27040;
chunk__26034 = G__27041;
count__26035 = G__27042;
i__26036 = G__27043;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__26029);
if(temp__5823__auto__){
var seq__26029__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__26029__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__26029__$1);
var G__27044 = cljs.core.chunk_rest(seq__26029__$1);
var G__27045 = c__5673__auto__;
var G__27046 = cljs.core.count(c__5673__auto__);
var G__27047 = (0);
seq__26029 = G__27044;
chunk__26034 = G__27045;
count__26035 = G__27046;
i__26036 = G__27047;
continue;
} else {
var n = cljs.core.first(seq__26029__$1);
(shadow.dom.remove.cljs$core$IFn$_invoke$arity$1 ? shadow.dom.remove.cljs$core$IFn$_invoke$arity$1(n) : shadow.dom.remove.call(null,n));


var G__27048 = cljs.core.next(seq__26029__$1);
var G__27049 = null;
var G__27050 = (0);
var G__27051 = (0);
seq__26029 = G__27048;
chunk__26034 = G__27049;
count__26035 = G__27050;
i__26036 = G__27051;
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
var G__26058 = arguments.length;
switch (G__26058) {
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
var G__26115 = arguments.length;
switch (G__26115) {
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
var G__26142 = arguments.length;
switch (G__26142) {
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
var len__5876__auto___27077 = arguments.length;
var i__5877__auto___27078 = (0);
while(true){
if((i__5877__auto___27078 < len__5876__auto___27077)){
args__5882__auto__.push((arguments[i__5877__auto___27078]));

var G__27081 = (i__5877__auto___27078 + (1));
i__5877__auto___27078 = G__27081;
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
var seq__26169_27084 = cljs.core.seq(nodes);
var chunk__26170_27085 = null;
var count__26171_27086 = (0);
var i__26172_27087 = (0);
while(true){
if((i__26172_27087 < count__26171_27086)){
var node_27089 = chunk__26170_27085.cljs$core$IIndexed$_nth$arity$2(null,i__26172_27087);
fragment.appendChild(shadow.dom._to_dom(node_27089));


var G__27090 = seq__26169_27084;
var G__27091 = chunk__26170_27085;
var G__27092 = count__26171_27086;
var G__27093 = (i__26172_27087 + (1));
seq__26169_27084 = G__27090;
chunk__26170_27085 = G__27091;
count__26171_27086 = G__27092;
i__26172_27087 = G__27093;
continue;
} else {
var temp__5823__auto___27095 = cljs.core.seq(seq__26169_27084);
if(temp__5823__auto___27095){
var seq__26169_27096__$1 = temp__5823__auto___27095;
if(cljs.core.chunked_seq_QMARK_(seq__26169_27096__$1)){
var c__5673__auto___27097 = cljs.core.chunk_first(seq__26169_27096__$1);
var G__27098 = cljs.core.chunk_rest(seq__26169_27096__$1);
var G__27099 = c__5673__auto___27097;
var G__27100 = cljs.core.count(c__5673__auto___27097);
var G__27101 = (0);
seq__26169_27084 = G__27098;
chunk__26170_27085 = G__27099;
count__26171_27086 = G__27100;
i__26172_27087 = G__27101;
continue;
} else {
var node_27102 = cljs.core.first(seq__26169_27096__$1);
fragment.appendChild(shadow.dom._to_dom(node_27102));


var G__27103 = cljs.core.next(seq__26169_27096__$1);
var G__27104 = null;
var G__27105 = (0);
var G__27106 = (0);
seq__26169_27084 = G__27103;
chunk__26170_27085 = G__27104;
count__26171_27086 = G__27105;
i__26172_27087 = G__27106;
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
(shadow.dom.fragment.cljs$lang$applyTo = (function (seq26165){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq26165));
}));

/**
 * given a html string, eval all <script> tags and return the html without the scripts
 * don't do this for everything, only content you trust.
 */
shadow.dom.eval_scripts = (function shadow$dom$eval_scripts(s){
var scripts = cljs.core.re_seq(/<script[^>]*?>(.+?)<\/script>/,s);
var seq__26179_27112 = cljs.core.seq(scripts);
var chunk__26180_27113 = null;
var count__26181_27114 = (0);
var i__26182_27115 = (0);
while(true){
if((i__26182_27115 < count__26181_27114)){
var vec__26197_27116 = chunk__26180_27113.cljs$core$IIndexed$_nth$arity$2(null,i__26182_27115);
var script_tag_27117 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26197_27116,(0),null);
var script_body_27118 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26197_27116,(1),null);
eval(script_body_27118);


var G__27120 = seq__26179_27112;
var G__27121 = chunk__26180_27113;
var G__27122 = count__26181_27114;
var G__27123 = (i__26182_27115 + (1));
seq__26179_27112 = G__27120;
chunk__26180_27113 = G__27121;
count__26181_27114 = G__27122;
i__26182_27115 = G__27123;
continue;
} else {
var temp__5823__auto___27124 = cljs.core.seq(seq__26179_27112);
if(temp__5823__auto___27124){
var seq__26179_27125__$1 = temp__5823__auto___27124;
if(cljs.core.chunked_seq_QMARK_(seq__26179_27125__$1)){
var c__5673__auto___27126 = cljs.core.chunk_first(seq__26179_27125__$1);
var G__27127 = cljs.core.chunk_rest(seq__26179_27125__$1);
var G__27128 = c__5673__auto___27126;
var G__27129 = cljs.core.count(c__5673__auto___27126);
var G__27130 = (0);
seq__26179_27112 = G__27127;
chunk__26180_27113 = G__27128;
count__26181_27114 = G__27129;
i__26182_27115 = G__27130;
continue;
} else {
var vec__26208_27133 = cljs.core.first(seq__26179_27125__$1);
var script_tag_27134 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26208_27133,(0),null);
var script_body_27135 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26208_27133,(1),null);
eval(script_body_27135);


var G__27137 = cljs.core.next(seq__26179_27125__$1);
var G__27138 = null;
var G__27139 = (0);
var G__27140 = (0);
seq__26179_27112 = G__27137;
chunk__26180_27113 = G__27138;
count__26181_27114 = G__27139;
i__26182_27115 = G__27140;
continue;
}
} else {
}
}
break;
}

return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (s__$1,p__26237){
var vec__26238 = p__26237;
var script_tag = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26238,(0),null);
var script_body = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26238,(1),null);
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
var G__26268 = arguments.length;
switch (G__26268) {
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
var seq__26306 = cljs.core.seq(style_keys);
var chunk__26307 = null;
var count__26308 = (0);
var i__26309 = (0);
while(true){
if((i__26309 < count__26308)){
var it = chunk__26307.cljs$core$IIndexed$_nth$arity$2(null,i__26309);
shadow.dom.remove_style_STAR_(el__$1,it);


var G__27167 = seq__26306;
var G__27168 = chunk__26307;
var G__27169 = count__26308;
var G__27170 = (i__26309 + (1));
seq__26306 = G__27167;
chunk__26307 = G__27168;
count__26308 = G__27169;
i__26309 = G__27170;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__26306);
if(temp__5823__auto__){
var seq__26306__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__26306__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__26306__$1);
var G__27171 = cljs.core.chunk_rest(seq__26306__$1);
var G__27172 = c__5673__auto__;
var G__27173 = cljs.core.count(c__5673__auto__);
var G__27174 = (0);
seq__26306 = G__27171;
chunk__26307 = G__27172;
count__26308 = G__27173;
i__26309 = G__27174;
continue;
} else {
var it = cljs.core.first(seq__26306__$1);
shadow.dom.remove_style_STAR_(el__$1,it);


var G__27175 = cljs.core.next(seq__26306__$1);
var G__27176 = null;
var G__27177 = (0);
var G__27178 = (0);
seq__26306 = G__27175;
chunk__26307 = G__27176;
count__26308 = G__27177;
i__26309 = G__27178;
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

(shadow.dom.Coordinate.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k26312,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__26318 = k26312;
var G__26318__$1 = (((G__26318 instanceof cljs.core.Keyword))?G__26318.fqn:null);
switch (G__26318__$1) {
case "x":
return self__.x;

break;
case "y":
return self__.y;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k26312,else__5451__auto__);

}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__26319){
var vec__26320 = p__26319;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26320,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26320,(1),null);
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

(shadow.dom.Coordinate.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__26311){
var self__ = this;
var G__26311__$1 = this;
return (new cljs.core.RecordIter((0),G__26311__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"x","x",2099068185),new cljs.core.Keyword(null,"y","y",-1757859776)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
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

(shadow.dom.Coordinate.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this26313,other26314){
var self__ = this;
var this26313__$1 = this;
return (((!((other26314 == null)))) && ((((this26313__$1.constructor === other26314.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26313__$1.x,other26314.x)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26313__$1.y,other26314.y)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26313__$1.__extmap,other26314.__extmap)))))))));
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

(shadow.dom.Coordinate.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k26312){
var self__ = this;
var this__5455__auto____$1 = this;
var G__26330 = k26312;
var G__26330__$1 = (((G__26330 instanceof cljs.core.Keyword))?G__26330.fqn:null);
switch (G__26330__$1) {
case "x":
case "y":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k26312);

}
}));

(shadow.dom.Coordinate.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__26311){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__26331 = cljs.core.keyword_identical_QMARK_;
var expr__26332 = k__5457__auto__;
if(cljs.core.truth_((pred__26331.cljs$core$IFn$_invoke$arity$2 ? pred__26331.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"x","x",2099068185),expr__26332) : pred__26331.call(null,new cljs.core.Keyword(null,"x","x",2099068185),expr__26332)))){
return (new shadow.dom.Coordinate(G__26311,self__.y,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__26331.cljs$core$IFn$_invoke$arity$2 ? pred__26331.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"y","y",-1757859776),expr__26332) : pred__26331.call(null,new cljs.core.Keyword(null,"y","y",-1757859776),expr__26332)))){
return (new shadow.dom.Coordinate(self__.x,G__26311,self__.__meta,self__.__extmap,null));
} else {
return (new shadow.dom.Coordinate(self__.x,self__.y,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__26311),null));
}
}
}));

(shadow.dom.Coordinate.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"x","x",2099068185),self__.x,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"y","y",-1757859776),self__.y,null))], null),self__.__extmap));
}));

(shadow.dom.Coordinate.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__26311){
var self__ = this;
var this__5447__auto____$1 = this;
return (new shadow.dom.Coordinate(self__.x,self__.y,G__26311,self__.__extmap,self__.__hash));
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
shadow.dom.map__GT_Coordinate = (function shadow$dom$map__GT_Coordinate(G__26316){
var extmap__5490__auto__ = (function (){var G__26343 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(G__26316,new cljs.core.Keyword(null,"x","x",2099068185),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"y","y",-1757859776)], 0));
if(cljs.core.record_QMARK_(G__26316)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__26343);
} else {
return G__26343;
}
})();
return (new shadow.dom.Coordinate(new cljs.core.Keyword(null,"x","x",2099068185).cljs$core$IFn$_invoke$arity$1(G__26316),new cljs.core.Keyword(null,"y","y",-1757859776).cljs$core$IFn$_invoke$arity$1(G__26316),null,cljs.core.not_empty(extmap__5490__auto__),null));
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

(shadow.dom.Size.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k26352,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__26356 = k26352;
var G__26356__$1 = (((G__26356 instanceof cljs.core.Keyword))?G__26356.fqn:null);
switch (G__26356__$1) {
case "w":
return self__.w;

break;
case "h":
return self__.h;

break;
default:
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k26352,else__5451__auto__);

}
}));

(shadow.dom.Size.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (ret__5471__auto__,p__26357){
var vec__26358 = p__26357;
var k__5472__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26358,(0),null);
var v__5473__auto__ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26358,(1),null);
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

(shadow.dom.Size.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__26351){
var self__ = this;
var G__26351__$1 = this;
return (new cljs.core.RecordIter((0),G__26351__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"w","w",354169001),new cljs.core.Keyword(null,"h","h",1109658740)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator(self__.__extmap):cljs.core.nil_iter())));
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

(shadow.dom.Size.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this26353,other26354){
var self__ = this;
var this26353__$1 = this;
return (((!((other26354 == null)))) && ((((this26353__$1.constructor === other26354.constructor)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26353__$1.w,other26354.w)) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26353__$1.h,other26354.h)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(this26353__$1.__extmap,other26354.__extmap)))))))));
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

(shadow.dom.Size.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k26352){
var self__ = this;
var this__5455__auto____$1 = this;
var G__26373 = k26352;
var G__26373__$1 = (((G__26373 instanceof cljs.core.Keyword))?G__26373.fqn:null);
switch (G__26373__$1) {
case "w":
case "h":
return true;

break;
default:
return cljs.core.contains_QMARK_(self__.__extmap,k26352);

}
}));

(shadow.dom.Size.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__26351){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__26374 = cljs.core.keyword_identical_QMARK_;
var expr__26375 = k__5457__auto__;
if(cljs.core.truth_((pred__26374.cljs$core$IFn$_invoke$arity$2 ? pred__26374.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"w","w",354169001),expr__26375) : pred__26374.call(null,new cljs.core.Keyword(null,"w","w",354169001),expr__26375)))){
return (new shadow.dom.Size(G__26351,self__.h,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_((pred__26374.cljs$core$IFn$_invoke$arity$2 ? pred__26374.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"h","h",1109658740),expr__26375) : pred__26374.call(null,new cljs.core.Keyword(null,"h","h",1109658740),expr__26375)))){
return (new shadow.dom.Size(self__.w,G__26351,self__.__meta,self__.__extmap,null));
} else {
return (new shadow.dom.Size(self__.w,self__.h,self__.__meta,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(self__.__extmap,k__5457__auto__,G__26351),null));
}
}
}));

(shadow.dom.Size.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"w","w",354169001),self__.w,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"h","h",1109658740),self__.h,null))], null),self__.__extmap));
}));

(shadow.dom.Size.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__26351){
var self__ = this;
var this__5447__auto____$1 = this;
return (new shadow.dom.Size(self__.w,self__.h,G__26351,self__.__extmap,self__.__hash));
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
shadow.dom.map__GT_Size = (function shadow$dom$map__GT_Size(G__26355){
var extmap__5490__auto__ = (function (){var G__26378 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(G__26355,new cljs.core.Keyword(null,"w","w",354169001),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"h","h",1109658740)], 0));
if(cljs.core.record_QMARK_(G__26355)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,G__26378);
} else {
return G__26378;
}
})();
return (new shadow.dom.Size(new cljs.core.Keyword(null,"w","w",354169001).cljs$core$IFn$_invoke$arity$1(G__26355),new cljs.core.Keyword(null,"h","h",1109658740).cljs$core$IFn$_invoke$arity$1(G__26355),null,cljs.core.not_empty(extmap__5490__auto__),null));
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
var G__27243 = (i + (1));
var G__27244 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(ret,(opts[i]["value"]));
i = G__27243;
ret = G__27244;
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
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("&",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__26410){
var vec__26412 = p__26410;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26412,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26412,(1),null);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(k))+"="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v)))));
}),query_params))));
}
});
shadow.dom.redirect = (function shadow$dom$redirect(var_args){
var G__26427 = arguments.length;
switch (G__26427) {
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
var G__27256 = ps;
var G__27257 = (i + (1));
el__$1 = G__27256;
i = G__27257;
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
var vec__26473 = shadow.dom.parse_tag(tag_def);
var tag_name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26473,(0),null);
var tag_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26473,(1),null);
var tag_classes = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26473,(2),null);
var el = document.createElementNS("http://www.w3.org/2000/svg",tag_name);
if(cljs.core.truth_(tag_id)){
el.setAttribute("id",tag_id);
} else {
}

if(cljs.core.truth_(tag_classes)){
el.setAttribute("class",shadow.dom.merge_class_string(new cljs.core.Keyword(null,"class","class",-2030961996).cljs$core$IFn$_invoke$arity$1(props),tag_classes));
} else {
}

var seq__26479_27261 = cljs.core.seq(props);
var chunk__26480_27262 = null;
var count__26481_27263 = (0);
var i__26482_27264 = (0);
while(true){
if((i__26482_27264 < count__26481_27263)){
var vec__26498_27267 = chunk__26480_27262.cljs$core$IIndexed$_nth$arity$2(null,i__26482_27264);
var k_27268 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26498_27267,(0),null);
var v_27269 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26498_27267,(1),null);
el.setAttributeNS((function (){var temp__5823__auto__ = cljs.core.namespace(k_27268);
if(cljs.core.truth_(temp__5823__auto__)){
var ns = temp__5823__auto__;
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(shadow.dom.xmlns),ns);
} else {
return null;
}
})(),cljs.core.name(k_27268),v_27269);


var G__27273 = seq__26479_27261;
var G__27274 = chunk__26480_27262;
var G__27275 = count__26481_27263;
var G__27276 = (i__26482_27264 + (1));
seq__26479_27261 = G__27273;
chunk__26480_27262 = G__27274;
count__26481_27263 = G__27275;
i__26482_27264 = G__27276;
continue;
} else {
var temp__5823__auto___27277 = cljs.core.seq(seq__26479_27261);
if(temp__5823__auto___27277){
var seq__26479_27279__$1 = temp__5823__auto___27277;
if(cljs.core.chunked_seq_QMARK_(seq__26479_27279__$1)){
var c__5673__auto___27281 = cljs.core.chunk_first(seq__26479_27279__$1);
var G__27282 = cljs.core.chunk_rest(seq__26479_27279__$1);
var G__27283 = c__5673__auto___27281;
var G__27284 = cljs.core.count(c__5673__auto___27281);
var G__27285 = (0);
seq__26479_27261 = G__27282;
chunk__26480_27262 = G__27283;
count__26481_27263 = G__27284;
i__26482_27264 = G__27285;
continue;
} else {
var vec__26509_27286 = cljs.core.first(seq__26479_27279__$1);
var k_27287 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26509_27286,(0),null);
var v_27288 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26509_27286,(1),null);
el.setAttributeNS((function (){var temp__5823__auto____$1 = cljs.core.namespace(k_27287);
if(cljs.core.truth_(temp__5823__auto____$1)){
var ns = temp__5823__auto____$1;
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(shadow.dom.xmlns),ns);
} else {
return null;
}
})(),cljs.core.name(k_27287),v_27288);


var G__27290 = cljs.core.next(seq__26479_27279__$1);
var G__27291 = null;
var G__27292 = (0);
var G__27293 = (0);
seq__26479_27261 = G__27290;
chunk__26480_27262 = G__27291;
count__26481_27263 = G__27292;
i__26482_27264 = G__27293;
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
var vec__26529 = shadow.dom.destructure_node(shadow.dom.create_svg_node,structure);
var node = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26529,(0),null);
var node_children = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26529,(1),null);
var seq__26533_27301 = cljs.core.seq(node_children);
var chunk__26535_27302 = null;
var count__26536_27303 = (0);
var i__26537_27304 = (0);
while(true){
if((i__26537_27304 < count__26536_27303)){
var child_struct_27305 = chunk__26535_27302.cljs$core$IIndexed$_nth$arity$2(null,i__26537_27304);
if((!((child_struct_27305 == null)))){
if(typeof child_struct_27305 === 'string'){
var text_27306 = (node["textContent"]);
(node["textContent"] = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text_27306)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_struct_27305)));
} else {
var children_27308 = shadow.dom.svg_node(child_struct_27305);
if(cljs.core.seq_QMARK_(children_27308)){
var seq__26659_27309 = cljs.core.seq(children_27308);
var chunk__26661_27310 = null;
var count__26662_27311 = (0);
var i__26663_27312 = (0);
while(true){
if((i__26663_27312 < count__26662_27311)){
var child_27315 = chunk__26661_27310.cljs$core$IIndexed$_nth$arity$2(null,i__26663_27312);
if(cljs.core.truth_(child_27315)){
node.appendChild(child_27315);


var G__27316 = seq__26659_27309;
var G__27317 = chunk__26661_27310;
var G__27318 = count__26662_27311;
var G__27319 = (i__26663_27312 + (1));
seq__26659_27309 = G__27316;
chunk__26661_27310 = G__27317;
count__26662_27311 = G__27318;
i__26663_27312 = G__27319;
continue;
} else {
var G__27321 = seq__26659_27309;
var G__27322 = chunk__26661_27310;
var G__27323 = count__26662_27311;
var G__27324 = (i__26663_27312 + (1));
seq__26659_27309 = G__27321;
chunk__26661_27310 = G__27322;
count__26662_27311 = G__27323;
i__26663_27312 = G__27324;
continue;
}
} else {
var temp__5823__auto___27325 = cljs.core.seq(seq__26659_27309);
if(temp__5823__auto___27325){
var seq__26659_27326__$1 = temp__5823__auto___27325;
if(cljs.core.chunked_seq_QMARK_(seq__26659_27326__$1)){
var c__5673__auto___27327 = cljs.core.chunk_first(seq__26659_27326__$1);
var G__27328 = cljs.core.chunk_rest(seq__26659_27326__$1);
var G__27329 = c__5673__auto___27327;
var G__27330 = cljs.core.count(c__5673__auto___27327);
var G__27331 = (0);
seq__26659_27309 = G__27328;
chunk__26661_27310 = G__27329;
count__26662_27311 = G__27330;
i__26663_27312 = G__27331;
continue;
} else {
var child_27332 = cljs.core.first(seq__26659_27326__$1);
if(cljs.core.truth_(child_27332)){
node.appendChild(child_27332);


var G__27333 = cljs.core.next(seq__26659_27326__$1);
var G__27334 = null;
var G__27335 = (0);
var G__27336 = (0);
seq__26659_27309 = G__27333;
chunk__26661_27310 = G__27334;
count__26662_27311 = G__27335;
i__26663_27312 = G__27336;
continue;
} else {
var G__27337 = cljs.core.next(seq__26659_27326__$1);
var G__27338 = null;
var G__27339 = (0);
var G__27340 = (0);
seq__26659_27309 = G__27337;
chunk__26661_27310 = G__27338;
count__26662_27311 = G__27339;
i__26663_27312 = G__27340;
continue;
}
}
} else {
}
}
break;
}
} else {
node.appendChild(children_27308);
}
}


var G__27341 = seq__26533_27301;
var G__27342 = chunk__26535_27302;
var G__27343 = count__26536_27303;
var G__27344 = (i__26537_27304 + (1));
seq__26533_27301 = G__27341;
chunk__26535_27302 = G__27342;
count__26536_27303 = G__27343;
i__26537_27304 = G__27344;
continue;
} else {
var G__27346 = seq__26533_27301;
var G__27347 = chunk__26535_27302;
var G__27348 = count__26536_27303;
var G__27349 = (i__26537_27304 + (1));
seq__26533_27301 = G__27346;
chunk__26535_27302 = G__27347;
count__26536_27303 = G__27348;
i__26537_27304 = G__27349;
continue;
}
} else {
var temp__5823__auto___27351 = cljs.core.seq(seq__26533_27301);
if(temp__5823__auto___27351){
var seq__26533_27352__$1 = temp__5823__auto___27351;
if(cljs.core.chunked_seq_QMARK_(seq__26533_27352__$1)){
var c__5673__auto___27353 = cljs.core.chunk_first(seq__26533_27352__$1);
var G__27354 = cljs.core.chunk_rest(seq__26533_27352__$1);
var G__27355 = c__5673__auto___27353;
var G__27356 = cljs.core.count(c__5673__auto___27353);
var G__27357 = (0);
seq__26533_27301 = G__27354;
chunk__26535_27302 = G__27355;
count__26536_27303 = G__27356;
i__26537_27304 = G__27357;
continue;
} else {
var child_struct_27358 = cljs.core.first(seq__26533_27352__$1);
if((!((child_struct_27358 == null)))){
if(typeof child_struct_27358 === 'string'){
var text_27361 = (node["textContent"]);
(node["textContent"] = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text_27361)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_struct_27358)));
} else {
var children_27364 = shadow.dom.svg_node(child_struct_27358);
if(cljs.core.seq_QMARK_(children_27364)){
var seq__26715_27367 = cljs.core.seq(children_27364);
var chunk__26717_27368 = null;
var count__26718_27369 = (0);
var i__26719_27370 = (0);
while(true){
if((i__26719_27370 < count__26718_27369)){
var child_27379 = chunk__26717_27368.cljs$core$IIndexed$_nth$arity$2(null,i__26719_27370);
if(cljs.core.truth_(child_27379)){
node.appendChild(child_27379);


var G__27381 = seq__26715_27367;
var G__27382 = chunk__26717_27368;
var G__27383 = count__26718_27369;
var G__27384 = (i__26719_27370 + (1));
seq__26715_27367 = G__27381;
chunk__26717_27368 = G__27382;
count__26718_27369 = G__27383;
i__26719_27370 = G__27384;
continue;
} else {
var G__27385 = seq__26715_27367;
var G__27386 = chunk__26717_27368;
var G__27387 = count__26718_27369;
var G__27388 = (i__26719_27370 + (1));
seq__26715_27367 = G__27385;
chunk__26717_27368 = G__27386;
count__26718_27369 = G__27387;
i__26719_27370 = G__27388;
continue;
}
} else {
var temp__5823__auto___27394__$1 = cljs.core.seq(seq__26715_27367);
if(temp__5823__auto___27394__$1){
var seq__26715_27397__$1 = temp__5823__auto___27394__$1;
if(cljs.core.chunked_seq_QMARK_(seq__26715_27397__$1)){
var c__5673__auto___27399 = cljs.core.chunk_first(seq__26715_27397__$1);
var G__27401 = cljs.core.chunk_rest(seq__26715_27397__$1);
var G__27402 = c__5673__auto___27399;
var G__27403 = cljs.core.count(c__5673__auto___27399);
var G__27404 = (0);
seq__26715_27367 = G__27401;
chunk__26717_27368 = G__27402;
count__26718_27369 = G__27403;
i__26719_27370 = G__27404;
continue;
} else {
var child_27406 = cljs.core.first(seq__26715_27397__$1);
if(cljs.core.truth_(child_27406)){
node.appendChild(child_27406);


var G__27407 = cljs.core.next(seq__26715_27397__$1);
var G__27408 = null;
var G__27409 = (0);
var G__27410 = (0);
seq__26715_27367 = G__27407;
chunk__26717_27368 = G__27408;
count__26718_27369 = G__27409;
i__26719_27370 = G__27410;
continue;
} else {
var G__27411 = cljs.core.next(seq__26715_27397__$1);
var G__27412 = null;
var G__27413 = (0);
var G__27414 = (0);
seq__26715_27367 = G__27411;
chunk__26717_27368 = G__27412;
count__26718_27369 = G__27413;
i__26719_27370 = G__27414;
continue;
}
}
} else {
}
}
break;
}
} else {
node.appendChild(children_27364);
}
}


var G__27415 = cljs.core.next(seq__26533_27352__$1);
var G__27416 = null;
var G__27417 = (0);
var G__27418 = (0);
seq__26533_27301 = G__27415;
chunk__26535_27302 = G__27416;
count__26536_27303 = G__27417;
i__26537_27304 = G__27418;
continue;
} else {
var G__27419 = cljs.core.next(seq__26533_27352__$1);
var G__27420 = null;
var G__27421 = (0);
var G__27422 = (0);
seq__26533_27301 = G__27419;
chunk__26535_27302 = G__27420;
count__26536_27303 = G__27421;
i__26537_27304 = G__27422;
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
var len__5876__auto___27440 = arguments.length;
var i__5877__auto___27441 = (0);
while(true){
if((i__5877__auto___27441 < len__5876__auto___27440)){
args__5882__auto__.push((arguments[i__5877__auto___27441]));

var G__27446 = (i__5877__auto___27441 + (1));
i__5877__auto___27441 = G__27446;
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
(shadow.dom.svg.cljs$lang$applyTo = (function (seq26729){
var G__26730 = cljs.core.first(seq26729);
var seq26729__$1 = cljs.core.next(seq26729);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__26730,seq26729__$1);
}));


//# sourceMappingURL=shadow.dom.js.map
