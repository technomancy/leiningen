(ns leiningen.search
  (:use [leiningen.core :only [repositories-for user-settings]]
        [leiningen.util.file :only [delete-file-recursively]]
        [leiningen.util.paths :only [leiningen-home]])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clucy.core :as clucy])
  (:import (java.util.zip ZipFile)
           (java.net URL)))

;;; Fetching Indices

(defn- unzip [source target-dir]
  (let [zip (ZipFile. source)
        entries (enumeration-seq (.entries zip))
        target-file #(io/file target-dir (.getName %))]
    (doseq [entry entries :when (not (.isDirectory entry))
            :let [f (target-file entry)]]
      (.mkdirs (.getParentFile f))
      (io/copy (.getInputStream zip entry) f))))

(defn index-location [url]
  (io/file (leiningen-home) "indices" (string/replace url #"[:/]" "_")))

(defn remote-index-url [url]
  (URL. (format "%s/.index/nexus-maven-repository-index.zip" url)))

(defn- download-index [[id {url :url}]]
  (with-open [stream (.openStream (remote-index-url url))]
    (println "Downloading index from" id "-" url)
    (let [tmp (java.io.File/createTempFile "lein" "index")]
      (try (io/copy stream tmp)
           (unzip tmp (index-location url))
           (finally (.delete tmp))))))

(defn- download-needed? [[id {:keys [url]}]]
  (not (.exists (index-location url))))

(defn ensure-fresh-index [repository]
  (try (when (download-needed? repository)
         (download-index repository))
       true
       (catch java.io.IOException _
         false)))

;;; Searching

(def ^{:private true} page-size (:search-page-size (user-settings) 10))

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

(defn parse-result [{:keys [u d]}]
  (let [[group artifact version classifier] (.split u "\\|")
        group (if (not= group artifact) group)
        identifier [(symbol group artifact) (format "\"%s\"" version)]]
    (if d
      [identifier d]
      [identifier])))

(defn- print-results [[id] results page]
  (when (seq results)
    (println " == Results from" id "-" "Showing page" page "/"
             ;; TODO: divide by page size?
             (:_total-hits (meta results) page-size) "total")
    (doseq [result (map parse-result results)]
      (apply println result))
    (prn)))

(defn ^{:help-arglists '([query] [query page])} search
  "Search remote repositories.

The first run will download a set of indices, which will take a
while. Pass in --update as the query to force a fresh download of all
indices. Also accepts a second parameter for fetching successive pages."
  ;; support running outside project
  ([query] (search {} query))
  ([project-or-query query-or-page]
     ;; this arity does double-duty: simple query inside project or
     ;; query+page outside project
     (if (string? project-or-query)
       (search {} project-or-query query-or-page)
       (search project-or-query query-or-page 1)))
  ([project query page]
     ;; you know what would be just super? pattern matching.
     (if (= "--update" query)
       (doseq [[_ {url :url} :as repo] (repositories-for project)]
         (delete-file-recursively (index-location url) :silently)
         (ensure-fresh-index repo))
       (doseq [repo (repositories-for project)
               :let [page (Integer. page)]]
         (print-results repo (search-repository repo query page) page)))))
