(ns leiningen.search
  "Search Central and Clojars for published artifacts."
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main])
  (:import (java.net URLEncoder)
           (javax.xml.parsers SAXParser)
           (org.xml.sax.helpers DefaultHandler)))

(defn- decruft-central-xml [content]
  (zipmap (map #(get-in % [:attrs :name]) content)
          (map #(get-in % [:content 0]) content)))

;; replace clojure.xml version with one that doesn't do illegal access
(defn- startparse [^String url ^DefaultHandler ch]
  (let [^SAXParser p (xml/disable-external-entities (xml/sax-parser))]
    (.parse p url ch)))

(defn parse [url]
  (try (xml/parse url startparse)
       (catch Exception e
         (main/warn "Could not retrieve search results from" url "because of"
                    (class e))
         (when main/*debug*
           (.printStackTrace e)))))

(defn search-central [query]
  (let [url (str "https://search.maven.org/solrsearch/select?wt=xml&q=" query)]
    (doseq [doc (get-in (parse url) [:content 1 :content])]
      (let [result (decruft-central-xml (:content doc))
            dep (if (= (result "a") (result "g"))
                  (result "a")
                  (str (result "g") "/" (result "a")))]
        (println (format "[%s \"%s\"]" dep (result "latestVersion")))))))

(defn search-clojars [query]
  (let [url (str "https://clojars.org/search?format=xml&q=" query)]
    (doseq [{result :attrs} (:content (parse url))]
      (let [dep (if (= (result :jar_name) (result :group_name))
                  (result :jar_name)
                  (str (result :group_name) "/" (result :jar_name)))]
        (println (format "[%s \"%s\"]" dep (result :version)))
        (when-let [desc (and (not= (string/trim (result :description "")) "")
                             (result :description))]
          (println " " (string/trim (first (string/split desc #"\n")))))))))

(defn ^:no-project-needed search
  "Search Central and Clojars for published artifacts."
  [project ^String query]
  (let [project (or project (project/make {}))
        repos (into {} (:repositories project))]
    (doseq [[repo searcher] [["central" search-central]
                             ["clojars" search-clojars]]]
      (when (repos repo)
        (try (println "Searching" repo "...")
             (searcher (URLEncoder/encode query "UTF-8"))
             (catch java.io.IOException e
               (binding [*out* *err*]
                 (if (re-find #"HTTP response code: (400|505)" (str e))
                   (println "Query syntax unsupported.")
                   (println "Remote error" (.getMessage e))))))))))
