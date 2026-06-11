(ns knoxx.backend.infra.stores.mongo-policy-studio
  "Mongo-backed studio storage — slice 6 of the PG policy DB migration
   (kanban 14-04): the studio_state and studio_audio_assets tables.

   Two collections in one namespace (both are studio-domain tables):

   1. knoxx_studio_state — player/playlist state per user+org+kind.
      Unique on (user_id, org_id, kind). Upsert on conflict.

   2. knoxx_studio_audio_assets — waveform/spectrogram binary images.
      Unique on (audio_path, asset_type). Upsert on conflict.

   Row-shape adapter: studio_state documents store state_json as a nested
   map (not a stringified JSON blob) for Mongo-native access. The get
   functions return the state as a CLJS map. Audio assets store image_data
   as a Mongo Binary (Buffer).

   DISPATCH SEAM: these functions replace the raw SQL in
   infra.routes.studio. Future flag-dispatch must route at the query
   seam inside the studio route handlers.

   Documents are stamped with :system_instance_id like other policy twins."
  (:require
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def STUDIO_STATE_COLLECTION "knoxx_studio_state")
(def STUDIO_AUDIO_ASSETS_COLLECTION "knoxx_studio_audio_assets")

(defn- state-coll [db] (.collection db STUDIO_STATE_COLLECTION))
(defn- assets-coll [db] (.collection db STUDIO_AUDIO_ASSETS_COLLECTION))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

;; ---------------------------------------------------------------------------
;; Studio state
;; ---------------------------------------------------------------------------

(defn ^:async setup-state-indexes!
  "Create studio-state indexes. Idempotent.
   Unique on (user_id, org_id, kind) mirrors PG's UNIQUE constraint."
  [db]
  (let [coll (state-coll db)]
    (await (.createIndex coll #js {"user_id" 1 "org_id" 1 "kind" 1}
                         #js {"unique" true}))
    true))

(defn ^:async get-studio-state!
  "Return the state_json map for a user+org+kind, or nil."
  ([user-id org-id kind]
   (get-studio-state! (mongo-client/get-db) user-id org-id kind))
  ([db user-id org-id kind]
   (let [coll (state-coll db)
         doc (keywordize (await (.findOne coll #js {"user_id"  (str user-id)
                                                    "org_id"   (str org-id)
                                                    "kind"     (str kind)})))]
     (:state_json doc))))

(defn ^:async put-studio-state!
  "Upsert studio state. Returns true on success.
   Mirrors PG's ON CONFLICT (user_id,org_id,kind) DO UPDATE."
  ([user-id org-id kind state]
   (put-studio-state! (mongo-client/get-db) user-id org-id kind state))
  ([db user-id org-id kind state]
   (let [coll (state-coll db)
         now (js/Date.)
         state-js (if (string? state)
                    (js/JSON.parse state)
                    (clj->js (or state {})))]
     (await (.updateOne coll
                        #js {"user_id" (str user-id)
                             "org_id"  (str org-id)
                             "kind"    (str kind)}
                        #js {"$set" (clj->js {:state_json (js->clj state-js :keywordize-keys true)
                                              :updated_at now})
                             "$setOnInsert" (clj->js {:user_id          (str user-id)
                                                     :org_id           (str org-id)
                                                     :kind             (str kind)
                                                     :created_at       now
                                                     :system_instance_id (system-instance/current-id)})}
                        #js {"upsert" true}))
     true)))

(defn ^:async get-studio-playlist!
  "Return the playlist items vector for a user+org, or [].
   Reads kind='playlist' and extracts :items from state_json."
  ([user-id org-id]
   (get-studio-playlist! (mongo-client/get-db) user-id org-id))
  ([db user-id org-id]
   (let [state (await (get-studio-state! db user-id org-id "playlist"))]
     (or (:items state) []))))

(defn ^:async put-studio-playlist!
  "Upsert the playlist state. Returns true on success."
  ([user-id org-id items]
   (put-studio-state! user-id org-id "playlist" {:items (or items [])}))
  ([db user-id org-id items]
   (put-studio-state! db user-id org-id "playlist" {:items (or items [])})))

;; ---------------------------------------------------------------------------
;; Studio audio assets
;; ---------------------------------------------------------------------------

(defn ^:async setup-assets-indexes!
  "Create audio-asset indexes. Idempotent.
   Unique on (audio_path, asset_type) mirrors PG's UNIQUE constraint."
  [db]
  (let [coll (assets-coll db)]
    (await (.createIndex coll #js {"audio_path" 1 "asset_type" 1}
                         #js {"unique" true}))
    true))

(defn ^:async get-audio-asset!
  "Return {:image-data :mime-type :width :height} for an audio asset, or nil.
   image_data is returned as a Node Buffer."
  ([audio-path asset-type]
   (get-audio-asset! (mongo-client/get-db) audio-path asset-type))
  ([db audio-path asset-type]
   (let [coll (assets-coll db)
         doc (keywordize (await (.findOne coll #js {"audio_path"  (str audio-path)
                                                    "asset_type"  (str asset-type)})))]
     (when doc
       {:image-data (:image_data doc)
        :mime-type  (:mime_type doc)
        :width      (:width doc)
        :height     (:height doc)}))))

(defn ^:async save-audio-asset!
  "Upsert an audio asset. image-data should be a Node Buffer or base64 string.
   Returns true on success.
   Mirrors PG's ON CONFLICT (audio_path, asset_type) DO UPDATE."
  ([audio-path asset-type image-data mime-type width height]
   (save-audio-asset! (mongo-client/get-db) audio-path asset-type image-data mime-type width height))
  ([db audio-path asset-type image-data mime-type width height]
   (let [coll (assets-coll db)
         now (js/Date.)
         buffer (if (string? image-data)
                  (js/Buffer.from image-data "base64")
                  image-data)]
     (await (.updateOne coll
                        #js {"audio_path" (str audio-path)
                             "asset_type" (str asset-type)}
                        #js {"$set" (clj->js {:image_data buffer
                                              :mime_type  (or mime-type "image/png")
                                              :width      width
                                              :height     height
                                              :updated_at now})
                             "$setOnInsert" (clj->js {:audio_path       (str audio-path)
                                                     :asset_type       (str asset-type)
                                                     :created_at       now
                                                     :system_instance_id (system-instance/current-id)})}
                        #js {"upsert" true}))
     true)))
