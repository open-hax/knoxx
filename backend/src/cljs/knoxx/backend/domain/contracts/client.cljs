(ns knoxx.backend.domain.contracts.client
  "Contract librarian HTTP client protocol.

   Covers `/api/agent/contracts`, contract read/write, and validation calls."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IContractLibrarianClient
  (list-contracts! [client klass])
  (read-contract! [client klass contract-id])
  (write-contract! [client klass contract-id edn-text])
  (validate-contract! [client klass edn-text]))

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn- class-query
  [klass]
  (str "?kind=" (js/encodeURIComponent (or klass "agents"))))

(defn- contract-url
  [base-url path]
  (str (trim-trailing-slashes base-url) path))

(defn- text-request!
  [http-client base-url method path opts]
  (p/let [resp (xfetch/text! http-client
                             {:url (contract-url base-url path)
                              :opts (merge {:method method} opts)
                              :timeout-ms 30000})]
    {:ok (:ok resp)
     :status (:status resp)
     :text (:body resp)}))

(defrecord FetchContractLibrarianClient [base-url http-client]
  IContractLibrarianClient
  (list-contracts! [_ klass]
    (text-request! (or http-client xfetch/default-client)
                   base-url
                   "GET"
                   (str "/api/agent/contracts" (class-query klass))
                   nil))
  (read-contract! [_ klass contract-id]
    (text-request! (or http-client xfetch/default-client)
                   base-url
                   "GET"
                   (str "/api/agent/contracts/" (js/encodeURIComponent contract-id) (class-query klass))
                   nil))
  (write-contract! [_ klass contract-id edn-text]
    (text-request! (or http-client xfetch/default-client)
                   base-url
                   "PUT"
                   (str "/api/agent/contracts/" (js/encodeURIComponent contract-id) (class-query klass))
                   {:headers {"Content-Type" "text/plain; charset=utf-8"}
                    :body edn-text}))
  (validate-contract! [_ klass edn-text]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url (contract-url base-url "/api/agent/contracts/validate")
                                :opts {:method "POST"
                                       :headers {"Content-Type" "application/json"}
                                       :json {:edn_text edn-text
                                              :contract_class klass}}
                                :timeout-ms 30000})]
      (:body resp))))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client]}]
   (->FetchContractLibrarianClient (or (:knoxx-base-url config) "")
                                   (or http-client xfetch/default-client))))
