import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.esm.esm_import$discord.js";
import "./shadow.esm.esm_import$$discordjs$voice.js";
import "./shadow.esm.esm_import$prism_media.js";
import "./shadow.esm.esm_import$libsodium_wrappers.js";
import "./shadow.esm.esm_import$node_stream.js";
goog.provide('knoxx.backend.discord_gateway');
knoxx.backend.discord_gateway.voice_listener_sample_rate = (48000);
knoxx.backend.discord_gateway.voice_listener_channels = (2);
knoxx.backend.discord_gateway.voice_listener_bytes_per_sample = (2);
knoxx.backend.discord_gateway.voice_listener_min_duration_s = 0.8;
knoxx.backend.discord_gateway.voice_listener_silence_debounce_ms = (900);
knoxx.backend.discord_gateway.intent_bits = (function knoxx$backend$discord_gateway$intent_bits(){
return (shadow.esm.esm_import$discord["GatewayIntentBits"]);
});
knoxx.backend.discord_gateway.partials_enum = (function knoxx$backend$discord_gateway$partials_enum(){
return (shadow.esm.esm_import$discord["Partials"]);
});
knoxx.backend.discord_gateway.events_enum = (function knoxx$backend$discord_gateway$events_enum(){
return (shadow.esm.esm_import$discord["Events"]);
});
knoxx.backend.discord_gateway.channel_type_enum = (function knoxx$backend$discord_gateway$channel_type_enum(){
return (shadow.esm.esm_import$discord["ChannelType"]);
});
knoxx.backend.discord_gateway.Client_class = (function knoxx$backend$discord_gateway$Client_class(){
return (shadow.esm.esm_import$discord["Client"]);
});
/**
 * Wrap raw PCM16LE bytes in a WAV container so ffmpeg (and thus STT) can decode it.
 * 
 * pcm: Node Buffer of signed 16-bit little-endian samples.
 * rate: sample rate in Hz (Discord voice is typically 48000)
 * channels: 1 or 2 (Discord voice is typically 2)
 * 
 * Returns a Node Buffer containing a complete .wav file.
 */
knoxx.backend.discord_gateway.pcm16le__GT_wav_buffer = (function knoxx$backend$discord_gateway$pcm16le__GT_wav_buffer(pcm,rate,channels){
var rate__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.long$((function (){var or__5142__auto__ = rate;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (48000);
}
})()));
var channels__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.long$((function (){var or__5142__auto__ = channels;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (2);
}
})()));
var data_size = pcm.length;
var byte_rate = ((rate__$1 * channels__$1) * (2));
var block_align = (channels__$1 * (2));
var wav = Buffer.alloc(((44) + data_size));
wav.write("RIFF",(0));

wav.writeUInt32LE(((36) + data_size),(4));

wav.write("WAVE",(8));

wav.write("fmt ",(12));

wav.writeUInt32LE((16),(16));

wav.writeUInt16LE((1),(20));

wav.writeUInt16LE(channels__$1,(22));

wav.writeUInt32LE(rate__$1,(24));

wav.writeUInt32LE(byte_rate,(28));

wav.writeUInt16LE(block_align,(32));

wav.writeUInt16LE((16),(34));

wav.write("data",(36));

wav.writeUInt32LE(data_size,(40));

pcm.copy(wav,(44));

return wav;
});
/**
 * Convert a discord.js Message to a plain JS map.
 */
