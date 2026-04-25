(ns hooks.promise-chain
  (:require [clj-kondo.hooks-api :as api]))

;; file-state: {filename -> {:max-row int :loc meta :reported? bool}}
(def ^:private file-state (atom {}))

(defn- update-file-max! [filename end-row loc]
  (swap! file-state
         (fn [m]
           (let [cur  (get m filename {:max-row 0 :reported? false})
                 nmax (max (:max-row cur) (or end-row 0))]
             (assoc m filename (assoc cur :max-row nmax :loc loc))))))

(defn- emit-file-finding! [filename]
  (let [st (get @file-state filename)]
    (when (and st (not (:reported? st)))
      (swap! file-state assoc-in [filename :reported?] true)
      (let [n (:max-row st) loc (:loc st)]
        (cond
          (>= n 800) (api/reg-finding!
                      (assoc loc
                             :message (str "file ~" n " lines (error >=800)")
                             :type :file-length/too-long :level :error))
          (>= n 400) (api/reg-finding!
                      (assoc loc
                             :message (str "file ~" n " lines (warning >=400)")
                             :type :file-length/long :level :warning)))))))

(defn- dot-method? [node]
  (when (api/list-node? node)
    (when-let [op (first (:children node))]
      (when (api/token-node? op)
        (let [s (str (api/sexpr op))]
          (or (= s ".then") (= s ".catch")))))))

(defn- walk! [node]
  (when (api/list-node? node)
    (let [ch (:children node) op (first ch)]
      (when-not (and op (api/token-node? op) (= 'quote (api/sexpr op)))
        (when (dot-method? node)
          (api/reg-finding!
           (assoc (meta node)
                  :message (str (api/sexpr op) " — raw Promise chain; use js-await")
                  :type :promise-chain/prefer-js-await :level :warning)))
        (doseq [c ch] (walk! c))))))

(defn check [{:keys [node]}]
  (doseq [c (:children node)] (walk! c)))

(defn- check-fn-length [node]
  (let [loc  (meta node)
        nm   (when (> (count (:children node)) 1)
               (api/sexpr (second (:children node))))
        span (- (or (:end-row loc) 0) (or (:row loc) 0))]
    (update-file-max! (or (:filename loc) "") (or (:end-row loc) 0) loc)
    (cond
      (>= span 60) (api/reg-finding!
                    (assoc loc
                           :message (str nm " spans " span " lines (error >=60)")
                           :type :fn-length/too-long :level :error))
      (>= span 30) (api/reg-finding!
                    (assoc loc
                           :message (str nm " spans " span " lines (warning >=30)")
                           :type :fn-length/long :level :warning)))))

(def ^:private branch-ops
  '#{if if-let if-some when when-let when-some cond condp case
     and or loop recur try catch doseq for})

(defn- node-score [node]
  (if (api/list-node? node)
    (let [ch   (:children node)
          op   (when (seq ch) (api/sexpr (first ch)))
          self (if (and (symbol? op) (contains? branch-ops op)) 1 0)]
      (+ self (reduce + (map node-score (rest ch)))))
    0))

(defn- check-complexity [node]
  (let [ch      (:children node)
        nm      (when (> (count ch) 1) (api/sexpr (second ch)))
        score   (reduce + (map node-score (drop 1 ch)))
        loc     (meta node)]
    (cond
      (>= score 30) (api/reg-finding!
                     (assoc loc
                            :message (str nm " complexity " score " >=30")
                            :type :complexity/too-complex :level :error))
      (>= score 15) (api/reg-finding!
                     (assoc loc
                            :message (str nm " complexity " score " >=15")
                            :type :complexity/high :level :warning)))))

(defn check-ns [{:keys [node]}]
  (let [loc      (meta node)
        filename (or (:filename loc) "")]
    (doseq [[f _] @file-state] (emit-file-finding! f))
    (when-not (get @file-state filename)
      (swap! file-state assoc filename
             {:max-row (or (:end-row loc) 0)
              :loc      loc
              :reported? false}))))

(defn check-defn [{:keys [node]}]
  (doseq [c (:children node)] (walk! c))
  (check-fn-length node)
  (check-complexity node))