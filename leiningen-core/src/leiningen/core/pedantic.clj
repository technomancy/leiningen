(ns leiningen.core.pedantic
  "This namespace exists to hook into Aether's dependency
  resolution and provide feedback about the dependency tree. Using a
  `DependencyGraphTransformer` allows us to look at the tree both before
  and after conflict resolution so that downloading all of the
  dependencies only occurs once.

  Aether uses a `NearestVersionConflictResolver` to resolve which
  versions to use in case of a conflict. The
  `NearestVersionConflictResolver` uses a `ConflictIdSorter` to
  determine those, and it will save the information in
  `SORTED_CONFLICT_IDS` and `CONFLICT_IDS`. We can similarly use the
  conflict information to determine which version is choosen in a
  conflict.

  Additional important classes from Aether:

  * `DependencyGraphTransformationContext`
  * `DependencyNode`
  * `Dependency`
  * `Artifact`
  * `Version`
  * `VersionConstraint`"
  (:refer-clojure :exclude [do])
  (:require [cemerick.pomegranate.aether :as aether])
  (:import (org.eclipse.aether.graph Exclusion)
           (org.eclipse.aether.collection DependencyGraphTransformer)
           (org.eclipse.aether.util.graph.transformer TransformationContextKeys
                                                      ConflictIdSorter)))

