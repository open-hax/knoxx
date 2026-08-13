(ns knoxx.backend.law.publication-surface
  "The one required-surface contract for the contract-owned publication path.

  Imported by deploy verification AND by the contract-publication E2E, so both
  check the same list. Two copies would drift, and the copy that drifted would be
  the one that stopped catching a missing route.

  Every entry names its method and its intended authorization, because a surface
  that exists but authorizes the wrong way is not a working surface. Read and
  write permissions are deliberately distinct: seeing the publication topology
  must not imply authority to change what is public.

  NOTE: the card describes `/api/publications/gardens` and
  `/api/publications/health`. Neither was built — gardens arrive inside the
  topology response rather than as a separate route, and no health route exists
  on this path. The list below is what actually ships; `surface-count` is
  asserted against it so a silently shortened list fails."
  (:require [knoxx.backend.law.publication :as publication]))

(def RequiredSurface
  [:map {:closed true}
   [:method [:enum "GET" "PATCH"]]
   [:path publication/NonBlankString]
   [:permission publication/NonBlankString]
   [:access [:enum :read :write]]
   [:why publication/NonBlankString]])

(def required-surfaces
  "Every surface the contract-owned publication path needs in production. These
   are required UNCONDITIONALLY: there is no flag that lets a deploy skip the CMS
   or publication checks because a hosted backend is absent, because being
   absent-tolerant is the entire point of this work."
  [{:method "GET"
    :path "/api/publications/documents"
    :permission "org.publications.read"
    :access :read
    :why "the desired publication topology, resolved from resources alone"}

   {:method "GET"
    :path "/api/publications/documents/:documentId"
    :permission "org.publications.read"
    :access :read
    :why "one document's desired topology"}

   {:method "GET"
    :path "/api/cms/publications/documents"
    :permission "org.publications.read"
    :access :read
    :why "the CMS editor's normalized view, desired and observed side by side"}

   {:method "PATCH"
    :path "/api/cms/publications/intents/:publicationId"
    :permission "org.publications.manage"
    :access :write
    :why "the only way desired publication state changes; identity is immutable"}

   {:method "GET"
    :path "/api/translations/config"
    :permission "org.translations.read"
    :access :read
    :why "translation model and review policy, resolved from resources"}

   {:method "PATCH"
    :path "/api/translations/config"
    :permission "org.translations.manage"
    :access :write
    :why "the authoritative translation model selection"}])

(def surface-count
  "Asserted in tests so a silently shortened list fails rather than quietly
   verifying less."
  6)

(def retired-authority-paths
  "Paths that must have NO shipped caller. Each was authoritative only because a
   hosted backend owned the state."
  ["/api/openplanner/v1/gardens"
   "/api/openplanner/v1/translations/config"
   "/v1/translations/config"])

(def retired-deploy-flags
  "Flags that let a deploy conditionally skip the CMS/publication surfaces. The
   replacement surfaces are unconditional, so these must not exist."
  ["KNOXX_EXPECT_OPENPLANNER_REST"])

(defn assert-surfaces!
  []
  (publication/assert-valid! :publication/required-surfaces
                             [:vector RequiredSurface]
                             required-surfaces))