knoxx.backend.discord_gateway.map_message = (function knoxx$backend$discord_gateway$map_message(message){
var author = message.author;
var guild = message.guild;
return ({"authorId": (function (){var or__5142__auto__ = (cljs.core.truth_(author)?author.id:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "guildId": (function (){var or__5142__auto__ = (cljs.core.truth_(guild)?guild.id:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "content": (function (){var or__5142__auto__ = message.content;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "channelId": message.channelId, "attachments": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$map_message_$_iter__50711(s__50712){
return (new cljs.core.LazySeq(null,(function (){
var s__50712__$1 = s__50712;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__50712__$1);
if(temp__5825__auto__){
var s__50712__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__50712__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__50712__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__50714 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__50713 = (0);
while(true){
if((i__50713 < size__5627__auto__)){
var vec__50719 = cljs.core._nth(c__5626__auto__,i__50713);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50719,(0),null);
var att = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50719,(1),null);
cljs.core.chunk_append(b__50714,({"id": att.id, "filename": (function (){var or__5142__auto__ = att.name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "contentType": (function (){var or__5142__auto__ = att.contentType;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "size": (function (){var or__5142__auto__ = att.size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(), "url": (function (){var or__5142__auto__ = att.url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()}));

var G__51591 = (i__50713 + (1));
i__50713 = G__51591;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__50714),knoxx$backend$discord_gateway$map_message_$_iter__50711(cljs.core.chunk_rest(s__50712__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__50714),null);
}
} else {
var vec__50727 = cljs.core.first(s__50712__$2);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50727,(0),null);
var att = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50727,(1),null);
return cljs.core.cons(({"id": att.id, "filename": (function (){var or__5142__auto__ = att.name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "contentType": (function (){var or__5142__auto__ = att.contentType;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "size": (function (){var or__5142__auto__ = att.size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(), "url": (function (){var or__5142__auto__ = att.url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()}),knoxx$backend$discord_gateway$map_message_$_iter__50711(cljs.core.rest(s__50712__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(message.attachments);
})()), "authorUsername": (function (){var or__5142__auto__ = (cljs.core.truth_(author)?author.username:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})(), "authorIsBot": cljs.core.boolean$((cljs.core.truth_(author)?author.bot:null)), "id": message.id, "embeds": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$map_message_$_iter__50731(s__50732){
return (new cljs.core.LazySeq(null,(function (){
var s__50732__$1 = s__50732;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__50732__$1);
if(temp__5825__auto__){
var s__50732__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__50732__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__50732__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__50734 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__50733 = (0);
while(true){
if((i__50733 < size__5627__auto__)){
var embed = cljs.core._nth(c__5626__auto__,i__50733);
cljs.core.chunk_append(b__50734,({"title": (function (){var or__5142__auto__ = embed.title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "description": (function (){var or__5142__auto__ = embed.description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "url": (function (){var or__5142__auto__ = embed.url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()}));

var G__51592 = (i__50733 + (1));
i__50733 = G__51592;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__50734),knoxx$backend$discord_gateway$map_message_$_iter__50731(cljs.core.chunk_rest(s__50732__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__50734),null);
}
} else {
var embed = cljs.core.first(s__50732__$2);
return cljs.core.cons(({"title": (function (){var or__5142__auto__ = embed.title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "description": (function (){var or__5142__auto__ = embed.description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "url": (function (){var or__5142__auto__ = embed.url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()}),knoxx$backend$discord_gateway$map_message_$_iter__50731(cljs.core.rest(s__50732__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(message.embeds);
})()), "timestamp": (function (){try{return message.createdAt.toISOString();
}catch (e50741){if((e50741 instanceof Error)){
var _ = e50741;
return (new Date()).toISOString();
} else {
throw e50741;

}
}})()});
});
/**
 * Check if a channel is a text-based channel we can read.
 */
knoxx.backend.discord_gateway.readable_text_channel_QMARK_ = (function knoxx$backend$discord_gateway$readable_text_channel_QMARK_(channel){
var and__5140__auto__ = channel;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.fn_QMARK_(channel.isTextBased);
if(and__5140__auto____$1){
return channel.isTextBased();
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
/**
 * Sort an array of message maps by timestamp, newest first.
 */
knoxx.backend.discord_gateway.sort_newest_first = (function knoxx$backend$discord_gateway$sort_newest_first(messages){
return Array.from(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(messages).sort((function (a,b){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((b["timestamp"]))).localeCompare((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((a["timestamp"]))));
})));
});
/**
 * Split text into chunks of ≤2000 chars, preferring paragraph/line/word breaks.
 */
knoxx.backend.discord_gateway.split_message = (function knoxx$backend$discord_gateway$split_message(text){
var normalized = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())).trim();
if((normalized.length <= (2000))){
return [normalized];
} else {
var parts = cljs.core.atom.cljs$core$IFn$_invoke$arity$1([]);
var remaining = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(normalized);
while(true){
if((cljs.core.deref(remaining).length > (2000))){
var r_51593 = cljs.core.deref(remaining);
var split_at_para_51594 = r_51593.lastIndexOf("\n\n",(2000));
var split_at_line_51595 = r_51593.lastIndexOf("\n",(2000));
var split_at_space_51596 = r_51593.lastIndexOf(" ",(2000));
var split_at_51597 = (((split_at_para_51594 > (1000)))?split_at_para_51594:(((split_at_line_51595 > (1000)))?split_at_line_51595:(((split_at_space_51596 > (1000)))?split_at_space_51596:(2000)
)));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(parts,((function (r_51593,split_at_para_51594,split_at_line_51595,split_at_space_51596,split_at_51597,parts,remaining,normalized){
return (function (p){
return p.concat([r_51593.slice((0),split_at_51597).trimEnd()]);
});})(r_51593,split_at_para_51594,split_at_line_51595,split_at_space_51596,split_at_51597,parts,remaining,normalized))
);

cljs.core.reset_BANG_(remaining,r_51593.slice(split_at_51597).trimStart());

continue;
} else {
}
break;
}

if((cljs.core.deref(remaining).length > (0))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(parts,(function (p){
return p.concat([cljs.core.deref(remaining)]);
}));
} else {
}

return cljs.core.deref(parts);
}
});
/**
 * Read an attachment field from either a CLJS map or a plain JS object.
 */
knoxx.backend.discord_gateway.attachment_value = (function knoxx$backend$discord_gateway$attachment_value(attachment,k,js_key){
var or__5142__auto__ = ((cljs.core.map_QMARK_(attachment))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(attachment,k):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.object_QMARK_(attachment)){
return (attachment[js_key]);
} else {
return null;
}
}
});
knoxx.backend.discord_gateway.discord_file_payload = (function knoxx$backend$discord_gateway$discord_file_payload(attachment){
var buffer = (function (){var or__5142__auto__ = knoxx.backend.discord_gateway.attachment_value(attachment,new cljs.core.Keyword(null,"buffer","buffer",617295198),"buffer");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.discord_gateway.attachment_value(attachment,new cljs.core.Keyword(null,"attachment","attachment",-956025313),"attachment");
}
})();
var name = (function (){var or__5142__auto__ = knoxx.backend.discord_gateway.attachment_value(attachment,new cljs.core.Keyword(null,"name","name",1843675177),"name");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.discord_gateway.attachment_value(attachment,new cljs.core.Keyword(null,"filename","filename",-1428840783),"filename");
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "attachment.bin";
}
}
})();
if(cljs.core.truth_(buffer)){
} else {
throw (new Error("Discord attachment is missing file data"));
}

return ({"attachment": buffer, "name": name});
});
/**
 * Return a logger function (or nil) for a given level keyword.
 * 
 * We avoid the old (.-info? log) style because js/console doesn't expose
 * predicate fields; it only exposes methods like .info/.warn/.error.
 */
knoxx.backend.discord_gateway.log_fn = (function knoxx$backend$discord_gateway$log_fn(log,level){
var candidate = (function (){var G__50779 = level;
var G__50779__$1 = (((G__50779 instanceof cljs.core.Keyword))?G__50779.fqn:null);
switch (G__50779__$1) {
case "info":
return (log["info"]);

break;
case "warn":
return (log["warn"]);

break;
case "error":
return (log["error"]);

break;
case "debug":
return (log["debug"]);

break;
default:
return null;

}
})();
if(cljs.core.fn_QMARK_(candidate)){
return (function() { 
var G__51607__delegate = function (args){
try{return candidate.apply(log,cljs.core.to_array(args));
}catch (e50780){if((e50780 instanceof Error)){
var _ = e50780;
return null;
} else {
throw e50780;

}
}};
var G__51607 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__51611__i = 0, G__51611__a = new Array(arguments.length -  0);
while (G__51611__i < G__51611__a.length) {G__51611__a[G__51611__i] = arguments[G__51611__i + 0]; ++G__51611__i;}
  args = new cljs.core.IndexedSeq(G__51611__a,0,null);
} 
return G__51607__delegate.call(this,args);};
G__51607.cljs$lang$maxFixedArity = 0;
G__51607.cljs$lang$applyTo = (function (arglist__51612){
var args = cljs.core.seq(arglist__51612);
return G__51607__delegate(args);
});
G__51607.cljs$core$IFn$_invoke$arity$variadic = G__51607__delegate;
return G__51607;
})()
;
} else {
return null;
}
});
knoxx.backend.discord_gateway.notify_message_BANG_ = (function knoxx$backend$discord_gateway$notify_message_BANG_(listeners,log,message){
var mapped = knoxx.backend.discord_gateway.map_message(message);
var log_error = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"error","error",-978969032));
return cljs.core.deref(listeners).forEach((function (listener){
try{return (listener.cljs$core$IFn$_invoke$arity$2 ? listener.cljs$core$IFn$_invoke$arity$2(mapped,message) : listener.call(null,mapped,message));
}catch (e50785){if((e50785 instanceof Error)){
var error = e50785;
if(cljs.core.truth_(log_error)){
return log_error("[discord-gateway] listener failed",error);
} else {
return null;
}
} else {
throw e50785;

}
}}));
});
knoxx.backend.discord_gateway.notify_reaction_BANG_ = (function knoxx$backend$discord_gateway$notify_reaction_BANG_(reaction_listeners,log,reaction,user){
var message = reaction.message;
var emoji = reaction.emoji;
var mapped = ({"emoji": (function (){var or__5142__auto__ = emoji.name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "message": (cljs.core.truth_(message)?knoxx.backend.discord_gateway.map_message(message):null), "messageId": (function (){var or__5142__auto__ = (cljs.core.truth_(message)?message.id:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "channelId": (function (){var or__5142__auto__ = (cljs.core.truth_(message)?message.channelId:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "userId": (function (){var or__5142__auto__ = (cljs.core.truth_(user)?user.id:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "userUsername": (function (){var or__5142__auto__ = (cljs.core.truth_(user)?user.username:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})()});
var log_error = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"error","error",-978969032));
return cljs.core.deref(reaction_listeners).forEach((function (listener){
try{return (listener.cljs$core$IFn$_invoke$arity$3 ? listener.cljs$core$IFn$_invoke$arity$3(mapped,reaction,user) : listener.call(null,mapped,reaction,user));
}catch (e50789){if((e50789 instanceof Error)){
var error = e50789;
if(cljs.core.truth_(log_error)){
return log_error("[discord-gateway] reaction listener failed",error);
} else {
return null;
}
} else {
throw e50789;

}
}}));
});
knoxx.backend.discord_gateway.notify_voice_state_BANG_ = (function knoxx$backend$discord_gateway$notify_voice_state_BANG_(voice_state_listeners,log,old_state,new_state){
var old_channel_id = (cljs.core.truth_(old_state)?old_state.channelId:null);
var new_channel_id = (cljs.core.truth_(new_state)?new_state.channelId:null);
var user = (cljs.core.truth_(new_state)?new_state.member:null);
var user_id = (cljs.core.truth_(user)?user.id:null);
var guild_id = (cljs.core.truth_(new_state)?(function (){
new_state.guild;

return new_state.guild.id;
})()
:null);
var action = (cljs.core.truth_((function (){var and__5140__auto__ = (old_channel_id == null);
if(and__5140__auto__){
return new_channel_id;
} else {
return and__5140__auto__;
}
})())?"join":(cljs.core.truth_((function (){var and__5140__auto__ = old_channel_id;
if(cljs.core.truth_(and__5140__auto__)){
return (new_channel_id == null);
} else {
return and__5140__auto__;
}
})())?"leave":(cljs.core.truth_((function (){var and__5140__auto__ = old_channel_id;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = new_channel_id;
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(old_channel_id,new_channel_id);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())?"move":null
)));
var mapped = ({"action": action, "userId": user_id, "username": (function (){var or__5142__auto__ = (cljs.core.truth_(user)?user.user:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (cljs.core.truth_(user)?user.username:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown";
}
}
})(), "guildId": guild_id, "channelId": (function (){var or__5142__auto__ = new_channel_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return old_channel_id;
}
})(), "oldChannelId": old_channel_id, "newChannelId": new_channel_id});
var log_error = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"error","error",-978969032));
if(cljs.core.truth_(action)){
return cljs.core.deref(voice_state_listeners).forEach((function (listener){
try{return (listener.cljs$core$IFn$_invoke$arity$3 ? listener.cljs$core$IFn$_invoke$arity$3(mapped,old_state,new_state) : listener.call(null,mapped,old_state,new_state));
}catch (e50801){if((e50801 instanceof Error)){
var error = e50801;
if(cljs.core.truth_(log_error)){
return log_error("[discord-gateway] voice state listener failed",error);
} else {
return null;
}
} else {
throw e50801;

}
}}));
} else {
return null;
}
});
knoxx.backend.discord_gateway.handle_client_ready = (function knoxx$backend$discord_gateway$handle_client_ready(log_info,ready_client){
if(cljs.core.truth_(log_info)){
var G__50802 = (""+"[discord-gateway] ready as "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (cljs.core.truth_(ready_client.user)?ready_client.user.tag:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())+" in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ready_client.guilds.cache.size)+" guilds");
return (log_info.cljs$core$IFn$_invoke$arity$1 ? log_info.cljs$core$IFn$_invoke$arity$1(G__50802) : log_info.call(null,G__50802));
} else {
return null;
}
});
knoxx.backend.discord_gateway.handle_message_create = (function knoxx$backend$discord_gateway$handle_message_create(notify_message,message){
return (notify_message.cljs$core$IFn$_invoke$arity$1 ? notify_message.cljs$core$IFn$_invoke$arity$1(message) : notify_message.call(null,message));
});
knoxx.backend.discord_gateway.handle_reaction_add = (function knoxx$backend$discord_gateway$handle_reaction_add(log_warn,notify_reaction,reaction,user){
return (cljs.core.truth_(reaction.partial)?reaction.fetch():Promise.resolve(reaction)).then((function (full_reaction){
var message = full_reaction.message;
return (cljs.core.truth_((function (){var and__5140__auto__ = message;
if(cljs.core.truth_(and__5140__auto__)){
return message.partial;
} else {
return and__5140__auto__;
}
})())?message.fetch():Promise.resolve(message)).then((function (_){
return (notify_reaction.cljs$core$IFn$_invoke$arity$2 ? notify_reaction.cljs$core$IFn$_invoke$arity$2(full_reaction,user) : notify_reaction.call(null,full_reaction,user));
}));
})).catch((function (error){
if(cljs.core.truth_(log_warn)){
return (log_warn.cljs$core$IFn$_invoke$arity$2 ? log_warn.cljs$core$IFn$_invoke$arity$2("[discord-gateway] reaction ingest failed",error) : log_warn.call(null,"[discord-gateway] reaction ingest failed",error));
} else {
return null;
}
}));
});
knoxx.backend.discord_gateway.handle_client_error = (function knoxx$backend$discord_gateway$handle_client_error(log_error,error){
if(cljs.core.truth_(log_error)){
return (log_error.cljs$core$IFn$_invoke$arity$2 ? log_error.cljs$core$IFn$_invoke$arity$2("[discord-gateway] client error",error) : log_error.call(null,"[discord-gateway] client error",error));
} else {
return null;
}
});
knoxx.backend.discord_gateway.handle_voice_state_update = (function knoxx$backend$discord_gateway$handle_voice_state_update(notify_voice_state,old_state,new_state){
return (notify_voice_state.cljs$core$IFn$_invoke$arity$2 ? notify_voice_state.cljs$core$IFn$_invoke$arity$2(old_state,new_state) : notify_voice_state.call(null,old_state,new_state));
});
/**
 * Create a new discord.js Client and attach event listeners.
 */
knoxx.backend.discord_gateway.build_discord_client = (function knoxx$backend$discord_gateway$build_discord_client(log,notify_message,notify_reaction,notify_voice_state){
var Client = knoxx.backend.discord_gateway.Client_class();
var GatewayIntentBits = knoxx.backend.discord_gateway.intent_bits();
var Partials = knoxx.backend.discord_gateway.partials_enum();
var Events = knoxx.backend.discord_gateway.events_enum();
var log_info = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"info","info",-317069002));
var log_warn = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"warn","warn",-436710552));
var log_error = knoxx.backend.discord_gateway.log_fn(log,new cljs.core.Keyword(null,"error","error",-978969032));
var next_client = (new Client(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"intents","intents",-1002507708),new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [GatewayIntentBits.Guilds,GatewayIntentBits.GuildMessages,GatewayIntentBits.DirectMessages,GatewayIntentBits.GuildMessageReactions,GatewayIntentBits.DirectMessageReactions,GatewayIntentBits.GuildVoiceStates,GatewayIntentBits.MessageContent], null),new cljs.core.Keyword(null,"partials","partials",-1361599690),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [Partials.Channel,Partials.Message,Partials.Reaction], null)], null))));
next_client.on(Events.ClientReady,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.handle_client_ready,log_info));

next_client.on(Events.MessageCreate,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.handle_message_create,notify_message));

next_client.on(Events.MessageReactionAdd,cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.handle_reaction_add,log_warn,notify_reaction));

next_client.on(Events.Error,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.handle_client_error,log_error));

next_client.on(Events.VoiceStateUpdate,cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.handle_voice_state_update,notify_voice_state));

return next_client;
});
knoxx.backend.discord_gateway.ensure_client_BANG_ = (function knoxx$backend$discord_gateway$ensure_client_BANG_(client_state,ready_promise){
if(cljs.core.not(cljs.core.deref(client_state))){
return Promise.reject((new Error("Discord gateway client is not started")));
} else {
if(cljs.core.truth_(cljs.core.deref(ready_promise))){
return cljs.core.deref(ready_promise).then((function (_){
return cljs.core.deref(client_state);
}));
} else {
return Promise.resolve(cljs.core.deref(client_state));
}
}
});
/**
 * Start the gateway client with a bot token.
 */
knoxx.backend.discord_gateway.gw_start = (function knoxx$backend$discord_gateway$gw_start(client_state,ready_promise,current_token,listeners,log,this_stop,build_client,token){
var next_token = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())).trim();
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(next_token,"")){
return (this_stop.cljs$core$IFn$_invoke$arity$0 ? this_stop.cljs$core$IFn$_invoke$arity$0() : this_stop.call(null)).then((function (_){
return null;
}));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.deref(client_state);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(current_token),next_token);
} else {
return and__5140__auto__;
}
})())){
if(cljs.core.truth_(cljs.core.deref(ready_promise))){
return cljs.core.deref(ready_promise);
} else {
return Promise.resolve(cljs.core.deref(client_state));
}
} else {
return (this_stop.cljs$core$IFn$_invoke$arity$0 ? this_stop.cljs$core$IFn$_invoke$arity$0() : this_stop.call(null)).then((function (_){
cljs.core.reset_BANG_(current_token,next_token);

var new_client = (build_client.cljs$core$IFn$_invoke$arity$0 ? build_client.cljs$core$IFn$_invoke$arity$0() : build_client.call(null));
cljs.core.reset_BANG_(client_state,new_client);

var login_promise = new_client.login(next_token).then((function (___$1){
return new_client;
})).catch((function (error){
if(cljs.core.truth_(log.error_QMARK_)){
log.error("[discord-gateway] login failed",error);
} else {
}

try{new_client.destroy();
}catch (e50951){if((e50951 instanceof Error)){
var __51628__$1 = e50951;
} else {
throw e50951;

}
}
cljs.core.reset_BANG_(client_state,null);

cljs.core.reset_BANG_(ready_promise,null);

cljs.core.reset_BANG_(current_token,null);

return Promise.reject(error);
}));
cljs.core.reset_BANG_(ready_promise,login_promise);

return login_promise;
}));
}
}
});
/**
 * Stop the gateway client.
 */
knoxx.backend.discord_gateway.gw_stop = (function knoxx$backend$discord_gateway$gw_stop(client_state,ready_promise,current_token){
var result = (cljs.core.truth_(cljs.core.deref(client_state))?(function (){try{return cljs.core.deref(client_state).destroy().then((function (_){
return null;
}));
}catch (e50952){if((e50952 instanceof Error)){
var _ = e50952;
return Promise.resolve(null);
} else {
throw e50952;

}
}})():Promise.resolve(null));
cljs.core.reset_BANG_(client_state,null);

cljs.core.reset_BANG_(ready_promise,null);

cljs.core.reset_BANG_(current_token,null);

return result;
});
/**
 * Get gateway status.
 */
knoxx.backend.discord_gateway.gw_status = (function knoxx$backend$discord_gateway$gw_status(client_state){
var c = cljs.core.deref(client_state);
var G__50955 = ({"started": (!((c == null))), "ready": false, "userId": null, "userTag": null, "guildCount": (0)});
if(cljs.core.truth_(c)){
var G__50956 = G__50955;
(G__50956["ready"] = (function (){try{return c.isReady();
}catch (e50957){if((e50957 instanceof Error)){
var _ = e50957;
return false;
} else {
throw e50957;

}
}})());

(G__50956["userId"] = (function (){try{return c.user.id;
}catch (e50958){if((e50958 instanceof Error)){
var _ = e50958;
return null;
} else {
throw e50958;

}
}})());

(G__50956["userTag"] = (function (){try{return c.user.tag;
}catch (e50960){if((e50960 instanceof Error)){
var _ = e50960;
return null;
} else {
throw e50960;

}
}})());

(G__50956["guildCount"] = (function (){try{return c.guilds.cache.size;
}catch (e50961){if((e50961 instanceof Error)){
var _ = e50961;
return (0);
} else {
throw e50961;

}
}})());

return G__50956;
} else {
return G__50955;
}
});
/**
 * List all guilds the bot is in.
 */
knoxx.backend.discord_gateway.gw_list_servers = (function knoxx$backend$discord_gateway$gw_list_servers(ensure_client){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$gw_list_servers_$_iter__50964(s__50965){
return (new cljs.core.LazySeq(null,(function (){
var s__50965__$1 = s__50965;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__50965__$1);
if(temp__5825__auto__){
var s__50965__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__50965__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__50965__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__50967 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__50966 = (0);
while(true){
if((i__50966 < size__5627__auto__)){
var vec__50969 = cljs.core._nth(c__5626__auto__,i__50966);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50969,(0),null);
var guild = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50969,(1),null);
cljs.core.chunk_append(b__50967,({"id": guild.id, "name": guild.name, "memberCount": (function (){var or__5142__auto__ = guild.memberCount;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()}));

var G__51654 = (i__50966 + (1));
i__50966 = G__51654;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__50967),knoxx$backend$discord_gateway$gw_list_servers_$_iter__50964(cljs.core.chunk_rest(s__50965__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__50967),null);
}
} else {
var vec__50972 = cljs.core.first(s__50965__$2);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50972,(0),null);
var guild = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50972,(1),null);
return cljs.core.cons(({"id": guild.id, "name": guild.name, "memberCount": (function (){var or__5142__auto__ = guild.memberCount;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()}),knoxx$backend$discord_gateway$gw_list_servers_$_iter__50964(cljs.core.rest(s__50965__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(active_client.guilds.cache);
})());
}));
});
/**
 * List channels in a guild or all guilds.
 */
knoxx.backend.discord_gateway.gw_list_channels = (function knoxx$backend$discord_gateway$gw_list_channels(ensure_client,log,guild_id){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
var ChannelType = knoxx.backend.discord_gateway.channel_type_enum();
var collect = (function (guild){
return guild.channels.fetch().then((function (fetched){
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$gw_list_channels_$_iter__50975(s__50976){
return (new cljs.core.LazySeq(null,(function (){
var s__50976__$1 = s__50976;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__50976__$1);
if(temp__5825__auto__){
var s__50976__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__50976__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__50976__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__50978 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__50977 = (0);
while(true){
if((i__50977 < size__5627__auto__)){
var vec__50980 = cljs.core._nth(c__5626__auto__,i__50977);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50980,(0),null);
var ch = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50980,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = ch;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = knoxx.backend.discord_gateway.readable_text_channel_QMARK_(ch);
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(ch.type,ChannelType.DM);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
cljs.core.chunk_append(b__50978,({"id": ch.id, "name": (function (){var or__5142__auto__ = ch.name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "guildId": guild.id, "type": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch.type))}));

var G__51667 = (i__50977 + (1));
i__50977 = G__51667;
continue;
} else {
var G__51668 = (i__50977 + (1));
i__50977 = G__51668;
continue;
}
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__50978),knoxx$backend$discord_gateway$gw_list_channels_$_iter__50975(cljs.core.chunk_rest(s__50976__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__50978),null);
}
} else {
var vec__50984 = cljs.core.first(s__50976__$2);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50984,(0),null);
var ch = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50984,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = ch;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = knoxx.backend.discord_gateway.readable_text_channel_QMARK_(ch);
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(ch.type,ChannelType.DM);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return cljs.core.cons(({"id": ch.id, "name": (function (){var or__5142__auto__ = ch.name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "guildId": guild.id, "type": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch.type))}),knoxx$backend$discord_gateway$gw_list_channels_$_iter__50975(cljs.core.rest(s__50976__$2)));
} else {
var G__51676 = cljs.core.rest(s__50976__$2);
s__50976__$1 = G__51676;
continue;
}
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(fetched);
})());
}));
});
if(cljs.core.truth_(guild_id)){
var guild = active_client.guilds.cache.get(guild_id);
if(cljs.core.not(guild)){
return Promise.reject((new Error((""+"Guild not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)))));
} else {
return collect(guild);
}
} else {
var promises = cljs.core.atom.cljs$core$IFn$_invoke$arity$1([]);
var seq__50990_51677 = cljs.core.seq(active_client.guilds.cache);
var chunk__50991_51678 = null;
var count__50992_51679 = (0);
var i__50993_51680 = (0);
while(true){
if((i__50993_51680 < count__50992_51679)){
var vec__51001_51681 = chunk__50991_51678.cljs$core$IIndexed$_nth$arity$2(null,i__50993_51680);
var _id_51682 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51001_51681,(0),null);
var guild_51683 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51001_51681,(1),null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(promises,((function (seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51001_51681,_id_51682,guild_51683,promises,ChannelType,collect){
return (function (ps){
return ps.concat([collect(guild_51683).catch(((function (seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51001_51681,_id_51682,guild_51683,promises,ChannelType,collect){
return (function (err){
if(cljs.core.truth_(log.warn_QMARK_)){
log.warn("[discord-gateway] listChannels guild failed",guild_51683.id,err);
} else {
}

return [];
});})(seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51001_51681,_id_51682,guild_51683,promises,ChannelType,collect))
)]);
});})(seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51001_51681,_id_51682,guild_51683,promises,ChannelType,collect))
);


var G__51693 = seq__50990_51677;
var G__51694 = chunk__50991_51678;
var G__51695 = count__50992_51679;
var G__51696 = (i__50993_51680 + (1));
seq__50990_51677 = G__51693;
chunk__50991_51678 = G__51694;
count__50992_51679 = G__51695;
i__50993_51680 = G__51696;
continue;
} else {
var temp__5825__auto___51697 = cljs.core.seq(seq__50990_51677);
if(temp__5825__auto___51697){
var seq__50990_51698__$1 = temp__5825__auto___51697;
if(cljs.core.chunked_seq_QMARK_(seq__50990_51698__$1)){
var c__5673__auto___51703 = cljs.core.chunk_first(seq__50990_51698__$1);
var G__51704 = cljs.core.chunk_rest(seq__50990_51698__$1);
var G__51705 = c__5673__auto___51703;
var G__51706 = cljs.core.count(c__5673__auto___51703);
var G__51707 = (0);
seq__50990_51677 = G__51704;
chunk__50991_51678 = G__51705;
count__50992_51679 = G__51706;
i__50993_51680 = G__51707;
continue;
} else {
var vec__51004_51708 = cljs.core.first(seq__50990_51698__$1);
var _id_51709 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51004_51708,(0),null);
var guild_51710 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51004_51708,(1),null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(promises,((function (seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51004_51708,_id_51709,guild_51710,seq__50990_51698__$1,temp__5825__auto___51697,promises,ChannelType,collect){
return (function (ps){
return ps.concat([collect(guild_51710).catch(((function (seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51004_51708,_id_51709,guild_51710,seq__50990_51698__$1,temp__5825__auto___51697,promises,ChannelType,collect){
return (function (err){
if(cljs.core.truth_(log.warn_QMARK_)){
log.warn("[discord-gateway] listChannels guild failed",guild_51710.id,err);
} else {
}

return [];
});})(seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51004_51708,_id_51709,guild_51710,seq__50990_51698__$1,temp__5825__auto___51697,promises,ChannelType,collect))
)]);
});})(seq__50990_51677,chunk__50991_51678,count__50992_51679,i__50993_51680,vec__51004_51708,_id_51709,guild_51710,seq__50990_51698__$1,temp__5825__auto___51697,promises,ChannelType,collect))
);


var G__51714 = cljs.core.next(seq__50990_51698__$1);
var G__51715 = null;
var G__51716 = (0);
var G__51717 = (0);
seq__50990_51677 = G__51714;
chunk__50991_51678 = G__51715;
count__50992_51679 = G__51716;
i__50993_51680 = G__51717;
continue;
}
} else {
}
}
break;
}

return Promise.all(cljs.core.deref(promises)).then((function (results){
var flat = cljs.core.atom.cljs$core$IFn$_invoke$arity$1([]);
var seq__51009_51722 = cljs.core.seq(results);
var chunk__51010_51723 = null;
var count__51011_51724 = (0);
var i__51012_51725 = (0);
while(true){
if((i__51012_51725 < count__51011_51724)){
var r_51726 = chunk__51010_51723.cljs$core$IIndexed$_nth$arity$2(null,i__51012_51725);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(flat,((function (seq__51009_51722,chunk__51010_51723,count__51011_51724,i__51012_51725,r_51726,flat,promises,ChannelType,collect){
return (function (f){
return f.concat(r_51726);
});})(seq__51009_51722,chunk__51010_51723,count__51011_51724,i__51012_51725,r_51726,flat,promises,ChannelType,collect))
);


var G__51727 = seq__51009_51722;
var G__51728 = chunk__51010_51723;
var G__51729 = count__51011_51724;
var G__51730 = (i__51012_51725 + (1));
seq__51009_51722 = G__51727;
chunk__51010_51723 = G__51728;
count__51011_51724 = G__51729;
i__51012_51725 = G__51730;
continue;
} else {
var temp__5825__auto___51732 = cljs.core.seq(seq__51009_51722);
if(temp__5825__auto___51732){
var seq__51009_51733__$1 = temp__5825__auto___51732;
if(cljs.core.chunked_seq_QMARK_(seq__51009_51733__$1)){
var c__5673__auto___51734 = cljs.core.chunk_first(seq__51009_51733__$1);
var G__51735 = cljs.core.chunk_rest(seq__51009_51733__$1);
var G__51736 = c__5673__auto___51734;
var G__51737 = cljs.core.count(c__5673__auto___51734);
var G__51738 = (0);
seq__51009_51722 = G__51735;
chunk__51010_51723 = G__51736;
count__51011_51724 = G__51737;
i__51012_51725 = G__51738;
continue;
} else {
var r_51739 = cljs.core.first(seq__51009_51733__$1);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(flat,((function (seq__51009_51722,chunk__51010_51723,count__51011_51724,i__51012_51725,r_51739,seq__51009_51733__$1,temp__5825__auto___51732,flat,promises,ChannelType,collect){
return (function (f){
return f.concat(r_51739);
});})(seq__51009_51722,chunk__51010_51723,count__51011_51724,i__51012_51725,r_51739,seq__51009_51733__$1,temp__5825__auto___51732,flat,promises,ChannelType,collect))
);


var G__51740 = cljs.core.next(seq__51009_51733__$1);
var G__51741 = null;
var G__51742 = (0);
var G__51743 = (0);
seq__51009_51722 = G__51740;
chunk__51010_51723 = G__51741;
count__51011_51724 = G__51742;
i__51012_51725 = G__51743;
continue;
}
} else {
}
}
break;
}

return cljs.core.deref(flat);
}));
}
}));
});
/**
 * Fetch messages from a channel.
 */
knoxx.backend.discord_gateway.gw_fetch_channel_messages = (function knoxx$backend$discord_gateway$gw_fetch_channel_messages(ensure_client,channel_id,opts){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return active_client.channels.fetch(channel_id).then((function (channel){
if(((cljs.core.not(channel)) || (cljs.core.not(knoxx.backend.discord_gateway.readable_text_channel_QMARK_(channel))))){
return Promise.reject((new Error((""+"Channel not found or not text-based: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)))));
} else {
return channel.messages.fetch(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"limit","limit",-1355822363),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})())),new cljs.core.Keyword(null,"before","before",-1633692388),(opts["before"]),new cljs.core.Keyword(null,"after","after",594996914),(opts["after"]),new cljs.core.Keyword(null,"around","around",-265975553),(opts["around"])], null))).then((function (fetched){
return knoxx.backend.discord_gateway.sort_newest_first(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.map_message,(function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$gw_fetch_channel_messages_$_iter__51020(s__51021){
return (new cljs.core.LazySeq(null,(function (){
var s__51021__$1 = s__51021;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__51021__$1);
if(temp__5825__auto__){
var s__51021__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__51021__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__51021__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__51023 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__51022 = (0);
while(true){
if((i__51022 < size__5627__auto__)){
var vec__51026 = cljs.core._nth(c__5626__auto__,i__51022);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51026,(0),null);
var msg = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51026,(1),null);
cljs.core.chunk_append(b__51023,msg);

var G__51760 = (i__51022 + (1));
i__51022 = G__51760;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__51023),knoxx$backend$discord_gateway$gw_fetch_channel_messages_$_iter__51020(cljs.core.chunk_rest(s__51021__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__51023),null);
}
} else {
var vec__51031 = cljs.core.first(s__51021__$2);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51031,(0),null);
var msg = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51031,(1),null);
return cljs.core.cons(msg,knoxx$backend$discord_gateway$gw_fetch_channel_messages_$_iter__51020(cljs.core.rest(s__51021__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(fetched);
})()));
}));
}
}));
}));
});
/**
 * Fetch DM messages with a user.
 */
knoxx.backend.discord_gateway.gw_fetch_dm_messages = (function knoxx$backend$discord_gateway$gw_fetch_dm_messages(ensure_client,user_id,opts){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return active_client.users.fetch(user_id).then((function (user){
return user.createDM().then((function (dm){
return dm.messages.fetch(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})())),new cljs.core.Keyword(null,"before","before",-1633692388),(opts["before"])], null))).then((function (fetched){
return ({"dmChannelId": dm.id, "messages": knoxx.backend.discord_gateway.sort_newest_first(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.map_message,(function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$gw_fetch_dm_messages_$_iter__51037(s__51038){
return (new cljs.core.LazySeq(null,(function (){
var s__51038__$1 = s__51038;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__51038__$1);
if(temp__5825__auto__){
var s__51038__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__51038__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__51038__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__51040 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__51039 = (0);
while(true){
if((i__51039 < size__5627__auto__)){
var vec__51043 = cljs.core._nth(c__5626__auto__,i__51039);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51043,(0),null);
var msg = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51043,(1),null);
cljs.core.chunk_append(b__51040,msg);

var G__51766 = (i__51039 + (1));
i__51039 = G__51766;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__51040),knoxx$backend$discord_gateway$gw_fetch_dm_messages_$_iter__51037(cljs.core.chunk_rest(s__51038__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__51040),null);
}
} else {
var vec__51046 = cljs.core.first(s__51038__$2);
var _id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51046,(0),null);
var msg = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51046,(1),null);
return cljs.core.cons(msg,knoxx$backend$discord_gateway$gw_fetch_dm_messages_$_iter__51037(cljs.core.rest(s__51038__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(fetched);
})()))});
}));
}));
}));
}));
});
/**
 * Create a filter function for message search.
 */
knoxx.backend.discord_gateway.search_filter_fn = (function knoxx$backend$discord_gateway$search_filter_fn(opts){
var needle = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (opts["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())).toLowerCase();
var target_user_id = (opts["userId"]);
return (function (message){
var content_ok = (function (){var or__5142__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(needle,"");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return (function (){var or__5142__auto____$1 = (message["content"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
})().toLowerCase().includes(needle);
}
})();
var author_ok = ((cljs.core.not(target_user_id)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((message["authorId"]),target_user_id)));
var and__5140__auto__ = content_ok;
if(cljs.core.truth_(and__5140__auto__)){
return author_ok;
} else {
return and__5140__auto__;
}
});
});
/**
 * Search messages in a channel or DM.
 */
knoxx.backend.discord_gateway.gw_search_messages = (function knoxx$backend$discord_gateway$gw_search_messages(this_fn,scope,opts){
var normalized_scope = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = scope;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "channel";
}
})())).toLowerCase();
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized_scope,"dm")){
return this_fn.fetchDmMessages((opts["userId"]),cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(100),new cljs.core.Keyword(null,"before","before",-1633692388),(opts["before"])], null))).then((function (result){
var filtered = (result["messages"]).filter(knoxx.backend.discord_gateway.search_filter_fn(opts));
return ({"dmChannelId": (result["dmChannelId"]), "messages": filtered.slice((0),(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})()), "count": cljs.core.min.cljs$core$IFn$_invoke$arity$2(filtered.length,(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})()), "source": "gateway-cache"});
}));
} else {
return this_fn.fetchChannelMessages((opts["channelId"]),cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"limit","limit",-1355822363),(100),new cljs.core.Keyword(null,"before","before",-1633692388),(opts["before"]),new cljs.core.Keyword(null,"after","after",594996914),(opts["after"])], null))).then((function (messages){
var filtered = messages.filter(knoxx.backend.discord_gateway.search_filter_fn(opts));
return ({"channelId": (opts["channelId"]), "messages": filtered.slice((0),(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})()), "count": cljs.core.min.cljs$core$IFn$_invoke$arity$2(filtered.length,(function (){var or__5142__auto__ = (opts["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})()), "source": "gateway-cache"});
}));
}
});
/**
 * Send a message to a channel, splitting into chunks if needed.
 */
knoxx.backend.discord_gateway.gw_send_message = (function knoxx$backend$discord_gateway$gw_send_message(ensure_client,channel_id,text,reply_to,attachments){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return active_client.channels.fetch(channel_id).then((function (channel){
if(((cljs.core.not(channel)) || (cljs.core.not(knoxx.backend.discord_gateway.readable_text_channel_QMARK_(channel))))){
return Promise.reject((new Error((""+"Channel not found or not text-based: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)))));
} else {
var base_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var chunks = knoxx.backend.discord_gateway.split_message(((((clojure.string.blank_QMARK_(base_text)) && (cljs.core.seq(attachments))))?"[attachment]":base_text));
return chunks.reduce((function (promise,chunk,index){
return (function (){var or__5142__auto__ = promise;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return Promise.resolve(null);
}
})().then((function (_){
var payload = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),chunk], null));
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(index,(0));
if(and__5140__auto__){
return reply_to;
} else {
return and__5140__auto__;
}
})())){
(payload["reply"] = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"messageReference","messageReference",-2136149455),reply_to], null)));
} else {
}

if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(index,(0))) && (cljs.core.seq(attachments)))){
(payload["files"] = cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.discord_gateway.discord_file_payload,attachments)));
} else {
}

return channel.send(payload);
}));
}),null).then((function (_){
return ({"channelId": channel_id, "messageId": "", "sent": true, "timestamp": (new Date()).toISOString(), "chunkCount": chunks.length, "attachmentCount": cljs.core.count(attachments)});
}));
}
}));
}));
});
/**
 * Join a voice channel. Returns a VoiceConnection.
 */
