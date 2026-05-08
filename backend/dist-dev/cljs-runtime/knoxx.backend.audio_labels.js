import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.audio_labels');
knoxx.backend.audio_labels.labels_file = "audio-labels.json";
/**
 * Read the labels JSON file from the workspace root. Returns promise.
 */
knoxx.backend.audio_labels.read_labels_file = (function knoxx$backend$audio_labels$read_labels_file(fs,workspace_root){
var path = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(workspace_root)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.audio_labels.labels_file));
return fs.readFile(path,"utf8").then((function (content){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(content),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
})).catch((function (_){
return cljs.core.PersistentArrayMap.EMPTY;
}));
});
/**
 * Write labels to the JSON file. Returns promise.
 */
knoxx.backend.audio_labels.write_labels_file_BANG_ = (function knoxx$backend$audio_labels$write_labels_file_BANG_(fs,workspace_root,labels){
var path = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(workspace_root)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.audio_labels.labels_file));
var data = JSON.stringify(cljs.core.clj__GT_js(labels),null,(2));
return fs.writeFile(path,data,"utf8");
});
/**
 * Create labels file if it doesn't exist. Returns promise of labels.
 */
knoxx.backend.audio_labels.ensure_labels_file_BANG_ = (function knoxx$backend$audio_labels$ensure_labels_file_BANG_(fs,workspace_root){
var path = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(workspace_root)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.audio_labels.labels_file));
return fs.stat(path).catch((function (_){
return knoxx.backend.audio_labels.write_labels_file_BANG_(fs,workspace_root,cljs.core.PersistentArrayMap.EMPTY);
})).then((function (_){
return knoxx.backend.audio_labels.read_labels_file(fs,workspace_root);
}));
});
/**
 * Get all labels for a file path. Returns promise.
 */
knoxx.backend.audio_labels.get_labels = (function knoxx$backend$audio_labels$get_labels(fs,workspace_root,file_path){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
return cljs.core.get.cljs$core$IFn$_invoke$arity$3(labels,file_path,cljs.core.PersistentVector.EMPTY);
}));
});
/**
 * Get all unique labels across all files. Returns promise.
 */
knoxx.backend.audio_labels.get_all_labels = (function knoxx$backend$audio_labels$get_all_labels(fs,workspace_root){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.concat,cljs.core.vals(labels)))));
}));
});
/**
 * Get all file paths that have a specific label. Returns promise.
 */
knoxx.backend.audio_labels.get_files_by_label = (function knoxx$backend$audio_labels$get_files_by_label(fs,workspace_root,label){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
return cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.key,cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__52345){
var vec__52346 = p__52345;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52346,(0),null);
var labels__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52346,(1),null);
return cljs.core.some((function (p1__52344_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__52344_SHARP_,label);
}),labels__$1);
}),labels)));
}));
});
/**
 * Add a label to a file. Returns promise of updated labels.
 */
knoxx.backend.audio_labels.add_label_BANG_ = (function knoxx$backend$audio_labels$add_label_BANG_(fs,workspace_root,file_path,label){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
var current = cljs.core.get.cljs$core$IFn$_invoke$arity$3(labels,file_path,cljs.core.PersistentVector.EMPTY);
var updated = (cljs.core.truth_(cljs.core.some((function (p1__52351_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__52351_SHARP_,label);
}),current))?current:cljs.core.conj.cljs$core$IFn$_invoke$arity$2(current,label));
return knoxx.backend.audio_labels.write_labels_file_BANG_(fs,workspace_root,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(labels,file_path,updated)).then((function (_){
return cljs.core.vec(updated);
}));
}));
});
/**
 * Remove a label from a file. Returns promise of updated labels.
 */
