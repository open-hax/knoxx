(ns big-pi.optimizely.main
  (:require [big-pi.optimizely.extern.server :as server]))

(defn -main [& args]
  (server/start args))
