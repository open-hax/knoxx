import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.runtime.models.js";
goog.provide('knoxx.backend.runtime.defaults');
knoxx.backend.runtime.defaults.default_model = (function knoxx$backend$runtime$defaults$default_model(config){
var or__5142__auto__ = new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "glm-5";
}
});
knoxx.backend.runtime.defaults.default_settings = (function knoxx$backend$runtime$defaults$default_settings(config){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"hybridFusion","hybridFusion",243129089),new cljs.core.Keyword(null,"retrievalMode","retrievalMode",-1090540764),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882),new cljs.core.Keyword(null,"chunkMaxTokens","chunkMaxTokens",285856486),new cljs.core.Keyword(null,"retrievalTopK","retrievalTopK",1126924102),new cljs.core.Keyword(null,"projectName","projectName",295421548),new cljs.core.Keyword(null,"embedModel","embedModel",1987630417),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371),new cljs.core.Keyword(null,"hybridTopKFinal","hybridTopKFinal",-935279468),new cljs.core.Keyword(null,"hybridTopKSparse","hybridTopKSparse",1648181684),new cljs.core.Keyword(null,"embedBaseUrl","embedBaseUrl",645581556),new cljs.core.Keyword(null,"llmMaxTokens","llmMaxTokens",-456005003),new cljs.core.Keyword(null,"maxContextTokens","maxContextTokens",-1106735978),new cljs.core.Keyword(null,"hybridRrfK","hybridRrfK",540609690),new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982),new cljs.core.Keyword(null,"llmBaseUrl","llmBaseUrl",545459739),new cljs.core.Keyword(null,"docsExtensions","docsExtensions",1868790204),new cljs.core.Keyword(null,"chunkTargetTokens","chunkTargetTokens",-811403010),new cljs.core.Keyword(null,"vectorDim","vectorDim",-200961730),new cljs.core.Keyword(null,"hybridTopKDense","hybridTopKDense",388190815)],["rrf","dense",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config))+"/.knoxx/databases/default/docs"),(700),(6),new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"proxx-embed-model","proxx-embed-model",-289269914).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"collection-name","collection-name",600435477).cljs$core$IFn$_invoke$arity$1(config),(6),(20),new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config),(8192),(128000),(60),knoxx.backend.runtime.defaults.default_model(config),knoxx.backend.runtime.models.provider_openai_base_url(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config)),".md,.mdx,.txt,.json,.org,.html,.csv,.pdf",(500),(1024),(12)]);
});

//# sourceMappingURL=knoxx.backend.runtime.defaults.js.map
