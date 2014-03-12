(ns leiningen.search
  "Search remote maven repositories for matching jars."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.user :as user]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clj-http.client :as http])
  (:import (org.apache.maven.index IteratorSearchRequest MAVEN NexusIndexer)
           (org.apache.maven.index.context IndexingContext)
           (org.apache.maven.index.creator JarFileContentsIndexCreator
                                           MavenPluginArtifactInfoIndexCreator
                                           MinimalArtifactInfoIndexCreator)
           (org.apache.maven.index.expr UserInputSearchExpression)
           (org.apache.maven.index.updater IndexUpdater IndexUpdateRequest
                                           ResourceFetcher)
           (org.codehaus.plexus DefaultPlexusContainer PlexusContainer)
           (leiningen extstream)))

(defonce container (DefaultPlexusContainer.))

(defonce indexer (.lookup container NexusIndexer))

(def ^:private default-indexers [(MinimalArtifactInfoIndexCreator.)
                                 (JarFileContentsIndexCreator.)
                                 (MavenPluginArtifactInfoIndexCreator.)])

(defn index-location [url]
  (io/file (user/leiningen-home) "indices" (string/replace url #"[:/]" "_")))

(defn- add-context [[id {:keys [url]}]]
  (.addIndexingContextForced indexer id url nil (index-location url)
                             url nil default-indexers))

(defn- remove-context [context]
  (.removeIndexingContext indexer context false))


(defn- http-resource-fetcher []
  (let [base-url (promise)
        stream (promise)]
    (proxy [ResourceFetcher] []
      ;; TODO: handle connect/disconnect properly
      (connect [id url]
        (deliver base-url url))
      (disconnect []
        (.close @stream))
      (^java.io.InputStream retrieve [name]
        (println "Downloading" (str @base-url "/" name))
        (let [r (http/get (str @base-url "/" name)
                                 {:throw-exceptions false :as :stream})
              s (extstream.
                 (:body r)
                 (Long/parseLong (get (:headers r) "content-length")))]
          (deliver stream s)
          s)))))

(defn update-index [context]
  (.fetchAndUpdateIndex (.lookup container IndexUpdater)
                        (IndexUpdateRequest. context (http-resource-fetcher))))

(defn- refresh? [url project]
  (if-not (:offline? project)
    (< (.lastModified (io/file (index-location url) "timestamp"))
       (- (System/currentTimeMillis) 86400000))))

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
     (or (.description result) "")]))

(def ^:private page-size (:search-page-size (:user (user/profiles)) 50))

(defn- print-results [response page]
  (when (seq response)
    (println " == Showing page" page "/"
             (-> (.getTotalHitsCount response) (/ page-size) Math/ceil int))
    (doseq [[dep description] (map parse-result response)]
      (println dep description))
    (println)))

(def ^{:private true}
  field-splitter-re #"^([a-z]+)(?:\:)(.+)")

(defn- lookup-lucene-field-for
  [^String s]
  (case (-> (or s "") .toLowerCase .trim)
    ""            MAVEN/ARTIFACT_ID
    "id"          MAVEN/ARTIFACT_ID
    "a"           MAVEN/ARTIFACT_ID
    "artifact-id" MAVEN/ARTIFACT_ID
    "artifact_id" MAVEN/ARTIFACT_ID
    "g"           MAVEN/GROUP_ID
    "group"       MAVEN/GROUP_ID
    "group-id"    MAVEN/GROUP_ID
    "group_id"    MAVEN/GROUP_ID
    "d"           MAVEN/DESCRIPTION
    "desc"        MAVEN/DESCRIPTION
    "description" MAVEN/DESCRIPTION
    (throw (IllegalArgumentException. (format "search over the field %s is not supported; known fields: id, description (aliased as d), group (aliased as g)" s)))))

(defn- split-query
  "Splits \"field:query\" into \"field\" and \"query\""
  [^String s]
  (let [[_ field query] (re-find field-splitter-re s)]
    [(lookup-lucene-field-for field)
     (or query s)]))

(defn search-repository [query contexts page]
  (let [[field q]         (split-query query)
        search-expression (UserInputSearchExpression. q)
        constructed-query (.constructQuery indexer field
                                           search-expression)
        request (doto (IteratorSearchRequest. constructed-query contexts)
                  (.setStart (* (dec page) page-size))
                  (.setCount page-size))]
    (with-open [response (.searchIterator indexer request)]
      (println (format "Searching over %s..." (.getDescription field)))
      (print-results response page))))

(defn ^:no-project-needed search
  "Search remote maven repositories for matching jars.

The first run will download a set of indices, which will take a very long time.

The query is evaluated as a lucene search. You can search for simple string
matches or do more advanced queries such as this:

  $ lein search clojure
  $ lein search description:crawl
  $ lein search group:clojurewerkz
  $ lein search \"Riak client\"

Also accepts a second parameter for fetching successive pages."
  ([project query] (search project query 1))
  ([project query page]
     ;; Maven's indexer requires over 1GB of free space for a <100M index
     (let [orig-tmp (System/getProperty "java.io.tmpdir")
           new-tmp (io/file (user/leiningen-home) "indices" "tmp")
           project (or project (project/make {}))
           contexts (doall (map add-context (:repositories project)))]
       (try
         (.mkdirs new-tmp)
         (System/setProperty "java.io.tmpdir" (str new-tmp))
         (doseq [context contexts]
           (when (refresh? (.getRepositoryUrl context) project)
             (do
               (println "Updating the search index. This may take a few minutes...")
               (update-index context))))
         ;; TODO: improve error message when page isn't numeric
         (search-repository query contexts (Integer. page))
         (finally
           (doall (map remove-context contexts))
           (System/setProperty "java.io.tmpdir" orig-tmp))))))