knoxx.backend.discord_gateway.gw_join_voice = (function knoxx$backend$discord_gateway$gw_join_voice(ensure_client,channel_id){
console.log("[voice:gw] joining channel:",channel_id);

return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return active_client.channels.fetch(channel_id).then((function (channel){
if(cljs.core.not(channel)){
console.error("[voice:gw] channel not found:",channel_id);

return Promise.reject((new Error((""+"Channel not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)))));
} else {
var guild_id = channel.guildId;
console.log("[voice:gw] channel found:",channel_id,"guild:",guild_id,"selfDeaf:false");

var conn = shadow.esm.esm_import$$discordjs$voice.joinVoiceChannel(({"channelId": channel_id, "guildId": guild_id, "adapterCreator": channel.guild.voiceAdapterCreator, "selfDeaf": false, "selfMute": false}));
(conn["__guildId"] = guild_id);

console.log("[voice:gw] joinVoiceChannel returned, waiting for ready state\u2026");

return shadow.esm.esm_import$$discordjs$voice.entersState(conn,shadow.esm.esm_import$$discordjs$voice.VoiceConnectionStatus.Ready,(15000)).then((function (_){
console.log("[voice:gw] voice connection ready for guild:",(function (){var or__5142__auto__ = conn.__guildId;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__51060 = conn;
var G__51060__$1 = (((G__51060 == null))?null:G__51060.joinConfig);
if((G__51060__$1 == null)){
return null;
} else {
return G__51060__$1.guildId;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return conn.guildId;
}
}
})());

return conn;
})).catch((function (err){
console.error("[voice:gw] voice connection failed to ready:",err.message);

return Promise.reject(err);
}));
}
}));
}));
});
/**
 * Leave a voice channel for a guild.
 */
