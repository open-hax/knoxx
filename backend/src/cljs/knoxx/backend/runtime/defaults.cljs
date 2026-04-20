(ns knoxx.backend.runtime.defaults
  (:require [knoxx.backend.runtime.models :refer [proxx-openai-base-url]]))

(defn default-model
  [config]
  (or (:proxx-default-model config) "glm-5"))

(defn default-settings
  [config]
  {:llmModel (default-model config)
   :embedModel (:proxx-embed-model config)
   :maxContextTokens 128000
   :llmMaxTokens 8192
   :llmBaseUrl (proxx-openai-base-url config)
   :embedBaseUrl (:proxx-base-url config)
   :retrievalMode "dense"
   :retrievalTopK 6
   :hybridTopKDense 12
   :hybridTopKSparse 20
   :hybridTopKFinal 6
   :hybridFusion "rrf"
   :hybridRrfK 60
   :vectorDim 1024
   :chunkTargetTokens 500
   :chunkMaxTokens 700
   :projectName (:project-name config)
   :qdrantCollection (:collection-name config)
   :docsPath (str (:workspace-root config) "/.knoxx/databases/default/docs")
   :docsExtensions ".md,.mdx,.txt,.json,.org,.html,.csv,.pdf"})
