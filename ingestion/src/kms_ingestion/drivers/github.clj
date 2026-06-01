(ns kms-ingestion.drivers.github
  "GitHub driver — ingests code, issues, and PRs from GitHub organization repositories.

   Config:
   - :org                 — GitHub organization or owner (required)
   - :repos               — vector of repo names; empty/nil = all org repos
   - :token               — GitHub Personal Access Token (required for private + rate)
   - :include-issues      — fetch issue bodies (default true)
   - :include-prs         — fetch pull request descriptions (default true)
   - :include-discussions — placeholder, not yet implemented (default false)
   - :include-wiki        — placeholder, not yet implemented (default true)
   - :file-types          — seq of file extensions to include (default common code/docs)
   - :branch              — branch/ref to walk for file trees (default repo default branch)
   - :api-base            — override API base (default https://api.github.com)

   The driver:
   1. DISCOVER — lists repos in the org, walks each repo's file tree, lists
      issues and PRs, and reports new/changed/unchanged items via content hash.
   2. EXTRACT  — fetches file content (base64-decoded), issue bodies, or PR
      descriptions for a single discovered item id.

   File ids are namespaced so EXTRACT can route correctly:
   - blob:<owner>/<repo>@<sha>:<path>
   - issue:<owner>/<repo>#<number>
   - pr:<owner>/<repo>#<number>

   State tracks the last scan timestamp per repo for incremental discovery."
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.security MessageDigest]
   [java.util Base64]
   [java.time Instant]))

;; ─── Defaults ─────────────────────────────────────────────────────────────

(def default-api-base "https://api.github.com")

(def default-file-types
  [".md" ".cljs" ".clj" ".cljc" ".ts" ".tsx" ".js" ".jsx" ".edn" ".txt"])

(def ^:private max-rate-retries 5)

;; ─── Hashing ──────────────────────────────────────────────────────────────

