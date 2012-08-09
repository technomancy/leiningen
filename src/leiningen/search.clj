(ns leiningen.search
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [clj-http.client :as http])
  (:import (org.apache.maven.index ArtifactInfo IteratorSearchRequest MAVEN
                                   NexusIndexer)
           (org.apache.maven.index.context IndexingContext)
           (org.apache.maven.index.creator
            JarFileContentsIndexCreator MavenPluginArtifactInfoIndexCreator
            MinimalArtifactInfoIndexCreator)
           (org.apache.maven.index.expr UserInputSearchExpression)
           (org.apache.maven.index.updater IndexUpdater IndexUpdateRequest
                                           ResourceFetcher)
           (org.codehaus.plexus DefaultPlexusContainer PlexusContainer)))

(defonce container (DefaultPlexusContainer.))

(defonce indexer (.lookup container NexusIndexer))

(def ^:private default-indexers [(MinimalArtifactInfoIndexCreator.)
                                 (JarFileContentsIndexCreator.)
                                 (MavenPluginArtifactInfoIndexCreator.)])

(defn index-location [url]
  (io/file (user/leiningen-home) "indices" (string/replace url #"[:/]" "_")))

(defmacro with-context [[context-local id url] & body]
  `(let [~context-local (.addIndexingContextForced
                         indexer ~id ~url nil (index-location ~url)
                         ~url nil default-indexers)]
     (locking ~url
       (try ~@body
            (finally
              (.removeIndexingContext indexer ~context-local false))))))

;; TODO: add progress reporting back in
(defn- http-resource-fetcher []
  (let [base-url (promise)]
    (proxy [ResourceFetcher] []
      ;; TODO: handle connect/disconnect properly
      (connect [id url]
        (deliver base-url url))
      (disconnect [])
      (^java.io.InputStream retrieve [name]
        (println "Downloading" name "from" @base-url)
        (:body (http/get (str @base-url "/" name) {:throw-exceptions false
                                                   :as :stream}))))))

(defn update-index [context]
  (.fetchAndUpdateIndex (.lookup container IndexUpdater)
                        (IndexUpdateRequest. context (http-resource-fetcher))))

(defn- parse-result [result]
  (let [group-id (.groupId result)
        artifact-id (.artifactId result)
        version (.version result)
        classifier (.classifier result)
        packaging (.packaging result)
        name (if (= group-id artifact-id)
               (symbol artifact-id)
               (symbol group-id artifact-id))
        classifier-opts (and classifier [:classifier classifier])
        packaging-opts (if (not= "jar" packaging) [:packaging packaging])]
    [(pr-str (into [name version] (concat classifier-opts packaging-opts)))
     (.description result)]))

(defn- print-results [id response page]
  (when (seq response)
    (println " == Results from" id "-" "Showing page" page "/"
             (.getTotalHitsCount response))
    (doseq [[dep description] (map parse-result response)]
      (println dep description))
    (println)))

(defn search-repository [[id {:keys [url]}] query page offline?]
  (with-context [context id url]
    (when-not offline? (update-index context))
    (let [search-expression (UserInputSearchExpression. query)
          ;; TODO: support querying other fields
          artifact-id-query (.constructQuery indexer MAVEN/ARTIFACT_ID
                                             search-expression)
          request (IteratorSearchRequest. artifact-id-query context)]
      (with-open [response (.searchIterator indexer request)]
        (print-results id response page)))))

(defn ^:no-project-needed search
  "Search remote maven repositories for matching jars.

The first run will download a set of indices, which will take a very long time.

The query is evaluated as a lucene search. You can search for simple string
matches or do more advanced queries such as this:

  $ lein search \"clojure AND http AND NOT g:org.clojars*\"

Also accepts a second parameter for fetching successive pages."
  ([project query] (search project query 1))
  ([project query page]
     ;; TODO: still some issues with central
     (doseq [repo (reverse (:repositories project (:repositories project/defaults)))
             :let [page (Integer. page)]]
       ;; TODO: bring back pagination
       (search-repository repo query page (:offline? project)))))