knoxx.backend.discord_gateway.gw_leave_voice = (function knoxx$backend$discord_gateway$gw_leave_voice(connections,guild_id){
var temp__5825__auto___51796 = connections.get(guild_id);
if(cljs.core.truth_(temp__5825__auto___51796)){
var conn_51797 = temp__5825__auto___51796;
conn_51797.destroy();

connections.delete(guild_id);
} else {
}

return Promise.resolve(true);
});
/**
 * Play an audio buffer (PCM s16le 48kHz stereo or any ffmpeg-decodable) in the voice connection.
 */
knoxx.backend.discord_gateway.gw_play_audio = (function knoxx$backend$discord_gateway$gw_play_audio(connections,guild_id,audio_buffer){
var conn = connections.get(guild_id);
if(cljs.core.not(conn)){
return Promise.reject((new Error((""+"No voice connection for guild: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)))));
} else {
var player = (function (){var or__5142__auto__ = conn.__audioPlayer;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var p = shadow.esm.esm_import$$discordjs$voice.createAudioPlayer();
(conn["__audioPlayer"] = p);

conn.subscribe(p);

return p;
}
})();
var stream = (new shadow.esm.esm_import$node_stream.Readable(({"read": (function (){
return null;
})})));
var _ = (function (){
stream.push(audio_buffer);

return stream.push(null);
})()
;
var resource = shadow.esm.esm_import$$discordjs$voice.createAudioResource(stream,({"inputType": (shadow.esm.esm_import$$discordjs$voice["StreamType"]).Arbitrary}));
player.play(resource);

return Promise.resolve(true);
}
});
/**
 * Subscribe to audio from a specific user. Returns an unsubscribe function.
 */
knoxx.backend.discord_gateway.gw_subscribe_voice = (function knoxx$backend$discord_gateway$gw_subscribe_voice(connections,guild_id,user_id,callback){
var conn = connections.get(guild_id);
if(cljs.core.not(conn)){
return Promise.reject((new Error((""+"No voice connection for guild: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)))));
} else {
var receiver = conn.receiver;
var opus_stream = receiver.subscribe(user_id,({"mode": "opus"}));
opus_stream.on("data",(function (chunk){
return (callback.cljs$core$IFn$_invoke$arity$2 ? callback.cljs$core$IFn$_invoke$arity$2(user_id,chunk) : callback.call(null,user_id,chunk));
}));

return Promise.resolve((function (){
return opus_stream.destroy();
}));
}
});
/**
 * List members in a voice channel.
 */
knoxx.backend.discord_gateway.gw_list_voice_members = (function knoxx$backend$discord_gateway$gw_list_voice_members(ensure_client,guild_id,channel_id){
return (ensure_client.cljs$core$IFn$_invoke$arity$0 ? ensure_client.cljs$core$IFn$_invoke$arity$0() : ensure_client.call(null)).then((function (active_client){
return active_client.guilds.fetch(guild_id).then((function (guild){
if(cljs.core.not(guild)){
return Promise.reject((new Error((""+"Guild not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)))));
} else {
return guild.channels.fetch(channel_id).then((function (channel){
if(cljs.core.not(channel)){
return Promise.reject((new Error((""+"Channel not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)))));
} else {
var members = channel.members;
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$discord_gateway$gw_list_voice_members_$_iter__51075(s__51076){
return (new cljs.core.LazySeq(null,(function (){
var s__51076__$1 = s__51076;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__51076__$1);
if(temp__5825__auto__){
var s__51076__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__51076__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__51076__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__51078 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__51077 = (0);
while(true){
if((i__51077 < size__5627__auto__)){
var vec__51079 = cljs.core._nth(c__5626__auto__,i__51077);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51079,(0),null);
var member = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51079,(1),null);
cljs.core.chunk_append(b__51078,(function (){var user = member.user;
return ({"userId": user.id, "username": user.username, "displayName": (function (){var or__5142__auto__ = member.displayName;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return user.username;
}
})(), "isBot": cljs.core.boolean$(user.bot), "isMuted": cljs.core.boolean$(member.mute), "isDeaf": cljs.core.boolean$(member.deaf), "isSpeaking": false});
})());

var G__51818 = (i__51077 + (1));
i__51077 = G__51818;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__51078),knoxx$backend$discord_gateway$gw_list_voice_members_$_iter__51075(cljs.core.chunk_rest(s__51076__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__51078),null);
}
} else {
var vec__51083 = cljs.core.first(s__51076__$2);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51083,(0),null);
var member = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51083,(1),null);
return cljs.core.cons((function (){var user = member.user;
return ({"userId": user.id, "username": user.username, "displayName": (function (){var or__5142__auto__ = member.displayName;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return user.username;
}
})(), "isBot": cljs.core.boolean$(user.bot), "isMuted": cljs.core.boolean$(member.mute), "isDeaf": cljs.core.boolean$(member.deaf), "isSpeaking": false});
})(),knoxx$backend$discord_gateway$gw_list_voice_members_$_iter__51075(cljs.core.rest(s__51076__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(members);
})());
}
}));
}
}));
}));
});
/**
 * Start listening for all speaking users in a voice channel.
 * Calls (on-start user-id) when a user starts speaking.
 * Calls (on-audio user-id wav-buffer) when a user stops speaking with accumulated audio.
 * 
 * NOTE: We decode Opus -> PCM -> WAV here so downstream STT can simply POST bytes
 * to /transcribe (the STT service uses ffmpeg).
 * 
 * Returns a stop function.
 */
knoxx.backend.discord_gateway.gw_start_voice_listener = (function knoxx$backend$discord_gateway$gw_start_voice_listener(connections,guild_id,on_start,on_audio){
console.log("[voice:listener] starting for guild:",guild_id,"connections:",connections.size);

var conn = connections.get(guild_id);
if(cljs.core.not(conn)){
console.error("[voice:listener] no connection for guild:",guild_id);

return Promise.reject((new Error((""+"No voice connection for guild: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)))));
} else {
var receiver = conn.receiver;
var speaking_map = receiver.speaking;
var pcm_buffers = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var streams = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var decoders = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var active_users = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentHashSet.EMPTY);
var silence_timers = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var create_decoder = (function (){
var OpusDecoder = (function (){var G__51089 = shadow.esm.esm_import$prism_media;
var G__51089__$1 = (((G__51089 == null))?null:(G__51089["opus"]));
if((G__51089__$1 == null)){
return null;
} else {
return (G__51089__$1["Decoder"]);
}
})();
if(cljs.core.fn_QMARK_(OpusDecoder)){
} else {
throw (new Error("prism-media Opus decoder unavailable"));
}

return (new OpusDecoder(({"rate": (48000), "channels": (2), "frameSize": (960)})));
});
var silence_debounce_ms = knoxx.backend.discord_gateway.voice_listener_silence_debounce_ms;
var flush_audio_BANG_ = (function (uid){
var temp__5825__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(pcm_buffers),uid);
if(cljs.core.truth_(temp__5825__auto__)){
var buf = temp__5825__auto__;
var chunks = Array.from(buf);
var pcm = Buffer.concat(chunks);
var duration_s = (((pcm.length / knoxx.backend.discord_gateway.voice_listener_sample_rate) / knoxx.backend.discord_gateway.voice_listener_bytes_per_sample) / knoxx.backend.discord_gateway.voice_listener_channels);
var wav = knoxx.backend.discord_gateway.pcm16le__GT_wav_buffer(pcm,knoxx.backend.discord_gateway.voice_listener_sample_rate,knoxx.backend.discord_gateway.voice_listener_channels);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(pcm_buffers,cljs.core.dissoc,uid);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(silence_timers,cljs.core.dissoc,uid);

if((duration_s < knoxx.backend.discord_gateway.voice_listener_min_duration_s)){
return console.log("[voice:listener] skipping very short audio for",uid,"duration:",duration_s,"s");
} else {
console.log("[voice:listener] calling on-audio for",uid,"wav bytes:",wav.length,"duration:",duration_s,"s");

return (on_audio.cljs$core$IFn$_invoke$arity$2 ? on_audio.cljs$core$IFn$_invoke$arity$2(uid,wav) : on_audio.call(null,uid,wav));
}
} else {
return null;
}
});
var on_start_speaking = (function (user_id){
var uid = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id));
var temp__5825__auto___51852 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(silence_timers),uid);
if(cljs.core.truth_(temp__5825__auto___51852)){
var t_51853 = temp__5825__auto___51852;
clearTimeout(t_51853);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(silence_timers,cljs.core.dissoc,uid);
} else {
}

if(cljs.core.contains_QMARK_(cljs.core.deref(active_users),uid)){
return null;
} else {
console.log("[voice:listener] >>> SPEAKING START:",uid);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(active_users,cljs.core.conj,uid);

if(cljs.core.truth_(on_start)){
(on_start.cljs$core$IFn$_invoke$arity$1 ? on_start.cljs$core$IFn$_invoke$arity$1(uid) : on_start.call(null,uid));
} else {
}

if(cljs.core.truth_(cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(pcm_buffers),uid))){
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(pcm_buffers,cljs.core.assoc,uid,[]);
}

var audio_stream = receiver.subscribe(uid);
var decoder = create_decoder();
audio_stream.pipe(decoder);

decoder.on("data",(function (pcm_chunk){
var temp__5825__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(pcm_buffers),uid);
if(cljs.core.truth_(temp__5825__auto__)){
var buf = temp__5825__auto__;
return buf.push(pcm_chunk);
} else {
return null;
}
}));

decoder.on("error",(function (err){
return console.error("[voice:listener] decoder error for",uid,":",err.message);
}));

audio_stream.on("error",(function (err){
return console.error("[voice:listener] audio stream error for",uid,":",err.message);
}));

audio_stream.on("end",(function (){
return console.log("[voice:listener] audio stream ended for",uid);
}));

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(streams,cljs.core.assoc,uid,audio_stream);

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(decoders,cljs.core.assoc,uid,decoder);
}
});
var on_end_speaking = (function (user_id){
var uid = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id));
console.log("[voice:listener] >>> SPEAKING END:",uid);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(active_users,cljs.core.disj,uid);

var temp__5825__auto___51870 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(streams),uid);
if(cljs.core.truth_(temp__5825__auto___51870)){
var audio_stream_51871 = temp__5825__auto___51870;
try{audio_stream_51871.destroy();
}catch (e51104){if((e51104 instanceof Error)){
var __51872 = e51104;
} else {
throw e51104;

}
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(streams,cljs.core.dissoc,uid);
} else {
}

var temp__5825__auto___51873 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(decoders),uid);
if(cljs.core.truth_(temp__5825__auto___51873)){
var decoder_51874 = temp__5825__auto___51873;
try{decoder_51874.destroy();
}catch (e51105){if((e51105 instanceof Error)){
var __51875 = e51105;
} else {
throw e51105;

}
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(decoders,cljs.core.dissoc,uid);
} else {
}

var t = setTimeout((function (){
return flush_audio_BANG_(uid);
}),silence_debounce_ms);
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(silence_timers,cljs.core.assoc,uid,t);
});
console.log("[voice:listener] attaching listeners");

speaking_map.on("start",on_start_speaking);

speaking_map.on("end",on_end_speaking);

return Promise.resolve((function (){
console.log("[voice:listener] stopping for guild:",guild_id);

speaking_map.removeListener("start",on_start_speaking);

speaking_map.removeListener("end",on_end_speaking);

var seq__51110_51876 = cljs.core.seq(cljs.core.deref(streams));
var chunk__51111_51877 = null;
var count__51112_51878 = (0);
var i__51113_51879 = (0);
while(true){
if((i__51113_51879 < count__51112_51878)){
var vec__51139_51884 = chunk__51111_51877.cljs$core$IIndexed$_nth$arity$2(null,i__51113_51879);
var __51885 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51139_51884,(0),null);
var s_51886 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51139_51884,(1),null);
try{s_51886.destroy();
}catch (e51142){if((e51142 instanceof Error)){
var __51888__$1 = e51142;
} else {
throw e51142;

}
}

var G__51890 = seq__51110_51876;
var G__51891 = chunk__51111_51877;
var G__51892 = count__51112_51878;
var G__51893 = (i__51113_51879 + (1));
seq__51110_51876 = G__51890;
chunk__51111_51877 = G__51891;
count__51112_51878 = G__51892;
i__51113_51879 = G__51893;
continue;
} else {
var temp__5825__auto___51894 = cljs.core.seq(seq__51110_51876);
if(temp__5825__auto___51894){
var seq__51110_51895__$1 = temp__5825__auto___51894;
if(cljs.core.chunked_seq_QMARK_(seq__51110_51895__$1)){
var c__5673__auto___51896 = cljs.core.chunk_first(seq__51110_51895__$1);
var G__51897 = cljs.core.chunk_rest(seq__51110_51895__$1);
var G__51898 = c__5673__auto___51896;
var G__51899 = cljs.core.count(c__5673__auto___51896);
var G__51900 = (0);
seq__51110_51876 = G__51897;
chunk__51111_51877 = G__51898;
count__51112_51878 = G__51899;
i__51113_51879 = G__51900;
continue;
} else {
var vec__51145_51901 = cljs.core.first(seq__51110_51895__$1);
var __51902 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51145_51901,(0),null);
var s_51903 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51145_51901,(1),null);
try{s_51903.destroy();
}catch (e51148){if((e51148 instanceof Error)){
var __51904__$1 = e51148;
} else {
throw e51148;

}
}

var G__51905 = cljs.core.next(seq__51110_51895__$1);
var G__51906 = null;
var G__51907 = (0);
var G__51908 = (0);
seq__51110_51876 = G__51905;
chunk__51111_51877 = G__51906;
count__51112_51878 = G__51907;
i__51113_51879 = G__51908;
continue;
}
} else {
}
}
break;
}

var seq__51149_51909 = cljs.core.seq(cljs.core.deref(decoders));
var chunk__51150_51910 = null;
var count__51151_51911 = (0);
var i__51152_51912 = (0);
while(true){
if((i__51152_51912 < count__51151_51911)){
var vec__51164_51913 = chunk__51150_51910.cljs$core$IIndexed$_nth$arity$2(null,i__51152_51912);
var __51914 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51164_51913,(0),null);
var d_51915 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51164_51913,(1),null);
try{d_51915.destroy();
}catch (e51167){if((e51167 instanceof Error)){
var __51916__$1 = e51167;
} else {
throw e51167;

}
}

var G__51917 = seq__51149_51909;
var G__51918 = chunk__51150_51910;
var G__51919 = count__51151_51911;
var G__51920 = (i__51152_51912 + (1));
seq__51149_51909 = G__51917;
chunk__51150_51910 = G__51918;
count__51151_51911 = G__51919;
i__51152_51912 = G__51920;
continue;
} else {
var temp__5825__auto___51921 = cljs.core.seq(seq__51149_51909);
if(temp__5825__auto___51921){
var seq__51149_51922__$1 = temp__5825__auto___51921;
if(cljs.core.chunked_seq_QMARK_(seq__51149_51922__$1)){
var c__5673__auto___51923 = cljs.core.chunk_first(seq__51149_51922__$1);
var G__51924 = cljs.core.chunk_rest(seq__51149_51922__$1);
var G__51925 = c__5673__auto___51923;
var G__51926 = cljs.core.count(c__5673__auto___51923);
var G__51927 = (0);
seq__51149_51909 = G__51924;
chunk__51150_51910 = G__51925;
count__51151_51911 = G__51926;
i__51152_51912 = G__51927;
continue;
} else {
var vec__51170_51928 = cljs.core.first(seq__51149_51922__$1);
var __51929 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51170_51928,(0),null);
var d_51930 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51170_51928,(1),null);
try{d_51930.destroy();
}catch (e51173){if((e51173 instanceof Error)){
var __51931__$1 = e51173;
} else {
throw e51173;

}
}

var G__51932 = cljs.core.next(seq__51149_51922__$1);
var G__51933 = null;
var G__51934 = (0);
var G__51935 = (0);
seq__51149_51909 = G__51932;
chunk__51150_51910 = G__51933;
count__51151_51911 = G__51934;
i__51152_51912 = G__51935;
continue;
}
} else {
}
}
break;
}

var seq__51174_51936 = cljs.core.seq(cljs.core.deref(silence_timers));
var chunk__51175_51937 = null;
var count__51176_51938 = (0);
var i__51177_51939 = (0);
while(true){
if((i__51177_51939 < count__51176_51938)){
var vec__51190_51940 = chunk__51175_51937.cljs$core$IIndexed$_nth$arity$2(null,i__51177_51939);
var uid_51941 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51190_51940,(0),null);
var t_51942 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51190_51940,(1),null);
clearTimeout(t_51942);

flush_audio_BANG_(uid_51941);


var G__51943 = seq__51174_51936;
var G__51944 = chunk__51175_51937;
var G__51945 = count__51176_51938;
var G__51946 = (i__51177_51939 + (1));
seq__51174_51936 = G__51943;
chunk__51175_51937 = G__51944;
count__51176_51938 = G__51945;
i__51177_51939 = G__51946;
continue;
} else {
var temp__5825__auto___51947 = cljs.core.seq(seq__51174_51936);
if(temp__5825__auto___51947){
var seq__51174_51948__$1 = temp__5825__auto___51947;
if(cljs.core.chunked_seq_QMARK_(seq__51174_51948__$1)){
var c__5673__auto___51949 = cljs.core.chunk_first(seq__51174_51948__$1);
var G__51950 = cljs.core.chunk_rest(seq__51174_51948__$1);
var G__51951 = c__5673__auto___51949;
var G__51952 = cljs.core.count(c__5673__auto___51949);
var G__51953 = (0);
seq__51174_51936 = G__51950;
chunk__51175_51937 = G__51951;
count__51176_51938 = G__51952;
i__51177_51939 = G__51953;
continue;
} else {
var vec__51196_51954 = cljs.core.first(seq__51174_51948__$1);
var uid_51955 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51196_51954,(0),null);
var t_51956 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51196_51954,(1),null);
clearTimeout(t_51956);

flush_audio_BANG_(uid_51955);


var G__51957 = cljs.core.next(seq__51174_51948__$1);
var G__51958 = null;
var G__51959 = (0);
var G__51960 = (0);
seq__51174_51936 = G__51957;
chunk__51175_51937 = G__51958;
count__51176_51938 = G__51959;
i__51177_51939 = G__51960;
continue;
}
} else {
}
}
break;
}

cljs.core.reset_BANG_(pcm_buffers,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(streams,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(decoders,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(active_users,cljs.core.PersistentHashSet.EMPTY);

return cljs.core.reset_BANG_(silence_timers,cljs.core.PersistentArrayMap.EMPTY);
}));
}
});
/**
 * Create a Discord gateway manager. Returns a JS object with async methods.
 * 
 * Options (CLJS map or JS object):
 *   - :log / "log": logger object (default: console)
 *   - :set-default? / "setDefault": when false, do not replace the legacy
 *     singleton manager.
 * 
 * Methods: start, stop, restart, onMessage, status, listServers,
 *          listChannels, fetchChannelMessages, fetchDmMessages,
 *          searchMessages, sendMessage
 */
