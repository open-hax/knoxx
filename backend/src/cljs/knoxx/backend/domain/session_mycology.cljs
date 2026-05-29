(ns knoxx.backend.domain.session-mycology
  "Per-turn retrospection with p-scores and skill-spore incubation.

   Knoxx-native port of the eta-mu extension session-mycology."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj]]
            ["node:fs" :as fs]
            ["node:fs/promises" :as fs-promises]
            ["node:os" :as os]
            ["node:path" :as path]))

(def ^:const SPORE-THRESHOLD 0.72)
(def ^:const PROMOTION-MIN-RECURRENCE
  (js/Math.max 2 (js/Number (or (aget js/process.env "KNOXX_MYCOLOGY_PROMOTION_MIN_RECURRENCE") 2))))
(def ^:const PROMOTION-HINT-P
  (let [v (js/Number (or (aget js/process.env "KNOXX_MYCOLOGY_PROMOTION_HINT_P") 0.84))]
    (js/Math.max 0 (js/Math.min 1 (if (js/Number.isFinite v) v 0.84)))))

(def session-mycology-params
  [:map
   [:action {:description "Action: reflect to record a retrospective, or list_recent to inspect recent spores."} :string]
   [:efficiencyP {:optional true :description "Confidence 0..1 that the chosen path was near-minimal."} [:double {:min 0 :max 1}]]
   [:frictionP {:optional true :description "Confidence 0..1 that the work was harder than it should have been."} [:double {:min 0 :max 1}]]
   [:skillCandidateP {:optional true :description "Confidence 0..1 that a reusable skill or protocol would compress future effort."} [:double {:min 0 :max 1}]]
   [:lesson {:optional true :description "Short lesson from the turn."} :string]
   [:betterPath {:optional true :description "Better path to try next time."} :string]
   [:candidateName {:optional true :description "Candidate skill name if a spore should be incubated."} :string]
   [:candidateDescription {:optional true :description "One sentence describing the candidate skill."} :string]
   [:reuseScope {:optional true :description "Optional reuse scope: turn, session, or multi-session."} :string]])

(defn- make-state-dir-fn [node-os node-path]
  (fn [] (node-path.join (.homedir node-os) ".knoxx" "state" "session-mycology")))
(defn- make-reflections-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "turn-reflections.jsonl")))
(defn- make-spores-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "skill-spores.jsonl")))
(defn- make-promotions-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "skill-promotions.jsonl")))
(defn- make-spore-drafts-dir-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "spores")))

(defn- now-iso [] (.toISOString (js/Date.)))
(defn- clamp-01 [value fallback]
  (let [n (js/Number value)]
    (if (js/Number.isFinite n)
      (js/Math.max 0 (js/Math.min 1 n))
      fallback)))
