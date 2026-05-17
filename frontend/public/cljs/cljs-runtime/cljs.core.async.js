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
cljs.core.async.t_cljs$core$async26649 = (function (f,blockable,meta26650){
this.f = f;
this.blockable = blockable;
this.meta26650 = meta26650;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_26651,meta26650__$1){
var self__ = this;
var _26651__$1 = this;
return (new cljs.core.async.t_cljs$core$async26649(self__.f,self__.blockable,meta26650__$1));
}));

(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_26651){
var self__ = this;
var _26651__$1 = this;
return self__.meta26650;
}));

(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.blockable;
}));

(cljs.core.async.t_cljs$core$async26649.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.f;
}));

(cljs.core.async.t_cljs$core$async26649.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"blockable","blockable",-28395259,null),new cljs.core.Symbol(null,"meta26650","meta26650",-936444520,null)], null);
}));

(cljs.core.async.t_cljs$core$async26649.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async26649.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async26649");

(cljs.core.async.t_cljs$core$async26649.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async26649");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async26649.
 */
cljs.core.async.__GT_t_cljs$core$async26649 = (function cljs$core$async$__GT_t_cljs$core$async26649(f,blockable,meta26650){
return (new cljs.core.async.t_cljs$core$async26649(f,blockable,meta26650));
});


cljs.core.async.fn_handler = (function cljs$core$async$fn_handler(var_args){
var G__26627 = arguments.length;
switch (G__26627) {
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
return (new cljs.core.async.t_cljs$core$async26649(f,blockable,cljs.core.PersistentArrayMap.EMPTY));
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
var G__26724 = arguments.length;
switch (G__26724) {
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
var G__26741 = arguments.length;
switch (G__26741) {
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
var G__26759 = arguments.length;
switch (G__26759) {
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
var val_31123 = cljs.core.deref(ret);
if(cljs.core.truth_(on_caller_QMARK_)){
(fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(val_31123) : fn1.call(null,val_31123));
} else {
cljs.core.async.impl.dispatch.run((function (){
return (fn1.cljs$core$IFn$_invoke$arity$1 ? fn1.cljs$core$IFn$_invoke$arity$1(val_31123) : fn1.call(null,val_31123));
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
var G__26792 = arguments.length;
switch (G__26792) {
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
var n__5741__auto___31140 = n;
var x_31145 = (0);
while(true){
if((x_31145 < n__5741__auto___31140)){
(a[x_31145] = x_31145);

var G__31146 = (x_31145 + (1));
x_31145 = G__31146;
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
cljs.core.async.t_cljs$core$async26875 = (function (flag,meta26876){
this.flag = flag;
this.meta26876 = meta26876;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_26877,meta26876__$1){
var self__ = this;
var _26877__$1 = this;
return (new cljs.core.async.t_cljs$core$async26875(self__.flag,meta26876__$1));
}));

(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_26877){
var self__ = this;
var _26877__$1 = this;
return self__.meta26876;
}));

(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref(self__.flag);
}));

(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async26875.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.flag,null);

return true;
}));

(cljs.core.async.t_cljs$core$async26875.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"meta26876","meta26876",455541875,null)], null);
}));

(cljs.core.async.t_cljs$core$async26875.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async26875.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async26875");

(cljs.core.async.t_cljs$core$async26875.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async26875");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async26875.
 */
cljs.core.async.__GT_t_cljs$core$async26875 = (function cljs$core$async$__GT_t_cljs$core$async26875(flag,meta26876){
return (new cljs.core.async.t_cljs$core$async26875(flag,meta26876));
});


cljs.core.async.alt_flag = (function cljs$core$async$alt_flag(){
var flag = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(true);
return (new cljs.core.async.t_cljs$core$async26875(flag,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Handler}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async26893 = (function (flag,cb,meta26894){
this.flag = flag;
this.cb = cb;
this.meta26894 = meta26894;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_26895,meta26894__$1){
var self__ = this;
var _26895__$1 = this;
return (new cljs.core.async.t_cljs$core$async26893(self__.flag,self__.cb,meta26894__$1));
}));

(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_26895){
var self__ = this;
var _26895__$1 = this;
return self__.meta26894;
}));

(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.active_QMARK_(self__.flag);
}));

(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async26893.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.async.impl.protocols.commit(self__.flag);

return self__.cb;
}));

(cljs.core.async.t_cljs$core$async26893.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"cb","cb",-2064487928,null),new cljs.core.Symbol(null,"meta26894","meta26894",727367749,null)], null);
}));

