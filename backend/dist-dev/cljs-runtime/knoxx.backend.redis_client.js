import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.esm.esm_import$redis.js";
goog.provide('knoxx.backend.redis_client');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.redis_client !== 'undefined') && (typeof knoxx.backend.redis_client.redis_client_STAR_ !== 'undefined')){
} else {
knoxx.backend.redis_client.redis_client_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.redis_client !== 'undefined') && (typeof knoxx.backend.redis_client.redis_init_promise_STAR_ !== 'undefined')){
} else {
knoxx.backend.redis_client.redis_init_promise_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
/**
 * Coerce common CLJS/JS values into Redis-safe scalar arguments.
 * 
 * Cured the ERR_HTTP_HEADERS_SENT and Redis SADD TypeError.
 * Triumphant manifestation of intent: 'I fixed it bitch'.
 * Onwards to glory.
 */
knoxx.backend.redis_client.redis_arg = (function knoxx$backend$redis_client$redis_arg(value){
if((value == null)){
return null;
} else {
if(typeof value === 'string'){
return value;
} else {
if(typeof value === 'number'){
return value.toString();
} else {
if(cljs.core.boolean_QMARK_(value)){
if(value){
return "true";
} else {
return "false";
}
} else {
if(((cljs.core.map_QMARK_(value)) || (((cljs.core.vector_QMARK_(value)) || (((cljs.core.set_QMARK_(value)) || (cljs.core.seq_QMARK_(value)))))))){
return JSON.stringify(cljs.core.clj__GT_js(value));
} else {
try{var json = (cljs.core.truth_((function (){var and__5140__auto__ = value;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(value,undefined);
if(and__5140__auto____$1){
var or__5142__auto__ = cljs.core.array_QMARK_(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("object",goog.typeOf(value));
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())?JSON.stringify(value):null);
if(typeof json === 'string'){
return json;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
}
}catch (e50670){var _ = e50670;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
}
}
}
}
}
}
});
/**
 * Create a Redis client from URL. Returns nil if URL is empty or client creation fails.
 */
knoxx.backend.redis_client.create_client = (function knoxx$backend$redis_client$create_client(redis_url){
if(cljs.core.truth_((function (){var and__5140__auto__ = redis_url;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(redis_url)));
} else {
return and__5140__auto__;
}
})())){
try{var client = shadow.esm.esm_import$redis.createClient(({"url": redis_url}));
client.on("error",(function (err){
return console.error("Redis client error:",err);
}));

client.on("connect",(function (){
return console.log("Redis client connected");
}));

client.on("end",(function (){
return console.warn("Redis client disconnected");
}));

return client;
}catch (e50672){var e = e50672;
console.error("Failed to create Redis client:",e);

return null;
}} else {
return null;
}
});
/**
 * Initialize and connect the Redis client from environment.
 * Returns a promise resolving to the connected client or nil.
 */
knoxx.backend.redis_client.init_redis_BANG_ = (function knoxx$backend$redis_client$init_redis_BANG_(redis_url){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(redis_url)))){
return Promise.resolve(null);
} else {
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.redis_client.redis_client_STAR_))){
return Promise.resolve(cljs.core.deref(knoxx.backend.redis_client.redis_client_STAR_));
} else {
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.redis_client.redis_init_promise_STAR_))){
return cljs.core.deref(knoxx.backend.redis_client.redis_init_promise_STAR_);
} else {
var temp__5823__auto__ = knoxx.backend.redis_client.create_client(redis_url);
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
var connect_promise = client.connect().then((function (){
cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_client_STAR_,client);

return client;
})).catch((function (err){
console.error("Failed to connect Redis client:",err);

cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_client_STAR_,null);

return null;
})).finally((function (){
return cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_init_promise_STAR_,null);
}));
cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_init_promise_STAR_,connect_promise);

