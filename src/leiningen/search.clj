(ns leiningen.search
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [clucy.core :as clucy]
            [clj-http.client :as http])
  (:import (java.util.zip ZipFile)
           (java.net URL)
           (java.io File InputStream OutputStream FileOutputStream)))

;;; Fetching Indices

(defn- unzip [source target-dir]
  (let [zip (ZipFile. source)
        entries (enumeration-seq (.entries zip))
        target-file #(io/file target-dir (.getName %))]
    (doseq [entry entries :when (not (.isDirectory entry))
            :let [f (target-file entry)]]
      (.mkdirs (.getParentFile f))
      (io/copy (.getInputStream zip entry) f))))

(defn ^:internal index-location [url]
  (io/file (user/leiningen-home) "indices" (string/replace url #"[:/]" "_")))

(defn ^:internal remote-index-url [url]
  (URL. (format "%s/.index/nexus-maven-repository-index.zip" url)))

(defn- download [^URL url ^OutputStream out-stream  & {:keys [callback]}]
  (let [resp (http/get (str url) {:as :stream
                                  :headers {"User-Agent" (main/user-agent)}})
        content-len (try (Long/valueOf
                          (get-in resp [:headers "content-length"]))
                         (catch Exception _))
        in ^InputStream (:body resp) ;closes itself after completion
        buf (byte-array 1024)]
    (loop [cnt 0]
      (let [size (.read in buf)]
        (when (pos? size)
          (let [cnt* (+ cnt size)]
            (.write out-stream buf 0 size)
            (when callback
              (callback {:byte-count cnt*
                         :content-len content-len
                         :percentage (when content-len
                                       (int (* 100 (/ cnt* content-len))))}))
            (recur cnt*)))))))

(defn- download-index [[id {url :url}]]
  (main/info "Downloading index from" id "-" url)
  (main/info "This can take a very, very long time. While you wait you might")
  (main/info "be interested in searching via the web interfaces at")
  (main/info "http://search.maven.org or http://clojars.org.")
  (main/info "0%...")
  (flush)
  (let [index-url ^URL (remote-index-url url)
        tmp (File/createTempFile "lein" "index")
        tmp-stream (FileOutputStream. tmp)
        progress (atom 0)
        callback (fn [{:keys [percentage]}]
                   (when (and main/*info* (not= percentage @progress))
                     (reset! progress percentage)
                     (print (str "\r" percentage "%..."))
                     (flush)))]
    (try (if (= "file" (.getProtocol index-url))
           (io/copy (.openStream index-url) tmp-stream)
           (download index-url tmp-stream :callback callback))
         (unzip tmp (index-location url))
         (finally (.delete tmp))))
  (main/info))

(defn- download-needed? [[id {:keys [url]}]]
  (not (.exists (index-location url))))

(defn ^:internal ensure-fresh-index [repository]
  (try (when (download-needed? repository)
         (download-index repository))
       true
       (catch java.io.IOException _
         false)))

;;; Searching

(def ^:private page-size (:search-page-size (:user (user/profiles)) 25))

(defn search-repository [[id {:keys [url]} :as repo] query page]
  (if (ensure-fresh-index repo)
    (let [location (.getAbsolutePath (index-location url))
          fetch-count (* page page-size)
          offset (* (dec page) page-size)
          results (clucy/search (clucy/disk-index location)
                                query fetch-count :default-field :a)]
      (with-meta (drop offset results) (meta results)))
    (binding [*out* *err*]
      (println "Warning: couldn't download index for" url))))

(defn ^:internal parse-result [{:keys [u d]}]
  (let [[group artifact version classifier] (.split u "\\|")
        group (if (not= group artifact) group)
        identifier [(symbol group artifact) (format "\"%s\"" version)]]
    (if d
      [identifier d]
      [identifier])))

(defn- print-results [[id] results page]
  (when (seq results)
    (println " == Results from" id "-" "Showing page" page "/"
             (-> results meta :_total-hits (/ page-size) Math/ceil int) "total")
    (doseq [result (map parse-result results)]
      (apply println result))
    (println)))

(defn ^:no-project-needed search
  "Search remote maven repositories for matching jars.

The first run will download a set of indices, which will take a very long time.

The query is evaluated as a lucene search. You can search for simple string
matches or do more advanced queries such as this:

  $ lein search \"clojure AND http AND NOT g:org.clojars*\"

Also accepts a second parameter for fetching successive pages."
  ([project query] (search project query 1))
  ([project query page]
     (doseq [repo (:repositories project (:repositories project/defaults))
             :let [page (Integer. page)]]
       (print-results repo (search-repository repo query page) page))))