(defn- slugify [value]
  (let [s (-> (str (or value "")) str/lower-case (str/replace #"[^a-z0-9]+" "-") (str/replace #"^-+|-$" "") (str/replace #"--+" "-"))]
    (if (str/blank? s) "skill-spore" s)))
(defn- yaml-quote [value] (js/JSON.stringify (str (or value ""))))
(defn- same-cwd [node-path a b] (and a b (= (node-path.resolve a) (node-path.resolve b))))
(defn- normalize-reuse-scope [value]
  (let [v (-> (str (or value "session")) str/trim str/lower-case)]
    (if ((set ["turn" "session" "multi-session"]) v) v "session")))
(defn- reflection-kind [reflection]
  (if-not reflection
    "none"
    (let [skill-p (clamp-01 (aget reflection "skillCandidateP") 0)
          fric-p (clamp-01 (aget reflection "frictionP") 0)
          eff-p (clamp-01 (aget reflection "efficiencyP") 0)]
      (cond
        (>= skill-p SPORE-THRESHOLD) "sporeworthy"
        (>= fric-p 0.68) "gnarly"
        (and (>= eff-p 0.75) (<= fric-p 0.35)) "smooth"
        :else "mixed"))))

;; Async I/O helpers to avoid blocking the event loop during agent turns.
(defn- make-append-jsonl-fn [node-path]
  (fn [file-path value]
    (-> (fs-promises/mkdir (node-path.dirname file-path) #js {:recursive true})
        (.then (fn [] (fs-promises/appendFile file-path (str (js/JSON.stringify value) "\n") "utf8"))))))

(defn- make-read-jsonl-fn []
  (fn [file-path limit]
    (-> (fs-promises/readFile file-path "utf8")
        (.then (fn [text]
                 (let [lines (-> text (str/split #"\r?\n") (->> (filter identity)) (js/Array.prototype.slice.call (- limit)))]
                   (js/Array.from
                    (-> lines
                        (.map (fn [line] (try (js/JSON.parse line) (catch js/Error _ nil))))
                        (.filter (fn [x] x)))))))
        (.catch (fn [_] #js [])))))

(defn- make-load-recent-spores-fn [read-jsonl-fn spores-file-fn node-path]
  (fn [cwd limit]
    (-> (read-jsonl-fn (spores-file-fn) 400)
        (.then (fn [rows]
                 (let [filtered (.filter rows (fn [row] (or (not cwd) (same-cwd node-path (aget row "cwd") cwd))))]
                   (-> (.slice filtered (- (.-length filtered) limit)) (.reverse) (js/Array.from))))))))

(defn- make-find-latest-spore-fn [read-jsonl-fn spores-file-fn node-path]
  (fn [slug cwd]
    (-> (read-jsonl-fn (spores-file-fn) 400)
        (.then (fn [rows]
                 (let [filtered (.filter rows (fn [row] (and (= (aget row "slug") slug)
                                                             (or (not cwd) (same-cwd node-path (aget row "cwd") cwd)))))]
                   (or (.at filtered -1) nil)))))))

(defn- summarize-spores [spores]
  (if (zero? (.-length spores))
    "- none yet"
    (.join (.map spores
                 (fn [spore]
                   (str "- " (aget spore "name")
                        " (recurrence " (aget spore "recurrence")
                        ", p_skill " (.toFixed (clamp-01 (aget spore "skillCandidateP") 0) 2) ")"
                        ": " (aget spore "description"))))
           "\n")))

(defn- build-spore-skill-draft [spore]
  (str "---\n"
       "name: " (aget spore "slug") "\n"
       "description: " (yaml-quote (aget spore "description")) "\n"
       "disable-model-invocation: true\n"
       "metadata:\n  origin: session-mycology-spore\n  recurrence: " (aget spore "recurrence") "\n---\n\n"
       "# " (aget spore "name") "\n\n## Goal\n" (aget spore "description") "\n\n"
       "## Use This Skill When\n- The same friction pattern recurs.\n\n"
       "## Do Not Use This Skill When\n- The pain was only a one-off environment glitch.\n"))

(defn- build-spore-contract-draft [spore]
  (str "(skill-contract\n  (name " (yaml-quote (aget spore "slug")) ")\n"
       "  (v \"knoxx.skill/" (aget spore "slug") "@0.0.1-spore\")\n"
       "  (intent " (yaml-quote (aget spore "description")) ")\n\n"
       "  (activation\n    (priority 35)\n    (explicit [\"skill:" (aget spore "slug") "\"])\n"
       "    (triggers [" (yaml-quote (.toLowerCase (aget spore "name"))) "]))\n\n"
       "  (governance\n    (touch-layer :mutable)\n    (non-override [:mission :directives :safety :license :output-shape])\n    (requires-user-approval false))\n)"))

(defn- promotion-eligible? [spore]
  (if-not spore
    false
    (let [reuse-scope (str (or (aget spore "reuseScope") "session"))
          recurrence (js/Number (or (aget spore "recurrence") 0))
          skill-p (clamp-01 (aget spore "skillCandidateP") 0)]
      (cond
        (and (= reuse-scope "turn") (< recurrence (inc PROMOTION-MIN-RECURRENCE))) false
        (>= recurrence PROMOTION-MIN-RECURRENCE) true
        (>= skill-p PROMOTION-HINT-P) true
        :else false))))

(defn- build-live-skill [spore]
  (str "---\nname: " (aget spore "slug") "\ndescription: " (yaml-quote (aget spore "description")) "\nlicense: GPL-3.0\nmetadata:\n"
       "  origin: session-mycology-promotion\n  promoted-from-spore: " (aget spore "slug") "\n  recurrence: " (aget spore "recurrence") "\n---\n\n"
       "# Skill: " (aget spore "name") "\n\n## Goal\n" (aget spore "description") "\n"))

(defn- build-live-contract [spore]
  (str "(skill-contract\n  (name " (yaml-quote (aget spore "slug")) ")\n"
       "  (v \"knoxx.skill/" (aget spore "slug") "@0.1.0\")\n\n"
       "  (intent " (yaml-quote (aget spore "description")) ")\n\n"
       "  (activation\n    (explicit [\"skill:" (aget spore "slug") "\"])\n"
       "    (triggers [" (yaml-quote (.toLowerCase (aget spore "name"))) " " (yaml-quote (str/replace (aget spore "slug") #"-" " ")) "]))\n\n"
       "  (governance\n    (touch-layer :mutable)\n    (non-override [:mission :directives :safety :license :output-shape])\n    (requires-user-approval false))\n)\n"))

(defn- make-promote-spore-to-skill-fn [node-path node-os promotions-file-fn]
  (fn [spore]
    (if-not (promotion-eligible? spore)
      (js/Promise.resolve #js {:promoted false :eligible false})
      (let [home (.homedir node-os)
            dir (node-path.join home ".knoxx" "skills" (aget spore "slug"))
            skill-path (node-path.join dir "SKILL.md")
            contract-path (node-path.join dir "CONTRACT.edn")
            append-jsonl-fn (make-append-jsonl-fn node-path)]
        (-> (fs-promises/mkdir dir #js {:recursive true})
            (.then (fn [] (fs-promises/access skill-path)))
            (.then (fn [] false)
                   (fn [_] (-> (fs-promises/writeFile skill-path (build-live-skill spore) "utf8")
                               (.then (fn [] true)))))
            (.then (fn [created-skill]
                     (-> (fs-promises/access contract-path)
                         (.then (fn [] false)
                                (fn [_] (-> (fs-promises/writeFile contract-path (build-live-contract spore) "utf8")
                                            (.then (fn [] true)))))
                         (.then (fn [created-contract]
                                  (when (or created-skill created-contract)
                                    (append-jsonl-fn
                                     (promotions-file-fn)
                                     #js {:ts (now-iso) :slug (aget spore "slug") :name (aget spore "name")
                                          :recurrence (aget spore "recurrence") :skillCandidateP (aget spore "skillCandidateP")
                                          :cwd (aget spore "cwd") :sessionFile (aget spore "sessionFile")
                                          :skillPath skill-path :contractPath contract-path
                                          :createdSkill created-skill :createdContract created-contract}))
                                  (js/Promise.resolve
                                   #js {:promoted (or created-skill created-contract) :eligible true
                                        :skillPath skill-path :contractPath contract-path
                                         :createdSkill created-skill :createdContract created-contract})))))))))))

(defn- make-write-spore-draft-fn [node-path node-os spore-drafts-dir-fn]
  (fn [reflection spore]
    (let [home (.homedir node-os)
          file-path (node-path.join (spore-drafts-dir-fn) (str (aget spore "slug") ".md"))
          skill-draft (build-spore-skill-draft spore)
          contract-draft (build-spore-contract-draft spore)
          content (str "# Skill Spore: " (aget spore "name") "\n\n"
                       "- Generated: " (aget spore "ts") "\n"
                       "- Recurrence: " (aget spore "recurrence") "\n"
                       "- CWD: " (aget spore "cwd") "\n"
                       "- Reuse scope: " (aget spore "reuseScope") "\n"
                       "- Reflection kind: " (aget spore "reflectionKind") "\n"
                       "- p-efficiency: " (.toFixed (clamp-01 (aget reflection "efficiencyP") 0) 2) "\n"
                       "- p-friction: " (.toFixed (clamp-01 (aget reflection "frictionP") 0) 2) "\n"
                       "- p-skill-candidate: " (.toFixed (clamp-01 (aget reflection "skillCandidateP") 0) 2) "\n\n"
                       "## Lesson\n" (or (aget reflection "lesson") "_none captured_") "\n\n"
                       "## Better path next time\n" (or (aget reflection "betterPath") "_none captured_") "\n\n"
                       "## Candidate description\n" (aget spore "description") "\n\n"
                       "## Promotion gate\nPromote this spore into a live skill after either:\n"
                       "- recurrence >= " PROMOTION-MIN-RECURRENCE "\n- explicit user request\n- or strong evidence that the pattern generalizes beyond the current task\n\n"
                       "## Draft SKILL.md\n\n~~~markdown\n" skill-draft "~~~\n\n"
                       "## Draft CONTRACT.edn\n\n~~~edn\n" contract-draft "~~~\n\n"
                       "## Suggested live-skill path\n\n- " (node-path.join home ".knoxx" "skills" (aget spore "slug") "SKILL.md")
                       "\n- " (node-path.join home ".knoxx" "skills" (aget spore "slug") "CONTRACT.edn") "\n")]
      (-> (fs-promises/mkdir (node-path.dirname file-path) #js {:recursive true})
          (.then (fn [] (fs-promises/writeFile file-path content "utf8")))
          (.then (fn [] file-path))))))

(defn- execute-reflect-action
  [params cwd session-file model-label node-path append-jsonl-fn reflections-file-fn
   find-latest-spore-fn spore-drafts-dir-fn write-spore-draft-fn
   spores-file-fn promote-spore-to-skill-fn]
  (let [reflection #js {:ts (now-iso) :cwd cwd :sessionFile session-file :model model-label
                        :efficiencyP (clamp-01 (aget params "efficiencyP") 0.5)
                        :frictionP (clamp-01 (aget params "frictionP") 0.5)
                        :skillCandidateP (clamp-01 (aget params "skillCandidateP") 0.5)
                        :lesson (.trim (str (or (aget params "lesson") "")))
                        :betterPath (.trim (str (or (aget params "betterPath") "")))}]
    (-> (append-jsonl-fn (reflections-file-fn) reflection)
        (.then (fn []
                 (let [name (.trim (str (or (aget params "candidateName") "")))
                       description (.trim (str (or (aget params "candidateDescription") "")))
                       should-incubate (and (pos? (.-length name))
                                            (pos? (.-length description))
                                            (or (>= (aget reflection "skillCandidateP") SPORE-THRESHOLD)
                                                (>= (aget reflection "frictionP") 0.68)))]
                   (if-not should-incubate
                     (js/Promise.resolve
                      (tool-text-result
                       (str "Recorded reflection (p_eff=" (.toFixed (aget reflection "efficiencyP") 2)
                            ", p_fric=" (.toFixed (aget reflection "frictionP") 2)
                            ", p_skill=" (.toFixed (aget reflection "skillCandidateP") 2)
                            "). No spore incubated.")
                       #js {:reflection reflection}))
                     (let [slug (slugify name)]
                       (-> (find-latest-spore-fn slug cwd)
                           (.then (fn [prior]
                                    (let [prior-recurrence (js/Number (or (when prior (aget prior "recurrence")) 0))
                                          prior-draft-path (when prior (aget prior "draftPath"))
                                          spore #js {:ts (now-iso) :name name :slug slug :description description
                                                     :reuseScope (normalize-reuse-scope (aget params "reuseScope"))
                                                     :cwd cwd :sessionFile session-file :model model-label
                                                     :reflectionTs (aget reflection "ts")
                                                     :reflectionKind (reflection-kind reflection)
                                                     :recurrence (js/Math.max 1 (inc prior-recurrence))
                                                     :efficiencyP (aget reflection "efficiencyP")
                                                     :frictionP (aget reflection "frictionP")
                                                     :skillCandidateP (aget reflection "skillCandidateP")}]
                                      (aset spore "draftPath" (or prior-draft-path
                                                                  (node-path.join (spore-drafts-dir-fn)
                                                                                  (str slug ".md"))))
                                      (-> (write-spore-draft-fn reflection spore)
                                          (.then (fn [] (append-jsonl-fn (spores-file-fn) spore)))
                                          (.then (fn [] (promote-spore-to-skill-fn spore)))
                                          (.then (fn [promotion]
                                                   (tool-text-result
                                                    (str "Recorded reflection (p_eff="
                                                         (.toFixed (aget reflection "efficiencyP") 2)
                                                         ", p_fric=" (.toFixed (aget reflection "frictionP") 2)
                                                         ", p_skill=" (.toFixed (aget reflection "skillCandidateP") 2)
                                                         "). Incubated spore: " name " -> "
                                                         (aget spore "draftPath")
                                                         (when (aget promotion "promoted")
                                                           (str " | promoted live skill: "
                                                                (aget promotion "skillPath"))))
                                                    #js {:reflection reflection :spore spore :promotion promotion}))))))))))))))))

(defn- make-execute-fn [node-path node-os]
  (let [state-dir-fn (make-state-dir-fn node-os node-path)
        reflections-file-fn (make-reflections-file-fn state-dir-fn node-path)
        spores-file-fn (make-spores-file-fn state-dir-fn node-path)
        promotions-file-fn (make-promotions-file-fn state-dir-fn node-path)
        spore-drafts-dir-fn (make-spore-drafts-dir-fn state-dir-fn node-path)
        append-jsonl-fn (make-append-jsonl-fn node-path)
        read-jsonl-fn (make-read-jsonl-fn)
        load-recent-spores-fn (make-load-recent-spores-fn read-jsonl-fn spores-file-fn node-path)
        find-latest-spore-fn (make-find-latest-spore-fn read-jsonl-fn spores-file-fn node-path)
        promote-spore-to-skill-fn (make-promote-spore-to-skill-fn node-path node-os promotions-file-fn)
        write-spore-draft-fn (make-write-spore-draft-fn node-path node-os spore-drafts-dir-fn)]
    (fn [_tcid params _sig _on-update ctx]
      (let [action (.toLowerCase (.trim (str (or (aget params "action") "reflect"))))
            cwd (or (aget ctx "cwd") (.. js/process cwd))
            session-file (or (aget ctx "sessionFile") "")
            model-label (or (aget ctx "modelLabel") "unknown")]
        (cond
          (= action "list_recent")
          (-> (load-recent-spores-fn cwd 5)
              (.then (fn [spores]
                       (tool-text-result (if (pos? (.-length spores)) (summarize-spores spores) "- none yet")
                                         #js {:spores spores}))))

          (not= action "reflect")
          (js/Promise.reject (js/Error. (str "Unknown session_mycology action: " (aget params "action"))))

          :else
          (execute-reflect-action params cwd session-file model-label node-path append-jsonl-fn reflections-file-fn
                                  find-latest-spore-fn spore-drafts-dir-fn write-spore-draft-fn
                                  spores-file-fn promote-spore-to-skill-fn))))))

(defn make-session-mycology-tool [execute-fn]
  (partial create-tool-obj
           "session_mycology"
           "Session Mycology"
           (str "Record a per-turn retrospective with p-scores and incubate reusable skill spores when work felt harder than it should have.\n"
                "At the end of each substantive turn, silently run a tiny retrospective:\n"
                "- p-efficiency = confidence the path was near-minimal.\n"
                "- p-friction = confidence the work felt harder than it should have.\n"
                "- p-skill-candidate = confidence a reusable skill or protocol would compress future effort.\n"
                "If you have enough evidence, call session_mycology with action=\"reflect\" near the end of the turn.\n"
                "If p-skill-candidate >= " (.toFixed SPORE-THRESHOLD 2)
                " and the pattern seems reusable beyond the immediate task, include candidateName and candidateDescription.\n"
                "Skip the tool for tiny conversational turns or when evidence is too thin.")
           "Call session_mycology to record retrospection and incubate skill spores."
           ["Call session_mycology with action=\"reflect\" near the end of substantive turns."
            "Include candidateName and candidateDescription when p-skill-candidate is high and the pattern feels reusable."
            "Use action=\"list_recent\" when the user asks about prior spores or skill incubation status."
            "Keep the loop quiet; do not narrate the retrospective unless the user asks."]
           session-mycology-params
           execute-fn))

(defn create-session-mycology-tools
  ([runtime config] (create-session-mycology-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id] (or (nil? auth-context) (ctx-tool-allowed? auth-context tool-id)))
         execute-fn (make-execute-fn path os)]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "session_mycology")
                  ((make-session-mycology-tool execute-fn) runtime config))]))))))