knoxx.backend.discord_gateway.createDiscordGatewayManager = (function knoxx$backend$discord_gateway$createDiscordGatewayManager(opts){
var log = (function (){var or__5142__auto__ = ((cljs.core.map_QMARK_(opts))?new cljs.core.Keyword(null,"log","log",-1595516004).cljs$core$IFn$_invoke$arity$1(opts):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.object_QMARK_(opts))?(opts["log"]):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return console;
}
}
})();
var set_default_QMARK_ = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,(function (){var or__5142__auto__ = ((cljs.core.map_QMARK_(opts))?new cljs.core.Keyword(null,"set-default?","set-default?",-1700805320).cljs$core$IFn$_invoke$arity$1(opts):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.object_QMARK_(opts))?(opts["setDefault"]):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return true;
}
}
})());
var client_state = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var ready_promise = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var current_token = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var listeners = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((new Set()));
var reaction_listeners = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((new Set()));
var voice_state_listeners = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((new Set()));
var notify_message = cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.notify_message_BANG_,listeners,log);
var notify_reaction = cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.notify_reaction_BANG_,reaction_listeners,log);
var notify_voice_state = cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.notify_voice_state_BANG_,voice_state_listeners,log);
var build_client = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.discord_gateway.build_discord_client,log,notify_message,notify_reaction,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([notify_voice_state], 0));
var ensure_client = cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.ensure_client_BANG_,client_state,ready_promise);
var this_stop = (function (){
return knoxx.backend.discord_gateway.gw_stop(client_state,ready_promise,current_token);
});
var voice_connections = (new Map());
var this_obj = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
var this_fn = (function knoxx$backend$discord_gateway$createDiscordGatewayManager_$_this_fn(){
return cljs.core.deref(this_obj);
});
cljs.core.reset_BANG_(this_obj,({"onReaction": (function (listener){
cljs.core.deref(reaction_listeners).add(listener);

return (function (){
return cljs.core.deref(reaction_listeners).delete(listener);
});
}), "onVoiceStateUpdate": (function (listener){
cljs.core.deref(voice_state_listeners).add(listener);

return (function (){
return cljs.core.deref(voice_state_listeners).delete(listener);
});
}), "restart": (function (token){
return this_stop().then((function (_){
return this_fn().start(token);
}));
}), "playAudio": (function (guild_id,audio_buffer){
return knoxx.backend.discord_gateway.gw_play_audio(voice_connections,guild_id,audio_buffer);
}), "fetchDmMessages": (function (user_id,opts__$1){
return knoxx.backend.discord_gateway.gw_fetch_dm_messages(ensure_client,user_id,opts__$1);
}), "listChannels": (function (guild_id){
return knoxx.backend.discord_gateway.gw_list_channels(ensure_client,log,guild_id);
}), "start": (function (token){
return knoxx.backend.discord_gateway.gw_start(client_state,ready_promise,current_token,listeners,log,this_stop,build_client,token);
}), "getVoiceConnection": (function (guild_id){
if(cljs.core.truth_(guild_id)){
return voice_connections.get(guild_id);
} else {
if((voice_connections.size > (0))){
var entries = voice_connections.entries();
return entries.next().value;
} else {
return null;
}
}
}), "searchMessages": (function (scope,opts__$1){
return knoxx.backend.discord_gateway.gw_search_messages(this_fn(),scope,opts__$1);
}), "sendMessage": (function (channel_id,text,reply_to,attachments){
return knoxx.backend.discord_gateway.gw_send_message(ensure_client,channel_id,text,reply_to,attachments);
}), "startVoiceListener": (function (guild_id,on_start,on_audio){
return knoxx.backend.discord_gateway.gw_start_voice_listener(voice_connections,guild_id,on_start,on_audio);
}), "joinVoice": (function (channel_id){
return knoxx.backend.discord_gateway.gw_join_voice(ensure_client,channel_id).then((function (conn){
var guild_id = (function (){var or__5142__auto__ = conn.__guildId;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return conn.guildId;
}
})();
voice_connections.set(guild_id,conn);

return ({"guildId": guild_id, "channelId": channel_id, "joined": true});
}));
}), "stop": (function (){
voice_connections.forEach((function (conn,_key){
try{return conn.destroy();
}catch (e51241){if((e51241 instanceof Error)){
var _ = e51241;
return null;
} else {
throw e51241;

}
}}));

voice_connections.clear();

return this_stop();
}), "leaveVoice": (function (guild_id){
knoxx.backend.discord_gateway.gw_leave_voice(voice_connections,guild_id);

return ({"guildId": guild_id, "left": true});
}), "listVoiceMembers": (function (guild_id,channel_id){
return knoxx.backend.discord_gateway.gw_list_voice_members(ensure_client,guild_id,channel_id);
}), "status": (function (){
return knoxx.backend.discord_gateway.gw_status(client_state);
}), "subscribeVoice": (function (guild_id,user_id,callback){
return knoxx.backend.discord_gateway.gw_subscribe_voice(voice_connections,guild_id,user_id,callback);
}), "listServers": (function (){
return knoxx.backend.discord_gateway.gw_list_servers(ensure_client);
}), "onMessage": (function (listener){
cljs.core.deref(listeners).add(listener);

return (function (){
return cljs.core.deref(listeners).delete(listener);
});
}), "fetchChannelMessages": (function (channel_id,opts__$1){
return knoxx.backend.discord_gateway.gw_fetch_channel_messages(ensure_client,channel_id,opts__$1);
})}));

