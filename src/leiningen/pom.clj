(ns leiningen.pom
  "Write a pom.xml file to disk for Maven interoperability."
  (:import java.io.IOException)
  (:require [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as s]
            [clojure.java.shell :as sh]
            [clojure.data.xml :as xml]
            [clojure.data.xml.name :as name]
            [leiningen.core.classpath :as classpath]))

(def pom-uri "https://maven.apache.org/POM/4.0.0")

(def ^:private xsi-uri "http://www.w3.org/2001/XMLSchema-instance")

(xml/alias-uri 'pom pom-uri
               'xsi xsi-uri)

(defn- relativize [project]
  (let [root (str (:root project) (System/getProperty "file.separator"))]
    (reduce #(update-in %1 [%2]
                        (fn [xs]
                          (if (sequential? xs)
                            (vec (for [x xs]
                                   (.replace x root "")))
                            (and xs (.replace xs root "")))))
            project
            [:target-path :compile-path :source-paths :test-paths
             :resource-paths :java-source-paths])))

;; git scm

(defn- resolve-git-dir [project]
  (let [alternate-git-root (io/file (get-in project [:scm :dir]))
        git-dir-file (io/file (or alternate-git-root (:root project)) ".git")]
    (if (.isFile git-dir-file)
      (io/file (second (re-find #"gitdir: (\S+)" (slurp (str git-dir-file)))))
      git-dir-file)))

(defn- read-git-ref
  "Reads the commit SHA1 for a git ref path, or nil if no commit exist."
  [git-dir ref-path]
  (let [ref (io/file git-dir ref-path)]
    (if (.exists ref)
      (.trim (slurp ref))
      nil)))

(defn- read-git-head-file
  "Reads the current value of HEAD by attempting to read .git/HEAD, returning
  the SHA1 or nil if none exists."
  [git-dir]
  (let [head (.trim (slurp (str (io/file git-dir "HEAD"))))]
                           (if-let [ref-path (second (re-find #"ref: (\S+)" head))]
                             (read-git-ref git-dir ref-path))))

(defn- read-git-head
  "Reads the value of HEAD and returns a commit SHA1, or nil if no commit
  exist."
  [git-dir]
  (try
    (let [git-ref (sh/sh "git" "rev-parse" "HEAD" :dir git-dir)]
      (if (= (:exit git-ref) 0)
        (.trim (:out git-ref))
        (read-git-head-file git-dir)))
    (catch IOException e (read-git-head-file git-dir))))

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
  (if url
    (next
     (or (re-matches #"(?:[A-Za-z-]{2,}@)?github.com:([^/]+)/([^/]+).git" url)
         (re-matches #"[^:]+://(?:[A-Za-z-]{2,}@)?github.com/([^/]+)/([^/]+?)(?:.git)?" url)))))

(defn- github-urls [url]
  (if-let [[user repo] (parse-github-url url)]
    {:public-clone (str "git://github.com/" user "/" repo ".git")
     :dev-clone (str "ssh://git@github.com/" user "/" repo ".git")
     :browse (str "https://github.com/" user "/" repo)}))

(defn- make-git-scm-map [git-dir]
  (try
    (let [origin (read-git-origin git-dir)
          head (read-git-head git-dir)
          urls (github-urls origin)]
      (cond-> {:url (:browse urls)}
        (:public-clone urls) (assoc :connection (str "scm:git:" (:public-clone urls)))
        (:dev-clone urls)    (assoc :developerConnection (str "scm:git:" (:dev-clone urls)))
        head                 (assoc :tag head)))
    (catch java.io.FileNotFoundException _)))

(def disclaimer
  "A notice to place at the bottom of generated files."
  "\n<!-- This file was autogenerated by Leiningen.
  Please do not edit it directly; instead edit project.clj and regenerate it.
  It should not be considered canonical data. For more information see
  https://codeberg.org/leiningen/leiningen -->\n")

(defn- camelize [string]
  (s/replace string #"[-_](\w)" (comp s/upper-case second)))

(defn- pomify [key]
  (->> key name camelize (xml/qname pom-uri)))

(defn- pomify-sexp [x]
  (cond
    (vector? x) (let [[tag & [attrs & content :as all-content]] x
                      tag (cond-> tag (not (name/namespaced? tag)) pomify)]
                  (if (map? attrs)
                    (into [tag attrs] (map pomify-sexp) content)
                    (into [tag] (map pomify-sexp) all-content)))
    (seq? x) (map pomify-sexp x)
    :else x))

(defmulti ^:private xml-tags
  (fn [tag value] (keyword "leiningen.pom" (name tag))))

(defn- guess-scm
  "Returns the name of the SCM used in project.clj or \"auto\" if nonexistant.
  Example: :scm {:name \"git\" :tag \"deadbeef\"}"
  [project]
  (or (-> project :scm :name) "auto"))

(defn- xmlify
  "Converts the map identified by :scm"
  [scm]
  (map #(xml-tags (first %) (second %)) scm))

(defn- make-project-scm-map [project]
  (select-keys (:scm project) [:url :connection
                               :tag :developerConnection]))

(defn- write-scm-tag
  "Write the <scm> tag for pom.xml.
  Retains backwards compatibility without an :scm map."
  [scm project]
  (->> (case scm
         "auto" (make-git-scm-map (resolve-git-dir project))
         "git" (merge (make-git-scm-map (resolve-git-dir project))
                      (make-project-scm-map project))
         ; else
         (make-project-scm-map project))
       xmlify
       (xml-tags :scm)))

(defmethod xml-tags :default
  ([tag value]
     (and value [(pomify tag) value])))

(defmethod xml-tags ::list
  ([tag values]
     [(pomify tag) (map (partial xml-tags
                                 (-> tag name (s/replace #"ies$" "y") keyword))
                        values)]))

(doseq [c [::dependencies ::repositories]]
  (derive c ::list))

(defmethod xml-tags ::exclusions
  [tag values]
  (and (seq values)
       [::pom/exclusions
        (for [exclusion-spec values
              :let [[dep & {:keys [classifier extension]}]
                    (if (symbol? exclusion-spec)
                      [exclusion-spec]
                      exclusion-spec)]]
          [::pom/exclusion (map (partial apply xml-tags)
                                (merge (project/artifact-map dep)
                                       {:classifier classifier
                                        :type extension}))])]))

(defmethod xml-tags ::dependency
  ([_ [dep version & {:keys [optional classifier
                             exclusions scope
                             extension]}]]
     [::pom/dependency
      (map (partial apply xml-tags)
           {:group-id (or (namespace dep) (name dep))
            :artifact-id (name dep)
            :version version
            :optional optional
            :classifier classifier
            :type extension
            :exclusions exclusions
            :scope scope})]))

(defn- policy-tags [type opts]
  (seq (keep (partial apply xml-tags)
             {:enabled (str (if (nil? (type opts))
                              true
                              (boolean
                               (type opts))))
              :update-policy (or (some-> opts type :update name)
                                 (some-> opts :update name))
              :checksum-policy (or (some-> opts type :checksum name)
                                   (some-> opts :checksum name))})))

(defmethod xml-tags ::repository
  ([_ [id opts]]
     [::pom/repository
      (map (partial apply xml-tags)
           {:id id
            :url (:url opts)
            :snapshots (policy-tags :snapshots opts)
            :releases (policy-tags :releases opts)})]))

(defmethod xml-tags ::license
  ([_ opts]
     (and opts
          (if-let [tags (if (string? opts)
                          [::pom/name opts]
                          (seq (for [key [:name :url :distribution :comments]
                                     :let [val (opts key)] :when val]
                                 [(pomify key) (name val)])))]
            [::pom/license tags]))))

(defn- license-tags [project]
  (seq (concat (for [k [:license :licence]
                     :let [l (xml-tags :license (get project k))]
                     :when l]
                 l)
               (keep (partial xml-tags :license) (:licenses project))
               (keep (partial xml-tags :license) (:licences project)))))

(defn- resource-tags [project type]
  (if-let [resources (seq (:resource-paths project))]
    (let [types (pomify (str (name type) "s"))]
      (vec (concat [types]
                   (for [resource resources]
                     [(pomify type) [::pom/directory resource]]))))))

(defmethod xml-tags ::build
  ([_ [project test-project]]
     (let [[src & extra-src] (concat (:source-paths project)
                                     (:java-source-paths project))
           [test & extra-test] (:test-paths test-project)]
       [::pom/build
        [::pom/sourceDirectory src]
        (xml-tags :testSourceDirectory test)
        (resource-tags project :resource)
        (resource-tags test-project :testResource)
        (if-let [extensions (seq (:extensions project))]
          (vec (concat [::pom/extensions]
                       (for [[dep version] extensions]
                         [::pom/extension
                          [::pom/artifactId (name dep)]
                          [::pom/groupId (or (namespace dep) (name dep))]
                          [::pom/version version]]))))
        [::pom/directory (:target-path project)]
        [::pom/outputDirectory (:compile-path project)]
        [::pom/plugins
            (if-let [plugins (seq (:pom-plugins project))]
                           (for [[dep version plugin-addition] plugins]
                             [::pom/plugin
                              [::pom/groupId (or (namespace dep) (name dep))]
                              [::pom/artifactId (name dep)]
                              [::pom/version version]
                              (pomify-sexp
                                (cond
                                  (map? plugin-addition) (seq plugin-addition)
                                  (vector? plugin-addition) (seq (apply hash-map plugin-addition))
                                  (list? plugin-addition) (vec plugin-addition)))
                           ]
                          ))
        (if (or (seq extra-src) (seq extra-test))
           [::pom/plugin
            [::pom/groupId "org.codehaus.mojo"]
            [::pom/artifactId "build-helper-maven-plugin"]
            [::pom/version "1.7"]
            [::pom/executions
             (if (seq extra-src)
               [::pom/execution
                [::pom/id "add-source"]
                [::pom/phase "generate-sources"]
                [::pom/goals [::pom/goal "add-source"]]
                [::pom/configuration
                 (vec (concat [::pom/sources]
                              (map (fn [x] [::pom/source x]) extra-src)))]])
             (if (seq extra-test)
               [::pom/execution
                [::pom/id "add-test-source"]
                [::pom/phase "generate-test-sources"]
                [::pom/goals [::pom/goal "add-test-source"]]
                [::pom/configuration
                 (vec (concat [::pom/sources]
                              (map (fn [x] [::pom/source x]) extra-test)))]])]])]])))

(defmethod xml-tags ::parent
  ([_ [dep version & opts]]
     (let [opts (apply hash-map opts)]
       [::pom/parent
        [::pom/artifactId (name dep)]
        [::pom/groupId (or (namespace dep) (name dep))]
        [::pom/version version]
        [::pom/relativePath (:relative-path opts)]])))

(defmethod xml-tags ::mailing-list
  ([_ opts]
     (if opts
       [::pom/mailingLists
        [::pom/mailingList
         [::pom/name (:name opts)]
         [::pom/subscribe (:subscribe opts)]
         [::pom/unsubscribe (:unsubscribe opts)]
         [::pom/post (:post opts)]
         [::pom/archive (:archive opts)]
         (if-let [other-archives (:other-archives opts)]
           (vec (concat [::pom/otherArchives]
                        (for [other other-archives]
                          [::pom/otherArchive other]))))]])))

(defn- distinct-key [k xs]
  ((fn step [seen xs]
     (lazy-seq
      (if (seq xs)
        (let [x (first xs), key (k x)]
          (if (seen key)
            (step seen (rest xs))
            (cons x (step (conj seen key) (rest xs))))))))
   #{} (seq xs)))

(defn- make-scope [scope [dep version & opts]]
  (list* dep version (apply concat (assoc (apply hash-map opts) :scope scope))))

(defn- dep-key [dep]
  (select-keys (project/dependency-map dep) [:group-id :artifact-id :classifier
                                             :extension :scope]))

(defmethod xml-tags ::project
  ([_ project]
     (let [original-project (-> project meta ::original-project)
           profile-kws (concat
                        (set/difference
                         (set (project/non-leaky-profiles original-project))
                         (set (project/pom-scope-profiles
                               original-project :provided))
                         (set (project/pom-scope-profiles
                               original-project :test))))
           test-project (-> original-project
                            (project/unmerge-profiles profile-kws)
                            (project/merge-profiles [:test])
                            relativize)
           managed-deps (:managed-dependencies test-project)
           deps (:dependencies test-project)]
       (list
        [::pom/project {:xmlns pom-uri
                        :xmlns/xsi xsi-uri
                        ::xsi/schemaLocation
                        (str pom-uri " https://maven.apache.org/xsd/maven-4.0.0.xsd")}
         [::pom/modelVersion "4.0.0"]
         (and (:parent project) (xml-tags :parent (:parent project)))
         [::pom/groupId (:group project)]
         [::pom/artifactId (:name project)]
         [::pom/packaging (:packaging project "jar")]
         [::pom/version (:version project)]
         (and (:classifier project) [::pom/classifier (:classifier project)])
         [::pom/name (:name project)]
         [::pom/description (:description project)]
         (xml-tags :url (:url project))
         (if-let [licenses (license-tags project)]
           [::pom/licenses licenses])
         (xml-tags :mailing-list (:mailing-list project))
         (write-scm-tag (guess-scm project) project)
         ;; TODO: this results in lots of duplicate entries
         (xml-tags :build [project test-project])
         (xml-tags :repositories (:repositories project))
         (xml-tags :dependencyManagement
                   (xml-tags :dependencies (distinct-key dep-key managed-deps)))
         (xml-tags :dependencies (distinct-key dep-key deps))
         (and (:pom-addition project) (pomify-sexp (:pom-addition project)))]))))

(defn snapshot? [project]
  (and (:version project)
       (re-find #"SNAPSHOT" (:version project))))

(defn check-for-snapshot-deps [project]
  (when (and (not (snapshot? project))
             (not (System/getenv "LEIN_SNAPSHOTS_IN_RELEASE")))
    (let [merged-deps (classpath/merge-versions-from-managed-coords
                       (:dependencies project)
                       (:managed-dependencies project))]
      (when (some #(re-find #"SNAPSHOT" (second %)) merged-deps)
        (main/abort "Release versions may not depend upon snapshots."
                    "\nFreeze snapshots to dated versions or set the"
                    "LEIN_SNAPSHOTS_IN_RELEASE environment variable to override.")))))

(defn make-pom
  ([project] (make-pom project false))
  ([project disclaimer?]
     (let [profile-kws (project/non-leaky-profiles project)
           project (-> project
                       (project/unmerge-profiles profile-kws)
                       (vary-meta assoc ::original-project project))]
       (check-for-snapshot-deps project)
       (str
        (xml/indent-str
         (xml/sexp-as-element
          (xml-tags :project (relativize project))))
        (if disclaimer? disclaimer)))))

(defn ^{:help-arglists '([])} pom
  "Write a pom.xml file to disk for Maven interoperability."
  ([project pom-location-or-properties]
     (let [pom (make-pom project true)
           pom-file (io/file (:root project) pom-location-or-properties)]
       (utils/mkdirs (.getParentFile pom-file))
       (with-open [pom-writer (io/writer pom-file)]
         (.write pom-writer pom))
       (main/info "Wrote" (str pom-file))
       (.getAbsolutePath pom-file)))
  ([project] (pom project (io/file (:pom-location project) "pom.xml"))))
