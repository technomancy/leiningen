(ns leiningen.uberjar
  "Package up the project files and dependencies into a jar file."
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
            [leiningen.jar :as jar]
            [leiningen.pom :as pom]
            [clojure.set :as set])
  (:import (java.io File FileOutputStream PrintWriter InputStream)
           (java.util.regex Pattern)
           (java.util.zip ZipFile ZipOutputStream ZipEntry)
           (javax.xml.parsers SAXParser)
           (org.xml.sax.helpers DefaultHandler)
           (org.apache.commons.io.output CloseShieldOutputStream)
           (org.apache.commons.lang StringEscapeUtils)))

(defn- check-for-snapshot-deps [project]
  (->> (project/non-leaky-profiles project)
       (project/unmerge-profiles project)
       pom/check-for-snapshot-deps))

(defn- tree-edit
  "Walk the componment xml dom looking for description tag"
  [zipper editor]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (= :description (:tag (zip/node loc)))
        (let [new-loc (zip/edit loc editor)]
          (recur (zip/next new-loc)))
        (recur (zip/next loc))))))

(defn- html-escape-editor
  "Escape <,>,& from content"
  [node]
  (let [content (get (:content node) 0)]
    (if-not (nil? content)
      (assoc-in node [:content 0] (StringEscapeUtils/escapeXml content))
      node)))

;; replace clojure.xml version with one that doesn't do illegal access
(defn- startparse [^InputStream ins ^DefaultHandler ch]
  (let [^SAXParser p (xml/disable-external-entities (xml/sax-parser))]
    (.parse p ins ch)))

(defn- components-read [ins]
  (let [zipper (-> ins (xml/parse startparse) zip/xml-zip)]
    (->> (tree-edit zipper html-escape-editor) zip/xml-zip zip/children
         (filter #(= (:tag %) :components))
         first :content)))

(defn- components-write [out components]
  (binding [*out* (PrintWriter. out)]
    (xml/emit {:tag :component-set
               :content
               [{:tag :components
                 :content components}]})
    (.flush *out*)))

(def components-merger
  "Project `:uberjar-merge-with` merger for components.xml files."
  [components-read into components-write])

(def clj-map-merger
  "Project `:uberjar-merge-with` for files containing a single map
  read with `clojure.core/read`, such as data_readers.clj."
  [(comp read-string slurp) merge #(spit %1 (pr-str %2))])

(defn- merger-match? [[pattern] filename]
  (boolean
   (condp instance? pattern
     String (= pattern filename)
     Pattern (re-find pattern filename))))

(def ^:private skip-merger
  [(constantly ::skip)
   (constantly nil)])

(def ^:private default-merger
  [(fn [in out file prev]
     (when-not prev
       (.setCompressedSize file -1)
       (.putNextEntry out file)
       (io/copy (.getInputStream in file) out)
       (.closeEntry out))
     ::skip)
   (constantly nil)])

(defn- make-merger [fns]
  {:pre [(sequential? fns) (= 3 (count fns)) (every? ifn? fns)]}
  (let [[read-fn merge-fn write-fn] fns]
    [(fn [in out file prev]
       (with-open [ins (.getInputStream in file)]
         (let [new (read-fn ins)]
           (if-not prev
             new
             (merge-fn new prev)))))
     (fn [out filename result]
       (.putNextEntry out (ZipEntry. filename))
       (write-fn (CloseShieldOutputStream. out) result)
       (.closeEntry out))]))

(defn- make-mergers [project]
  (into (utils/map-vals (:uberjar-merge-with project)
                        (comp make-merger eval))
        (map #(vector % skip-merger)
             (:uberjar-exclusions project))))

(defn- select-merger [mergers filename]
  (or (->> mergers (filter #(merger-match? % filename)) first second)
      default-merger))

(defn- warn-on-drop [filename]
  (let [non-code #".*/|project\.clj|META-INF/(MANIFEST\.MF|(NOTICE|LICENSE)(.*\.txt)?|DEPENDENCIES)"]
    (if-not (re-matches non-code filename)
      (main/debug "  Dropping" filename))))

;; TODO: unify with copy-to-jar functionality in jar.clj (for 3.0?)
(defn- copy-entries
  "Read entries of ZipFile `in` and apply the filename-determined entry-merging
  logic captured in `mergers`. The default merger copies entry contents directly
  to the ZipOutputStream `out` and skips subsequent same-named files. Returns
  new `merged-map` merged entry map."
  [in out mergers merged-map]
  (reduce (fn [merged-map file]
            (let [filename (.getName file), prev (get merged-map filename)]
              (if (identical? ::skip prev)
                (do (warn-on-drop filename)
                  merged-map)
                (let [[read-merge] (select-merger mergers filename)]
                  (assoc merged-map
                    filename (read-merge in out file prev))))))
          merged-map (enumeration-seq (.entries in))))

(defn- include-dep [out mergers merged-map dep]
  (main/debug "Including" (.getName dep))
  (with-open [zipfile (ZipFile. dep)]
    (copy-entries zipfile out mergers merged-map)))

(defn write-components
  "Given a list of jarfiles, writes contents to a stream"
  [project jars out]
  (let [mergers (make-mergers project)
        include-dep (partial include-dep out mergers)
        merged-map (reduce include-dep {} jars)]
    (doseq [[filename result] merged-map
            :when (not (identical? ::skip result))
            :let [[_ write] (select-merger mergers filename)]]
      (write out filename result))))

(defn uberjar
  "Package up the project files and all dependencies into a jar file.

Includes the contents of each of the dependency jars. Suitable for standalone
distribution.

With an argument, the uberjar will be built with an alternate main.

The namespace you choose as main should have :gen-class in its ns form
as well as defining a -main function.

Note: The :uberjar profile is implicitly activated for this task, and cannot
be deactivated."

  ([project main]
   (let [scoped-profiles (set (project/pom-scope-profiles project :provided))
         default-profiles (set (project/expand-profile project :default))
         provided-profiles (remove
                            (set/difference default-profiles scoped-profiles)
                            (-> project meta :included-profiles))
         project (->> (into [:uberjar] provided-profiles)
                      (project/merge-profiles project))
         _ (check-for-snapshot-deps project)
         project (update-in project [:jar-inclusions]
                            concat (:uberjar-inclusions project))
         [_ jar] (try (first (jar/jar project main))
                      (catch Exception e
                        (when main/*debug*
                          (.printStackTrace e))
                        (main/abort "Uberjar aborting because jar failed:"
                                    (.getMessage e))))
         standalone-filename (jar/get-jar-filename project :standalone)]
     (with-open [out (-> standalone-filename
                         (FileOutputStream.)
                         (ZipOutputStream.))]
       (let [whitelisted (select-keys project project/whitelist-keys)
             project (-> (project/unmerge-profiles project [:default])
                         (merge whitelisted))
             deps (->> (classpath/resolve-managed-dependencies
                        :dependencies :managed-dependencies project)
                       (filter #(.endsWith (.getName %) ".jar")))
             jars (cons (io/file jar) deps)]
         (write-components project jars out)))
     (main/info "Created" standalone-filename)
     standalone-filename))
  ([project] (uberjar project nil)))
