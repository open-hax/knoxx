(ns knoxx.backend.infra.stores.translation-evidence-registry
  "Holds the translation evidence store, so bootstrap and the routes that read
  it do not have to import each other.

  Nil until Mongo is up. Nil is a *refusal*, never a fallback: substituting the
  in-memory store would accept a dispatch, lose its revision binding on the next
  restart, and leave a translation that completed with no receipt anyone can
  join it to — the gate would then report that work as never done, forever. A
  route with no store answers 503 instead.")

(defonce store* (atom nil))

(defn current
  "The durable translation evidence store, or nil when persistence is absent."
  []
  @store*)
