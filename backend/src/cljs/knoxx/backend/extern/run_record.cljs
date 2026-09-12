(ns knoxx.backend.extern.run-record
  "Normalize SDK-native arrays before storing run records in the CLJS heap.")

(defn normalize-run
  "Decode the native array fields accepted by the existing run record contract."
  [run]
  (reduce (fn [record field]
            (if (array? (get record field))
              (update record field #(js->clj % :keywordize-keys true))
              record))
          run
          [:tool_receipts :trace_blocks :content_parts :events
           :request_messages :resources :settings]))
