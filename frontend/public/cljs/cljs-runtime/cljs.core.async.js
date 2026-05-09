goog.provide('cljs.core.async');
goog.scope(function(){
  cljs.core.async.goog$module$goog$array = goog.module.get('goog.array');
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Handler}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async13611 = (function (f,blockable,meta13612){
this.f = f;
this.blockable = blockable;
this.meta13612 = meta13612;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_13613,meta13612__$1){
var self__ = this;
var _13613__$1 = this;
return (new cljs.core.async.t_cljs$core$async13611(self__.f,self__.blockable,meta13612__$1));
}));

(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_13613){
var self__ = this;
var _13613__$1 = this;
return self__.meta13612;
}));

(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.blockable;
}));

(cljs.core.async.t_cljs$core$async13611.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.f;
}));

(cljs.core.async.t_cljs$core$async13611.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"blockable","blockable",-28395259,null),new cljs.core.Symbol(null,"meta13612","meta13612",-1643578736,null)], null);
}));

(cljs.core.async.t_cljs$core$async13611.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async13611.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async13611");

(cljs.core.async.t_cljs$core$async13611.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async13611");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async13611.
 */
cljs.core.async.__GT_t_cljs$core$async13611 = (function cljs$core$async$__GT_t_cljs$core$async13611(f,blockable,meta13612){
return (new cljs.core.async.t_cljs$core$async13611(f,blockable,meta13612));
});


cljs.core.async.fn_handler = (function cljs$core$async$fn_handler(var_args){
var G__13610 = arguments.length;
switch (G__13610) {
case 1:
return cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$1 = (function (f){
return cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$2(f,true);
}));

(cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$2 = (function (f,blockable){
return (new cljs.core.async.t_cljs$core$async13611(f,blockable,cljs.core.PersistentArrayMap.EMPTY));
}));

(cljs.core.async.fn_handler.cljs$lang$maxFixedArity = 2);

/**
 * Returns a fixed buffer of size n. When full, puts will block/park.
 */
cljs.core.async.buffer = (function cljs$core$async$buffer(n){
return cljs.core.async.impl.buffers.fixed_buffer(n);
});
/**
 * Returns a buffer of size n. When full, puts will complete but
 *   val will be dropped (no transfer).
 */
cljs.core.async.dropping_buffer = (function cljs$core$async$dropping_buffer(n){
return cljs.core.async.impl.buffers.dropping_buffer(n);
});
/**
 * Returns a buffer of size n. When full, puts will complete, and be
 *   buffered, but oldest elements in buffer will be dropped (not
 *   transferred).
 */
cljs.core.async.sliding_buffer = (function cljs$core$async$sliding_buffer(n){
return cljs.core.async.impl.buffers.sliding_buffer(n);
});
/**
 * Returns true if a channel created with buff will never block. That is to say,
 * puts into this buffer will never cause the buffer to be full. 
 */
cljs.core.async.unblocking_buffer_QMARK_ = (function cljs$core$async$unblocking_buffer_QMARK_(buff){
if((!((buff == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === buff.cljs$core$async$impl$protocols$UnblockingBuffer$)))){
return true;
} else {
if((!buff.cljs$lang$protocol_mask$partition$)){
return cljs.core.native_satisfies_QMARK_(cljs.core.async.impl.protocols.UnblockingBuffer,buff);
} else {
return false;
}
}
} else {
return cljs.core.native_satisfies_QMARK_(cljs.core.async.impl.protocols.UnblockingBuffer,buff);
}
});
/**
 * Creates a channel with an optional buffer, an optional transducer (like (map f),
 *   (filter p) etc or a composition thereof), and an optional exception handler.
 *   If buf-or-n is a number, will create and use a fixed buffer of that size. If a
 *   transducer is supplied a buffer must be specified. ex-handler must be a
 *   fn of one argument - if an exception occurs during transformation it will be called
 *   with the thrown value as an argument, and any non-nil return value will be placed
 *   in the channel.
 */
cljs.core.async.chan = (function cljs$core$async$chan(var_args){
var G__13628 = arguments.length;
switch (G__13628) {
case 0:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(null);
}));

(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1 = (function (buf_or_n){
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3(buf_or_n,null,null);
}));

(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$2 = (function (buf_or_n,xform){
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3(buf_or_n,xform,null);
}));

(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3 = (function (buf_or_n,xform,ex_handler){
var buf_or_n__$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(buf_or_n,(0)))?null:buf_or_n);
if(cljs.core.truth_(xform)){
if(cljs.core.truth_(buf_or_n__$1)){
} else {
throw (new Error((""+"Assert failed: "+"buffer must be supplied when transducer is"+"\n"+"buf-or-n")));
}
} else {
}

return cljs.core.async.impl.channels.chan.cljs$core$IFn$_invoke$arity$3(((typeof buf_or_n__$1 === 'number')?cljs.core.async.buffer(buf_or_n__$1):buf_or_n__$1),xform,ex_handler);
}));

(cljs.core.async.chan.cljs$lang$maxFixedArity = 3);

/**
 * Creates a promise channel with an optional transducer, and an optional
 *   exception-handler. A promise channel can take exactly one value that consumers
 *   will receive. Once full, puts complete but val is dropped (no transfer).
 *   Consumers will block until either a value is placed in the channel or the
 *   channel is closed, then return the value (or nil) forever. See chan for the
 *   semantics of xform and ex-handler.
 */
cljs.core.async.promise_chan = (function cljs$core$async$promise_chan(var_args){
var G__13643 = arguments.length;
switch (G__13643) {
case 0:
return cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$1(null);
}));

(cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$1 = (function (xform){
return cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$2(xform,null);
}));

(cljs.core.async.promise_chan.cljs$core$IFn$_invoke$arity$2 = (function (xform,ex_handler){
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3(cljs.core.async.impl.buffers.promise_buffer(),xform,ex_handler);
}));

(cljs.core.async.promise_chan.cljs$lang$maxFixedArity = 2);

/**
 * Returns a channel that will close after msecs
 */
cljs.core.async.timeout = (function cljs$core$async$timeout(msecs){
return cljs.core.async.impl.timers.timeout(msecs);
});
/**
 * takes a val from port. Must be called inside a (go ...) block. Will
 *   return nil if closed. Will park if nothing is available.
 *   Returns true unless port is already closed
 */
cljs.core.async._LT__BANG_ = (function cljs$core$async$_LT__BANG_(port){
throw (new Error("<! used not in (go ...) block"));
});
/**
 * Asynchronously takes a val from port, passing to fn1. Will pass nil
 * if closed. If on-caller? (default true) is true, and value is
 * immediately available, will call fn1 on calling thread.
 * Returns nil.
 */
cljs.core.async.take_BANG_ = (function cljs$core$async$take_BANG_(var_args){
var G__13659 = arguments.length;
switch (G__13659) {
case 2:
return cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (port,fn1){
return cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$3(port,fn1,true);
}));

(cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (port,fn1,on_caller_QMARK_){
var ret = cljs.core.async.impl.protocols.take_BANG_(port,cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$1(fn1));
if(cljs.core.truth_(ret)){
var val_17429 = cljs.core.deref(ret);
if(cljs.core.truth_(on_caller_QMARK_)){
(fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(val_17429) : fn1.call(null,val_17429));
} else {
cljs.core.async.impl.dispatch.run((function (){
return (fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(val_17429) : fn1.call(null,val_17429));
}));
}
} else {
}

return null;
}));

(cljs.core.async.take_BANG_.cljs$lang$maxFixedArity = 3);

cljs.core.async.nop = (function cljs$core$async$nop(_){
return null;
});
cljs.core.async.fhnop = cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$1(cljs.core.async.nop);
/**
 * puts a val into port. nil values are not allowed. Must be called
 *   inside a (go ...) block. Will park if no buffer space is available.
 *   Returns true unless port is already closed.
 */
cljs.core.async._GT__BANG_ = (function cljs$core$async$_GT__BANG_(port,val){
throw (new Error(">! used not in (go ...) block"));
});
/**
 * Asynchronously puts a val into port, calling fn1 (if supplied) when
 * complete. nil values are not allowed. Will throw if closed. If
 * on-caller? (default true) is true, and the put is immediately
 * accepted, will call fn1 on calling thread.  Returns nil.
 */
cljs.core.async.put_BANG_ = (function cljs$core$async$put_BANG_(var_args){
var G__13668 = arguments.length;
switch (G__13668) {
case 2:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (port,val){
var temp__5821__auto__ = cljs.core.async.impl.protocols.put_BANG_(port,val,cljs.core.async.fhnop);
if(cljs.core.truth_(temp__5821__auto__)){
var ret = temp__5821__auto__;
return cljs.core.deref(ret);
} else {
return true;
}
}));

(cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (port,val,fn1){
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$4(port,val,fn1,true);
}));

(cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (port,val,fn1,on_caller_QMARK_){
var temp__5821__auto__ = cljs.core.async.impl.protocols.put_BANG_(port,val,cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$1(fn1));
if(cljs.core.truth_(temp__5821__auto__)){
var retb = temp__5821__auto__;
var ret = cljs.core.deref(retb);
if(cljs.core.truth_(on_caller_QMARK_)){
(fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(ret) : fn1.call(null,ret));
} else {
cljs.core.async.impl.dispatch.run((function (){
return (fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(ret) : fn1.call(null,ret));
}));
}

return ret;
} else {
return true;
}
}));

(cljs.core.async.put_BANG_.cljs$lang$maxFixedArity = 4);

cljs.core.async.close_BANG_ = (function cljs$core$async$close_BANG_(port){
return cljs.core.async.impl.protocols.close_BANG_(port);
});
cljs.core.async.random_array = (function cljs$core$async$random_array(n){
var a = (new Array(n));
var n__5741__auto___17438 = n;
var x_17439 = (0);
while(true){
if((x_17439 < n__5741__auto___17438)){
(a[x_17439] = x_17439);

var G__17440 = (x_17439 + (1));
x_17439 = G__17440;
continue;
} else {
}
break;
}

cljs.core.async.goog$module$goog$array.shuffle(a);

return a;
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Handler}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async13722 = (function (flag,meta13723){
this.flag = flag;
this.meta13723 = meta13723;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_13724,meta13723__$1){
var self__ = this;
var _13724__$1 = this;
return (new cljs.core.async.t_cljs$core$async13722(self__.flag,meta13723__$1));
}));

(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_13724){
var self__ = this;
var _13724__$1 = this;
return self__.meta13723;
}));

(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref(self__.flag);
}));

(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async13722.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.flag,null);

return true;
}));

(cljs.core.async.t_cljs$core$async13722.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"meta13723","meta13723",-1716726142,null)], null);
}));

(cljs.core.async.t_cljs$core$async13722.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async13722.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async13722");

(cljs.core.async.t_cljs$core$async13722.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async13722");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async13722.
 */
cljs.core.async.__GT_t_cljs$core$async13722 = (function cljs$core$async$__GT_t_cljs$core$async13722(flag,meta13723){
return (new cljs.core.async.t_cljs$core$async13722(flag,meta13723));
});


cljs.core.async.alt_flag = (function cljs$core$async$alt_flag(){
var flag = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(true);
return (new cljs.core.async.t_cljs$core$async13722(flag,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Handler}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async13741 = (function (flag,cb,meta13742){
this.flag = flag;
this.cb = cb;
this.meta13742 = meta13742;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_13743,meta13742__$1){
var self__ = this;
var _13743__$1 = this;
return (new cljs.core.async.t_cljs$core$async13741(self__.flag,self__.cb,meta13742__$1));
}));

(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_13743){
var self__ = this;
var _13743__$1 = this;
return self__.meta13742;
}));

(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.active_QMARK_(self__.flag);
}));

(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async13741.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.async.impl.protocols.commit(self__.flag);

return self__.cb;
}));

(cljs.core.async.t_cljs$core$async13741.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"cb","cb",-2064487928,null),new cljs.core.Symbol(null,"meta13742","meta13742",-362133875,null)], null);
}));

(cljs.core.async.t_cljs$core$async13741.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async13741.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async13741");

(cljs.core.async.t_cljs$core$async13741.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async13741");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async13741.
 */
cljs.core.async.__GT_t_cljs$core$async13741 = (function cljs$core$async$__GT_t_cljs$core$async13741(flag,cb,meta13742){
return (new cljs.core.async.t_cljs$core$async13741(flag,cb,meta13742));
});


cljs.core.async.alt_handler = (function cljs$core$async$alt_handler(flag,cb){
return (new cljs.core.async.t_cljs$core$async13741(flag,cb,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * returns derefable [val port] if immediate, nil if enqueued
 */
cljs.core.async.do_alts = (function cljs$core$async$do_alts(fret,ports,opts){
if((cljs.core.count(ports) > (0))){
} else {
throw (new Error((""+"Assert failed: "+"alts must have at least one channel operation"+"\n"+"(pos? (count ports))")));
}

var flag = cljs.core.async.alt_flag();
var ports__$1 = cljs.core.vec(ports);
var n = cljs.core.count(ports__$1);
var _ = (function (){var i = (0);
while(true){
if((i < n)){
var port_17455 = cljs.core.nth.cljs$core$IFn$_invoke$arity$2(ports__$1,i);
if(cljs.core.vector_QMARK_(port_17455)){
if((!(((port_17455.cljs$core$IFn$_invoke$arity$1 ? port_17455.cljs$core$IFn$_invoke$arity$1((1)) : port_17455.call(null,(1))) == null)))){
} else {
throw (new Error((""+"Assert failed: "+"can't put nil on channel"+"\n"+"(some? (port 1))")));
}
} else {
}

var G__17456 = (i + (1));
i = G__17456;
continue;
} else {
return null;
}
break;
}
})();
var idxs = cljs.core.async.random_array(n);
var priority = new cljs.core.Keyword(null,"priority","priority",1431093715).cljs$core$IFn$_invoke$arity$1(opts);
var ret = (function (){var i = (0);
while(true){
if((i < n)){
var idx = (cljs.core.truth_(priority)?i:(idxs[i]));
var port = cljs.core.nth.cljs$core$IFn$_invoke$arity$2(ports__$1,idx);
var wport = ((cljs.core.vector_QMARK_(port))?(port.cljs$core$IFn$_invoke$arity$1 ? port.cljs$core$IFn$_invoke$arity$1((0)) : port.call(null,(0))):null);
var vbox = (cljs.core.truth_(wport)?(function (){var val = (port.cljs$core$IFn$_invoke$arity$1 ? port.cljs$core$IFn$_invoke$arity$1((1)) : port.call(null,(1)));
return cljs.core.async.impl.protocols.put_BANG_(wport,val,cljs.core.async.alt_handler(flag,((function (i,val,idx,port,wport,flag,ports__$1,n,_,idxs,priority){
return (function (p1__13764_SHARP_){
var G__13777 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__13764_SHARP_,wport], null);
return (fret.cljs$core$IFn$_invoke$arity$1 ? fret.cljs$core$IFn$_invoke$arity$1(G__13777) : fret.call(null,G__13777));
});})(i,val,idx,port,wport,flag,ports__$1,n,_,idxs,priority))
));
})():cljs.core.async.impl.protocols.take_BANG_(port,cljs.core.async.alt_handler(flag,((function (i,idx,port,wport,flag,ports__$1,n,_,idxs,priority){
return (function (p1__13765_SHARP_){
var G__13779 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__13765_SHARP_,port], null);
return (fret.cljs$core$IFn$_invoke$arity$1 ? fret.cljs$core$IFn$_invoke$arity$1(G__13779) : fret.call(null,G__13779));
});})(i,idx,port,wport,flag,ports__$1,n,_,idxs,priority))
)));
if(cljs.core.truth_(vbox)){
return cljs.core.async.impl.channels.box(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.deref(vbox),(function (){var or__5142__auto__ = wport;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return port;
}
})()], null));
} else {
var G__17465 = (i + (1));
i = G__17465;
continue;
}
} else {
return null;
}
break;
}
})();
var or__5142__auto__ = ret;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.contains_QMARK_(opts,new cljs.core.Keyword(null,"default","default",-1987822328))){
var temp__5823__auto__ = (function (){var and__5140__auto__ = flag.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1(null);
if(cljs.core.truth_(and__5140__auto__)){
return flag.cljs$core$async$impl$protocols$Handler$commit$arity$1(null);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var got = temp__5823__auto__;
return cljs.core.async.impl.channels.box(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"default","default",-1987822328).cljs$core$IFn$_invoke$arity$1(opts),new cljs.core.Keyword(null,"default","default",-1987822328)], null));
} else {
return null;
}
} else {
return null;
}
}
});
/**
 * Completes at most one of several channel operations. Must be called
 * inside a (go ...) block. ports is a vector of channel endpoints,
 * which can be either a channel to take from or a vector of
 *   [channel-to-put-to val-to-put], in any combination. Takes will be
 *   made as if by <!, and puts will be made as if by >!. Unless
 *   the :priority option is true, if more than one port operation is
 *   ready a non-deterministic choice will be made. If no operation is
 *   ready and a :default value is supplied, [default-val :default] will
 *   be returned, otherwise alts! will park until the first operation to
 *   become ready completes. Returns [val port] of the completed
 *   operation, where val is the value taken for takes, and a
 *   boolean (true unless already closed, as per put!) for puts.
 * 
 *   opts are passed as :key val ... Supported options:
 * 
 *   :default val - the value to use if none of the operations are immediately ready
 *   :priority true - (default nil) when true, the operations will be tried in order.
 * 
 *   Note: there is no guarantee that the port exps or val exprs will be
 *   used, nor in what order should they be, so they should not be
 *   depended upon for side effects.
 */