knoxx.backend.audio_labels.remove_label_BANG_ = (function knoxx$backend$audio_labels$remove_label_BANG_(fs,workspace_root,file_path,label){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
var current = cljs.core.get.cljs$core$IFn$_invoke$arity$3(labels,file_path,cljs.core.PersistentVector.EMPTY);
var updated = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__52356_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__52356_SHARP_,label);
}),current));
return knoxx.backend.audio_labels.write_labels_file_BANG_(fs,workspace_root,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(labels,file_path,updated)).then((function (_){
return updated;
}));
}));
});
/**
 * Set all labels for a file (replaces existing). Returns promise.
 */
knoxx.backend.audio_labels.set_labels_BANG_ = (function knoxx$backend$audio_labels$set_labels_BANG_(fs,workspace_root,file_path,new_labels){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
return knoxx.backend.audio_labels.write_labels_file_BANG_(fs,workspace_root,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(labels,file_path,cljs.core.vec(new_labels))).then((function (_){
return cljs.core.vec(new_labels);
}));
}));
});
/**
 * Sanitize a label for use as a directory name.
 */
knoxx.backend.audio_labels.sanitize_dirname = (function knoxx$backend$audio_labels$sanitize_dirname(label){
return clojure.string.replace(clojure.string.replace(clojure.string.lower_case(label),/[^a-z0-9]+/,"-"),/^-|-$/,"");
});
/**
 * Create symlinks for a single label. Returns promise.
 */
knoxx.backend.audio_labels.create_symlinks_for_label = (function knoxx$backend$audio_labels$create_symlinks_for_label(fs,node_path,audio_dir,label,files){
var label_dir = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(audio_dir)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.audio_labels.sanitize_dirname(label)));
return fs.mkdir(label_dir,({"recursive": true})).catch((function (_){
return null;
})).then((function (_){
var create_link = (function (file_path){
var filename = node_path.basename(file_path);
var link_path = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label_dir)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename));
return fs.symlink(file_path,link_path).catch((function (___$1){
return null;
}));
});
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(create_link,files)));
}));
});
/**
 * Count total symlinks in audio directory. Returns promise.
 */
knoxx.backend.audio_labels.count_symlinks = (function knoxx$backend$audio_labels$count_symlinks(fs,audio_dir){
return fs.readdir(audio_dir).then((function (dirs){
var dir_paths = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (d){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(audio_dir)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(d));
}),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(dirs));
var count_dir = (function (dp){
return fs.stat(dp).then((function (stat){
if(cljs.core.truth_(stat.isDirectory())){
return fs.readdir(dp).then((function (f){
return cljs.core.count(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(f));
})).catch((function (_){
return (0);
}));
} else {
return (0);
}
})).catch((function (_){
return (0);
}));
});
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(count_dir,dir_paths))).then((function (counts){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._PLUS_,(0),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(counts));
}));
})).catch((function (_){
return (0);
}));
});
/**
 * Create symlink directory structure for labeled files.
 * Creates ./audio/<label>/ symlinks pointing to original files.
 * Returns promise of symlink count.
 */
knoxx.backend.audio_labels.sync_symlinks_BANG_ = (function knoxx$backend$audio_labels$sync_symlinks_BANG_(fs,node_path,workspace_root){
return knoxx.backend.audio_labels.ensure_labels_file_BANG_(fs,workspace_root).then((function (labels){
var audio_dir = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(workspace_root)+"/audio");
var all_labels = cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.concat,cljs.core.vals(labels)));
var label_files = (function (label){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.key,cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__52403){
var vec__52404 = p__52403;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52404,(0),null);
var lbls = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__52404,(1),null);
return cljs.core.some((function (p1__52395_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__52395_SHARP_,label);
}),lbls);
}),labels));
});
return fs.mkdir(audio_dir,({"recursive": true})).catch((function (_){
return null;
})).then((function (_){
var process_label = (function (label){
return knoxx.backend.audio_labels.create_symlinks_for_label(fs,node_path,audio_dir,label,label_files(label));
});
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(process_label,all_labels)));
})).then((function (_){
return knoxx.backend.audio_labels.count_symlinks(fs,audio_dir);
}));
}));
});

//# sourceMappingURL=knoxx.backend.audio_labels.js.map