(cljs.core.async.t_cljs$core$async26893.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async26893.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async26893");

(cljs.core.async.t_cljs$core$async26893.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async26893");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async26893.
 */
cljs.core.async.__GT_t_cljs$core$async26893 = (function cljs$core$async$__GT_t_cljs$core$async26893(flag,cb,meta26894){
return (new cljs.core.async.t_cljs$core$async26893(flag,cb,meta26894));
});


cljs.core.async.alt_handler = (function cljs$core$async$alt_handler(flag,cb){
return (new cljs.core.async.t_cljs$core$async26893(flag,cb,cljs.core.PersistentArrayMap.EMPTY));
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
var port_31174 = cljs.core.nth.cljs$core$IFn$_invoke$arity$2(ports__$1,i);
if(cljs.core.vector_QMARK_(port_31174)){
if((!(((port_31174.cljs$core$IFn$_invoke$arity$1 ? port_31174.cljs$core$IFn$_invoke$arity$1((1)) : port_31174.call(null,(1))) == null)))){
} else {
throw (new Error((""+"Assert failed: "+"can't put nil on channel"+"\n"+"(some? (port 1))")));
}
} else {
}

var G__31178 = (i + (1));
i = G__31178;
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
return (function (p1__26909_SHARP_){
var G__26980 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__26909_SHARP_,wport], null);
return (fret.cljs$core$IFn$_invoke$arity$1 ? fret.cljs$core$IFn$_invoke$arity$1(G__26980) : fret.call(null,G__26980));
});})(i,val,idx,port,wport,flag,ports__$1,n,_,idxs,priority))
));
})():cljs.core.async.impl.protocols.take_BANG_(port,cljs.core.async.alt_handler(flag,((function (i,idx,port,wport,flag,ports__$1,n,_,idxs,priority){
return (function (p1__26911_SHARP_){
var G__27027 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__26911_SHARP_,port], null);
return (fret.cljs$core$IFn$_invoke$arity$1 ? fret.cljs$core$IFn$_invoke$arity$1(G__27027) : fret.call(null,G__27027));
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
var G__31197 = (i + (1));
i = G__31197;
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
var len__5876__auto___31198 = arguments.length;
var i__5877__auto___31199 = (0);
while(true){
if((i__5877__auto___31199 < len__5876__auto___31198)){
args__5882__auto__.push((arguments[i__5877__auto___31199]));

var G__31200 = (i__5877__auto___31199 + (1));
i__5877__auto___31199 = G__31200;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (ports,p__27155){
var map__27156 = p__27155;
var map__27156__$1 = cljs.core.__destructure_map(map__27156);
var opts = map__27156__$1;
throw (new Error("alts! used not in (go ...) block"));
}));

(cljs.core.async.alts_BANG_.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(cljs.core.async.alts_BANG_.cljs$lang$applyTo = (function (seq27109){
var G__27111 = cljs.core.first(seq27109);
var seq27109__$1 = cljs.core.next(seq27109);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__27111,seq27109__$1);
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
var G__27202 = arguments.length;
switch (G__27202) {
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
var c__11733__auto___31207 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_27511){
var state_val_27513 = (state_27511[(1)]);
if((state_val_27513 === (7))){
var inst_27480 = (state_27511[(2)]);
var state_27511__$1 = state_27511;
var statearr_27527_31208 = state_27511__$1;
(statearr_27527_31208[(2)] = inst_27480);

(statearr_27527_31208[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (1))){
var state_27511__$1 = state_27511;
var statearr_27551_31209 = state_27511__$1;
(statearr_27551_31209[(2)] = null);

(statearr_27551_31209[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (4))){
var inst_27431 = (state_27511[(7)]);
var inst_27431__$1 = (state_27511[(2)]);
var inst_27454 = (inst_27431__$1 == null);
var state_27511__$1 = (function (){var statearr_27557 = state_27511;
(statearr_27557[(7)] = inst_27431__$1);

return statearr_27557;
})();
if(cljs.core.truth_(inst_27454)){
var statearr_27570_31210 = state_27511__$1;
(statearr_27570_31210[(1)] = (5));

} else {
var statearr_27583_31211 = state_27511__$1;
(statearr_27583_31211[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (13))){
var state_27511__$1 = state_27511;
var statearr_27584_31212 = state_27511__$1;
(statearr_27584_31212[(2)] = null);

(statearr_27584_31212[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (6))){
var inst_27431 = (state_27511[(7)]);
var state_27511__$1 = state_27511;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_27511__$1,(11),to,inst_27431);
} else {
if((state_val_27513 === (3))){
var inst_27487 = (state_27511[(2)]);
var state_27511__$1 = state_27511;
return cljs.core.async.impl.ioc_helpers.return_chan(state_27511__$1,inst_27487);
} else {
if((state_val_27513 === (12))){
var state_27511__$1 = state_27511;
var statearr_27585_31213 = state_27511__$1;
(statearr_27585_31213[(2)] = null);

(statearr_27585_31213[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (2))){
var state_27511__$1 = state_27511;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_27511__$1,(4),from);
} else {
if((state_val_27513 === (11))){
var inst_27472 = (state_27511[(2)]);
var state_27511__$1 = state_27511;
if(cljs.core.truth_(inst_27472)){
var statearr_27586_31214 = state_27511__$1;
(statearr_27586_31214[(1)] = (12));

} else {
var statearr_27587_31215 = state_27511__$1;
(statearr_27587_31215[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (9))){
var state_27511__$1 = state_27511;
var statearr_27588_31219 = state_27511__$1;
(statearr_27588_31219[(2)] = null);

(statearr_27588_31219[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (5))){
var state_27511__$1 = state_27511;
if(cljs.core.truth_(close_QMARK_)){
var statearr_27589_31220 = state_27511__$1;
(statearr_27589_31220[(1)] = (8));

} else {
var statearr_27590_31221 = state_27511__$1;
(statearr_27590_31221[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (14))){
var inst_27478 = (state_27511[(2)]);
var state_27511__$1 = state_27511;
var statearr_27592_31222 = state_27511__$1;
(statearr_27592_31222[(2)] = inst_27478);

(statearr_27592_31222[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (10))){
var inst_27469 = (state_27511[(2)]);
var state_27511__$1 = state_27511;
var statearr_27596_31223 = state_27511__$1;
(statearr_27596_31223[(2)] = inst_27469);

(statearr_27596_31223[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27513 === (8))){
var inst_27466 = cljs.core.async.close_BANG_(to);
var state_27511__$1 = state_27511;
var statearr_27598_31224 = state_27511__$1;
(statearr_27598_31224[(2)] = inst_27466);

(statearr_27598_31224[(1)] = (10));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_27602 = [null,null,null,null,null,null,null,null];
(statearr_27602[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_27602[(1)] = (1));

return statearr_27602;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_27511){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_27511);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e27605){var ex__11650__auto__ = e27605;
var statearr_27606_31225 = state_27511;
(statearr_27606_31225[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_27511[(4)]))){
var statearr_27607_31226 = state_27511;
(statearr_27607_31226[(1)] = cljs.core.first((state_27511[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31227 = state_27511;
state_27511 = G__31227;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_27511){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_27511);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_27608 = f__11734__auto__();
(statearr_27608[(6)] = c__11733__auto___31207);

return statearr_27608;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var process__$1 = (function (p__27611){
var vec__27616 = p__27611;
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__27616,(0),null);
var p = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__27616,(1),null);
var job = vec__27616;
if((job == null)){
cljs.core.async.close_BANG_(results);

return null;
} else {
var res = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3((1),xf,ex_handler);
var c__11733__auto___31229 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_27625){
var state_val_27626 = (state_27625[(1)]);
if((state_val_27626 === (1))){
var state_27625__$1 = state_27625;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_27625__$1,(2),res,v);
} else {
if((state_val_27626 === (2))){
var inst_27622 = (state_27625[(2)]);
var inst_27623 = cljs.core.async.close_BANG_(res);
var state_27625__$1 = (function (){var statearr_27627 = state_27625;
(statearr_27627[(7)] = inst_27622);

return statearr_27627;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_27625__$1,inst_27623);
} else {
return null;
}
}
});
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_27628 = [null,null,null,null,null,null,null,null];
(statearr_27628[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__);

(statearr_27628[(1)] = (1));

return statearr_27628;
});
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1 = (function (state_27625){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_27625);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e27635){var ex__11650__auto__ = e27635;
var statearr_27637_31230 = state_27625;
(statearr_27637_31230[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_27625[(4)]))){
var statearr_27638_31231 = state_27625;
(statearr_27638_31231[(1)] = cljs.core.first((state_27625[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31232 = state_27625;
state_27625 = G__31232;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = function(state_27625){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1.call(this,state_27625);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_27639 = f__11734__auto__();
(statearr_27639[(6)] = c__11733__auto___31229);

return statearr_27639;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(p,res);

return true;
}
});
var async = (function (p__27663){
var vec__27667 = p__27663;
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__27667,(0),null);
var p = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__27667,(1),null);
var job = vec__27667;
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
var n__5741__auto___31234 = n;
var __31235 = (0);
while(true){
if((__31235 < n__5741__auto___31234)){
var G__27678_31236 = type;
var G__27678_31237__$1 = (((G__27678_31236 instanceof cljs.core.Keyword))?G__27678_31236.fqn:null);
switch (G__27678_31237__$1) {
case "compute":
var c__11733__auto___31239 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run(((function (__31235,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = ((function (__31235,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function (state_27701){
var state_val_27702 = (state_27701[(1)]);
if((state_val_27702 === (1))){
var state_27701__$1 = state_27701;
var statearr_27707_31241 = state_27701__$1;
(statearr_27707_31241[(2)] = null);

(statearr_27707_31241[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27702 === (2))){
var state_27701__$1 = state_27701;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_27701__$1,(4),jobs);
} else {
if((state_val_27702 === (3))){
var inst_27699 = (state_27701[(2)]);
var state_27701__$1 = state_27701;
return cljs.core.async.impl.ioc_helpers.return_chan(state_27701__$1,inst_27699);
} else {
if((state_val_27702 === (4))){
var inst_27691 = (state_27701[(2)]);
var inst_27692 = process__$1(inst_27691);
var state_27701__$1 = state_27701;
if(cljs.core.truth_(inst_27692)){
var statearr_27719_31243 = state_27701__$1;
(statearr_27719_31243[(1)] = (5));

} else {
var statearr_27720_31244 = state_27701__$1;
(statearr_27720_31244[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27702 === (5))){
var state_27701__$1 = state_27701;
var statearr_27722_31245 = state_27701__$1;
(statearr_27722_31245[(2)] = null);

(statearr_27722_31245[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27702 === (6))){
var state_27701__$1 = state_27701;
var statearr_27724_31246 = state_27701__$1;
(statearr_27724_31246[(2)] = null);

(statearr_27724_31246[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27702 === (7))){
var inst_27697 = (state_27701[(2)]);
var state_27701__$1 = state_27701;
var statearr_27725_31249 = state_27701__$1;
(statearr_27725_31249[(2)] = inst_27697);

(statearr_27725_31249[(1)] = (3));


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
});})(__31235,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
;
return ((function (__31235,switch__11646__auto__,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_27758 = [null,null,null,null,null,null,null];
(statearr_27758[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__);

(statearr_27758[(1)] = (1));

return statearr_27758;
});
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1 = (function (state_27701){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_27701);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e27760){var ex__11650__auto__ = e27760;
var statearr_27763_31253 = state_27701;
(statearr_27763_31253[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_27701[(4)]))){
var statearr_27764_31255 = state_27701;
(statearr_27764_31255[(1)] = cljs.core.first((state_27701[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31256 = state_27701;
state_27701 = G__31256;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = function(state_27701){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1.call(this,state_27701);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__;
})()
;})(__31235,switch__11646__auto__,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
})();
var state__11735__auto__ = (function (){var statearr_27774 = f__11734__auto__();
(statearr_27774[(6)] = c__11733__auto___31239);

return statearr_27774;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
});})(__31235,c__11733__auto___31239,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
);


break;
case "async":
var c__11733__auto___31257 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run(((function (__31235,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = ((function (__31235,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function (state_27789){
var state_val_27790 = (state_27789[(1)]);
if((state_val_27790 === (1))){
var state_27789__$1 = state_27789;
var statearr_27795_31261 = state_27789__$1;
(statearr_27795_31261[(2)] = null);

(statearr_27795_31261[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27790 === (2))){
var state_27789__$1 = state_27789;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_27789__$1,(4),jobs);
} else {
if((state_val_27790 === (3))){
var inst_27787 = (state_27789[(2)]);
var state_27789__$1 = state_27789;
return cljs.core.async.impl.ioc_helpers.return_chan(state_27789__$1,inst_27787);
} else {
if((state_val_27790 === (4))){
var inst_27778 = (state_27789[(2)]);
var inst_27779 = async(inst_27778);
var state_27789__$1 = state_27789;
if(cljs.core.truth_(inst_27779)){
var statearr_27796_31262 = state_27789__$1;
(statearr_27796_31262[(1)] = (5));

} else {
var statearr_27800_31263 = state_27789__$1;
(statearr_27800_31263[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27790 === (5))){
var state_27789__$1 = state_27789;
var statearr_27805_31264 = state_27789__$1;
(statearr_27805_31264[(2)] = null);

(statearr_27805_31264[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27790 === (6))){
var state_27789__$1 = state_27789;
var statearr_27819_31265 = state_27789__$1;
(statearr_27819_31265[(2)] = null);

(statearr_27819_31265[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27790 === (7))){
var inst_27785 = (state_27789[(2)]);
var state_27789__$1 = state_27789;
var statearr_27822_31268 = state_27789__$1;
(statearr_27822_31268[(2)] = inst_27785);

(statearr_27822_31268[(1)] = (3));


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
});})(__31235,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
;
return ((function (__31235,switch__11646__auto__,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_27828 = [null,null,null,null,null,null,null];
(statearr_27828[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__);

(statearr_27828[(1)] = (1));

return statearr_27828;
});
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1 = (function (state_27789){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_27789);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e27833){var ex__11650__auto__ = e27833;
var statearr_27834_31278 = state_27789;
(statearr_27834_31278[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_27789[(4)]))){
var statearr_27835_31279 = state_27789;
(statearr_27835_31279[(1)] = cljs.core.first((state_27789[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31280 = state_27789;
state_27789 = G__31280;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = function(state_27789){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1.call(this,state_27789);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__;
})()
;})(__31235,switch__11646__auto__,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
})();
var state__11735__auto__ = (function (){var statearr_27837 = f__11734__auto__();
(statearr_27837[(6)] = c__11733__auto___31257);

return statearr_27837;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
});})(__31235,c__11733__auto___31257,G__27678_31236,G__27678_31237__$1,n__5741__auto___31234,jobs,results,process__$1,async))
);


break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__27678_31237__$1))));

}

var G__31284 = (__31235 + (1));
__31235 = G__31284;
continue;
} else {
}
break;
}

var c__11733__auto___31285 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_27883){
var state_val_27887 = (state_27883[(1)]);
if((state_val_27887 === (7))){
var inst_27878 = (state_27883[(2)]);
var state_27883__$1 = state_27883;
var statearr_27893_31286 = state_27883__$1;
(statearr_27893_31286[(2)] = inst_27878);

(statearr_27893_31286[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27887 === (1))){
var state_27883__$1 = state_27883;
var statearr_27894_31287 = state_27883__$1;
(statearr_27894_31287[(2)] = null);

(statearr_27894_31287[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27887 === (4))){
var inst_27849 = (state_27883[(7)]);
var inst_27849__$1 = (state_27883[(2)]);
var inst_27850 = (inst_27849__$1 == null);
var state_27883__$1 = (function (){var statearr_27896 = state_27883;
(statearr_27896[(7)] = inst_27849__$1);

return statearr_27896;
})();
if(cljs.core.truth_(inst_27850)){
var statearr_27899_31291 = state_27883__$1;
(statearr_27899_31291[(1)] = (5));

} else {
var statearr_27900_31292 = state_27883__$1;
(statearr_27900_31292[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27887 === (6))){
var inst_27849 = (state_27883[(7)]);
var inst_27857 = (state_27883[(8)]);
var inst_27857__$1 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
var inst_27867 = cljs.core.PersistentVector.EMPTY_NODE;
var inst_27870 = [inst_27849,inst_27857__$1];
var inst_27871 = (new cljs.core.PersistentVector(null,2,(5),inst_27867,inst_27870,null));
var state_27883__$1 = (function (){var statearr_27905 = state_27883;
(statearr_27905[(8)] = inst_27857__$1);

return statearr_27905;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_27883__$1,(8),jobs,inst_27871);
} else {
if((state_val_27887 === (3))){
var inst_27880 = (state_27883[(2)]);
var state_27883__$1 = state_27883;
return cljs.core.async.impl.ioc_helpers.return_chan(state_27883__$1,inst_27880);
} else {
if((state_val_27887 === (2))){
var state_27883__$1 = state_27883;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_27883__$1,(4),from);
} else {
if((state_val_27887 === (9))){
var inst_27875 = (state_27883[(2)]);
var state_27883__$1 = (function (){var statearr_27927 = state_27883;
(statearr_27927[(9)] = inst_27875);

return statearr_27927;
})();
var statearr_27930_31299 = state_27883__$1;
(statearr_27930_31299[(2)] = null);

(statearr_27930_31299[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27887 === (5))){
var inst_27852 = cljs.core.async.close_BANG_(jobs);
var state_27883__$1 = state_27883;
var statearr_27934_31300 = state_27883__$1;
(statearr_27934_31300[(2)] = inst_27852);

(statearr_27934_31300[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_27887 === (8))){
var inst_27857 = (state_27883[(8)]);
var inst_27873 = (state_27883[(2)]);
var state_27883__$1 = (function (){var statearr_27938 = state_27883;
(statearr_27938[(10)] = inst_27873);

return statearr_27938;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_27883__$1,(9),results,inst_27857);
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
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_27949 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_27949[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__);

(statearr_27949[(1)] = (1));

return statearr_27949;
});
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1 = (function (state_27883){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_27883);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e27952){var ex__11650__auto__ = e27952;
var statearr_27953_31305 = state_27883;
(statearr_27953_31305[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_27883[(4)]))){
var statearr_27954_31306 = state_27883;
(statearr_27954_31306[(1)] = cljs.core.first((state_27883[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31309 = state_27883;
state_27883 = G__31309;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = function(state_27883){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1.call(this,state_27883);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_27955 = f__11734__auto__();
(statearr_27955[(6)] = c__11733__auto___31285);

return statearr_27955;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


var c__11733__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28024){
var state_val_28026 = (state_28024[(1)]);
if((state_val_28026 === (7))){
var inst_28017 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
var statearr_28063_31316 = state_28024__$1;
(statearr_28063_31316[(2)] = inst_28017);

(statearr_28063_31316[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (20))){
var state_28024__$1 = state_28024;
var statearr_28071_31317 = state_28024__$1;
(statearr_28071_31317[(2)] = null);

(statearr_28071_31317[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (1))){
var state_28024__$1 = state_28024;
var statearr_28076_31318 = state_28024__$1;
(statearr_28076_31318[(2)] = null);

(statearr_28076_31318[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (4))){
var inst_27962 = (state_28024[(7)]);
var inst_27962__$1 = (state_28024[(2)]);
var inst_27975 = (inst_27962__$1 == null);
var state_28024__$1 = (function (){var statearr_28081 = state_28024;
(statearr_28081[(7)] = inst_27962__$1);

return statearr_28081;
})();
if(cljs.core.truth_(inst_27975)){
var statearr_28082_31328 = state_28024__$1;
(statearr_28082_31328[(1)] = (5));

} else {
var statearr_28083_31330 = state_28024__$1;
(statearr_28083_31330[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (15))){
var inst_27997 = (state_28024[(8)]);
var state_28024__$1 = state_28024;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_28024__$1,(18),to,inst_27997);
} else {
if((state_val_28026 === (21))){
var inst_28012 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
var statearr_28091_31332 = state_28024__$1;
(statearr_28091_31332[(2)] = inst_28012);

(statearr_28091_31332[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (13))){
var inst_28014 = (state_28024[(2)]);
var state_28024__$1 = (function (){var statearr_28097 = state_28024;
(statearr_28097[(9)] = inst_28014);

return statearr_28097;
})();
var statearr_28100_31333 = state_28024__$1;
(statearr_28100_31333[(2)] = null);

(statearr_28100_31333[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (6))){
var inst_27962 = (state_28024[(7)]);
var state_28024__$1 = state_28024;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28024__$1,(11),inst_27962);
} else {
if((state_val_28026 === (17))){
var inst_28007 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
if(cljs.core.truth_(inst_28007)){
var statearr_28115_31335 = state_28024__$1;
(statearr_28115_31335[(1)] = (19));

} else {
var statearr_28116_31336 = state_28024__$1;
(statearr_28116_31336[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (3))){
var inst_28019 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28024__$1,inst_28019);
} else {
if((state_val_28026 === (12))){
var inst_27989 = (state_28024[(10)]);
var state_28024__$1 = state_28024;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28024__$1,(14),inst_27989);
} else {
if((state_val_28026 === (2))){
var state_28024__$1 = state_28024;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28024__$1,(4),results);
} else {
if((state_val_28026 === (19))){
var state_28024__$1 = state_28024;
var statearr_28130_31338 = state_28024__$1;
(statearr_28130_31338[(2)] = null);

(statearr_28130_31338[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (11))){
var inst_27989 = (state_28024[(2)]);
var state_28024__$1 = (function (){var statearr_28136 = state_28024;
(statearr_28136[(10)] = inst_27989);

return statearr_28136;
})();
var statearr_28141_31340 = state_28024__$1;
(statearr_28141_31340[(2)] = null);

(statearr_28141_31340[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (9))){
var state_28024__$1 = state_28024;
var statearr_28146_31341 = state_28024__$1;
(statearr_28146_31341[(2)] = null);

(statearr_28146_31341[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (5))){
var state_28024__$1 = state_28024;
if(cljs.core.truth_(close_QMARK_)){
var statearr_28156_31342 = state_28024__$1;
(statearr_28156_31342[(1)] = (8));

} else {
var statearr_28162_31343 = state_28024__$1;
(statearr_28162_31343[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (14))){
var inst_27997 = (state_28024[(8)]);
var inst_28001 = (state_28024[(11)]);
var inst_27997__$1 = (state_28024[(2)]);
var inst_28000 = (inst_27997__$1 == null);
var inst_28001__$1 = cljs.core.not(inst_28000);
var state_28024__$1 = (function (){var statearr_28173 = state_28024;
(statearr_28173[(8)] = inst_27997__$1);

(statearr_28173[(11)] = inst_28001__$1);

return statearr_28173;
})();
if(inst_28001__$1){
var statearr_28178_31353 = state_28024__$1;
(statearr_28178_31353[(1)] = (15));

} else {
var statearr_28186_31355 = state_28024__$1;
(statearr_28186_31355[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (16))){
var inst_28001 = (state_28024[(11)]);
var state_28024__$1 = state_28024;
var statearr_28200_31363 = state_28024__$1;
(statearr_28200_31363[(2)] = inst_28001);

(statearr_28200_31363[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (10))){
var inst_27985 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
var statearr_28204_31365 = state_28024__$1;
(statearr_28204_31365[(2)] = inst_27985);

(statearr_28204_31365[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (18))){
var inst_28004 = (state_28024[(2)]);
var state_28024__$1 = state_28024;
var statearr_28207_31366 = state_28024__$1;
(statearr_28207_31366[(2)] = inst_28004);

(statearr_28207_31366[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28026 === (8))){
var inst_27981 = cljs.core.async.close_BANG_(to);
var state_28024__$1 = state_28024;
var statearr_28208_31370 = state_28024__$1;
(statearr_28208_31370[(2)] = inst_27981);

(statearr_28208_31370[(1)] = (10));


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
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_28210 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_28210[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__);

(statearr_28210[(1)] = (1));

return statearr_28210;
});
var cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1 = (function (state_28024){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28024);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e28213){var ex__11650__auto__ = e28213;
var statearr_28216_31377 = state_28024;
(statearr_28216_31377[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28024[(4)]))){
var statearr_28217_31382 = state_28024;
(statearr_28217_31382[(1)] = cljs.core.first((state_28024[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31383 = state_28024;
state_28024 = G__31383;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__ = function(state_28024){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1.call(this,state_28024);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_28218 = f__11734__auto__();
(statearr_28218[(6)] = c__11733__auto__);

return statearr_28218;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));

return c__11733__auto__;
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
var G__28223 = arguments.length;
switch (G__28223) {
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
var G__28229 = arguments.length;
switch (G__28229) {
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
var G__28243 = arguments.length;
switch (G__28243) {
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
var c__11733__auto___31394 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28272){
var state_val_28273 = (state_28272[(1)]);
if((state_val_28273 === (7))){
var inst_28268 = (state_28272[(2)]);
var state_28272__$1 = state_28272;
var statearr_28277_31398 = state_28272__$1;
(statearr_28277_31398[(2)] = inst_28268);

(statearr_28277_31398[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (1))){
var state_28272__$1 = state_28272;
var statearr_28285_31401 = state_28272__$1;
(statearr_28285_31401[(2)] = null);

(statearr_28285_31401[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (4))){
var inst_28247 = (state_28272[(7)]);
var inst_28247__$1 = (state_28272[(2)]);
var inst_28248 = (inst_28247__$1 == null);
var state_28272__$1 = (function (){var statearr_28288 = state_28272;
(statearr_28288[(7)] = inst_28247__$1);

return statearr_28288;
})();
if(cljs.core.truth_(inst_28248)){
var statearr_28289_31402 = state_28272__$1;
(statearr_28289_31402[(1)] = (5));

} else {
var statearr_28290_31403 = state_28272__$1;
(statearr_28290_31403[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (13))){
var state_28272__$1 = state_28272;
var statearr_28293_31404 = state_28272__$1;
(statearr_28293_31404[(2)] = null);

(statearr_28293_31404[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (6))){
var inst_28247 = (state_28272[(7)]);
var inst_28253 = (p.cljs$core$IFn$_invoke$arity$1 ? p.cljs$core$IFn$_invoke$arity$1(inst_28247) : p.call(null,inst_28247));
var state_28272__$1 = state_28272;
if(cljs.core.truth_(inst_28253)){
var statearr_28294_31405 = state_28272__$1;
(statearr_28294_31405[(1)] = (9));

} else {
var statearr_28295_31406 = state_28272__$1;
(statearr_28295_31406[(1)] = (10));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (3))){
var inst_28270 = (state_28272[(2)]);
var state_28272__$1 = state_28272;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28272__$1,inst_28270);
} else {
if((state_val_28273 === (12))){
var state_28272__$1 = state_28272;
var statearr_28297_31409 = state_28272__$1;
(statearr_28297_31409[(2)] = null);

(statearr_28297_31409[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (2))){
var state_28272__$1 = state_28272;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28272__$1,(4),ch);
} else {
if((state_val_28273 === (11))){
var inst_28247 = (state_28272[(7)]);
var inst_28257 = (state_28272[(2)]);
var state_28272__$1 = state_28272;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_28272__$1,(8),inst_28257,inst_28247);
} else {
if((state_val_28273 === (9))){
var state_28272__$1 = state_28272;
var statearr_28298_31410 = state_28272__$1;
(statearr_28298_31410[(2)] = tc);

(statearr_28298_31410[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (5))){
var inst_28250 = cljs.core.async.close_BANG_(tc);
var inst_28251 = cljs.core.async.close_BANG_(fc);
var state_28272__$1 = (function (){var statearr_28299 = state_28272;
(statearr_28299[(8)] = inst_28250);

return statearr_28299;
})();
var statearr_28300_31414 = state_28272__$1;
(statearr_28300_31414[(2)] = inst_28251);

(statearr_28300_31414[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (14))){
var inst_28266 = (state_28272[(2)]);
var state_28272__$1 = state_28272;
var statearr_28301_31415 = state_28272__$1;
(statearr_28301_31415[(2)] = inst_28266);

(statearr_28301_31415[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (10))){
var state_28272__$1 = state_28272;
var statearr_28302_31416 = state_28272__$1;
(statearr_28302_31416[(2)] = fc);

(statearr_28302_31416[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28273 === (8))){
var inst_28259 = (state_28272[(2)]);
var state_28272__$1 = state_28272;
if(cljs.core.truth_(inst_28259)){
var statearr_28304_31417 = state_28272__$1;
(statearr_28304_31417[(1)] = (12));

} else {
var statearr_28305_31418 = state_28272__$1;
(statearr_28305_31418[(1)] = (13));

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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_28309 = [null,null,null,null,null,null,null,null,null];
(statearr_28309[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_28309[(1)] = (1));

return statearr_28309;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_28272){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28272);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e28310){var ex__11650__auto__ = e28310;
var statearr_28311_31419 = state_28272;
(statearr_28311_31419[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28272[(4)]))){
var statearr_28312_31420 = state_28272;
(statearr_28312_31420[(1)] = cljs.core.first((state_28272[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31421 = state_28272;
state_28272 = G__31421;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_28272){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_28272);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_28314 = f__11734__auto__();
(statearr_28314[(6)] = c__11733__auto___31394);

return statearr_28314;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var c__11733__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28346){
var state_val_28347 = (state_28346[(1)]);
if((state_val_28347 === (7))){
var inst_28342 = (state_28346[(2)]);
var state_28346__$1 = state_28346;
var statearr_28360_31422 = state_28346__$1;
(statearr_28360_31422[(2)] = inst_28342);

(statearr_28360_31422[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (1))){
var inst_28320 = init;
var inst_28321 = inst_28320;
var state_28346__$1 = (function (){var statearr_28361 = state_28346;
(statearr_28361[(7)] = inst_28321);

return statearr_28361;
})();
var statearr_28363_31424 = state_28346__$1;
(statearr_28363_31424[(2)] = null);

(statearr_28363_31424[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (4))){
var inst_28328 = (state_28346[(8)]);
var inst_28328__$1 = (state_28346[(2)]);
var inst_28329 = (inst_28328__$1 == null);
var state_28346__$1 = (function (){var statearr_28367 = state_28346;
(statearr_28367[(8)] = inst_28328__$1);

return statearr_28367;
})();
if(cljs.core.truth_(inst_28329)){
var statearr_28375_31425 = state_28346__$1;
(statearr_28375_31425[(1)] = (5));

} else {
var statearr_28377_31426 = state_28346__$1;
(statearr_28377_31426[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (6))){
var inst_28321 = (state_28346[(7)]);
var inst_28328 = (state_28346[(8)]);
var inst_28333 = (state_28346[(9)]);
var inst_28333__$1 = (f.cljs$core$IFn$_invoke$arity$2 ? f.cljs$core$IFn$_invoke$arity$2(inst_28321,inst_28328) : f.call(null,inst_28321,inst_28328));
var inst_28334 = cljs.core.reduced_QMARK_(inst_28333__$1);
var state_28346__$1 = (function (){var statearr_28378 = state_28346;
(statearr_28378[(9)] = inst_28333__$1);

return statearr_28378;
})();
if(inst_28334){
var statearr_28379_31427 = state_28346__$1;
(statearr_28379_31427[(1)] = (8));

} else {
var statearr_28380_31428 = state_28346__$1;
(statearr_28380_31428[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (3))){
var inst_28344 = (state_28346[(2)]);
var state_28346__$1 = state_28346;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28346__$1,inst_28344);
} else {
if((state_val_28347 === (2))){
var state_28346__$1 = state_28346;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28346__$1,(4),ch);
} else {
if((state_val_28347 === (9))){
var inst_28333 = (state_28346[(9)]);
var inst_28321 = inst_28333;
var state_28346__$1 = (function (){var statearr_28384 = state_28346;
(statearr_28384[(7)] = inst_28321);

return statearr_28384;
})();
var statearr_28385_31435 = state_28346__$1;
(statearr_28385_31435[(2)] = null);

(statearr_28385_31435[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (5))){
var inst_28321 = (state_28346[(7)]);
var state_28346__$1 = state_28346;
var statearr_28386_31437 = state_28346__$1;
(statearr_28386_31437[(2)] = inst_28321);

(statearr_28386_31437[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (10))){
var inst_28340 = (state_28346[(2)]);
var state_28346__$1 = state_28346;
var statearr_28387_31439 = state_28346__$1;
(statearr_28387_31439[(2)] = inst_28340);

(statearr_28387_31439[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28347 === (8))){
var inst_28333 = (state_28346[(9)]);
var inst_28336 = cljs.core.deref(inst_28333);
var state_28346__$1 = state_28346;
var statearr_28388_31440 = state_28346__$1;
(statearr_28388_31440[(2)] = inst_28336);

(statearr_28388_31440[(1)] = (10));


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
var cljs$core$async$reduce_$_state_machine__11647__auto__ = null;
var cljs$core$async$reduce_$_state_machine__11647__auto____0 = (function (){
var statearr_28389 = [null,null,null,null,null,null,null,null,null,null];
(statearr_28389[(0)] = cljs$core$async$reduce_$_state_machine__11647__auto__);

(statearr_28389[(1)] = (1));

return statearr_28389;
});
var cljs$core$async$reduce_$_state_machine__11647__auto____1 = (function (state_28346){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28346);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e28390){var ex__11650__auto__ = e28390;
var statearr_28391_31441 = state_28346;
(statearr_28391_31441[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28346[(4)]))){
var statearr_28392_31442 = state_28346;
(statearr_28392_31442[(1)] = cljs.core.first((state_28346[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31443 = state_28346;
state_28346 = G__31443;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$reduce_$_state_machine__11647__auto__ = function(state_28346){
switch(arguments.length){
case 0:
return cljs$core$async$reduce_$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$reduce_$_state_machine__11647__auto____1.call(this,state_28346);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$reduce_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$reduce_$_state_machine__11647__auto____0;
cljs$core$async$reduce_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$reduce_$_state_machine__11647__auto____1;
return cljs$core$async$reduce_$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_28400 = f__11734__auto__();
(statearr_28400[(6)] = c__11733__auto__);

return statearr_28400;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));

return c__11733__auto__;
});
/**
 * async/reduces a channel with a transformation (xform f).
 *   Returns a channel containing the result.  ch must close before
 *   transduce produces a result.
 */
cljs.core.async.transduce = (function cljs$core$async$transduce(xform,f,init,ch){
var f__$1 = (xform.cljs$core$IFn$_invoke$arity$1 ? xform.cljs$core$IFn$_invoke$arity$1(f) : xform.call(null,f));
var c__11733__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28418){
var state_val_28419 = (state_28418[(1)]);
if((state_val_28419 === (1))){
var inst_28413 = cljs.core.async.reduce(f__$1,init,ch);
var state_28418__$1 = state_28418;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28418__$1,(2),inst_28413);
} else {
if((state_val_28419 === (2))){
var inst_28415 = (state_28418[(2)]);
var inst_28416 = (f__$1.cljs$core$IFn$_invoke$arity$1 ? f__$1.cljs$core$IFn$_invoke$arity$1(inst_28415) : f__$1.call(null,inst_28415));
var state_28418__$1 = state_28418;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28418__$1,inst_28416);
} else {
return null;
}
}
});
return (function() {
var cljs$core$async$transduce_$_state_machine__11647__auto__ = null;
var cljs$core$async$transduce_$_state_machine__11647__auto____0 = (function (){
var statearr_28445 = [null,null,null,null,null,null,null];
(statearr_28445[(0)] = cljs$core$async$transduce_$_state_machine__11647__auto__);

(statearr_28445[(1)] = (1));

return statearr_28445;
});
var cljs$core$async$transduce_$_state_machine__11647__auto____1 = (function (state_28418){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28418);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e28452){var ex__11650__auto__ = e28452;
var statearr_28453_31450 = state_28418;
(statearr_28453_31450[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28418[(4)]))){
var statearr_28454_31451 = state_28418;
(statearr_28454_31451[(1)] = cljs.core.first((state_28418[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31452 = state_28418;
state_28418 = G__31452;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$transduce_$_state_machine__11647__auto__ = function(state_28418){
switch(arguments.length){
case 0:
return cljs$core$async$transduce_$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$transduce_$_state_machine__11647__auto____1.call(this,state_28418);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$transduce_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$transduce_$_state_machine__11647__auto____0;
cljs$core$async$transduce_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$transduce_$_state_machine__11647__auto____1;
return cljs$core$async$transduce_$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_28455 = f__11734__auto__();
(statearr_28455[(6)] = c__11733__auto__);

return statearr_28455;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));

return c__11733__auto__;
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
var G__28465 = arguments.length;
switch (G__28465) {
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
var c__11733__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28501){
var state_val_28502 = (state_28501[(1)]);
if((state_val_28502 === (7))){
var inst_28479 = (state_28501[(2)]);
var state_28501__$1 = state_28501;
var statearr_28508_31455 = state_28501__$1;
(statearr_28508_31455[(2)] = inst_28479);

(statearr_28508_31455[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (1))){
var inst_28473 = cljs.core.seq(coll);
var inst_28474 = inst_28473;
var state_28501__$1 = (function (){var statearr_28509 = state_28501;
(statearr_28509[(7)] = inst_28474);

return statearr_28509;
})();
var statearr_28510_31456 = state_28501__$1;
(statearr_28510_31456[(2)] = null);

(statearr_28510_31456[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (4))){
var inst_28474 = (state_28501[(7)]);
var inst_28477 = cljs.core.first(inst_28474);
var state_28501__$1 = state_28501;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_28501__$1,(7),ch,inst_28477);
} else {
if((state_val_28502 === (13))){
var inst_28495 = (state_28501[(2)]);
var state_28501__$1 = state_28501;
var statearr_28512_31460 = state_28501__$1;
(statearr_28512_31460[(2)] = inst_28495);

(statearr_28512_31460[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (6))){
var inst_28482 = (state_28501[(2)]);
var state_28501__$1 = state_28501;
if(cljs.core.truth_(inst_28482)){
var statearr_28518_31461 = state_28501__$1;
(statearr_28518_31461[(1)] = (8));

} else {
var statearr_28519_31464 = state_28501__$1;
(statearr_28519_31464[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (3))){
var inst_28499 = (state_28501[(2)]);
var state_28501__$1 = state_28501;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28501__$1,inst_28499);
} else {
if((state_val_28502 === (12))){
var state_28501__$1 = state_28501;
var statearr_28521_31466 = state_28501__$1;
(statearr_28521_31466[(2)] = null);

(statearr_28521_31466[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (2))){
var inst_28474 = (state_28501[(7)]);
var state_28501__$1 = state_28501;
if(cljs.core.truth_(inst_28474)){
var statearr_28522_31468 = state_28501__$1;
(statearr_28522_31468[(1)] = (4));

} else {
var statearr_28523_31469 = state_28501__$1;
(statearr_28523_31469[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (11))){
var inst_28489 = cljs.core.async.close_BANG_(ch);
var state_28501__$1 = state_28501;
var statearr_28526_31470 = state_28501__$1;
(statearr_28526_31470[(2)] = inst_28489);

(statearr_28526_31470[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (9))){
var state_28501__$1 = state_28501;
if(cljs.core.truth_(close_QMARK_)){
var statearr_28529_31471 = state_28501__$1;
(statearr_28529_31471[(1)] = (11));

} else {
var statearr_28534_31472 = state_28501__$1;
(statearr_28534_31472[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (5))){
var inst_28474 = (state_28501[(7)]);
var state_28501__$1 = state_28501;
var statearr_28539_31473 = state_28501__$1;
(statearr_28539_31473[(2)] = inst_28474);

(statearr_28539_31473[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (10))){
var inst_28497 = (state_28501[(2)]);
var state_28501__$1 = state_28501;
var statearr_28541_31474 = state_28501__$1;
(statearr_28541_31474[(2)] = inst_28497);

(statearr_28541_31474[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28502 === (8))){
var inst_28474 = (state_28501[(7)]);
var inst_28485 = cljs.core.next(inst_28474);
var inst_28474__$1 = inst_28485;
var state_28501__$1 = (function (){var statearr_28543 = state_28501;
(statearr_28543[(7)] = inst_28474__$1);

return statearr_28543;
})();
var statearr_28544_31476 = state_28501__$1;
(statearr_28544_31476[(2)] = null);

(statearr_28544_31476[(1)] = (2));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_28552 = [null,null,null,null,null,null,null,null];
(statearr_28552[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_28552[(1)] = (1));

return statearr_28552;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_28501){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28501);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e28557){var ex__11650__auto__ = e28557;
var statearr_28558_31479 = state_28501;
(statearr_28558_31479[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28501[(4)]))){
var statearr_28560_31480 = state_28501;
(statearr_28560_31480[(1)] = cljs.core.first((state_28501[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31481 = state_28501;
state_28501 = G__31481;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_28501){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_28501);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_28566 = f__11734__auto__();
(statearr_28566[(6)] = c__11733__auto__);

return statearr_28566;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));

return c__11733__auto__;
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
var G__28574 = arguments.length;
switch (G__28574) {
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

var cljs$core$async$Mux$muxch_STAR_$dyn_31484 = (function (_){
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
return cljs$core$async$Mux$muxch_STAR_$dyn_31484(_);
}
});


/**
 * @interface
 */
cljs.core.async.Mult = function(){};

var cljs$core$async$Mult$tap_STAR_$dyn_31490 = (function (m,ch,close_QMARK_){
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
return cljs$core$async$Mult$tap_STAR_$dyn_31490(m,ch,close_QMARK_);
}
});

var cljs$core$async$Mult$untap_STAR_$dyn_31497 = (function (m,ch){
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
return cljs$core$async$Mult$untap_STAR_$dyn_31497(m,ch);
}
});

var cljs$core$async$Mult$untap_all_STAR_$dyn_31505 = (function (m){
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
return cljs$core$async$Mult$untap_all_STAR_$dyn_31505(m);
}
});


/**
* @constructor
 * @implements {cljs.core.async.Mult}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.async.Mux}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async28629 = (function (ch,cs,meta28630){
this.ch = ch;
this.cs = cs;
this.meta28630 = meta28630;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_28631,meta28630__$1){
var self__ = this;
var _28631__$1 = this;
return (new cljs.core.async.t_cljs$core$async28629(self__.ch,self__.cs,meta28630__$1));
}));

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_28631){
var self__ = this;
var _28631__$1 = this;
return self__.meta28630;
}));

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
}));

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mult$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mult$tap_STAR_$arity$3 = (function (_,ch__$1,close_QMARK_){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(self__.cs,cljs.core.assoc,ch__$1,close_QMARK_);

return null;
}));

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mult$untap_STAR_$arity$2 = (function (_,ch__$1){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.dissoc,ch__$1);

return null;
}));

(cljs.core.async.t_cljs$core$async28629.prototype.cljs$core$async$Mult$untap_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return null;
}));

(cljs.core.async.t_cljs$core$async28629.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"meta28630","meta28630",479154643,null)], null);
}));

(cljs.core.async.t_cljs$core$async28629.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async28629.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async28629");

(cljs.core.async.t_cljs$core$async28629.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async28629");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async28629.
 */
cljs.core.async.__GT_t_cljs$core$async28629 = (function cljs$core$async$__GT_t_cljs$core$async28629(ch,cs,meta28630){
return (new cljs.core.async.t_cljs$core$async28629(ch,cs,meta28630));
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
var m = (new cljs.core.async.t_cljs$core$async28629(ch,cs,cljs.core.PersistentArrayMap.EMPTY));
var dchan = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
var dctr = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var done = (function (_){
if((cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(dctr,cljs.core.dec) === (0))){
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2(dchan,true);
} else {
return null;
}
});
var c__11733__auto___31521 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_28821){
var state_val_28822 = (state_28821[(1)]);
if((state_val_28822 === (7))){
var inst_28812 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28824_31523 = state_28821__$1;
(statearr_28824_31523[(2)] = inst_28812);

(statearr_28824_31523[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (20))){
var inst_28695 = (state_28821[(7)]);
var inst_28707 = cljs.core.first(inst_28695);
var inst_28708 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_28707,(0),null);
var inst_28709 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_28707,(1),null);
var state_28821__$1 = (function (){var statearr_28825 = state_28821;
(statearr_28825[(8)] = inst_28708);

return statearr_28825;
})();
if(cljs.core.truth_(inst_28709)){
var statearr_28826_31524 = state_28821__$1;
(statearr_28826_31524[(1)] = (22));

} else {
var statearr_28827_31527 = state_28821__$1;
(statearr_28827_31527[(1)] = (23));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (27))){
var inst_28745 = (state_28821[(9)]);
var inst_28747 = (state_28821[(10)]);
var inst_28752 = (state_28821[(11)]);
var inst_28660 = (state_28821[(12)]);
var inst_28752__$1 = cljs.core._nth(inst_28745,inst_28747);
var inst_28753 = cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3(inst_28752__$1,inst_28660,done);
var state_28821__$1 = (function (){var statearr_28832 = state_28821;
(statearr_28832[(11)] = inst_28752__$1);

return statearr_28832;
})();
if(cljs.core.truth_(inst_28753)){
var statearr_28836_31530 = state_28821__$1;
(statearr_28836_31530[(1)] = (30));

} else {
var statearr_28837_31532 = state_28821__$1;
(statearr_28837_31532[(1)] = (31));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (1))){
var state_28821__$1 = state_28821;
var statearr_28838_31533 = state_28821__$1;
(statearr_28838_31533[(2)] = null);

(statearr_28838_31533[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (24))){
var inst_28695 = (state_28821[(7)]);
var inst_28714 = (state_28821[(2)]);
var inst_28721 = cljs.core.next(inst_28695);
var inst_28671 = inst_28721;
var inst_28672 = null;
var inst_28673 = (0);
var inst_28674 = (0);
var state_28821__$1 = (function (){var statearr_28840 = state_28821;
(statearr_28840[(13)] = inst_28714);

(statearr_28840[(14)] = inst_28671);

(statearr_28840[(15)] = inst_28672);

(statearr_28840[(16)] = inst_28673);

(statearr_28840[(17)] = inst_28674);

return statearr_28840;
})();
var statearr_28842_31536 = state_28821__$1;
(statearr_28842_31536[(2)] = null);

(statearr_28842_31536[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (39))){
var state_28821__$1 = state_28821;
var statearr_28850_31539 = state_28821__$1;
(statearr_28850_31539[(2)] = null);

(statearr_28850_31539[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (4))){
var inst_28660 = (state_28821[(12)]);
var inst_28660__$1 = (state_28821[(2)]);
var inst_28661 = (inst_28660__$1 == null);
var state_28821__$1 = (function (){var statearr_28852 = state_28821;
(statearr_28852[(12)] = inst_28660__$1);

return statearr_28852;
})();
if(cljs.core.truth_(inst_28661)){
var statearr_28853_31540 = state_28821__$1;
(statearr_28853_31540[(1)] = (5));

} else {
var statearr_28854_31541 = state_28821__$1;
(statearr_28854_31541[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (15))){
var inst_28674 = (state_28821[(17)]);
var inst_28671 = (state_28821[(14)]);
var inst_28672 = (state_28821[(15)]);
var inst_28673 = (state_28821[(16)]);
var inst_28689 = (state_28821[(2)]);
var inst_28692 = (inst_28674 + (1));
var tmp28846 = inst_28673;
var tmp28847 = inst_28671;
var tmp28848 = inst_28672;
var inst_28671__$1 = tmp28847;
var inst_28672__$1 = tmp28848;
var inst_28673__$1 = tmp28846;
var inst_28674__$1 = inst_28692;
var state_28821__$1 = (function (){var statearr_28855 = state_28821;
(statearr_28855[(18)] = inst_28689);

(statearr_28855[(14)] = inst_28671__$1);

(statearr_28855[(15)] = inst_28672__$1);

(statearr_28855[(16)] = inst_28673__$1);

(statearr_28855[(17)] = inst_28674__$1);

return statearr_28855;
})();
var statearr_28856_31543 = state_28821__$1;
(statearr_28856_31543[(2)] = null);

(statearr_28856_31543[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (21))){
var inst_28724 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28861_31544 = state_28821__$1;
(statearr_28861_31544[(2)] = inst_28724);

(statearr_28861_31544[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (31))){
var inst_28752 = (state_28821[(11)]);
var inst_28758 = m.cljs$core$async$Mult$untap_STAR_$arity$2(null,inst_28752);
var state_28821__$1 = state_28821;
var statearr_28862_31545 = state_28821__$1;
(statearr_28862_31545[(2)] = inst_28758);

(statearr_28862_31545[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (32))){
var inst_28747 = (state_28821[(10)]);
var inst_28744 = (state_28821[(19)]);
var inst_28745 = (state_28821[(9)]);
var inst_28746 = (state_28821[(20)]);
var inst_28760 = (state_28821[(2)]);
var inst_28762 = (inst_28747 + (1));
var tmp28858 = inst_28746;
var tmp28859 = inst_28744;
var tmp28860 = inst_28745;
var inst_28744__$1 = tmp28859;
var inst_28745__$1 = tmp28860;
var inst_28746__$1 = tmp28858;
var inst_28747__$1 = inst_28762;
var state_28821__$1 = (function (){var statearr_28863 = state_28821;
(statearr_28863[(21)] = inst_28760);

(statearr_28863[(19)] = inst_28744__$1);

(statearr_28863[(9)] = inst_28745__$1);

(statearr_28863[(20)] = inst_28746__$1);

(statearr_28863[(10)] = inst_28747__$1);

return statearr_28863;
})();
var statearr_28865_31631 = state_28821__$1;
(statearr_28865_31631[(2)] = null);

(statearr_28865_31631[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (40))){
var inst_28783 = (state_28821[(22)]);
var inst_28788 = m.cljs$core$async$Mult$untap_STAR_$arity$2(null,inst_28783);
var state_28821__$1 = state_28821;
var statearr_28878_31632 = state_28821__$1;
(statearr_28878_31632[(2)] = inst_28788);

(statearr_28878_31632[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (33))){
var inst_28771 = (state_28821[(23)]);
var inst_28774 = cljs.core.chunked_seq_QMARK_(inst_28771);
var state_28821__$1 = state_28821;
if(inst_28774){
var statearr_28881_31637 = state_28821__$1;
(statearr_28881_31637[(1)] = (36));

} else {
var statearr_28882_31639 = state_28821__$1;
(statearr_28882_31639[(1)] = (37));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (13))){
var inst_28683 = (state_28821[(24)]);
var inst_28686 = cljs.core.async.close_BANG_(inst_28683);
var state_28821__$1 = state_28821;
var statearr_28888_31649 = state_28821__$1;
(statearr_28888_31649[(2)] = inst_28686);

(statearr_28888_31649[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (22))){
var inst_28708 = (state_28821[(8)]);
var inst_28711 = cljs.core.async.close_BANG_(inst_28708);
var state_28821__$1 = state_28821;
var statearr_28892_31659 = state_28821__$1;
(statearr_28892_31659[(2)] = inst_28711);

(statearr_28892_31659[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (36))){
var inst_28771 = (state_28821[(23)]);
var inst_28776 = cljs.core.chunk_first(inst_28771);
var inst_28778 = cljs.core.chunk_rest(inst_28771);
var inst_28779 = cljs.core.count(inst_28776);
var inst_28744 = inst_28778;
var inst_28745 = inst_28776;
var inst_28746 = inst_28779;
var inst_28747 = (0);
var state_28821__$1 = (function (){var statearr_28895 = state_28821;
(statearr_28895[(19)] = inst_28744);

(statearr_28895[(9)] = inst_28745);

(statearr_28895[(20)] = inst_28746);

(statearr_28895[(10)] = inst_28747);

return statearr_28895;
})();
var statearr_28896_31668 = state_28821__$1;
(statearr_28896_31668[(2)] = null);

(statearr_28896_31668[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (41))){
var inst_28771 = (state_28821[(23)]);
var inst_28791 = (state_28821[(2)]);
var inst_28792 = cljs.core.next(inst_28771);
var inst_28744 = inst_28792;
var inst_28745 = null;
var inst_28746 = (0);
var inst_28747 = (0);
var state_28821__$1 = (function (){var statearr_28903 = state_28821;
(statearr_28903[(25)] = inst_28791);

(statearr_28903[(19)] = inst_28744);

(statearr_28903[(9)] = inst_28745);

(statearr_28903[(20)] = inst_28746);

(statearr_28903[(10)] = inst_28747);

return statearr_28903;
})();
var statearr_28904_31676 = state_28821__$1;
(statearr_28904_31676[(2)] = null);

(statearr_28904_31676[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (43))){
var state_28821__$1 = state_28821;
var statearr_28905_31687 = state_28821__$1;
(statearr_28905_31687[(2)] = null);

(statearr_28905_31687[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (29))){
var inst_28800 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28906_31688 = state_28821__$1;
(statearr_28906_31688[(2)] = inst_28800);

(statearr_28906_31688[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (44))){
var inst_28809 = (state_28821[(2)]);
var state_28821__$1 = (function (){var statearr_28907 = state_28821;
(statearr_28907[(26)] = inst_28809);

return statearr_28907;
})();
var statearr_28908_31695 = state_28821__$1;
(statearr_28908_31695[(2)] = null);

(statearr_28908_31695[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (6))){
var inst_28734 = (state_28821[(27)]);
var inst_28733 = cljs.core.deref(cs);
var inst_28734__$1 = cljs.core.keys(inst_28733);
var inst_28735 = cljs.core.count(inst_28734__$1);
var inst_28736 = cljs.core.reset_BANG_(dctr,inst_28735);
var inst_28743 = cljs.core.seq(inst_28734__$1);
var inst_28744 = inst_28743;
var inst_28745 = null;
var inst_28746 = (0);
var inst_28747 = (0);
var state_28821__$1 = (function (){var statearr_28909 = state_28821;
(statearr_28909[(27)] = inst_28734__$1);

(statearr_28909[(28)] = inst_28736);

(statearr_28909[(19)] = inst_28744);

(statearr_28909[(9)] = inst_28745);

(statearr_28909[(20)] = inst_28746);

(statearr_28909[(10)] = inst_28747);

return statearr_28909;
})();
var statearr_28910_31711 = state_28821__$1;
(statearr_28910_31711[(2)] = null);

(statearr_28910_31711[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (28))){
var inst_28744 = (state_28821[(19)]);
var inst_28771 = (state_28821[(23)]);
var inst_28771__$1 = cljs.core.seq(inst_28744);
var state_28821__$1 = (function (){var statearr_28920 = state_28821;
(statearr_28920[(23)] = inst_28771__$1);

return statearr_28920;
})();
if(inst_28771__$1){
var statearr_28922_31712 = state_28821__$1;
(statearr_28922_31712[(1)] = (33));

} else {
var statearr_28923_31713 = state_28821__$1;
(statearr_28923_31713[(1)] = (34));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (25))){
var inst_28747 = (state_28821[(10)]);
var inst_28746 = (state_28821[(20)]);
var inst_28749 = (inst_28747 < inst_28746);
var inst_28750 = inst_28749;
var state_28821__$1 = state_28821;
if(cljs.core.truth_(inst_28750)){
var statearr_28926_31720 = state_28821__$1;
(statearr_28926_31720[(1)] = (27));

} else {
var statearr_28927_31721 = state_28821__$1;
(statearr_28927_31721[(1)] = (28));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (34))){
var state_28821__$1 = state_28821;
var statearr_28929_31730 = state_28821__$1;
(statearr_28929_31730[(2)] = null);

(statearr_28929_31730[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (17))){
var state_28821__$1 = state_28821;
var statearr_28930_31735 = state_28821__$1;
(statearr_28930_31735[(2)] = null);

(statearr_28930_31735[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (3))){
var inst_28814 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
return cljs.core.async.impl.ioc_helpers.return_chan(state_28821__$1,inst_28814);
} else {
if((state_val_28822 === (12))){
var inst_28729 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28938_31745 = state_28821__$1;
(statearr_28938_31745[(2)] = inst_28729);

(statearr_28938_31745[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (2))){
var state_28821__$1 = state_28821;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28821__$1,(4),ch);
} else {
if((state_val_28822 === (23))){
var state_28821__$1 = state_28821;
var statearr_28939_31753 = state_28821__$1;
(statearr_28939_31753[(2)] = null);

(statearr_28939_31753[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (35))){
var inst_28798 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28940_31757 = state_28821__$1;
(statearr_28940_31757[(2)] = inst_28798);

(statearr_28940_31757[(1)] = (29));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (19))){
var inst_28695 = (state_28821[(7)]);
var inst_28699 = cljs.core.chunk_first(inst_28695);
var inst_28700 = cljs.core.chunk_rest(inst_28695);
var inst_28701 = cljs.core.count(inst_28699);
var inst_28671 = inst_28700;
var inst_28672 = inst_28699;
var inst_28673 = inst_28701;
var inst_28674 = (0);
var state_28821__$1 = (function (){var statearr_28941 = state_28821;
(statearr_28941[(14)] = inst_28671);

(statearr_28941[(15)] = inst_28672);

(statearr_28941[(16)] = inst_28673);

(statearr_28941[(17)] = inst_28674);

return statearr_28941;
})();
var statearr_28942_31769 = state_28821__$1;
(statearr_28942_31769[(2)] = null);

(statearr_28942_31769[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (11))){
var inst_28671 = (state_28821[(14)]);
var inst_28695 = (state_28821[(7)]);
var inst_28695__$1 = cljs.core.seq(inst_28671);
var state_28821__$1 = (function (){var statearr_28944 = state_28821;
(statearr_28944[(7)] = inst_28695__$1);

return statearr_28944;
})();
if(inst_28695__$1){
var statearr_28945_31778 = state_28821__$1;
(statearr_28945_31778[(1)] = (16));

} else {
var statearr_28948_31780 = state_28821__$1;
(statearr_28948_31780[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (9))){
var inst_28731 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28949_31786 = state_28821__$1;
(statearr_28949_31786[(2)] = inst_28731);

(statearr_28949_31786[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (5))){
var inst_28669 = cljs.core.deref(cs);
var inst_28670 = cljs.core.seq(inst_28669);
var inst_28671 = inst_28670;
var inst_28672 = null;
var inst_28673 = (0);
var inst_28674 = (0);
var state_28821__$1 = (function (){var statearr_28950 = state_28821;
(statearr_28950[(14)] = inst_28671);

(statearr_28950[(15)] = inst_28672);

(statearr_28950[(16)] = inst_28673);

(statearr_28950[(17)] = inst_28674);

return statearr_28950;
})();
var statearr_28951_31791 = state_28821__$1;
(statearr_28951_31791[(2)] = null);

(statearr_28951_31791[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (14))){
var state_28821__$1 = state_28821;
var statearr_28952_31792 = state_28821__$1;
(statearr_28952_31792[(2)] = null);

(statearr_28952_31792[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (45))){
var inst_28806 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28953_31793 = state_28821__$1;
(statearr_28953_31793[(2)] = inst_28806);

(statearr_28953_31793[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (26))){
var inst_28734 = (state_28821[(27)]);
var inst_28802 = (state_28821[(2)]);
var inst_28803 = cljs.core.seq(inst_28734);
var state_28821__$1 = (function (){var statearr_28956 = state_28821;
(statearr_28956[(29)] = inst_28802);

return statearr_28956;
})();
if(inst_28803){
var statearr_28957_31794 = state_28821__$1;
(statearr_28957_31794[(1)] = (42));

} else {
var statearr_28960_31796 = state_28821__$1;
(statearr_28960_31796[(1)] = (43));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (16))){
var inst_28695 = (state_28821[(7)]);
var inst_28697 = cljs.core.chunked_seq_QMARK_(inst_28695);
var state_28821__$1 = state_28821;
if(inst_28697){
var statearr_28963_31797 = state_28821__$1;
(statearr_28963_31797[(1)] = (19));

} else {
var statearr_28964_31798 = state_28821__$1;
(statearr_28964_31798[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (38))){
var inst_28795 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28965_31799 = state_28821__$1;
(statearr_28965_31799[(2)] = inst_28795);

(statearr_28965_31799[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (30))){
var state_28821__$1 = state_28821;
var statearr_28970_31800 = state_28821__$1;
(statearr_28970_31800[(2)] = null);

(statearr_28970_31800[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (10))){
var inst_28672 = (state_28821[(15)]);
var inst_28674 = (state_28821[(17)]);
var inst_28682 = cljs.core._nth(inst_28672,inst_28674);
var inst_28683 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_28682,(0),null);
var inst_28684 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_28682,(1),null);
var state_28821__$1 = (function (){var statearr_28973 = state_28821;
(statearr_28973[(24)] = inst_28683);

return statearr_28973;
})();
if(cljs.core.truth_(inst_28684)){
var statearr_28974_31801 = state_28821__$1;
(statearr_28974_31801[(1)] = (13));

} else {
var statearr_28975_31802 = state_28821__$1;
(statearr_28975_31802[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (18))){
var inst_28727 = (state_28821[(2)]);
var state_28821__$1 = state_28821;
var statearr_28976_31803 = state_28821__$1;
(statearr_28976_31803[(2)] = inst_28727);

(statearr_28976_31803[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (42))){
var state_28821__$1 = state_28821;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_28821__$1,(45),dchan);
} else {
if((state_val_28822 === (37))){
var inst_28771 = (state_28821[(23)]);
var inst_28783 = (state_28821[(22)]);
var inst_28660 = (state_28821[(12)]);
var inst_28783__$1 = cljs.core.first(inst_28771);
var inst_28785 = cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3(inst_28783__$1,inst_28660,done);
var state_28821__$1 = (function (){var statearr_28977 = state_28821;
(statearr_28977[(22)] = inst_28783__$1);

return statearr_28977;
})();
if(cljs.core.truth_(inst_28785)){
var statearr_28978_31811 = state_28821__$1;
(statearr_28978_31811[(1)] = (39));

} else {
var statearr_28992_31812 = state_28821__$1;
(statearr_28992_31812[(1)] = (40));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_28822 === (8))){
var inst_28674 = (state_28821[(17)]);
var inst_28673 = (state_28821[(16)]);
var inst_28676 = (inst_28674 < inst_28673);
var inst_28677 = inst_28676;
var state_28821__$1 = state_28821;
if(cljs.core.truth_(inst_28677)){
var statearr_28993_31813 = state_28821__$1;
(statearr_28993_31813[(1)] = (10));

} else {
var statearr_28994_31814 = state_28821__$1;
(statearr_28994_31814[(1)] = (11));

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
var cljs$core$async$mult_$_state_machine__11647__auto__ = null;
var cljs$core$async$mult_$_state_machine__11647__auto____0 = (function (){
var statearr_29006 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_29006[(0)] = cljs$core$async$mult_$_state_machine__11647__auto__);

(statearr_29006[(1)] = (1));

return statearr_29006;
});
var cljs$core$async$mult_$_state_machine__11647__auto____1 = (function (state_28821){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_28821);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e29013){var ex__11650__auto__ = e29013;
var statearr_29014_31815 = state_28821;
(statearr_29014_31815[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_28821[(4)]))){
var statearr_29015_31817 = state_28821;
(statearr_29015_31817[(1)] = cljs.core.first((state_28821[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31819 = state_28821;
state_28821 = G__31819;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$mult_$_state_machine__11647__auto__ = function(state_28821){
switch(arguments.length){
case 0:
return cljs$core$async$mult_$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$mult_$_state_machine__11647__auto____1.call(this,state_28821);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mult_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mult_$_state_machine__11647__auto____0;
cljs$core$async$mult_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mult_$_state_machine__11647__auto____1;
return cljs$core$async$mult_$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_29016 = f__11734__auto__();
(statearr_29016[(6)] = c__11733__auto___31521);

return statearr_29016;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var G__29020 = arguments.length;
switch (G__29020) {
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

var cljs$core$async$Mix$admix_STAR_$dyn_31833 = (function (m,ch){
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
return cljs$core$async$Mix$admix_STAR_$dyn_31833(m,ch);
}
});

var cljs$core$async$Mix$unmix_STAR_$dyn_31838 = (function (m,ch){
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
return cljs$core$async$Mix$unmix_STAR_$dyn_31838(m,ch);
}
});

var cljs$core$async$Mix$unmix_all_STAR_$dyn_31839 = (function (m){
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
return cljs$core$async$Mix$unmix_all_STAR_$dyn_31839(m);
}
});

var cljs$core$async$Mix$toggle_STAR_$dyn_31840 = (function (m,state_map){
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
return cljs$core$async$Mix$toggle_STAR_$dyn_31840(m,state_map);
}
});

var cljs$core$async$Mix$solo_mode_STAR_$dyn_31843 = (function (m,mode){
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
return cljs$core$async$Mix$solo_mode_STAR_$dyn_31843(m,mode);
}
});

cljs.core.async.ioc_alts_BANG_ = (function cljs$core$async$ioc_alts_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___31847 = arguments.length;
var i__5877__auto___31848 = (0);
while(true){
if((i__5877__auto___31848 < len__5876__auto___31847)){
args__5882__auto__.push((arguments[i__5877__auto___31848]));

var G__31849 = (i__5877__auto___31848 + (1));
i__5877__auto___31848 = G__31849;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (state,cont_block,ports,p__29097){
var map__29101 = p__29097;
var map__29101__$1 = cljs.core.__destructure_map(map__29101);
var opts = map__29101__$1;
var statearr_29102_31854 = state;
(statearr_29102_31854[(1)] = cont_block);


var temp__5823__auto__ = cljs.core.async.do_alts((function (val){
var statearr_29103_31856 = state;
(statearr_29103_31856[(2)] = val);


return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state);
}),ports,opts);
if(cljs.core.truth_(temp__5823__auto__)){
var cb = temp__5823__auto__;
var statearr_29104_31857 = state;
(statearr_29104_31857[(2)] = cljs.core.deref(cb));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}));

(cljs.core.async.ioc_alts_BANG_.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(cljs.core.async.ioc_alts_BANG_.cljs$lang$applyTo = (function (seq29093){
var G__29094 = cljs.core.first(seq29093);
var seq29093__$1 = cljs.core.next(seq29093);
var G__29095 = cljs.core.first(seq29093__$1);
var seq29093__$2 = cljs.core.next(seq29093__$1);
var G__29096 = cljs.core.first(seq29093__$2);
var seq29093__$3 = cljs.core.next(seq29093__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__29094,G__29095,G__29096,seq29093__$3);
}));


/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.async.Mix}
 * @implements {cljs.core.async.Mux}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async29107 = (function (change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta29108){
this.change = change;
this.solo_mode = solo_mode;
this.pick = pick;
this.cs = cs;
this.calc_state = calc_state;
this.out = out;
this.changed = changed;
this.solo_modes = solo_modes;
this.attrs = attrs;
this.meta29108 = meta29108;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_29109,meta29108__$1){
var self__ = this;
var _29109__$1 = this;
return (new cljs.core.async.t_cljs$core$async29107(self__.change,self__.solo_mode,self__.pick,self__.cs,self__.calc_state,self__.out,self__.changed,self__.solo_modes,self__.attrs,meta29108__$1));
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_29109){
var self__ = this;
var _29109__$1 = this;
return self__.meta29108;
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.out;
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$admix_STAR_$arity$2 = (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(self__.cs,cljs.core.assoc,ch,cljs.core.PersistentArrayMap.EMPTY);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$unmix_STAR_$arity$2 = (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.dissoc,ch);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$unmix_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_(self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$toggle_STAR_$arity$2 = (function (_,state_map){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.cs,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(cljs.core.merge_with,cljs.core.merge),state_map);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async29107.prototype.cljs$core$async$Mix$solo_mode_STAR_$arity$2 = (function (_,mode){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_((self__.solo_modes.cljs$core$IFn$_invoke$arity$1 ? self__.solo_modes.cljs$core$IFn$_invoke$arity$1(mode) : self__.solo_modes.call(null,mode)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"mode must be one of: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(self__.solo_modes)))+"\n"+"(solo-modes mode)")));
}

cljs.core.reset_BANG_(self__.solo_mode,mode);

return (self__.changed.cljs$core$IFn$_invoke$arity$0 ? self__.changed.cljs$core$IFn$_invoke$arity$0() : self__.changed.call(null));
}));

(cljs.core.async.t_cljs$core$async29107.getBasis = (function (){
return new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"change","change",477485025,null),new cljs.core.Symbol(null,"solo-mode","solo-mode",2031788074,null),new cljs.core.Symbol(null,"pick","pick",1300068175,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"calc-state","calc-state",-349968968,null),new cljs.core.Symbol(null,"out","out",729986010,null),new cljs.core.Symbol(null,"changed","changed",-2083710852,null),new cljs.core.Symbol(null,"solo-modes","solo-modes",882180540,null),new cljs.core.Symbol(null,"attrs","attrs",-450137186,null),new cljs.core.Symbol(null,"meta29108","meta29108",1480727367,null)], null);
}));

(cljs.core.async.t_cljs$core$async29107.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async29107.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async29107");

(cljs.core.async.t_cljs$core$async29107.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async29107");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async29107.
 */
cljs.core.async.__GT_t_cljs$core$async29107 = (function cljs$core$async$__GT_t_cljs$core$async29107(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta29108){
return (new cljs.core.async.t_cljs$core$async29107(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta29108));
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
var m = (new cljs.core.async.t_cljs$core$async29107(change,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,cljs.core.PersistentArrayMap.EMPTY));
var c__11733__auto___31876 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_29214){
var state_val_29215 = (state_29214[(1)]);
if((state_val_29215 === (7))){
var inst_29173 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
if(cljs.core.truth_(inst_29173)){
var statearr_29217_31880 = state_29214__$1;
(statearr_29217_31880[(1)] = (8));

} else {
var statearr_29218_31881 = state_29214__$1;
(statearr_29218_31881[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (20))){
var inst_29161 = (state_29214[(7)]);
var state_29214__$1 = state_29214;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_29214__$1,(23),out,inst_29161);
} else {
if((state_val_29215 === (1))){
var inst_29141 = calc_state();
var inst_29142 = cljs.core.__destructure_map(inst_29141);
var inst_29143 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29142,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_29144 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29142,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_29145 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29142,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var inst_29149 = inst_29141;
var state_29214__$1 = (function (){var statearr_29221 = state_29214;
(statearr_29221[(8)] = inst_29143);

(statearr_29221[(9)] = inst_29144);

(statearr_29221[(10)] = inst_29145);

(statearr_29221[(11)] = inst_29149);

return statearr_29221;
})();
var statearr_29222_31882 = state_29214__$1;
(statearr_29222_31882[(2)] = null);

(statearr_29222_31882[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (24))){
var inst_29152 = (state_29214[(12)]);
var inst_29149 = inst_29152;
var state_29214__$1 = (function (){var statearr_29223 = state_29214;
(statearr_29223[(11)] = inst_29149);

return statearr_29223;
})();
var statearr_29224_31883 = state_29214__$1;
(statearr_29224_31883[(2)] = null);

(statearr_29224_31883[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (4))){
var inst_29161 = (state_29214[(7)]);
var inst_29168 = (state_29214[(13)]);
var inst_29160 = (state_29214[(2)]);
var inst_29161__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_29160,(0),null);
var inst_29162 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_29160,(1),null);
var inst_29168__$1 = (inst_29161__$1 == null);
var state_29214__$1 = (function (){var statearr_29225 = state_29214;
(statearr_29225[(7)] = inst_29161__$1);

(statearr_29225[(14)] = inst_29162);

(statearr_29225[(13)] = inst_29168__$1);

return statearr_29225;
})();
if(cljs.core.truth_(inst_29168__$1)){
var statearr_29226_31884 = state_29214__$1;
(statearr_29226_31884[(1)] = (5));

} else {
var statearr_29227_31886 = state_29214__$1;
(statearr_29227_31886[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (15))){
var inst_29153 = (state_29214[(15)]);
var inst_29188 = (state_29214[(16)]);
var inst_29188__$1 = cljs.core.empty_QMARK_(inst_29153);
var state_29214__$1 = (function (){var statearr_29231 = state_29214;
(statearr_29231[(16)] = inst_29188__$1);

return statearr_29231;
})();
if(inst_29188__$1){
var statearr_29232_31890 = state_29214__$1;
(statearr_29232_31890[(1)] = (17));

} else {
var statearr_29233_31891 = state_29214__$1;
(statearr_29233_31891[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (21))){
var inst_29152 = (state_29214[(12)]);
var inst_29149 = inst_29152;
var state_29214__$1 = (function (){var statearr_29234 = state_29214;
(statearr_29234[(11)] = inst_29149);

return statearr_29234;
})();
var statearr_29236_31892 = state_29214__$1;
(statearr_29236_31892[(2)] = null);

(statearr_29236_31892[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (13))){
var inst_29180 = (state_29214[(2)]);
var inst_29181 = calc_state();
var inst_29149 = inst_29181;
var state_29214__$1 = (function (){var statearr_29238 = state_29214;
(statearr_29238[(17)] = inst_29180);

(statearr_29238[(11)] = inst_29149);

return statearr_29238;
})();
var statearr_29239_31893 = state_29214__$1;
(statearr_29239_31893[(2)] = null);

(statearr_29239_31893[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (22))){
var inst_29208 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
var statearr_29242_31894 = state_29214__$1;
(statearr_29242_31894[(2)] = inst_29208);

(statearr_29242_31894[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (6))){
var inst_29162 = (state_29214[(14)]);
var inst_29171 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_29162,change);
var state_29214__$1 = state_29214;
var statearr_29244_31895 = state_29214__$1;
(statearr_29244_31895[(2)] = inst_29171);

(statearr_29244_31895[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (25))){
var state_29214__$1 = state_29214;
var statearr_29245_31896 = state_29214__$1;
(statearr_29245_31896[(2)] = null);

(statearr_29245_31896[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (17))){
var inst_29154 = (state_29214[(18)]);
var inst_29162 = (state_29214[(14)]);
var inst_29190 = (inst_29154.cljs$core$IFn$_invoke$arity$1 ? inst_29154.cljs$core$IFn$_invoke$arity$1(inst_29162) : inst_29154.call(null,inst_29162));
var inst_29191 = cljs.core.not(inst_29190);
var state_29214__$1 = state_29214;
var statearr_29247_31897 = state_29214__$1;
(statearr_29247_31897[(2)] = inst_29191);

(statearr_29247_31897[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (3))){
var inst_29212 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
return cljs.core.async.impl.ioc_helpers.return_chan(state_29214__$1,inst_29212);
} else {
if((state_val_29215 === (12))){
var state_29214__$1 = state_29214;
var statearr_29249_31899 = state_29214__$1;
(statearr_29249_31899[(2)] = null);

(statearr_29249_31899[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (2))){
var inst_29149 = (state_29214[(11)]);
var inst_29152 = (state_29214[(12)]);
var inst_29152__$1 = cljs.core.__destructure_map(inst_29149);
var inst_29153 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29152__$1,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_29154 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29152__$1,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_29155 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29152__$1,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var state_29214__$1 = (function (){var statearr_29252 = state_29214;
(statearr_29252[(12)] = inst_29152__$1);

(statearr_29252[(15)] = inst_29153);

(statearr_29252[(18)] = inst_29154);

return statearr_29252;
})();
return cljs.core.async.ioc_alts_BANG_(state_29214__$1,(4),inst_29155);
} else {
if((state_val_29215 === (23))){
var inst_29199 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
if(cljs.core.truth_(inst_29199)){
var statearr_29258_31905 = state_29214__$1;
(statearr_29258_31905[(1)] = (24));

} else {
var statearr_29260_31906 = state_29214__$1;
(statearr_29260_31906[(1)] = (25));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (19))){
var inst_29194 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
var statearr_29261_31908 = state_29214__$1;
(statearr_29261_31908[(2)] = inst_29194);

(statearr_29261_31908[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (11))){
var inst_29162 = (state_29214[(14)]);
var inst_29177 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(cs,cljs.core.dissoc,inst_29162);
var state_29214__$1 = state_29214;
var statearr_29263_31909 = state_29214__$1;
(statearr_29263_31909[(2)] = inst_29177);

(statearr_29263_31909[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (9))){
var inst_29153 = (state_29214[(15)]);
var inst_29162 = (state_29214[(14)]);
var inst_29184 = (state_29214[(19)]);
var inst_29184__$1 = (inst_29153.cljs$core$IFn$_invoke$arity$1 ? inst_29153.cljs$core$IFn$_invoke$arity$1(inst_29162) : inst_29153.call(null,inst_29162));
var state_29214__$1 = (function (){var statearr_29267 = state_29214;
(statearr_29267[(19)] = inst_29184__$1);

return statearr_29267;
})();
if(cljs.core.truth_(inst_29184__$1)){
var statearr_29268_31911 = state_29214__$1;
(statearr_29268_31911[(1)] = (14));

} else {
var statearr_29269_31912 = state_29214__$1;
(statearr_29269_31912[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (5))){
var inst_29168 = (state_29214[(13)]);
var state_29214__$1 = state_29214;
var statearr_29276_31914 = state_29214__$1;
(statearr_29276_31914[(2)] = inst_29168);

(statearr_29276_31914[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (14))){
var inst_29184 = (state_29214[(19)]);
var state_29214__$1 = state_29214;
var statearr_29278_31920 = state_29214__$1;
(statearr_29278_31920[(2)] = inst_29184);

(statearr_29278_31920[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (26))){
var inst_29204 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
var statearr_29280_31924 = state_29214__$1;
(statearr_29280_31924[(2)] = inst_29204);

(statearr_29280_31924[(1)] = (22));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (16))){
var inst_29196 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
if(cljs.core.truth_(inst_29196)){
var statearr_29281_31925 = state_29214__$1;
(statearr_29281_31925[(1)] = (20));

} else {
var statearr_29282_31926 = state_29214__$1;
(statearr_29282_31926[(1)] = (21));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (10))){
var inst_29210 = (state_29214[(2)]);
var state_29214__$1 = state_29214;
var statearr_29287_31946 = state_29214__$1;
(statearr_29287_31946[(2)] = inst_29210);

(statearr_29287_31946[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (18))){
var inst_29188 = (state_29214[(16)]);
var state_29214__$1 = state_29214;
var statearr_29288_31947 = state_29214__$1;
(statearr_29288_31947[(2)] = inst_29188);

(statearr_29288_31947[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29215 === (8))){
var inst_29161 = (state_29214[(7)]);
var inst_29175 = (inst_29161 == null);
var state_29214__$1 = state_29214;
if(cljs.core.truth_(inst_29175)){
var statearr_29289_31948 = state_29214__$1;
(statearr_29289_31948[(1)] = (11));

} else {
var statearr_29290_31949 = state_29214__$1;
(statearr_29290_31949[(1)] = (12));

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
var cljs$core$async$mix_$_state_machine__11647__auto__ = null;
var cljs$core$async$mix_$_state_machine__11647__auto____0 = (function (){
var statearr_29297 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_29297[(0)] = cljs$core$async$mix_$_state_machine__11647__auto__);

(statearr_29297[(1)] = (1));

return statearr_29297;
});
var cljs$core$async$mix_$_state_machine__11647__auto____1 = (function (state_29214){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_29214);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e29300){var ex__11650__auto__ = e29300;
var statearr_29301_31952 = state_29214;
(statearr_29301_31952[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_29214[(4)]))){
var statearr_29302_31953 = state_29214;
(statearr_29302_31953[(1)] = cljs.core.first((state_29214[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31954 = state_29214;
state_29214 = G__31954;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$mix_$_state_machine__11647__auto__ = function(state_29214){
switch(arguments.length){
case 0:
return cljs$core$async$mix_$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$mix_$_state_machine__11647__auto____1.call(this,state_29214);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mix_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mix_$_state_machine__11647__auto____0;
cljs$core$async$mix_$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mix_$_state_machine__11647__auto____1;
return cljs$core$async$mix_$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_29317 = f__11734__auto__();
(statearr_29317[(6)] = c__11733__auto___31876);

return statearr_29317;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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

var cljs$core$async$Pub$sub_STAR_$dyn_31961 = (function (p,v,ch,close_QMARK_){
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
return cljs$core$async$Pub$sub_STAR_$dyn_31961(p,v,ch,close_QMARK_);
}
});

var cljs$core$async$Pub$unsub_STAR_$dyn_31966 = (function (p,v,ch){
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
return cljs$core$async$Pub$unsub_STAR_$dyn_31966(p,v,ch);
}
});

var cljs$core$async$Pub$unsub_all_STAR_$dyn_31970 = (function() {
var G__31971 = null;
var G__31971__1 = (function (p){
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
var G__31971__2 = (function (p,v){
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
G__31971 = function(p,v){
switch(arguments.length){
case 1:
return G__31971__1.call(this,p);
case 2:
return G__31971__2.call(this,p,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__31971.cljs$core$IFn$_invoke$arity$1 = G__31971__1;
G__31971.cljs$core$IFn$_invoke$arity$2 = G__31971__2;
return G__31971;
})()
;
cljs.core.async.unsub_all_STAR_ = (function cljs$core$async$unsub_all_STAR_(var_args){
var G__29413 = arguments.length;
switch (G__29413) {
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
return cljs$core$async$Pub$unsub_all_STAR_$dyn_31970(p);
}
}));

(cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (p,v){
if((((!((p == null)))) && ((!((p.cljs$core$async$Pub$unsub_all_STAR_$arity$2 == null)))))){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$2(p,v);
} else {
return cljs$core$async$Pub$unsub_all_STAR_$dyn_31970(p,v);
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
cljs.core.async.t_cljs$core$async29444 = (function (ch,topic_fn,buf_fn,mults,ensure_mult,meta29445){
this.ch = ch;
this.topic_fn = topic_fn;
this.buf_fn = buf_fn;
this.mults = mults;
this.ensure_mult = ensure_mult;
this.meta29445 = meta29445;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_29446,meta29445__$1){
var self__ = this;
var _29446__$1 = this;
return (new cljs.core.async.t_cljs$core$async29444(self__.ch,self__.topic_fn,self__.buf_fn,self__.mults,self__.ensure_mult,meta29445__$1));
}));

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_29446){
var self__ = this;
var _29446__$1 = this;
return self__.meta29445;
}));

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Mux$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
}));

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Pub$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Pub$sub_STAR_$arity$4 = (function (p,topic,ch__$1,close_QMARK_){
var self__ = this;
var p__$1 = this;
var m = (self__.ensure_mult.cljs$core$IFn$_invoke$arity$1 ? self__.ensure_mult.cljs$core$IFn$_invoke$arity$1(topic) : self__.ensure_mult.call(null,topic));
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3(m,ch__$1,close_QMARK_);
}));

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Pub$unsub_STAR_$arity$3 = (function (p,topic,ch__$1){
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

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.reset_BANG_(self__.mults,cljs.core.PersistentArrayMap.EMPTY);
}));

(cljs.core.async.t_cljs$core$async29444.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$2 = (function (_,topic){
var self__ = this;
var ___$1 = this;
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(self__.mults,cljs.core.dissoc,topic);
}));

(cljs.core.async.t_cljs$core$async29444.getBasis = (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"topic-fn","topic-fn",-862449736,null),new cljs.core.Symbol(null,"buf-fn","buf-fn",-1200281591,null),new cljs.core.Symbol(null,"mults","mults",-461114485,null),new cljs.core.Symbol(null,"ensure-mult","ensure-mult",1796584816,null),new cljs.core.Symbol(null,"meta29445","meta29445",-2105825355,null)], null);
}));

(cljs.core.async.t_cljs$core$async29444.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async29444.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async29444");

(cljs.core.async.t_cljs$core$async29444.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async29444");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async29444.
 */
cljs.core.async.__GT_t_cljs$core$async29444 = (function cljs$core$async$__GT_t_cljs$core$async29444(ch,topic_fn,buf_fn,mults,ensure_mult,meta29445){
return (new cljs.core.async.t_cljs$core$async29444(ch,topic_fn,buf_fn,mults,ensure_mult,meta29445));
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
var G__29422 = arguments.length;
switch (G__29422) {
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
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(mults,(function (p1__29418_SHARP_){
if(cljs.core.truth_((p1__29418_SHARP_.cljs$core$IFn$_invoke$arity$1 ? p1__29418_SHARP_.cljs$core$IFn$_invoke$arity$1(topic) : p1__29418_SHARP_.call(null,topic)))){
return p1__29418_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__29418_SHARP_,topic,cljs.core.async.mult(cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((buf_fn.cljs$core$IFn$_invoke$arity$1 ? buf_fn.cljs$core$IFn$_invoke$arity$1(topic) : buf_fn.call(null,topic)))));
}
})),topic);
}
});
var p = (new cljs.core.async.t_cljs$core$async29444(ch,topic_fn,buf_fn,mults,ensure_mult,cljs.core.PersistentArrayMap.EMPTY));
var c__11733__auto___31989 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_29607){
var state_val_29608 = (state_29607[(1)]);
if((state_val_29608 === (7))){
var inst_29585 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29609_31990 = state_29607__$1;
(statearr_29609_31990[(2)] = inst_29585);

(statearr_29609_31990[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (20))){
var state_29607__$1 = state_29607;
var statearr_29615_31991 = state_29607__$1;
(statearr_29615_31991[(2)] = null);

(statearr_29615_31991[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (1))){
var state_29607__$1 = state_29607;
var statearr_29616_31993 = state_29607__$1;
(statearr_29616_31993[(2)] = null);

(statearr_29616_31993[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (24))){
var inst_29568 = (state_29607[(7)]);
var inst_29577 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(mults,cljs.core.dissoc,inst_29568);
var state_29607__$1 = state_29607;
var statearr_29618_31994 = state_29607__$1;
(statearr_29618_31994[(2)] = inst_29577);

(statearr_29618_31994[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (4))){
var inst_29512 = (state_29607[(8)]);
var inst_29512__$1 = (state_29607[(2)]);
var inst_29513 = (inst_29512__$1 == null);
var state_29607__$1 = (function (){var statearr_29619 = state_29607;
(statearr_29619[(8)] = inst_29512__$1);

return statearr_29619;
})();
if(cljs.core.truth_(inst_29513)){
var statearr_29621_31997 = state_29607__$1;
(statearr_29621_31997[(1)] = (5));

} else {
var statearr_29622_31998 = state_29607__$1;
(statearr_29622_31998[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (15))){
var inst_29562 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29623_32001 = state_29607__$1;
(statearr_29623_32001[(2)] = inst_29562);

(statearr_29623_32001[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (21))){
var inst_29582 = (state_29607[(2)]);
var state_29607__$1 = (function (){var statearr_29624 = state_29607;
(statearr_29624[(9)] = inst_29582);

return statearr_29624;
})();
var statearr_29628_32002 = state_29607__$1;
(statearr_29628_32002[(2)] = null);

(statearr_29628_32002[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (13))){
var inst_29541 = (state_29607[(10)]);
var inst_29544 = cljs.core.chunked_seq_QMARK_(inst_29541);
var state_29607__$1 = state_29607;
if(inst_29544){
var statearr_29629_32005 = state_29607__$1;
(statearr_29629_32005[(1)] = (16));

} else {
var statearr_29631_32006 = state_29607__$1;
(statearr_29631_32006[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (22))){
var inst_29574 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
if(cljs.core.truth_(inst_29574)){
var statearr_29633_32007 = state_29607__$1;
(statearr_29633_32007[(1)] = (23));

} else {
var statearr_29640_32008 = state_29607__$1;
(statearr_29640_32008[(1)] = (24));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (6))){
var inst_29512 = (state_29607[(8)]);
var inst_29568 = (state_29607[(7)]);
var inst_29570 = (state_29607[(11)]);
var inst_29568__$1 = (topic_fn.cljs$core$IFn$_invoke$arity$1 ? topic_fn.cljs$core$IFn$_invoke$arity$1(inst_29512) : topic_fn.call(null,inst_29512));
var inst_29569 = cljs.core.deref(mults);
var inst_29570__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(inst_29569,inst_29568__$1);
var state_29607__$1 = (function (){var statearr_29642 = state_29607;
(statearr_29642[(7)] = inst_29568__$1);

(statearr_29642[(11)] = inst_29570__$1);

return statearr_29642;
})();
if(cljs.core.truth_(inst_29570__$1)){
var statearr_29644_32014 = state_29607__$1;
(statearr_29644_32014[(1)] = (19));

} else {
var statearr_29645_32015 = state_29607__$1;
(statearr_29645_32015[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (25))){
var inst_29579 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29646_32017 = state_29607__$1;
(statearr_29646_32017[(2)] = inst_29579);

(statearr_29646_32017[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (17))){
var inst_29541 = (state_29607[(10)]);
var inst_29551 = cljs.core.first(inst_29541);
var inst_29552 = cljs.core.async.muxch_STAR_(inst_29551);
var inst_29553 = cljs.core.async.close_BANG_(inst_29552);
var inst_29554 = cljs.core.next(inst_29541);
var inst_29522 = inst_29554;
var inst_29523 = null;
var inst_29524 = (0);
var inst_29525 = (0);
var state_29607__$1 = (function (){var statearr_29648 = state_29607;
(statearr_29648[(12)] = inst_29553);

(statearr_29648[(13)] = inst_29522);

(statearr_29648[(14)] = inst_29523);

(statearr_29648[(15)] = inst_29524);

(statearr_29648[(16)] = inst_29525);

return statearr_29648;
})();
var statearr_29649_32019 = state_29607__$1;
(statearr_29649_32019[(2)] = null);

(statearr_29649_32019[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (3))){
var inst_29587 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
return cljs.core.async.impl.ioc_helpers.return_chan(state_29607__$1,inst_29587);
} else {
if((state_val_29608 === (12))){
var inst_29564 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29651_32020 = state_29607__$1;
(statearr_29651_32020[(2)] = inst_29564);

(statearr_29651_32020[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (2))){
var state_29607__$1 = state_29607;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_29607__$1,(4),ch);
} else {
if((state_val_29608 === (23))){
var state_29607__$1 = state_29607;
var statearr_29652_32021 = state_29607__$1;
(statearr_29652_32021[(2)] = null);

(statearr_29652_32021[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (19))){
var inst_29570 = (state_29607[(11)]);
var inst_29512 = (state_29607[(8)]);
var inst_29572 = cljs.core.async.muxch_STAR_(inst_29570);
var state_29607__$1 = state_29607;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_29607__$1,(22),inst_29572,inst_29512);
} else {
if((state_val_29608 === (11))){
var inst_29522 = (state_29607[(13)]);
var inst_29541 = (state_29607[(10)]);
var inst_29541__$1 = cljs.core.seq(inst_29522);
var state_29607__$1 = (function (){var statearr_29662 = state_29607;
(statearr_29662[(10)] = inst_29541__$1);

return statearr_29662;
})();
if(inst_29541__$1){
var statearr_29669_32022 = state_29607__$1;
(statearr_29669_32022[(1)] = (13));

} else {
var statearr_29670_32023 = state_29607__$1;
(statearr_29670_32023[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (9))){
var inst_29566 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29672_32025 = state_29607__$1;
(statearr_29672_32025[(2)] = inst_29566);

(statearr_29672_32025[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (5))){
var inst_29519 = cljs.core.deref(mults);
var inst_29520 = cljs.core.vals(inst_29519);
var inst_29521 = cljs.core.seq(inst_29520);
var inst_29522 = inst_29521;
var inst_29523 = null;
var inst_29524 = (0);
var inst_29525 = (0);
var state_29607__$1 = (function (){var statearr_29673 = state_29607;
(statearr_29673[(13)] = inst_29522);

(statearr_29673[(14)] = inst_29523);

(statearr_29673[(15)] = inst_29524);

(statearr_29673[(16)] = inst_29525);

return statearr_29673;
})();
var statearr_29683_32026 = state_29607__$1;
(statearr_29683_32026[(2)] = null);

(statearr_29683_32026[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (14))){
var state_29607__$1 = state_29607;
var statearr_29687_32027 = state_29607__$1;
(statearr_29687_32027[(2)] = null);

(statearr_29687_32027[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (16))){
var inst_29541 = (state_29607[(10)]);
var inst_29546 = cljs.core.chunk_first(inst_29541);
var inst_29547 = cljs.core.chunk_rest(inst_29541);
var inst_29548 = cljs.core.count(inst_29546);
var inst_29522 = inst_29547;
var inst_29523 = inst_29546;
var inst_29524 = inst_29548;
var inst_29525 = (0);
var state_29607__$1 = (function (){var statearr_29688 = state_29607;
(statearr_29688[(13)] = inst_29522);

(statearr_29688[(14)] = inst_29523);

(statearr_29688[(15)] = inst_29524);

(statearr_29688[(16)] = inst_29525);

return statearr_29688;
})();
var statearr_29689_32029 = state_29607__$1;
(statearr_29689_32029[(2)] = null);

(statearr_29689_32029[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (10))){
var inst_29523 = (state_29607[(14)]);
var inst_29525 = (state_29607[(16)]);
var inst_29522 = (state_29607[(13)]);
var inst_29524 = (state_29607[(15)]);
var inst_29535 = cljs.core._nth(inst_29523,inst_29525);
var inst_29536 = cljs.core.async.muxch_STAR_(inst_29535);
var inst_29537 = cljs.core.async.close_BANG_(inst_29536);
var inst_29538 = (inst_29525 + (1));
var tmp29684 = inst_29524;
var tmp29685 = inst_29522;
var tmp29686 = inst_29523;
var inst_29522__$1 = tmp29685;
var inst_29523__$1 = tmp29686;
var inst_29524__$1 = tmp29684;
var inst_29525__$1 = inst_29538;
var state_29607__$1 = (function (){var statearr_29694 = state_29607;
(statearr_29694[(17)] = inst_29537);

(statearr_29694[(13)] = inst_29522__$1);

(statearr_29694[(14)] = inst_29523__$1);

(statearr_29694[(15)] = inst_29524__$1);

(statearr_29694[(16)] = inst_29525__$1);

return statearr_29694;
})();
var statearr_29695_32033 = state_29607__$1;
(statearr_29695_32033[(2)] = null);

(statearr_29695_32033[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (18))){
var inst_29557 = (state_29607[(2)]);
var state_29607__$1 = state_29607;
var statearr_29696_32035 = state_29607__$1;
(statearr_29696_32035[(2)] = inst_29557);

(statearr_29696_32035[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29608 === (8))){
var inst_29525 = (state_29607[(16)]);
var inst_29524 = (state_29607[(15)]);
var inst_29527 = (inst_29525 < inst_29524);
var inst_29528 = inst_29527;
var state_29607__$1 = state_29607;
if(cljs.core.truth_(inst_29528)){
var statearr_29697_32036 = state_29607__$1;
(statearr_29697_32036[(1)] = (10));

} else {
var statearr_29698_32037 = state_29607__$1;
(statearr_29698_32037[(1)] = (11));

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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_29704 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_29704[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_29704[(1)] = (1));

return statearr_29704;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_29607){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_29607);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e29712){var ex__11650__auto__ = e29712;
var statearr_29713_32039 = state_29607;
(statearr_29713_32039[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_29607[(4)]))){
var statearr_29716_32040 = state_29607;
(statearr_29716_32040[(1)] = cljs.core.first((state_29607[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32041 = state_29607;
state_29607 = G__32041;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_29607){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_29607);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_29726 = f__11734__auto__();
(statearr_29726[(6)] = c__11733__auto___31989);

return statearr_29726;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var G__29737 = arguments.length;
switch (G__29737) {
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
var G__29790 = arguments.length;
switch (G__29790) {
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
var G__29795 = arguments.length;
switch (G__29795) {
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
var c__11733__auto___32057 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_29926){
var state_val_29927 = (state_29926[(1)]);
if((state_val_29927 === (7))){
var state_29926__$1 = state_29926;
var statearr_29934_32059 = state_29926__$1;
(statearr_29934_32059[(2)] = null);

(statearr_29934_32059[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (1))){
var state_29926__$1 = state_29926;
var statearr_29935_32060 = state_29926__$1;
(statearr_29935_32060[(2)] = null);

(statearr_29935_32060[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (4))){
var inst_29814 = (state_29926[(7)]);
var inst_29813 = (state_29926[(8)]);
var inst_29816 = (inst_29814 < inst_29813);
var state_29926__$1 = state_29926;
if(cljs.core.truth_(inst_29816)){
var statearr_29937_32061 = state_29926__$1;
(statearr_29937_32061[(1)] = (6));

} else {
var statearr_29938_32062 = state_29926__$1;
(statearr_29938_32062[(1)] = (7));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (15))){
var inst_29902 = (state_29926[(9)]);
var inst_29915 = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(f,inst_29902);
var state_29926__$1 = state_29926;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_29926__$1,(17),out,inst_29915);
} else {
if((state_val_29927 === (13))){
var inst_29902 = (state_29926[(9)]);
var inst_29902__$1 = (state_29926[(2)]);
var inst_29906 = cljs.core.some(cljs.core.nil_QMARK_,inst_29902__$1);
var state_29926__$1 = (function (){var statearr_29944 = state_29926;
(statearr_29944[(9)] = inst_29902__$1);

return statearr_29944;
})();
if(cljs.core.truth_(inst_29906)){
var statearr_29945_32063 = state_29926__$1;
(statearr_29945_32063[(1)] = (14));

} else {
var statearr_29946_32064 = state_29926__$1;
(statearr_29946_32064[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (6))){
var state_29926__$1 = state_29926;
var statearr_29947_32065 = state_29926__$1;
(statearr_29947_32065[(2)] = null);

(statearr_29947_32065[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (17))){
var inst_29919 = (state_29926[(2)]);
var state_29926__$1 = (function (){var statearr_29955 = state_29926;
(statearr_29955[(10)] = inst_29919);

return statearr_29955;
})();
var statearr_29956_32067 = state_29926__$1;
(statearr_29956_32067[(2)] = null);

(statearr_29956_32067[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (3))){
var inst_29924 = (state_29926[(2)]);
var state_29926__$1 = state_29926;
return cljs.core.async.impl.ioc_helpers.return_chan(state_29926__$1,inst_29924);
} else {
if((state_val_29927 === (12))){
var _ = (function (){var statearr_29959 = state_29926;
(statearr_29959[(4)] = cljs.core.rest((state_29926[(4)])));

return statearr_29959;
})();
var state_29926__$1 = state_29926;
var ex29953 = (state_29926__$1[(2)]);
var statearr_29960_32068 = state_29926__$1;
(statearr_29960_32068[(5)] = ex29953);


if((ex29953 instanceof Object)){
var statearr_29962_32069 = state_29926__$1;
(statearr_29962_32069[(1)] = (11));

(statearr_29962_32069[(5)] = null);

} else {
throw ex29953;

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (2))){
var inst_29810 = cljs.core.reset_BANG_(dctr,cnt);
var inst_29813 = cnt;
var inst_29814 = (0);
var state_29926__$1 = (function (){var statearr_29963 = state_29926;
(statearr_29963[(11)] = inst_29810);

(statearr_29963[(8)] = inst_29813);

(statearr_29963[(7)] = inst_29814);

return statearr_29963;
})();
var statearr_29969_32070 = state_29926__$1;
(statearr_29969_32070[(2)] = null);

(statearr_29969_32070[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (11))){
var inst_29840 = (state_29926[(2)]);
var inst_29841 = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(dctr,cljs.core.dec);
var state_29926__$1 = (function (){var statearr_29970 = state_29926;
(statearr_29970[(12)] = inst_29840);

return statearr_29970;
})();
var statearr_29971_32071 = state_29926__$1;
(statearr_29971_32071[(2)] = inst_29841);

(statearr_29971_32071[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (9))){
var inst_29814 = (state_29926[(7)]);
var _ = (function (){var statearr_29972 = state_29926;
(statearr_29972[(4)] = cljs.core.cons((12),(state_29926[(4)])));

return statearr_29972;
})();
var inst_29884 = (chs__$1.cljs$core$IFn$_invoke$arity$1 ? chs__$1.cljs$core$IFn$_invoke$arity$1(inst_29814) : chs__$1.call(null,inst_29814));
var inst_29885 = (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(inst_29814) : done.call(null,inst_29814));
var inst_29886 = cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2(inst_29884,inst_29885);
var ___$1 = (function (){var statearr_29973 = state_29926;
(statearr_29973[(4)] = cljs.core.rest((state_29926[(4)])));

return statearr_29973;
})();
var state_29926__$1 = state_29926;
var statearr_29978_32072 = state_29926__$1;
(statearr_29978_32072[(2)] = inst_29886);

(statearr_29978_32072[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (5))){
var inst_29898 = (state_29926[(2)]);
var state_29926__$1 = (function (){var statearr_29985 = state_29926;
(statearr_29985[(13)] = inst_29898);

return statearr_29985;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_29926__$1,(13),dchan);
} else {
if((state_val_29927 === (14))){
var inst_29908 = cljs.core.async.close_BANG_(out);
var state_29926__$1 = state_29926;
var statearr_29986_32076 = state_29926__$1;
(statearr_29986_32076[(2)] = inst_29908);

(statearr_29986_32076[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (16))){
var inst_29922 = (state_29926[(2)]);
var state_29926__$1 = state_29926;
var statearr_29987_32077 = state_29926__$1;
(statearr_29987_32077[(2)] = inst_29922);

(statearr_29987_32077[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (10))){
var inst_29814 = (state_29926[(7)]);
var inst_29889 = (state_29926[(2)]);
var inst_29890 = (inst_29814 + (1));
var inst_29814__$1 = inst_29890;
var state_29926__$1 = (function (){var statearr_29989 = state_29926;
(statearr_29989[(14)] = inst_29889);

(statearr_29989[(7)] = inst_29814__$1);

return statearr_29989;
})();
var statearr_29990_32079 = state_29926__$1;
(statearr_29990_32079[(2)] = null);

(statearr_29990_32079[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_29927 === (8))){
var inst_29894 = (state_29926[(2)]);
var state_29926__$1 = state_29926;
var statearr_29993_32080 = state_29926__$1;
(statearr_29993_32080[(2)] = inst_29894);

(statearr_29993_32080[(1)] = (5));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_29995 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_29995[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_29995[(1)] = (1));

return statearr_29995;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_29926){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_29926);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e29999){var ex__11650__auto__ = e29999;
var statearr_30000_32081 = state_29926;
(statearr_30000_32081[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_29926[(4)]))){
var statearr_30002_32082 = state_29926;
(statearr_30002_32082[(1)] = cljs.core.first((state_29926[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32083 = state_29926;
state_29926 = G__32083;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_29926){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_29926);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30004 = f__11734__auto__();
(statearr_30004[(6)] = c__11733__auto___32057);

return statearr_30004;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var G__30009 = arguments.length;
switch (G__30009) {
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
var c__11733__auto___32087 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30055){
var state_val_30057 = (state_30055[(1)]);
if((state_val_30057 === (7))){
var inst_30025 = (state_30055[(7)]);
var inst_30026 = (state_30055[(8)]);
var inst_30025__$1 = (state_30055[(2)]);
var inst_30026__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_30025__$1,(0),null);
var inst_30027 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(inst_30025__$1,(1),null);
var inst_30028 = (inst_30026__$1 == null);
var state_30055__$1 = (function (){var statearr_30068 = state_30055;
(statearr_30068[(7)] = inst_30025__$1);

(statearr_30068[(8)] = inst_30026__$1);

(statearr_30068[(9)] = inst_30027);

return statearr_30068;
})();
if(cljs.core.truth_(inst_30028)){
var statearr_30073_32088 = state_30055__$1;
(statearr_30073_32088[(1)] = (8));

} else {
var statearr_30081_32089 = state_30055__$1;
(statearr_30081_32089[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (1))){
var inst_30014 = cljs.core.vec(chs);
var inst_30015 = inst_30014;
var state_30055__$1 = (function (){var statearr_30086 = state_30055;
(statearr_30086[(10)] = inst_30015);

return statearr_30086;
})();
var statearr_30087_32090 = state_30055__$1;
(statearr_30087_32090[(2)] = null);

(statearr_30087_32090[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (4))){
var inst_30015 = (state_30055[(10)]);
var state_30055__$1 = state_30055;
return cljs.core.async.ioc_alts_BANG_(state_30055__$1,(7),inst_30015);
} else {
if((state_val_30057 === (6))){
var inst_30049 = (state_30055[(2)]);
var state_30055__$1 = state_30055;
var statearr_30091_32091 = state_30055__$1;
(statearr_30091_32091[(2)] = inst_30049);

(statearr_30091_32091[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (3))){
var inst_30051 = (state_30055[(2)]);
var state_30055__$1 = state_30055;
return cljs.core.async.impl.ioc_helpers.return_chan(state_30055__$1,inst_30051);
} else {
if((state_val_30057 === (2))){
var inst_30015 = (state_30055[(10)]);
var inst_30017 = cljs.core.count(inst_30015);
var inst_30018 = (inst_30017 > (0));
var state_30055__$1 = state_30055;
if(cljs.core.truth_(inst_30018)){
var statearr_30095_32117 = state_30055__$1;
(statearr_30095_32117[(1)] = (4));

} else {
var statearr_30096_32118 = state_30055__$1;
(statearr_30096_32118[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (11))){
var inst_30015 = (state_30055[(10)]);
var inst_30039 = (state_30055[(2)]);
var tmp30093 = inst_30015;
var inst_30015__$1 = tmp30093;
var state_30055__$1 = (function (){var statearr_30097 = state_30055;
(statearr_30097[(11)] = inst_30039);

(statearr_30097[(10)] = inst_30015__$1);

return statearr_30097;
})();
var statearr_30098_32124 = state_30055__$1;
(statearr_30098_32124[(2)] = null);

(statearr_30098_32124[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (9))){
var inst_30026 = (state_30055[(8)]);
var state_30055__$1 = state_30055;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30055__$1,(11),out,inst_30026);
} else {
if((state_val_30057 === (5))){
var inst_30047 = cljs.core.async.close_BANG_(out);
var state_30055__$1 = state_30055;
var statearr_30101_32125 = state_30055__$1;
(statearr_30101_32125[(2)] = inst_30047);

(statearr_30101_32125[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (10))){
var inst_30042 = (state_30055[(2)]);
var state_30055__$1 = state_30055;
var statearr_30102_32126 = state_30055__$1;
(statearr_30102_32126[(2)] = inst_30042);

(statearr_30102_32126[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30057 === (8))){
var inst_30015 = (state_30055[(10)]);
var inst_30025 = (state_30055[(7)]);
var inst_30026 = (state_30055[(8)]);
var inst_30027 = (state_30055[(9)]);
var inst_30033 = (function (){var cs = inst_30015;
var vec__30020 = inst_30025;
var v = inst_30026;
var c = inst_30027;
return (function (p1__30007_SHARP_){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(c,p1__30007_SHARP_);
});
})();
var inst_30035 = cljs.core.filterv(inst_30033,inst_30015);
var inst_30015__$1 = inst_30035;
var state_30055__$1 = (function (){var statearr_30104 = state_30055;
(statearr_30104[(10)] = inst_30015__$1);

return statearr_30104;
})();
var statearr_30105_32134 = state_30055__$1;
(statearr_30105_32134[(2)] = null);

(statearr_30105_32134[(1)] = (2));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30110 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_30110[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30110[(1)] = (1));

return statearr_30110;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30055){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30055);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30111){var ex__11650__auto__ = e30111;
var statearr_30112_32136 = state_30055;
(statearr_30112_32136[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30055[(4)]))){
var statearr_30113_32138 = state_30055;
(statearr_30113_32138[(1)] = cljs.core.first((state_30055[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32139 = state_30055;
state_30055 = G__32139;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30055){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30055);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30116 = f__11734__auto__();
(statearr_30116[(6)] = c__11733__auto___32087);

return statearr_30116;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
var G__30138 = arguments.length;
switch (G__30138) {
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
var c__11733__auto___32150 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30165){
var state_val_30166 = (state_30165[(1)]);
if((state_val_30166 === (7))){
var inst_30147 = (state_30165[(7)]);
var inst_30147__$1 = (state_30165[(2)]);
var inst_30148 = (inst_30147__$1 == null);
var inst_30149 = cljs.core.not(inst_30148);
var state_30165__$1 = (function (){var statearr_30173 = state_30165;
(statearr_30173[(7)] = inst_30147__$1);

return statearr_30173;
})();
if(inst_30149){
var statearr_30174_32152 = state_30165__$1;
(statearr_30174_32152[(1)] = (8));

} else {
var statearr_30175_32153 = state_30165__$1;
(statearr_30175_32153[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (1))){
var inst_30141 = (0);
var state_30165__$1 = (function (){var statearr_30176 = state_30165;
(statearr_30176[(8)] = inst_30141);

return statearr_30176;
})();
var statearr_30177_32157 = state_30165__$1;
(statearr_30177_32157[(2)] = null);

(statearr_30177_32157[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (4))){
var state_30165__$1 = state_30165;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30165__$1,(7),ch);
} else {
if((state_val_30166 === (6))){
var inst_30160 = (state_30165[(2)]);
var state_30165__$1 = state_30165;
var statearr_30178_32162 = state_30165__$1;
(statearr_30178_32162[(2)] = inst_30160);

(statearr_30178_32162[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (3))){
var inst_30162 = (state_30165[(2)]);
var inst_30163 = cljs.core.async.close_BANG_(out);
var state_30165__$1 = (function (){var statearr_30179 = state_30165;
(statearr_30179[(9)] = inst_30162);

return statearr_30179;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_30165__$1,inst_30163);
} else {
if((state_val_30166 === (2))){
var inst_30141 = (state_30165[(8)]);
var inst_30143 = (inst_30141 < n);
var state_30165__$1 = state_30165;
if(cljs.core.truth_(inst_30143)){
var statearr_30180_32168 = state_30165__$1;
(statearr_30180_32168[(1)] = (4));

} else {
var statearr_30181_32169 = state_30165__$1;
(statearr_30181_32169[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (11))){
var inst_30141 = (state_30165[(8)]);
var inst_30152 = (state_30165[(2)]);
var inst_30153 = (inst_30141 + (1));
var inst_30141__$1 = inst_30153;
var state_30165__$1 = (function (){var statearr_30182 = state_30165;
(statearr_30182[(10)] = inst_30152);

(statearr_30182[(8)] = inst_30141__$1);

return statearr_30182;
})();
var statearr_30183_32170 = state_30165__$1;
(statearr_30183_32170[(2)] = null);

(statearr_30183_32170[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (9))){
var state_30165__$1 = state_30165;
var statearr_30185_32171 = state_30165__$1;
(statearr_30185_32171[(2)] = null);

(statearr_30185_32171[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (5))){
var state_30165__$1 = state_30165;
var statearr_30186_32174 = state_30165__$1;
(statearr_30186_32174[(2)] = null);

(statearr_30186_32174[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (10))){
var inst_30157 = (state_30165[(2)]);
var state_30165__$1 = state_30165;
var statearr_30187_32175 = state_30165__$1;
(statearr_30187_32175[(2)] = inst_30157);

(statearr_30187_32175[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30166 === (8))){
var inst_30147 = (state_30165[(7)]);
var state_30165__$1 = state_30165;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30165__$1,(11),out,inst_30147);
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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30190 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_30190[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30190[(1)] = (1));

return statearr_30190;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30165){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30165);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30191){var ex__11650__auto__ = e30191;
var statearr_30192_32176 = state_30165;
(statearr_30192_32176[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30165[(4)]))){
var statearr_30193_32177 = state_30165;
(statearr_30193_32177[(1)] = cljs.core.first((state_30165[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32179 = state_30165;
state_30165 = G__32179;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30165){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30165);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30195 = f__11734__auto__();
(statearr_30195[(6)] = c__11733__auto___32150);

return statearr_30195;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
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
cljs.core.async.t_cljs$core$async30208 = (function (f,ch,meta30204,_,fn1,meta30209){
this.f = f;
this.ch = ch;
this.meta30204 = meta30204;
this._ = _;
this.fn1 = fn1;
this.meta30209 = meta30209;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_30210,meta30209__$1){
var self__ = this;
var _30210__$1 = this;
return (new cljs.core.async.t_cljs$core$async30208(self__.f,self__.ch,self__.meta30204,self__._,self__.fn1,meta30209__$1));
}));

(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_30210){
var self__ = this;
var _30210__$1 = this;
return self__.meta30209;
}));

(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$async$impl$protocols$Handler$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return cljs.core.async.impl.protocols.active_QMARK_(self__.fn1);
}));

(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$async$impl$protocols$Handler$blockable_QMARK_$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return true;
}));

(cljs.core.async.t_cljs$core$async30208.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
var f1 = cljs.core.async.impl.protocols.commit(self__.fn1);
return (function (p1__30202_SHARP_){
var G__30222 = (((p1__30202_SHARP_ == null))?null:(self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(p1__30202_SHARP_) : self__.f.call(null,p1__30202_SHARP_)));
return (f1.cljs$core$IFn$_invoke$arity$1 ? f1.cljs$core$IFn$_invoke$arity$1(G__30222) : f1.call(null,G__30222));
});
}));

(cljs.core.async.t_cljs$core$async30208.getBasis = (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta30204","meta30204",-451746520,null),cljs.core.with_meta(new cljs.core.Symbol(null,"_","_",-1201019570,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol("cljs.core.async","t_cljs$core$async30203","cljs.core.async/t_cljs$core$async30203",-49453735,null)], null)),new cljs.core.Symbol(null,"fn1","fn1",895834444,null),new cljs.core.Symbol(null,"meta30209","meta30209",928436287,null)], null);
}));

(cljs.core.async.t_cljs$core$async30208.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async30208.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async30208");

(cljs.core.async.t_cljs$core$async30208.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async30208");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async30208.
 */
cljs.core.async.__GT_t_cljs$core$async30208 = (function cljs$core$async$__GT_t_cljs$core$async30208(f,ch,meta30204,_,fn1,meta30209){
return (new cljs.core.async.t_cljs$core$async30208(f,ch,meta30204,_,fn1,meta30209));
});



/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async30203 = (function (f,ch,meta30204){
this.f = f;
this.ch = ch;
this.meta30204 = meta30204;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_30205,meta30204__$1){
var self__ = this;
var _30205__$1 = this;
return (new cljs.core.async.t_cljs$core$async30203(self__.f,self__.ch,meta30204__$1));
}));

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_30205){
var self__ = this;
var _30205__$1 = this;
return self__.meta30204;
}));

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
var ret = cljs.core.async.impl.protocols.take_BANG_(self__.ch,(new cljs.core.async.t_cljs$core$async30208(self__.f,self__.ch,self__.meta30204,___$1,fn1,cljs.core.PersistentArrayMap.EMPTY)));
if(cljs.core.truth_((function (){var and__5140__auto__ = ret;
if(cljs.core.truth_(and__5140__auto__)){
return (!((cljs.core.deref(ret) == null)));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.async.impl.channels.box((function (){var G__30229 = cljs.core.deref(ret);
return (self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(G__30229) : self__.f.call(null,G__30229));
})());
} else {
return ret;
}
}));

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30203.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,val,fn1);
}));

(cljs.core.async.t_cljs$core$async30203.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta30204","meta30204",-451746520,null)], null);
}));

(cljs.core.async.t_cljs$core$async30203.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async30203.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async30203");

(cljs.core.async.t_cljs$core$async30203.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async30203");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async30203.
 */
cljs.core.async.__GT_t_cljs$core$async30203 = (function cljs$core$async$__GT_t_cljs$core$async30203(f,ch,meta30204){
return (new cljs.core.async.t_cljs$core$async30203(f,ch,meta30204));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_LT_ = (function cljs$core$async$map_LT_(f,ch){
return (new cljs.core.async.t_cljs$core$async30203(f,ch,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async30235 = (function (f,ch,meta30236){
this.f = f;
this.ch = ch;
this.meta30236 = meta30236;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_30237,meta30236__$1){
var self__ = this;
var _30237__$1 = this;
return (new cljs.core.async.t_cljs$core$async30235(self__.f,self__.ch,meta30236__$1));
}));

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_30237){
var self__ = this;
var _30237__$1 = this;
return self__.meta30236;
}));

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_(self__.ch,fn1);
}));

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30235.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,(self__.f.cljs$core$IFn$_invoke$arity$1 ? self__.f.cljs$core$IFn$_invoke$arity$1(val) : self__.f.call(null,val)),fn1);
}));

(cljs.core.async.t_cljs$core$async30235.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta30236","meta30236",1964109477,null)], null);
}));

(cljs.core.async.t_cljs$core$async30235.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async30235.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async30235");

(cljs.core.async.t_cljs$core$async30235.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async30235");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async30235.
 */
cljs.core.async.__GT_t_cljs$core$async30235 = (function cljs$core$async$__GT_t_cljs$core$async30235(f,ch,meta30236){
return (new cljs.core.async.t_cljs$core$async30235(f,ch,meta30236));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_GT_ = (function cljs$core$async$map_GT_(f,ch){
return (new cljs.core.async.t_cljs$core$async30235(f,ch,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {cljs.core.async.impl.protocols.Channel}
 * @implements {cljs.core.async.impl.protocols.WritePort}
 * @implements {cljs.core.async.impl.protocols.ReadPort}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
cljs.core.async.t_cljs$core$async30256 = (function (p,ch,meta30257){
this.p = p;
this.ch = ch;
this.meta30257 = meta30257;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_30258,meta30257__$1){
var self__ = this;
var _30258__$1 = this;
return (new cljs.core.async.t_cljs$core$async30256(self__.p,self__.ch,meta30257__$1));
}));

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_30258){
var self__ = this;
var _30258__$1 = this;
return self__.meta30257;
}));

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$Channel$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_(self__.ch);
}));

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$ReadPort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_(self__.ch,fn1);
}));

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$WritePort$ = cljs.core.PROTOCOL_SENTINEL);

(cljs.core.async.t_cljs$core$async30256.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_((self__.p.cljs$core$IFn$_invoke$arity$1 ? self__.p.cljs$core$IFn$_invoke$arity$1(val) : self__.p.call(null,val)))){
return cljs.core.async.impl.protocols.put_BANG_(self__.ch,val,fn1);
} else {
return cljs.core.async.impl.channels.box(cljs.core.not(cljs.core.async.impl.protocols.closed_QMARK_(self__.ch)));
}
}));

(cljs.core.async.t_cljs$core$async30256.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"p","p",1791580836,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta30257","meta30257",283845667,null)], null);
}));

(cljs.core.async.t_cljs$core$async30256.cljs$lang$type = true);

(cljs.core.async.t_cljs$core$async30256.cljs$lang$ctorStr = "cljs.core.async/t_cljs$core$async30256");

(cljs.core.async.t_cljs$core$async30256.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"cljs.core.async/t_cljs$core$async30256");
}));

/**
 * Positional factory function for cljs.core.async/t_cljs$core$async30256.
 */
cljs.core.async.__GT_t_cljs$core$async30256 = (function cljs$core$async$__GT_t_cljs$core$async30256(p,ch,meta30257){
return (new cljs.core.async.t_cljs$core$async30256(p,ch,meta30257));
});


/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.filter_GT_ = (function cljs$core$async$filter_GT_(p,ch){
return (new cljs.core.async.t_cljs$core$async30256(p,ch,cljs.core.PersistentArrayMap.EMPTY));
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
var G__30278 = arguments.length;
switch (G__30278) {
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
var c__11733__auto___32198 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30303){
var state_val_30304 = (state_30303[(1)]);
if((state_val_30304 === (7))){
var inst_30299 = (state_30303[(2)]);
var state_30303__$1 = state_30303;
var statearr_30332_32199 = state_30303__$1;
(statearr_30332_32199[(2)] = inst_30299);

(statearr_30332_32199[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (1))){
var state_30303__$1 = state_30303;
var statearr_30333_32200 = state_30303__$1;
(statearr_30333_32200[(2)] = null);

(statearr_30333_32200[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (4))){
var inst_30285 = (state_30303[(7)]);
var inst_30285__$1 = (state_30303[(2)]);
var inst_30286 = (inst_30285__$1 == null);
var state_30303__$1 = (function (){var statearr_30334 = state_30303;
(statearr_30334[(7)] = inst_30285__$1);

return statearr_30334;
})();
if(cljs.core.truth_(inst_30286)){
var statearr_30335_32201 = state_30303__$1;
(statearr_30335_32201[(1)] = (5));

} else {
var statearr_30336_32202 = state_30303__$1;
(statearr_30336_32202[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (6))){
var inst_30285 = (state_30303[(7)]);
var inst_30290 = (p.cljs$core$IFn$_invoke$arity$1 ? p.cljs$core$IFn$_invoke$arity$1(inst_30285) : p.call(null,inst_30285));
var state_30303__$1 = state_30303;
if(cljs.core.truth_(inst_30290)){
var statearr_30339_32204 = state_30303__$1;
(statearr_30339_32204[(1)] = (8));

} else {
var statearr_30340_32205 = state_30303__$1;
(statearr_30340_32205[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (3))){
var inst_30301 = (state_30303[(2)]);
var state_30303__$1 = state_30303;
return cljs.core.async.impl.ioc_helpers.return_chan(state_30303__$1,inst_30301);
} else {
if((state_val_30304 === (2))){
var state_30303__$1 = state_30303;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30303__$1,(4),ch);
} else {
if((state_val_30304 === (11))){
var inst_30293 = (state_30303[(2)]);
var state_30303__$1 = state_30303;
var statearr_30341_32207 = state_30303__$1;
(statearr_30341_32207[(2)] = inst_30293);

(statearr_30341_32207[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (9))){
var state_30303__$1 = state_30303;
var statearr_30343_32208 = state_30303__$1;
(statearr_30343_32208[(2)] = null);

(statearr_30343_32208[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (5))){
var inst_30288 = cljs.core.async.close_BANG_(out);
var state_30303__$1 = state_30303;
var statearr_30351_32210 = state_30303__$1;
(statearr_30351_32210[(2)] = inst_30288);

(statearr_30351_32210[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (10))){
var inst_30296 = (state_30303[(2)]);
var state_30303__$1 = (function (){var statearr_30352 = state_30303;
(statearr_30352[(8)] = inst_30296);

return statearr_30352;
})();
var statearr_30353_32211 = state_30303__$1;
(statearr_30353_32211[(2)] = null);

(statearr_30353_32211[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30304 === (8))){
var inst_30285 = (state_30303[(7)]);
var state_30303__$1 = state_30303;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30303__$1,(11),out,inst_30285);
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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30366 = [null,null,null,null,null,null,null,null,null];
(statearr_30366[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30366[(1)] = (1));

return statearr_30366;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30303){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30303);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30367){var ex__11650__auto__ = e30367;
var statearr_30368_32216 = state_30303;
(statearr_30368_32216[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30303[(4)]))){
var statearr_30377_32217 = state_30303;
(statearr_30377_32217[(1)] = cljs.core.first((state_30303[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32218 = state_30303;
state_30303 = G__32218;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30303){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30303);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30381 = f__11734__auto__();
(statearr_30381[(6)] = c__11733__auto___32198);

return statearr_30381;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


return out;
}));

(cljs.core.async.filter_LT_.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.remove_LT_ = (function cljs$core$async$remove_LT_(var_args){
var G__30392 = arguments.length;
switch (G__30392) {
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
var c__11733__auto__ = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30482){
var state_val_30483 = (state_30482[(1)]);
if((state_val_30483 === (7))){
var inst_30474 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
var statearr_30491_32224 = state_30482__$1;
(statearr_30491_32224[(2)] = inst_30474);

(statearr_30491_32224[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (20))){
var inst_30440 = (state_30482[(7)]);
var inst_30452 = (state_30482[(2)]);
var inst_30453 = cljs.core.next(inst_30440);
var inst_30422 = inst_30453;
var inst_30423 = null;
var inst_30424 = (0);
var inst_30425 = (0);
var state_30482__$1 = (function (){var statearr_30493 = state_30482;
(statearr_30493[(8)] = inst_30452);

(statearr_30493[(9)] = inst_30422);

(statearr_30493[(10)] = inst_30423);

(statearr_30493[(11)] = inst_30424);

(statearr_30493[(12)] = inst_30425);

return statearr_30493;
})();
var statearr_30494_32225 = state_30482__$1;
(statearr_30494_32225[(2)] = null);

(statearr_30494_32225[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (1))){
var state_30482__$1 = state_30482;
var statearr_30495_32226 = state_30482__$1;
(statearr_30495_32226[(2)] = null);

(statearr_30495_32226[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (4))){
var inst_30408 = (state_30482[(13)]);
var inst_30408__$1 = (state_30482[(2)]);
var inst_30409 = (inst_30408__$1 == null);
var state_30482__$1 = (function (){var statearr_30496 = state_30482;
(statearr_30496[(13)] = inst_30408__$1);

return statearr_30496;
})();
if(cljs.core.truth_(inst_30409)){
var statearr_30497_32233 = state_30482__$1;
(statearr_30497_32233[(1)] = (5));

} else {
var statearr_30498_32234 = state_30482__$1;
(statearr_30498_32234[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (15))){
var state_30482__$1 = state_30482;
var statearr_30503_32235 = state_30482__$1;
(statearr_30503_32235[(2)] = null);

(statearr_30503_32235[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (21))){
var state_30482__$1 = state_30482;
var statearr_30505_32236 = state_30482__$1;
(statearr_30505_32236[(2)] = null);

(statearr_30505_32236[(1)] = (23));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (13))){
var inst_30425 = (state_30482[(12)]);
var inst_30422 = (state_30482[(9)]);
var inst_30423 = (state_30482[(10)]);
var inst_30424 = (state_30482[(11)]);
var inst_30434 = (state_30482[(2)]);
var inst_30437 = (inst_30425 + (1));
var tmp30500 = inst_30423;
var tmp30501 = inst_30424;
var tmp30502 = inst_30422;
var inst_30422__$1 = tmp30502;
var inst_30423__$1 = tmp30500;
var inst_30424__$1 = tmp30501;
var inst_30425__$1 = inst_30437;
var state_30482__$1 = (function (){var statearr_30506 = state_30482;
(statearr_30506[(14)] = inst_30434);

(statearr_30506[(9)] = inst_30422__$1);

(statearr_30506[(10)] = inst_30423__$1);

(statearr_30506[(11)] = inst_30424__$1);

(statearr_30506[(12)] = inst_30425__$1);

return statearr_30506;
})();
var statearr_30507_32240 = state_30482__$1;
(statearr_30507_32240[(2)] = null);

(statearr_30507_32240[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (22))){
var state_30482__$1 = state_30482;
var statearr_30508_32241 = state_30482__$1;
(statearr_30508_32241[(2)] = null);

(statearr_30508_32241[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (6))){
var inst_30408 = (state_30482[(13)]);
var inst_30417 = (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(inst_30408) : f.call(null,inst_30408));
var inst_30418 = cljs.core.seq(inst_30417);
var inst_30422 = inst_30418;
var inst_30423 = null;
var inst_30424 = (0);
var inst_30425 = (0);
var state_30482__$1 = (function (){var statearr_30512 = state_30482;
(statearr_30512[(9)] = inst_30422);

(statearr_30512[(10)] = inst_30423);

(statearr_30512[(11)] = inst_30424);

(statearr_30512[(12)] = inst_30425);

return statearr_30512;
})();
var statearr_30516_32242 = state_30482__$1;
(statearr_30516_32242[(2)] = null);

(statearr_30516_32242[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (17))){
var inst_30440 = (state_30482[(7)]);
var inst_30444 = cljs.core.chunk_first(inst_30440);
var inst_30445 = cljs.core.chunk_rest(inst_30440);
var inst_30447 = cljs.core.count(inst_30444);
var inst_30422 = inst_30445;
var inst_30423 = inst_30444;
var inst_30424 = inst_30447;
var inst_30425 = (0);
var state_30482__$1 = (function (){var statearr_30517 = state_30482;
(statearr_30517[(9)] = inst_30422);

(statearr_30517[(10)] = inst_30423);

(statearr_30517[(11)] = inst_30424);

(statearr_30517[(12)] = inst_30425);

return statearr_30517;
})();
var statearr_30519_32243 = state_30482__$1;
(statearr_30519_32243[(2)] = null);

(statearr_30519_32243[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (3))){
var inst_30476 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
return cljs.core.async.impl.ioc_helpers.return_chan(state_30482__$1,inst_30476);
} else {
if((state_val_30483 === (12))){
var inst_30463 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
var statearr_30524_32245 = state_30482__$1;
(statearr_30524_32245[(2)] = inst_30463);

(statearr_30524_32245[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (2))){
var state_30482__$1 = state_30482;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30482__$1,(4),in$);
} else {
if((state_val_30483 === (23))){
var inst_30472 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
var statearr_30528_32246 = state_30482__$1;
(statearr_30528_32246[(2)] = inst_30472);

(statearr_30528_32246[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (19))){
var inst_30457 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
var statearr_30530_32247 = state_30482__$1;
(statearr_30530_32247[(2)] = inst_30457);

(statearr_30530_32247[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (11))){
var inst_30422 = (state_30482[(9)]);
var inst_30440 = (state_30482[(7)]);
var inst_30440__$1 = cljs.core.seq(inst_30422);
var state_30482__$1 = (function (){var statearr_30531 = state_30482;
(statearr_30531[(7)] = inst_30440__$1);

return statearr_30531;
})();
if(inst_30440__$1){
var statearr_30532_32248 = state_30482__$1;
(statearr_30532_32248[(1)] = (14));

} else {
var statearr_30533_32250 = state_30482__$1;
(statearr_30533_32250[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (9))){
var inst_30466 = (state_30482[(2)]);
var inst_30467 = cljs.core.async.impl.protocols.closed_QMARK_(out);
var state_30482__$1 = (function (){var statearr_30534 = state_30482;
(statearr_30534[(15)] = inst_30466);

return statearr_30534;
})();
if(cljs.core.truth_(inst_30467)){
var statearr_30537_32254 = state_30482__$1;
(statearr_30537_32254[(1)] = (21));

} else {
var statearr_30538_32255 = state_30482__$1;
(statearr_30538_32255[(1)] = (22));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (5))){
var inst_30411 = cljs.core.async.close_BANG_(out);
var state_30482__$1 = state_30482;
var statearr_30539_32256 = state_30482__$1;
(statearr_30539_32256[(2)] = inst_30411);

(statearr_30539_32256[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (14))){
var inst_30440 = (state_30482[(7)]);
var inst_30442 = cljs.core.chunked_seq_QMARK_(inst_30440);
var state_30482__$1 = state_30482;
if(inst_30442){
var statearr_30540_32257 = state_30482__$1;
(statearr_30540_32257[(1)] = (17));

} else {
var statearr_30542_32258 = state_30482__$1;
(statearr_30542_32258[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (16))){
var inst_30460 = (state_30482[(2)]);
var state_30482__$1 = state_30482;
var statearr_30543_32259 = state_30482__$1;
(statearr_30543_32259[(2)] = inst_30460);

(statearr_30543_32259[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30483 === (10))){
var inst_30423 = (state_30482[(10)]);
var inst_30425 = (state_30482[(12)]);
var inst_30432 = cljs.core._nth(inst_30423,inst_30425);
var state_30482__$1 = state_30482;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30482__$1,(13),out,inst_30432);
} else {
if((state_val_30483 === (18))){
var inst_30440 = (state_30482[(7)]);
var inst_30450 = cljs.core.first(inst_30440);
var state_30482__$1 = state_30482;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30482__$1,(20),out,inst_30450);
} else {
if((state_val_30483 === (8))){
var inst_30425 = (state_30482[(12)]);
var inst_30424 = (state_30482[(11)]);
var inst_30428 = (inst_30425 < inst_30424);
var inst_30429 = inst_30428;
var state_30482__$1 = state_30482;
if(cljs.core.truth_(inst_30429)){
var statearr_30546_32261 = state_30482__$1;
(statearr_30546_32261[(1)] = (10));

} else {
var statearr_30547_32262 = state_30482__$1;
(statearr_30547_32262[(1)] = (11));

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
var cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__ = null;
var cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____0 = (function (){
var statearr_30556 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_30556[(0)] = cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__);

(statearr_30556[(1)] = (1));

return statearr_30556;
});
var cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____1 = (function (state_30482){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30482);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30561){var ex__11650__auto__ = e30561;
var statearr_30562_32263 = state_30482;
(statearr_30562_32263[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30482[(4)]))){
var statearr_30563_32265 = state_30482;
(statearr_30563_32265[(1)] = cljs.core.first((state_30482[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32266 = state_30482;
state_30482 = G__32266;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__ = function(state_30482){
switch(arguments.length){
case 0:
return cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____1.call(this,state_30482);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____0;
cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mapcat_STAR__$_state_machine__11647__auto____1;
return cljs$core$async$mapcat_STAR__$_state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30566 = f__11734__auto__();
(statearr_30566[(6)] = c__11733__auto__);

return statearr_30566;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));

return c__11733__auto__;
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.mapcat_LT_ = (function cljs$core$async$mapcat_LT_(var_args){
var G__30568 = arguments.length;
switch (G__30568) {
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
var G__30576 = arguments.length;
switch (G__30576) {
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
var G__30580 = arguments.length;
switch (G__30580) {
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
var c__11733__auto___32283 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30627){
var state_val_30628 = (state_30627[(1)]);
if((state_val_30628 === (7))){
var inst_30619 = (state_30627[(2)]);
var state_30627__$1 = state_30627;
var statearr_30646_32285 = state_30627__$1;
(statearr_30646_32285[(2)] = inst_30619);

(statearr_30646_32285[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (1))){
var inst_30591 = null;
var state_30627__$1 = (function (){var statearr_30654 = state_30627;
(statearr_30654[(7)] = inst_30591);

return statearr_30654;
})();
var statearr_30656_32286 = state_30627__$1;
(statearr_30656_32286[(2)] = null);

(statearr_30656_32286[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (4))){
var inst_30599 = (state_30627[(8)]);
var inst_30599__$1 = (state_30627[(2)]);
var inst_30601 = (inst_30599__$1 == null);
var inst_30602 = cljs.core.not(inst_30601);
var state_30627__$1 = (function (){var statearr_30670 = state_30627;
(statearr_30670[(8)] = inst_30599__$1);

return statearr_30670;
})();
if(inst_30602){
var statearr_30671_32287 = state_30627__$1;
(statearr_30671_32287[(1)] = (5));

} else {
var statearr_30673_32288 = state_30627__$1;
(statearr_30673_32288[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (6))){
var state_30627__$1 = state_30627;
var statearr_30674_32289 = state_30627__$1;
(statearr_30674_32289[(2)] = null);

(statearr_30674_32289[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (3))){
var inst_30621 = (state_30627[(2)]);
var inst_30622 = cljs.core.async.close_BANG_(out);
var state_30627__$1 = (function (){var statearr_30677 = state_30627;
(statearr_30677[(9)] = inst_30621);

return statearr_30677;
})();
return cljs.core.async.impl.ioc_helpers.return_chan(state_30627__$1,inst_30622);
} else {
if((state_val_30628 === (2))){
var state_30627__$1 = state_30627;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30627__$1,(4),ch);
} else {
if((state_val_30628 === (11))){
var inst_30599 = (state_30627[(8)]);
var inst_30609 = (state_30627[(2)]);
var inst_30591 = inst_30599;
var state_30627__$1 = (function (){var statearr_30678 = state_30627;
(statearr_30678[(10)] = inst_30609);

(statearr_30678[(7)] = inst_30591);

return statearr_30678;
})();
var statearr_30679_32296 = state_30627__$1;
(statearr_30679_32296[(2)] = null);

(statearr_30679_32296[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (9))){
var inst_30599 = (state_30627[(8)]);
var state_30627__$1 = state_30627;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30627__$1,(11),out,inst_30599);
} else {
if((state_val_30628 === (5))){
var inst_30599 = (state_30627[(8)]);
var inst_30591 = (state_30627[(7)]);
var inst_30604 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_30599,inst_30591);
var state_30627__$1 = state_30627;
if(inst_30604){
var statearr_30682_32298 = state_30627__$1;
(statearr_30682_32298[(1)] = (8));

} else {
var statearr_30683_32299 = state_30627__$1;
(statearr_30683_32299[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (10))){
var inst_30614 = (state_30627[(2)]);
var state_30627__$1 = state_30627;
var statearr_30684_32300 = state_30627__$1;
(statearr_30684_32300[(2)] = inst_30614);

(statearr_30684_32300[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30628 === (8))){
var inst_30591 = (state_30627[(7)]);
var tmp30680 = inst_30591;
var inst_30591__$1 = tmp30680;
var state_30627__$1 = (function (){var statearr_30685 = state_30627;
(statearr_30685[(7)] = inst_30591__$1);

return statearr_30685;
})();
var statearr_30686_32302 = state_30627__$1;
(statearr_30686_32302[(2)] = null);

(statearr_30686_32302[(1)] = (2));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30691 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_30691[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30691[(1)] = (1));

return statearr_30691;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30627){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30627);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30696){var ex__11650__auto__ = e30696;
var statearr_30697_32307 = state_30627;
(statearr_30697_32307[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30627[(4)]))){
var statearr_30702_32308 = state_30627;
(statearr_30702_32308[(1)] = cljs.core.first((state_30627[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32310 = state_30627;
state_30627 = G__32310;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30627){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30627);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30703 = f__11734__auto__();
(statearr_30703[(6)] = c__11733__auto___32283);

return statearr_30703;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


return out;
}));

(cljs.core.async.unique.cljs$lang$maxFixedArity = 2);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition = (function cljs$core$async$partition(var_args){
var G__30705 = arguments.length;
switch (G__30705) {
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
var c__11733__auto___32313 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30744){
var state_val_30745 = (state_30744[(1)]);
if((state_val_30745 === (7))){
var inst_30740 = (state_30744[(2)]);
var state_30744__$1 = state_30744;
var statearr_30746_32320 = state_30744__$1;
(statearr_30746_32320[(2)] = inst_30740);

(statearr_30746_32320[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (1))){
var inst_30706 = (new Array(n));
var inst_30707 = inst_30706;
var inst_30708 = (0);
var state_30744__$1 = (function (){var statearr_30747 = state_30744;
(statearr_30747[(7)] = inst_30707);

(statearr_30747[(8)] = inst_30708);

return statearr_30747;
})();
var statearr_30748_32321 = state_30744__$1;
(statearr_30748_32321[(2)] = null);

(statearr_30748_32321[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (4))){
var inst_30711 = (state_30744[(9)]);
var inst_30711__$1 = (state_30744[(2)]);
var inst_30712 = (inst_30711__$1 == null);
var inst_30713 = cljs.core.not(inst_30712);
var state_30744__$1 = (function (){var statearr_30751 = state_30744;
(statearr_30751[(9)] = inst_30711__$1);

return statearr_30751;
})();
if(inst_30713){
var statearr_30752_32324 = state_30744__$1;
(statearr_30752_32324[(1)] = (5));

} else {
var statearr_30753_32325 = state_30744__$1;
(statearr_30753_32325[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (15))){
var inst_30734 = (state_30744[(2)]);
var state_30744__$1 = state_30744;
var statearr_30754_32326 = state_30744__$1;
(statearr_30754_32326[(2)] = inst_30734);

(statearr_30754_32326[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (13))){
var state_30744__$1 = state_30744;
var statearr_30757_32327 = state_30744__$1;
(statearr_30757_32327[(2)] = null);

(statearr_30757_32327[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (6))){
var inst_30708 = (state_30744[(8)]);
var inst_30730 = (inst_30708 > (0));
var state_30744__$1 = state_30744;
if(cljs.core.truth_(inst_30730)){
var statearr_30759_32330 = state_30744__$1;
(statearr_30759_32330[(1)] = (12));

} else {
var statearr_30761_32333 = state_30744__$1;
(statearr_30761_32333[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (3))){
var inst_30742 = (state_30744[(2)]);
var state_30744__$1 = state_30744;
return cljs.core.async.impl.ioc_helpers.return_chan(state_30744__$1,inst_30742);
} else {
if((state_val_30745 === (12))){
var inst_30707 = (state_30744[(7)]);
var inst_30732 = cljs.core.vec(inst_30707);
var state_30744__$1 = state_30744;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30744__$1,(15),out,inst_30732);
} else {
if((state_val_30745 === (2))){
var state_30744__$1 = state_30744;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30744__$1,(4),ch);
} else {
if((state_val_30745 === (11))){
var inst_30724 = (state_30744[(2)]);
var inst_30725 = (new Array(n));
var inst_30707 = inst_30725;
var inst_30708 = (0);
var state_30744__$1 = (function (){var statearr_30765 = state_30744;
(statearr_30765[(10)] = inst_30724);

(statearr_30765[(7)] = inst_30707);

(statearr_30765[(8)] = inst_30708);

return statearr_30765;
})();
var statearr_30767_32336 = state_30744__$1;
(statearr_30767_32336[(2)] = null);

(statearr_30767_32336[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (9))){
var inst_30707 = (state_30744[(7)]);
var inst_30722 = cljs.core.vec(inst_30707);
var state_30744__$1 = state_30744;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30744__$1,(11),out,inst_30722);
} else {
if((state_val_30745 === (5))){
var inst_30707 = (state_30744[(7)]);
var inst_30708 = (state_30744[(8)]);
var inst_30711 = (state_30744[(9)]);
var inst_30717 = (state_30744[(11)]);
var inst_30716 = (inst_30707[inst_30708] = inst_30711);
var inst_30717__$1 = (inst_30708 + (1));
var inst_30718 = (inst_30717__$1 < n);
var state_30744__$1 = (function (){var statearr_30769 = state_30744;
(statearr_30769[(12)] = inst_30716);

(statearr_30769[(11)] = inst_30717__$1);

return statearr_30769;
})();
if(cljs.core.truth_(inst_30718)){
var statearr_30770_32338 = state_30744__$1;
(statearr_30770_32338[(1)] = (8));

} else {
var statearr_30771_32339 = state_30744__$1;
(statearr_30771_32339[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (14))){
var inst_30737 = (state_30744[(2)]);
var inst_30738 = cljs.core.async.close_BANG_(out);
var state_30744__$1 = (function (){var statearr_30776 = state_30744;
(statearr_30776[(13)] = inst_30737);

return statearr_30776;
})();
var statearr_30777_32340 = state_30744__$1;
(statearr_30777_32340[(2)] = inst_30738);

(statearr_30777_32340[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (10))){
var inst_30728 = (state_30744[(2)]);
var state_30744__$1 = state_30744;
var statearr_30778_32341 = state_30744__$1;
(statearr_30778_32341[(2)] = inst_30728);

(statearr_30778_32341[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30745 === (8))){
var inst_30707 = (state_30744[(7)]);
var inst_30717 = (state_30744[(11)]);
var tmp30774 = inst_30707;
var inst_30707__$1 = tmp30774;
var inst_30708 = inst_30717;
var state_30744__$1 = (function (){var statearr_30779 = state_30744;
(statearr_30779[(7)] = inst_30707__$1);

(statearr_30779[(8)] = inst_30708);

return statearr_30779;
})();
var statearr_30780_32344 = state_30744__$1;
(statearr_30780_32344[(2)] = null);

(statearr_30780_32344[(1)] = (2));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30782 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_30782[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30782[(1)] = (1));

return statearr_30782;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30744){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30744);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30783){var ex__11650__auto__ = e30783;
var statearr_30784_32354 = state_30744;
(statearr_30784_32354[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30744[(4)]))){
var statearr_30785_32355 = state_30744;
(statearr_30785_32355[(1)] = cljs.core.first((state_30744[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32361 = state_30744;
state_30744 = G__32361;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30744){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30744);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30786 = f__11734__auto__();
(statearr_30786[(6)] = c__11733__auto___32313);

return statearr_30786;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


return out;
}));

(cljs.core.async.partition.cljs$lang$maxFixedArity = 3);

/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition_by = (function cljs$core$async$partition_by(var_args){
var G__30790 = arguments.length;
switch (G__30790) {
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
var c__11733__auto___32377 = cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((1));
cljs.core.async.impl.dispatch.run((function (){
var f__11734__auto__ = (function (){var switch__11646__auto__ = (function (state_30852){
var state_val_30853 = (state_30852[(1)]);
if((state_val_30853 === (7))){
var inst_30848 = (state_30852[(2)]);
var state_30852__$1 = state_30852;
var statearr_30858_32378 = state_30852__$1;
(statearr_30858_32378[(2)] = inst_30848);

(statearr_30858_32378[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (1))){
var inst_30802 = [];
var inst_30804 = inst_30802;
var inst_30805 = new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123);
var state_30852__$1 = (function (){var statearr_30859 = state_30852;
(statearr_30859[(7)] = inst_30804);

(statearr_30859[(8)] = inst_30805);

return statearr_30859;
})();
var statearr_30860_32379 = state_30852__$1;
(statearr_30860_32379[(2)] = null);

(statearr_30860_32379[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (4))){
var inst_30809 = (state_30852[(9)]);
var inst_30809__$1 = (state_30852[(2)]);
var inst_30810 = (inst_30809__$1 == null);
var inst_30811 = cljs.core.not(inst_30810);
var state_30852__$1 = (function (){var statearr_30867 = state_30852;
(statearr_30867[(9)] = inst_30809__$1);

return statearr_30867;
})();
if(inst_30811){
var statearr_30868_32380 = state_30852__$1;
(statearr_30868_32380[(1)] = (5));

} else {
var statearr_30873_32381 = state_30852__$1;
(statearr_30873_32381[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (15))){
var inst_30804 = (state_30852[(7)]);
var inst_30840 = cljs.core.vec(inst_30804);
var state_30852__$1 = state_30852;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30852__$1,(18),out,inst_30840);
} else {
if((state_val_30853 === (13))){
var inst_30835 = (state_30852[(2)]);
var state_30852__$1 = state_30852;
var statearr_30875_32385 = state_30852__$1;
(statearr_30875_32385[(2)] = inst_30835);

(statearr_30875_32385[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (6))){
var inst_30804 = (state_30852[(7)]);
var inst_30837 = inst_30804.length;
var inst_30838 = (inst_30837 > (0));
var state_30852__$1 = state_30852;
if(cljs.core.truth_(inst_30838)){
var statearr_30876_32388 = state_30852__$1;
(statearr_30876_32388[(1)] = (15));

} else {
var statearr_30878_32390 = state_30852__$1;
(statearr_30878_32390[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (17))){
var inst_30845 = (state_30852[(2)]);
var inst_30846 = cljs.core.async.close_BANG_(out);
var state_30852__$1 = (function (){var statearr_30879 = state_30852;
(statearr_30879[(10)] = inst_30845);

return statearr_30879;
})();
var statearr_30880_32394 = state_30852__$1;
(statearr_30880_32394[(2)] = inst_30846);

(statearr_30880_32394[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (3))){
var inst_30850 = (state_30852[(2)]);
var state_30852__$1 = state_30852;
return cljs.core.async.impl.ioc_helpers.return_chan(state_30852__$1,inst_30850);
} else {
if((state_val_30853 === (12))){
var inst_30804 = (state_30852[(7)]);
var inst_30827 = cljs.core.vec(inst_30804);
var state_30852__$1 = state_30852;
return cljs.core.async.impl.ioc_helpers.put_BANG_(state_30852__$1,(14),out,inst_30827);
} else {
if((state_val_30853 === (2))){
var state_30852__$1 = state_30852;
return cljs.core.async.impl.ioc_helpers.take_BANG_(state_30852__$1,(4),ch);
} else {
if((state_val_30853 === (11))){
var inst_30804 = (state_30852[(7)]);
var inst_30809 = (state_30852[(9)]);
var inst_30813 = (state_30852[(11)]);
var inst_30823 = inst_30804.push(inst_30809);
var tmp30881 = inst_30804;
var inst_30804__$1 = tmp30881;
var inst_30805 = inst_30813;
var state_30852__$1 = (function (){var statearr_30895 = state_30852;
(statearr_30895[(12)] = inst_30823);

(statearr_30895[(7)] = inst_30804__$1);

(statearr_30895[(8)] = inst_30805);

return statearr_30895;
})();
var statearr_30897_32398 = state_30852__$1;
(statearr_30897_32398[(2)] = null);

(statearr_30897_32398[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (9))){
var inst_30805 = (state_30852[(8)]);
var inst_30818 = cljs.core.keyword_identical_QMARK_(inst_30805,new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123));
var state_30852__$1 = state_30852;
var statearr_30902_32400 = state_30852__$1;
(statearr_30902_32400[(2)] = inst_30818);

(statearr_30902_32400[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (5))){
var inst_30809 = (state_30852[(9)]);
var inst_30813 = (state_30852[(11)]);
var inst_30805 = (state_30852[(8)]);
var inst_30814 = (state_30852[(13)]);
var inst_30813__$1 = (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(inst_30809) : f.call(null,inst_30809));
var inst_30814__$1 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(inst_30813__$1,inst_30805);
var state_30852__$1 = (function (){var statearr_30903 = state_30852;
(statearr_30903[(11)] = inst_30813__$1);

(statearr_30903[(13)] = inst_30814__$1);

return statearr_30903;
})();
if(inst_30814__$1){
var statearr_30904_32402 = state_30852__$1;
(statearr_30904_32402[(1)] = (8));

} else {
var statearr_30905_32407 = state_30852__$1;
(statearr_30905_32407[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (14))){
var inst_30809 = (state_30852[(9)]);
var inst_30813 = (state_30852[(11)]);
var inst_30829 = (state_30852[(2)]);
var inst_30831 = [];
var inst_30832 = inst_30831.push(inst_30809);
var inst_30804 = inst_30831;
var inst_30805 = inst_30813;
var state_30852__$1 = (function (){var statearr_30906 = state_30852;
(statearr_30906[(14)] = inst_30829);

(statearr_30906[(15)] = inst_30832);

(statearr_30906[(7)] = inst_30804);

(statearr_30906[(8)] = inst_30805);

return statearr_30906;
})();
var statearr_30908_32420 = state_30852__$1;
(statearr_30908_32420[(2)] = null);

(statearr_30908_32420[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (16))){
var state_30852__$1 = state_30852;
var statearr_30910_32421 = state_30852__$1;
(statearr_30910_32421[(2)] = null);

(statearr_30910_32421[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (10))){
var inst_30820 = (state_30852[(2)]);
var state_30852__$1 = state_30852;
if(cljs.core.truth_(inst_30820)){
var statearr_30911_32422 = state_30852__$1;
(statearr_30911_32422[(1)] = (11));

} else {
var statearr_30912_32423 = state_30852__$1;
(statearr_30912_32423[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (18))){
var inst_30842 = (state_30852[(2)]);
var state_30852__$1 = state_30852;
var statearr_30913_32424 = state_30852__$1;
(statearr_30913_32424[(2)] = inst_30842);

(statearr_30913_32424[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30853 === (8))){
var inst_30814 = (state_30852[(13)]);
var state_30852__$1 = state_30852;
var statearr_30914_32426 = state_30852__$1;
(statearr_30914_32426[(2)] = inst_30814);

(statearr_30914_32426[(1)] = (10));


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
var cljs$core$async$state_machine__11647__auto__ = null;
var cljs$core$async$state_machine__11647__auto____0 = (function (){
var statearr_30915 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_30915[(0)] = cljs$core$async$state_machine__11647__auto__);

(statearr_30915[(1)] = (1));

return statearr_30915;
});
var cljs$core$async$state_machine__11647__auto____1 = (function (state_30852){
while(true){
var ret_value__11648__auto__ = (function (){try{while(true){
var result__11649__auto__ = switch__11646__auto__(state_30852);
if(cljs.core.keyword_identical_QMARK_(result__11649__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__11649__auto__;
}
break;
}
}catch (e30916){var ex__11650__auto__ = e30916;
var statearr_30917_32429 = state_30852;
(statearr_30917_32429[(2)] = ex__11650__auto__);


if(cljs.core.seq((state_30852[(4)]))){
var statearr_30918_32431 = state_30852;
(statearr_30918_32431[(1)] = cljs.core.first((state_30852[(4)])));

} else {
throw ex__11650__auto__;
}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
}})();
if(cljs.core.keyword_identical_QMARK_(ret_value__11648__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__32435 = state_30852;
state_30852 = G__32435;
continue;
} else {
return ret_value__11648__auto__;
}
break;
}
});
cljs$core$async$state_machine__11647__auto__ = function(state_30852){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__11647__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__11647__auto____1.call(this,state_30852);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__11647__auto____0;
cljs$core$async$state_machine__11647__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__11647__auto____1;
return cljs$core$async$state_machine__11647__auto__;
})()
})();
var state__11735__auto__ = (function (){var statearr_30925 = f__11734__auto__();
(statearr_30925[(6)] = c__11733__auto___32377);

return statearr_30925;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped(state__11735__auto__);
}));


return out;
}));

(cljs.core.async.partition_by.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=cljs.core.async.js.map
