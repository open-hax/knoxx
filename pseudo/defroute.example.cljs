(defrouter some-api
                                        ; defines a function with args   [app runtime helpers]
                                        ; this space is inside of a closure
  ;; do-route do-json do-err do-ctx do-perm do-perm do-text
  (route GET "/path/to/:id")
  (route POST "/path/to/:id"
         ;; request, reply, ctx are availbale in here
         )
  )