(defn- sha256 [^String s]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (.digest digest (.getBytes (or s "") "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) bytes))))

;; ─── URL building ─────────────────────────────────────────────────────────

(defn org-repos-url [api-base org]
  (str api-base "/orgs/" org "/repos"))

(defn repo-url [api-base owner repo]
  (str api-base "/repos/" owner "/" repo))

(defn tree-url [api-base owner repo ref]
  (str api-base "/repos/" owner "/" repo "/git/trees/" ref "?recursive=1"))

(defn contents-url [api-base owner repo path]
  (str api-base "/repos/" owner "/" repo "/contents/" path))

(defn issues-url [api-base owner repo]
  (str api-base "/repos/" owner "/" repo "/issues"))

(defn pulls-url [api-base owner repo]
  (str api-base "/repos/" owner "/" repo "/pulls"))

;; ─── File-id encoding ─────────────────────────────────────────────────────

(defn blob-id [owner repo sha path]
  (str "blob:" owner "/" repo "@" sha ":" path))

(defn issue-id [owner repo number]
  (str "issue:" owner "/" repo "#" number))

(defn pr-id [owner repo number]
  (str "pr:" owner "/" repo "#" number))

(defn parse-file-id
  "Parse a namespaced file id back into its components.
   Returns {:kind :blob/:issue/:pr :owner ... :repo ... :sha ... :path ... :number ...}
   or nil if the id is unrecognized."
  [^String id]
  (cond
    (str/starts-with? id "blob:")
    (let [body (subs id 5)
          [loc-sha path] (str/split body #":" 2)
          [loc sha] (str/split loc-sha #"@" 2)
          [owner repo] (str/split loc #"/" 2)]
      (when (and owner repo)
        {:kind :blob :owner owner :repo repo :sha sha :path path}))

    (str/starts-with? id "issue:")
    (let [body (subs id 6)
          [loc number] (str/split body #"#" 2)
          [owner repo] (str/split loc #"/" 2)]
      (when (and owner repo number)
        {:kind :issue :owner owner :repo repo :number number}))

    (str/starts-with? id "pr:")
    (let [body (subs id 3)
          [loc number] (str/split body #"#" 2)
          [owner repo] (str/split loc #"/" 2)]
      (when (and owner repo number)
        {:kind :pr :owner owner :repo repo :number number}))

    :else nil))

;; ─── HTTP helpers with rate-limit backoff ───────────────────────────────────

(defn auth-headers
  "Build GitHub API request headers. Includes Authorization when a token is set."
  [token]
  (cond-> {"Accept" "application/vnd.github+json"
           "X-GitHub-Api-Version" "2022-11-28"
           "User-Agent" "kms-ingestion-github/1.0"}
    (not (str/blank? token)) (assoc "Authorization" (str "Bearer " token))))

(defn rate-limited?
  "Detect a GitHub rate-limit / secondary-limit response.
   A 403/429 with remaining=0 (or a Retry-After header) signals throttling."
  [resp]
  (let [status (:status resp)
        headers (:headers resp)
        remaining (get headers "X-RateLimit-Remaining")]
    (boolean
     (or (= 429 status)
         (and (= 403 status)
              (or (some? (get headers "Retry-After"))
                  (= "0" remaining)))))))

(defn retry-after-ms
  "Compute how long to back off before retrying, in milliseconds.
   Prefers Retry-After (seconds); falls back to X-RateLimit-Reset (epoch secs);
   defaults to a doubling delay based on attempt number."
  [resp attempt]
  (let [headers (:headers resp)
        retry-after (some-> (get headers "Retry-After") str str/trim)
        reset (some-> (get headers "X-RateLimit-Reset") str str/trim)]
    (cond
      (and retry-after (re-matches #"\d+" retry-after))
      (* 1000 (parse-long retry-after))

      (and reset (re-matches #"\d+" reset))
      (max 0 (- (* 1000 (parse-long reset)) (System/currentTimeMillis)))

      :else
      (long (* 1000 (Math/pow 2 (min attempt 6)))))))

(defn api-get
  "GET a GitHub API URL, returning the parsed JSON body (keyword keys).
   Honors rate-limit backoff with bounded retries.
   Returns {:ok true :body ... :headers ...} or {:ok false :status ... :error ...}."
  [url {:keys [token] :as opts}]
  (let [headers (auth-headers token)]
    (loop [attempt 0]
      (let [resp (try
                   (http/get url {:headers headers
                                  :as :string
                                  :throw-exceptions false
                                  :socket-timeout (:timeout opts 30000)
                                  :connection-timeout (:timeout opts 30000)})
                   (catch Exception e
                     {:status -1 :error (.getMessage e)}))]
        (cond
          (= 200 (:status resp))
          {:ok true
           :body (try (json/parse-string (:body resp) true)
                      (catch Exception _ nil))
           :headers (:headers resp)}

          (and (rate-limited? resp) (< attempt max-rate-retries))
          (let [wait (min (retry-after-ms resp attempt) 60000)]
            (println "[github] rate limited on" url "- backing off" wait "ms")
            (Thread/sleep wait)
            (recur (inc attempt)))

          :else
          {:ok false
           :status (:status resp)
           :error (or (:error resp)
                      (str "HTTP " (:status resp)))})))))

;; ─── Config ─────────────────────────────────────────────────────────────────

(defn validate-config
  "Validate driver config. Returns {:valid true} or {:valid false :error \"...\"}."
  [config]
  (cond
    (str/blank? (:org config))
    {:valid false :error "Missing required :org in config"}

    (str/blank? (:token config))
    {:valid false :error "Missing required :token in config"}

    :else {:valid true}))

(defn- normalized-file-types [config]
  (let [types (or (:file-types config) (:file_types config) default-file-types)]
    (mapv (fn [t]
            (let [t (str/lower-case (str t))]
              (if (str/starts-with? t ".") t (str "." t))))
          types)))

(defn matches-file-type?
  "True when path ends with one of the configured file-type suffixes.
   An empty file-type list matches everything."
  [path file-types]
  (let [p (str/lower-case (str path))]
    (or (empty? file-types)
        (boolean (some #(str/ends-with? p %) file-types)))))

;; ─── Discovery ──────────────────────────────────────────────────────────────

(defn list-repos
  "List repos to scan. Honors explicit :repos; otherwise lists all org repos.
   Returns a seq of {:owner :name :default-branch}."
  [config]
  (let [api-base (or (:api-base config) default-api-base)
        org (:org config)
        explicit (seq (or (:repos config) []))]
    (if explicit
      (mapv (fn [r] {:owner org :name r :default-branch (:branch config)}) explicit)
      (let [resp (api-get (org-repos-url api-base org) config)]
        (if (:ok resp)
          (mapv (fn [r] {:owner org
                         :name (:name r)
                         :default-branch (or (:branch config) (:default_branch r))})
                (:body resp))
          [])))))

(defn- resolve-default-branch [config owner repo branch]
  (or branch
      (let [api-base (or (:api-base config) default-api-base)
            resp (api-get (repo-url api-base owner repo) config)]
        (if (:ok resp) (get-in resp [:body :default_branch] "main") "main"))))

(defn discover-files
  "Walk a repo's git tree and return discovered blob entries (filtered by file type).
   Returns a seq of file maps {:id :path :content-hash :size :sha :owner :repo}."
  [config owner repo branch file-types]
  (let [api-base (or (:api-base config) default-api-base)
        ref (resolve-default-branch config owner repo branch)
        resp (api-get (tree-url api-base owner repo ref) config)]
    (if (:ok resp)
      (->> (get-in resp [:body :tree])
           (filter #(= "blob" (:type %)))
           (filter #(matches-file-type? (:path %) file-types))
           (map (fn [{:keys [path sha size]}]
                  {:id (blob-id owner repo sha path)
                   :path (str owner "/" repo "/" path)
                   :content-hash sha ;; git blob sha is a stable content identity
                   :size (or size 0)
                   :sha sha
                   :owner owner
                   :repo repo
                   :kind :blob})))
      [])))

(defn discover-issues
  "List issues (excluding PRs) for a repo, optionally since a timestamp.
   Returns a seq of file maps."
  [config owner repo since]
  (let [api-base (or (:api-base config) default-api-base)
        base (issues-url api-base owner repo)
        url (cond-> (str base "?state=all&per_page=100")
              (not (str/blank? since)) (str "&since=" since))
        resp (api-get url config)]
    (if (:ok resp)
      (->> (:body resp)
           ;; GitHub returns PRs in the issues list; exclude them here.
           (remove :pull_request)
           (map (fn [{:keys [number title body updated_at]}]
                  {:id (issue-id owner repo number)
                   :path (str owner "/" repo "/issues/" number)
                   :content-hash (sha256 (str title "\n" body))
                   :size (count (str body))
                   :owner owner
                   :repo repo
                   :number number
                   :modified-at updated_at
                   :kind :issue})))
      [])))

(defn discover-prs
  "List pull requests for a repo. Returns a seq of file maps."
  [config owner repo]
  (let [api-base (or (:api-base config) default-api-base)
        url (str (pulls-url api-base owner repo) "?state=all&per_page=100")
        resp (api-get url config)]
    (if (:ok resp)
      (->> (:body resp)
           (map (fn [{:keys [number title body updated_at]}]
                  {:id (pr-id owner repo number)
                   :path (str owner "/" repo "/pulls/" number)
                   :content-hash (sha256 (str title "\n" body))
                   :size (count (str body))
                   :owner owner
                   :repo repo
                   :number number
                   :modified-at updated_at
                   :kind :pr})))
      [])))

(defn classify
  "Classify discovered files against existing hashes into new/changed/unchanged."
  [files existing-hashes]
  (map (fn [{:keys [id content-hash] :as f}]
         (let [prev (get existing-hashes id)
               status (cond
                        (nil? prev) :new
                        (= prev content-hash) :unchanged
                        :else :changed)]
           (assoc f :status status)))
       files))

;; ─── Extraction ───────────────────────────────────────────────────────────

(defn- decode-base64 [^String s]
  (let [cleaned (str/replace (or s "") #"\s" "")]
    (String. (.decode (Base64/getDecoder) cleaned) "UTF-8")))

(defn extract-blob
  "Fetch and decode file content for a blob id."
  [config {:keys [owner repo sha path]} file-id]
  (let [api-base (or (:api-base config) default-api-base)
        url (cond-> (contents-url api-base owner repo path)
              (not (str/blank? sha)) (str "?ref=" sha))
        resp (api-get url config)]
    (if (:ok resp)
      (let [body (:body resp)
            content (if (= "base64" (:encoding body))
                      (decode-base64 (:content body))
                      (str (:content body)))]
        {:id file-id
         :path (str owner "/" repo "/" path)
         :content content
         :content-hash (or sha (sha256 content))})
      {:id file-id :path path :content nil :error (:error resp)})))

(defn extract-issue
  "Fetch issue body for an issue id."
  [config {:keys [owner repo number]} file-id]
  (let [api-base (or (:api-base config) default-api-base)
        url (str (issues-url api-base owner repo) "/" number)
        resp (api-get url config)]
    (if (:ok resp)
      (let [{:keys [title body user state]} (:body resp)
            content (str "# Issue #" number ": " title "\n\n"
                         "State: " state "\n"
                         "Author: " (:login user) "\n\n"
                         (or body ""))]
        {:id file-id
         :path (str owner "/" repo "/issues/" number)
         :content content
         :content-hash (sha256 (str title "\n" body))})
      {:id file-id :path (str owner "/" repo "/issues/" number)
       :content nil :error (:error resp)})))

(defn extract-pr
  "Fetch PR description + metadata for a pr id."
  [config {:keys [owner repo number]} file-id]
  (let [api-base (or (:api-base config) default-api-base)
        url (str (pulls-url api-base owner repo) "/" number)
        resp (api-get url config)]
    (if (:ok resp)
      (let [{:keys [title body user state head base merged]} (:body resp)
            content (str "# PR #" number ": " title "\n\n"
                         "State: " state (when merged " (merged)") "\n"
                         "Author: " (:login user) "\n"
                         "Head: " (:ref head) " -> Base: " (:ref base) "\n\n"
                         (or body ""))]
        {:id file-id
         :path (str owner "/" repo "/pulls/" number)
         :content content
         :content-hash (sha256 (str title "\n" body))})
      {:id file-id :path (str owner "/" repo "/pulls/" number)
       :content nil :error (:error resp)})))

(defn extract-item
  "Route a single file-id to the right extraction strategy."
  [config file-id]
  (if-let [parsed (parse-file-id file-id)]
    (case (:kind parsed)
      :blob (extract-blob config parsed file-id)
      :issue (extract-issue config parsed file-id)
      :pr (extract-pr config parsed file-id)
      {:id file-id :content nil :error "Unknown file-id kind"})
    {:id file-id :content nil :error "Unparseable file-id"}))

;; ─── GitHub Driver ──────────────────────────────────────────────────────────

(deftype GitHubDriver [config state]
  protocol/Driver

  (discover [_this opts]
    (let [validation (validate-config config)]
      (if-not (:valid validation)
        {:total-files 0 :new-files 0 :changed-files 0
         :unchanged-files 0 :deleted-files 0 :skipped-files 0
         :files [] :error (:error validation)}
        (let [existing-hashes (or (:existing-hashes opts) {})
              file-types (normalized-file-types config)
              include-issues (get config :include-issues true)
              include-prs (get config :include-prs true)
              repos (list-repos config)
              scan-state (:repos @state {})
              all-files
              (reduce
               (fn [acc {:keys [owner name default-branch]}]
                 (let [since (or (:since opts)
                                 (get-in scan-state [(str owner "/" name) :last-scan]))
                       files (discover-files config owner name default-branch file-types)
                       issues (when include-issues
                                (discover-issues config owner name since))
                       prs (when include-prs
                             (discover-prs config owner name))]
                   (into acc (concat files issues prs))))
               []
               repos)
              classified (classify all-files existing-hashes)
              counts (frequencies (map :status classified))
              now (str (Instant/now))
              new-scan (reduce (fn [m {:keys [owner name]}]
                                 (assoc m (str owner "/" name) {:last-scan now}))
                               scan-state repos)]
          (swap! state assoc :repos new-scan)
          {:total-files (count classified)
           :new-files (get counts :new 0)
           :changed-files (get counts :changed 0)
           :unchanged-files (get counts :unchanged 0)
           :deleted-files 0
           :skipped-files 0
           :files (vec classified)}))))

  (extract [_this file-id]
    (extract-item config file-id))

  (extract-batch [this file-ids]
    (mapv #(protocol/extract this %) file-ids))

  (get-state [_this]
    @state)

  (set-state [_this new-state]
    (reset! state (or new-state {})))

  (close [_this]
    ;; Nothing to clean up
    ))

(defn create-driver
  "Create a GitHub driver instance for the given config."
  [config]
  (GitHubDriver. config (atom {:repos {}})))
