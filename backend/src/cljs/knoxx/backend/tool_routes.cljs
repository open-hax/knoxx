(ns knoxx.backend.tool-routes
  (:require [clojure.string :as str]
            [knoxx.backend.http :as backend-http]))

(defn send-email!
  "Send an email via Gmail SMTP using nodemailer.
   Returns a promise that resolves with the result on success or rejects on failure."
  [runtime config to subject text-body cc bcc]
  (let [email (:gmail-app-email config)
        password (:gmail-app-password config)
        nodemailer (aget runtime "nodemailer")]
    (if (or (str/blank? email) (str/blank? password))
      (js/Promise.reject (js/Error. "Gmail credentials not configured"))
      (let [transporter (.createTransport nodemailer
                                           #js {:host "smtp.gmail.com"
                                                :port 587
                                                :secure false
                                                :auth #js {:user email
                                                           :pass password}})]
        (.sendMail transporter
                   #js {:from email
                        :to (str/join ", " to)
                        :cc (when (seq cc) (str/join ", " cc))
                        :bcc (when (seq bcc) (str/join ", " bcc))
                        :subject subject
                        :text text-body})))))

(defn register-tool-routes!
  [app runtime config {:keys [route!
                              json-response!
                              error-response!
                              with-request-context!
                              ensure-permission!
                              tool-catalog
                              ensure-role-can-use!
                              resolve-workspace-path
                              count-occurrences
                              replace-first
                              clip-text]}]
  (route! app "GET" "/api/tools/catalog"
          (fn [request reply]
            (let [role (or (aget request "query" "role") (:knoxx-default-role config))]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx
                    (ensure-permission! ctx "agent.chat.use"))
                  (json-response! reply 200 (tool-catalog config role ctx)))))))

  (route! app "POST" "/api/tools/email/send"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "email.send")
                        to (or (aget body "to") #js [])
                        cc (or (aget body "cc") #js [])
                        bcc (or (aget body "bcc") #js [])
                        subject (str (or (aget body "subject") "(no subject)"))
                        markdown (str (or (aget body "markdown") ""))]
                    (if (empty? to)
                      (json-response! reply 400 {:detail "Missing required field: to array"})
                      (-> (send-email! runtime config to subject markdown cc bcc)
                          (.then (fn [result]
                                   (json-response! reply 200 {:ok true
                                                              :role role
                                                              :message_id (aget result "messageId")})))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "Failed to send email: " (or (aget err "message") (str err)))}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/websearch"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "websearch")
                        query (str/trim (str (or (aget body "query") "")))
                        num-results (or (aget body "numResults") 8)
                        search-context-size (aget body "searchContextSize")
                        allowed-domains (or (aget body "allowedDomains") #js [])
                        model (aget body "model")]
                    (if (str/blank? query)
                      (json-response! reply 400 {:detail "query is required"})
                      (let [req-promise (backend-http/fetch-json (str (:proxx-base-url config) "/api/tools/websearch")
                                                                 #js {:method "POST"
                                                                      :headers (backend-http/bearer-headers (:proxx-auth-token config))
                                                                      :body (.stringify js/JSON
                                                                                        #js {:query query
                                                                                             :numResults num-results
                                                                                             :searchContextSize search-context-size
                                                                                             :allowedDomains allowed-domains
                                                                                             :model model})})]
                        (-> req-promise
                            (.then (fn [resp]
                                     (if (aget resp "ok")
                                       (json-response! reply 200 (assoc (js->clj (aget resp "body") :keywordize-keys true) :role role))
                                       (json-response! reply (or (aget resp "status") 502)
                                                       {:detail (pr-str (js->clj (aget resp "body") :keywordize-keys true))}))))
                            (.catch (fn [err]
                                      (json-response! reply 502 {:detail (str err)}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/read"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "read")
                        node-fs (aget runtime "fs")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        offset (max 1 (or (aget body "offset") 1))
                        limit (max 1 (or (aget body "limit") 400))]
                    (-> (.stat node-fs path-str)
                        (.then (fn [stat]
                                 (if (.isDirectory stat)
                                   (-> (.readdir node-fs path-str #js {:withFileTypes true})
                                       (.then (fn [entries]
                                                (let [content-lines (map (fn [entry]
                                                                           (str (aget entry "name")
                                                                                (when (.isDirectory entry) "/")))
                                                                         (array-seq entries))
                                                      [content truncated] (clip-text (str/join "\n" content-lines))]
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :content content
                                                                             :truncated truncated})))))
                                   (-> (.readFile node-fs path-str "utf8")
                                       (.then (fn [text]
                                                (let [lines (str/split-lines text)
                                                      start (dec offset)
                                                      stop (+ start limit)
                                                      numbered (map-indexed (fn [idx line]
                                                                              (str (+ start idx 1) ": " line))
                                                                            (take limit (drop start lines)))
                                                      [content clipped?] (clip-text (str/join "\n" numbered))]
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :content content
                                                                             :truncated (or clipped? (< stop (count lines)))}))))))))
                        (.catch (fn [err]
                                  (json-response! reply 404 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/write"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "write")
                        node-fs (aget runtime "fs")
                        node-path (aget runtime "path")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        content (str (or (aget body "content") ""))
                        overwrite (not= false (aget body "overwrite"))
                        create-parents (not= false (aget body "create_parents"))
                        parent (.dirname node-path path-str)
                        check-promise (if overwrite
                                        (js/Promise.resolve nil)
                                        (-> (.stat node-fs path-str)
                                            (.then (fn [_]
                                                     (js/Promise.reject (js/Error. (str "File exists and overwrite is false: " path-str)))))
                                            (.catch (fn [_]
                                                      (js/Promise.resolve nil)))))]
                    (-> check-promise
                        (.then (fn []
                                 (if create-parents
                                   (.mkdir node-fs parent #js {:recursive true})
                                   (js/Promise.resolve nil))))
                        (.then (fn []
                                 (.writeFile node-fs path-str content "utf8")))
                        (.then (fn []
                                 (json-response! reply 200 {:ok true
                                                            :role role
                                                            :path path-str
                                                            :bytes_written (.-length (.from js/Buffer content "utf8"))})))
                        (.catch (fn [err]
                                  (json-response! reply 409 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/edit"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "edit")
                        node-fs (aget runtime "fs")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        old-string (str (or (aget body "old_string") ""))
                        new-string (str (or (aget body "new_string") ""))
                        replace-all (true? (aget body "replace_all"))]
                    (-> (.readFile node-fs path-str "utf8")
                        (.then (fn [current]
                                 (if (= (.indexOf current old-string) -1)
                                   (js/Promise.reject (js/Error. "old_string not found in file"))
                                   (let [replacements (if replace-all
                                                        (count-occurrences current old-string)
                                                        1)
                                         updated (if replace-all
                                                   (str/replace current old-string new-string)
                                                   (replace-first current old-string new-string))]
                                     (-> (.writeFile node-fs path-str updated "utf8")
                                         (.then (fn []
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :replacements replacements}))))))))
                        (.catch (fn [err]
                                  (json-response! reply 409 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/bash"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "bash")
                        timeout-ms (min (max (or (aget body "timeout_ms") 120000) 1000) 300000)
                        workdir (if-let [raw-workdir (aget body "workdir")]
                                  (resolve-workspace-path runtime config raw-workdir)
                                  (.resolve (aget runtime "path") (:workspace-root config)))
                        exec-file-async (aget runtime "execFileAsync")]
                    (-> (exec-file-async "/bin/bash"
                                         #js ["-lc" (or (aget body "command") "")]
                                         #js {:cwd workdir
                                              :timeout timeout-ms
                                              :maxBuffer 1048576})
                        (.then (fn [result]
                                 (let [[stdout _] (clip-text (or (aget result "stdout") "") 24000)
                                       [stderr __] (clip-text (or (aget result "stderr") "") 12000)]
                                   (json-response! reply 200 {:ok true
                                                              :role role
                                                              :command (or (aget body "command") "")
                                                              :exit_code 0
                                                              :stdout stdout
                                                              :stderr stderr}))))
                        (.catch (fn [err]
                                  (if (and (aget err "killed") (not (number? (aget err "code"))))
                                    (json-response! reply 408 {:detail (str "Command timed out after " (/ timeout-ms 1000) "s")})
                                    (let [[stdout _] (clip-text (or (aget err "stdout") "") 24000)
                                          [stderr __] (clip-text (or (aget err "stderr") "") 12000)]
                                      (json-response! reply 200 {:ok false
                                                                 :role role
                                                                 :command (or (aget body "command") "")
                                                                 :exit_code (if (number? (aget err "code")) (aget err "code") 1)
                                                                 :stdout stdout
                                                                 :stderr stderr})))))))
                  (catch :default err
                    (error-response! reply err)))))))
  nil))