if(set_default_QMARK_){
var G__51248_51983 = cljs.core.deref(this_obj);
(knoxx.backend.discord_gateway.set_manager_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.discord_gateway.set_manager_BANG_.cljs$core$IFn$_invoke$arity$1(G__51248_51983) : knoxx.backend.discord_gateway.set_manager_BANG_.call(null,G__51248_51983));
} else {
}

return cljs.core.deref(this_obj);
});
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.discord_gateway !== 'undefined') && (typeof knoxx.backend.discord_gateway.manager_STAR_ !== 'undefined')){
} else {
knoxx.backend.discord_gateway.manager_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.discord_gateway !== 'undefined') && (typeof knoxx.backend.discord_gateway.actor_managers_STAR_ !== 'undefined')){
} else {
knoxx.backend.discord_gateway.actor_managers_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
/**
 * Store the gateway manager instance for CLJS API access.
 */
knoxx.backend.discord_gateway.set_manager_BANG_ = (function knoxx$backend$discord_gateway$set_manager_BANG_(m){
return cljs.core.reset_BANG_(knoxx.backend.discord_gateway.manager_STAR_,m);
});
/**
 * Returns the legacy/default gateway manager instance, or an actor-owned manager.
 */
knoxx.backend.discord_gateway.gateway_manager = (function knoxx$backend$discord_gateway$gateway_manager(var_args){
var G__51259 = arguments.length;
switch (G__51259) {
case 0:
return knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
}));

(knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$1 = (function (actor_id){
var temp__5823__auto__ = (function (){var G__51265 = actor_id;
var G__51265__$1 = (((G__51265 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51265)));
var G__51265__$2 = (((G__51265__$1 == null))?null:clojure.string.trim(G__51265__$1));
if((G__51265__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51265__$2);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var id = temp__5823__auto__;
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.discord_gateway.actor_managers_STAR_),id);
} else {
return cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
}
}));

(knoxx.backend.discord_gateway.gateway_manager.cljs$lang$maxFixedArity = 1);

/**
 * Returns a map of actor-id to actor-owned Discord gateway managers.
 */
knoxx.backend.discord_gateway.gateway_managers = (function knoxx$backend$discord_gateway$gateway_managers(){
return cljs.core.deref(knoxx.backend.discord_gateway.actor_managers_STAR_);
});
knoxx.backend.discord_gateway.credential_value = (function knoxx$backend$discord_gateway$credential_value(credential,k){
var or__5142__auto__ = ((cljs.core.map_QMARK_(credential))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(credential,k):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.map_QMARK_(credential))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(credential,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k)):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = ((cljs.core.map_QMARK_(credential))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(credential,cljs.core.name(k)):null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
if(cljs.core.object_QMARK_(credential)){
return (credential[cljs.core.name(k)]);
} else {
return null;
}
}
}
}
});
knoxx.backend.discord_gateway.credential_secret_value = (function knoxx$backend$discord_gateway$credential_secret_value(var_args){
var args__5882__auto__ = [];
var len__5876__auto___52006 = arguments.length;
var i__5877__auto___52007 = (0);
while(true){
if((i__5877__auto___52007 < len__5876__auto___52006)){
args__5882__auto__.push((arguments[i__5877__auto___52007]));

var G__52008 = (i__5877__auto___52007 + (1));
i__5877__auto___52007 = G__52008;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.backend.discord_gateway.credential_secret_value.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.backend.discord_gateway.credential_secret_value.cljs$core$IFn$_invoke$arity$variadic = (function (credential,ks){
var secrets = knoxx.backend.discord_gateway.credential_value(credential,new cljs.core.Keyword(null,"secretJson","secretJson",1807839704));
return cljs.core.some((function (k){
var G__51285 = (function (){var or__5142__auto__ = knoxx.backend.discord_gateway.credential_value(secrets,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.discord_gateway.credential_value(secrets,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.discord_gateway.credential_value(secrets,cljs.core.name(k));
}
}
})();
var G__51285__$1 = (((G__51285 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51285)));
var G__51285__$2 = (((G__51285__$1 == null))?null:clojure.string.trim(G__51285__$1));
if((G__51285__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51285__$2);
}
}),ks);
}));

(knoxx.backend.discord_gateway.credential_secret_value.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.backend.discord_gateway.credential_secret_value.cljs$lang$applyTo = (function (seq51278){
var G__51279 = cljs.core.first(seq51278);
var seq51278__$1 = cljs.core.next(seq51278);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__51279,seq51278__$1);
}));

knoxx.backend.discord_gateway.credential_actor_id = (function knoxx$backend$discord_gateway$credential_actor_id(credential){
var G__51295 = (function (){var or__5142__auto__ = knoxx.backend.discord_gateway.credential_value(credential,new cljs.core.Keyword(null,"actorId","actorId",989542370));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.discord_gateway.credential_value(credential,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.discord_gateway.credential_value(credential,new cljs.core.Keyword(null,"actor_id","actor_id",2086217260));
}
}
})();
var G__51295__$1 = (((G__51295 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51295)));
var G__51295__$2 = (((G__51295__$1 == null))?null:clojure.string.trim(G__51295__$1));
if((G__51295__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51295__$2);
}
});
knoxx.backend.discord_gateway.credential_bot_token = (function knoxx$backend$discord_gateway$credential_bot_token(credential){
return knoxx.backend.discord_gateway.credential_secret_value.cljs$core$IFn$_invoke$arity$variadic(credential,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"botToken","botToken",1995464313),new cljs.core.Keyword(null,"bot-token","bot-token",-851028031),new cljs.core.Keyword(null,"token","token",-1211463215)], 0));
});
knoxx.backend.discord_gateway.ensure_actor_manager_BANG_ = (function knoxx$backend$discord_gateway$ensure_actor_manager_BANG_(actor_id){
var actor_id__$1 = (function (){var G__51303 = actor_id;
var G__51303__$1 = (((G__51303 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51303)));
var G__51303__$2 = (((G__51303__$1 == null))?null:clojure.string.trim(G__51303__$1));
if((G__51303__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51303__$2);
}
})();
if(cljs.core.truth_(actor_id__$1)){
} else {
throw (new Error("actor id is required for Discord actor gateway"));
}

var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.discord_gateway.actor_managers_STAR_),actor_id__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var manager = knoxx.backend.discord_gateway.createDiscordGatewayManager(({"log": console, "setDefault": false}));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.discord_gateway.actor_managers_STAR_,cljs.core.assoc,actor_id__$1,manager);

