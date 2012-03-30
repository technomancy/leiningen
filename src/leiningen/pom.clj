(ns leiningen.pom
  "Write a pom.xml file to disk for Maven interoperability."
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.data.xml :as xml]
            [useful.string :as useful]))

(def ^:private sonatype-url
  "http://oss.sonatype.org/content/repositories/releases")

;; git scm

(defn- read-git-ref
  "Reads the commit SHA1 for a git ref path."
  [git-dir ref-path]
  (.trim (slurp (str (io/file git-dir ref-path)))))

(defn- read-git-head
  "Reads the value of HEAD and returns a commit SHA1."
  [git-dir]
  (let [head (.trim (slurp (str (io/file git-dir "HEAD"))))]
    (if-let [ref-path (second (re-find #"ref: (\S+)" head))]
      (read-git-ref git-dir ref-path)
      head)))

(defn- read-git-origin
  "Reads the URL for the remote origin repository."
  [git-dir]
  (with-open [rdr (io/reader (io/file git-dir "config"))]
    (->> (map #(.trim %) (line-seq rdr))
         (drop-while #(not= "[remote \"origin\"]" %))
         (next)
         (take-while #(not (.startsWith % "[")))
         (map #(re-matches #"url\s*=\s*(\S*)\s*" %))
         (filter identity)
         (first)
         (second))))

(defn- parse-github-url
  "Parses a GitHub URL returning a [username repo] pair."
  [url]
  (when url
    (next
     (or
      (re-matches #"(?:git@)?github.com:([^/]+)/([^/]+).git" url)
      (re-matches #"[^:]+://(?:git@)?github.com/([^/]+)/([^/]+).git" url)))))

(defn- github-urls [url]
  (when-let [[user repo] (parse-github-url url)]
    {:public-clone (str "git://github.com/" user "/" repo ".git")
     :dev-clone (str "ssh://git@github.com/" user "/" repo ".git")
     :browse (str "https://github.com/" user "/" repo)}))

(defn- make-git-scm [git-dir]
  (try
    (let [origin (read-git-origin git-dir)
          head (read-git-head git-dir)
          urls (github-urls origin)]
      [:scm
       (when (:public-clone urls)
         [:connection (str "scm:git:" (:public-clone urls))])
       (when (:dev-clone urls)
         [:developerConnection (str "scm:git:" (:dev-clone urls))])
       [:tag head]
       [:url (:browse urls)]])
    (catch java.io.FileNotFoundException e
      nil)))

(def ^{:doc "A notice to place at the bottom of generated files."} disclaimer
     "\n<!-- This file was autogenerated by Leiningen.
  Please do not edit it directly; instead edit project.clj and regenerate it.
  It should not be considered canonical data. For more information see
  https://github.com/technomancy/leiningen -->\n")

(defn- make-test-scope [[dep version opts]]
  [dep version (assoc opts :scope "test")])

(defn- pomify [key]
  (->> key name useful/camelize keyword))

(defmulti ^:private xml-tags
  (fn [tag value] (keyword "leiningen.pom" (name tag))))

(defmethod xml-tags :default
  ([tag value]
     (when value
       [(pomify tag) value])))

(defmethod xml-tags ::list
  ([tag values]
     [(pomify tag) (map (partial xml-tags
                                 (-> tag name (s/replace #"ies$" "y") keyword))
                        values)]))

(doseq [c [::dependencies ::repositories ::pluginRepositories]]
  (derive c ::list))

(defmethod xml-tags ::exclusions
  [tag values]
  (when values
    [:exclusions
     (map
      (fn [dep]
        [:exclusion (map (partial apply xml-tags)
                         {:group-id (namespace dep)
                          :artifact-id (name dep)})])
      values)]))

(defmethod xml-tags ::dependency
  ([_ [dep version & opts]]
     (let [opts (apply hash-map opts)]
       [:dependency
        (map (partial apply xml-tags)
             {:group-id (or (namespace dep) (name dep))
              :artifact-id (name dep)
              :version version
              :classifier (:classifier opts)
              :exclusions (:exclusions opts)
              :scope (:scope opts)})])))

(defmethod xml-tags ::repository
  ([_ [id opts]]
     [:repository [:id id] [:url (:url opts)]]))

(defmethod xml-tags ::pluginRepository
  ([_ [id opts]]
     [:pluginRepository [:id id] [:url (:url opts)]]))

(defmethod xml-tags ::license
  ([_ opts]
     [:licenses
      [:license (for [key [:name :url :distribution :comments]
                      :let [val (get opts key)] :when val]
                  [key (name val)])]]))

(defmethod xml-tags ::build
  ([_ project]
     (let [dev-project (project/merge-profiles project [:dev])
           [src & extra-src] (:source-paths project)
           [test & extra-test] (:test-paths dev-project)
           aot (:aot project)]
       [:build
        [:sourceDirectory src]
        [:testSourceDirectory test]
        (if-let [resources (:resource-paths project)]
          (when (not (empty? resources))
            (vec (concat [:resources]
                         (map (fn [x] [:resource [:directory x]]) resources)))))
        (if-let [resources (:resource-paths dev-project)]
          (when (not (empty? resources))
            (vec (concat [:testResources]
                         (map (fn [x] [:testResource [:directory x]]) resources)))))
        (if-let [extensions (:extensions project)]
          (when (not (empty? extensions))
            (vec (concat [:extensions]
                         (map (fn [[dep version]] [:extension
                                                  [:artifactId (name dep)]
                                                  [:groupId (or (namespace dep) (name dep))]
                                                  [:version version]])
                              extensions)))))
        (when (or (not (empty? extra-src))
                  (not (empty? extra-test)))
          [:plugins
           [:plugin
            [:groupId "org.codehaus.mojo"]
            [:artifactId "build-helper-maven-plugin"]
            [:version "1.7"]
            [:executions
             (when (not (empty? extra-src))
               [:execution
                [:id "add-source"]
                [:phase "generate-sources"]
                [:goals [:goal "add-source"]]
                [:configuration
                 (vec (concat [:sources]
                              (map (fn [x] [:source x]) extra-src)))]])
             (when (not (empty? extra-src))
               [:execution
                [:id "add-test-source"]
                [:phase "generate-test-sources"]
                [:goals [:goal "add-test-source"]]
                [:configuration
                 (vec (concat [:sources]
                              (map (fn [x] [:source x]) extra-test)))]])]]
           [:plugin
            [:groupId "org.cloudhoist.plugin"]
            [:artifactId "zi"]
            [:version "0.4.5"]
            [:executions
             (when-not (:omit-source project)
               [:execution
                [:id "default-resources"]
                [:phase "process-resources"]
                [:goals [:goal "resources"]]])
             [:execution
              [:id "default-test-resources"]
              [:phase "process-test-resources"]
              [:goals [:goal "testResources"]]]
             [:execution
              [:id "default-test"]
              [:phase "test"]
              [:goals [:goal "test"]]]
             (when-not (empty? aot)
               [:execution
                [:id "default-compile"]
                [:phase "compile"]
                [:goals [:goal "compile"]]
                (when-not (= aot :all)
                  [:configuration
                   (vec
                  (concat
                   [:includes]
                   (map
                    (fn [x]
                      [:include (-> x (s/replace "-" "_") (s/replace "." "/"))])
                    aot)))])])]]])])))

(defmethod xml-tags ::parent
  ([_ [dep version & opts]]
     (let [opts (apply hash-map opts)]
       [:parent
        [:artifactId (name dep)]
        [:groupId (or (namespace dep) (name dep))]
        [:version version]
        [:relativePath (:relative-path opts)]])))

(defmethod xml-tags ::mailing-list
  ([_ opts]
     [:mailingLists
      [:mailingList
       [:name (:name opts)]
       [:subscribe (:subscribe opts)]
       [:unsubscribe (:unsubscribe opts)]
       [:post (:post opts)]
       [:archive (:archive opts)]
       (if-let [other-archives (:other-archives opts)]
         (when other-archives
           (vec (concat [:otherArchives]
                        (map (fn [x] [:otherArchive x]) other-archives)))))]]))

(defn- add-exclusions [exclusions [dep version & opts]]
  (concat [dep version]
          (apply concat (update-in (apply hash-map opts)
                                   [:exclusions]
                                   #(concat exclusions %)))))

(defmethod xml-tags ::project
  ([_ project]
     (list
      [:project {:xsi:schemaLocation "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.0.xsd"
                 :xmlns "http://maven.apache.org/POM/4.0.0"
                 :xmlns:xsi "http://www.w3.org/2001/XMLSchema-instance"}
       [:modelVersion "4.0.0"]
       (when (:parent project) (xml-tags :parent (:parent project)))
       [:groupId (:group project)]
       [:artifactId (:name project)]
       [:version (:version project)]
       (when (:classifier project) [:classifier (:classifier project)])
       [:name (:name project)]
       [:description (:description project)]
       [:url (:url project)]
       (xml-tags :license (:license project))
       (when (:mailing-list project)
         (xml-tags :mailing-list (:mailing-list project)))
       (make-git-scm (io/file (:root project) ".git"))
       (xml-tags :build project)
       (xml-tags :repositories (:repositories project))
       (xml-tags :pluginRepositories {"sonatype" {:url sonatype-url}})
       (xml-tags :dependencies (map (partial add-exclusions (:exclusions project))
                                    ;; TODO: include :dependencies
                                    ;; from :dev profile as test-scoped.
                                    (:dependencies project)))])))

(defn snapshot? [project]
  (re-find #"SNAPSHOT" (:version project)))

(defn check-for-snapshot-deps [project]
  (when (and (not (snapshot? project))
             (not (System/getenv "LEIN_SNAPSHOTS_IN_RELEASE"))
             (some #(re-find #"SNAPSHOT" (second %)) (:dependencies project)))
    (main/abort "Release versions may not depend upon snapshots."
                "\nFreeze snapshots to dated versions or set the"
                "LEIN_SNAPSHOTS_IN_RELEASE environment variable to override.")))

(defn make-pom
  ([project] (make-pom project false))
  ([project disclaimer?]
     (when-not (check-for-snapshot-deps project)
       (str
        (xml/indent-str
         (xml/sexp-as-element
          (xml-tags :project (:without-profiles (meta project) project))))
        (when disclaimer?
          disclaimer)))))

(defn make-pom-properties [project]
  (with-open [baos (java.io.ByteArrayOutputStream.)]
    (.store (doto (java.util.Properties.)
              (.setProperty "version" (:version project))
              (.setProperty "groupId" (:group project))
              (.setProperty "artifactId" (:name project)))
              baos "Leiningen")
    (str baos)))

(defn ^{:help-arglists '([])} pom
  "Write a pom.xml file to disk for Maven interoperability."
  ([project pom-location silently?]
     (when-let [pom (make-pom project true)]
       (let [pom-file (io/file (:target-path project) pom-location)]
         (.mkdirs (.getParentFile pom-file))
         (with-open [pom-writer (io/writer pom-file)]
           (.write pom-writer pom))
         (when-not silently? (println "Wrote" (str pom-file)))
         (.getAbsolutePath pom-file))))
  ([project pom-location] (pom project pom-location false))
  ([project] (pom project "pom.xml")))
