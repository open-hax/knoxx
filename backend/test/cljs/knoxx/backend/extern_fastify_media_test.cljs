(ns knoxx.backend.extern-fastify-media-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.multipart :as multipart]
            [knoxx.backend.extern.websocket :as websocket]))

(deftest fastify-request-extraction-normalizes-cljs-data
  (testing "request body/query/headers are exposed as CLJS maps and query strings preserve repeated values"
    (let [request #js {:body #js {:text "hello" :nested #js {:ok true}}
                       :query #js {:tag #js ["a" "b"] :empty nil :q "knoxx"}
                       :headers #js {:host "example.test"
                                     :connection "close"
                                     :authorization "Bearer token"}
                       :method "POST"}]
      (is (= {:text "hello" :nested {:ok true}}
             (fastify/request-body request)))
      (is (= {:tag ["a" "b"] :empty nil :q "knoxx"}
             (fastify/request-query request)))
      (is (= "Bearer token" (fastify/request-header request :authorization)))
      (is (= "?tag=a&tag=b&q=knoxx" (fastify/query-string request)))
      (let [headers (fastify/forward-headers request {"x-api-key" "secret"})]
        (is (= "Bearer token" (.get headers "authorization")))
        (is (= "secret" (.get headers "x-api-key")))
        (is (nil? (.get headers "host")))
        (is (nil? (.get headers "connection")))))))

(deftest fastify-send-json-guards-already-sent-replies
  (testing "JSON send converts CLJS body only when reply is still writable"
    (let [sent* (atom nil)
          reply (js-obj)]
      (aset reply "sent" false)
      (aset reply "raw" #js {:writableEnded false})
      (aset reply "code" (fn [status]
                            (swap! sent* assoc :status status)
                            reply))
      (aset reply "type" (fn [mime]
                            (swap! sent* assoc :type mime)
                            reply))
      (aset reply "send" (fn [body]
                            (swap! sent* assoc :body body)
                            reply))
      (fastify/send-json! reply 201 {:ok true})
      (is (= 201 (:status @sent*)))
      (is (= "application/json" (:type @sent*)))
      (is (true? (aget (:body @sent*) "ok")))))
  (testing "already-sent replies are left untouched"
    (let [called* (atom false)
          reply #js {:sent true
                     :send (fn [_]
                             (reset! called* true))}]
      (fastify/send-json! reply 200 {:ok true})
      (is (false? @called*)))))

(deftest ^:async multipart-file-part-normalization-and-buffer-read
  (testing "multipart file metadata remains opaque while buffers are read at the adapter boundary"
    (let [part #js {:type "file"
                    :filename "voice.wav"
                    :mimetype "audio/wav"
                    :size 5
                    :file (js/Blob. #js ["hello"])}
          buffer (await (multipart/part-buffer! part))]
      (is (multipart/file-part? part))
      (is (= [part] (vec (multipart/file-parts [part #js {:type "field"}]))))
      (is (= "voice.wav" (multipart/part-filename part)))
      (is (= "audio/wav" (multipart/part-mime-type part)))
      (is (= 5 (multipart/part-size part)))
      (is (= "hello" (.toString buffer "utf8"))))))

(deftest websocket-send-json-and-event-normalization
  (testing "websocket adapter owns JSON serialization and provider event shape"
    (let [sent* (atom nil)
          socket #js {:readyState 1
                      :send (fn [payload]
                              (reset! sent* payload))}]
      (websocket/send-json! socket {:type "ping" :ok true})
      (is (= {:type "ping" :ok true}
             (js->clj (.parse js/JSON @sent*) :keywordize-keys true)))
      (is (= {:type "audio"
              :audio "abc"
              :alignment {:chars ["a"]}
              :normalized_alignment {:chars ["a"]}}
             (websocket/voice-stream-event
              #js {:audio "abc"
                   :alignment #js {:chars #js ["a"]}
                   :normalizedAlignment #js {:chars #js ["a"]}})))
      (is (= {:type "final" :isFinal true}
             (websocket/voice-stream-event #js {:isFinal true}))))))
