(ns knoxx.backend.tools.session-mycology
  "Per-turn retrospection with p-scores and skill-spore incubation.

   Knoxx-native port of the eta-mu extension session-mycology.
   Because Knoxx does not yet run the full eta-mu event runtime,
   this is exposed as a voluntary agent tool.  The model is instructed
   (via the tool description and prompt snippet) to call it near the
   end of substantive turns.

   State is stored in ~/.knoxx/state/session-mycology/ and is
   compatible with the eta-mu spore format so skills can be promoted
   into either runtime.

   ETA-MU RUNTIME INTEGRATION NOTES
   ================================
   To make the full eta-mu runtime \"just work\" in Knoxx, the following
   gaps would need to close:

   1. Extension loader
      - eta-mu extensions compile to :node-library builds via shadow-cljs.
      - Knoxx would need a loader that discovers .js artifacts (either
        from ~/.knoxx/extensions/ or from built-in slices) and requires
        them at startup.  The loader calls the platform-specific init
        fn that the eta-mu build script generates.

   2. Event bus
      - eta-mu extensions hook: session_start, session_switch, turn_start,
        turn_end, before_agent_start, context, session_shutdown.
      - Knoxx turn lifecycle lives in knoxx.backend.agents.turn and
        knoxx.backend.agent-runtime.  Add a small multimethod or protocol
        (knoxx.backend.extension-runtime/dispatch-event event-name payload ctx)
        and call it at the matching lifecycle points.

   3. Command registry
      - eta-mu slash commands (e.g. /mycology) are registered via
        (em/command ...).  Knoxx chat commands currently live in the
        frontend or in hardcoded route handlers.  A command router
        that delegates to extension command handlers would bridge this.

   4. Prompt injection
      - eta-mu extensions mutate the system prompt via the context and
        before_agent_start events.  Knoxx already has build-agent-user-message
        and build-agent-multimodal-message in agent-hydration; a hook
        there for extensions to append prompt sections would suffice.

   5. Context message pruning
      - session-mycology injects customType messages and then prunes
        old ones in the context event.  Knoxx would need a pass over
        request messages before sending to the model that lets extensions
        filter/inject.

   6. UI status / notify / widget callbacks
      - eta-mu ctx exposes hasUI, ui.setStatus, ui.notify, ui.setWidget.
      - Knoxx could expose these via the WebSocket broadcast layer
        (realtime.cljs) so extensions can push ephemeral UI state.

   The fastest path to full compatibility is probably:
     a) Write knoxx.backend.extension-runtime  (loader + event bus)
     b) Add extension-runtime/dispatch-event calls in agents.turn
     c) Add a prompt-extension hook in agent-hydration
     d) Point the loader at ~/.knoxx/extensions/ and built-ins
     e) Re-use this namespace as the first built-in that also works
        as an eta-mu extension when loaded through the adapter."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]
            ["node:fs/promises" :as fsp]
            ["node:path" :as path]))

;; ---------------------------------------------------------------------------
;; Constants

(def ^:const SPORE-THRESHOLD 0.72)
(def ^:const PROMOTION-MIN-RECURRENCE
  (js/Math.max 2 (js/Number (or (aget js/process.env "KNOXX_MYCOLOGY_PROMOTION_MIN_RECURRENCE") 2))))
(def ^:const PROMOTION-HINT-P
  (let [v (js/Number (or (aget js/process.env "KNOXX_MYCOLOGY_PROMOTION_HINT_P") 0.84))]
    (js/Math.max 0 (js/Math.min 1 (if (js/Number.isFinite v) v 0.84)))))

;; ---------------------------------------------------------------------------
;; State helpers (closed over fsp / node-path / node-os at factory time)

(defn- ^:private make-state-dir-fn [node-os node-path]
  (fn []
    (node-path.join (.homedir node-os) ".knoxx" "state" "session-mycology")))

(defn- ^:private make-reflections-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "turn-reflections.jsonl")))

(defn- ^:private make-spores-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "skill-spores.jsonl")))

(defn- ^:private make-promotions-file-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "skill-promotions.jsonl")))

(defn- ^:private make-spore-drafts-dir-fn [state-dir-fn node-path]
  (fn [] (node-path.join (state-dir-fn) "spores")))

;; ---------------------------------------------------------------------------
;; Pure helpers

(defn- now-iso []
  (.toISOString (js/Date.)))

