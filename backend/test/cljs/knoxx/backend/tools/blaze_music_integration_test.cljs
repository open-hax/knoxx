(ns knoxx.backend.tools.blaze-music-integration-test
  (:require [cljs.test :refer [async deftest is testing]]
            [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.domain.media.blaze :as blaze]
            [knoxx.backend.domain.media :as media]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(defn- env
  [k]
  (some-> js/process .-env (aget k) str str/trim not-empty))

(defn- bool-env?
  [k]
  (contains? #{"1" "true" "yes" "on"}
             (str/lower-case (str (or (env k) "")))))

(defn- skip!
  [done msg]
  ;; Keep test green when not explicitly enabled.
  (is true msg)
  (done))

(deftest blaze-music-generate-execute-integration
  (async done
    (testing "Full API integration: calls real Proxx /v1/music/generations and asserts Knoxx saves an output file"
      (let [enabled? (bool-env? "KNOXX_RUN_BLAZE_INTEGRATION")
            token (or (env "PROXX_AUTH_TOKEN")
                      (env "PROXY_AUTH_TOKEN"))
            base-url (or (env "PROXX_BASE_URL") "http://127.0.0.1:8789")
            workspace-root (str "/tmp/knoxx-blaze-integration-" (.randomUUID crypto))
            output-path (str "Music/blaze/integration-" (.randomUUID crypto) ".mp3")]
        (cond
          (not enabled?)
          (skip! done "Skipping integration test; set KNOXX_RUN_BLAZE_INTEGRATION=true")

          (str/blank? (str token))
          (skip! done "Skipping integration test; missing PROXX_AUTH_TOKEN/PROXY_AUTH_TOKEN")

          :else
          (let [promise
                (p/let [runtime #js {}
                        config (models/enrich-config
                                {:contracts-dir "contracts"
                                 :workspace-root workspace-root
                                 :proxx-base-url base-url
                                 :proxx-auth-token token})
                        _ (.mkdir fs workspace-root #js {:recursive true})
                        ;; Use a small instrumental prompt so this can complete quickly,
                        ;; but still exercises the full HTTP path.
                        _ (blaze/blaze-music-generate-execute
                           runtime
                           config
                           "integration-tool-call"
                           #js {"prompt" "A minor, 90 BPM. Short ambient instrumental test cue."
                                "output_path" output-path
                                "is_instrumental" true
                                "lyrics_optimizer" false}
                           nil nil nil)
                        {:keys [absolute]} (media/resolve-workspace-media-path runtime config output-path)
                        st (.stat fs absolute)]
                  (is (number? (.-size st)))
                  (is (> (.-size st) 1000) "saved mp3 is non-trivial")
                  ;; Optional: leave a marker file for operator inspection.
                  (.writeFile fs (.join path workspace-root "integration.result.txt")
                              (str "saved=" absolute "\n")
                              "utf8"))]
            (-> promise
                (.then (fn [_] (done)))
                (.catch (fn [e]
                          (is false (str e))
                          (done))))))))))