return manager;
}
});
knoxx.backend.discord_gateway.start_actor_gateway_BANG_ = (function knoxx$backend$discord_gateway$start_actor_gateway_BANG_(actor_id,token){
var manager = knoxx.backend.discord_gateway.ensure_actor_manager_BANG_(actor_id);
return manager.start(token).then((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actorId","actorId",989542370),actor_id,new cljs.core.Keyword(null,"status","status",-1997798413),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(manager.status(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
}));
});
knoxx.backend.discord_gateway.start_actor_gateways_BANG_ = (function knoxx$backend$discord_gateway$start_actor_gateways_BANG_(credentials){
var rows = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = credentials;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var valid = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (credential){
var actor_id = knoxx.backend.discord_gateway.credential_actor_id(credential);
var token = knoxx.backend.discord_gateway.credential_bot_token(credential);
if(cljs.core.truth_((function (){var and__5140__auto__ = actor_id;
if(cljs.core.truth_(and__5140__auto__)){
return token;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actorId","actorId",989542370),actor_id,new cljs.core.Keyword(null,"token","token",-1211463215),token], null);
} else {
return null;
}
}),rows));
var active_actor_ids = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"actorId","actorId",989542370),valid));
var seq__51320_52042 = cljs.core.seq(cljs.core.deref(knoxx.backend.discord_gateway.actor_managers_STAR_));
var chunk__51321_52043 = null;
var count__51322_52044 = (0);
var i__51323_52045 = (0);
while(true){
if((i__51323_52045 < count__51322_52044)){
var vec__51341_52046 = chunk__51321_52043.cljs$core$IIndexed$_nth$arity$2(null,i__51323_52045);
var actor_id_52047 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51341_52046,(0),null);
var manager_52048 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51341_52046,(1),null);
if(cljs.core.contains_QMARK_(active_actor_ids,actor_id_52047)){
} else {
try{manager_52048.stop();
}catch (e51344){if((e51344 instanceof Error)){
var __52053 = e51344;
} else {
throw e51344;

}
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.actor_managers_STAR_,cljs.core.dissoc,actor_id_52047);
}


