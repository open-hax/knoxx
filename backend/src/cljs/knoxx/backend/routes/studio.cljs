(ns knoxx.backend.routes.studio
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.audio-labels :as labels]
            [knoxx.backend.routes.studio.discord-scan :as studio-discord-scan]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; -- Audio file discovery helpers --

(defn- audio-extensions [] #{".mp3" ".wav" ".ogg" ".m4a" ".flac" ".aac" ".opus" ".wma"})

(defn- audio-mime-type [ext]
  (case ext ".mp3" "audio/mpeg" ".wav" "audio/wav" ".ogg" "audio/ogg"
            ".m4a" "audio/mp4" ".flac" "audio/flac" ".aac" "audio/aac"
            ".opus" "audio/opus" "audio/mpeg"))

(declare walk-audio-files!)

(defn- process-entry [node-fs node-path root-dir base-relative depth max-depth entry]
  (let [nm (.-name entry)
        abs (.join node-path root-dir nm)
        rel (if (str/blank? base-relative) nm (str base-relative "/" nm))]
    (cond
      (str/starts-with? nm ".")
      (.resolve js/Promise [])

      (.isDirectory entry)
      (walk-audio-files! node-fs node-path abs rel (inc depth) max-depth)

      :else
      (let [ext (str/lower-case (or (some-> (.extname node-path nm) str/trim) ""))]
        (if (contains? (audio-extensions) ext)
          (-> (.stat node-fs abs)
              (.then (fn [s] [{:name nm :path rel :ext ext :size (.-size s) :modified (-> (.-mtime s) (.getTime)) :mime (audio-mime-type ext)}]))
              (.catch (fn [_] [])))
          (.resolve js/Promise []))))))

(defn- walk-audio-files! [node-fs node-path root-dir base-relative depth max-depth]
  (if (> depth max-depth)
    (.resolve js/Promise [])
    (-> (.readdir node-fs root-dir #js {:withFileTypes true})
        (.then (fn [entries]
                 (let [promises (mapv #(process-entry node-fs node-path root-dir base-relative depth max-depth %) (vec (array-seq entries)))]
                   (-> (.all js/Promise (into-array promises))
                       (.then (fn [r] (vec (mapcat identity r))))))))
        (.catch (fn [_] [])))))

;; -- Routes --

(defroute studio-audio-library! [policy-db]
  "GET" "/api/studio/audio-library"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [subpath (or (aget request "query" "path") ".")
        max-depth (let [d (js/parseInt (or (aget request "query" "depth") "16") 10)]
                    (if (js/isNaN d) 16 (max 0 (min d 64))))
        ;; For root path, use workspace root directly to avoid path validation issues
        is-root (or (= subpath ".") (= subpath "") (= subpath "/"))
        absolute (if is-root
                   (:workspace-root config)
                   (let [normalized (media/normalize-tool-path-arg subpath)
                         {:keys [absolute]} (media/resolve-workspace-media-path runtime config normalized)]
                     absolute))
        base-relative (if is-root "" subpath)]
    (-> (walk-audio-files! fs path absolute base-relative 0 max-depth)
        (.then (fn [files]
                 (let [sorted (vec (sort-by :modified > files))]
                   (json-response! reply 200 {:ok true :root subpath :count (count sorted) :files sorted}))))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Scan failed: " err)}))))))

(defroute studio-state-get! [policy-db]
  "GET" "/api/studio/state"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            kind (or (aget request "query" "kind") "player")]
        (if (and user-id org-id)
          (-> (.query db "SELECT state_json FROM studio_state WHERE user_id = $1 AND org_id = $2 AND kind = $3" #js [user-id org-id kind])
              (.then (fn [res]
                       (let [row (some-> res .-rows (aget 0))]
                         (json-response! reply 200 {:ok true :state (if row (js->clj (.-state_json row) :keywordize-keys true) {})}))))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Load failed: " err)}))))
          (json-response! reply 200 {:ok true :state {}})))
      (json-response! reply 200 {:ok true :state {}}))))

(defroute studio-state-put! [policy-db]
  "PUT" "/api/studio/state"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            body (or (aget request "body") #js {})
            kind (or (aget body "kind") "player")
            state (js->clj (or (aget body "state") #js {}) :keywordize-keys true)]
        (if (and user-id org-id)
          (-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW() RETURNING *" #js [user-id org-id kind (.stringify js/JSON (clj->js state))])
              (.then (fn [_] (json-response! reply 200 {:ok true :saved true})))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Save failed: " err)}))))
          (json-response! reply 400 {:detail "User context required"})))
      (json-response! reply 503 {:detail "Database not configured"}))))

(defroute studio-playlist-get! [policy-db]
  "GET" "/api/studio/playlist"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))]
        (if (and user-id org-id)
          (-> (.query db "SELECT state_json FROM studio_state WHERE user_id=$1 AND org_id=$2 AND kind='playlist'" #js [user-id org-id])
              (.then (fn [res]
                       (let [row (some-> res .-rows (aget 0))
                             state (if row (js->clj (.-state_json row) :keywordize-keys true) {})]
                         (json-response! reply 200 {:ok true :playlist (or (:items state) [])}))))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Load failed: " err)}))))
          (json-response! reply 200 {:ok true :playlist []})))
      (json-response! reply 200 {:ok true :playlist []}))))

(defroute studio-playlist-put! [policy-db]
  "PUT" "/api/studio/playlist"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)]
    (if db
      (let [user-id (or (:user-id ctx) (some-> ctx :user :id))
            org-id (or (:org-id ctx) (some-> ctx :org :id))
            body (or (aget request "body") #js {})
            items (js->clj (or (aget body "items") #js []) :keywordize-keys true)]
        (if (and user-id org-id)
          (-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,'playlist',$3::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW()" #js [user-id org-id (.stringify js/JSON (clj->js {:items items}))])
              (.then (fn [_] (json-response! reply 200 {:ok true :saved true :count (count items)})))
              (.catch (fn [err] (json-response! reply 500 {:detail (str "Save failed: " err)}))))
          (json-response! reply 400 {:detail "User context required"})))
      (json-response! reply 503 {:detail "Database not configured"}))))

(defroute studio-stream! []
  "GET" "/api/studio/stream"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [raw-path (or (aget request "query" "path") "")
        normalized (media/normalize-tool-path-arg raw-path)
        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)
        mime-type (or (media/workspace-media-mime-type relative) "audio/mpeg")]
    (-> (media/fs-stat! fs absolute)
        (.then (fn [stat]
                 (when-not (.isFile stat) (throw (js/Error. (str relative " is not a file"))))
                 (let [total-size (.-size stat)
                       filename (media/path-basename path absolute)
                       safe? (every? (fn [c] (let [n (.charCodeAt c 0)] (and (>= n 32) (<= n 126)))) (str filename))
                       disp (if safe? (str "inline; filename=\"" filename "\"") (str "inline; filename*=UTF-8''" (js/encodeURIComponent filename)))]
                   (.header reply "Content-Type" mime-type)
                   (.header reply "Accept-Ranges" "bytes")
                   (.header reply "Cache-Control" "private, max-age=0")
                   (.header reply "Content-Disposition" disp)
                   (.header reply "Content-Length" (str total-size))
                   (.send reply (.createReadStream node-fs absolute)))))
        (.catch (fn [err] (json-response! reply 404 {:detail (str err)}))))))

