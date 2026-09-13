(ns knoxx.frontend.lib.app-routes-test
  "cljs.test parity for the ported app-routes helpers — mirrors
   src/lib/app-routes.test.ts so the CLJS impl is verified canonical before the
   TypeScript copy retires."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.app-routes :as sut]))

(t/deftest builds-canonical-ops-routes-without-duplicate-slashes
  (t/is (= "/ops/admin" (sut/join-path "/ops/" "/admin/")))
  (t/is (= "/ops" (sut/join-path "/ops" "")))
  (t/is (= "/ops" (sut/join-path "/ops")))
  (t/is (= "/" (sut/join-path "/" "")))
  (t/is (= "/ops/documents" (:documents sut/ops-routes)))
  (t/is (= "/ops/docs/view" (:docs-view sut/ops-routes)))
  (t/is (= "/agents" sut/agents-route))
  (t/is (= "/events" sut/events-route))
  (t/is (= "/events" sut/event-agents-route))
  (t/is (= "/event-agents" sut/legacy-event-agents-route)))

(t/deftest remaps-legacy-next-routes-to-ops-routes
  (t/is (= "/ops" (sut/remap-legacy-ops-path "/next")))
  (t/is (= "/ops/admin" (sut/remap-legacy-ops-path "/next/admin")))
  (t/is (= "/ops/docs/view?path=docs%2Freadme.md#L12"
         (sut/remap-legacy-ops-path "/next/docs/view" "?path=docs%2Freadme.md" "#L12"))))

(t/deftest leaves-non-legacy-routes-untouched
  (t/is (= "/" (sut/remap-legacy-ops-path "/")))
  (t/is (= "/translations?q=test" (sut/remap-legacy-ops-path "/translations" "?q=test"))))

(t/deftest marks-basic-users-and-limits-them-to-the-chat-surface
  (t/is (true? (sut/basic-user-role? [sut/basic-user-role])))
  (t/is (false? (sut/basic-user-role? ["system_admin"])))
  (t/is (true? (sut/can-access-path? "/" [sut/basic-user-role])))
  (t/is (true? (sut/can-access-path? "/signup" [sut/basic-user-role])))
  (t/is (false? (sut/can-access-path? "/contracts" [sut/basic-user-role])))
  (t/is (false? (sut/can-access-path? "/ops/admin" [sut/basic-user-role])))
  (t/testing "non-basic users are unrestricted"
    (t/is (true? (sut/can-access-path? "/ops/admin" ["system_admin"])))
    (t/is (true? (sut/can-access-path? "/contracts" [])))))
