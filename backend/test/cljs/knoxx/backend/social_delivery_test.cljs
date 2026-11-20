(ns knoxx.backend.social-delivery-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.bluesky-text :as bluesky-text]
            [knoxx.backend.infra.agent.social-delivery :as delivery]
            [knoxx.backend.law.social-delivery :as law]
            [malli.core :as m]))

(def policy
  {:channel-id "home" :self-identities ["openhax.test" "did:plc:self"]})

(def source-url "https://outside.test/work")

(def params {:channel_id "home" :text (str "A specific observation. " source-url)})

(defn receipt
  "A completed original receipt, not a retrieved description of one."
  [name input details]
  {:tool_name name :input input :status "completed" :is_error false :result {:details details}})

(def complete-evidence
  [(receipt "bluesky_thread" {:uri "at://did:plc:outside/app.bsky.feed.post/key"}
            {:root {:handle "outside.test" :uri "at://did:plc:outside/app.bsky.feed.post/key"}})
   (receipt "browser_read" {:url source-url} {:metadata {:sourceReadable true}})
   (receipt "discord_read" {:channel_id "home" :limit 100} {:messages [{:content "An unrelated prior message"}]})
   (receipt "memory_search" {:kind "knoxx.tool_receipt" :query source-url} {:hits []})])

