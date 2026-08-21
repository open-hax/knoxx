(ns knoxx.backend.law.cms-publication
  "JSON wire contracts for the CMS publication surface.

  Every wire key is **unqualified** and every wire map is `{:closed true}`.
  `knoxx.frontend.lib.api/request` serializes bodies with `clj->js`, which drops
  keyword namespaces, and decodes responses with `js->clj :keywordize-keys
  true`. So `:publication/state` leaves the browser as JSON `\"state\"` and comes
  back as `:state`: a wire contract demanding `:publication/state` would reject
  every real publish request before it reached the domain patch. Qualified domain
  keys are produced by explicit adapter mapping, never by wire validation.

  Every value is a JSON scalar. Enums cross as strings and are converted to
  domain keywords explicitly in both directions — `clj->js` is never relied on to
  preserve a keyword, because it renders one with `name` and would silently
  collapse `:tenant-a/foo` and `:tenant-b/foo` onto `\"foo\"`."
  (:require [knoxx.backend.law.publication :as publication]
            [open-hax.publication-wire :as wire]))

(def ResourceWireId
  "A qualified id as `\"namespace/name\"`, with no EDN leading colon."
  publication/NonBlankString)

(def DocumentWireJson
  [:map {:closed true}
   [:id ResourceWireId]
   [:title :string]
   [:source-locale publication/NonBlankString]
   ;; Only `:path` crosses. The resource's `:document/source` map is NOT passed
   ;; through: doing so would leak whatever fields a future resource gains
   ;; across the JSON boundary without anyone deciding to publish them.
   [:source [:map {:closed true} [:path publication/NonBlankString]]]])

(def GardenWireJson
  [:map {:closed true}
   [:id ResourceWireId]
   [:title :string]
   [:status (into [:enum] wire/garden-status-wire-values)]])

(def PublicationWireJson
  [:map {:closed true}
   [:id ResourceWireId]
   [:document ResourceWireId]
   [:garden ResourceWireId]
   [:locale publication/NonBlankString]
   [:revision publication/NonBlankString]
   [:path publication/NonBlankString]
   ;; Desired state, from the resource. Rendered separately from `:observed`,
   ;; which is runtime evidence and may legitimately disagree.
   [:desired (into [:enum] wire/state-wire-values)]
   [:observed [:maybe :string]]
   [:blockers [:vector :string]]])

(def CmsDocumentWireJson
  "Not double-wrapped: a document view is `{document, publications}`, never
   nested under a further `:document` key."
  [:map {:closed true}
   [:document DocumentWireJson]
   [:publications [:vector PublicationWireJson]]])

(def CmsListWireJson
  [:map {:closed true}
   [:documents [:vector CmsDocumentWireJson]]
   [:gardens [:vector GardenWireJson]]])

(def PublicationStatePatchJson
  "The body the frontend actually sends.

   The key and the enum values come from `open-hax.publication-wire`, the same
   namespace the frontend builds its body from — so a rename breaks both sides at
   once and this contract cannot drift away from the request it validates."
  [:map {:closed true}
   [wire/state-patch-key (into [:enum] wire/state-wire-values)]])

(def PublicationStatePatch
  "The domain patch the wire body decodes into. Deliberately state-only:
   document, garden, locale and revision are publication IDENTITY and cannot
   move through a state edit."
  [:map {:closed true}
   [:publication/state (into [:enum] (sort (keys wire/state-values)))]])
