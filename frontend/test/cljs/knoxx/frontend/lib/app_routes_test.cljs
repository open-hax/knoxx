(ns knoxx.frontend.lib.app-routes-test
  "cljs.test parity for the ported app-routes helpers — mirrors
   src/lib/app-routes.test.ts so the CLJS impl is verified canonical before the
   TypeScript copy retires."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.lib.app-routes :as sut]))

(deftest builds-canonical-ops-routes-without-duplicate-slashes
  (is (= "/ops/admin" (sut/join-path "/ops/" "/admin/")))
  (is (= "/ops" (sut/join-path "/ops" "")))
  (is (= "/ops" (sut/join-path "/ops")))
  (is (= "/" (sut/join-path "/" "")))
  (is (= "/ops/documents" (:documents sut/ops-routes)))
  (is (= "/ops/docs/view" (:docs-view sut/ops-routes)))
  (is (= "/agents" sut/agents-route))
  (is (= "/events" sut/events-route))
  (is (= "/events" sut/event-agents-route))
  (is (= "/event-agents" sut/legacy-event-agents-route)))

(deftest remaps-legacy-next-routes-to-ops-routes
  (is (= "/ops" (sut/remap-legacy-ops-path "/next")))
  (is (= "/ops/admin" (sut/remap-legacy-ops-path "/next/admin")))
  (is (= "/ops/docs/view?path=docs%2Freadme.md#L12"
         (sut/remap-legacy-ops-path "/next/docs/view" "?path=docs%2Freadme.md" "#L12"))))

(deftest leaves-non-legacy-routes-untouched
  (is (= "/" (sut/remap-legacy-ops-path "/")))
  (is (= "/translations?q=test" (sut/remap-legacy-ops-path "/translations" "?q=test"))))

(deftest marks-basic-users-and-limits-them-to-the-chat-surface
  (is (true? (sut/basic-user-role? [sut/basic-user-role])))
  (is (false? (sut/basic-user-role? ["system_admin"])))
  (is (true? (sut/can-access-path? "/" [sut/basic-user-role])))
  (is (true? (sut/can-access-path? "/signup" [sut/basic-user-role])))
  (is (false? (sut/can-access-path? "/contracts" [sut/basic-user-role])))
  (is (false? (sut/can-access-path? "/ops/admin" [sut/basic-user-role])))
  (testing "non-basic users are unrestricted"
    (is (true? (sut/can-access-path? "/ops/admin" ["system_admin"])))
    (is (true? (sut/can-access-path? "/contracts" [])))))