(deftest source-delivery-requires-each-independent-evidence-stage
  (is (empty? (law/missing-evidence policy complete-evidence params)))
  (doseq [index (range (count complete-evidence))]
    (testing (str "Removing stage " index " prevents delivery")
      (is (seq (law/missing-evidence policy (vec (keep-indexed #(when (not= index %1) %2) complete-evidence)) params)))
      (is (seq (law/missing-evidence policy (assoc-in complete-evidence [index :status] "running") params)))
      (is (seq (law/missing-evidence policy (assoc-in complete-evidence [index :is_error] true) params))))))

(deftest outside-evidence-is-author-and-source-specific
  (is (seq (law/missing-evidence policy (assoc-in complete-evidence [0 :result :details :root :handle] "openhax.test") params)))
  (is (seq (law/missing-evidence policy (assoc-in complete-evidence [0 :result :details :root :uri] "at://did:plc:self/app.bsky.feed.post/k") params)))
  (is (seq (law/missing-evidence policy (assoc-in complete-evidence [1 :result :details :metadata :sourceReadable] false) params)))
  (is (seq (law/missing-evidence policy (assoc-in complete-evidence [3 :input :query] "outside work") params)))
  (is (seq (law/missing-evidence policy complete-evidence (assoc params :text "An unlinked manifesto"))))
  (is (seq (law/missing-evidence policy complete-evidence (assoc params :text "https://uninspected.test"))))
  (is (seq (law/missing-evidence policy complete-evidence (assoc params :channel_id "<CONTEXTUAL_CHANNEL_ID>"))))
  (is (seq (law/missing-evidence policy (assoc-in complete-evidence [2 :result :details :messages 0 :content] source-url) params)))
  (is (thrown? js/Error (law/missing-evidence {} complete-evidence params))))

(deftest actual-media-can-satisfy-the-outside-material-read
  (let [with-media (assoc complete-evidence 1
                          (receipt "multimodal_inspect" {:source source-url} {:observation "An attributed audio observation"}))]
    (is (empty? (law/missing-evidence policy with-media params)))
    (is (seq (law/missing-evidence policy (assoc-in with-media [1 :result :details] {}) params)))))

(deftest source-refusal-identifies-inspected-work-without-accepting-a-related-url
  (let [unread "https://bsky.app/profile/outside.test/post/older"
        check (law/delivery-evidence policy complete-evidence (assoc params :text unread))]
    (is (seq (:missing check)))
    (is (= [source-url "https://bsky.app/profile/outside.test/post/key"]
           (:inspected-source-urls check)))
    (is (= [unread] (:uninspected-source-urls check)))
    (is (not (some #{unread} (:inspected-source-urls check))))
    (is (not (some #{source-url}
                   (:inspected-source-urls
                    (law/delivery-evidence policy
                                           (assoc-in complete-evidence [1 :is_error] true)
                                           params)))))
    (is (empty? (:missing (law/delivery-evidence policy complete-evidence params))))))

(deftest captured-refusal-gives-the-model-a-concrete-source-repair
  (with-redefs [run-state/runs* (atom {"r" {:run_id "r" :conversation_id "c" :actor_id "a"
                                          :status "running" :tool_receipts complete-evidence}})
                turn-control/active-turn (constantly {:run_id "r"})]
    (let [guard (delivery/make-guard {:conversation-id "c" :actor-id "a" :policy policy})]
      (try
        (guard (assoc params :text "https://uninspected.test"))
        (is false "Related or invented URLs cannot substitute for inspected work")
        (catch :default err
          (is (re-find #"Inspected source URLs" (.-message err)))
          (is (str/includes? (.-message err) source-url))
          (is (= ["https://uninspected.test"] (:uninspected-source-urls (ex-data err))))))
      (is (nil? (guard params)) "A corrected source can proceed without weakening the guard"))))

(deftest captured-conversation-receipts-and-one-attempt-reservation
  (let [runs (atom {"r1" {:run_id "r1" :conversation_id "c1" :actor_id "a1" :status "running" :tool_receipts complete-evidence}
                    "r2" {:run_id "r2" :conversation_id "c2" :actor_id "a2" :status "running" :tool_receipts complete-evidence}})
        active (atom {"c1" {:run_id "r1"} "c2" {:run_id "r2"}})]
    (with-redefs [run-state/runs* runs turn-control/active-turn #(get @active %)]
      (let [guard (delivery/make-guard {:conversation-id "c1" :actor-id "a1" :policy policy})]
        (is (thrown? js/Error (guard (assoc params :text "Missing source"))))
        (is (nil? (guard params)) "A refusal does not consume the one platform attempt")
        (is (thrown? js/Error (guard params)) "Parallel or ambiguous repeat attempts cannot send twice")
        (swap! active assoc "c1" {:run_id "r2"})
        (is (thrown? js/Error (guard params)) "A different actor/conversation cannot supply evidence")
        (swap! runs assoc "r3" {:run_id "r3" :conversation_id "c1" :actor_id "a1" :status "running" :tool_receipts complete-evidence})
        (swap! active assoc "c1" {:run_id "r3"})
        (is (nil? (guard params)) "A new turn does not inherit the previous delivery reservation")
        (swap! active dissoc "c1")
        (is (thrown? js/Error (guard params)) "A persisted run alone is not live evidence")))))

(def outside-uri "at://did:plc:outside/app.bsky.feed.post/key")
(def reply-params {:replyTo outside-uri :text "Which part did you build first?"})
(def reply-evidence
  [(receipt "bluesky_thread" {:uri outside-uri}
            {:root {:uri outside-uri :handle "outside.test" :text "I built this."}})
   (receipt "memory_search" {:kind "knoxx.tool_receipt" :query outside-uri} {:hits []})])

(deftest reply-evidence-distinguishes-the-requested-post-from-the-original-root
  (let [target {:uri outside-uri :handle "outside.test" :available true}
        root {:uri "at://did:plc:self/app.bsky.feed.post/root" :handle "openhax.test"}
        evidence (assoc-in reply-evidence [0 :result :details] {:target target :root root})]
    (is (empty? (:missing (law/bluesky-publish-evidence policy evidence reply-params)))
        "an outside reply inside our own original thread is a valid exact reply target")
    (doseq [wrong-target [(assoc target :uri (str outside-uri "other"))
                         (assoc target :available false)
                         (assoc target :handle "openhax.test")
                         nil]]
      (is (seq (:missing (law/bluesky-publish-evidence
                          policy (assoc-in evidence [0 :result :details :target] wrong-target)
                          reply-params)))
          "the original root cannot substitute for a missing, unavailable or different requested post"))))

(deftest bluesky-replies-require-the-exact-completed-outside-thread-and-memory
  (is (empty? (:missing (law/bluesky-publish-evidence policy reply-evidence reply-params))))
  (doseq [index (range (count reply-evidence))]
    (is (seq (:missing (law/bluesky-publish-evidence policy (assoc-in reply-evidence [index :is_error] true) reply-params))))
    (is (seq (:missing (law/bluesky-publish-evidence policy (assoc-in reply-evidence [index :status] "running") reply-params)))))
  (doseq [receipts [(assoc-in reply-evidence [0 :input :uri] (str outside-uri "other"))
                   (assoc-in reply-evidence [0 :result :details :root :uri] (str outside-uri "other"))
                   (assoc-in reply-evidence [0 :tool_name] "bluesky_author_feed")
                   (assoc-in reply-evidence [0 :result :details :root :handle] "openhax.test")
                   (assoc-in reply-evidence [1 :input :query] "outside.test")
                   [(receipt "memory_session" {} {:tool_receipts reply-evidence})]]]
    (is (seq (:missing (law/bluesky-publish-evidence policy receipts reply-params)))))
  (is (seq (:missing (law/bluesky-publish-evidence policy reply-evidence (assoc reply-params :replyTo "invalid")))))
  (is (thrown? js/Error (law/bluesky-publish-evidence {} reply-evidence reply-params))))

(deftest bluesky-context-is-independent-of-discord-history-and-browser-availability
  (let [failed-browser (assoc (receipt "browser_read" {:url "https://blocked.test/"} {}) :is_error true)]
    (is (empty? (:missing (law/bluesky-publish-evidence policy (conj reply-evidence failed-browser) reply-params)))
        "A text reply can proceed from the exact thread; this is not factual verification of its claims")
    (is (seq (:missing (law/bluesky-publish-evidence policy complete-evidence reply-params)))
        "Discord-ready evidence lacks the exact reply-target memory query")))

(deftest bluesky-publish-policy-and-linked-reply-evidence
  (is (m/validate law/Obligations (assoc policy :bluesky-publish? true)))
  (is (not (m/validate law/Obligations (assoc policy :bluesky-publish? "true"))))
  (is (seq (:missing (law/bluesky-publish-evidence policy reply-evidence
                                                 (assoc reply-params :text "Read https://uninspected.test/")))))
  (is (empty? (:missing (law/bluesky-publish-evidence policy (into reply-evidence complete-evidence)
                                                    (assoc reply-params :text source-url))))))

(deftest original-bluesky-publications-require-an-inspected-attributed-source
  (let [post (dissoc params :channel_id)]
    (is (empty? (:missing (law/bluesky-publish-evidence policy complete-evidence post))))
    (is (seq (:missing (law/bluesky-publish-evidence policy [] post))))
    (is (seq (:missing (law/bluesky-publish-evidence policy complete-evidence {:text "An unsupported announcement"}))))
    (is (seq (:missing (law/bluesky-publish-evidence policy complete-evidence {:text "https://uninspected.test/"}))))
    (is (seq (:missing (law/bluesky-publish-evidence policy
                                                    (assoc-in complete-evidence [1 :is_error] true) post))))))

(deftest bluesky-publish-guard-captures-identity-and-reserves-one-platform-attempt
  (let [runs (atom {"r" {:run_id "r" :conversation_id "c" :actor_id "a" :status "running" :tool_receipts []}})]
    (with-redefs [run-state/runs* runs turn-control/active-turn (constantly {:run_id "r"})]
      (let [guard (delivery/make-bluesky-guard {:conversation-id "c" :actor-id "a" :policy policy})]
        (try (guard reply-params)
             (is false "No platform call may precede the exact context reads")
             (catch :default err
               (is (str/includes? (.-message err) "No Bluesky post was sent"))
               (is (str/includes? (.-message err) outside-uri))))
        (swap! runs assoc-in ["r" :tool_receipts] reply-evidence)
        (is (nil? (guard reply-params)) "Repairing evidence does not consume the reservation")
        (is (thrown? js/Error (guard reply-params)) "An ambiguous platform failure cannot trigger another publish")
        (swap! runs assoc-in ["r" :actor_id] "foreign")
        (is (thrown? js/Error (guard reply-params)) "Another actor cannot supply context")))))

(def self-post-one "at://did:plc:self/app.bsky.feed.post/part1")
(def self-post-two "at://did:plc:self/app.bsky.feed.post/part2")

(deftest self-replies-require-an-opted-in-confirmed-linear-thread
  (let [thread-policy (assoc policy :bluesky-thread-max-posts 3)
        published (receipt "bluesky_publish" reply-params {:uri self-post-one :cid "one-cid"})
        evidence (conj reply-evidence published)
        next-post {:replyTo self-post-one :text "One concrete additional thought."}]
    (is (empty? (:missing (law/bluesky-publish-evidence thread-policy evidence next-post))))
    (doseq [bad [(assoc published :status "running")
                 (assoc published :is_error true)
                 (assoc-in published [:result :details :cid] "")
                 (assoc-in published [:result :details :uri] outside-uri)
                 (assoc-in published [:result :details :uri] "at://did:plc:selfish/app.bsky.feed.post/key")]]
      (is (seq (:missing (law/bluesky-publish-evidence thread-policy (conj reply-evidence bad) next-post)))))
    (is (seq (:missing (law/bluesky-publish-evidence policy evidence next-post))))
    (is (seq (:missing (law/bluesky-publish-evidence thread-policy evidence
                                                   (assoc next-post :text "https://uninspected.test/")))))
    (is (not (m/validate law/Obligations (assoc policy :bluesky-thread-max-posts 6))))))

(deftest bounded-thread-reservations-wait-for-receipts-and-refuse-forks
  (let [runs (atom {"r" {:run_id "r" :conversation_id "c" :actor_id "a" :status "running"
                         :tool_receipts reply-evidence}})
        thread-policy (assoc policy :bluesky-thread-max-posts 3)]
    (with-redefs [run-state/runs* runs turn-control/active-turn (constantly {:run_id "r"})]
      (let [guard (delivery/make-bluesky-guard {:conversation-id "c" :actor-id "a" :policy thread-policy})
            second-post {:replyTo self-post-one :text "Part two"}
            third-post {:replyTo self-post-two :text "Part three"}]
        (is (nil? (guard reply-params)))
        (is (thrown? js/Error (guard second-post)) "No continuation before confirmed platform receipt")
        (swap! runs update-in ["r" :tool_receipts] conj
               (receipt "bluesky_publish" reply-params {:uri self-post-one :cid "one-cid"}))
        (is (thrown? js/Error (guard reply-params)) "A second outside reply would fork the run")
        (is (nil? (guard second-post)))
        (is (thrown? js/Error (guard second-post)) "No parallel retry after reserving part two")
        (swap! runs update-in ["r" :tool_receipts] conj
               (receipt "bluesky_publish" second-post {:uri self-post-two :cid "two-cid"}))
        (is (thrown? js/Error (guard second-post)) "Cannot branch from an earlier part")
        (is (nil? (guard third-post)))
        (swap! runs update-in ["r" :tool_receipts] conj
               (receipt "bluesky_publish" third-post
                        {:uri "at://did:plc:self/app.bsky.feed.post/part3" :cid "three-cid"}))
        (is (thrown? js/Error (guard {:replyTo "at://did:plc:self/app.bsky.feed.post/part3" :text "Part four"}))
            "The configured thread cap includes the first publication")))))

(deftest unicode-limits-are-measured-before-the-platform
  (is (= {:graphemes 300 :utf8-bytes 1200}
         (bluesky-text/validate-post-text! (apply str (repeat 300 "🦆")))))
  (is (= {:graphemes 300 :utf8-bytes 900}
         (bluesky-text/validate-post-text! (apply str (repeat 300 "é")))))
  (doseq [text [nil " " (apply str (repeat 301 "x")) (apply str (repeat 150 "👨‍👩‍👧‍👦"))]]
    (is (thrown? js/Error (bluesky-text/validate-post-text! text)))))

(deftest local-length-refusal-does-not-spend-a-thread-position
  (with-redefs [run-state/runs* (atom {"r" {:run_id "r" :conversation_id "c" :actor_id "a"
                                          :status "running" :tool_receipts reply-evidence}})
                turn-control/active-turn (constantly {:run_id "r"})]
    (let [guard (delivery/make-bluesky-guard {:conversation-id "c" :actor-id "a" :policy policy})]
      (try (guard (assoc reply-params :text (apply str (repeat 301 "x"))))
           (is false "An overlong post must be refused locally")
           (catch :default err
             (is (= "bluesky_text_invalid" (:code (ex-data err))))))
      (is (nil? (guard reply-params)) "Corrected prose can use the still-unspent reservation"))))
