(ns knoxx.backend.law.publication-surface
  "The one required-surface contract for the contract-owned publication path.

  Imported by deploy verification AND by the contract-publication E2E, so both
  check the same list. Two copies would drift, and the copy that drifted would be
  the one that stopped catching a missing route.

  Every entry names its method and its intended authorization, because a surface
  that exists but authorizes the wrong way is not a working surface. Read and
  write permissions are deliberately distinct: seeing the publication topology
  must not imply authority to change what is public.

  NOTE: `/api/publications/gardens` is the deploy-owned Garden review surface.
  No health route exists on this path. The list below is what actually ships;
  `surface-count` is asserted against it so a silently shortened list fails."
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
    :path "/api/publications/gardens"
    :permission "org.publications.read"
    :access :read
    :why "deployed Garden contracts, locale catalogs, and publication placements"}

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
    ;; Platform-scoped, not org-scoped: this route rewrites the *global* pipeline
    ;; default, which is a deploy-time default set by contract files. An
    ;; org-scoped permission let one tenant's administrator change it for every
    ;; other tenant (#233).
    :permission "platform.translations.manage"
    :access :write
    :why "the authoritative translation model selection"}

   {:method "GET"
    :path "/api/publications/translations/reviews"
    :permission "org.translations.read"
    :access :read
    :why "revision-bound translation evidence available for publication review"}])

(def surface-count
  "Asserted in tests so a silently shortened list fails rather than quietly
   verifying less."
  8)

(def retired-authority-paths
  "Paths this epic actually retired: they must have NO caller anywhere in the
   shipped source trees, and the guard scans those trees rather than an
   allow-list of files somebody remembered to add.

  Garden REST is included because the deploy-owned Garden review surface has
  replaced every supported caller."
  ["/api/openplanner/v1/gardens"
   "/api/openplanner/v1/public/gardens/"
   "/api/openplanner/v1/translations/config"
   "/v1/translations/config"])

(def legacy-paths-with-known-callers
  "Legacy paths that still have shipped callers, mapped to the production files
   that call them. The remaining CMS publish call is retired by the frontend
   cutover, not by the Garden read-model migration.

   Asserted positively rather than skipped: the guard checks that these are
   *exactly* the callers, so a new one fails the build and a removed one shows up
   as progress rather than as a silently weakened test."
  {"/api/openplanner/v1/cms/publish"
   ["frontend/src/pages/CmsPage.tsx"]})

(def scanned-source-roots
  "Shipped source trees the retirement guard walks, relative to the repository
   root. Test and fixture files are excluded by the guard itself: a test naming a
   legacy path to assert something *about* it is not a caller."
  ["backend/src" "frontend/src" "ingestion/src" "shared/src"])

(def retired-deploy-flags
  "Flags that let a deploy conditionally skip the CMS/publication surfaces. The
   replacement surfaces are unconditional, so these must not exist."
  ["KNOXX_EXPECT_OPENPLANNER_REST"])

(defn assert-surfaces!
  []
  (publication/assert-valid! :publication/required-surfaces
                             [:vector RequiredSurface]
                             required-surfaces))
