(ns knoxx.frontend.law.migration-shadow
  "Contracts for Shadow source and build effects outside the TypeScript inventory.")

(defn assert-file-resolutions!
  "Require explicit inventory support for file modules beyond the governed bridges."
  [{:keys [path resolutions] :as facts}]
  (doseq [[module resolution] resolutions
          :when (and (= :file (:target resolution))
                     (not (contains? #{"@open-hax/knoxx-app-bridge"
                                       "@open-hax/knoxx-frontend-bridge"} module)))]
    (throw (ex-info "Shadow file resolution requires explicit migration inventory support"
                    {:path path :module module :resolution resolution})))
  facts)

(defn assert-build-hooks!
  "Reject executable build hooks whose source and output effects are not inventoried."
  [{:keys [path build-hooks] :as facts}]
  (doseq [hooks build-hooks
          :when (not (or (nil? hooks) (and (sequential? hooks) (empty? hooks))))]
    (throw (ex-info "Shadow build hooks require explicit migration inventory support"
                    {:path path :build-hooks hooks})))
  facts)

(def ^:private test-bootstrap-js
  "The existing node-test environment bootstrap; additional source needs inventory support."
  (str "const { JSDOM } = require('jsdom'); const __jsdom = new JSDOM('<!doctype html><html><body></body></html>'); "
       "globalThis.window = __jsdom.window; globalThis.document = __jsdom.window.document; "
       "Object.defineProperty(globalThis, 'navigator', { value: __jsdom.window.navigator, configurable: true }); "
       "globalThis.IS_REACT_ACT_ENVIRONMENT = true;"))

(defn assert-javascript-inputs!
  "Admit empty Shadow text inputs and the exact existing test environment bootstrap."
  [{:keys [path javascript-inputs] :as facts}]
  (doseq [{:keys [config-path value]} javascript-inputs
          :when (not (or (nil? value) (= "" value)
                         (and (= [:builds :test :prepend-js] config-path)
                              (= test-bootstrap-js value))))]
    (throw (ex-info "Shadow JavaScript text inputs require explicit migration inventory support"
                    {:path path :config-path config-path})))
  facts)