(defn- clamp-01 [value fallback]
  (let [n (js/Number value)]
    (if (js/Number.isFinite n)
      (js/Math.max 0 (js/Math.min 1 n))
      fallback)))

(defn- slugify [value]
  (let [s (-> (str (or value ""))
              str/lower-case
              (str/replace #"[^a-z0-9]+" "-")
              (str/replace #"^-+|-$" "")
              (str/replace #"--+" "-"))]
    (if (str/blank? s) "skill-spore" s)))

(defn- yaml-quote [value]
  (js/JSON.stringify (str (or value ""))))

(defn- same-cwd [node-path a b]
  (and a b (= (node-path.resolve a) (node-path.resolve b))))

(defn- normalize-reuse-scope [value]
  (let [v (-> (str (or value "session")) str/trim str/lower-case)]
    (if (#{"turn" "session" "multi-session"} v) v "session")))

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

;; ---------------------------------------------------------------------------
;; File I/O helpers (closed over fsp / node-path)

(defn- ^:private make-append-jsonl-fn [node-path]
  (fn [file-path value]
    (p/let [_ (.mkdir fsp (node-path.dirname file-path) #js {:recursive true})
            _ (.appendFile fsp file-path (str (js/JSON.stringify value) "\n") "utf8")])))

(defn- ^:private make-read-jsonl-fn []
  (fn [file-path limit]
    (-> (.readFile fsp file-path "utf8")
        (.then (fn [text]
                 (let [lines (-> text
                               (str/split #"\r?\n")
                               (->> (filter identity))
                               (js/Array.prototype.slice.call (- limit)))]
                   (js/Array.from
                    (-> lines
                        (.map (fn [line]
                                (try (js/JSON.parse line)
                                     (catch js/Error _ nil))))
                        (.filter (fn [x] x))))))
        (.catch (fn [_] #js []))))))

(defn- ^:private make-load-recent-spores-fn [spores-file-fn node-path]
  (fn [cwd limit]
    (p/let [read-fn (make-read-jsonl-fn)
                  rows (-> (read-fn (spores-file-fn) 400)
                             (.filter (fn [row]
                                       (or (not cwd) (same-cwd node-path (aget row "cwd") cwd)))))]
      (-> (.slice rows (- (.-length rows) limit))
          (.reverse)
          (js/Array.from)))))

(defn- ^:private make-find-latest-spore-fn [spores-file-fn node-path]
  (fn [slug cwd]
    (let [read-fn (make-read-jsonl-fn)
          rows (-> (read-fn (spores-file-fn) 400)
                   (.filter (fn [row]
                              (and (= (aget row "slug") slug)
                                   (or (not cwd) (same-cwd node-path (aget row "cwd") cwd))))))]
      (or (.at rows -1) nil))))

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

;; ---------------------------------------------------------------------------
;; Draft builders

(defn- build-spore-skill-draft [spore]
  (str "---\n"
       "name: " (aget spore "slug") "\n"
       "description: " (yaml-quote (aget spore "description")) "\n"
       "disable-model-invocation: true\n"
       "metadata:\n"
       "  origin: session-mycology-spore\n"
       "  recurrence: " (aget spore "recurrence") "\n"
       "---\n\n"
       "# " (aget spore "name") "\n\n"
       "## Goal\n"
       (aget spore "description") "\n\n"
       "## Use This Skill When\n"
       "- The same friction pattern recurs.\n\n"
       "## Do Not Use This Skill When\n"
       "- The pain was only a one-off environment glitch.\n"))

(defn- build-spore-contract-draft [spore]
  (str "(skill-contract\n"
       "  (name " (yaml-quote (aget spore "slug")) ")\n"
       "  (v \"knoxx.skill/" (aget spore "slug") "@0.0.1-spore\")\n"
       "  (intent " (yaml-quote (aget spore "description")) ")\n\n"
       "  (activation\n"
       "    (priority 35)\n"
       "    (explicit [\"skill:" (aget spore "slug") "\"])\n"
       "    (triggers [" (yaml-quote (.toLowerCase (aget spore "name"))) "]))\n\n"
       "  (governance\n"
       "    (touch-layer :mutable)\n"
       "    (non-override [:mission :directives :safety :license :output-shape])\n"
       "    (requires-user-approval false))\n)"))

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

(defn- latest-spores-by-slug [spores-file-fn node-path cwd]
  (when-let [rows (js-await (spores-file-fn cwd))]  ;; async tail-read
    (let [latest (js/Map.)]
      (->> rows
           (filter (fn [row] (or (nil? cwd) (same-cwd node-path (.-cwd row) cwd))))
           (run! (fn [row] (when (.-slug row) (.set latest (str (.-slug row)) row))))
           (Array.from (.values latest))))))

(defn- build-live-skill [spore]
  (str "---\n"
       "name: " (aget spore "slug") "\n"
       "description: " (yaml-quote (aget spore "description")) "\n"
       "license: GPL-3.0\n"
       "metadata:\n"
       "  origin: session-mycology-promotion\n"
       "  promoted-from-spore: " (aget spore "slug") "\n"
       "  recurrence: " (aget spore "recurrence") "\n"
       "---\n\n"
       "# Skill: " (aget spore "name") "\n\n"
       "## Goal\n"
       (aget spore "description") "\n\n"
       "## Use This Skill When\n"
       "- The same pattern or failure mode has recurred enough to deserve a named protocol.\n"
       "- The current task clearly matches the lesson captured by this promoted spore.\n\n"
       "## Do Not Use This Skill When\n"
       "- The situation is obviously unrelated to " (aget spore "name") ".\n"
       "- You only have a one-off glitch with no evidence that the recurring pattern applies.\n\n"
       "## Inputs\n"
       "- The current task context.\n"
       "- The relevant files, logs, or artifacts that exhibit the pattern.\n\n"
       "## Steps\n"
       "1. Verify the current task really matches the recurring pattern.\n"
       "2. Apply the core lesson from the originating spore: " (aget spore "description") "\n"
       "3. Prefer concrete evidence over narrative momentum.\n"
       "4. If the pattern no longer fits reality, update or retire this skill instead of forcing it.\n\n"
       "## Output\n"
       "- A truthful, concrete application of the pattern to the current task.\n"))

(defn- build-live-contract [spore]
  (str "(skill-contract\n"
       "  (name " (yaml-quote (aget spore "slug")) ")\n"
       "  (v \"knoxx.skill/" (aget spore "slug") "@0.1.0\")\n\n"
       "  (intent " (yaml-quote (aget spore "description")) ")\n\n"
       "  (activation\n"
       "    (explicit [\"skill:" (aget spore "slug") "\"])\n"
       "    (triggers [" (yaml-quote (.toLowerCase (aget spore "name")))
       " " (yaml-quote (str/replace (aget spore "slug") #"-" " ")) "]))\n\n"
       "  (governance\n"
       "    (touch-layer :mutable)\n"
       "    (non-override [:mission :directives :safety :license :output-shape])\n"
       "    (requires-user-approval false))\n)\n"))

(defn- ^:private make-promote-spore-to-skill-fn [node-path node-os
                                                  promotions-file-fn]
  (fn [spore]
    (if-not (promotion-eligible? spore)
      #js {:promoted false :eligible false}
      (let [home (.homedir node-os)
            dir (node-path.join home ".knoxx" "skills" (aget spore "slug"))
            skill-path (node-path.join dir "SKILL.md")
            contract-path (node-path.join dir "CONTRACT.edn")]
        (p/let [_ (.mkdir fsp dir #js {:recursive true})
                  created-skill (-> (.access fsp skill-path)
                                        (.then (fn [_] false))
                                        (.catch (fn [_]
                                                 (.writeFile fsp skill-path (build-live-skill spore) "utf8")
                                                 true)))
                  created-contract (-> (.access fsp contract-path)
                                         (.then (fn [_] false))
                                         (.catch (fn [_]
                                                  (.writeFile fsp contract-path (build-live-contract spore) "utf8")
                                                  true)))]
          (when (or created-skill created-contract)
            ((make-append-jsonl-fn node-path)
             (promotions-file-fn)
             #js {:ts (now-iso)
                  :slug (aget spore "slug")
                  :name (aget spore "name")
                  :recurrence (aget spore "recurrence")
                  :skillCandidateP (aget spore "skillCandidateP")
                  :cwd (aget spore "cwd")
                  :sessionFile (aget spore "sessionFile")
                  :skillPath skill-path
                  :contractPath contract-path
                  :createdSkill created-skill
                  :createdContract created-contract}))
          #js {:promoted (or created-skill created-contract)
               :eligible true
               :skillPath skill-path
               :contractPath contract-path
               :createdSkill created-skill
               :createdContract created-contract})))))

(defn- ^:private make-write-spore-draft-fn [node-path node-os
                                             spore-drafts-dir-fn]
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
                       "## Lesson\n"
                       (or (aget reflection "lesson") "_none captured_") "\n\n"
                       "## Better path next time\n"
                       (or (aget reflection "betterPath") "_none captured_") "\n\n"
                       "## Candidate description\n"
                       (aget spore "description") "\n\n"
                       "## Promotion gate\n"
                       "Promote this spore into a live skill after either:\n"
                       "- recurrence >= " PROMOTION-MIN-RECURRENCE "\n"
                       "- explicit user request\n"
                       "- or strong evidence that the pattern generalizes beyond the current task\n\n"
                       "## Draft SKILL.md\n\n"
                       "~~~markdown\n"
                       skill-draft
                       "~~~\n\n"
                       "## Draft CONTRACT.edn\n\n"
                       "~~~edn\n"
                       contract-draft
                       "~~~\n\n"
                       "## Suggested live-skill path\n\n"
                       "- " (node-path.join home ".knoxx" "skills" (aget spore "slug") "SKILL.md") "\n"
                       "- " (node-path.join home ".knoxx" "skills" (aget spore "slug") "CONTRACT.edn") "\n")]
      (.writeFile fsp file-path content "utf8")
      file-path)))

;; ---------------------------------------------------------------------------
;; Tool execute (closed over runtime deps)

(defn- ^:private make-execute-fn [node-path node-os]
  (let [state-dir-fn (make-state-dir-fn node-os node-path)
        reflections-file-fn (make-reflections-file-fn state-dir-fn node-path)
        spores-file-fn (make-spores-file-fn state-dir-fn node-path)
        promotions-file-fn (make-promotions-file-fn state-dir-fn node-path)
        spore-drafts-dir-fn (make-spore-drafts-dir-fn state-dir-fn node-path)
        append-jsonl-fn (make-append-jsonl-fn node-path)
        read-jsonl-fn (make-read-jsonl-fn)
        load-recent-spores-fn (make-load-recent-spores-fn spores-file-fn node-path)
        find-latest-spore-fn (make-find-latest-spore-fn spores-file-fn node-path)
        promote-spore-to-skill-fn (make-promote-spore-to-skill-fn node-path node-os promotions-file-fn)
        write-spore-draft-fn (make-write-spore-draft-fn node-path node-os spore-drafts-dir-fn)]
    (fn execute-session-mycology-tool [_tcid params _sig on-update ctx]
      (let [action (.toLowerCase (.trim (str (or (aget params "action") "reflect"))))
            cwd (or (aget ctx "cwd") (.. js/process cwd))
            session-file (or (aget ctx "sessionFile") "")
            model-label (or (aget ctx "modelLabel") "unknown")]
        (cond
          (= action "list_recent")
          (let [spores (load-recent-spores-fn cwd 5)]
            (js/Promise.resolve
             (tool-text-result (if (pos? (.-length spores))
                                 (summarize-spores spores)
                                 "- none yet")
                               #js {:spores spores})))

          (not= action "reflect")
          (js/Promise.reject (js/Error. (str "Unknown session_mycology action: " (aget params "action"))))

          :else
          (let [reflection #js {:ts (now-iso)
                                :cwd cwd
                                :sessionFile session-file
                                :model model-label
                                :efficiencyP (clamp-01 (aget params "efficiencyP") 0.5)
                                :frictionP (clamp-01 (aget params "frictionP") 0.5)
                                :skillCandidateP (clamp-01 (aget params "skillCandidateP") 0.5)
                                :lesson (.trim (str (or (aget params "lesson") "")))
                                :betterPath (.trim (str (or (aget params "betterPath") "")))}]
            (append-jsonl-fn (reflections-file-fn) reflection)
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
                (let [slug (slugify name)
                      prior (find-latest-spore-fn slug cwd)
                      prior-recurrence (js/Number (or (when prior (aget prior "recurrence")) 0))
                      prior-draft-path (when prior (aget prior "draftPath"))
                      spore #js {:ts (now-iso)
                                 :name name
                                 :slug slug
                                 :description description
                                 :reuseScope (normalize-reuse-scope (aget params "reuseScope"))
                                 :cwd cwd
                                 :sessionFile session-file
                                 :model model-label
                                 :reflectionTs (aget reflection "ts")
                                 :reflectionKind (reflection-kind reflection)
                                 :recurrence (js/Math.max 1 (inc prior-recurrence))
                                 :efficiencyP (aget reflection "efficiencyP")
                                 :frictionP (aget reflection "frictionP")
                                 :skillCandidateP (aget reflection "skillCandidateP")}]
                  (aset spore "draftPath"
                        (or prior-draft-path
                            (node-path.join (spore-drafts-dir-fn) (str slug ".md"))))
                  (write-spore-draft-fn reflection spore)
                  (append-jsonl-fn (spores-file-fn) spore)
                  (let [promotion (promote-spore-to-skill-fn spore)]
                    (js/Promise.resolve
                     (tool-text-result
                      (str "Recorded reflection (p_eff=" (.toFixed (aget reflection "efficiencyP") 2)
                           ", p_fric=" (.toFixed (aget reflection "frictionP") 2)
                           ", p_skill=" (.toFixed (aget reflection "skillCandidateP") 2)
                           "). Incubated spore: " name " -> " (aget spore "draftPath")
                           (when (aget promotion "promoted")
                             (str " | promoted live skill: " (aget promotion "skillPath"))))
                      #js {:reflection reflection
                           :spore spore
                           :promotion promotion}))))))))))))

;; ---------------------------------------------------------------------------
;; Public factory

(defn create-session-mycology-tools
  "Create the session-mycology tool suite."
  ([runtime config] (create-session-mycology-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         node-path (aget runtime "path")
         node-os (aget runtime "os")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         params (.Object Type
                         #js {:action (.String Type #js {:description "Action: reflect to record a retrospective, or list_recent to inspect recent spores."})
                              :efficiencyP (type-optional Type (.Number Type #js {:description "Confidence 0..1 that the chosen path was near-minimal." :minimum 0 :maximum 1}))
                              :frictionP (type-optional Type (.Number Type #js {:description "Confidence 0..1 that the work was harder than it should have been." :minimum 0 :maximum 1}))
                              :skillCandidateP (type-optional Type (.Number Type #js {:description "Confidence 0..1 that a reusable skill or protocol would compress future effort." :minimum 0 :maximum 1}))
                              :lesson (type-optional Type (.String Type #js {:description "Short lesson from the turn."}))
                              :betterPath (type-optional Type (.String Type #js {:description "Better path to try next time."}))
                              :candidateName (type-optional Type (.String Type #js {:description "Candidate skill name if a spore should be incubated."}))
                              :candidateDescription (type-optional Type (.String Type #js {:description "One sentence describing the candidate skill."}))
                              :reuseScope (type-optional Type (.String Type #js {:description "Optional reuse scope: turn, session, or multi-session."}))})
         execute-fn (make-execute-fn node-path node-os)]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "session_mycology")
                  (doto (js-obj)
                    (aset "name" "session_mycology")
                    (aset "label" "Session Mycology")
                    (aset "description"
                          (str "Record a per-turn retrospective with p-scores and incubate reusable skill spores when work felt harder than it should have.\n"
                               "At the end of each substantive turn, silently run a tiny retrospective:\n"
                               "- p-efficiency = confidence the path was near-minimal.\n"
                               "- p-friction = confidence the work felt harder than it should have.\n"
                               "- p-skill-candidate = confidence a reusable skill or protocol would compress future effort.\n"
                               "If you have enough evidence, call session_mycology with action=\"reflect\" near the end of the turn.\n"
                               "If p-skill-candidate >= " (.toFixed SPORE-THRESHOLD 2)
                               " and the pattern seems reusable beyond the immediate task, include candidateName and candidateDescription.\n"
                               "Skip the tool for tiny conversational turns or when evidence is too thin."))
                    (aset "promptSnippet"
                          "Call session_mycology to record retrospection and incubate skill spores.")
                    (aset "promptGuidelines"
                          (clj->js ["Call session_mycology with action=\"reflect\" near the end of substantive turns."
                                    "Include candidateName and candidateDescription when p-skill-candidate is high and the pattern feels reusable."
                                    "Use action=\"list_recent\" when the user asks about prior spores or skill incubation status."
                                    "Keep the loop quiet; do not narrate the retrospective unless the user asks."]))
                    (aset "parameters" params)
                    (aset "execute" execute-fn)))]))))))
