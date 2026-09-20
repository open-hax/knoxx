(ns knoxx.backend.extern.resource-routes
  "Decode native resource requests and write legacy plain-text responses.")

(defn text-response!
  "Write the legacy plain-text resource response through its native reply handle."
  [reply status text]
  (.end reply (.status reply status) text (clj->js {"Content-Type" "text/plain; charset=utf-8"})))

(defn body-map
  "Decode a native resource request body into keyword-keyed data."
  [request]
  (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true))

(defn request-resource-kind
  "Select the resource kind or legacy class query parameter."
  [request default]
  (or (aget request "query" "kind")
      (aget request "query" "class")
      default))

(defn request-contract-class
  "Compatibility alias for old contract route clients."
  [request default]
  (request-resource-kind request default))

(defn body-resource-kind
  "Resolve supported body aliases before falling back to query data or the default."
  ([body default]
   (body-resource-kind body nil default))
  ([body request default]
   (or (:kind body)
       (:class body)
       (:resource_kind body)
       (:resource-kind body)
       (:resourceClass body)
       (:resource-class body)
       (:contract_class body)
       (:contract-class body)
       (some-> request (aget "query" "kind"))
       (some-> request (aget "query" "class"))
       default)))

(defn body-contract-class
  "Compatibility alias for old contract route clients."
  ([body default]
   (body-resource-kind body default))
  ([body request default]
   (body-resource-kind body request default)))

(defn body-edn-text
  "Read EDN text from the supported request body spellings."
  [body]
  (str (or (:ednText body)
           (:edn_text body)
           (:edn-text body)
           "")))

(defn query-value "Read one resource route query parameter." [request field-key]
  (aget request "query" field-key))
(defn params-value "Read one resource route path parameter." [request field-key]
  (aget request "params" field-key))
(defn body-value "Read the opaque compatibility request body." [request]
  (aget request "body"))
