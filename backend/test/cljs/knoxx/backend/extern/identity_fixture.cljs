(ns knoxx.backend.extern.identity-fixture
  "Native request and disposable filesystem fixtures for actual identity composition."
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(defn directory!
  "Create one isolated identity integration directory."
  [] (fs/mkdtempSync (path/join (os/tmpdir) "knoxx-identity-")))

(defn remove!
  "Remove only the fixture's owned directory and children."
  [directory] (fs/rmSync directory #js {:recursive true :force true}))

(defn request
  "Make a native request with explicit cookie, headers and method."
  ([token] (request token {} "GET"))
  ([token headers method]
   (clj->js {:method method :headers headers :socket {:remoteAddress "127.0.0.1"}
             :cookies (if token {:axxium_session token} {})})))
