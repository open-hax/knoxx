(defn looks-like-url?  [value]
  (and (string? value)
       (or (str/starts-with? value "http://")
           (str/starts-with? value "https://"))))
(defn media-url?  [value]
  (and (string? value)
       (or (looks-like-url? value)
           (str/starts-with? value "/"))))

(defn data-url?  [value]
  (and (string? value) (str/starts-with? value "data:")))
