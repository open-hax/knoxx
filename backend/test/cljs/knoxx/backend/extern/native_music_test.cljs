(ns knoxx.backend.extern.native-music-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.music :as music]
            [knoxx.backend.extern.native-music :as native-music]
            ["node:child_process" :refer [execFile]]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(deftest ^:async promisified-exec-file-returns-a-process-result-object
  (let [result (await ((promisify execFile) (.-execPath js/process)
                      #js ["-e" "process.stdout.write('stdout'); process.stderr.write('stderr')"]))]
    (is (= "stdout" (.-stdout result)))
    (is (= "stderr" (.-stderr result)))
    (is (not (string? result)))))

(deftest native-music-decodes-stdout-and-rejects-invalid-engine-metadata
  (let [metadata {:ok true :outputPath "/tmp/proof.wav" :durationSec 0.5
                  :sampleRate 44100 :channels 2 :samples 22050}]
    (is (= metadata (native-music/decode-result!
                     #js {:stdout (.stringify js/JSON (clj->js metadata))
                          :stderr "diagnostic text is not JSON"})))
    (doseq [stdout ["not JSON" "[]" "{}" "{\"ok\":false}"]]
      (is (thrown-with-msg? cljs.core/ExceptionInfo #"stdout violates"
                           (native-music/decode-result! #js {:stdout stdout :stderr ""}))))))

(deftest ^:async native-music-generation-returns-metadata-for-the-rendered-wav
  (let [workspace (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-native-music-test-")))
        output-path "Music/generated/regression.wav"
        spec-json "{\"bpm\":120,\"duration\":0.5,\"tracks\":[{\"instrument\":\"synth\",\"waveform\":\"sine\",\"notes\":[{\"note\":\"A4\",\"time\":0,\"duration\":0.25}]}]}"]
    (try
      (let [result (try
                     (await (#'music/music-generate! nil {:workspace-root workspace}
                                                    spec-json output-path))
                     (catch :default error {:error (.-message error)}))
            wav (await (.readFile fs (.join path workspace output-path)))]
        (testing "the existing native engine really produced audible PCM data"
          (is (= "RIFF" (.toString wav "ascii" 0 4)))
          (is (= "WAVE" (.toString wav "ascii" 8 12)))
          (is (> (.-length wav) 44))
          (is (.some (.subarray wav 44) (fn [byte] (not (zero? byte))))))
        (testing "music.generate decodes stdout, retaining native metadata and workspace paths"
          (is (= true (:ok result)) (str "native process result: " (pr-str result)))
          (is (= 44100 (:sampleRate result)))
          (is (= 2 (:channels result)))
          (is (pos? (or (:samples result) 0)))
          (is (= output-path (:workspace-path result)))
          (is (= (.join path workspace output-path) (:absolute-path result)))))
      (finally
        (await (.rm fs workspace #js {:recursive true :force true}))))))