cljs.core.async.alts_BANG_ = (function cljs$core$async$alts_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___17467 = arguments.length;
var i__5877__auto___17468 = (0);
while(true){
if((i__5877__auto___17468 < len__5876__auto___17467)){
args__5882__auto__.push((arguments[i__5877__auto___17468]));

var G__17472 = (i__5877__auto___17468 + (1));
i__5877__auto___17468 = G__17472;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (ports,p__13815){
var map__13816 = p__13815;
var map__13816__$1 = cljs.core.__destructure_map(map__13816);
var opts = map__13816__$1;
throw (new Error("alts! used not in (go ...) block"));
}));

(cljs.core.async.alts_BANG_.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(cljs.core.async.alts_BANG_.cljs$lang$applyTo = (function (seq13787){
var G__13788 = cljs.core.first(seq13787);
var seq13787__$1 = cljs.core.next(seq13787);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__13788,seq13787__$1);
}));

/**
 * Puts a val into port if it's possible to do so immediately.
 *   nil values are not allowed. Never blocks. Returns true if offer succeeds.
 */
cljs.core.async.offer_BANG_ = (function cljs$core$async$offer_BANG_(port,val){
var ret = cljs.core.async.impl.protocols.put_BANG_(port,val,cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$2(cljs.core.async.nop,false));
if(cljs.core.truth_(ret)){
return cljs.core.deref(ret);
} else {
return null;
}
});
/**
 * Takes a val from port if it's possible to do so immediately.
 *   Never blocks. Returns value if successful, nil otherwise.
 */
cljs.core.async.poll_BANG_ = (function cljs$core$async$poll_BANG_(port){
var ret = cljs.core.async.impl.protocols.take_BANG_(port,cljs.core.async.fn_handler.cljs$core$IFn$_invoke$arity$2(cljs.core.async.nop,false));
if(cljs.core.truth_(ret)){
return cljs.core.deref(ret);
} else {
return null;
}
});
/**
 * Takes elements from the from channel and supplies them to the to
 * channel. By default, the to channel will be closed when the from
 * channel closes, but can be determined by the close?  parameter. Will
 * stop consuming the from channel if the to channel closes
 */
cljs.core.async.pipe = (function cljs$core$async$pipe(var_args){
var G__13851 = arguments.length;
switch (G__13851) {
case 2:
return cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$2 = (function (from,to){
return cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$3(from,to,true);
}));

(cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$3 = (function (from,to,close_QMARK_){
var c__13502__auto___17492 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_13943){
var state_val_13944 = (state_13943[(1)]);
if((state_val_13944 === (7))){
var inst_13935 = (state_13943[(2)]);
var state_13943__$1 = state_13943;
var statearr_13970_17493 = state_13943__$1;
(statearr_13970_17493[(2)] = inst_13935);

(statearr_13970_17493[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (1))){
var state_13943__$1 = state_13943;
var statearr_13978_17494 = state_13943__$1;
(statearr_13978_17494[(2)] = null);

(statearr_13978_17494[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (4))){
var inst_13911 = (state_13943[(7)]);
var inst_13911__$1 = (state_13943[(2)]);
var inst_13917 = (inst_13911__$1 == null);
var state_13943__$1 = (function (){var statearr_13988 = state_13943;
(statearr_13988[(7)] = inst_13911__$1);

return statearr_13988;
})();
if(cljs.core.truth_(inst_13917)){
var statearr_13990_17495 = state_13943__$1;
(statearr_13990_17495[(1)] = (5));

} else {
var statearr_13993_17496 = state_13943__$1;
(statearr_13993_17496[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (13))){
var state_13943__$1 = state_13943;
var statearr_14001_17497 = state_13943__$1;
(statearr_14001_17497[(2)] = null);

(statearr_14001_17497[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (6))){
var inst_13911 = (state_13943[(7)]);
var state_13943__$1 = state_13943;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_13943__$1,(11),to,inst_13911);
} else {
if((state_val_13944 === (3))){
var inst_13937 = (state_13943[(2)]);
var state_13943__$1 = state_13943;
return cljs.core.async.impl.ioc_helpers.return_chan(state_13943__$1,inst_13937);
} else {
if((state_val_13944 === (12))){
var state_13943__$1 = state_13943;
var statearr_14036_17499 = state_13943__$1;
(statearr_14036_17499[(2)] = null);

(statearr_14036_17499[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (2))){
var state_13943__$1 = state_13943;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_13943__$1,(4),from);
} else {
if((state_val_13944 === (11))){
var inst_13928 = (state_13943[(2)]);
var state_13943__$1 = state_13943;
if(cljs.core.truth_(inst_13928)){
var statearr_14049_17500 = state_13943__$1;
(statearr_14049_17500[(1)] = (12));

} else {
var statearr_14050_17501 = state_13943__$1;
(statearr_14050_17501[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (9))){
var state_13943__$1 = state_13943;
var statearr_14053_17502 = state_13943__$1;
(statearr_14053_17502[(2)] = null);

(statearr_14053_17502[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (5))){
var state_13943__$1 = state_13943;
if(cljs.core.truth_(close_QMARK_)){
var statearr_14056_17503 = state_13943__$1;
(statearr_14056_17503[(1)] = (8));

} else {
var statearr_14058_17504 = state_13943__$1;
(statearr_14058_17504[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (14))){
var inst_13933 = (state_13943[(2)]);
var state_13943__$1 = state_13943;
var statearr_14061_17505 = state_13943__$1;
(statearr_14061_17505[(2)] = inst_13933);

(statearr_14061_17505[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (10))){
var inst_13925 = (state_13943[(2)]);
var state_13943__$1 = state_13943;
var statearr_14062_17506 = state_13943__$1;
(statearr_14062_17506[(2)] = inst_13925);

(statearr_14062_17506[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_13944 === (8))){
var inst_13922 = cljs.core.async.close_BANG_(to);
var state_13943__$1 = state_13943;
var statearr_14067_17521 = state_13943__$1;
(statearr_14067_17521[(2)] = inst_13922);

(statearr_14067_17521[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_14069 = [null,null,null,null,null,null,null,null];
(statearr_14069[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_14069[(1)] = (1));

return statearr_14069;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_13943){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_13943);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14072){var ex__12399__auto__ = e14072;
var statearr_14074_17525 = state_13943;
(statearr_14074_17525[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_13943[(4)]))){
var statearr_14076_17529 = state_13943;
(statearr_14076_17529[(1)] = cljs.core.first((state_13943[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17530 = state_13943;
state_13943 = G__17530;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_13943){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_13943);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14078 = f__13503__auto__();
(statearr_14078[(6)] = c__13502__auto___17492);

return statearr_14078;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return to;
}));

(cljs.core.async.pipe.cljs$lang$maxFixedArity = 3);

cljs.core.async.pipeline_STAR_ = (function cljs$core$async$pipeline_STAR_(n,to,xf,from,close_QMARK_,ex_handler,type){
if((n > (0))){
} else {
throw (new Error("Assert failed: (pos? n)"));
}

var jobs = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(n);
var results = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(n);
var process__$1 = (function (p__14105){
var vec__14106 = p__14105;
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__14106,(0),null);
var p = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__14106,(1),null);
var job = vec__14106;
if((job == null)){
cljs.core.async.close_BANG_(results);

return null;
} else {
var res = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3((1),xf,ex_handler);
var c__13502__auto___17531 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14124){
var state_val_14125 = (state_14124[(1)]);
if((state_val_14125 === (1))){
var state_14124__$1 = state_14124;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14124__$1,(2),res,v);
} else {
if((state_val_14125 === (2))){
var inst_14121 = (state_14124[(2)]);
var inst_14122 = cljs.core.async.close_BANG_(res);
var state_14124__$1 = (function (){var statearr_14127 = state_14124;
(statearr_14127[(7)] = inst_14121);

return statearr_14127;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_14124__$1,inst_14122);
} else {
return null;
}
}
});
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_14133 = [null,null,null,null,null,null,null,null];
(statearr_14133[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__);

(statearr_14133[(1)] = (1));

return statearr_14133;
});
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1 = (function (state_14124){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14124);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14134){var ex__12399__auto__ = e14134;
var statearr_14135_17537 = state_14124;
(statearr_14135_17537[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14124[(4)]))){
var statearr_14136_17538 = state_14124;
(statearr_14136_17538[(1)] = cljs.core.first((state_14124[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17541 = state_14124;
state_14124 = G__17541;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = function(state_14124){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1.call(this,state_14124);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14153 = f__13503__auto__();
(statearr_14153[(6)] = c__13502__auto___17531);

return statearr_14153;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(p,res);

return true;
}
});
var async = (function (p__14162){
var vec__14163 = p__14162;
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__14163,(0),null);
var p = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__14163,(1),null);
var job = vec__14163;
if((job == null)){
cljs.core.async.close_BANG_(results);

return null;
} else {
var res = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
(xf.cljs$core$IFn$_invoke$arity$2 ? xf.cljs$core$IFn$_invoke$arity$2(v,res) : xf.call(null,v,res));

cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(p,res);

return true;
}
});
var n__5741__auto___17545 = n;
var __17547 = (0);
while(true){
if((__17547 < n__5741__auto___17545)){
var G__14200_17550 = type;
var G__14200_17551__$1 = (((G__14200_17550 instanceof cljs.core.Keyword))?G__14200_17550.fqn:null);
switch (G__14200_17551__$1) {
case "compute":
var c__13502__auto___17555 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run(((function (__17547,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = ((function (__17547,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function (state_14215){
var state_val_14217 = (state_14215[(1)]);
if((state_val_14217 === (1))){
var state_14215__$1 = state_14215;
var statearr_14223_17556 = state_14215__$1;
(statearr_14223_17556[(2)] = null);

(statearr_14223_17556[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14217 === (2))){
var state_14215__$1 = state_14215;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14215__$1,(4),jobs);
} else {
if((state_val_14217 === (3))){
var inst_14212 = (state_14215[(2)]);
var state_14215__$1 = state_14215;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14215__$1,inst_14212);
} else {
if((state_val_14217 === (4))){
var inst_14204 = (state_14215[(2)]);
var inst_14205 = process__$1(inst_14204);
var state_14215__$1 = state_14215;
if(cljs.core.truth_(inst_14205)){
var statearr_14225_17557 = state_14215__$1;
(statearr_14225_17557[(1)] = (5));

} else {
var statearr_14226_17562 = state_14215__$1;
(statearr_14226_17562[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14217 === (5))){
var state_14215__$1 = state_14215;
var statearr_14227_17563 = state_14215__$1;
(statearr_14227_17563[(2)] = null);

(statearr_14227_17563[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14217 === (6))){
var state_14215__$1 = state_14215;
var statearr_14228_17565 = state_14215__$1;
(statearr_14228_17565[(2)] = null);

(statearr_14228_17565[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14217 === (7))){
var inst_14210 = (state_14215[(2)]);
var state_14215__$1 = state_14215;
var statearr_14242_17571 = state_14215__$1;
(statearr_14242_17571[(2)] = inst_14210);

(statearr_14242_17571[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
});})(__17547,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
;
return ((function (__17547,switch__12395__auto__,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_14245 = [null,null,null,null,null,null,null];
(statearr_14245[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__);

(statearr_14245[(1)] = (1));

return statearr_14245;
});
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1 = (function (state_14215){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14215);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14247){var ex__12399__auto__ = e14247;
var statearr_14248_17584 = state_14215;
(statearr_14248_17584[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14215[(4)]))){
var statearr_14251_17585 = state_14215;
(statearr_14251_17585[(1)] = cljs.core.first((state_14215[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17590 = state_14215;
state_14215 = G__17590;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = function(state_14215){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1.call(this,state_14215);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__;
})()
;})(__17547,switch__12395__auto__,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
})();
var state__13504__auto__ = (function (){var statearr_14253 = f__13503__auto__();
(statearr_14253[(6)] = c__13502__auto___17555);

return statearr_14253;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
});})(__17547,c__13502__auto___17555,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
);


break;
case "async":
var c__13502__auto___17595 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run(((function (__17547,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = ((function (__17547,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function (state_14270){
var state_val_14271 = (state_14270[(1)]);
if((state_val_14271 === (1))){
var state_14270__$1 = state_14270;
var statearr_14275_17597 = state_14270__$1;
(statearr_14275_17597[(2)] = null);

(statearr_14275_17597[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14271 === (2))){
var state_14270__$1 = state_14270;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14270__$1,(4),jobs);
} else {
if((state_val_14271 === (3))){
var inst_14268 = (state_14270[(2)]);
var state_14270__$1 = state_14270;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14270__$1,inst_14268);
} else {
if((state_val_14271 === (4))){
var inst_14260 = (state_14270[(2)]);
var inst_14261 = async(inst_14260);
var state_14270__$1 = state_14270;
if(cljs.core.truth_(inst_14261)){
var statearr_14276_17606 = state_14270__$1;
(statearr_14276_17606[(1)] = (5));

} else {
var statearr_14277_17608 = state_14270__$1;
(statearr_14277_17608[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14271 === (5))){
var state_14270__$1 = state_14270;
var statearr_14282_17610 = state_14270__$1;
(statearr_14282_17610[(2)] = null);

(statearr_14282_17610[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14271 === (6))){
var state_14270__$1 = state_14270;
var statearr_14286_17611 = state_14270__$1;
(statearr_14286_17611[(2)] = null);

(statearr_14286_17611[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14271 === (7))){
var inst_14266 = (state_14270[(2)]);
var state_14270__$1 = state_14270;
var statearr_14287_17612 = state_14270__$1;
(statearr_14287_17612[(2)] = inst_14266);

(statearr_14287_17612[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
});})(__17547,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
;
return ((function (__17547,switch__12395__auto__,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_14289 = [null,null,null,null,null,null,null];
(statearr_14289[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__);

(statearr_14289[(1)] = (1));

return statearr_14289;
});
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1 = (function (state_14270){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14270);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14290){var ex__12399__auto__ = e14290;
var statearr_14292_17616 = state_14270;
(statearr_14292_17616[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14270[(4)]))){
var statearr_14297_17617 = state_14270;
(statearr_14297_17617[(1)] = cljs.core.first((state_14270[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17621 = state_14270;
state_14270 = G__17621;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = function(state_14270){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1.call(this,state_14270);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__;
})()
;})(__17547,switch__12395__auto__,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
})();
var state__13504__auto__ = (function (){var statearr_14299 = f__13503__auto__();
(statearr_14299[(6)] = c__13502__auto___17595);

return statearr_14299;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
});})(__17547,c__13502__auto___17595,G__14200_17550,G__14200_17551__$1,n__5741__auto___17545,jobs,results,process__$1,async))
);


break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__14200_17551__$1))));

}

var G__17626 = (__17547 + (1));
__17547 = G__17626;
continue;
} else {
}
break;
}

var c__13502__auto___17627 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14335){
var state_val_14336 = (state_14335[(1)]);
if((state_val_14336 === (7))){
var inst_14330 = (state_14335[(2)]);
var state_14335__$1 = state_14335;
var statearr_14406_17630 = state_14335__$1;
(statearr_14406_17630[(2)] = inst_14330);

(statearr_14406_17630[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14336 === (1))){
var state_14335__$1 = state_14335;
var statearr_14407_17631 = state_14335__$1;
(statearr_14407_17631[(2)] = null);

(statearr_14407_17631[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14336 === (4))){
var inst_14306 = (state_14335[(7)]);
var inst_14306__$1 = (state_14335[(2)]);
var inst_14307 = (inst_14306__$1 == null);
var state_14335__$1 = (function (){var statearr_14408 = state_14335;
(statearr_14408[(7)] = inst_14306__$1);

return statearr_14408;
})();
if(cljs.core.truth_(inst_14307)){
var statearr_14409_17636 = state_14335__$1;
(statearr_14409_17636[(1)] = (5));

} else {
var statearr_14411_17648 = state_14335__$1;
(statearr_14411_17648[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14336 === (6))){
var inst_14306 = (state_14335[(7)]);
var inst_14312 = (state_14335[(8)]);
var inst_14312__$1 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
var inst_14320 = cljs.core.PersistentVector.EMPTY_NODE;
var inst_14321 = [inst_14306,inst_14312__$1];
var inst_14322 = (new cljs.core.PersistentVector(null,2,(5),inst_14320,inst_14321,null));
var state_14335__$1 = (function (){var statearr_14412 = state_14335;
(statearr_14412[(8)] = inst_14312__$1);

return statearr_14412;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14335__$1,(8),jobs,inst_14322);
} else {
if((state_val_14336 === (3))){
var inst_14333 = (state_14335[(2)]);
var state_14335__$1 = state_14335;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14335__$1,inst_14333);
} else {
if((state_val_14336 === (2))){
var state_14335__$1 = state_14335;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14335__$1,(4),from);
} else {
if((state_val_14336 === (9))){
var inst_14327 = (state_14335[(2)]);
var state_14335__$1 = (function (){var statearr_14418 = state_14335;
(statearr_14418[(9)] = inst_14327);

return statearr_14418;
})();
var statearr_14419_17653 = state_14335__$1;
(statearr_14419_17653[(2)] = null);

(statearr_14419_17653[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14336 === (5))){
var inst_14309 = cljs.core.async.close_BANG_(jobs);
var state_14335__$1 = state_14335;
var statearr_14426_17654 = state_14335__$1;
(statearr_14426_17654[(2)] = inst_14309);

(statearr_14426_17654[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14336 === (8))){
var inst_14312 = (state_14335[(8)]);
var inst_14324 = (state_14335[(2)]);
var state_14335__$1 = (function (){var statearr_14430 = state_14335;
(statearr_14430[(10)] = inst_14324);

return statearr_14430;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14335__$1,(9),results,inst_14312);
} else {
return null;
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_14431 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_14431[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__);

(statearr_14431[(1)] = (1));

return statearr_14431;
});
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1 = (function (state_14335){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14335);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14435){var ex__12399__auto__ = e14435;
var statearr_14436_17659 = state_14335;
(statearr_14436_17659[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14335[(4)]))){
var statearr_14438_17664 = state_14335;
(statearr_14438_17664[(1)] = cljs.core.first((state_14335[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17666 = state_14335;
state_14335 = G__17666;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = function(state_14335){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1.call(this,state_14335);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14441 = f__13503__auto__();
(statearr_14441[(6)] = c__13502__auto___17627);

return statearr_14441;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


var c__13502__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14492){
var state_val_14493 = (state_14492[(1)]);
if((state_val_14493 === (7))){
var inst_14486 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
var statearr_14508_17672 = state_14492__$1;
(statearr_14508_17672[(2)] = inst_14486);

(statearr_14508_17672[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (20))){
var state_14492__$1 = state_14492;
var statearr_14514_17675 = state_14492__$1;
(statearr_14514_17675[(2)] = null);

(statearr_14514_17675[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (1))){
var state_14492__$1 = state_14492;
var statearr_14524_17676 = state_14492__$1;
(statearr_14524_17676[(2)] = null);

(statearr_14524_17676[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (4))){
var inst_14447 = (state_14492[(7)]);
var inst_14447__$1 = (state_14492[(2)]);
var inst_14450 = (inst_14447__$1 == null);
var state_14492__$1 = (function (){var statearr_14533 = state_14492;
(statearr_14533[(7)] = inst_14447__$1);

return statearr_14533;
})();
if(cljs.core.truth_(inst_14450)){
var statearr_14534_17678 = state_14492__$1;
(statearr_14534_17678[(1)] = (5));

} else {
var statearr_14536_17680 = state_14492__$1;
(statearr_14536_17680[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (15))){
var inst_14465 = (state_14492[(8)]);
var state_14492__$1 = state_14492;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14492__$1,(18),to,inst_14465);
} else {
if((state_val_14493 === (21))){
var inst_14481 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
var statearr_14541_17685 = state_14492__$1;
(statearr_14541_17685[(2)] = inst_14481);

(statearr_14541_17685[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (13))){
var inst_14483 = (state_14492[(2)]);
var state_14492__$1 = (function (){var statearr_14543 = state_14492;
(statearr_14543[(9)] = inst_14483);

return statearr_14543;
})();
var statearr_14544_17688 = state_14492__$1;
(statearr_14544_17688[(2)] = null);

(statearr_14544_17688[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (6))){
var inst_14447 = (state_14492[(7)]);
var state_14492__$1 = state_14492;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14492__$1,(11),inst_14447);
} else {
if((state_val_14493 === (17))){
var inst_14475 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
if(cljs.core.truth_(inst_14475)){
var statearr_14550_17690 = state_14492__$1;
(statearr_14550_17690[(1)] = (19));

} else {
var statearr_14552_17691 = state_14492__$1;
(statearr_14552_17691[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (3))){
var inst_14488 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14492__$1,inst_14488);
} else {
if((state_val_14493 === (12))){
var inst_14459 = (state_14492[(10)]);
var state_14492__$1 = state_14492;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14492__$1,(14),inst_14459);
} else {
if((state_val_14493 === (2))){
var state_14492__$1 = state_14492;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14492__$1,(4),results);
} else {
if((state_val_14493 === (19))){
var state_14492__$1 = state_14492;
var statearr_14569_17693 = state_14492__$1;
(statearr_14569_17693[(2)] = null);

(statearr_14569_17693[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (11))){
var inst_14459 = (state_14492[(2)]);
var state_14492__$1 = (function (){var statearr_14571 = state_14492;
(statearr_14571[(10)] = inst_14459);

return statearr_14571;
})();
var statearr_14577_17694 = state_14492__$1;
(statearr_14577_17694[(2)] = null);

(statearr_14577_17694[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (9))){
var state_14492__$1 = state_14492;
var statearr_14585_17695 = state_14492__$1;
(statearr_14585_17695[(2)] = null);

(statearr_14585_17695[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (5))){
var state_14492__$1 = state_14492;
if(cljs.core.truth_(close_QMARK_)){
var statearr_14587_17697 = state_14492__$1;
(statearr_14587_17697[(1)] = (8));

} else {
var statearr_14592_17698 = state_14492__$1;
(statearr_14592_17698[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (14))){
var inst_14465 = (state_14492[(8)]);
var inst_14469 = (state_14492[(11)]);
var inst_14465__$1 = (state_14492[(2)]);
var inst_14468 = (inst_14465__$1 == null);
var inst_14469__$1 = cljs.core.not(inst_14468);
var state_14492__$1 = (function (){var statearr_14599 = state_14492;
(statearr_14599[(8)] = inst_14465__$1);

(statearr_14599[(11)] = inst_14469__$1);

return statearr_14599;
})();
if(inst_14469__$1){
var statearr_14601_17700 = state_14492__$1;
(statearr_14601_17700[(1)] = (15));

} else {
var statearr_14602_17702 = state_14492__$1;
(statearr_14602_17702[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (16))){
var inst_14469 = (state_14492[(11)]);
var state_14492__$1 = state_14492;
var statearr_14604_17703 = state_14492__$1;
(statearr_14604_17703[(2)] = inst_14469);

(statearr_14604_17703[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (10))){
var inst_14456 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
var statearr_14609_17704 = state_14492__$1;
(statearr_14609_17704[(2)] = inst_14456);

(statearr_14609_17704[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (18))){
var inst_14472 = (state_14492[(2)]);
var state_14492__$1 = state_14492;
var statearr_14611_17705 = state_14492__$1;
(statearr_14611_17705[(2)] = inst_14472);

(statearr_14611_17705[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14493 === (8))){
var inst_14453 = cljs.core.async.close_BANG_(to);
var state_14492__$1 = state_14492;
var statearr_14614_17706 = state_14492__$1;
(statearr_14614_17706[(2)] = inst_14453);

(statearr_14614_17706[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_14617 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_14617[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__);

(statearr_14617[(1)] = (1));

return statearr_14617;
});
var cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1 = (function (state_14492){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14492);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14618){var ex__12399__auto__ = e14618;
var statearr_14619_17711 = state_14492;
(statearr_14619_17711[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14492[(4)]))){
var statearr_14620_17712 = state_14492;
(statearr_14620_17712[(1)] = cljs.core.first((state_14492[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17714 = state_14492;
state_14492 = G__17714;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__ = function(state_14492){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1.call(this,state_14492);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14628 = f__13503__auto__();
(statearr_14628[(6)] = c__13502__auto__);

return statearr_14628;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

return c__13502__auto__;
});
/**
 * Takes elements from the from channel and supplies them to the to
 *   channel, subject to the async function af, with parallelism n. af
 *   must be a function of two arguments, the first an input value and
 *   the second a channel on which to place the result(s). The
 *   presumption is that af will return immediately, having launched some
 *   asynchronous operation whose completion/callback will put results on
 *   the channel, then close! it. Outputs will be returned in order
 *   relative to the inputs. By default, the to channel will be closed
 *   when the from channel closes, but can be determined by the close?
 *   parameter. Will stop consuming the from channel if the to channel
 *   closes. See also pipeline, pipeline-blocking.
 */
cljs.core.async.pipeline_async = (function cljs$core$async$pipeline_async(var_args){
var G__14636 = arguments.length;
switch (G__14636) {
case 4:
return cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$4 = (function (n,to,af,from){
return cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$5(n,to,af,from,true);
}));

(cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$5 = (function (n,to,af,from,close_QMARK_){
return cljs.core.async.pipeline_STAR_(n,to,af,from,close_QMARK_,null,new cljs.core.Keyword(null,"async","async",1050769601));
}));

(cljs.core.async.pipeline_async.cljs$lang$maxFixedArity = 5);

/**
 * Takes elements from the from channel and supplies them to the to
 *   channel, subject to the transducer xf, with parallelism n. Because
 *   it is parallel, the transducer will be applied independently to each
 *   element, not across elements, and may produce zero or more outputs
 *   per input.  Outputs will be returned in order relative to the
 *   inputs. By default, the to channel will be closed when the from
 *   channel closes, but can be determined by the close?  parameter. Will
 *   stop consuming the from channel if the to channel closes.
 * 
 *   Note this is supplied for API compatibility with the Clojure version.
 *   Values of N > 1 will not result in actual concurrency in a
 *   single-threaded runtime.
 */
cljs.core.async.pipeline = (function cljs$core$async$pipeline(var_args){
var G__14648 = arguments.length;
switch (G__14648) {
case 4:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$4 = (function (n,to,xf,from){
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$5(n,to,xf,from,true);
}));

(cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$5 = (function (n,to,xf,from,close_QMARK_){
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$6(n,to,xf,from,close_QMARK_,null);
}));

(cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$6 = (function (n,to,xf,from,close_QMARK_,ex_handler){
return cljs.core.async.pipeline_STAR_(n,to,xf,from,close_QMARK_,ex_handler,new cljs.core.Keyword(null,"compute","compute",1555393130));
}));

(cljs.core.async.pipeline.cljs$lang$maxFixedArity = 6);

/**
 * Takes a predicate and a source channel and returns a vector of two
 *   channels, the first of which will contain the values for which the
 *   predicate returned true, the second those for which it returned
 *   false.
 * 
 *   The out channels will be unbuffered by default, or two buf-or-ns can
 *   be supplied. The channels will close after the source channel has
 *   closed.
 */
cljs.core.async.split = (function cljs$core$async$split(var_args){
var G__14660 = arguments.length;
switch (G__14660) {
case 2:
return cljs.core.async.split.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return cljs.core.async.split.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.split.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.split.cljs$core$IFn$_invoke$arity$4(p,ch,null,null);
}));

(cljs.core.async.split.cljs$core$IFn$_invoke$arity$4 = (function (p,ch,t_buf_or_n,f_buf_or_n){
var tc = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(t_buf_or_n);
var fc = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(f_buf_or_n);
var c__13502__auto___17736 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14700){
var state_val_14701 = (state_14700[(1)]);
if((state_val_14701 === (7))){
var inst_14696 = (state_14700[(2)]);
var state_14700__$1 = state_14700;
var statearr_14708_17743 = state_14700__$1;
(statearr_14708_17743[(2)] = inst_14696);

(statearr_14708_17743[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (1))){
var state_14700__$1 = state_14700;
var statearr_14709_17744 = state_14700__$1;
(statearr_14709_17744[(2)] = null);

(statearr_14709_17744[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (4))){
var inst_14676 = (state_14700[(7)]);
var inst_14676__$1 = (state_14700[(2)]);
var inst_14677 = (inst_14676__$1 == null);
var state_14700__$1 = (function (){var statearr_14710 = state_14700;
(statearr_14710[(7)] = inst_14676__$1);

return statearr_14710;
})();
if(cljs.core.truth_(inst_14677)){
var statearr_14711_17745 = state_14700__$1;
(statearr_14711_17745[(1)] = (5));

} else {
var statearr_14712_17746 = state_14700__$1;
(statearr_14712_17746[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (13))){
var state_14700__$1 = state_14700;
var statearr_14715_17747 = state_14700__$1;
(statearr_14715_17747[(2)] = null);

(statearr_14715_17747[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (6))){
var inst_14676 = (state_14700[(7)]);
var inst_14683 = (p.cljs$core$IFn$_invoke$arity$1 ? p.cljs$core$IFn$_invoke$arity$1(inst_14676) : p.call(null,inst_14676));
var state_14700__$1 = state_14700;
if(cljs.core.truth_(inst_14683)){
var statearr_14716_17748 = state_14700__$1;
(statearr_14716_17748[(1)] = (9));

} else {
var statearr_14717_17749 = state_14700__$1;
(statearr_14717_17749[(1)] = (10));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (3))){
var inst_14698 = (state_14700[(2)]);
var state_14700__$1 = state_14700;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14700__$1,inst_14698);
} else {
if((state_val_14701 === (12))){
var state_14700__$1 = state_14700;
var statearr_14737_17750 = state_14700__$1;
(statearr_14737_17750[(2)] = null);

(statearr_14737_17750[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (2))){
var state_14700__$1 = state_14700;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14700__$1,(4),ch);
} else {
if((state_val_14701 === (11))){
var inst_14676 = (state_14700[(7)]);
var inst_14687 = (state_14700[(2)]);
var state_14700__$1 = state_14700;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14700__$1,(8),inst_14687,inst_14676);
} else {
if((state_val_14701 === (9))){
var state_14700__$1 = state_14700;
var statearr_14756_17751 = state_14700__$1;
(statearr_14756_17751[(2)] = tc);

(statearr_14756_17751[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (5))){
var inst_14679 = cljs.core.async.close_BANG_(tc);
var inst_14680 = cljs.core.async.close_BANG_(fc);
var state_14700__$1 = (function (){var statearr_14758 = state_14700;
(statearr_14758[(8)] = inst_14679);

return statearr_14758;
})();
var statearr_14759_17752 = state_14700__$1;
(statearr_14759_17752[(2)] = inst_14680);

(statearr_14759_17752[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (14))){
var inst_14694 = (state_14700[(2)]);
var state_14700__$1 = state_14700;
var statearr_14766_17753 = state_14700__$1;
(statearr_14766_17753[(2)] = inst_14694);

(statearr_14766_17753[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (10))){
var state_14700__$1 = state_14700;
var statearr_14767_17754 = state_14700__$1;
(statearr_14767_17754[(2)] = fc);

(statearr_14767_17754[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14701 === (8))){
var inst_14689 = (state_14700[(2)]);
var state_14700__$1 = state_14700;
if(cljs.core.truth_(inst_14689)){
var statearr_14771_17755 = state_14700__$1;
(statearr_14771_17755[(1)] = (12));

} else {
var statearr_14772_17756 = state_14700__$1;
(statearr_14772_17756[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_14773 = [null,null,null,null,null,null,null,null,null];
(statearr_14773[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_14773[(1)] = (1));

return statearr_14773;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_14700){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14700);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14774){var ex__12399__auto__ = e14774;
var statearr_14775_17761 = state_14700;
(statearr_14775_17761[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14700[(4)]))){
var statearr_14776_17762 = state_14700;
(statearr_14776_17762[(1)] = cljs.core.first((state_14700[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17766 = state_14700;
state_14700 = G__17766;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_14700){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_14700);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14778 = f__13503__auto__();
(statearr_14778[(6)] = c__13502__auto___17736);

return statearr_14778;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [tc,fc], null);
}));

(cljs.core.async.split.cljs$lang$maxFixedArity = 4);

/**
 * f should be a function of 2 arguments. Returns a channel containing
 *   the single result of applying f to init and the first item from the
 *   channel, then applying f to that result and the 2nd item, etc. If
 *   the channel closes without yielding items, returns init and f is not
 *   called. ch must close before reduce produces a result.
 */
cljs.core.async.reduce = (function cljs$core$async$reduce(f,init,ch){
var c__13502__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14805){
var state_val_14806 = (state_14805[(1)]);
if((state_val_14806 === (7))){
var inst_14801 = (state_14805[(2)]);
var state_14805__$1 = state_14805;
var statearr_14810_17773 = state_14805__$1;
(statearr_14810_17773[(2)] = inst_14801);

(statearr_14810_17773[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (1))){
var inst_14783 = init;
var inst_14785 = inst_14783;
var state_14805__$1 = (function (){var statearr_14811 = state_14805;
(statearr_14811[(7)] = inst_14785);

return statearr_14811;
})();
var statearr_14814_17775 = state_14805__$1;
(statearr_14814_17775[(2)] = null);

(statearr_14814_17775[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (4))){
var inst_14788 = (state_14805[(8)]);
var inst_14788__$1 = (state_14805[(2)]);
var inst_14789 = (inst_14788__$1 == null);
var state_14805__$1 = (function (){var statearr_14815 = state_14805;
(statearr_14815[(8)] = inst_14788__$1);

return statearr_14815;
})();
if(cljs.core.truth_(inst_14789)){
var statearr_14816_17781 = state_14805__$1;
(statearr_14816_17781[(1)] = (5));

} else {
var statearr_14817_17782 = state_14805__$1;
(statearr_14817_17782[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (6))){
var inst_14785 = (state_14805[(7)]);
var inst_14788 = (state_14805[(8)]);
var inst_14792 = (state_14805[(9)]);
var inst_14792__$1 = (f.cljs$core$IFn$_invoke$arity$2 ? f.cljs$core$IFn$_invoke$arity$2(inst_14785,inst_14788) : f.call(null,inst_14785,inst_14788));
var inst_14793 = cljs.core.reduced_QMARK_(inst_14792__$1);
var state_14805__$1 = (function (){var statearr_14822 = state_14805;
(statearr_14822[(9)] = inst_14792__$1);

return statearr_14822;
})();
if(inst_14793){
var statearr_14823_17787 = state_14805__$1;
(statearr_14823_17787[(1)] = (8));

} else {
var statearr_14824_17788 = state_14805__$1;
(statearr_14824_17788[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (3))){
var inst_14803 = (state_14805[(2)]);
var state_14805__$1 = state_14805;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14805__$1,inst_14803);
} else {
if((state_val_14806 === (2))){
var state_14805__$1 = state_14805;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14805__$1,(4),ch);
} else {
if((state_val_14806 === (9))){
var inst_14792 = (state_14805[(9)]);
var inst_14785 = inst_14792;
var state_14805__$1 = (function (){var statearr_14831 = state_14805;
(statearr_14831[(7)] = inst_14785);

return statearr_14831;
})();
var statearr_14832_17792 = state_14805__$1;
(statearr_14832_17792[(2)] = null);

(statearr_14832_17792[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (5))){
var inst_14785 = (state_14805[(7)]);
var state_14805__$1 = state_14805;
var statearr_14834_17793 = state_14805__$1;
(statearr_14834_17793[(2)] = inst_14785);

(statearr_14834_17793[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (10))){
var inst_14799 = (state_14805[(2)]);
var state_14805__$1 = state_14805;
var statearr_14835_17794 = state_14805__$1;
(statearr_14835_17794[(2)] = inst_14799);

(statearr_14835_17794[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14806 === (8))){
var inst_14792 = (state_14805[(9)]);
var inst_14795 = cljs.core.deref(inst_14792);
var state_14805__$1 = state_14805;
var statearr_14842_17795 = state_14805__$1;
(statearr_14842_17795[(2)] = inst_14795);

(statearr_14842_17795[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$reduce_$_state_machine__12396__auto__ = null;
var cljs$core$async$reduce_$_state_machine__12396__auto____0 = (function (){
var statearr_14850 = [null,null,null,null,null,null,null,null,null,null];
(statearr_14850[(0)] = cljs$core$async$reduce_$_state_machine__12396__auto__);

(statearr_14850[(1)] = (1));

return statearr_14850;
});
var cljs$core$async$reduce_$_state_machine__12396__auto____1 = (function (state_14805){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14805);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14852){var ex__12399__auto__ = e14852;
var statearr_14854_17796 = state_14805;
(statearr_14854_17796[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14805[(4)]))){
var statearr_14858_17797 = state_14805;
(statearr_14858_17797[(1)] = cljs.core.first((state_14805[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17798 = state_14805;
state_14805 = G__17798;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$reduce_$_state_machine__12396__auto__ = function(state_14805){
switch(arguments.length){
case 0:
return cljs$core$async$reduce_$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$reduce_$_state_machine__12396__auto____1.call(this,state_14805);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$reduce_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$reduce_$_state_machine__12396__auto____0;
cljs$core$async$reduce_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$reduce_$_state_machine__12396__auto____1;
return cljs$core$async$reduce_$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14864 = f__13503__auto__();
(statearr_14864[(6)] = c__13502__auto__);

return statearr_14864;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

return c__13502__auto__;
});
/**
 * async/reduces a channel with a transformation (xform f).
 *   Returns a channel containing the result.  ch must close before
 *   transduce produces a result.
 */
cljs.core.async.transduce = (function cljs$core$async$transduce(xform,f,init,ch){
var f__$1 = (xform.cljs$core$IFn$_invoke$arity$1 ? xform.cljs$core$IFn$_invoke$arity$1(f) : xform.call(null,f));
var c__13502__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14884){
var state_val_14885 = (state_14884[(1)]);
if((state_val_14885 === (1))){
var inst_14877 = cljs.core.async.reduce(f__$1,init,ch);
var state_14884__$1 = state_14884;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_14884__$1,(2),inst_14877);
} else {
if((state_val_14885 === (2))){
var inst_14879 = (state_14884[(2)]);
var inst_14880 = (f__$1.cljs$core$IFn$_invoke$arity$1 ? f__$1.cljs$core$IFn$_invoke$arity$1(inst_14879) : f__$1.call(null,inst_14879));
var state_14884__$1 = state_14884;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14884__$1,inst_14880);
} else {
return null;
}
}
});
return (function() {
var cljs$core$async$transduce_$_state_machine__12396__auto__ = null;
var cljs$core$async$transduce_$_state_machine__12396__auto____0 = (function (){
var statearr_14894 = [null,null,null,null,null,null,null];
(statearr_14894[(0)] = cljs$core$async$transduce_$_state_machine__12396__auto__);

(statearr_14894[(1)] = (1));

return statearr_14894;
});
var cljs$core$async$transduce_$_state_machine__12396__auto____1 = (function (state_14884){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14884);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e14898){var ex__12399__auto__ = e14898;
var statearr_14899_17802 = state_14884;
(statearr_14899_17802[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14884[(4)]))){
var statearr_14900_17803 = state_14884;
(statearr_14900_17803[(1)] = cljs.core.first((state_14884[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17804 = state_14884;
state_14884 = G__17804;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$transduce_$_state_machine__12396__auto__ = function(state_14884){
switch(arguments.length){
case 0:
return cljs$core$async$transduce_$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$transduce_$_state_machine__12396__auto____1.call(this,state_14884);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$transduce_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$transduce_$_state_machine__12396__auto____0;
cljs$core$async$transduce_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$transduce_$_state_machine__12396__auto____1;
return cljs$core$async$transduce_$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_14903 = f__13503__auto__();
(statearr_14903[(6)] = c__13502__auto__);

return statearr_14903;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

return c__13502__auto__;
});
/**
 * Puts the contents of coll into the supplied channel.
 * 
 *   By default the channel will be closed after the items are copied,
 *   but can be determined by the close? parameter.
 * 
 *   Returns a channel which will close after the items are copied.
 */
cljs.core.async.onto_chan_BANG_ = (function cljs$core$async$onto_chan_BANG_(var_args){
var G__14905 = arguments.length;
switch (G__14905) {
case 2:
return cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (ch,coll){
return cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$3(ch,coll,true);
}));

(cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (ch,coll,close_QMARK_){
var c__13502__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_14950){
var state_val_14951 = (state_14950[(1)]);
if((state_val_14951 === (7))){
var inst_14912 = (state_14950[(2)]);
var state_14950__$1 = state_14950;
var statearr_14956_17809 = state_14950__$1;
(statearr_14956_17809[(2)] = inst_14912);

(statearr_14956_17809[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (1))){
var inst_14906 = cljs.core.seq(coll);
var inst_14907 = inst_14906;
var state_14950__$1 = (function (){var statearr_14959 = state_14950;
(statearr_14959[(7)] = inst_14907);

return statearr_14959;
})();
var statearr_14960_17810 = state_14950__$1;
(statearr_14960_17810[(2)] = null);

(statearr_14960_17810[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (4))){
var inst_14907 = (state_14950[(7)]);
var inst_14910 = cljs.core.first(inst_14907);
var state_14950__$1 = state_14950;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_14950__$1,(7),ch,inst_14910);
} else {
if((state_val_14951 === (13))){
var inst_14944 = (state_14950[(2)]);
var state_14950__$1 = state_14950;
var statearr_14963_17811 = state_14950__$1;
(statearr_14963_17811[(2)] = inst_14944);

(statearr_14963_17811[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (6))){
var inst_14915 = (state_14950[(2)]);
var state_14950__$1 = state_14950;
if(cljs.core.truth_(inst_14915)){
var statearr_14969_17812 = state_14950__$1;
(statearr_14969_17812[(1)] = (8));

} else {
var statearr_14970_17813 = state_14950__$1;
(statearr_14970_17813[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (3))){
var inst_14948 = (state_14950[(2)]);
var state_14950__$1 = state_14950;
return cljs.core.async.impl.ioc_helpers.return_chan(state_14950__$1,inst_14948);
} else {
if((state_val_14951 === (12))){
var state_14950__$1 = state_14950;
var statearr_14982_17814 = state_14950__$1;
(statearr_14982_17814[(2)] = null);

(statearr_14982_17814[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (2))){
var inst_14907 = (state_14950[(7)]);
var state_14950__$1 = state_14950;
if(cljs.core.truth_(inst_14907)){
var statearr_14985_17815 = state_14950__$1;
(statearr_14985_17815[(1)] = (4));

} else {
var statearr_14986_17818 = state_14950__$1;
(statearr_14986_17818[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (11))){
var inst_14941 = cljs.core.async.close_BANG_(ch);
var state_14950__$1 = state_14950;
var statearr_14989_17825 = state_14950__$1;
(statearr_14989_17825[(2)] = inst_14941);

(statearr_14989_17825[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (9))){
var state_14950__$1 = state_14950;
if(cljs.core.truth_(close_QMARK_)){
var statearr_14990_17828 = state_14950__$1;
(statearr_14990_17828[(1)] = (11));

} else {
var statearr_14993_17829 = state_14950__$1;
(statearr_14993_17829[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (5))){
var inst_14907 = (state_14950[(7)]);
var state_14950__$1 = state_14950;
var statearr_15002_17834 = state_14950__$1;
(statearr_15002_17834[(2)] = inst_14907);

(statearr_15002_17834[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (10))){
var inst_14946 = (state_14950[(2)]);
var state_14950__$1 = state_14950;
var statearr_15003_17839 = state_14950__$1;
(statearr_15003_17839[(2)] = inst_14946);

(statearr_15003_17839[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_14951 === (8))){
var inst_14907 = (state_14950[(7)]);
var inst_14937 = cljs.core.next(inst_14907);
var inst_14907__$1 = inst_14937;
var state_14950__$1 = (function (){var statearr_15004 = state_14950;
(statearr_15004[(7)] = inst_14907__$1);

return statearr_15004;
})();
var statearr_15008_17840 = state_14950__$1;
(statearr_15008_17840[(2)] = null);

(statearr_15008_17840[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_15009 = [null,null,null,null,null,null,null,null];
(statearr_15009[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_15009[(1)] = (1));

return statearr_15009;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_14950){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_14950);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e15010){var ex__12399__auto__ = e15010;
var statearr_15011_17845 = state_14950;
(statearr_15011_17845[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_14950[(4)]))){
var statearr_15013_17846 = state_14950;
(statearr_15013_17846[(1)] = cljs.core.first((state_14950[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__17847 = state_14950;
state_14950 = G__17847;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_14950){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_14950);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_15015 = f__13503__auto__();
(statearr_15015[(6)] = c__13502__auto__);

return statearr_15015;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

return c__13502__auto__;
}));

(cljs.core.async.onto_chan_BANG_.cljs$lang$maxFixedArity = 3);

/**
 * Creates and returns a channel which contains the contents of coll,
 *   closing when exhausted.
 */
cljs.core.async.to_chan_BANG_ = (function cljs$core$async$to_chan_BANG_(coll){
var ch = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(cljs.core.bounded_count((100),coll));
cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$2(ch,coll);

return ch;
});
/**
 * Deprecated - use onto-chan!
 */
cljs.core.async.onto_chan = (function cljs$core$async$onto_chan(var_args){
var G__15024 = arguments.length;
switch (G__15024) {
case 2:
return cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$2 = (function (ch,coll){
return cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$3(ch,coll,true);
}));

(cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$3 = (function (ch,coll,close_QMARK_){
return cljs.core.async.onto_chan_BANG_.cljs$core$IFn$_invoke$arity$3(ch,coll,close_QMARK_);
}));

(cljs.core.async.onto_chan.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - use to-chan!
 */
cljs.core.async.to_chan = (function cljs$core$async$to_chan(coll){
return cljs.core.async.to_chan_BANG_(coll);
});

/**
 * @interface
 */
cljs.core.async.Mux = function(){};

var cljs$core$async$Mux$muxch_STAR_$dyn_17856 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (cljs.core.async.muxch_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(_) : m__5499__auto__.call(null,_));
} else {
var m__5497__auto__ = (cljs.core.async.muxch_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(_) : m__5497__auto__.call(null,_));
} else {
throw cljs.core.missing_protocol("Mux.muxch*",_);
}
}
});
cljs.core.async.muxch_STAR_ = (function cljs$core$async$muxch_STAR_(_){
if((((!((_ == null)))) && ((!((_.cljs$core$async$Mux$muxch_STAR_$arity$1 == null)))))){
return _.cljs$core$async$Mux$muxch_STAR_$arity$1(_);
} else {
return cljs$core$async$Mux$muxch_STAR_$dyn_17856(_);
}
});


/**
 * @interface
 */
cljs.core.async.Mult = function(){};

var cljs$core$async$Mult$tap_STAR_$dyn_17864 = (function (m,ch,close_QMARK_){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.tap_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(m,ch,close_QMARK_) : m__5499__auto__.call(null,m,ch,close_QMARK_));
} else {
var m__5497__auto__ = (cljs.core.async.tap_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(m,ch,close_QMARK_) : m__5497__auto__.call(null,m,ch,close_QMARK_));
} else {
throw cljs.core.missing_protocol("Mult.tap*",m);
}
}
});
cljs.core.async.tap_STAR_ = (function cljs$core$async$tap_STAR_(m,ch,close_QMARK_){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mult$tap_STAR_$arity$3 == null)))))){
return m.cljs$core$async$Mult$tap_STAR_$arity$3(m,ch,close_QMARK_);
} else {
return cljs$core$async$Mult$tap_STAR_$dyn_17864(m,ch,close_QMARK_);
}
});

var cljs$core$async$Mult$untap_STAR_$dyn_17866 = (function (m,ch){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.untap_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5499__auto__.call(null,m,ch));
} else {
var m__5497__auto__ = (cljs.core.async.untap_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5497__auto__.call(null,m,ch));
} else {
throw cljs.core.missing_protocol("Mult.untap*",m);
}
}
});
cljs.core.async.untap_STAR_ = (function cljs$core$async$untap_STAR_(m,ch){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mult$untap_STAR_$arity$2 == null)))))){
return m.cljs$core$async$Mult$untap_STAR_$arity$2(m,ch);
} else {
return cljs$core$async$Mult$untap_STAR_$dyn_17866(m,ch);
}
});

var cljs$core$async$Mult$untap_all_STAR_$dyn_17867 = (function (m){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.untap_all_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(m) : m__5499__auto__.call(null,m));
} else {
var m__5497__auto__ = (cljs.core.async.untap_all_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(m) : m__5497__auto__.call(null,m));
} else {
throw cljs.core.missing_protocol("Mult.untap-all*",m);
}
}
});
cljs.core.async.untap_all_STAR_ = (function cljs$core$async$untap_all_STAR_(m){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mult$untap_all_STAR_$arity$1 == null)))))){
return m.cljs$core$async$Mult$untap_all_STAR_$arity$1(m);
} else {
return cljs$core$async$Mult$untap_all_STAR_$dyn_17867(m);
}
});


/**
* @constructor
 * @implements {cljs.core.async.Mult}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.async.Mux}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async15071 = (function (ch,cs,meta15072){
this.ch = ch;
this.cs = cs;
this.meta15072 = meta15072;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_15073,meta15072__$1){
var self__ = this;
var _15073__$1 = this;
return (new cljs.core.async.t_cljs$core$async15071(self__.ch,self__.cs,meta15072__$1));
}));

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_15073){
var self__ = this;
var _15073__$1 = this;
return self__.meta15072;
}));

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
}));

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mult$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mult$tap_STAR_$arity$3 = (function (_,ch__$1,close_QMARK_){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(self__.cs,cljs.core.assoc,ch__$1,close_QMARK_);

return null;
}));

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mult$untap_STAR_$arity$2 = (function (_,ch__$1){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.dissoc,ch__$1);

return null;
}));

(cljs.core.async.t_cljs$core$async15071.prototype.cljs$core$async$Mult$untap_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return null;
}));

(cljs.core.async.t_cljs$core$async15071.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"meta15072","meta15072",-1261564192,null)], null);
}));

(cljs.core.async.t_cljs$core$async15071.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async15071.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async15071");

(cljs.core.async.t_cljs$core$async15071.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async15071");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async15071.
 */
cljs.core.async.__GT_t_cljs$core$async15071 = (function cljs$core$async$__GT_t_cljs$core$async15071(ch,cs,meta15072){
return (new cljs.core.async.t_cljs$core$async15071(ch,cs,meta15072));
});


/**
 * Creates and returns a mult(iple) of the supplied channel. Channels
 *   containing copies of the channel can be created with 'tap', and
 *   detached with 'untap'.
 * 
 *   Each item is distributed to all taps in parallel and synchronously,
 *   i.e. each tap must accept before the next item is distributed. Use
 *   buffering/windowing to prevent slow taps from holding up the mult.
 * 
 *   Items received when there are no taps get dropped.
 * 
 *   If a tap puts to a closed channel, it will be removed from the mult.
 */
cljs.core.async.mult = (function cljs$core$async$mult(ch){
var cs = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var m = (new cljs.core.async.t_cljs$core$async15071(ch,cs,cljs.core.PersistentArrayMap.EMPTY));
var dchan = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
var dctr = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var done = (function (_){
if((cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(dctr,cljs.core.dec) === (0))){
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(dchan,true);
} else {
return null;
}
});
var c__13502__auto___17882 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_15331){
var state_val_15332 = (state_15331[(1)]);
if((state_val_15332 === (7))){
var inst_15318 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15338_17883 = state_15331__$1;
(statearr_15338_17883[(2)] = inst_15318);

(statearr_15338_17883[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (20))){
var inst_15168 = (state_15331[(7)]);
var inst_15184 = cljs.core.first(inst_15168);
var inst_15185 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15184,(0),null);
var inst_15187 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15184,(1),null);
var state_15331__$1 = (function (){var statearr_15340 = state_15331;
(statearr_15340[(8)] = inst_15185);

return statearr_15340;
})();
if(cljs.core.truth_(inst_15187)){
var statearr_15341_17890 = state_15331__$1;
(statearr_15341_17890[(1)] = (22));

} else {
var statearr_15342_17891 = state_15331__$1;
(statearr_15342_17891[(1)] = (23));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (27))){
var inst_15225 = (state_15331[(9)]);
var inst_15227 = (state_15331[(10)]);
var inst_15242 = (state_15331[(11)]);
var inst_15125 = (state_15331[(12)]);
var inst_15242__$1 = cljs.core._nth(inst_15225,inst_15227);
var inst_15243 = cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3(inst_15242__$1,inst_15125,done);
var state_15331__$1 = (function (){var statearr_15343 = state_15331;
(statearr_15343[(11)] = inst_15242__$1);

return statearr_15343;
})();
if(cljs.core.truth_(inst_15243)){
var statearr_15344_17894 = state_15331__$1;
(statearr_15344_17894[(1)] = (30));

} else {
var statearr_15345_17897 = state_15331__$1;
(statearr_15345_17897[(1)] = (31));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (1))){
var state_15331__$1 = state_15331;
var statearr_15348_17907 = state_15331__$1;
(statearr_15348_17907[(2)] = null);

(statearr_15348_17907[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (24))){
var inst_15168 = (state_15331[(7)]);
var inst_15192 = (state_15331[(2)]);
var inst_15195 = cljs.core.next(inst_15168);
var inst_15139 = inst_15195;
var inst_15140 = null;
var inst_15141 = (0);
var inst_15142 = (0);
var state_15331__$1 = (function (){var statearr_15352 = state_15331;
(statearr_15352[(13)] = inst_15192);

(statearr_15352[(14)] = inst_15139);

(statearr_15352[(15)] = inst_15140);

(statearr_15352[(16)] = inst_15141);

(statearr_15352[(17)] = inst_15142);

return statearr_15352;
})();
var statearr_15358_17912 = state_15331__$1;
(statearr_15358_17912[(2)] = null);

(statearr_15358_17912[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (39))){
var state_15331__$1 = state_15331;
var statearr_15370_17913 = state_15331__$1;
(statearr_15370_17913[(2)] = null);

(statearr_15370_17913[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (4))){
var inst_15125 = (state_15331[(12)]);
var inst_15125__$1 = (state_15331[(2)]);
var inst_15126 = (inst_15125__$1 == null);
var state_15331__$1 = (function (){var statearr_15385 = state_15331;
(statearr_15385[(12)] = inst_15125__$1);

return statearr_15385;
})();
if(cljs.core.truth_(inst_15126)){
var statearr_15386_17918 = state_15331__$1;
(statearr_15386_17918[(1)] = (5));

} else {
var statearr_15389_17919 = state_15331__$1;
(statearr_15389_17919[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (15))){
var inst_15142 = (state_15331[(17)]);
var inst_15139 = (state_15331[(14)]);
var inst_15140 = (state_15331[(15)]);
var inst_15141 = (state_15331[(16)]);
var inst_15157 = (state_15331[(2)]);
var inst_15158 = (inst_15142 + (1));
var tmp15366 = inst_15140;
var tmp15367 = inst_15139;
var tmp15368 = inst_15141;
var inst_15139__$1 = tmp15367;
var inst_15140__$1 = tmp15366;
var inst_15141__$1 = tmp15368;
var inst_15142__$1 = inst_15158;
var state_15331__$1 = (function (){var statearr_15392 = state_15331;
(statearr_15392[(18)] = inst_15157);

(statearr_15392[(14)] = inst_15139__$1);

(statearr_15392[(15)] = inst_15140__$1);

(statearr_15392[(16)] = inst_15141__$1);

(statearr_15392[(17)] = inst_15142__$1);

return statearr_15392;
})();
var statearr_15394_17927 = state_15331__$1;
(statearr_15394_17927[(2)] = null);

(statearr_15394_17927[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (21))){
var inst_15198 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15406_17928 = state_15331__$1;
(statearr_15406_17928[(2)] = inst_15198);

(statearr_15406_17928[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (31))){
var inst_15242 = (state_15331[(11)]);
var inst_15247 = m.cljs$core$async$Mult$untap_STAR_$arity$2(null,inst_15242);
var state_15331__$1 = state_15331;
var statearr_15408_17933 = state_15331__$1;
(statearr_15408_17933[(2)] = inst_15247);

(statearr_15408_17933[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (32))){
var inst_15227 = (state_15331[(10)]);
var inst_15224 = (state_15331[(19)]);
var inst_15225 = (state_15331[(9)]);
var inst_15226 = (state_15331[(20)]);
var inst_15249 = (state_15331[(2)]);
var inst_15250 = (inst_15227 + (1));
var tmp15397 = inst_15224;
var tmp15398 = inst_15225;
var tmp15399 = inst_15226;
var inst_15224__$1 = tmp15397;
var inst_15225__$1 = tmp15398;
var inst_15226__$1 = tmp15399;
var inst_15227__$1 = inst_15250;
var state_15331__$1 = (function (){var statearr_15409 = state_15331;
(statearr_15409[(21)] = inst_15249);

(statearr_15409[(19)] = inst_15224__$1);

(statearr_15409[(9)] = inst_15225__$1);

(statearr_15409[(20)] = inst_15226__$1);

(statearr_15409[(10)] = inst_15227__$1);

return statearr_15409;
})();
var statearr_15410_17936 = state_15331__$1;
(statearr_15410_17936[(2)] = null);

(statearr_15410_17936[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (40))){
var inst_15282 = (state_15331[(22)]);
var inst_15288 = m.cljs$core$async$Mult$untap_STAR_$arity$2(null,inst_15282);
var state_15331__$1 = state_15331;
var statearr_15414_17937 = state_15331__$1;
(statearr_15414_17937[(2)] = inst_15288);

(statearr_15414_17937[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (33))){
var inst_15255 = (state_15331[(23)]);
var inst_15264 = cljs.core.chunked_seq_QMARK_(inst_15255);
var state_15331__$1 = state_15331;
if(inst_15264){
var statearr_15422_17939 = state_15331__$1;
(statearr_15422_17939[(1)] = (36));

} else {
var statearr_15424_17941 = state_15331__$1;
(statearr_15424_17941[(1)] = (37));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (13))){
var inst_15151 = (state_15331[(24)]);
var inst_15154 = cljs.core.async.close_BANG_(inst_15151);
var state_15331__$1 = state_15331;
var statearr_15425_17944 = state_15331__$1;
(statearr_15425_17944[(2)] = inst_15154);

(statearr_15425_17944[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (22))){
var inst_15185 = (state_15331[(8)]);
var inst_15189 = cljs.core.async.close_BANG_(inst_15185);
var state_15331__$1 = state_15331;
var statearr_15434_17945 = state_15331__$1;
(statearr_15434_17945[(2)] = inst_15189);

(statearr_15434_17945[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (36))){
var inst_15255 = (state_15331[(23)]);
var inst_15266 = cljs.core.chunk_first(inst_15255);
var inst_15270 = cljs.core.chunk_rest(inst_15255);
var inst_15271 = cljs.core.count(inst_15266);
var inst_15224 = inst_15270;
var inst_15225 = inst_15266;
var inst_15226 = inst_15271;
var inst_15227 = (0);
var state_15331__$1 = (function (){var statearr_15443 = state_15331;
(statearr_15443[(19)] = inst_15224);

(statearr_15443[(9)] = inst_15225);

(statearr_15443[(20)] = inst_15226);

(statearr_15443[(10)] = inst_15227);

return statearr_15443;
})();
var statearr_15446_17948 = state_15331__$1;
(statearr_15446_17948[(2)] = null);

(statearr_15446_17948[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (41))){
var inst_15255 = (state_15331[(23)]);
var inst_15293 = (state_15331[(2)]);
var inst_15297 = cljs.core.next(inst_15255);
var inst_15224 = inst_15297;
var inst_15225 = null;
var inst_15226 = (0);
var inst_15227 = (0);
var state_15331__$1 = (function (){var statearr_15468 = state_15331;
(statearr_15468[(25)] = inst_15293);

(statearr_15468[(19)] = inst_15224);

(statearr_15468[(9)] = inst_15225);

(statearr_15468[(20)] = inst_15226);

(statearr_15468[(10)] = inst_15227);

return statearr_15468;
})();
var statearr_15472_17949 = state_15331__$1;
(statearr_15472_17949[(2)] = null);

(statearr_15472_17949[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (43))){
var state_15331__$1 = state_15331;
var statearr_15476_17950 = state_15331__$1;
(statearr_15476_17950[(2)] = null);

(statearr_15476_17950[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (29))){
var inst_15305 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15487_17951 = state_15331__$1;
(statearr_15487_17951[(2)] = inst_15305);

(statearr_15487_17951[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (44))){
var inst_15314 = (state_15331[(2)]);
var state_15331__$1 = (function (){var statearr_15504 = state_15331;
(statearr_15504[(26)] = inst_15314);

return statearr_15504;
})();
var statearr_15506_17953 = state_15331__$1;
(statearr_15506_17953[(2)] = null);

(statearr_15506_17953[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (6))){
var inst_15216 = (state_15331[(27)]);
var inst_15215 = cljs.core.deref(cs);
var inst_15216__$1 = cljs.core.keys(inst_15215);
var inst_15217 = cljs.core.count(inst_15216__$1);
var inst_15218 = cljs.core.reset_BANG_(dctr,inst_15217);
var inst_15223 = cljs.core.seq(inst_15216__$1);
var inst_15224 = inst_15223;
var inst_15225 = null;
var inst_15226 = (0);
var inst_15227 = (0);
var state_15331__$1 = (function (){var statearr_15519 = state_15331;
(statearr_15519[(27)] = inst_15216__$1);

(statearr_15519[(28)] = inst_15218);

(statearr_15519[(19)] = inst_15224);

(statearr_15519[(9)] = inst_15225);

(statearr_15519[(20)] = inst_15226);

(statearr_15519[(10)] = inst_15227);

return statearr_15519;
})();
var statearr_15521_17955 = state_15331__$1;
(statearr_15521_17955[(2)] = null);

(statearr_15521_17955[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (28))){
var inst_15224 = (state_15331[(19)]);
var inst_15255 = (state_15331[(23)]);
var inst_15255__$1 = cljs.core.seq(inst_15224);
var state_15331__$1 = (function (){var statearr_15531 = state_15331;
(statearr_15531[(23)] = inst_15255__$1);

return statearr_15531;
})();
if(inst_15255__$1){
var statearr_15540_17956 = state_15331__$1;
(statearr_15540_17956[(1)] = (33));

} else {
var statearr_15543_17957 = state_15331__$1;
(statearr_15543_17957[(1)] = (34));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (25))){
var inst_15227 = (state_15331[(10)]);
var inst_15226 = (state_15331[(20)]);
var inst_15230 = (inst_15227 < inst_15226);
var inst_15231 = inst_15230;
var state_15331__$1 = state_15331;
if(cljs.core.truth_(inst_15231)){
var statearr_15554_17958 = state_15331__$1;
(statearr_15554_17958[(1)] = (27));

} else {
var statearr_15555_17959 = state_15331__$1;
(statearr_15555_17959[(1)] = (28));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (34))){
var state_15331__$1 = state_15331;
var statearr_15561_17960 = state_15331__$1;
(statearr_15561_17960[(2)] = null);

(statearr_15561_17960[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (17))){
var state_15331__$1 = state_15331;
var statearr_15562_17962 = state_15331__$1;
(statearr_15562_17962[(2)] = null);

(statearr_15562_17962[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (3))){
var inst_15320 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
return cljs.core.async.impl.ioc_helpers.return_chan(state_15331__$1,inst_15320);
} else {
if((state_val_15332 === (12))){
var inst_15207 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15563_17965 = state_15331__$1;
(statearr_15563_17965[(2)] = inst_15207);

(statearr_15563_17965[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (2))){
var state_15331__$1 = state_15331;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_15331__$1,(4),ch);
} else {
if((state_val_15332 === (23))){
var state_15331__$1 = state_15331;
var statearr_15568_17966 = state_15331__$1;
(statearr_15568_17966[(2)] = null);

(statearr_15568_17966[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (35))){
var inst_15303 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15578_17971 = state_15331__$1;
(statearr_15578_17971[(2)] = inst_15303);

(statearr_15578_17971[(1)] = (29));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (19))){
var inst_15168 = (state_15331[(7)]);
var inst_15176 = cljs.core.chunk_first(inst_15168);
var inst_15177 = cljs.core.chunk_rest(inst_15168);
var inst_15178 = cljs.core.count(inst_15176);
var inst_15139 = inst_15177;
var inst_15140 = inst_15176;
var inst_15141 = inst_15178;
var inst_15142 = (0);
var state_15331__$1 = (function (){var statearr_15580 = state_15331;
(statearr_15580[(14)] = inst_15139);

(statearr_15580[(15)] = inst_15140);

(statearr_15580[(16)] = inst_15141);

(statearr_15580[(17)] = inst_15142);

return statearr_15580;
})();
var statearr_15581_17975 = state_15331__$1;
(statearr_15581_17975[(2)] = null);

(statearr_15581_17975[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (11))){
var inst_15139 = (state_15331[(14)]);
var inst_15168 = (state_15331[(7)]);
var inst_15168__$1 = cljs.core.seq(inst_15139);
var state_15331__$1 = (function (){var statearr_15585 = state_15331;
(statearr_15585[(7)] = inst_15168__$1);

return statearr_15585;
})();
if(inst_15168__$1){
var statearr_15586_17979 = state_15331__$1;
(statearr_15586_17979[(1)] = (16));

} else {
var statearr_15588_17981 = state_15331__$1;
(statearr_15588_17981[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (9))){
var inst_15209 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15589_17982 = state_15331__$1;
(statearr_15589_17982[(2)] = inst_15209);

(statearr_15589_17982[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (5))){
var inst_15135 = cljs.core.deref(cs);
var inst_15136 = cljs.core.seq(inst_15135);
var inst_15139 = inst_15136;
var inst_15140 = null;
var inst_15141 = (0);
var inst_15142 = (0);
var state_15331__$1 = (function (){var statearr_15591 = state_15331;
(statearr_15591[(14)] = inst_15139);

(statearr_15591[(15)] = inst_15140);

(statearr_15591[(16)] = inst_15141);

(statearr_15591[(17)] = inst_15142);

return statearr_15591;
})();
var statearr_15592_17983 = state_15331__$1;
(statearr_15592_17983[(2)] = null);

(statearr_15592_17983[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (14))){
var state_15331__$1 = state_15331;
var statearr_15593_17985 = state_15331__$1;
(statearr_15593_17985[(2)] = null);

(statearr_15593_17985[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (45))){
var inst_15311 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15599_17986 = state_15331__$1;
(statearr_15599_17986[(2)] = inst_15311);

(statearr_15599_17986[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (26))){
var inst_15216 = (state_15331[(27)]);
var inst_15307 = (state_15331[(2)]);
var inst_15308 = cljs.core.seq(inst_15216);
var state_15331__$1 = (function (){var statearr_15600 = state_15331;
(statearr_15600[(29)] = inst_15307);

return statearr_15600;
})();
if(inst_15308){
var statearr_15602_17989 = state_15331__$1;
(statearr_15602_17989[(1)] = (42));

} else {
var statearr_15603_17990 = state_15331__$1;
(statearr_15603_17990[(1)] = (43));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (16))){
var inst_15168 = (state_15331[(7)]);
var inst_15172 = cljs.core.chunked_seq_QMARK_(inst_15168);
var state_15331__$1 = state_15331;
if(inst_15172){
var statearr_15605_17994 = state_15331__$1;
(statearr_15605_17994[(1)] = (19));

} else {
var statearr_15606_17995 = state_15331__$1;
(statearr_15606_17995[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (38))){
var inst_15300 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15607_17996 = state_15331__$1;
(statearr_15607_17996[(2)] = inst_15300);

(statearr_15607_17996[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (30))){
var state_15331__$1 = state_15331;
var statearr_15608_17997 = state_15331__$1;
(statearr_15608_17997[(2)] = null);

(statearr_15608_17997[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (10))){
var inst_15140 = (state_15331[(15)]);
var inst_15142 = (state_15331[(17)]);
var inst_15150 = cljs.core._nth(inst_15140,inst_15142);
var inst_15151 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15150,(0),null);
var inst_15152 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15150,(1),null);
var state_15331__$1 = (function (){var statearr_15611 = state_15331;
(statearr_15611[(24)] = inst_15151);

return statearr_15611;
})();
if(cljs.core.truth_(inst_15152)){
var statearr_15614_17998 = state_15331__$1;
(statearr_15614_17998[(1)] = (13));

} else {
var statearr_15615_17999 = state_15331__$1;
(statearr_15615_17999[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (18))){
var inst_15204 = (state_15331[(2)]);
var state_15331__$1 = state_15331;
var statearr_15616_18000 = state_15331__$1;
(statearr_15616_18000[(2)] = inst_15204);

(statearr_15616_18000[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (42))){
var state_15331__$1 = state_15331;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_15331__$1,(45),dchan);
} else {
if((state_val_15332 === (37))){
var inst_15255 = (state_15331[(23)]);
var inst_15282 = (state_15331[(22)]);
var inst_15125 = (state_15331[(12)]);
var inst_15282__$1 = cljs.core.first(inst_15255);
var inst_15284 = cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3(inst_15282__$1,inst_15125,done);
var state_15331__$1 = (function (){var statearr_15617 = state_15331;
(statearr_15617[(22)] = inst_15282__$1);

return statearr_15617;
})();
if(cljs.core.truth_(inst_15284)){
var statearr_15619_18001 = state_15331__$1;
(statearr_15619_18001[(1)] = (39));

} else {
var statearr_15620_18002 = state_15331__$1;
(statearr_15620_18002[(1)] = (40));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15332 === (8))){
var inst_15142 = (state_15331[(17)]);
var inst_15141 = (state_15331[(16)]);
var inst_15144 = (inst_15142 < inst_15141);
var inst_15145 = inst_15144;
var state_15331__$1 = state_15331;
if(cljs.core.truth_(inst_15145)){
var statearr_15621_18003 = state_15331__$1;
(statearr_15621_18003[(1)] = (10));

} else {
var statearr_15624_18004 = state_15331__$1;
(statearr_15624_18004[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$mult_$_state_machine__12396__auto__ = null;
var cljs$core$async$mult_$_state_machine__12396__auto____0 = (function (){
var statearr_15640 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_15640[(0)] = cljs$core$async$mult_$_state_machine__12396__auto__);

(statearr_15640[(1)] = (1));

return statearr_15640;
});
var cljs$core$async$mult_$_state_machine__12396__auto____1 = (function (state_15331){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_15331);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e15646){var ex__12399__auto__ = e15646;
var statearr_15647_18005 = state_15331;
(statearr_15647_18005[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_15331[(4)]))){
var statearr_15648_18006 = state_15331;
(statearr_15648_18006[(1)] = cljs.core.first((state_15331[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18007 = state_15331;
state_15331 = G__18007;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$mult_$_state_machine__12396__auto__ = function(state_15331){
switch(arguments.length){
case 0:
return cljs$core$async$mult_$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$mult_$_state_machine__12396__auto____1.call(this,state_15331);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mult_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mult_$_state_machine__12396__auto____0;
cljs$core$async$mult_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mult_$_state_machine__12396__auto____1;
return cljs$core$async$mult_$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_15650 = f__13503__auto__();
(statearr_15650[(6)] = c__13502__auto___17882);

return statearr_15650;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return m;
});
/**
 * Copies the mult source onto the supplied channel.
 * 
 *   By default the channel will be closed when the source closes,
 *   but can be determined by the close? parameter.
 */
cljs.core.async.tap = (function cljs$core$async$tap(var_args){
var G__15656 = arguments.length;
switch (G__15656) {
case 2:
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.tap.cljs$core$IFn$_invoke$arity$2 = (function (mult,ch){
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3(mult,ch,true);
}));

(cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3 = (function (mult,ch,close_QMARK_){
cljs.core.async.tap_STAR_(mult,ch,close_QMARK_);

return ch;
}));

(cljs.core.async.tap.cljs$lang$maxFixedArity = 3);

/**
 * Disconnects a target channel from a mult
 */
cljs.core.async.untap = (function cljs$core$async$untap(mult,ch){
return cljs.core.async.untap_STAR_(mult,ch);
});
/**
 * Disconnects all target channels from a mult
 */
cljs.core.async.untap_all = (function cljs$core$async$untap_all(mult){
return cljs.core.async.untap_all_STAR_(mult);
});

/**
 * @interface
 */
cljs.core.async.Mix = function(){};

var cljs$core$async$Mix$admix_STAR_$dyn_18014 = (function (m,ch){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.admix_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5499__auto__.call(null,m,ch));
} else {
var m__5497__auto__ = (cljs.core.async.admix_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5497__auto__.call(null,m,ch));
} else {
throw cljs.core.missing_protocol("Mix.admix*",m);
}
}
});
cljs.core.async.admix_STAR_ = (function cljs$core$async$admix_STAR_(m,ch){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mix$admix_STAR_$arity$2 == null)))))){
return m.cljs$core$async$Mix$admix_STAR_$arity$2(m,ch);
} else {
return cljs$core$async$Mix$admix_STAR_$dyn_18014(m,ch);
}
});

var cljs$core$async$Mix$unmix_STAR_$dyn_18015 = (function (m,ch){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.unmix_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5499__auto__.call(null,m,ch));
} else {
var m__5497__auto__ = (cljs.core.async.unmix_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(m,ch) : m__5497__auto__.call(null,m,ch));
} else {
throw cljs.core.missing_protocol("Mix.unmix*",m);
}
}
});
cljs.core.async.unmix_STAR_ = (function cljs$core$async$unmix_STAR_(m,ch){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mix$unmix_STAR_$arity$2 == null)))))){
return m.cljs$core$async$Mix$unmix_STAR_$arity$2(m,ch);
} else {
return cljs$core$async$Mix$unmix_STAR_$dyn_18015(m,ch);
}
});

var cljs$core$async$Mix$unmix_all_STAR_$dyn_18016 = (function (m){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.unmix_all_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(m) : m__5499__auto__.call(null,m));
} else {
var m__5497__auto__ = (cljs.core.async.unmix_all_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(m) : m__5497__auto__.call(null,m));
} else {
throw cljs.core.missing_protocol("Mix.unmix-all*",m);
}
}
});
cljs.core.async.unmix_all_STAR_ = (function cljs$core$async$unmix_all_STAR_(m){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mix$unmix_all_STAR_$arity$1 == null)))))){
return m.cljs$core$async$Mix$unmix_all_STAR_$arity$1(m);
} else {
return cljs$core$async$Mix$unmix_all_STAR_$dyn_18016(m);
}
});

var cljs$core$async$Mix$toggle_STAR_$dyn_18019 = (function (m,state_map){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.toggle_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(m,state_map) : m__5499__auto__.call(null,m,state_map));
} else {
var m__5497__auto__ = (cljs.core.async.toggle_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(m,state_map) : m__5497__auto__.call(null,m,state_map));
} else {
throw cljs.core.missing_protocol("Mix.toggle*",m);
}
}
});
cljs.core.async.toggle_STAR_ = (function cljs$core$async$toggle_STAR_(m,state_map){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mix$toggle_STAR_$arity$2 == null)))))){
return m.cljs$core$async$Mix$toggle_STAR_$arity$2(m,state_map);
} else {
return cljs$core$async$Mix$toggle_STAR_$dyn_18019(m,state_map);
}
});

var cljs$core$async$Mix$solo_mode_STAR_$dyn_18022 = (function (m,mode){
var x__5498__auto__ = (((m == null))?null:m);
var m__5499__auto__ = (cljs.core.async.solo_mode_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(m,mode) : m__5499__auto__.call(null,m,mode));
} else {
var m__5497__auto__ = (cljs.core.async.solo_mode_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(m,mode) : m__5497__auto__.call(null,m,mode));
} else {
throw cljs.core.missing_protocol("Mix.solo-mode*",m);
}
}
});
cljs.core.async.solo_mode_STAR_ = (function cljs$core$async$solo_mode_STAR_(m,mode){
if((((!((m == null)))) && ((!((m.cljs$core$async$Mix$solo_mode_STAR_$arity$2 == null)))))){
return m.cljs$core$async$Mix$solo_mode_STAR_$arity$2(m,mode);
} else {
return cljs$core$async$Mix$solo_mode_STAR_$dyn_18022(m,mode);
}
});

cljs.core.async.ioc_alts_BANG_ = (function cljs$core$async$ioc_alts_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___18024 = arguments.length;
var i__5877__auto___18025 = (0);
while(true){
if((i__5877__auto___18025 < len__5876__auto___18024)){
args__5882__auto__.push((arguments[i__5877__auto___18025]));

var G__18026 = (i__5877__auto___18025 + (1));
i__5877__auto___18025 = G__18026;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (state,cont_block,ports,p__15760){
var map__15761 = p__15760;
var map__15761__$1 = cljs.core.__destructure_map(map__15761);
var opts = map__15761__$1;
var statearr_15762_18032 = state;
(statearr_15762_18032[(1)] = cont_block);


var temp__5823__auto__ = cljs.core.async.do_alts((function (val){
var statearr_15763_18037 = state;
(statearr_15763_18037[(2)] = val);


return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state);
}),ports,opts);
if(cljs.core.truth_(temp__5823__auto__)){
var cb = temp__5823__auto__;
var statearr_15764_18038 = state;
(statearr_15764_18038[(2)] = cljs.core.deref(cb));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}));

(cljs.core.async.ioc_alts_BANG_.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(cljs.core.async.ioc_alts_BANG_.cljs$lang$applyTo = (function (seq15756){
var G__15757 = cljs.core.first(seq15756);
var seq15756__$1 = cljs.core.next(seq15756);
var G__15758 = cljs.core.first(seq15756__$1);
var seq15756__$2 = cljs.core.next(seq15756__$1);
var G__15759 = cljs.core.first(seq15756__$2);
var seq15756__$3 = cljs.core.next(seq15756__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__15757,G__15758,G__15759,seq15756__$3);
}));


/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.async.Mix}
 * @implements {cljs.core.async.Mux}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async15765 = (function (change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta15766){
this.change = change;
this.solo_mode = solo_mode;
this.pick = pick;
this.cs = cs;
this.calc_state = calc_state;
this.out = out;
this.changed = changed;
this.solo_modes = solo_modes;
this.attrs = attrs;
this.meta15766 = meta15766;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_15767,meta15766__$1){
var self__ = this;
var _15767__$1 = this;
return (new cljs.core.async.t_cljs$core$async15765(self__.change,self__.solo_mode,self__.pick,self__.cs,self__.calc_state,self__.out,self__.changed,self__.solo_modes,self__.attrs,meta15766__$1));
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_15767){
var self__ = this;
var _15767__$1 = this;
return self__.meta15766;
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.out;
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$admix_STAR_$arity$2 = (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(self__.cs,cljs.core.assoc,ch,cljs.core.PersistentArrayMap.EMPTY);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$unmix_STAR_$arity$2 = (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.dissoc,ch);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$unmix_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$toggle_STAR_$arity$2 = (function (_,state_map){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(cljs.core.merge_with,cljs.core.merge),state_map);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async15765.prototype.cljs$core$async$Mix$solo_mode_STAR_$arity$2 = (function (_,mode){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_((self__.solo_modes.cljs$core$IFn$_invoke$arity$1 ? self__.solo_modes.cljs$core$IFn$_invoke$arity$1(mode) : self__.solo_modes.call(null,mode)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"mode must be one of: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(self__.solo_modes)))+"\n"+"(solo-modes mode)")));
}

cljs.core.reset_BANG_(self__.solo_mode,mode);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async15765.getBasis = (function (){
return new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"change","change",477485025,null),new cljs.core.Symbol(null,"solo-mode","solo-mode",2031788074,null),new cljs.core.Symbol(null,"pick","pick",1300068175,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"calc-state","calc-state",-349968968,null),new cljs.core.Symbol(null,"out","out",729986010,null),new cljs.core.Symbol(null,"changed","changed",-2083710852,null),new cljs.core.Symbol(null,"solo-modes","solo-modes",882180540,null),new cljs.core.Symbol(null,"attrs","attrs",-450137186,null),new cljs.core.Symbol(null,"meta15766","meta15766",1943260017,null)], null);
}));

(cljs.core.async.t_cljs$core$async15765.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async15765.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async15765");

(cljs.core.async.t_cljs$core$async15765.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async15765");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async15765.
 */
cljs.core.async.__GT_t_cljs$core$async15765 = (function cljs$core$async$__GT_t_cljs$core$async15765(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta15766){
return (new cljs.core.async.t_cljs$core$async15765(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta15766));
});


/**
 * Creates and returns a mix of one or more input channels which will
 *   be put on the supplied out channel. Input sources can be added to
 *   the mix with 'admix', and removed with 'unmix'. A mix supports
 *   soloing, muting and pausing multiple inputs atomically using
 *   'toggle', and can solo using either muting or pausing as determined
 *   by 'solo-mode'.
 * 
 *   Each channel can have zero or more boolean modes set via 'toggle':
 * 
 *   :solo - when true, only this (ond other soloed) channel(s) will appear
 *        in the mix output channel. :mute and :pause states of soloed
 *        channels are ignored. If solo-mode is :mute, non-soloed
 *        channels are muted, if :pause, non-soloed channels are
 *        paused.
 * 
 *   :mute - muted channels will have their contents consumed but not included in the mix
 *   :pause - paused channels will not have their contents consumed (and thus also not included in the mix)
 */
cljs.core.async.mix = (function cljs$core$async$mix(out){
var cs = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var solo_modes = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pause","pause",-2095325672),null,new cljs.core.Keyword(null,"mute","mute",1151223646),null], null), null);
var attrs = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(solo_modes,new cljs.core.Keyword(null,"solo","solo",-316350075));
var solo_mode = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"mute","mute",1151223646));
var change = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(cljs.core.async.sliding_buffer((1)));
var changed = (function (){
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(change,true);
});
var pick = (function (attr,chs){
return cljs.core.reduce_kv((function (ret,c,v){
if(cljs.core.truth_((attr.cljs$core$IFn$_invoke$arity$1 ? attr.cljs$core$IFn$_invoke$arity$1(v) : attr.call(null,v)))){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(ret,c);
} else {
return ret;
}
}),cljs.core.PersistentHashSet.EMPTY,chs);
});
var calc_state = (function (){
var chs = cljs.core.deref(cs);
var mode = cljs.core.deref(solo_mode);
var solos = pick(new cljs.core.Keyword(null,"solo","solo",-316350075),chs);
var pauses = pick(new cljs.core.Keyword(null,"pause","pause",-2095325672),chs);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"solos","solos",1441458643),solos,new cljs.core.Keyword(null,"mutes","mutes",1068806309),pick(new cljs.core.Keyword(null,"mute","mute",1151223646),chs),new cljs.core.Keyword(null,"reads","reads",-1215067361),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(((((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mode,new cljs.core.Keyword(null,"pause","pause",-2095325672))) && (cljs.core.seq(solos))))?cljs.core.vec(solos):cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(pauses,cljs.core.keys(chs)))),change)], null);
});
var m = (new cljs.core.async.t_cljs$core$async15765(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,cljs.core.PersistentArrayMap.EMPTY));
var c__13502__auto___18063 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_15876){
var state_val_15877 = (state_15876[(1)]);
if((state_val_15877 === (7))){
var inst_15834 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
if(cljs.core.truth_(inst_15834)){
var statearr_15880_18066 = state_15876__$1;
(statearr_15880_18066[(1)] = (8));

} else {
var statearr_15881_18067 = state_15876__$1;
(statearr_15881_18067[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (20))){
var inst_15827 = (state_15876[(7)]);
var state_15876__$1 = state_15876;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_15876__$1,(23),out,inst_15827);
} else {
if((state_val_15877 === (1))){
var inst_15804 = calc_state();
var inst_15805 = cljs.core.__destructure_map(inst_15804);
var inst_15806 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15805,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_15807 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15805,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_15808 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15805,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var inst_15809 = inst_15804;
var state_15876__$1 = (function (){var statearr_15882 = state_15876;
(statearr_15882[(8)] = inst_15806);

(statearr_15882[(9)] = inst_15807);

(statearr_15882[(10)] = inst_15808);

(statearr_15882[(11)] = inst_15809);

return statearr_15882;
})();
var statearr_15883_18068 = state_15876__$1;
(statearr_15883_18068[(2)] = null);

(statearr_15883_18068[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (24))){
var inst_15817 = (state_15876[(12)]);
var inst_15809 = inst_15817;
var state_15876__$1 = (function (){var statearr_15886 = state_15876;
(statearr_15886[(11)] = inst_15809);

return statearr_15886;
})();
var statearr_15887_18071 = state_15876__$1;
(statearr_15887_18071[(2)] = null);

(statearr_15887_18071[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (4))){
var inst_15827 = (state_15876[(7)]);
var inst_15829 = (state_15876[(13)]);
var inst_15826 = (state_15876[(2)]);
var inst_15827__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15826,(0),null);
var inst_15828 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_15826,(1),null);
var inst_15829__$1 = (inst_15827__$1 == null);
var state_15876__$1 = (function (){var statearr_15888 = state_15876;
(statearr_15888[(7)] = inst_15827__$1);

(statearr_15888[(14)] = inst_15828);

(statearr_15888[(13)] = inst_15829__$1);

return statearr_15888;
})();
if(cljs.core.truth_(inst_15829__$1)){
var statearr_15889_18076 = state_15876__$1;
(statearr_15889_18076[(1)] = (5));

} else {
var statearr_15890_18077 = state_15876__$1;
(statearr_15890_18077[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (15))){
var inst_15818 = (state_15876[(15)]);
var inst_15848 = (state_15876[(16)]);
var inst_15848__$1 = cljs.core.empty_QMARK_(inst_15818);
var state_15876__$1 = (function (){var statearr_15895 = state_15876;
(statearr_15895[(16)] = inst_15848__$1);

return statearr_15895;
})();
if(inst_15848__$1){
var statearr_15896_18082 = state_15876__$1;
(statearr_15896_18082[(1)] = (17));

} else {
var statearr_15897_18083 = state_15876__$1;
(statearr_15897_18083[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (21))){
var inst_15817 = (state_15876[(12)]);
var inst_15809 = inst_15817;
var state_15876__$1 = (function (){var statearr_15899 = state_15876;
(statearr_15899[(11)] = inst_15809);

return statearr_15899;
})();
var statearr_15904_18084 = state_15876__$1;
(statearr_15904_18084[(2)] = null);

(statearr_15904_18084[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (13))){
var inst_15841 = (state_15876[(2)]);
var inst_15842 = calc_state();
var inst_15809 = inst_15842;
var state_15876__$1 = (function (){var statearr_15905 = state_15876;
(statearr_15905[(17)] = inst_15841);

(statearr_15905[(11)] = inst_15809);

return statearr_15905;
})();
var statearr_15906_18088 = state_15876__$1;
(statearr_15906_18088[(2)] = null);

(statearr_15906_18088[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (22))){
var inst_15870 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
var statearr_15907_18090 = state_15876__$1;
(statearr_15907_18090[(2)] = inst_15870);

(statearr_15907_18090[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (6))){
var inst_15828 = (state_15876[(14)]);
var inst_15832 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_15828,change);
var state_15876__$1 = state_15876;
var statearr_15908_18099 = state_15876__$1;
(statearr_15908_18099[(2)] = inst_15832);

(statearr_15908_18099[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (25))){
var state_15876__$1 = state_15876;
var statearr_15909_18100 = state_15876__$1;
(statearr_15909_18100[(2)] = null);

(statearr_15909_18100[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (17))){
var inst_15819 = (state_15876[(18)]);
var inst_15828 = (state_15876[(14)]);
var inst_15851 = (inst_15819.cljs$core$IFn$_invoke$arity$1 ? inst_15819.cljs$core$IFn$_invoke$arity$1(inst_15828) : inst_15819.call(null,inst_15828));
var inst_15852 = cljs.core.not(inst_15851);
var state_15876__$1 = state_15876;
var statearr_15910_18103 = state_15876__$1;
(statearr_15910_18103[(2)] = inst_15852);

(statearr_15910_18103[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (3))){
var inst_15874 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
return cljs.core.async.impl.ioc_helpers.return_chan(state_15876__$1,inst_15874);
} else {
if((state_val_15877 === (12))){
var state_15876__$1 = state_15876;
var statearr_15921_18105 = state_15876__$1;
(statearr_15921_18105[(2)] = null);

(statearr_15921_18105[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (2))){
var inst_15809 = (state_15876[(11)]);
var inst_15817 = (state_15876[(12)]);
var inst_15817__$1 = cljs.core.__destructure_map(inst_15809);
var inst_15818 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15817__$1,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_15819 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15817__$1,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_15820 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_15817__$1,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var state_15876__$1 = (function (){var statearr_15934 = state_15876;
(statearr_15934[(12)] = inst_15817__$1);

(statearr_15934[(15)] = inst_15818);

(statearr_15934[(18)] = inst_15819);

return statearr_15934;
})();
return cljs.core.async.ioc_alts_BANG_(state_15876__$1,(4),inst_15820);
} else {
if((state_val_15877 === (23))){
var inst_15861 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
if(cljs.core.truth_(inst_15861)){
var statearr_15940_18112 = state_15876__$1;
(statearr_15940_18112[(1)] = (24));

} else {
var statearr_15941_18116 = state_15876__$1;
(statearr_15941_18116[(1)] = (25));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (19))){
var inst_15856 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
var statearr_15944_18121 = state_15876__$1;
(statearr_15944_18121[(2)] = inst_15856);

(statearr_15944_18121[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (11))){
var inst_15828 = (state_15876[(14)]);
var inst_15838 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(cs,cljs.core.dissoc,inst_15828);
var state_15876__$1 = state_15876;
var statearr_15948_18129 = state_15876__$1;
(statearr_15948_18129[(2)] = inst_15838);

(statearr_15948_18129[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (9))){
var inst_15818 = (state_15876[(15)]);
var inst_15828 = (state_15876[(14)]);
var inst_15845 = (state_15876[(19)]);
var inst_15845__$1 = (inst_15818.cljs$core$IFn$_invoke$arity$1 ? inst_15818.cljs$core$IFn$_invoke$arity$1(inst_15828) : inst_15818.call(null,inst_15828));
var state_15876__$1 = (function (){var statearr_15953 = state_15876;
(statearr_15953[(19)] = inst_15845__$1);

return statearr_15953;
})();
if(cljs.core.truth_(inst_15845__$1)){
var statearr_15954_18130 = state_15876__$1;
(statearr_15954_18130[(1)] = (14));

} else {
var statearr_15955_18131 = state_15876__$1;
(statearr_15955_18131[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (5))){
var inst_15829 = (state_15876[(13)]);
var state_15876__$1 = state_15876;
var statearr_15959_18132 = state_15876__$1;
(statearr_15959_18132[(2)] = inst_15829);

(statearr_15959_18132[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (14))){
var inst_15845 = (state_15876[(19)]);
var state_15876__$1 = state_15876;
var statearr_15964_18135 = state_15876__$1;
(statearr_15964_18135[(2)] = inst_15845);

(statearr_15964_18135[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (26))){
var inst_15866 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
var statearr_15969_18139 = state_15876__$1;
(statearr_15969_18139[(2)] = inst_15866);

(statearr_15969_18139[(1)] = (22));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (16))){
var inst_15858 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
if(cljs.core.truth_(inst_15858)){
var statearr_15977_18141 = state_15876__$1;
(statearr_15977_18141[(1)] = (20));

} else {
var statearr_15978_18143 = state_15876__$1;
(statearr_15978_18143[(1)] = (21));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (10))){
var inst_15872 = (state_15876[(2)]);
var state_15876__$1 = state_15876;
var statearr_15982_18144 = state_15876__$1;
(statearr_15982_18144[(2)] = inst_15872);

(statearr_15982_18144[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (18))){
var inst_15848 = (state_15876[(16)]);
var state_15876__$1 = state_15876;
var statearr_15987_18145 = state_15876__$1;
(statearr_15987_18145[(2)] = inst_15848);

(statearr_15987_18145[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_15877 === (8))){
var inst_15827 = (state_15876[(7)]);
var inst_15836 = (inst_15827 == null);
var state_15876__$1 = state_15876;
if(cljs.core.truth_(inst_15836)){
var statearr_15988_18149 = state_15876__$1;
(statearr_15988_18149[(1)] = (11));

} else {
var statearr_15993_18150 = state_15876__$1;
(statearr_15993_18150[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$mix_$_state_machine__12396__auto__ = null;
var cljs$core$async$mix_$_state_machine__12396__auto____0 = (function (){
var statearr_15996 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_15996[(0)] = cljs$core$async$mix_$_state_machine__12396__auto__);

(statearr_15996[(1)] = (1));

return statearr_15996;
});
var cljs$core$async$mix_$_state_machine__12396__auto____1 = (function (state_15876){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_15876);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e15997){var ex__12399__auto__ = e15997;
var statearr_15998_18157 = state_15876;
(statearr_15998_18157[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_15876[(4)]))){
var statearr_16000_18158 = state_15876;
(statearr_16000_18158[(1)] = cljs.core.first((state_15876[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18159 = state_15876;
state_15876 = G__18159;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$mix_$_state_machine__12396__auto__ = function(state_15876){
switch(arguments.length){
case 0:
return cljs$core$async$mix_$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$mix_$_state_machine__12396__auto____1.call(this,state_15876);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mix_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mix_$_state_machine__12396__auto____0;
cljs$core$async$mix_$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mix_$_state_machine__12396__auto____1;
return cljs$core$async$mix_$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16004 = f__13503__auto__();
(statearr_16004[(6)] = c__13502__auto___18063);

return statearr_16004;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return m;
});
/**
 * Adds ch as an input to the mix
 */
cljs.core.async.admix = (function cljs$core$async$admix(mix,ch){
return cljs.core.async.admix_STAR_(mix,ch);
});
/**
 * Removes ch as an input to the mix
 */
cljs.core.async.unmix = (function cljs$core$async$unmix(mix,ch){
return cljs.core.async.unmix_STAR_(mix,ch);
});
/**
 * removes all inputs from the mix
 */
cljs.core.async.unmix_all = (function cljs$core$async$unmix_all(mix){
return cljs.core.async.unmix_all_STAR_(mix);
});
/**
 * Atomically sets the state(s) of one or more channels in a mix. The
 *   state map is a map of channels -> channel-state-map. A
 *   channel-state-map is a map of attrs -> boolean, where attr is one or
 *   more of :mute, :pause or :solo. Any states supplied are merged with
 *   the current state.
 * 
 *   Note that channels can be added to a mix via toggle, which can be
 *   used to add channels in a particular (e.g. paused) state.
 */
cljs.core.async.toggle = (function cljs$core$async$toggle(mix,state_map){
return cljs.core.async.toggle_STAR_(mix,state_map);
});
/**
 * Sets the solo mode of the mix. mode must be one of :mute or :pause
 */
cljs.core.async.solo_mode = (function cljs$core$async$solo_mode(mix,mode){
return cljs.core.async.solo_mode_STAR_(mix,mode);
});

/**
 * @interface
 */
cljs.core.async.Pub = function(){};

var cljs$core$async$Pub$sub_STAR_$dyn_18161 = (function (p,v,ch,close_QMARK_){
var x__5498__auto__ = (((p == null))?null:p);
var m__5499__auto__ = (cljs.core.async.sub_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$4 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$4(p,v,ch,close_QMARK_) : m__5499__auto__.call(null,p,v,ch,close_QMARK_));
} else {
var m__5497__auto__ = (cljs.core.async.sub_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$4 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$4(p,v,ch,close_QMARK_) : m__5497__auto__.call(null,p,v,ch,close_QMARK_));
} else {
throw cljs.core.missing_protocol("Pub.sub*",p);
}
}
});
cljs.core.async.sub_STAR_ = (function cljs$core$async$sub_STAR_(p,v,ch,close_QMARK_){
if((((!((p == null)))) && ((!((p.cljs$core$async$Pub$sub_STAR_$arity$4 == null)))))){
return p.cljs$core$async$Pub$sub_STAR_$arity$4(p,v,ch,close_QMARK_);
} else {
return cljs$core$async$Pub$sub_STAR_$dyn_18161(p,v,ch,close_QMARK_);
}
});

var cljs$core$async$Pub$unsub_STAR_$dyn_18164 = (function (p,v,ch){
var x__5498__auto__ = (((p == null))?null:p);
var m__5499__auto__ = (cljs.core.async.unsub_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(p,v,ch) : m__5499__auto__.call(null,p,v,ch));
} else {
var m__5497__auto__ = (cljs.core.async.unsub_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(p,v,ch) : m__5497__auto__.call(null,p,v,ch));
} else {
throw cljs.core.missing_protocol("Pub.unsub*",p);
}
}
});
cljs.core.async.unsub_STAR_ = (function cljs$core$async$unsub_STAR_(p,v,ch){
if((((!((p == null)))) && ((!((p.cljs$core$async$Pub$unsub_STAR_$arity$3 == null)))))){
return p.cljs$core$async$Pub$unsub_STAR_$arity$3(p,v,ch);
} else {
return cljs$core$async$Pub$unsub_STAR_$dyn_18164(p,v,ch);
}
});

var cljs$core$async$Pub$unsub_all_STAR_$dyn_18167 = (function() {
var G__18168 = null;
var G__18168__1 = (function (p){
var x__5498__auto__ = (((p == null))?null:p);
var m__5499__auto__ = (cljs.core.async.unsub_all_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(p) : m__5499__auto__.call(null,p));
} else {
var m__5497__auto__ = (cljs.core.async.unsub_all_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(p) : m__5497__auto__.call(null,p));
} else {
throw cljs.core.missing_protocol("Pub.unsub-all*",p);
}
}
});
var G__18168__2 = (function (p,v){
var x__5498__auto__ = (((p == null))?null:p);
var m__5499__auto__ = (cljs.core.async.unsub_all_STAR_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(p,v) : m__5499__auto__.call(null,p,v));
} else {
var m__5497__auto__ = (cljs.core.async.unsub_all_STAR_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(p,v) : m__5497__auto__.call(null,p,v));
} else {
throw cljs.core.missing_protocol("Pub.unsub-all*",p);
}
}
});
G__18168 = function(p,v){
switch(arguments.length){
case 1:
return G__18168__1.call(this,p);
case 2:
return G__18168__2.call(this,p,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__18168.cljs$core$IFn$_invoke$arity$1 = G__18168__1;
G__18168.cljs$core$IFn$_invoke$arity$2 = G__18168__2;
return G__18168;
})()
;
cljs.core.async.unsub_all_STAR_ = (function cljs$core$async$unsub_all_STAR_(var_args){
var G__16041 = arguments.length;
switch (G__16041) {
case 1:
return cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (p){
if((((!((p == null)))) && ((!((p.cljs$core$async$Pub$unsub_all_STAR_$arity$1 == null)))))){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$1(p);
} else {
return cljs$core$async$Pub$unsub_all_STAR_$dyn_18167(p);
}
}));

(cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (p,v){
if((((!((p == null)))) && ((!((p.cljs$core$async$Pub$unsub_all_STAR_$arity$2 == null)))))){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$2(p,v);
} else {
return cljs$core$async$Pub$unsub_all_STAR_$dyn_18167(p,v);
}
}));

(cljs.core.async.unsub_all_STAR_.cljs$lang$maxFixedArity = 2);



/**
* @constructor
 * @implements {cljs.core.async.Pub}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.async.Mux}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async16068 = (function (ch,topic_fn,buf_fn,mults,ensure_mult,meta16069){
this.ch = ch;
this.topic_fn = topic_fn;
this.buf_fn = buf_fn;
this.mults = mults;
this.ensure_mult = ensure_mult;
this.meta16069 = meta16069;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_16070,meta16069__$1){
var self__ = this;
var _16070__$1 = this;
return (new cljs.core.async.t_cljs$core$async16068(self__.ch,self__.topic_fn,self__.buf_fn,self__.mults,self__.ensure_mult,meta16069__$1));
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_16070){
var self__ = this;
var _16070__$1 = this;
return self__.meta16069;
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Pub$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Pub$sub_STAR_$arity$4 = (function (p,topic,ch__$1,close_QMARK_){
var self__ = this;
var p__$1 = this;
var m = (self__.ensure_mult.cljs$core$IFn$_invoke$arity$1 ? self__.ensure_mult.cljs$core$IFn$_invoke$arity$1(topic) : self__.ensure_mult.call(null,topic));
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3(m,ch__$1,close_QMARK_);
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Pub$unsub_STAR_$arity$3 = (function (p,topic,ch__$1){
var self__ = this;
var p__$1 = this;
var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(self__.mults),topic);
if(cljs.core.truth_(temp__5823__auto__)){
var m = temp__5823__auto__;
return cljs.core.async.untap(m,ch__$1);
} else {
return null;
}
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.reset_BANG_(self__.mults,cljs.core.PersistentArrayMap.EMPTY);
}));

(cljs.core.async.t_cljs$core$async16068.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$2 = (function (_,topic){
var self__ = this;
var ___$1 = this;
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.mults,cljs.core.dissoc,topic);
}));

(cljs.core.async.t_cljs$core$async16068.getBasis = (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"topic-fn","topic-fn",-862449736,null),new cljs.core.Symbol(null,"buf-fn","buf-fn",-1200281591,null),new cljs.core.Symbol(null,"mults","mults",-461114485,null),new cljs.core.Symbol(null,"ensure-mult","ensure-mult",1796584816,null),new cljs.core.Symbol(null,"meta16069","meta16069",1660784060,null)], null);
}));

(cljs.core.async.t_cljs$core$async16068.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async16068.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async16068");

(cljs.core.async.t_cljs$core$async16068.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async16068");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async16068.
 */
cljs.core.async.__GT_t_cljs$core$async16068 = (function cljs$core$async$__GT_t_cljs$core$async16068(ch,topic_fn,buf_fn,mults,ensure_mult,meta16069){
return (new cljs.core.async.t_cljs$core$async16068(ch,topic_fn,buf_fn,mults,ensure_mult,meta16069));
});


/**
 * Creates and returns a pub(lication) of the supplied channel,
 *   partitioned into topics by the topic-fn. topic-fn will be applied to
 *   each value on the channel and the result will determine the 'topic'
 *   on which that value will be put. Channels can be subscribed to
 *   receive copies of topics using 'sub', and unsubscribed using
 *   'unsub'. Each topic will be handled by an internal mult on a
 *   dedicated channel. By default these internal channels are
 *   unbuffered, but a buf-fn can be supplied which, given a topic,
 *   creates a buffer with desired properties.
 * 
 *   Each item is distributed to all subs in parallel and synchronously,
 *   i.e. each sub must accept before the next item is distributed. Use
 *   buffering/windowing to prevent slow subs from holding up the pub.
 * 
 *   Items received when there are no matching subs get dropped.
 * 
 *   Note that if buf-fns are used then each topic is handled
 *   asynchronously, i.e. if a channel is subscribed to more than one
 *   topic it should not expect them to be interleaved identically with
 *   the source.
 */
cljs.core.async.pub = (function cljs$core$async$pub(var_args){
var G__16057 = arguments.length;
switch (G__16057) {
case 2:
return cljs.core.async.pub.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.pub.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.pub.cljs$core$IFn$_invoke$arity$2 = (function (ch,topic_fn){
return cljs.core.async.pub.cljs$core$IFn$_invoke$arity$3(ch,topic_fn,cljs.core.constantly(null));
}));

(cljs.core.async.pub.cljs$core$IFn$_invoke$arity$3 = (function (ch,topic_fn,buf_fn){
var mults = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var ensure_mult = (function (topic){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(mults),topic);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(mults,(function (p1__16054_SHARP_){
if(cljs.core.truth_((p1__16054_SHARP_.cljs$core$IFn$_invoke$arity$1 ? p1__16054_SHARP_.cljs$core$IFn$_invoke$arity$1(topic) : p1__16054_SHARP_.call(null,topic)))){
return p1__16054_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__16054_SHARP_,topic,cljs.core.async.mult(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((buf_fn.cljs$core$IFn$_invoke$arity$1 ? buf_fn.cljs$core$IFn$_invoke$arity$1(topic) : buf_fn.call(null,topic)))));
}
})),topic);
}
});
var p = (new cljs.core.async.t_cljs$core$async16068(ch,topic_fn,buf_fn,mults,ensure_mult,cljs.core.PersistentArrayMap.EMPTY));
var c__13502__auto___18197 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16180){
var state_val_16181 = (state_16180[(1)]);
if((state_val_16181 === (7))){
var inst_16170 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16185_18198 = state_16180__$1;
(statearr_16185_18198[(2)] = inst_16170);

(statearr_16185_18198[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (20))){
var state_16180__$1 = state_16180;
var statearr_16187_18199 = state_16180__$1;
(statearr_16187_18199[(2)] = null);

(statearr_16187_18199[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (1))){
var state_16180__$1 = state_16180;
var statearr_16189_18200 = state_16180__$1;
(statearr_16189_18200[(2)] = null);

(statearr_16189_18200[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (24))){
var inst_16148 = (state_16180[(7)]);
var inst_16161 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(mults,cljs.core.dissoc,inst_16148);
var state_16180__$1 = state_16180;
var statearr_16192_18201 = state_16180__$1;
(statearr_16192_18201[(2)] = inst_16161);

(statearr_16192_18201[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (4))){
var inst_16077 = (state_16180[(8)]);
var inst_16077__$1 = (state_16180[(2)]);
var inst_16078 = (inst_16077__$1 == null);
var state_16180__$1 = (function (){var statearr_16193 = state_16180;
(statearr_16193[(8)] = inst_16077__$1);

return statearr_16193;
})();
if(cljs.core.truth_(inst_16078)){
var statearr_16194_18202 = state_16180__$1;
(statearr_16194_18202[(1)] = (5));

} else {
var statearr_16195_18203 = state_16180__$1;
(statearr_16195_18203[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (15))){
var inst_16133 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16198_18204 = state_16180__$1;
(statearr_16198_18204[(2)] = inst_16133);

(statearr_16198_18204[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (21))){
var inst_16166 = (state_16180[(2)]);
var state_16180__$1 = (function (){var statearr_16199 = state_16180;
(statearr_16199[(9)] = inst_16166);

return statearr_16199;
})();
var statearr_16203_18205 = state_16180__$1;
(statearr_16203_18205[(2)] = null);

(statearr_16203_18205[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (13))){
var inst_16101 = (state_16180[(10)]);
var inst_16114 = cljs.core.chunked_seq_QMARK_(inst_16101);
var state_16180__$1 = state_16180;
if(inst_16114){
var statearr_16206_18206 = state_16180__$1;
(statearr_16206_18206[(1)] = (16));

} else {
var statearr_16210_18207 = state_16180__$1;
(statearr_16210_18207[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (22))){
var inst_16154 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
if(cljs.core.truth_(inst_16154)){
var statearr_16213_18208 = state_16180__$1;
(statearr_16213_18208[(1)] = (23));

} else {
var statearr_16214_18209 = state_16180__$1;
(statearr_16214_18209[(1)] = (24));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (6))){
var inst_16077 = (state_16180[(8)]);
var inst_16148 = (state_16180[(7)]);
var inst_16150 = (state_16180[(11)]);
var inst_16148__$1 = (topic_fn.cljs$core$IFn$_invoke$arity$1 ? topic_fn.cljs$core$IFn$_invoke$arity$1(inst_16077) : topic_fn.call(null,inst_16077));
var inst_16149 = cljs.core.deref(mults);
var inst_16150__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_16149,inst_16148__$1);
var state_16180__$1 = (function (){var statearr_16218 = state_16180;
(statearr_16218[(7)] = inst_16148__$1);

(statearr_16218[(11)] = inst_16150__$1);

return statearr_16218;
})();
if(cljs.core.truth_(inst_16150__$1)){
var statearr_16219_18210 = state_16180__$1;
(statearr_16219_18210[(1)] = (19));

} else {
var statearr_16220_18211 = state_16180__$1;
(statearr_16220_18211[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (25))){
var inst_16163 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16224_18212 = state_16180__$1;
(statearr_16224_18212[(2)] = inst_16163);

(statearr_16224_18212[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (17))){
var inst_16101 = (state_16180[(10)]);
var inst_16124 = cljs.core.first(inst_16101);
var inst_16125 = cljs.core.async.muxch_STAR_(inst_16124);
var inst_16126 = cljs.core.async.close_BANG_(inst_16125);
var inst_16127 = cljs.core.next(inst_16101);
var inst_16087 = inst_16127;
var inst_16088 = null;
var inst_16089 = (0);
var inst_16090 = (0);
var state_16180__$1 = (function (){var statearr_16227 = state_16180;
(statearr_16227[(12)] = inst_16126);

(statearr_16227[(13)] = inst_16087);

(statearr_16227[(14)] = inst_16088);

(statearr_16227[(15)] = inst_16089);

(statearr_16227[(16)] = inst_16090);

return statearr_16227;
})();
var statearr_16228_18221 = state_16180__$1;
(statearr_16228_18221[(2)] = null);

(statearr_16228_18221[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (3))){
var inst_16178 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
return cljs.core.async.impl.ioc_helpers.return_chan(state_16180__$1,inst_16178);
} else {
if((state_val_16181 === (12))){
var inst_16135 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16229_18222 = state_16180__$1;
(statearr_16229_18222[(2)] = inst_16135);

(statearr_16229_18222[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (2))){
var state_16180__$1 = state_16180;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16180__$1,(4),ch);
} else {
if((state_val_16181 === (23))){
var state_16180__$1 = state_16180;
var statearr_16231_18223 = state_16180__$1;
(statearr_16231_18223[(2)] = null);

(statearr_16231_18223[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (19))){
var inst_16150 = (state_16180[(11)]);
var inst_16077 = (state_16180[(8)]);
var inst_16152 = cljs.core.async.muxch_STAR_(inst_16150);
var state_16180__$1 = state_16180;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16180__$1,(22),inst_16152,inst_16077);
} else {
if((state_val_16181 === (11))){
var inst_16087 = (state_16180[(13)]);
var inst_16101 = (state_16180[(10)]);
var inst_16101__$1 = cljs.core.seq(inst_16087);
var state_16180__$1 = (function (){var statearr_16232 = state_16180;
(statearr_16232[(10)] = inst_16101__$1);

return statearr_16232;
})();
if(inst_16101__$1){
var statearr_16234_18226 = state_16180__$1;
(statearr_16234_18226[(1)] = (13));

} else {
var statearr_16235_18227 = state_16180__$1;
(statearr_16235_18227[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (9))){
var inst_16137 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16236_18228 = state_16180__$1;
(statearr_16236_18228[(2)] = inst_16137);

(statearr_16236_18228[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (5))){
var inst_16084 = cljs.core.deref(mults);
var inst_16085 = cljs.core.vals(inst_16084);
var inst_16086 = cljs.core.seq(inst_16085);
var inst_16087 = inst_16086;
var inst_16088 = null;
var inst_16089 = (0);
var inst_16090 = (0);
var state_16180__$1 = (function (){var statearr_16237 = state_16180;
(statearr_16237[(13)] = inst_16087);

(statearr_16237[(14)] = inst_16088);

(statearr_16237[(15)] = inst_16089);

(statearr_16237[(16)] = inst_16090);

return statearr_16237;
})();
var statearr_16240_18230 = state_16180__$1;
(statearr_16240_18230[(2)] = null);

(statearr_16240_18230[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (14))){
var state_16180__$1 = state_16180;
var statearr_16247_18231 = state_16180__$1;
(statearr_16247_18231[(2)] = null);

(statearr_16247_18231[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (16))){
var inst_16101 = (state_16180[(10)]);
var inst_16119 = cljs.core.chunk_first(inst_16101);
var inst_16120 = cljs.core.chunk_rest(inst_16101);
var inst_16121 = cljs.core.count(inst_16119);
var inst_16087 = inst_16120;
var inst_16088 = inst_16119;
var inst_16089 = inst_16121;
var inst_16090 = (0);
var state_16180__$1 = (function (){var statearr_16249 = state_16180;
(statearr_16249[(13)] = inst_16087);

(statearr_16249[(14)] = inst_16088);

(statearr_16249[(15)] = inst_16089);

(statearr_16249[(16)] = inst_16090);

return statearr_16249;
})();
var statearr_16256_18232 = state_16180__$1;
(statearr_16256_18232[(2)] = null);

(statearr_16256_18232[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (10))){
var inst_16088 = (state_16180[(14)]);
var inst_16090 = (state_16180[(16)]);
var inst_16087 = (state_16180[(13)]);
var inst_16089 = (state_16180[(15)]);
var inst_16095 = cljs.core._nth(inst_16088,inst_16090);
var inst_16096 = cljs.core.async.muxch_STAR_(inst_16095);
var inst_16097 = cljs.core.async.close_BANG_(inst_16096);
var inst_16098 = (inst_16090 + (1));
var tmp16242 = inst_16087;
var tmp16243 = inst_16089;
var tmp16244 = inst_16088;
var inst_16087__$1 = tmp16242;
var inst_16088__$1 = tmp16244;
var inst_16089__$1 = tmp16243;
var inst_16090__$1 = inst_16098;
var state_16180__$1 = (function (){var statearr_16257 = state_16180;
(statearr_16257[(17)] = inst_16097);

(statearr_16257[(13)] = inst_16087__$1);

(statearr_16257[(14)] = inst_16088__$1);

(statearr_16257[(15)] = inst_16089__$1);

(statearr_16257[(16)] = inst_16090__$1);

return statearr_16257;
})();
var statearr_16258_18233 = state_16180__$1;
(statearr_16258_18233[(2)] = null);

(statearr_16258_18233[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (18))){
var inst_16130 = (state_16180[(2)]);
var state_16180__$1 = state_16180;
var statearr_16260_18234 = state_16180__$1;
(statearr_16260_18234[(2)] = inst_16130);

(statearr_16260_18234[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16181 === (8))){
var inst_16090 = (state_16180[(16)]);
var inst_16089 = (state_16180[(15)]);
var inst_16092 = (inst_16090 < inst_16089);
var inst_16093 = inst_16092;
var state_16180__$1 = state_16180;
if(cljs.core.truth_(inst_16093)){
var statearr_16261_18235 = state_16180__$1;
(statearr_16261_18235[(1)] = (10));

} else {
var statearr_16262_18236 = state_16180__$1;
(statearr_16262_18236[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_16265 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_16265[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_16265[(1)] = (1));

return statearr_16265;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16180){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16180);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16268){var ex__12399__auto__ = e16268;
var statearr_16269_18237 = state_16180;
(statearr_16269_18237[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16180[(4)]))){
var statearr_16270_18238 = state_16180;
(statearr_16270_18238[(1)] = cljs.core.first((state_16180[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18243 = state_16180;
state_16180 = G__18243;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16180){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16180);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16272 = f__13503__auto__();
(statearr_16272[(6)] = c__13502__auto___18197);

return statearr_16272;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return p;
}));

(cljs.core.async.pub.cljs$lang$maxFixedArity = 3);

/**
 * Subscribes a channel to a topic of a pub.
 * 
 *   By default the channel will be closed when the source closes,
 *   but can be determined by the close? parameter.
 */
cljs.core.async.sub = (function cljs$core$async$sub(var_args){
var G__16275 = arguments.length;
switch (G__16275) {
case 3:
return cljs.core.async.sub.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return cljs.core.async.sub.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.sub.cljs$core$IFn$_invoke$arity$3 = (function (p,topic,ch){
return cljs.core.async.sub.cljs$core$IFn$_invoke$arity$4(p,topic,ch,true);
}));

(cljs.core.async.sub.cljs$core$IFn$_invoke$arity$4 = (function (p,topic,ch,close_QMARK_){
return cljs.core.async.sub_STAR_(p,topic,ch,close_QMARK_);
}));

(cljs.core.async.sub.cljs$lang$maxFixedArity = 4);

/**
 * Unsubscribes a channel from a topic of a pub
 */
cljs.core.async.unsub = (function cljs$core$async$unsub(p,topic,ch){
return cljs.core.async.unsub_STAR_(p,topic,ch);
});
/**
 * Unsubscribes all channels from a pub, or a topic of a pub
 */
cljs.core.async.unsub_all = (function cljs$core$async$unsub_all(var_args){
var G__16279 = arguments.length;
switch (G__16279) {
case 1:
return cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$1 = (function (p){
return cljs.core.async.unsub_all_STAR_(p);
}));

(cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$2 = (function (p,topic){
return cljs.core.async.unsub_all_STAR_(p,topic);
}));

(cljs.core.async.unsub_all.cljs$lang$maxFixedArity = 2);

/**
 * Takes a function and a collection of source channels, and returns a
 *   channel which contains the values produced by applying f to the set
 *   of first items taken from each source channel, followed by applying
 *   f to the set of second items from each channel, until any one of the
 *   channels is closed, at which point the output channel will be
 *   closed. The returned channel will be unbuffered by default, or a
 *   buf-or-n can be supplied
 */
cljs.core.async.map = (function cljs$core$async$map(var_args){
var G__16286 = arguments.length;
switch (G__16286) {
case 2:
return cljs.core.async.map.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.map.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.map.cljs$core$IFn$_invoke$arity$2 = (function (f,chs){
return cljs.core.async.map.cljs$core$IFn$_invoke$arity$3(f,chs,null);
}));

(cljs.core.async.map.cljs$core$IFn$_invoke$arity$3 = (function (f,chs,buf_or_n){
var chs__$1 = cljs.core.vec(chs);
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var cnt = cljs.core.count(chs__$1);
var rets = cljs.core.object_array.cljs$core$IFn$_invoke$arity$1(cnt);
var dchan = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
var dctr = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var done = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (i){
return (function (ret){
(rets[i] = ret);

if((cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(dctr,cljs.core.dec) === (0))){
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(dchan,rets.slice((0)));
} else {
return null;
}
});
}),cljs.core.range.cljs$core$IFn$_invoke$arity$1(cnt));
if((cnt === (0))){
cljs.core.async.close_BANG_(out);
} else {
var c__13502__auto___18256 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16338){
var state_val_16339 = (state_16338[(1)]);
if((state_val_16339 === (7))){
var state_16338__$1 = state_16338;
var statearr_16347_18257 = state_16338__$1;
(statearr_16347_18257[(2)] = null);

(statearr_16347_18257[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (1))){
var state_16338__$1 = state_16338;
var statearr_16348_18258 = state_16338__$1;
(statearr_16348_18258[(2)] = null);

(statearr_16348_18258[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (4))){
var inst_16292 = (state_16338[(7)]);
var inst_16291 = (state_16338[(8)]);
var inst_16294 = (inst_16292 < inst_16291);
var state_16338__$1 = state_16338;
if(cljs.core.truth_(inst_16294)){
var statearr_16350_18259 = state_16338__$1;
(statearr_16350_18259[(1)] = (6));

} else {
var statearr_16351_18260 = state_16338__$1;
(statearr_16351_18260[(1)] = (7));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (15))){
var inst_16317 = (state_16338[(9)]);
var inst_16323 = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(f,inst_16317);
var state_16338__$1 = state_16338;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16338__$1,(17),out,inst_16323);
} else {
if((state_val_16339 === (13))){
var inst_16317 = (state_16338[(9)]);
var inst_16317__$1 = (state_16338[(2)]);
var inst_16318 = cljs.core.some(cljs.core.nil_QMARK_,inst_16317__$1);
var state_16338__$1 = (function (){var statearr_16358 = state_16338;
(statearr_16358[(9)] = inst_16317__$1);

return statearr_16358;
})();
if(cljs.core.truth_(inst_16318)){
var statearr_16359_18261 = state_16338__$1;
(statearr_16359_18261[(1)] = (14));

} else {
var statearr_16360_18262 = state_16338__$1;
(statearr_16360_18262[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (6))){
var state_16338__$1 = state_16338;
var statearr_16361_18264 = state_16338__$1;
(statearr_16361_18264[(2)] = null);

(statearr_16361_18264[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (17))){
var inst_16325 = (state_16338[(2)]);
var state_16338__$1 = (function (){var statearr_16370 = state_16338;
(statearr_16370[(10)] = inst_16325);

return statearr_16370;
})();
var statearr_16371_18266 = state_16338__$1;
(statearr_16371_18266[(2)] = null);

(statearr_16371_18266[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (3))){
var inst_16331 = (state_16338[(2)]);
var state_16338__$1 = state_16338;
return cljs.core.async.impl.ioc_helpers.return_chan(state_16338__$1,inst_16331);
} else {
if((state_val_16339 === (12))){
var _ = (function (){var statearr_16375 = state_16338;
(statearr_16375[(4)] = cljs.core.rest((state_16338[(4)])));

return statearr_16375;
})();
var state_16338__$1 = state_16338;
var ex16369 = (state_16338__$1[(2)]);
var statearr_16376_18267 = state_16338__$1;
(statearr_16376_18267[(5)] = ex16369);


if((ex16369 instanceof Object)){
var statearr_16377_18268 = state_16338__$1;
(statearr_16377_18268[(1)] = (11));

(statearr_16377_18268[(5)] = null);

} else {
throw ex16369;

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (2))){
var inst_16289 = cljs.core.reset_BANG_(dctr,cnt);
var inst_16291 = cnt;
var inst_16292 = (0);
var state_16338__$1 = (function (){var statearr_16379 = state_16338;
(statearr_16379[(11)] = inst_16289);

(statearr_16379[(8)] = inst_16291);

(statearr_16379[(7)] = inst_16292);

return statearr_16379;
})();
var statearr_16385_18271 = state_16338__$1;
(statearr_16385_18271[(2)] = null);

(statearr_16385_18271[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (11))){
var inst_16296 = (state_16338[(2)]);
var inst_16297 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(dctr,cljs.core.dec);
var state_16338__$1 = (function (){var statearr_16386 = state_16338;
(statearr_16386[(12)] = inst_16296);

return statearr_16386;
})();
var statearr_16387_18272 = state_16338__$1;
(statearr_16387_18272[(2)] = inst_16297);

(statearr_16387_18272[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (9))){
var inst_16292 = (state_16338[(7)]);
var _ = (function (){var statearr_16388 = state_16338;
(statearr_16388[(4)] = cljs.core.cons((12),(state_16338[(4)])));

return statearr_16388;
})();
var inst_16303 = (chs__$1.cljs$core$IFn$_invoke$arity$1 ? chs__$1.cljs$core$IFn$_invoke$arity$1(inst_16292) : chs__$1.call(null,inst_16292));
var inst_16304 = (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(inst_16292) : done.call(null,inst_16292));
var inst_16305 = cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2(inst_16303,inst_16304);
var ___$1 = (function (){var statearr_16389 = state_16338;
(statearr_16389[(4)] = cljs.core.rest((state_16338[(4)])));

return statearr_16389;
})();
var state_16338__$1 = state_16338;
var statearr_16390_18279 = state_16338__$1;
(statearr_16390_18279[(2)] = inst_16305);

(statearr_16390_18279[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (5))){
var inst_16315 = (state_16338[(2)]);
var state_16338__$1 = (function (){var statearr_16391 = state_16338;
(statearr_16391[(13)] = inst_16315);

return statearr_16391;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16338__$1,(13),dchan);
} else {
if((state_val_16339 === (14))){
var inst_16320 = cljs.core.async.close_BANG_(out);
var state_16338__$1 = state_16338;
var statearr_16397_18280 = state_16338__$1;
(statearr_16397_18280[(2)] = inst_16320);

(statearr_16397_18280[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (16))){
var inst_16328 = (state_16338[(2)]);
var state_16338__$1 = state_16338;
var statearr_16398_18281 = state_16338__$1;
(statearr_16398_18281[(2)] = inst_16328);

(statearr_16398_18281[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (10))){
var inst_16292 = (state_16338[(7)]);
var inst_16308 = (state_16338[(2)]);
var inst_16309 = (inst_16292 + (1));
var inst_16292__$1 = inst_16309;
var state_16338__$1 = (function (){var statearr_16399 = state_16338;
(statearr_16399[(14)] = inst_16308);

(statearr_16399[(7)] = inst_16292__$1);

return statearr_16399;
})();
var statearr_16400_18282 = state_16338__$1;
(statearr_16400_18282[(2)] = null);

(statearr_16400_18282[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16339 === (8))){
var inst_16313 = (state_16338[(2)]);
var state_16338__$1 = state_16338;
var statearr_16401_18284 = state_16338__$1;
(statearr_16401_18284[(2)] = inst_16313);

(statearr_16401_18284[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_16402 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_16402[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_16402[(1)] = (1));

return statearr_16402;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16338){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16338);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16403){var ex__12399__auto__ = e16403;
var statearr_16404_18287 = state_16338;
(statearr_16404_18287[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16338[(4)]))){
var statearr_16406_18289 = state_16338;
(statearr_16406_18289[(1)] = cljs.core.first((state_16338[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18290 = state_16338;
state_16338 = G__18290;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16338){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16338);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16407 = f__13503__auto__();
(statearr_16407[(6)] = c__13502__auto___18256);

return statearr_16407;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

}

return out;
}));

(cljs.core.async.map.cljs$lang$maxFixedArity = 3);

/**
 * Takes a collection of source channels and returns a channel which
 *   contains all values taken from them. The returned channel will be
 *   unbuffered by default, or a buf-or-n can be supplied. The channel
 *   will close after all the source channels have closed.
 */
cljs.core.async.merge = (function cljs$core$async$merge(var_args){
var G__16411 = arguments.length;
switch (G__16411) {
case 1:
return cljs.core.async.merge.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.merge.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.merge.cljs$core$IFn$_invoke$arity$1 = (function (chs){
return cljs.core.async.merge.cljs$core$IFn$_invoke$arity$2(chs,null);
}));

(cljs.core.async.merge.cljs$core$IFn$_invoke$arity$2 = (function (chs,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18293 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16450){
var state_val_16451 = (state_16450[(1)]);
if((state_val_16451 === (7))){
var inst_16423 = (state_16450[(7)]);
var inst_16424 = (state_16450[(8)]);
var inst_16423__$1 = (state_16450[(2)]);
var inst_16424__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_16423__$1,(0),null);
var inst_16425 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_16423__$1,(1),null);
var inst_16431 = (inst_16424__$1 == null);
var state_16450__$1 = (function (){var statearr_16457 = state_16450;
(statearr_16457[(7)] = inst_16423__$1);

(statearr_16457[(8)] = inst_16424__$1);

(statearr_16457[(9)] = inst_16425);

return statearr_16457;
})();
if(cljs.core.truth_(inst_16431)){
var statearr_16458_18294 = state_16450__$1;
(statearr_16458_18294[(1)] = (8));

} else {
var statearr_16459_18295 = state_16450__$1;
(statearr_16459_18295[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (1))){
var inst_16413 = cljs.core.vec(chs);
var inst_16414 = inst_16413;
var state_16450__$1 = (function (){var statearr_16460 = state_16450;
(statearr_16460[(10)] = inst_16414);

return statearr_16460;
})();
var statearr_16461_18296 = state_16450__$1;
(statearr_16461_18296[(2)] = null);

(statearr_16461_18296[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (4))){
var inst_16414 = (state_16450[(10)]);
var state_16450__$1 = state_16450;
return cljs.core.async.ioc_alts_BANG_(state_16450__$1,(7),inst_16414);
} else {
if((state_val_16451 === (6))){
var inst_16446 = (state_16450[(2)]);
var state_16450__$1 = state_16450;
var statearr_16462_18297 = state_16450__$1;
(statearr_16462_18297[(2)] = inst_16446);

(statearr_16462_18297[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (3))){
var inst_16448 = (state_16450[(2)]);
var state_16450__$1 = state_16450;
return cljs.core.async.impl.ioc_helpers.return_chan(state_16450__$1,inst_16448);
} else {
if((state_val_16451 === (2))){
var inst_16414 = (state_16450[(10)]);
var inst_16416 = cljs.core.count(inst_16414);
var inst_16417 = (inst_16416 > (0));
var state_16450__$1 = state_16450;
if(cljs.core.truth_(inst_16417)){
var statearr_16465_18299 = state_16450__$1;
(statearr_16465_18299[(1)] = (4));

} else {
var statearr_16466_18300 = state_16450__$1;
(statearr_16466_18300[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (11))){
var inst_16414 = (state_16450[(10)]);
var inst_16439 = (state_16450[(2)]);
var tmp16463 = inst_16414;
var inst_16414__$1 = tmp16463;
var state_16450__$1 = (function (){var statearr_16467 = state_16450;
(statearr_16467[(11)] = inst_16439);

(statearr_16467[(10)] = inst_16414__$1);

return statearr_16467;
})();
var statearr_16468_18304 = state_16450__$1;
(statearr_16468_18304[(2)] = null);

(statearr_16468_18304[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (9))){
var inst_16424 = (state_16450[(8)]);
var state_16450__$1 = state_16450;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16450__$1,(11),out,inst_16424);
} else {
if((state_val_16451 === (5))){
var inst_16444 = cljs.core.async.close_BANG_(out);
var state_16450__$1 = state_16450;
var statearr_16473_18305 = state_16450__$1;
(statearr_16473_18305[(2)] = inst_16444);

(statearr_16473_18305[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (10))){
var inst_16442 = (state_16450[(2)]);
var state_16450__$1 = state_16450;
var statearr_16474_18306 = state_16450__$1;
(statearr_16474_18306[(2)] = inst_16442);

(statearr_16474_18306[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16451 === (8))){
var inst_16414 = (state_16450[(10)]);
var inst_16423 = (state_16450[(7)]);
var inst_16424 = (state_16450[(8)]);
var inst_16425 = (state_16450[(9)]);
var inst_16434 = (function (){var cs = inst_16414;
var vec__16419 = inst_16423;
var v = inst_16424;
var c = inst_16425;
return (function (p1__16409_SHARP_){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(c,p1__16409_SHARP_);
});
})();
var inst_16435 = cljs.core.filterv(inst_16434,inst_16414);
var inst_16414__$1 = inst_16435;
var state_16450__$1 = (function (){var statearr_16476 = state_16450;
(statearr_16476[(10)] = inst_16414__$1);

return statearr_16476;
})();
var statearr_16477_18307 = state_16450__$1;
(statearr_16477_18307[(2)] = null);

(statearr_16477_18307[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_16481 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_16481[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_16481[(1)] = (1));

return statearr_16481;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16450){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16450);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16482){var ex__12399__auto__ = e16482;
var statearr_16486_18308 = state_16450;
(statearr_16486_18308[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16450[(4)]))){
var statearr_16487_18309 = state_16450;
(statearr_16487_18309[(1)] = cljs.core.first((state_16450[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18310 = state_16450;
state_16450 = G__18310;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16450){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16450);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16488 = f__13503__auto__();
(statearr_16488[(6)] = c__13502__auto___18293);

return statearr_16488;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.merge.cljs$lang$maxFixedArity = 2);

/**
 * Returns a channel containing the single (collection) result of the
 *   items taken from the channel conjoined to the supplied
 *   collection. ch must close before into produces a result.
 */
cljs.core.async.into = (function cljs$core$async$into(coll,ch){
return cljs.core.async.reduce(cljs.core.conj,coll,ch);
});
/**
 * Returns a channel that will return, at most, n items from ch. After n items
 * have been returned, or ch has been closed, the return chanel will close.
 * 
 *   The output channel is unbuffered by default, unless buf-or-n is given.
 */
cljs.core.async.take = (function cljs$core$async$take(var_args){
var G__16495 = arguments.length;
switch (G__16495) {
case 2:
return cljs.core.async.take.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.take.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.take.cljs$core$IFn$_invoke$arity$2 = (function (n,ch){
return cljs.core.async.take.cljs$core$IFn$_invoke$arity$3(n,ch,null);
}));

(cljs.core.async.take.cljs$core$IFn$_invoke$arity$3 = (function (n,ch,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18312 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16528){
var state_val_16529 = (state_16528[(1)]);
if((state_val_16529 === (7))){
var inst_16509 = (state_16528[(7)]);
var inst_16509__$1 = (state_16528[(2)]);
var inst_16510 = (inst_16509__$1 == null);
var inst_16511 = cljs.core.not(inst_16510);
var state_16528__$1 = (function (){var statearr_16545 = state_16528;
(statearr_16545[(7)] = inst_16509__$1);

return statearr_16545;
})();
if(inst_16511){
var statearr_16546_18314 = state_16528__$1;
(statearr_16546_18314[(1)] = (8));

} else {
var statearr_16547_18315 = state_16528__$1;
(statearr_16547_18315[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (1))){
var inst_16502 = (0);
var state_16528__$1 = (function (){var statearr_16548 = state_16528;
(statearr_16548[(8)] = inst_16502);

return statearr_16548;
})();
var statearr_16549_18317 = state_16528__$1;
(statearr_16549_18317[(2)] = null);

(statearr_16549_18317[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (4))){
var state_16528__$1 = state_16528;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16528__$1,(7),ch);
} else {
if((state_val_16529 === (6))){
var inst_16523 = (state_16528[(2)]);
var state_16528__$1 = state_16528;
var statearr_16550_18318 = state_16528__$1;
(statearr_16550_18318[(2)] = inst_16523);

(statearr_16550_18318[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (3))){
var inst_16525 = (state_16528[(2)]);
var inst_16526 = cljs.core.async.close_BANG_(out);
var state_16528__$1 = (function (){var statearr_16551 = state_16528;
(statearr_16551[(9)] = inst_16525);

return statearr_16551;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_16528__$1,inst_16526);
} else {
if((state_val_16529 === (2))){
var inst_16502 = (state_16528[(8)]);
var inst_16505 = (inst_16502 < n);
var state_16528__$1 = state_16528;
if(cljs.core.truth_(inst_16505)){
var statearr_16552_18319 = state_16528__$1;
(statearr_16552_18319[(1)] = (4));

} else {
var statearr_16553_18320 = state_16528__$1;
(statearr_16553_18320[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (11))){
var inst_16502 = (state_16528[(8)]);
var inst_16514 = (state_16528[(2)]);
var inst_16515 = (inst_16502 + (1));
var inst_16502__$1 = inst_16515;
var state_16528__$1 = (function (){var statearr_16554 = state_16528;
(statearr_16554[(10)] = inst_16514);

(statearr_16554[(8)] = inst_16502__$1);

return statearr_16554;
})();
var statearr_16556_18321 = state_16528__$1;
(statearr_16556_18321[(2)] = null);

(statearr_16556_18321[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (9))){
var state_16528__$1 = state_16528;
var statearr_16558_18322 = state_16528__$1;
(statearr_16558_18322[(2)] = null);

(statearr_16558_18322[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (5))){
var state_16528__$1 = state_16528;
var statearr_16559_18323 = state_16528__$1;
(statearr_16559_18323[(2)] = null);

(statearr_16559_18323[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (10))){
var inst_16520 = (state_16528[(2)]);
var state_16528__$1 = state_16528;
var statearr_16560_18324 = state_16528__$1;
(statearr_16560_18324[(2)] = inst_16520);

(statearr_16560_18324[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16529 === (8))){
var inst_16509 = (state_16528[(7)]);
var state_16528__$1 = state_16528;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16528__$1,(11),out,inst_16509);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_16561 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_16561[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_16561[(1)] = (1));

return statearr_16561;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16528){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16528);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16564){var ex__12399__auto__ = e16564;
var statearr_16565_18327 = state_16528;
(statearr_16565_18327[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16528[(4)]))){
var statearr_16566_18328 = state_16528;
(statearr_16566_18328[(1)] = cljs.core.first((state_16528[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18329 = state_16528;
state_16528 = G__18329;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16528){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16528);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16570 = f__13503__auto__();
(statearr_16570[(6)] = c__13502__auto___18312);

return statearr_16570;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.take.cljs$lang$maxFixedArity = 3);


/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Handler}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async16601 = (function (f,ch,meta16599,_,fn1,meta16602){
this.f = f;
this.ch = ch;
this.meta16599 = meta16599;
this._ = _;
this.fn1 = fn1;
this.meta16602 = meta16602;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_16603,meta16602__$1){
var self__ = this;
var _16603__$1 = this;
return (new cljs.core.async.t_cljs$core$async16601(self__.f,self__.ch,self__.meta16599,self__._,self__.fn1,meta16602__$1));
}));

(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_16603){
var self__ = this;
var _16603__$1 = this;
return self__.meta16602;
}));

(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return cljs.core.async.impl.protocols.active_QMARK_(self__.fn1);
}));

(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async16601.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
var f1 = cljs.core.async.impl.protocols.commit(self__.fn1);
return (function (p1__16571_SHARP_){
var G__16609 = (((p1__16571_SHARP_ == null))?null:(self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(p1__16571_SHARP_) : self__.f.call(null,p1__16571_SHARP_)));
return (f1.cljs$core$IFn$_invoke$arity$1 ? f1.cljs$core$IFn$_invoke$arity$1(G__16609) : f1.call(null,G__16609));
});
}));

(cljs.core.async.t_cljs$core$async16601.getBasis = (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta16599","meta16599",1572449777,null),cljs.core.with_meta(new cljs.core.Symbol(null,"_","_",-1201019570,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol("cljs.core.async","t_cljs$core$async16598","cljs.core.async/t_cljs$core$async16598",1154284394,null)], null)),new cljs.core.Symbol(null,"fn1","fn1",895834444,null),new cljs.core.Symbol(null,"meta16602","meta16602",2029699167,null)], null);
}));

(cljs.core.async.t_cljs$core$async16601.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async16601.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async16601");

(cljs.core.async.t_cljs$core$async16601.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async16601");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async16601.
 */
cljs.core.async.__GT_t_cljs$core$async16601 = (function cljs$core$async$__GT_t_cljs$core$async16601(f,ch,meta16599,_,fn1,meta16602){
return (new cljs.core.async.t_cljs$core$async16601(f,ch,meta16599,_,fn1,meta16602));
});



/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async16598 = (function (f,ch,meta16599){
this.f = f;
this.ch = ch;
this.meta16599 = meta16599;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_16600,meta16599__$1){
var self__ = this;
var _16600__$1 = this;
return (new cljs.core.async.t_cljs$core$async16598(self__.f,self__.ch,meta16599__$1));
}));

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_16600){
var self__ = this;
var _16600__$1 = this;
return self__.meta16599;
}));

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
var ret = cljs.core.async.impl.protocols.take_BANG_(self__.ch,(new cljs.core.async.t_cljs$core$async16601(self__.f,self__.ch,self__.meta16599,___$1,fn1,cljs.core.PersistentArrayMap.EMPTY)));
if(cljs.core.truth_((function (){var and__5140__auto__ = ret;
if(cljs.core.truth_(and__5140__auto__)){
return (!((cljs.core.deref(ret) == null)));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.async.impl.channels.box((function (){var G__16620 = cljs.core.deref(ret);
return (self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(G__16620) : self__.f.call(null,G__16620));
})());
} else {
return ret;
}
}));

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16598.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,val,fn1);
}));

(cljs.core.async.t_cljs$core$async16598.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta16599","meta16599",1572449777,null)], null);
}));

(cljs.core.async.t_cljs$core$async16598.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async16598.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async16598");

(cljs.core.async.t_cljs$core$async16598.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async16598");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async16598.
 */
cljs.core.async.__GT_t_cljs$core$async16598 = (function cljs$core$async$__GT_t_cljs$core$async16598(f,ch,meta16599){
return (new cljs.core.async.t_cljs$core$async16598(f,ch,meta16599));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_LT_ = (function cljs$core$async$map_LT_(f,ch){
return (new cljs.core.async.t_cljs$core$async16598(f,ch,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async16623 = (function (f,ch,meta16624){
this.f = f;
this.ch = ch;
this.meta16624 = meta16624;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_16625,meta16624__$1){
var self__ = this;
var _16625__$1 = this;
return (new cljs.core.async.t_cljs$core$async16623(self__.f,self__.ch,meta16624__$1));
}));

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_16625){
var self__ = this;
var _16625__$1 = this;
return self__.meta16624;
}));

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_(self__.ch,fn1);
}));

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16623.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,(self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(val) : self__.f.call(null,val)),fn1);
}));

(cljs.core.async.t_cljs$core$async16623.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta16624","meta16624",505563066,null)], null);
}));

(cljs.core.async.t_cljs$core$async16623.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async16623.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async16623");

(cljs.core.async.t_cljs$core$async16623.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async16623");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async16623.
 */
cljs.core.async.__GT_t_cljs$core$async16623 = (function cljs$core$async$__GT_t_cljs$core$async16623(f,ch,meta16624){
return (new cljs.core.async.t_cljs$core$async16623(f,ch,meta16624));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_GT_ = (function cljs$core$async$map_GT_(f,ch){
return (new cljs.core.async.t_cljs$core$async16623(f,ch,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async16626 = (function (p,ch,meta16627){
this.p = p;
this.ch = ch;
this.meta16627 = meta16627;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_16628,meta16627__$1){
var self__ = this;
var _16628__$1 = this;
return (new cljs.core.async.t_cljs$core$async16626(self__.p,self__.ch,meta16627__$1));
}));

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_16628){
var self__ = this;
var _16628__$1 = this;
return self__.meta16627;
}));

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_(self__.ch,fn1);
}));

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async16626.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_((self__.p.cljs$core$IFn$_invoke$arity$1 ? self__.p.cljs$core$IFn$_invoke$arity$1(val) : self__.p.call(null,val)))){
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,val,fn1);
} else {
return cljs.core.async.impl.channels.box(cljs.core.not(cljs.core.async.impl.protocols.closed_QMARK_(self__.ch)));
}
}));

(cljs.core.async.t_cljs$core$async16626.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"p","p",1791580836,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta16627","meta16627",-84741538,null)], null);
}));

(cljs.core.async.t_cljs$core$async16626.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async16626.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async16626");

(cljs.core.async.t_cljs$core$async16626.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async16626");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async16626.
 */
cljs.core.async.__GT_t_cljs$core$async16626 = (function cljs$core$async$__GT_t_cljs$core$async16626(p,ch,meta16627){
return (new cljs.core.async.t_cljs$core$async16626(p,ch,meta16627));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.filter_GT_ = (function cljs$core$async$filter_GT_(p,ch){
return (new cljs.core.async.t_cljs$core$async16626(p,ch,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.remove_GT_ = (function cljs$core$async$remove_GT_(p,ch){
return cljs.core.async.filter_GT_(cljs.core.complement(p),ch);
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.filter_LT_ = (function cljs$core$async$filter_LT_(var_args){
var G__16645 = arguments.length;
switch (G__16645) {
case 2:
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3(p,ch,null);
}));

(cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3 = (function (p,ch,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18345 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16671){
var state_val_16672 = (state_16671[(1)]);
if((state_val_16672 === (7))){
var inst_16667 = (state_16671[(2)]);
var state_16671__$1 = state_16671;
var statearr_16676_18346 = state_16671__$1;
(statearr_16676_18346[(2)] = inst_16667);

(statearr_16676_18346[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (1))){
var state_16671__$1 = state_16671;
var statearr_16677_18347 = state_16671__$1;
(statearr_16677_18347[(2)] = null);

(statearr_16677_18347[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (4))){
var inst_16653 = (state_16671[(7)]);
var inst_16653__$1 = (state_16671[(2)]);
var inst_16654 = (inst_16653__$1 == null);
var state_16671__$1 = (function (){var statearr_16681 = state_16671;
(statearr_16681[(7)] = inst_16653__$1);

return statearr_16681;
})();
if(cljs.core.truth_(inst_16654)){
var statearr_16682_18348 = state_16671__$1;
(statearr_16682_18348[(1)] = (5));

} else {
var statearr_16683_18349 = state_16671__$1;
(statearr_16683_18349[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (6))){
var inst_16653 = (state_16671[(7)]);
var inst_16658 = (p.cljs$core$IFn$_invoke$arity$1 ? p.cljs$core$IFn$_invoke$arity$1(inst_16653) : p.call(null,inst_16653));
var state_16671__$1 = state_16671;
if(cljs.core.truth_(inst_16658)){
var statearr_16684_18353 = state_16671__$1;
(statearr_16684_18353[(1)] = (8));

} else {
var statearr_16685_18354 = state_16671__$1;
(statearr_16685_18354[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (3))){
var inst_16669 = (state_16671[(2)]);
var state_16671__$1 = state_16671;
return cljs.core.async.impl.ioc_helpers.return_chan(state_16671__$1,inst_16669);
} else {
if((state_val_16672 === (2))){
var state_16671__$1 = state_16671;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16671__$1,(4),ch);
} else {
if((state_val_16672 === (11))){
var inst_16661 = (state_16671[(2)]);
var state_16671__$1 = state_16671;
var statearr_16686_18356 = state_16671__$1;
(statearr_16686_18356[(2)] = inst_16661);

(statearr_16686_18356[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (9))){
var state_16671__$1 = state_16671;
var statearr_16687_18357 = state_16671__$1;
(statearr_16687_18357[(2)] = null);

(statearr_16687_18357[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (5))){
var inst_16656 = cljs.core.async.close_BANG_(out);
var state_16671__$1 = state_16671;
var statearr_16689_18358 = state_16671__$1;
(statearr_16689_18358[(2)] = inst_16656);

(statearr_16689_18358[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (10))){
var inst_16664 = (state_16671[(2)]);
var state_16671__$1 = (function (){var statearr_16690 = state_16671;
(statearr_16690[(8)] = inst_16664);

return statearr_16690;
})();
var statearr_16691_18359 = state_16671__$1;
(statearr_16691_18359[(2)] = null);

(statearr_16691_18359[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16672 === (8))){
var inst_16653 = (state_16671[(7)]);
var state_16671__$1 = state_16671;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16671__$1,(11),out,inst_16653);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_16692 = [null,null,null,null,null,null,null,null,null];
(statearr_16692[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_16692[(1)] = (1));

return statearr_16692;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16671){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16671);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16693){var ex__12399__auto__ = e16693;
var statearr_16694_18363 = state_16671;
(statearr_16694_18363[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16671[(4)]))){
var statearr_16695_18364 = state_16671;
(statearr_16695_18364[(1)] = cljs.core.first((state_16671[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18365 = state_16671;
state_16671 = G__18365;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16671){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16671);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16696 = f__13503__auto__();
(statearr_16696[(6)] = c__13502__auto___18345);

return statearr_16696;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.filter_LT_.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.remove_LT_ = (function cljs$core$async$remove_LT_(var_args){
var G__16700 = arguments.length;
switch (G__16700) {
case 2:
return cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$3(p,ch,null);
}));

(cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$3 = (function (p,ch,buf_or_n){
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3(cljs.core.complement(p),ch,buf_or_n);
}));

(cljs.core.async.remove_LT_.cljs$lang$maxFixedArity = 3);

cljs.core.async.mapcat_STAR_ = (function cljs$core$async$mapcat_STAR_(f,in$,out){
var c__13502__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16765){
var state_val_16766 = (state_16765[(1)]);
if((state_val_16766 === (7))){
var inst_16761 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
var statearr_16768_18374 = state_16765__$1;
(statearr_16768_18374[(2)] = inst_16761);

(statearr_16768_18374[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (20))){
var inst_16731 = (state_16765[(7)]);
var inst_16742 = (state_16765[(2)]);
var inst_16743 = cljs.core.next(inst_16731);
var inst_16716 = inst_16743;
var inst_16717 = null;
var inst_16718 = (0);
var inst_16719 = (0);
var state_16765__$1 = (function (){var statearr_16769 = state_16765;
(statearr_16769[(8)] = inst_16742);

(statearr_16769[(9)] = inst_16716);

(statearr_16769[(10)] = inst_16717);

(statearr_16769[(11)] = inst_16718);

(statearr_16769[(12)] = inst_16719);

return statearr_16769;
})();
var statearr_16770_18377 = state_16765__$1;
(statearr_16770_18377[(2)] = null);

(statearr_16770_18377[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (1))){
var state_16765__$1 = state_16765;
var statearr_16771_18379 = state_16765__$1;
(statearr_16771_18379[(2)] = null);

(statearr_16771_18379[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (4))){
var inst_16705 = (state_16765[(13)]);
var inst_16705__$1 = (state_16765[(2)]);
var inst_16706 = (inst_16705__$1 == null);
var state_16765__$1 = (function (){var statearr_16772 = state_16765;
(statearr_16772[(13)] = inst_16705__$1);

return statearr_16772;
})();
if(cljs.core.truth_(inst_16706)){
var statearr_16773_18381 = state_16765__$1;
(statearr_16773_18381[(1)] = (5));

} else {
var statearr_16780_18382 = state_16765__$1;
(statearr_16780_18382[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (15))){
var state_16765__$1 = state_16765;
var statearr_16792_18383 = state_16765__$1;
(statearr_16792_18383[(2)] = null);

(statearr_16792_18383[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (21))){
var state_16765__$1 = state_16765;
var statearr_16798_18384 = state_16765__$1;
(statearr_16798_18384[(2)] = null);

(statearr_16798_18384[(1)] = (23));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (13))){
var inst_16719 = (state_16765[(12)]);
var inst_16716 = (state_16765[(9)]);
var inst_16717 = (state_16765[(10)]);
var inst_16718 = (state_16765[(11)]);
var inst_16726 = (state_16765[(2)]);
var inst_16727 = (inst_16719 + (1));
var tmp16785 = inst_16716;
var tmp16786 = inst_16717;
var tmp16787 = inst_16718;
var inst_16716__$1 = tmp16785;
var inst_16717__$1 = tmp16786;
var inst_16718__$1 = tmp16787;
var inst_16719__$1 = inst_16727;
var state_16765__$1 = (function (){var statearr_16802 = state_16765;
(statearr_16802[(14)] = inst_16726);

(statearr_16802[(9)] = inst_16716__$1);

(statearr_16802[(10)] = inst_16717__$1);

(statearr_16802[(11)] = inst_16718__$1);

(statearr_16802[(12)] = inst_16719__$1);

return statearr_16802;
})();
var statearr_16803_18386 = state_16765__$1;
(statearr_16803_18386[(2)] = null);

(statearr_16803_18386[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (22))){
var state_16765__$1 = state_16765;
var statearr_16816_18388 = state_16765__$1;
(statearr_16816_18388[(2)] = null);

(statearr_16816_18388[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (6))){
var inst_16705 = (state_16765[(13)]);
var inst_16714 = (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(inst_16705) : f.call(null,inst_16705));
var inst_16715 = cljs.core.seq(inst_16714);
var inst_16716 = inst_16715;
var inst_16717 = null;
var inst_16718 = (0);
var inst_16719 = (0);
var state_16765__$1 = (function (){var statearr_16821 = state_16765;
(statearr_16821[(9)] = inst_16716);

(statearr_16821[(10)] = inst_16717);

(statearr_16821[(11)] = inst_16718);

(statearr_16821[(12)] = inst_16719);

return statearr_16821;
})();
var statearr_16822_18390 = state_16765__$1;
(statearr_16822_18390[(2)] = null);

(statearr_16822_18390[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (17))){
var inst_16731 = (state_16765[(7)]);
var inst_16735 = cljs.core.chunk_first(inst_16731);
var inst_16736 = cljs.core.chunk_rest(inst_16731);
var inst_16737 = cljs.core.count(inst_16735);
var inst_16716 = inst_16736;
var inst_16717 = inst_16735;
var inst_16718 = inst_16737;
var inst_16719 = (0);
var state_16765__$1 = (function (){var statearr_16825 = state_16765;
(statearr_16825[(9)] = inst_16716);

(statearr_16825[(10)] = inst_16717);

(statearr_16825[(11)] = inst_16718);

(statearr_16825[(12)] = inst_16719);

return statearr_16825;
})();
var statearr_16826_18392 = state_16765__$1;
(statearr_16826_18392[(2)] = null);

(statearr_16826_18392[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (3))){
var inst_16763 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
return cljs.core.async.impl.ioc_helpers.return_chan(state_16765__$1,inst_16763);
} else {
if((state_val_16766 === (12))){
var inst_16751 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
var statearr_16827_18397 = state_16765__$1;
(statearr_16827_18397[(2)] = inst_16751);

(statearr_16827_18397[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (2))){
var state_16765__$1 = state_16765;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16765__$1,(4),in$);
} else {
if((state_val_16766 === (23))){
var inst_16759 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
var statearr_16828_18398 = state_16765__$1;
(statearr_16828_18398[(2)] = inst_16759);

(statearr_16828_18398[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (19))){
var inst_16746 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
var statearr_16829_18399 = state_16765__$1;
(statearr_16829_18399[(2)] = inst_16746);

(statearr_16829_18399[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (11))){
var inst_16716 = (state_16765[(9)]);
var inst_16731 = (state_16765[(7)]);
var inst_16731__$1 = cljs.core.seq(inst_16716);
var state_16765__$1 = (function (){var statearr_16830 = state_16765;
(statearr_16830[(7)] = inst_16731__$1);

return statearr_16830;
})();
if(inst_16731__$1){
var statearr_16832_18400 = state_16765__$1;
(statearr_16832_18400[(1)] = (14));

} else {
var statearr_16833_18401 = state_16765__$1;
(statearr_16833_18401[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (9))){
var inst_16753 = (state_16765[(2)]);
var inst_16754 = cljs.core.async.impl.protocols.closed_QMARK_(out);
var state_16765__$1 = (function (){var statearr_16834 = state_16765;
(statearr_16834[(15)] = inst_16753);

return statearr_16834;
})();
if(cljs.core.truth_(inst_16754)){
var statearr_16835_18402 = state_16765__$1;
(statearr_16835_18402[(1)] = (21));

} else {
var statearr_16836_18403 = state_16765__$1;
(statearr_16836_18403[(1)] = (22));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (5))){
var inst_16708 = cljs.core.async.close_BANG_(out);
var state_16765__$1 = state_16765;
var statearr_16837_18404 = state_16765__$1;
(statearr_16837_18404[(2)] = inst_16708);

(statearr_16837_18404[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (14))){
var inst_16731 = (state_16765[(7)]);
var inst_16733 = cljs.core.chunked_seq_QMARK_(inst_16731);
var state_16765__$1 = state_16765;
if(inst_16733){
var statearr_16838_18405 = state_16765__$1;
(statearr_16838_18405[(1)] = (17));

} else {
var statearr_16839_18406 = state_16765__$1;
(statearr_16839_18406[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (16))){
var inst_16749 = (state_16765[(2)]);
var state_16765__$1 = state_16765;
var statearr_16840_18407 = state_16765__$1;
(statearr_16840_18407[(2)] = inst_16749);

(statearr_16840_18407[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16766 === (10))){
var inst_16717 = (state_16765[(10)]);
var inst_16719 = (state_16765[(12)]);
var inst_16724 = cljs.core._nth(inst_16717,inst_16719);
var state_16765__$1 = state_16765;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16765__$1,(13),out,inst_16724);
} else {
if((state_val_16766 === (18))){
var inst_16731 = (state_16765[(7)]);
var inst_16740 = cljs.core.first(inst_16731);
var state_16765__$1 = state_16765;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16765__$1,(20),out,inst_16740);
} else {
if((state_val_16766 === (8))){
var inst_16719 = (state_16765[(12)]);
var inst_16718 = (state_16765[(11)]);
var inst_16721 = (inst_16719 < inst_16718);
var inst_16722 = inst_16721;
var state_16765__$1 = state_16765;
if(cljs.core.truth_(inst_16722)){
var statearr_16849_18408 = state_16765__$1;
(statearr_16849_18408[(1)] = (10));

} else {
var statearr_16850_18409 = state_16765__$1;
(statearr_16850_18409[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__ = null;
var cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____0 = (function (){
var statearr_16880 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_16880[(0)] = cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__);

(statearr_16880[(1)] = (1));

return statearr_16880;
});
var cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____1 = (function (state_16765){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16765);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e16881){var ex__12399__auto__ = e16881;
var statearr_16882_18410 = state_16765;
(statearr_16882_18410[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16765[(4)]))){
var statearr_16883_18411 = state_16765;
(statearr_16883_18411[(1)] = cljs.core.first((state_16765[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18412 = state_16765;
state_16765 = G__18412;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__ = function(state_16765){
switch(arguments.length){
case 0:
return cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____1.call(this,state_16765);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____0;
cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mapcat_STAR__$_state_machine__12396__auto____1;
return cljs$core$async$mapcat_STAR__$_state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_16892 = f__13503__auto__();
(statearr_16892[(6)] = c__13502__auto__);

return statearr_16892;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));

return c__13502__auto__;
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.mapcat_LT_ = (function cljs$core$async$mapcat_LT_(var_args){
var G__16897 = arguments.length;
switch (G__16897) {
case 2:
return cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$2 = (function (f,in$){
return cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$3(f,in$,null);
}));

(cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$3 = (function (f,in$,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
cljs.core.async.mapcat_STAR_(f,in$,out);

return out;
}));

(cljs.core.async.mapcat_LT_.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.mapcat_GT_ = (function cljs$core$async$mapcat_GT_(var_args){
var G__16906 = arguments.length;
switch (G__16906) {
case 2:
return cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$2 = (function (f,out){
return cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$3(f,out,null);
}));

(cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$3 = (function (f,out,buf_or_n){
var in$ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
cljs.core.async.mapcat_STAR_(f,in$,out);

return in$;
}));

(cljs.core.async.mapcat_GT_.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.unique = (function cljs$core$async$unique(var_args){
var G__16921 = arguments.length;
switch (G__16921) {
case 1:
return cljs.core.async.unique.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unique.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.unique.cljs$core$IFn$_invoke$arity$1 = (function (ch){
return cljs.core.async.unique.cljs$core$IFn$_invoke$arity$2(ch,null);
}));

(cljs.core.async.unique.cljs$core$IFn$_invoke$arity$2 = (function (ch,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18418 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_16956){
var state_val_16957 = (state_16956[(1)]);
if((state_val_16957 === (7))){
var inst_16951 = (state_16956[(2)]);
var state_16956__$1 = state_16956;
var statearr_16965_18420 = state_16956__$1;
(statearr_16965_18420[(2)] = inst_16951);

(statearr_16965_18420[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (1))){
var inst_16931 = null;
var state_16956__$1 = (function (){var statearr_16966 = state_16956;
(statearr_16966[(7)] = inst_16931);

return statearr_16966;
})();
var statearr_16967_18421 = state_16956__$1;
(statearr_16967_18421[(2)] = null);

(statearr_16967_18421[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (4))){
var inst_16934 = (state_16956[(8)]);
var inst_16934__$1 = (state_16956[(2)]);
var inst_16935 = (inst_16934__$1 == null);
var inst_16936 = cljs.core.not(inst_16935);
var state_16956__$1 = (function (){var statearr_16968 = state_16956;
(statearr_16968[(8)] = inst_16934__$1);

return statearr_16968;
})();
if(inst_16936){
var statearr_16969_18423 = state_16956__$1;
(statearr_16969_18423[(1)] = (5));

} else {
var statearr_16970_18424 = state_16956__$1;
(statearr_16970_18424[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (6))){
var state_16956__$1 = state_16956;
var statearr_16975_18425 = state_16956__$1;
(statearr_16975_18425[(2)] = null);

(statearr_16975_18425[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (3))){
var inst_16953 = (state_16956[(2)]);
var inst_16954 = cljs.core.async.close_BANG_(out);
var state_16956__$1 = (function (){var statearr_16980 = state_16956;
(statearr_16980[(9)] = inst_16953);

return statearr_16980;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_16956__$1,inst_16954);
} else {
if((state_val_16957 === (2))){
var state_16956__$1 = state_16956;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_16956__$1,(4),ch);
} else {
if((state_val_16957 === (11))){
var inst_16934 = (state_16956[(8)]);
var inst_16945 = (state_16956[(2)]);
var inst_16931 = inst_16934;
var state_16956__$1 = (function (){var statearr_17010 = state_16956;
(statearr_17010[(10)] = inst_16945);

(statearr_17010[(7)] = inst_16931);

return statearr_17010;
})();
var statearr_17011_18426 = state_16956__$1;
(statearr_17011_18426[(2)] = null);

(statearr_17011_18426[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (9))){
var inst_16934 = (state_16956[(8)]);
var state_16956__$1 = state_16956;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_16956__$1,(11),out,inst_16934);
} else {
if((state_val_16957 === (5))){
var inst_16934 = (state_16956[(8)]);
var inst_16931 = (state_16956[(7)]);
var inst_16940 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_16934,inst_16931);
var state_16956__$1 = state_16956;
if(inst_16940){
var statearr_17016_18428 = state_16956__$1;
(statearr_17016_18428[(1)] = (8));

} else {
var statearr_17022_18429 = state_16956__$1;
(statearr_17022_18429[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (10))){
var inst_16948 = (state_16956[(2)]);
var state_16956__$1 = state_16956;
var statearr_17032_18430 = state_16956__$1;
(statearr_17032_18430[(2)] = inst_16948);

(statearr_17032_18430[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_16957 === (8))){
var inst_16931 = (state_16956[(7)]);
var tmp17015 = inst_16931;
var inst_16931__$1 = tmp17015;
var state_16956__$1 = (function (){var statearr_17033 = state_16956;
(statearr_17033[(7)] = inst_16931__$1);

return statearr_17033;
})();
var statearr_17034_18432 = state_16956__$1;
(statearr_17034_18432[(2)] = null);

(statearr_17034_18432[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_17045 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_17045[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_17045[(1)] = (1));

return statearr_17045;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_16956){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_16956);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e17060){var ex__12399__auto__ = e17060;
var statearr_17061_18433 = state_16956;
(statearr_17061_18433[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_16956[(4)]))){
var statearr_17062_18434 = state_16956;
(statearr_17062_18434[(1)] = cljs.core.first((state_16956[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18439 = state_16956;
state_16956 = G__18439;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_16956){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_16956);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_17063 = f__13503__auto__();
(statearr_17063[(6)] = c__13502__auto___18418);

return statearr_17063;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.unique.cljs$lang$maxFixedArity = 2);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition = (function cljs$core$async$partition(var_args){
var G__17072 = arguments.length;
switch (G__17072) {
case 2:
return cljs.core.async.partition.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.partition.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.partition.cljs$core$IFn$_invoke$arity$2 = (function (n,ch){
return cljs.core.async.partition.cljs$core$IFn$_invoke$arity$3(n,ch,null);
}));

(cljs.core.async.partition.cljs$core$IFn$_invoke$arity$3 = (function (n,ch,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18441 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_17147){
var state_val_17154 = (state_17147[(1)]);
if((state_val_17154 === (7))){
var inst_17139 = (state_17147[(2)]);
var state_17147__$1 = state_17147;
var statearr_17163_18443 = state_17147__$1;
(statearr_17163_18443[(2)] = inst_17139);

(statearr_17163_18443[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (1))){
var inst_17098 = (new Array(n));
var inst_17099 = inst_17098;
var inst_17100 = (0);
var state_17147__$1 = (function (){var statearr_17164 = state_17147;
(statearr_17164[(7)] = inst_17099);

(statearr_17164[(8)] = inst_17100);

return statearr_17164;
})();
var statearr_17165_18445 = state_17147__$1;
(statearr_17165_18445[(2)] = null);

(statearr_17165_18445[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (4))){
var inst_17106 = (state_17147[(9)]);
var inst_17106__$1 = (state_17147[(2)]);
var inst_17107 = (inst_17106__$1 == null);
var inst_17108 = cljs.core.not(inst_17107);
var state_17147__$1 = (function (){var statearr_17167 = state_17147;
(statearr_17167[(9)] = inst_17106__$1);

return statearr_17167;
})();
if(inst_17108){
var statearr_17170_18446 = state_17147__$1;
(statearr_17170_18446[(1)] = (5));

} else {
var statearr_17171_18447 = state_17147__$1;
(statearr_17171_18447[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (15))){
var inst_17133 = (state_17147[(2)]);
var state_17147__$1 = state_17147;
var statearr_17173_18448 = state_17147__$1;
(statearr_17173_18448[(2)] = inst_17133);

(statearr_17173_18448[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (13))){
var state_17147__$1 = state_17147;
var statearr_17177_18449 = state_17147__$1;
(statearr_17177_18449[(2)] = null);

(statearr_17177_18449[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (6))){
var inst_17100 = (state_17147[(8)]);
var inst_17128 = (inst_17100 > (0));
var state_17147__$1 = state_17147;
if(cljs.core.truth_(inst_17128)){
var statearr_17178_18450 = state_17147__$1;
(statearr_17178_18450[(1)] = (12));

} else {
var statearr_17179_18451 = state_17147__$1;
(statearr_17179_18451[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (3))){
var inst_17145 = (state_17147[(2)]);
var state_17147__$1 = state_17147;
return cljs.core.async.impl.ioc_helpers.return_chan(state_17147__$1,inst_17145);
} else {
if((state_val_17154 === (12))){
var inst_17099 = (state_17147[(7)]);
var inst_17131 = cljs.core.vec(inst_17099);
var state_17147__$1 = state_17147;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_17147__$1,(15),out,inst_17131);
} else {
if((state_val_17154 === (2))){
var state_17147__$1 = state_17147;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_17147__$1,(4),ch);
} else {
if((state_val_17154 === (11))){
var inst_17122 = (state_17147[(2)]);
var inst_17123 = (new Array(n));
var inst_17099 = inst_17123;
var inst_17100 = (0);
var state_17147__$1 = (function (){var statearr_17182 = state_17147;
(statearr_17182[(10)] = inst_17122);

(statearr_17182[(7)] = inst_17099);

(statearr_17182[(8)] = inst_17100);

return statearr_17182;
})();
var statearr_17183_18452 = state_17147__$1;
(statearr_17183_18452[(2)] = null);

(statearr_17183_18452[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (9))){
var inst_17099 = (state_17147[(7)]);
var inst_17120 = cljs.core.vec(inst_17099);
var state_17147__$1 = state_17147;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_17147__$1,(11),out,inst_17120);
} else {
if((state_val_17154 === (5))){
var inst_17099 = (state_17147[(7)]);
var inst_17100 = (state_17147[(8)]);
var inst_17106 = (state_17147[(9)]);
var inst_17115 = (state_17147[(11)]);
var inst_17114 = (inst_17099[inst_17100] = inst_17106);
var inst_17115__$1 = (inst_17100 + (1));
var inst_17116 = (inst_17115__$1 < n);
var state_17147__$1 = (function (){var statearr_17184 = state_17147;
(statearr_17184[(12)] = inst_17114);

(statearr_17184[(11)] = inst_17115__$1);

return statearr_17184;
})();
if(cljs.core.truth_(inst_17116)){
var statearr_17188_18453 = state_17147__$1;
(statearr_17188_18453[(1)] = (8));

} else {
var statearr_17189_18454 = state_17147__$1;
(statearr_17189_18454[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (14))){
var inst_17136 = (state_17147[(2)]);
var inst_17137 = cljs.core.async.close_BANG_(out);
var state_17147__$1 = (function (){var statearr_17192 = state_17147;
(statearr_17192[(13)] = inst_17136);

return statearr_17192;
})();
var statearr_17193_18455 = state_17147__$1;
(statearr_17193_18455[(2)] = inst_17137);

(statearr_17193_18455[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (10))){
var inst_17126 = (state_17147[(2)]);
var state_17147__$1 = state_17147;
var statearr_17195_18456 = state_17147__$1;
(statearr_17195_18456[(2)] = inst_17126);

(statearr_17195_18456[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17154 === (8))){
var inst_17099 = (state_17147[(7)]);
var inst_17115 = (state_17147[(11)]);
var tmp17191 = inst_17099;
var inst_17099__$1 = tmp17191;
var inst_17100 = inst_17115;
var state_17147__$1 = (function (){var statearr_17196 = state_17147;
(statearr_17196[(7)] = inst_17099__$1);

(statearr_17196[(8)] = inst_17100);

return statearr_17196;
})();
var statearr_17197_18457 = state_17147__$1;
(statearr_17197_18457[(2)] = null);

(statearr_17197_18457[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_17199 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_17199[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_17199[(1)] = (1));

return statearr_17199;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_17147){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_17147);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e17200){var ex__12399__auto__ = e17200;
var statearr_17201_18462 = state_17147;
(statearr_17201_18462[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_17147[(4)]))){
var statearr_17202_18463 = state_17147;
(statearr_17202_18463[(1)] = cljs.core.first((state_17147[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18464 = state_17147;
state_17147 = G__18464;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_17147){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_17147);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_17203 = f__13503__auto__();
(statearr_17203[(6)] = c__13502__auto___18441);

return statearr_17203;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.partition.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition_by = (function cljs$core$async$partition_by(var_args){
var G__17212 = arguments.length;
switch (G__17212) {
case 2:
return cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$2 = (function (f,ch){
return cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$3(f,ch,null);
}));

(cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$3 = (function (f,ch,buf_or_n){
var out = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1(buf_or_n);
var c__13502__auto___18467 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__13503__auto__ = (function (){var switch__12395__auto__ = (function (state_17272){
var state_val_17273 = (state_17272[(1)]);
if((state_val_17273 === (7))){
var inst_17268 = (state_17272[(2)]);
var state_17272__$1 = state_17272;
var statearr_17275_18468 = state_17272__$1;
(statearr_17275_18468[(2)] = inst_17268);

(statearr_17275_18468[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (1))){
var inst_17225 = [];
var inst_17226 = inst_17225;
var inst_17227 = new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123);
var state_17272__$1 = (function (){var statearr_17276 = state_17272;
(statearr_17276[(7)] = inst_17226);

(statearr_17276[(8)] = inst_17227);

return statearr_17276;
})();
var statearr_17281_18469 = state_17272__$1;
(statearr_17281_18469[(2)] = null);

(statearr_17281_18469[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (4))){
var inst_17231 = (state_17272[(9)]);
var inst_17231__$1 = (state_17272[(2)]);
var inst_17232 = (inst_17231__$1 == null);
var inst_17233 = cljs.core.not(inst_17232);
var state_17272__$1 = (function (){var statearr_17282 = state_17272;
(statearr_17282[(9)] = inst_17231__$1);

return statearr_17282;
})();
if(inst_17233){
var statearr_17283_18471 = state_17272__$1;
(statearr_17283_18471[(1)] = (5));

} else {
var statearr_17284_18472 = state_17272__$1;
(statearr_17284_18472[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (15))){
var inst_17226 = (state_17272[(7)]);
var inst_17260 = cljs.core.vec(inst_17226);
var state_17272__$1 = state_17272;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_17272__$1,(18),out,inst_17260);
} else {
if((state_val_17273 === (13))){
var inst_17254 = (state_17272[(2)]);
var state_17272__$1 = state_17272;
var statearr_17289_18474 = state_17272__$1;
(statearr_17289_18474[(2)] = inst_17254);

(statearr_17289_18474[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (6))){
var inst_17226 = (state_17272[(7)]);
var inst_17257 = inst_17226.length;
var inst_17258 = (inst_17257 > (0));
var state_17272__$1 = state_17272;
if(cljs.core.truth_(inst_17258)){
var statearr_17291_18475 = state_17272__$1;
(statearr_17291_18475[(1)] = (15));

} else {
var statearr_17292_18476 = state_17272__$1;
(statearr_17292_18476[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (17))){
var inst_17265 = (state_17272[(2)]);
var inst_17266 = cljs.core.async.close_BANG_(out);
var state_17272__$1 = (function (){var statearr_17293 = state_17272;
(statearr_17293[(10)] = inst_17265);

return statearr_17293;
})();
var statearr_17294_18477 = state_17272__$1;
(statearr_17294_18477[(2)] = inst_17266);

(statearr_17294_18477[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (3))){
var inst_17270 = (state_17272[(2)]);
var state_17272__$1 = state_17272;
return cljs.core.async.impl.ioc_helpers.return_chan(state_17272__$1,inst_17270);
} else {
if((state_val_17273 === (12))){
var inst_17226 = (state_17272[(7)]);
var inst_17247 = cljs.core.vec(inst_17226);
var state_17272__$1 = state_17272;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_17272__$1,(14),out,inst_17247);
} else {
if((state_val_17273 === (2))){
var state_17272__$1 = state_17272;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_17272__$1,(4),ch);
} else {
if((state_val_17273 === (11))){
var inst_17226 = (state_17272[(7)]);
var inst_17231 = (state_17272[(9)]);
var inst_17235 = (state_17272[(11)]);
var inst_17244 = inst_17226.push(inst_17231);
var tmp17295 = inst_17226;
var inst_17226__$1 = tmp17295;
var inst_17227 = inst_17235;
var state_17272__$1 = (function (){var statearr_17296 = state_17272;
(statearr_17296[(12)] = inst_17244);

(statearr_17296[(7)] = inst_17226__$1);

(statearr_17296[(8)] = inst_17227);

return statearr_17296;
})();
var statearr_17297_18479 = state_17272__$1;
(statearr_17297_18479[(2)] = null);

(statearr_17297_18479[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (9))){
var inst_17227 = (state_17272[(8)]);
var inst_17239 = cljs.core.keyword_identical_QMARK_(inst_17227,new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123));
var state_17272__$1 = state_17272;
var statearr_17298_18481 = state_17272__$1;
(statearr_17298_18481[(2)] = inst_17239);

(statearr_17298_18481[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (5))){
var inst_17231 = (state_17272[(9)]);
var inst_17235 = (state_17272[(11)]);
var inst_17227 = (state_17272[(8)]);
var inst_17236 = (state_17272[(13)]);
var inst_17235__$1 = (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(inst_17231) : f.call(null,inst_17231));
var inst_17236__$1 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_17235__$1,inst_17227);
var state_17272__$1 = (function (){var statearr_17299 = state_17272;
(statearr_17299[(11)] = inst_17235__$1);

(statearr_17299[(13)] = inst_17236__$1);

return statearr_17299;
})();
if(inst_17236__$1){
var statearr_17300_18482 = state_17272__$1;
(statearr_17300_18482[(1)] = (8));

} else {
var statearr_17301_18483 = state_17272__$1;
(statearr_17301_18483[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (14))){
var inst_17231 = (state_17272[(9)]);
var inst_17235 = (state_17272[(11)]);
var inst_17249 = (state_17272[(2)]);
var inst_17250 = [];
var inst_17251 = inst_17250.push(inst_17231);
var inst_17226 = inst_17250;
var inst_17227 = inst_17235;
var state_17272__$1 = (function (){var statearr_17302 = state_17272;
(statearr_17302[(14)] = inst_17249);

(statearr_17302[(15)] = inst_17251);

(statearr_17302[(7)] = inst_17226);

(statearr_17302[(8)] = inst_17227);

return statearr_17302;
})();
var statearr_17303_18484 = state_17272__$1;
(statearr_17303_18484[(2)] = null);

(statearr_17303_18484[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (16))){
var state_17272__$1 = state_17272;
var statearr_17304_18486 = state_17272__$1;
(statearr_17304_18486[(2)] = null);

(statearr_17304_18486[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (10))){
var inst_17242 = (state_17272[(2)]);
var state_17272__$1 = state_17272;
if(cljs.core.truth_(inst_17242)){
var statearr_17310_18487 = state_17272__$1;
(statearr_17310_18487[(1)] = (11));

} else {
var statearr_17316_18488 = state_17272__$1;
(statearr_17316_18488[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (18))){
var inst_17262 = (state_17272[(2)]);
var state_17272__$1 = state_17272;
var statearr_17322_18489 = state_17272__$1;
(statearr_17322_18489[(2)] = inst_17262);

(statearr_17322_18489[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_17273 === (8))){
var inst_17236 = (state_17272[(13)]);
var state_17272__$1 = state_17272;
var statearr_17331_18490 = state_17272__$1;
(statearr_17331_18490[(2)] = inst_17236);

(statearr_17331_18490[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});
return (function() {
var cljs$core$async$state_machine__12396__auto__ = null;
var cljs$core$async$state_machine__12396__auto____0 = (function (){
var statearr_17332 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_17332[(0)] = cljs$core$async$state_machine__12396__auto__);

(statearr_17332[(1)] = (1));

return statearr_17332;
});
var cljs$core$async$state_machine__12396__auto____1 = (function (state_17272){
while(true){
var ret_value__12397__auto__ = (function (){try{while(true){
var result__12398__auto__ = switch__12395__auto__(state_17272);
if(cljs.core.keyword_identical_QMARK_(result__12398__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__12398__auto__;
}
break;
}
}catch (e17333){var ex__12399__auto__ = e17333;
var statearr_17339_18491 = state_17272;
(statearr_17339_18491[(2)] = ex__12399__auto__);


if(cljs.core.seq((state_17272[(4)]))){
var statearr_17340_18492 = state_17272;
(statearr_17340_18492[(1)] = cljs.core.first((state_17272[(4)])));

} else {
throw ex__12399__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__12397__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__18493 = state_17272;
state_17272 = G__18493;
continue;
} else {
return ret_value__12397__auto__;
}
break;
}
});
cljs$core$async$state_machine__12396__auto__ = function(state_17272){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__12396__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__12396__auto____1.call(this,state_17272);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__12396__auto____0;
cljs$core$async$state_machine__12396__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__12396__auto____1;
return cljs$core$async$state_machine__12396__auto__;
})()
})();
var state__13504__auto__ = (function (){var statearr_17341 = f__13503__auto__();
(statearr_17341[(6)] = c__13502__auto___18467);

return statearr_17341;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__13504__auto__);
}));


return out;
}));

(cljs.core.async.partition_by.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=cljs.core.async.js.map
