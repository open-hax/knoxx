(ns knoxx.backend.extern.mongo-invite-fixture
  "Read and seed only the disposable native Mongo invitation proof database.")

(defn- collection [db] (.collection db "knoxx_invites"))

(defn ^:async seed!
  "Seed one pending invite with either legacy BSON Date or portable ISO expiry."
  [db code expiry]
  (await (.insertOne (collection db)
                    (clj->js {:invite_id code :code code :email "alice@example.test" :org_id "org-a"
                              :status "pending" :role_slugs ["basic-user"] :expires_at expiry
                              :created_at "2026-09-20T00:00:00.000Z"}))))

(defn native-expiry "Return a real BSON-compatible Date for the legacy storage path." []
  (js/Date. "2099-01-01T00:00:00.000Z"))

(defn ^:async row
  "Inspect this test's invite state without going through the production admission path."
  [db code]
  (js->clj (await (.findOne (collection db) #js {:code code})) :keywordize-keys true))