(defroute studio-save-m3u! []
  "POST" "/api/studio/save-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        items (js->clj (or (aget body "items") #js []) :keywordize-keys true)
        name (or (aget body "name") (str "playlist-" (.toISOString (js/Date.))))
        ;; Build M3U content
        m3u-lines (concat
                   ["#EXTM3U"]
                   (mapcat (fn [item]
                             [(str "#EXTINF:-1," (:name item))
                              (:path item)])
                           items))
        m3u-content (str/join "\n" m3u-lines)
        ;; Save to Music/playlists/
        normalized (media/normalize-tool-path-arg "Music/playlists")
        {:keys [absolute]} (media/resolve-workspace-media-path runtime config normalized)
        safe-name (str/replace name #"[^a-zA-Z0-9_-]" "_")
        file-path (.join path absolute (str safe-name ".m3u"))]
    (-> (.mkdir fs absolute #js {:recursive true})
        (.then (fn [_] (.writeFile fs file-path m3u-content "utf8")))
        (.then (fn [_] (json-response! reply 200 {:ok true :path (str "Music/playlists/" safe-name ".m3u") :count (count items)})))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed to save playlist: " err)}))))))

(defroute studio-save-m3u-download! []
  "POST" "/api/studio/download-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [body (or (aget request "body") #js {})
        items (js->clj (or (aget body "items") #js []) :keywordize-keys true)
        name (or (aget body "name") "playlist")
        ;; Build M3U content
        m3u-lines (concat
                   ["#EXTM3U"]
                   (mapcat (fn [item]
                             [(str "#EXTINF:-1," (:name item))
                              (:path item)])
                           items))
        m3u-content (str/join "\n" m3u-lines)
        safe-name (str/replace name #"[^a-zA-Z0-9_-]" "_")]
    (.header reply "Content-Type" "audio/x-mpegurl")
    (.header reply "Content-Disposition" (str "attachment; filename=\"" safe-name ".m3u\""))
    (.send reply m3u-content)))

(defroute studio-load-m3u! []
  "GET" "/api/studio/load-m3u"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [file-path (aget request "query" "path")]
    (if-not file-path
      (json-response! reply 400 {:detail "Missing path parameter"})
      (-> (.readFile fs file-path "utf8")
          (.then (fn [content]
                   (let [lines (str/split-lines content)
                         ;; Parse M3U format
                         items (loop [remaining lines
                                      result []
                                      current-name nil]
                                 (if (empty? remaining)
                                   result
                                   (let [line (str/trim (first remaining))
                                         rest-lines (rest remaining)]
                                     (cond
                                       ;; Skip empty lines and comments
                                       (or (str/blank? line)
                                           (str/starts-with? line "#EXTM3U"))
                                       (recur rest-lines result current-name)
                                       ;; Parse EXTINF
                                       (str/starts-with? line "#EXTINF:")
                                       (let [name-part (second (str/split line #"," 2))]
                                         (recur rest-lines result (or name-part "Unknown")))
                                       ;; This is a file path
                                       :else
                                       (recur rest-lines
                                              (conj result {:path line
                                                            :name (or current-name
                                                                      (.basename path line))})
                                              nil)))))
                         playlist-name (.basename path file-path)
                         clean-name (str/replace playlist-name #"\.m3u$" "")]
                     (json-response! reply 200 {:ok true :name clean-name :items (vec items)}))))
          (.catch (fn [err]
                    (json-response! reply 500 {:detail (str "Failed to load M3U: " err)})))))))

(defroute studio-list-playlists! []
  "GET" "/api/studio/playlists"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)
        playlists-dir (.join path workspace-root "Music" "playlists")]
    (-> (.readdir fs playlists-dir)
        (.then (fn [files]
                 (let [m3u-files (->> (js->clj files)
                                      (filter #(str/ends-with? % ".m3u"))
                                      (map (fn [filename]
                                             {:name (str/replace filename #"\.m3u$" "")
                                              :path (.join path playlists-dir filename)
                                              :filename filename})))]
                   (json-response! reply 200 {:ok true :playlists (vec m3u-files)}))))
        (.catch (fn [_err]
                  ;; Directory doesn't exist or other error - return empty list
                  (json-response! reply 200 {:ok true :playlists []}))))))

;; -- Label routes --

(defroute studio-labels-get! []
  "GET" "/api/studio/labels"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)
        file-path (aget request "query" "path")
        all? (aget request "query" "all")]
    (cond
      all?
      (-> (labels/get-all-labels fs workspace-root)
          (.then (fn [all-labels] (json-response! reply 200 {:ok true :labels all-labels})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      file-path
      (-> (labels/get-labels fs workspace-root file-path)
          (.then (fn [file-labels] (json-response! reply 200 {:ok true :path file-path :labels file-labels})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      :else
      (json-response! reply 400 {:detail "Missing path or all parameter"}))))

(defroute studio-labels-add! []
  "POST" "/api/studio/labels/add"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)
        body (or (aget request "body") #js {})
        file-path (aget body "path")
        label (aget body "label")]
    (if (and file-path label)
      (-> (labels/add-label! fs workspace-root file-path label)
          (.then (fn [updated] (json-response! reply 200 {:ok true :path file-path :labels updated})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or label"}))))

(defroute studio-labels-remove! []
  "POST" "/api/studio/labels/remove"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)
        body (or (aget request "body") #js {})
        file-path (aget body "path")
        label (aget body "label")]
    (if (and file-path label)
      (-> (labels/remove-label! fs workspace-root file-path label)
          (.then (fn [updated] (json-response! reply 200 {:ok true :path file-path :labels updated})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or label"}))))

(defroute studio-labels-by-label! []
  "GET" "/api/studio/labels/by-label"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)
        label (aget request "query" "label")]
    (if label
      (-> (labels/get-files-by-label fs workspace-root label)
          (.then (fn [files] (json-response! reply 200 {:ok true :label label :files files})))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing label parameter"}))))

(defroute studio-sync-symlinks! []
  "POST" "/api/studio/sync-symlinks"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [workspace-root (:workspace-root config)]
    (-> (labels/sync-symlinks! fs path workspace-root)
        (.then (fn [count] (json-response! reply 200 {:ok true :symlinks count})))
        (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed to sync symlinks: " err)}))))))

;; -- Audio asset routes (waveform/spectrogram) --

(defroute studio-audio-asset-get! [policy-db]
  "GET" "/api/studio/audio-asset"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)
        audio-path (aget request "query" "path")
        asset-type (aget request "query" "type")]
    (if (and audio-path asset-type)
      (-> (.query db "SELECT image_data, mime_type, width, height FROM studio_audio_assets WHERE audio_path = $1 AND asset_type = $2" #js [audio-path asset-type])
          (.then (fn [res]
                   (if (.-rows res)
                     (let [row (first (js->clj (.-rows res)))]
                       (if row
                         (do
                           (.header reply "Content-Type" (get row "mime_type" "image/png"))
                           (.header reply "Cache-Control" "public, max-age=86400")
                           (.send reply (aget row "image_data")))
                         (json-response! reply 404 {:detail "Asset not found"})))
                     (json-response! reply 404 {:detail "Asset not found"}))))
          (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)}))))
      (json-response! reply 400 {:detail "Missing path or type"}))))

(defroute studio-audio-asset-save! [policy-db]
  "POST" "/api/studio/audio-asset"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [db (policy-db runtime)
        body (or (aget request "body") #js {})
        audio-path (aget body "path")
        asset-type (aget body "type")
        image-data (aget body "imageData")
        mime-type (or (aget body "mimeType") "image/png")
        width (aget body "width")
        height (aget body "height")]
    (if (and audio-path asset-type image-data)
      (let [buffer (js/Buffer.from image-data "base64")]
        (-> (.query db "INSERT INTO studio_audio_assets (audio_path, asset_type, image_data, mime_type, width, height) VALUES ($1, $2, $3, $4, $5, $6) ON CONFLICT (audio_path, asset_type) DO UPDATE SET image_data = $3, mime_type = $4, width = $5, height = $6, created_at = NOW()"
                    #js [audio-path asset-type buffer mime-type width height])
            (.then (fn [_] (json-response! reply 200 {:ok true :path audio-path :type asset-type})))
            (.catch (fn [err] (json-response! reply 500 {:detail (str "Failed: " err)})))))
      (json-response! reply 400 {:detail "Missing path, type, or imageData"}))))


(defn register-studio-routes! [app runtime config deps]
  (studio-audio-library! app runtime config deps)
  (studio-state-get! app runtime config deps)
  (studio-state-put! app runtime config deps)
  (studio-playlist-get! app runtime config deps)
  (studio-playlist-put! app runtime config deps)
  (studio-stream! app runtime config deps)
  (studio-save-m3u! app runtime config deps)
  (studio-save-m3u-download! app runtime config deps)
  (studio-load-m3u! app runtime config deps)
  (studio-list-playlists! app runtime config deps)
  (studio-labels-get! app runtime config deps)
  (studio-labels-add! app runtime config deps)
  (studio-labels-remove! app runtime config deps)
  (studio-labels-by-label! app runtime config deps)
  (studio-sync-symlinks! app runtime config deps)
  (studio-audio-asset-get! app runtime config deps)
  (studio-audio-asset-save! app runtime config deps)
  (studio-discord-scan/studio-discord-audio-scan! app runtime config deps)
  (studio-discord-scan/studio-discord-image-scan! app runtime config deps))