var G__52054 = seq__51320_52042;
var G__52055 = chunk__51321_52043;
var G__52056 = count__51322_52044;
var G__52057 = (i__51323_52045 + (1));
seq__51320_52042 = G__52054;
chunk__51321_52043 = G__52055;
count__51322_52044 = G__52056;
i__51323_52045 = G__52057;
continue;
} else {
var temp__5825__auto___52058 = cljs.core.seq(seq__51320_52042);
if(temp__5825__auto___52058){
var seq__51320_52067__$1 = temp__5825__auto___52058;
if(cljs.core.chunked_seq_QMARK_(seq__51320_52067__$1)){
var c__5673__auto___52068 = cljs.core.chunk_first(seq__51320_52067__$1);
var G__52069 = cljs.core.chunk_rest(seq__51320_52067__$1);
var G__52070 = c__5673__auto___52068;
var G__52071 = cljs.core.count(c__5673__auto___52068);
var G__52072 = (0);
seq__51320_52042 = G__52069;
chunk__51321_52043 = G__52070;
count__51322_52044 = G__52071;
i__51323_52045 = G__52072;
continue;
} else {
var vec__51411_52077 = cljs.core.first(seq__51320_52067__$1);
var actor_id_52078 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51411_52077,(0),null);
var manager_52079 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51411_52077,(1),null);
if(cljs.core.contains_QMARK_(active_actor_ids,actor_id_52078)){
} else {
try{manager_52079.stop();
}catch (e51418){if((e51418 instanceof Error)){
var __52086 = e51418;
} else {
throw e51418;

}
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.discord_gateway.actor_managers_STAR_,cljs.core.dissoc,actor_id_52078);
}


var G__52087 = cljs.core.next(seq__51320_52067__$1);
var G__52088 = null;
var G__52089 = (0);
var G__52090 = (0);
seq__51320_52042 = G__52087;
chunk__51321_52043 = G__52088;
count__51322_52044 = G__52089;
i__51323_52045 = G__52090;
continue;
}
} else {
}
}
break;
}

return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__51419){
var map__51420 = p__51419;
var map__51420__$1 = cljs.core.__destructure_map(map__51420);
var actorId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51420__$1,new cljs.core.Keyword(null,"actorId","actorId",989542370));
var token = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51420__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
return knoxx.backend.discord_gateway.start_actor_gateway_BANG_(actorId,token).catch((function (err){
console.warn("[discord-gateway] actor gateway start failed",actorId,err.message);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actorId","actorId",989542370),actorId,new cljs.core.Keyword(null,"error","error",-978969032),err.message], null);
}));
}),valid))).then((function (results){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}));
});
/**
 * Returns true if the gateway client exists.
 */
knoxx.backend.discord_gateway.started_QMARK_ = (function knoxx$backend$discord_gateway$started_QMARK_(){
return (!((cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_) == null)));
});
/**
 * Returns true if the gateway client is connected and ready.
 */
knoxx.backend.discord_gateway.ready_QMARK_ = (function knoxx$backend$discord_gateway$ready_QMARK_(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
var s = manager.status();
return cljs.core.boolean$((s["ready"]));
} else {
return null;
}
});
/**
 * Get gateway status as a JS object.
 */
knoxx.backend.discord_gateway.status = (function knoxx$backend$discord_gateway$status(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.status();
} else {
return null;
}
});
/**
 * Start the Discord gateway with the given token.
 */
knoxx.backend.discord_gateway.start_BANG_ = (function knoxx$backend$discord_gateway$start_BANG_(token){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.start(token);
} else {
return null;
}
});
/**
 * Stop the Discord gateway client.
 */
knoxx.backend.discord_gateway.stop_BANG_ = (function knoxx$backend$discord_gateway$stop_BANG_(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.stop();
} else {
return null;
}
});
/**
 * Stop and restart with the given token.
 */
knoxx.backend.discord_gateway.restart_BANG_ = (function knoxx$backend$discord_gateway$restart_BANG_(token){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.restart(token);
} else {
return null;
}
});
/**
 * Register a message listener. Returns an unsubscribe function.
 */
knoxx.backend.discord_gateway.on_message_BANG_ = (function knoxx$backend$discord_gateway$on_message_BANG_(listener){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.onMessage(listener);
} else {
return null;
}
});
/**
 * Register a reaction listener. Returns an unsubscribe function.
 */
knoxx.backend.discord_gateway.on_reaction_BANG_ = (function knoxx$backend$discord_gateway$on_reaction_BANG_(listener){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.onReaction(listener);
} else {
return null;
}
});
/**
 * Register a voice state update listener. Returns an unsubscribe function.
 */
knoxx.backend.discord_gateway.on_voice_state_update_BANG_ = (function knoxx$backend$discord_gateway$on_voice_state_update_BANG_(listener){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.onVoiceStateUpdate(listener);
} else {
return null;
}
});
/**
 * List all guilds the bot is in. Returns a Promise.
 */
knoxx.backend.discord_gateway.list_servers = (function knoxx$backend$discord_gateway$list_servers(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.listServers();
} else {
return null;
}
});
/**
 * List channels in a guild (or all guilds if guild-id is nil). Returns a Promise.
 */
knoxx.backend.discord_gateway.list_channels = (function knoxx$backend$discord_gateway$list_channels(var_args){
var G__51511 = arguments.length;
switch (G__51511) {
case 0:
return knoxx.backend.discord_gateway.list_channels.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return knoxx.backend.discord_gateway.list_channels.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.discord_gateway.list_channels.cljs$core$IFn$_invoke$arity$0 = (function (){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.listChannels();
} else {
return null;
}
}));

(knoxx.backend.discord_gateway.list_channels.cljs$core$IFn$_invoke$arity$1 = (function (guild_id){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.listChannels(guild_id);
} else {
return null;
}
}));

(knoxx.backend.discord_gateway.list_channels.cljs$lang$maxFixedArity = 1);

/**
 * Fetch messages from a channel. Returns a Promise.
 */
knoxx.backend.discord_gateway.fetch_channel_messages = (function knoxx$backend$discord_gateway$fetch_channel_messages(channel_id,opts){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.fetchChannelMessages(channel_id,opts);
} else {
return null;
}
});
/**
 * Fetch DM messages with a user. Returns a Promise.
 */
knoxx.backend.discord_gateway.fetch_dm_messages = (function knoxx$backend$discord_gateway$fetch_dm_messages(user_id,opts){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.fetchDmMessages(user_id,opts);
} else {
return null;
}
});
/**
 * Search messages in a channel or DM. Returns a Promise.
 */
knoxx.backend.discord_gateway.search_messages = (function knoxx$backend$discord_gateway$search_messages(scope,opts){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.searchMessages(scope,opts);
} else {
return null;
}
});
/**
 * Send a message to a channel. Returns a Promise.
 */
knoxx.backend.discord_gateway.send_message = (function knoxx$backend$discord_gateway$send_message(var_args){
var G__51539 = arguments.length;
switch (G__51539) {
case 3:
return knoxx.backend.discord_gateway.send_message.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.discord_gateway.send_message.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.discord_gateway.send_message.cljs$core$IFn$_invoke$arity$3 = (function (channel_id,text,reply_to){
return knoxx.backend.discord_gateway.send_message.cljs$core$IFn$_invoke$arity$4(channel_id,text,reply_to,null);
}));

(knoxx.backend.discord_gateway.send_message.cljs$core$IFn$_invoke$arity$4 = (function (channel_id,text,reply_to,attachments){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.sendMessage(channel_id,text,reply_to,attachments);
} else {
return null;
}
}));

(knoxx.backend.discord_gateway.send_message.cljs$lang$maxFixedArity = 4);

/**
 * Join a voice channel. Returns a Promise.
 */
knoxx.backend.discord_gateway.join_voice = (function knoxx$backend$discord_gateway$join_voice(channel_id){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.joinVoice(channel_id);
} else {
return null;
}
});
/**
 * Leave a voice channel for a guild. Returns a Promise.
 */
knoxx.backend.discord_gateway.leave_voice = (function knoxx$backend$discord_gateway$leave_voice(guild_id){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.leaveVoice(guild_id);
} else {
return null;
}
});
/**
 * Play an audio buffer in a voice channel. Returns a Promise.
 */
knoxx.backend.discord_gateway.play_audio = (function knoxx$backend$discord_gateway$play_audio(guild_id,audio_buffer){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.playAudio(guild_id,audio_buffer);
} else {
return null;
}
});
/**
 * Start listening for voice input. Returns a Promise of a stop function.
 */
knoxx.backend.discord_gateway.start_voice_listener = (function knoxx$backend$discord_gateway$start_voice_listener(guild_id,on_start,on_audio){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.startVoiceListener(guild_id,on_start,on_audio);
} else {
return null;
}
});
/**
 * Get the current voice connection for a guild.
 */
knoxx.backend.discord_gateway.get_voice_connection = (function knoxx$backend$discord_gateway$get_voice_connection(guild_id){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.getVoiceConnection(guild_id);
} else {
return null;
}
});
/**
 * List members in a voice channel. Returns a Promise.
 */
knoxx.backend.discord_gateway.list_voice_members = (function knoxx$backend$discord_gateway$list_voice_members(guild_id,channel_id){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.discord_gateway.manager_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var manager = temp__5825__auto__;
return manager.listVoiceMembers(guild_id,channel_id);
} else {
return null;
}
});

//# sourceMappingURL=knoxx.backend.discord_gateway.js.map