(defn- warn [& args]
  ;; TODO: remove me once #1227 is merged
  (require 'leiningen.core.main)
  (apply (resolve 'leiningen.core.main/warn) args))

(def ^:private warn-once (memoize warn))

;; This namespace originated as an independent library which was at
;; https://github.com/xeqi/pedantic in order to allow it to evolve
;; at its own pace decoupled from Leiningen's release cycle, but now it's
;; part of Leiningen as of 2.8.0.

(defn- initialize-conflict-ids!
  "Make sure that `SORTED_CONFLICT_IDS` and `CONFLICT_IDS` have been
  initialized. Similar to what a NearestVersionConflictResolver will do."
  [node context]
  (when-not (.get context TransformationContextKeys/SORTED_CONFLICT_IDS)
    (-> (ConflictIdSorter.)
        (.transformGraph node context))))

(defn- range?
  "Does the path point to a DependencyNode asking for a version range
   which contains several versions?"
  [{:keys [node]}]
  (when-let [vc (.getVersionConstraint node)]
    (let [range (.getRange vc)
          lb    (some-> range .getLowerBound)
          ub    (some-> range .getUpperBound)]
      (and (some? range)
           (some? lb)
           (not (.equals lb ub))))))

(defn- set-ranges!
  "Set ranges to contain all paths that asks for a version range"
  [ranges paths]
  (reset! ranges (doall (filter range? paths))))

(defn- node<
  "Is the version of node1 < version of node2."
  [node1 node2]
  (< (compare (.getVersion node1) (.getVersion node2)) 0))

(defn- node->artifact-map
  [node]
  (if-let [d (.getDependency node)]
    (if-let [a (.getArtifact d)]
      (let [b (bean a)]
        (-> b
            (select-keys [:artifactId :groupId :exclusions
                          :version :extension :properties])
            (update-in [:exclusions] vec))))))

(defn- node=
  "Check value equality instead of reference equality."
  [n1 n2]
  (= (node->artifact-map n1)
     (node->artifact-map n2)))

(defn- top-level?
  "Is the path a top level dependency in the project?"
  [{:keys [parents]}]
  ;; Parent is root node
  (= 1 (count parents)))

(defn- set-overrides!
  "Check each `accepted-path` against its conflicting paths. If a
  conflicting path fails the pedantic criteria then add information
  representing this possibly confusing situation to `overrides`."
  [overrides conflicts accepted-paths ranges]
  (doseq [{:keys [node parents] :as path} accepted-paths]
    (let [ignoreds (for [conflict-path (conflicts node)
                         :when (and (not= path conflict-path)
                                    ;; This is the pedantic criteria
                                    (or (node< node (:node conflict-path))
                                        (top-level? conflict-path)))]
                     conflict-path)]
      (when (not (empty? ignoreds))
        (swap! overrides conj {:accepted path
                               :ignoreds ignoreds
                               :ranges
                               (filter #(node= (:node %) node) ranges)})))))

(defn- all-paths
  "Breadth first traversal of the graph from DependencyNode node.
  Short circuits a path when a cycle is detected."
  [node]
  (loop [paths [{:node node :parents []}]
         results []]
    (if (empty? paths)
      results
      (recur (for [{:keys [node parents]} paths
                   :when (not (some #{node} parents))
                   c (.getChildren node)]
               {:node c
                :parents (conj parents node)})
             (doall (concat results paths))))))

(defn- transform-graph
  "Examine the tree with root `node` for version ranges, then
  allow the original `transformer` to perform resolution, then check for
  overriden dependencies."
  [ranges overrides node context transformer]
  ;; Force initialization of the context like NearestVersionConflictResolver
  (initialize-conflict-ids! node context)
  ;; Get all the paths of the graph before dependency resolution
  (let [potential-paths (all-paths node)]
    (set-ranges! ranges potential-paths)
    (.transformGraph transformer node context)
    ;; The original transformer should have done dependency resolution,
    ;; so now we can gather just the accepted paths and use the ConflictId
    ;; to match against the potential paths
    (let [node->id (.get context TransformationContextKeys/CONFLICT_IDS)
          id->paths (reduce (fn [acc {:keys [node] :as path}]
                              (update-in acc [(.get node->id node)] conj path))
                            {}
                            ;; Remove ranges as they cause problems and were
                            ;; warned above
                            (remove range? potential-paths))]
      (set-overrides! overrides
                      #(->> % (.get node->id) id->paths)
                      (all-paths node)
                      @ranges))))

(defn- use-transformer
  "Wrap the session's current `DependencyGraphTransformer` with one that checks
  for version ranges and overriden dependencies.

  `ranges` and `overrides` are expect to be (atom []).  This provides a way to
  send back information since the return value can't be used here.

  After resolution:
  `ranges` will be a vector of paths (see pedantic.path)
  `overrides` will be a vector of maps with keys [:accepted :ignoreds :ranges].
    `:accepted` is the path that was resolved. :ignored is a list of
    paths that were not used.
    `:ranges` is a list of paths containing version ranges that might
    have affected the resolution."
  [session ranges overrides]
  (let [transformer (.getDependencyGraphTransformer session)]
    (.setDependencyGraphTransformer
     session
     (reify DependencyGraphTransformer
       (transformGraph [_ node context]
         (try
           (transform-graph ranges overrides node context transformer)
           (catch java.lang.OutOfMemoryError _
             (warn "Pathological dependency tree detected.")
             (warn "Consider setting :pedantic? false in project.clj to bypass.")))
         ;;Return the DependencyNode in order to meet
         ;;transformGraph's contract
         node)))))

(defn ^:internal session [project ranges overrides]
  (if (:pedantic? project)
    #(-> % aether/repository-session
         (use-transformer ranges overrides))))

(defn- group-artifact [artifact]
  (if (= (.getGroupId artifact)
         (.getArtifactId artifact))
    (.getGroupId artifact)
    (str (.getGroupId artifact)
         "/"
         (.getArtifactId artifact))))

(defn- dependency-str [dependency & [version]]
  (if-let [artifact (and dependency (.getArtifact dependency))]
    (str "["
         (group-artifact artifact)
         " \"" (or version (.getVersion artifact)) "\""
         (if-let [classifier (.getClassifier artifact)]
           (if (not (empty? classifier))
             (str " :classifier \"" classifier "\"")))
         (if-let [extension (.getExtension artifact)]
           (if (not= extension "jar")
             (str " :extension \"" extension "\"")))
         (if-let [exclusions (seq (.getExclusions dependency))]
           (str " :exclusions " (mapv (comp symbol group-artifact)
                                      exclusions)))
         "]")))

(defn- message-for [path & [show-constraint?]]
  (->> path
       (map #(dependency-str (.getDependency %) (.getVersionConstraint %)))
       (remove nil?)
       (interpose " -> ")
       (apply str)))

(defn- message-for-version [{:keys [node parents]}]
  (message-for (conj parents node)))

(defn- message-for-version-newest [{:keys [node parents]}]
  (conj parents node))

(defn- exclusion-for-range [node parents]
  (if-let [top-level (second parents)]
    (let [excluded-artifact (.getArtifact (.getDependency node))
          exclusion (Exclusion. (.getGroupId excluded-artifact)
                                (.getArtifactId excluded-artifact) "*" "*")
          exclusion-set (into #{exclusion} (.getExclusions
                                            (.getDependency top-level)))
          with-exclusion (.setExclusions (.getDependency top-level) exclusion-set)]
      (dependency-str with-exclusion))
    ""))

(defn- message-for-range [{:keys [node parents]}]
  (str (message-for (conj parents node) :constraints) "\n"
       "Consider using "
       (exclusion-for-range node parents) "."))

(defn- exclusion-for-override [{:keys [node parents]}]
  (exclusion-for-range node parents))

(defn- message-for-override [{:keys [accepted ignoreds ranges]}]
  {:accepted (message-for-version accepted)
   :ignoreds (map message-for-version ignoreds)
   :ranges (map message-for-range ranges)
   :exclusions (map exclusion-for-override ignoreds)})

(defn- message-for-override-newest [{:keys [accepted ignoreds ranges]}]
  {:accepted (message-for-version-newest accepted)
   :ignoreds (map message-for-version-newest ignoreds)})

(defn- pedantic-print-ranges [messages]
  (when-not (empty? messages)
    (warn "WARNING!!! version ranges found for:")
    (doseq [dep-string messages]
      (warn dep-string))
    (warn)))

(defn- pedantic-print-overrides [messages]
  (when-not (empty? messages)
    (warn "Possibly confusing dependencies found:")
    (doseq [{:keys [accepted ignoreds ranges exclusions]} messages]
      (warn accepted)
      (warn " overrides")
      (doseq [ignored (interpose " and" ignoreds)]
        (warn ignored))
      (when-not (empty? ranges)
        (warn " possibly due to a version range in")
        (doseq [r ranges]
          (warn r)))
      (warn "\nConsider using these exclusions:")
      (doseq [ex (distinct exclusions)]
        (warn ex))
      (warn))))

(alter-var-root #'pedantic-print-ranges memoize)
(alter-var-root #'pedantic-print-overrides memoize)

(defn- node-str-version [node]
  (-> node :node .getVersion .toString))

(defn- newer-dep [{accepted :accepted
                   ignoreds :ignoreds
                   _ranges :ranges}]
  (let [dep-name (str (-> accepted :node .getArtifact .getGroupId)
                      "/"
                      (-> accepted :node .getArtifact .getArtifactId))
        newer-version (some->> (conj ignoreds accepted)
                               (sort-by :node node<)
                               last
                               node-str-version)]
    (str "[" dep-name " \"" newer-version "\"]")))

(defn- print-overrides [overrides]
  (run! #(->> % newer-dep (println "Suggest: "))  overrides))

(defn ^:internal do [pedantic-setting ranges overrides]
  ;; Need to turn everything into a string before calling
  ;; pedantic-print-*, otherwise we can't memoize due to bad equality
  ;; semantics on aether GraphEdge objects.
  (let [key (keyword pedantic-setting)
        abort-or-true (#{true :abort} key)]
    (when (and key (not= key :overrides))
      (pedantic-print-ranges (distinct (map message-for-range ranges))))
    (when (and key (not= key :ranges))
      (pedantic-print-overrides (map message-for-override overrides))
      (when (not-empty overrides)
        (println "Or adding depencies in the list of depency vector of project.cl.")
        (print-overrides overrides)
        (println)))
    (when (and abort-or-true
               (not (empty? (concat ranges overrides))))
      (require 'leiningen.core.main)
      ((resolve 'leiningen.core.main/abort) ; cyclic dependency =\
       "Aborting due to :pedantic? :abort"))))