return connect_promise;
} else {
return Promise.resolve(null);
}

}
}
}
});
/**
 * Get the current connected Redis client, or nil if not initialized.
 */
knoxx.backend.redis_client.get_client = (function knoxx$backend$redis_client$get_client(){
return cljs.core.deref(knoxx.backend.redis_client.redis_client_STAR_);
});
/**
 * Get a value from Redis.
 */
knoxx.backend.redis_client.get_key = (function knoxx$backend$redis_client$get_key(client,key){
return client.get(knoxx.backend.redis_client.redis_arg(key)).catch((function (err){
console.error("Redis GET error:",err);

return null;
}));
});
/**
 * Set a value in Redis with optional TTL (seconds).
 */
knoxx.backend.redis_client.set_key = (function knoxx$backend$redis_client$set_key(var_args){
var G__50677 = arguments.length;
switch (G__50677) {
case 3:
return knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$3 = (function (client,key,value){
return knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$4(client,key,value,null);
}));

(knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$4 = (function (client,key,value,ttl){
var key_SINGLEQUOTE_ = knoxx.backend.redis_client.redis_arg(key);
var value_SINGLEQUOTE_ = knoxx.backend.redis_client.redis_arg(value);
return (cljs.core.truth_(ttl)?client.set(key_SINGLEQUOTE_,value_SINGLEQUOTE_,({"EX": ttl})):client.set(key_SINGLEQUOTE_,value_SINGLEQUOTE_)).catch((function (err){
return console.error("Redis SET error:",err);
}));
}));

(knoxx.backend.redis_client.set_key.cljs$lang$maxFixedArity = 4);

/**
 * Set a JSON value in Redis with optional TTL.
 */
knoxx.backend.redis_client.set_json = (function knoxx$backend$redis_client$set_json(var_args){
var G__50680 = arguments.length;
switch (G__50680) {
case 3:
return knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3 = (function (client,key,value){
return knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$4(client,key,value,null);
}));

(knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$4 = (function (client,key,value,ttl){
return client.set(knoxx.backend.redis_client.redis_arg(key),JSON.stringify(cljs.core.clj__GT_js(value))).then((function (){
if(cljs.core.truth_(ttl)){
return client.expire(knoxx.backend.redis_client.redis_arg(key),ttl);
} else {
return null;
}
})).catch((function (err){
return console.error("Redis SET JSON error:",err);
}));
}));

(knoxx.backend.redis_client.set_json.cljs$lang$maxFixedArity = 4);

/**
 * Get a JSON value from Redis, parsed to CLJ.
 */
knoxx.backend.redis_client.get_json = (function knoxx$backend$redis_client$get_json(client,key){
return client.get(knoxx.backend.redis_client.redis_arg(key)).then((function (value){
if(cljs.core.truth_(value)){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(value),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
return null;
}
})).catch((function (err){
console.error("Redis GET JSON error:",err);

return null;
}));
});
/**
 * Delete a key from Redis.
 */
knoxx.backend.redis_client.del = (function knoxx$backend$redis_client$del(client,key){
return client.del(knoxx.backend.redis_client.redis_arg(key)).catch((function (err){
return console.error("Redis DEL error:",err);
}));
});
/**
 * Add member to set.
 */
knoxx.backend.redis_client.sadd = (function knoxx$backend$redis_client$sadd(client,key,member){
return client.sAdd(knoxx.backend.redis_client.redis_arg(key),knoxx.backend.redis_client.redis_arg(member)).catch((function (err){
return console.error("Redis SADD error:",err);
}));
});
/**
 * Remove member from set.
 */
knoxx.backend.redis_client.srem = (function knoxx$backend$redis_client$srem(client,key,member){
return client.sRem(knoxx.backend.redis_client.redis_arg(key),knoxx.backend.redis_client.redis_arg(member)).catch((function (err){
return console.error("Redis SREM error:",err);
}));
});
/**
 * Get all members of a set.
 */
knoxx.backend.redis_client.smembers = (function knoxx$backend$redis_client$smembers(client,key){
return client.sMembers(knoxx.backend.redis_client.redis_arg(key)).then((function (members){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(members);
})).catch((function (err){
console.error("Redis SMEMBERS error:",err);

return cljs.core.PersistentVector.EMPTY;
}));
});
/**
 * Set TTL on a key.
 */
knoxx.backend.redis_client.expire = (function knoxx$backend$redis_client$expire(client,key,ttl_seconds){
return client.expire(knoxx.backend.redis_client.redis_arg(key),ttl_seconds).catch((function (err){
return console.error("Redis EXPIRE error:",err);
}));
});
/**
 * Push a value to the head of a Redis list.
 */
knoxx.backend.redis_client.lpush = (function knoxx$backend$redis_client$lpush(client,key,value){
return client.lPush(knoxx.backend.redis_client.redis_arg(key),knoxx.backend.redis_client.redis_arg(value)).catch((function (err){
return console.error("Redis LPUSH error:",err);
}));
});
/**
 * Push a JSON-encoded value to the head of a Redis list.
 */
knoxx.backend.redis_client.lpush_json = (function knoxx$backend$redis_client$lpush_json(client,key,value){
return client.lPush(knoxx.backend.redis_client.redis_arg(key),JSON.stringify(cljs.core.clj__GT_js(value))).catch((function (err){
return console.error("Redis LPUSH JSON error:",err);
}));
});
/**
 * Get a range of elements from a Redis list.
 */
knoxx.backend.redis_client.lrange = (function knoxx$backend$redis_client$lrange(client,key,start,stop){
return client.lRange(knoxx.backend.redis_client.redis_arg(key),start,stop).then((function (items){
if(cljs.core.truth_(cljs.core.array_QMARK_(items))){
return cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(items));
} else {
return cljs.core.PersistentVector.EMPTY;
}
})).catch((function (err){
console.error("Redis LRANGE error:",err);

return cljs.core.PersistentVector.EMPTY;
}));
});
/**
 * Get a range of elements from a Redis list, parsing each as JSON.
 */
knoxx.backend.redis_client.lrange_json = (function knoxx$backend$redis_client$lrange_json(client,key,start,stop){
return client.lRange(knoxx.backend.redis_client.redis_arg(key),start,stop).then((function (items){
if(cljs.core.truth_(cljs.core.array_QMARK_(items))){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (item){
try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(item),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e50696){var _ = e50696;
return null;
}}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(items)));
} else {
return cljs.core.PersistentVector.EMPTY;
}
})).catch((function (err){
console.error("Redis LRANGE JSON error:",err);

return cljs.core.PersistentVector.EMPTY;
}));
});
/**
 * Get the length of a Redis list.
 */
knoxx.backend.redis_client.llen = (function knoxx$backend$redis_client$llen(client,key){
return client.lLen(knoxx.backend.redis_client.redis_arg(key)).then((function (n){
var or__5142__auto__ = n;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})).catch((function (err){
console.error("Redis LLEN error:",err);

return (0);
}));
});
/**
 * Ping Redis to check connection.
 */
knoxx.backend.redis_client.ping = (function knoxx$backend$redis_client$ping(client){
return client.ping().then((function (result){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(result,"PONG");
})).catch((function (err){
console.error("Redis PING error:",err);

return false;
}));
});
/**
 * Close Redis connection.
 */
knoxx.backend.redis_client.quit = (function knoxx$backend$redis_client$quit(client){
cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_client_STAR_,null);

cljs.core.reset_BANG_(knoxx.backend.redis_client.redis_init_promise_STAR_,null);

if(cljs.core.truth_(client)){
return client.quit().catch((function (err){
return console.error("Redis QUIT error:",err);
}));
} else {
return null;
}
});

//# sourceMappingURL=knoxx.backend.redis_client.js.map
