(ns leiningen.core.project-schema
  (:require
   [clojure.pprint :as pp]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [strictly-specking-standalone.ansi-util :refer [with-color]]
   [strictly-specking-standalone.parse-spec :as parse]
   [strictly-specking-standalone.spec :as s]
   [strictly-specking-standalone.core :refer [def-key] :as ssp]
   [strictly-specking-standalone.fuzzy :as fuz]))

(defn non-blank-string? [s]
  (and (string? s) (not (string/blank? s))))

(def sequential-sexp? sequential?)

(defn regex? [x]
  (instance? java.util.regex.Pattern x))

(defn boolean?
  "Return true if x is a Boolean"
  [x] (instance? Boolean x))

(s/def ::name-like (some-fn symbol? keyword? non-blank-string?))

(def-key ::leiningen-project-root
  (ssp/strict-keys
   :opt-un
   [::description
    ::url
    ::name
    ::mailing-list
    ::license
    ::licenses
    ::min-lein-version
    ::dependencies
    ::managed-dependencies
    ::pedantic?
    ::exclusions
    ::plugins
    ::repositories
    ::plugin-repositories
    ::mirrors
    ::local-repo
    ::update
    ::checksum
    ::offline?
    ::dependencies
    ::signing
    ::certificates
    ;; ::profiles ;; current imp not complete
    ::hooks
    ::middleware
    ::implicit-middleware
    ::implicit-hooks
    ::main
    ::aliases
    ::release-tasks
    ::prep-tasks
    ::aot
    ;; ::injections ;; current imp to general
    ::java-agents
    ::javac-options
    ::warn-on-reflection
    ::global-vars
    ::java-cmd
    ::jvm-opts
    ::eval-in
    ::bootclasspath
    ::source-paths
    ::java-source-paths
    ::test-paths
    ::resource-paths
    ::target-path
    ::compile-path
    ::native-path
    ::clean-targets
    ::clean-non-project-classes
    ;; ::checkout-deps-shares ;; current imp to general
    ;; ::test-selectors ;; no impl
    ::monkeypatch-clojure-test
    ::repl-options
    ::jar-name
    ::uberjar-name
    ::omit-source
    ::jar-exclusions
    ::uberjar-exclusions
    ::auto-clean
    ::uberjar-merge-with


    ::scm

    ::validate]))

(def-key ::description non-blank-string?
  "The description text is searchable from repositories like Clojars.")

(def-key ::url         non-blank-string?)

(def-key ::name        non-blank-string?)

(def-key ::mailing-list
  (ssp/strict-keys
   :opt-un
   [::name
    ::archive
    ::other-archives
    ::post
    ::subscribe
    ::unsubscribe])
  "The mailing list of the project. If the project has multiple mailing
lists, use the :mailing-lists key (bound to a seq of mailing list
descriptions as below).

Example:

  :mailing-list {:name \"sample mailing list\"
                 :archive \"http://example.org/sample-mailing-list-archives\"
                 :other-archives [\"http://example.org/sample-list-archive2\"
                                  \"http://example.org/sample-list-archive3\"]
                 :post \"list@example.org\"
                 :subscribe \"list-subscribe@example.org\"
                 :unsubscribe \"list-unsubscribe@example.org\"}"
  )

(def-key ::archive non-blank-string?)
(def-key ::other-archives (s/every non-blank-string? :min-count 1))
(def-key ::post non-blank-string?)
(def-key ::subscribe non-blank-string?)
(def-key ::unsubscribe non-blank-string?)

(def-key ::license
  (ssp/strict-keys
   :opt-un
   [::name
    ::url
    ::distribution
    ::comments])
  "The project's license. :distribution should be :repo or :manual;
:repo means it is OK for public repositories to host this project's
artifacts. A seq of :licenses is also supported.

Example:

  :license {:name \"Eclipse Public License - v 1.0\"
            :url \"http://www.eclipse.org/legal/epl-v10.html\"
            :distribution :repo
            :comments \"same as Clojure\"}")

(def-key ::distribution #{:repo :manual})

(def-key ::comments non-blank-string?)

(def-key ::licenses (s/every ::license :min-count 1))

(def-key ::min-lein-version non-blank-string?
  "Warns users of earlier versions of Leiningen. Set this if your project
relies on features only found in newer Leiningen versions.

Example:

  :min-lein-version \"2.0.0\""  )

(def-key ::optional boolean?)

(def-key ::scope         ::name-like)
;; ??? what values ???
(def-key ::classifier    ::name-like)
(def-key ::extension     (some-fn string? symbol? keyword?))
(def-key ::native-prefix (some-fn string? symbol? keyword?))

;; TODO duplication
(s/def ::exclusion-item
  (s/or
   :sym symbol?
   :vec (s/and
         (s/cat :lib-name symbol?
                :dependency-item-args ::dependency-item-args))))

(s/def ::dependency-item-args
  (s/* (s/alt :optional      (s/cat :ky #{:optional}
                                    :val boolean?)
              :scope         (s/cat :ky #{:scope}
                                    :val ::name-like)
              :classifier    (s/cat :ky #{:classifier}
                                    :val ::name-like)
              :native-prefix (s/cat :ky #{:native-prefix}
                                    :val (some-fn string? symbol? keyword?))
              :extension     (s/cat :ky  #{:extension}
                                    :val (some-fn string? symbol? keyword?))
              :exclusions    (s/cat :ky #{:exclusions}
                                    :val ::exclusions))))
(def-key ::exclusions (s/every ::exclusion-item :min-count 1))

;; TODO really look at this and see if we can get a better
;; path to the erroneous value
(s/def ::dependency-item
  (s/or
   :short-managed
   (s/cat :lib-name symbol?
          :dependency-item-args ::dependency-item-args)
   :long
   (s/cat :lib-name symbol?
          :version-str (some-fn nil? non-blank-string?)
          :dependency-item-args ::dependency-item-args)))

(def-key ::dependencies
  (s/every ::dependency-item)
  "Dependencies are listed as [group-id/name version]; in addition
to keywords supported by Pomegranate, you can use :native-prefix
to specify a prefix. This prefix is used to extract natives in
jars that don't adhere to the default  \"<os>/<arch>/\" layout that
Leiningen expects.

Example:

  :dependencies [[org.clojure/clojure \"1.3.0\"]
                 [org.jclouds/jclouds \"1.0\" :classifier \"jdk15\"]
                 [net.sf.ehcache/ehcache \"2.3.1\" :extension \"pom\"]
                 [log4j \"1.2.15\" :exclusions [[javax.mail/mail :extension \"jar\"]
                                               [javax.jms/jms :classifier \"*\"]
                                               com.sun.jdmk/jmxtools
                                               com.sun.jmx/jmxri]]
                 [org.lwjgl.lwjgl/lwjgl \"2.8.5\"]
                 [org.lwjgl.lwjgl/lwjgl-platform \"2.8.5\"
                  :classifier \"natives-osx\"
                  ;; LWJGL stores natives in the root of the jar; this
                  ;; :native-prefix will extract them.
                  :native-prefix \"\"]]")

(def-key ::managed-dependencies
  (s/every ::dependency-item)
"'Managed Dependencies' are a concept borrowed from maven pom files; see
https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management

Managed dependencies allow you to specify a desired version number for a dependency
*if* the dependency exists (often transitively), but a managed dependency
will not actually cause the described artifact to be a dependency on its own.
This feature is most useful in combination with some other mechanism for
defining a \"parent project\"; e.g. you can have a \"parent project\" that specifies
managed dependencies for common libraries that you use frequently in your other
projects, and then the downstream/child projects can specify a normal dependency on
those libraries *without specifying a version number*, and thus will inherit
the version number from the parent.  This provides a simpler means of keeping
common dependency versions in sync across a large number of clojure libraries.

For more info see ./doc/MANAGED_DEPS.md and https://github.com/achin/lein-parent

Example:

  :managed-dependencies [[clj-time \"0.12.0\"]
                         [me.raynes/fs \"1.4.6\"]]")


(def-key ::pedantic? #{true false :ranges :abort}
"What to do in the case of version issues. Defaults to :ranges, which
warns when version ranges are present anywhere in the dependency tree,
but can be set to true to warn for both ranges and overrides, or :abort
to exit in the case of ranges or overrides.")

(def-key ::plugins (s/every ::plugin-item)
  "Plugins are code that runs in Leiningen itself and usually
provide new tasks or hooks.")

(s/def ::plugin-item
  (s/and
   (s/cat :lib-name symbol?
          :version-str non-blank-string?
          :dependency-item-args
          (s/* (s/alt :optional      (s/cat :ky #{:optional}
                                            :val boolean?)
                      :scope         (s/cat :ky #{:scope}
                                            :val ::name-like)
                      :classifier    (s/cat :ky #{:classifier}
                                            :val ::name-like)
                      :native-prefix (s/cat :ky #{:native-prefix}
                                            :val (some-fn string? symbol? keyword?))
                      :extension     (s/cat :ky  #{:extension}
                                            :val (some-fn string? symbol? keyword?))
                      :exclusions    (s/cat :ky #{:exclusions}
                                            :val ::exclusions)
                      :plugins-args  (s/cat :ky #{:middleware :hooks}
                                            :val boolean?))))))

(def-key ::repositories
  (s/every (s/cat :nm non-blank-string?
                  :data (s/alt :url non-blank-string?
                               :info ::repository-data-map)))
  "These repositories will be searched for :dependencies and
:plugins and will also be available to deploy to.
Add ^:replace (:repositories ^:replace [...]) to only use repositories you
list below.

Example:

  :repositories [[\"java.net\" \"http://download.java.net/maven/2\"]
                 [\"sonatype\" {:url \"http://oss.sonatype.org/content/repositories/releases\"

                              ;; If a repository contains releases only setting
                              ;; :snapshots to false will speed up dependencies.
                              :snapshots false

                              ;; Disable signing releases deployed to this repo.
                              ;; (Not recommended.)
                              :sign-releases false

                              ;; You can also set the policies for how to handle
                              ;; :checksum failures to :fail, :warn, or :ignore.
                              :checksum :fail

                              ;; How often should this repository be checked for
                              ;; snapshot updates? (:daily, :always, or :never)
                              :update :always

                              ;; You can also apply them to releases only:
                              :releases {:checksum :fail :update :always}}]

                 ;; Repositories named \"snapshots\" and \"releases\" automatically
                 ;; have their :snapshots and :releases disabled as appropriate.
                 ;; Credentials for repositories should *not* be stored
                 ;; in project.clj but in ~/.lein/credentials.clj.gpg instead,
                 ;; see `lein help deploying` under \"Authentication\".

                 [\"snapshots\" \"http://blueant.com/archiva/snapshots\"]
                 [\"releases\" {:url \"http://blueant.com/archiva/internal\"
                              ;; Using :env as a value here will cause an
                              ;; environment variable to be used based on
                              ;; the key; in this case LEIN_PASSWORD.
                              :username \"milgrim\" :password :env}]]")

(def-key ::repository-data-map
  (ssp/strict-keys
   :opt-un
   [:lein.validate.repositories.info/url
    :lein.validate.repositories.info/snapshots
    :lein.validate.repositories.info/sign-releases
    ::checksum
    ::update
    :lein.validate.repositories.info/releases
    :lein.validate.repositories.info/username
    :lein.validate.repositories.info/password]))

(def-key :lein.validate.repositories.info/url non-blank-string?)

(def-key :lein.validate.repositories.info/snapshots boolean?
  "If a repository contains releases only setting
:snapshots to false will speed up dependencies.

Example:

  :snapshots false")



(def-key :lein.validate.repositories.info/sign-releases boolean?
  "Disable signing releases deployed to this repo.
(Not recommended.)

Example:

  :sign-releases false")

(def-key ::checksum #{:fail :warn :ignore}
  "You can also set the policies for how to handle
:checksum failures to :fail, :warn, or :ignore.

Example:

  :checksum :fail")

(def-key ::update #{:daily :always :never}
  "How often should this repository be checked for
snapshot updates? (:daily, :always, or :never)

Example:

  :update :always")

(def-key :lein.validate.repositories.info/releases ::repository-data-map)

(def-key :lein.validate.repositories.info/credential-store
  (s/or :string-literal non-blank-string?
        :GPG-store #{:gpg}
        :environment-variable #{:env}
        :user-named-environment-variable keyword?)
  "As defined in https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#authentication
:username and :password can be specified either as a string literal,
stored in GPG or in environment variables.")

(def-key :lein.validate.repositories.info/username :lein.validate.repositories.info/credential-store)

(def-key :lein.validate.repositories.info/password :lein.validate.repositories.info/credential-store
  "Using :env as a value here will cause an environment variable to be
used based on the key; in this case LEIN_PASSWORD.")



(def-key ::plugin-repositories
  (s/every (s/cat :nm non-blank-string?
                  :data (s/alt :url non-blank-string?
                               :info ::repository-data-map)))
  "These repositories will be included with :repositories when loading plugins.
This would normally be set in a profile for non-public repositories.
All the options are the same as in the :repositories map.")

(def-key ::mirrors (s/every-kv
                    (some-fn non-blank-string? regex?)
                    (ssp/strict-keys
                     :opt-un
                     [:lein.validate.mirrors/name
                      :lein.validate.mirrors/url
                      :lein.validate.mirrors/repo-manager]))
"Fetch dependencies from mirrors. Mirrors override repositories when the key
in the :mirrors map matches either the name or URL of a specified
repository. All settings supported in :repositories may be set here too.
The :name should match the name of the mirrored repository.

Example:
  :mirrors {\"central\" {:name \"central\"
                       :url \"http://mirrors.ibiblio.org/pub/mirrors/maven2\"}
            \"clojars\" {:name \"Internal nexus\"
                        :url \"http://mvn.local/nexus/releases\"
                        :repo-manager true}}")

(s/def :lein.validate.mirrors/name non-blank-string?)
(s/def :lein.validate.mirrors/url non-blank-string?)
(s/def :lein.validate.mirrors/repo-manager boolean?)

(def-key ::local-repo non-blank-string?
  "Override location of the local maven repository. Relative to project root.

Example:

  :local-repo \"local-m2\"")

(def-key ::offline? boolean?
"Prevent Leiningen from checking the network for dependencies.
This wouldn't normally be set in project.clj; it would come from a profile.

Example:

  :offline? true")


(def-key ::deploy-repositories
  (s/every (s/cat :nm non-blank-string?
                  :data (s/alt :url non-blank-string?
                               :info ::deploy-repository-data-map)))
  "The deploy task will give preference to repositories specified in
:deploy-repositories, and repos listed there will not be used for
dependency resolution.

Example:

   :deploy-repositories [[\"releases\" {:url \"http://blueant.com/archiva/internal/releases\"
                                     ;; Select a GPG private key to use for
                                     ;; signing. (See \"How to specify a user
                                     ;; ID\" in GPG's manual.) GPG will
                                     ;; otherwise pick the first private key
                                     ;; it finds in your keyring.
                                     ;; Currently only works in :deploy-repositories
                                     ;; or as a top-level (global) setting.
                                     :signing {:gpg-key \"0xAB123456\"}}]
                        [\"snapshots\" \"http://blueant.com/archiva/internal/snapshots\"]]")

(def-key ::deploy-repository-data-map
  (ssp/strict-keys
   :opt-un
   [:lein.validate.repositories.info/url
    :lein.validate.repositories.info/snapshots
    :lein.validate.repositories.info/sign-releases
    ::checksum
    ::update
    :lein.validate.repositories.info/releases
    :lein.validate.repositories.info/username
    :lein.validate.repositories.info/password
    ::signing]))

(def-key ::signing (s/map-of #{:gpg-key} non-blank-string?))

(def-key ::certificates (s/every non-blank-string?)
"If you configure a custom repository with a self-signed SSL
certificate, you will need to add it here. Paths should either
be on Leiningen's classpath or relative to the project root.

Example:

  :certificates [\"blueant.pem\"]")

(def keys-to-validate
  (:known-keys
   (apply parse/parse-keys-args (rest (s/describe ::leiningen-project-root)))))

(def-key ::profiles
  (s/map-of keyword?
            (s/and
             (s/conformer #(or (and (map? %)
                                    (fuz/fuzzy-select-keys % keys-to-validate))
                               ::strictly-specking-standalone.spec/invalid))
             ::leiningen-project-root))
"Profiles

Each active profile gets merged into the project map. The :dev
and :user profiles are active by default, but the latter should be
looked up in ~/.lein/profiles.clj rather than set in project.clj.
Use the with-profiles higher-order task to run a task with a
different set of active profiles.
See `lein help profiles` for a detailed explanation.

Example:

  :profiles {:debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             :1.4 {:dependencies [[org.clojure/clojure \"1.4.0\"]]}
             :1.5 {:dependencies [[org.clojure/clojure \"1.5.0\"]]}
             ;; activated by default
             :dev {:resource-paths [\"dummy-data\"]
                   :dependencies [[clj-stacktrace \"0.2.4\"]]}
             ;; activated automatically during uberjar
             :uberjar {:aot :all}
             ;; activated automatically in repl task
             :repl {:plugins [[cider/cider-nrepl \"0.7.1\"]]}}")


#_(s/conform ::dependency-item '[org.jclouds/jclouds "1.0" :classifier "jdk15"])

(def-key ::scm
  (s/map-of #{:name :tag :url :dir} non-blank-string?)
"Include <scm> tag in generated pom.xml file. All key/value pairs
appear exactly as configured. If absent, Leiningen will try to
use information from a .git directory.

Example:

  :scm {:name \"git\"
        :tag \"098afd745bcd\"
        :url \"http://127.0.0.1/git/my-project\"
        ;; Allows you to use a repository in a different directory than the
        ;; project's root, for instance, if you had multiple projects in a
        ;; single git repository.
        :dir \"..\"}")

(def-key ::hooks (s/every symbol? :min-count 1)
  "Load these namespaces from within Leiningen to pick up hooks from them.

Example:

  :hooks [leiningen.hooks.difftest]")

(def-key ::middleware (s/every symbol? :min-count 1)
  "Apply these middleware functions from plugins to your project
when it loads. Both hooks and middleware can be loaded implicitly
or by being listed here.

Example:

  :middleware [lein-xml.plugin/middleware]")

(def-key ::implicit-middleware boolean?
  "These settings disable the implicit loading of middleware and
hooks, respectively. You can disable both with :implicits false.

Example:

  :implicit-middleware false
  :implicit-hooks false")

(def-key ::implicit-hooks boolean?
  "These settings disable the implicit loading of middleware and
hooks, respectively. You can disable both with :implicits false.

Example:

  :implicit-middleware false
  :implicit-hooks false")

(def-key ::main symbol?
  "Entry Point
The -main function in this namespace will be run at launch
(either via `lein run` or from an uberjar). It should be variadic:

(defn -main
  \"Application Entry Point\"
  [& args]
  (comment Do app initialization here))

This will be AOT compiled by default; to disable this, attach ^:skip-aot
metadata to the namespace symbol. ^:skip-aot will not disable AOT
compilation of :main when :aot is :all or contains the main class. It's
best to be explicit with the :aot key rather than relying on
auto-compilation of :main. Setting :main to nil is useful when a
project contains several main functions. nil will produce a jar
with manifest.mf that lacks `Main-Class' property.")

(s/def ::command-element
  (s/or :str non-blank-string?
        :proj-key (s/and keyword?
                         #(= (namespace %) "project"))))

(def-key ::aliases
  (s/map-of
   non-blank-string?
   (s/or :command
         (s/every
          ::command-element
          :min-count 1)
         :do-command
         (s/cat
          :doo #{"do"}
          :rest (s/+ (s/alt :com-el ::command-element
                            :sub-vec (s/every ::command-element :min-count 1))))))
  "Support project-specific task aliases. These are interpreted in
the same way as command-line arguments to the lein command. If
the alias points to a vector, it uses partial application. For
example, \"lein with-magic run -m hi.core\" would be equivalent to
\"lein assoc :magic true run -m hi.core\". Remember, commas are not
considered to be special by argument parsers, they're just part
of the preceding argument.

Example:

  :aliases {\"launch\" [\"run\" \"-m\" \"myproject.main\"]
            ;; Values from the project map can be spliced into the arguments
            ;; using :project/key keywords.
            \"launch-version\" [\"run\" \"-m\" \"myproject.main\" :project/version]
            \"dumbrepl\" [\"trampoline\" \"run\" \"-m\" \"clojure.main/main\"]
            ;; :pass-through-help ensures `lein my-alias help` is not converted
            ;; into `lein help my-alias`.
            \"go\" ^:pass-through-help [\"run\" \"-m\"]
            ;; For complex aliases, a docstring may be attached. The docstring
            ;; will be printed instead of the expansion when running `lein help`.
            \"deploy!\" ^{:doc \"Recompile sources, then deploy if tests succeed.\"}
            ;; Nested vectors are supported for the \"do\" task
            [\"do\" \"clean\" [\"test\" \":integration\"] [\"deploy\" \"clojars\"]]}")



(def-key ::release-tasks
  (s/every
   (s/every ::command-element :min-count 1)
   :min-count 1)
  "Custom Release Tasks
By default `lein release` performs a series of tasks typical of the release
process for many Leiningen-managed projects. These tasks can be overridden
using `:release-tasks` as follows:

Example:

  :release-tasks [[\"vcs\" \"assert-committed\"]
                  [\"change\" \"version\"
                   \"leiningen.release/bump-version\" \"release\"]
                  [\"vcs\" \"commit\"]
                  [\"vcs\" \"tag\"]
                  [\"deploy\"]]

This differs from the default `:release-tasks` behavior in that it doesn't
go on to perform another `change version` or `vcs` task, instead leaving
that up to the developer to do manually.")


(def-key ::prep-tasks
  (s/every
   (s/or :str non-blank-string?
         :list-str (s/every string?))
   :min-count 1)
"Normally Leiningen runs the javac and compile tasks before
calling any eval-in-project code, but you can override this with
the :prep-tasks key to do other things like compile protocol buffers.

Example:

  :prep-tasks [[\"protobuf\" \"compile\"] \"javac\" \"compile\"]")

(def-key ::aot (s/or :all #{:all}
                     :seq (s/every (some-fn symbol? regex? )
                                   :min-count 1))
"These namespaces will be AOT-compiled. Needed for gen-class and
other Java interop functionality. Put a regex here to compile all
namespaces whose names match. If you only need AOT for an uberjar
gen-class, put `:aot :all` in the :uberjar profile and see :target-path for
how to enable profile-based target isolation.

Example:

  :aot [org.example.sample]")


(def-key ::injections
  (s/every ::s/any :min-count 1)
  "Forms to prepend to every form that is evaluated inside your project.
Allows working around the Gilardi Scenario: http://technomancy.us/143
Note: This code is not executed in jars or uberjars.

Example:

  :injections [(require 'clojure.pprint)]")

(def-key ::java-agents
  (s/every
   (s/cat :sym symbol? :str string?
          :opts (s/* (s/cat :k keyword? :v ::s/any)))
   :min-count 1)
  "Java agents can instrument and intercept certain VM features. Include
:bootclasspath true to place the agent jar on the bootstrap classpath.

Example:

  :java-agents [[nodisassemble \"0.1.1\" :options \"extra\"]]")


(def-key ::javac-options (s/every non-blank-string? :min-count 1)
  "Options to pass to java compiler for java source,
exactly the same as command line arguments to javac.

Example:

  :javac-options [\"-target\" \"1.6\" \"-source\" \"1.6\" \"-Xlint:-options\"]")

(def-key ::warn-on-reflection boolean?
  "Sets the values of global vars within Clojure. This example
disables all pre- and post-conditions and emits warnings on
reflective calls. See the Clojure documentation for the list of
valid global variables to set (and their meaningful values).

Example:
  :warn-on-reflection true")

(def-key ::global-vars
  (s/map-of symbol? ::s/any)
  "Sets the values of global vars within Clojure. This example
disables all pre- and post-conditions and emits warnings on
reflective calls. See the Clojure documentation for the list of
valid global variables to set (and their meaningful values).

Example:

  :global-vars {*warn-on-reflection* true
                *assert* false}")

(def-key ::java-cmd non-blank-string?
  "Use a different `java` executable for project JVMs. Leiningen's own JVM is
set with the LEIN_JAVA_CMD environment variable.

Example:

  :java-cmd \"/home/phil/bin/java1.7\"")

(def-key ::jvm-opts (s/every non-blank-string? :min-count 1)
"You can set JVM-level options here. The :java-opts key is an alias for this.

Example:

  :jvm-opts [\"-Xmx1g\"]")

(def-key ::eval-in #{:subprocess :leiningen :nrepl}
  "Set the context in which your project code is evaluated. Defaults
to :subprocess, but can also be :leiningen (for plugins) or :nrepl
to connect to an existing project process over nREPL. A project nREPL
server can be started simply by invoking `lein repl`. If no connection
can be established, :nrepl falls back to :subprocess.

Example:

  :eval-in :leiningen")

(def-key ::bootclasspath boolean?
"Enable bootclasspath optimization. This improves boot time but interferes
with certain libraries like Jetty that make assumptions about classloaders.

Example:

  :bootclasspath true")

(def-key ::source-paths (s/every string? :min-count 1)
  "The source paths to your source files, these will be added to the classpath.

Example:

  :source-paths [\"src\" \"src/main/clojure\"]")

(def-key ::java-source-paths (s/every string? :min-count 1)
  "The source paths to your source files, these will be added to the classpath.
Java source is stored seperately.

Example:

  :java-source-paths [\"src\" \"src/main/clojure\"]")

(def-key ::test-paths (s/every string? :min-count 1)
  "The source paths to your test source files, these will be added to the classpath.

Example:

  :test-paths [\"src\" \"src/main/clojure\"]")

(def-key ::resource-paths (s/every string? :min-count 1)
  "The source paths to your test source files, these will be added to the classpath.

Example:

  :resource-paths [\"src\" \"src/main/clojure\"]")

(def-key ::target-path non-blank-string?
  "All generated files will be placed in :target-path. In order to avoid
cross-profile contamination (for instance, uberjar classes interfering
with development), it's recommended to include %s in in your custom
:target-path, which will splice in names of the currently active profiles.

Example:

  :target-path \"target/%s/\"")

(def-key ::compile-path non-blank-string?
  "Directory in which to place AOT-compiled files. Including %s will
splice the :target-path into this value.

Example:

  :compile-path \"target/%s/\"")

(def-key ::native-path non-blank-string?
  "Directory in which to extract native components from inside dependencies.
Including %s will splice the :target-path into this value. Note that this
is not where to *look* for existing native libraries; use :jvm-opts with
-Djava.library.path=... instead for that.

Example:

  :native-path \"%s/bits-n-stuff\"")

(def-key ::clean-targets (s/every (s/or :str string?
                                        :ky keyword?
                                        :vec (s/every
                                              (s/or :str string?
                                                    :ky keyword?)
                                              :min-count 1))
                                  :min-count 1)
  "Directories under which `lein clean` removes files.
Specified by keyword or keyword-chain to get-in path in this defproject.
Both a single path and a collection of paths are accepted as each.
For example, if the other parts of project are like:
  :target-path \"target\"
  :compile-path \"classes\"
  :foobar-paths [\"foo\" \"bar\"]
  :baz-config {:qux-path \"qux\"}
:clean-targets below lets `lein clean` remove files under \"target\",
\"classes\", \"foo\", \"bar\", \"qux\", and \"out\".
By default, will protect paths outside the project root and within standard
lein source directories (\"src\", \"test\", \"resources\", \"doc\", \"project.clj\").
However, this protection can be overridden with metadata on the :clean-targets
vector - ^{:protect false}")

(def-key ::clean-non-project-classes boolean?
  "Workaround for http://dev.clojure.org/jira/browse/CLJ-322 by deleting
compilation artifacts for namespaces that come from dependencies.

Example:

  :clean-non-project-classes true")

(def-key ::checkout-deps-shares (s/every ::s/any :min-count 1)
"Paths to include on the classpath from each project in the
checkouts/ directory. (See the FAQ in the Readme for more details
about checkout dependencies.) Set this to be a vector of
functions that take the target project as argument. Defaults to
[:source-paths :compile-path :resource-paths], but you could use
the following to share code from the test suite:

Example:

  :checkout-deps-shares [:source-paths :test-paths
                         ~(fn [p] (str (:root p) \"/lib/dev/*\"))]")

;; TODO fix this
#_(def-key ::test-selectors
  )

(def-key ::monkeypatch-clojure-test boolean?
  "In order to support the `retest` task, Leiningen must monkeypatch the
clojure.test library. This disables that feature and breaks `lein retest`.

Example:

  :monkeypatch-clojure-test false")


;; TODO are we dealing with data or evaled data for sexps??
(def-key ::prompt sequential-sexp?
  "Specify the string to print when prompting for input.
  defaults to something like (fn [ns] (str *ns* \"=> \"))

Example:

  :prompt (fn [ns] (str \"your command for <\" ns \">, master? \" ))")

(def-key ::welcome sequential-sexp?
  "What to print when the repl session starts.

Example:

  :welcome (println \"Welcome to the magical world of the repl!\")")

(def-key ::init-ns symbol?
  "Specify the ns to start the REPL in (overrides :main in
this case only)

Example:

  :init-ns foo.bar")

(def-key ::init sequential-sexp?
  "This expression will run when first opening a REPL, in the
namespace from :init-ns or :main if specified.

Example:

  :init (println \"Welcome to the magical world of the repl!\")")

(def-key ::caught symbol?
  "Print stack traces on exceptions (highly recommended, but
currently overwrites *1, *2, etc).

Example:

  :caught clj-stacktrace.repl/pst+")

(def-key ::skip-default-init boolean?
  "Skip's the default requires and printed help message.

Example:

  :skip-default-init false")

(def-key ::host non-blank-string?
  "Customize the socket the repl task listens on and
attaches to.

Example:

  :skip-default-init false")

(def-key ::port integer?
  "Customize the socket the port or the nrepl server.

Example:

  :port 4001")

(def-key ::timeout integer?
  "If nREPL takes too long to load it may timeout,
increase this to wait longer before timing out.
Defaults to 30000 (30 seconds)

Example:

  :port 40000")

(def-key ::nrepl-handler sequential-sexp?
  "nREPL server customization
Only one of #{:nrepl-handler :nrepl-middleware}
may be used at a time.
Use a different server-side nREPL handler.

Example:

  :nrepl-handler (clojure.tools.nrepl.server/default-handler)")

(def-key ::nrepl-middleware (s/every
                             (s/or :sym symbol?
                                   :seq sequential-sexp?)
                             :min-count 1)
  "Add server-side middleware to nREPL stack.

Example:

  :nrepl-middleware [my.nrepl.thing/wrap-amazingness
                     ;; TODO: link to more detailed documentation.
                     ;; Middleware without appropriate metadata
                     ;; (see clojure.tools.nrepl.middleware/set-descriptor!
                     ;; for details) will simply be appended to the stack
                     ;; of middleware (rather than ordered based on its
                     ;; expectations and requirements).
                     (fn [handler]
                       (fn [& args]
                         (prn :middle args)
                         (apply handler args)))]")

(def-key ::repl-options
  (ssp/strict-keys
   :opt-un
   [::prompt
    ::welcome
    ::init-ns
    ::init
    ::caught
    ::skip-default-init
    ::host
    ::port
    ::timeout
    ::nrepl-handler
    ::nrepl-middleware])
  "Options to change the way the REPL behaves.

Example:

  :repl-options {;; Specify the string to print when prompting for input.
                 ;; defaults to something like (fn [ns] (str *ns* \"=> \"))
                 :prompt (fn [ns] (str \"your command for <\" ns \">, master? \" ))
                 ;; What to print when the repl session starts.
                 :welcome (println \"Welcome to the magical world of the repl!\")
                 ;; Specify the ns to start the REPL in (overrides :main in
                 ;; this case only)
                 :init-ns foo.bar
                 ;; This expression will run when first opening a REPL, in the
                 ;; namespace from :init-ns or :main if specified.
                 :init (println \"here we are in\" *ns*)
                 ;; Print stack traces on exceptions (highly recommended, but
                 ;; currently overwrites *1, *2, etc).
                 :caught clj-stacktrace.repl/pst+
                 ;; Skip's the default requires and printed help message.
                 :skip-default-init false
                 ;; Customize the socket the repl task listens on and
                 ;; attaches to.
                 :host \"0.0.0.0\"
                 :port 4001
                 ;; If nREPL takes too long to load it may timeout,
                 ;; increase this to wait longer before timing out.
                 ;; Defaults to 30000 (30 seconds)
                 :timeout 40000
                 ;; nREPL server customization
                 ;; Only one of #{:nrepl-handler :nrepl-middleware}
                 ;; may be used at a time.
                 ;; Use a different server-side nREPL handler.
                 :nrepl-handler (clojure.tools.nrepl.server/default-handler)
                 ;; Add server-side middleware to nREPL stack.
                 :nrepl-middleware [my.nrepl.thing/wrap-amazingness
                                    ;; TODO: link to more detailed documentation.
                                    ;; Middleware without appropriate metadata
                                    ;; (see clojure.tools.nrepl.middleware/set-descriptor!
                                    ;; for details) will simply be appended to the stack
                                    ;; of middleware (rather than ordered based on its
                                    ;; expectations and requirements).
                                    (fn [handler]
                                      (fn [& args]
                                        (prn :middle args)
                                        (apply handler args)))]}")

(def-key ::jar-name non-blank-string?
  "Name of the jar file produced. Will be placed inside :target-path.
Including %s will splice the project version into the filename.

Example:

  :jar-name \"sample.jar\"")

(def-key ::uberjar-name non-blank-string?
  "Name of the uber jar file produced. Will be placed inside :target-path.
Including %s will splice the project version into the filename.

Example:

  :uberjar-name \"sample.jar\"")

(def-key ::omit-source boolean?
  "Files with names matching any of these patterns will be excluded from jars.

Example:

  :omit-source true")

(def-key ::jar-exclusions (s/every regex? :min-count 1)
  "Files with names matching any of these patterns will be excluded from jars.

Example:

  :jar-exclusions [#\"(?:^|/).svn/\"]")

(def-key ::uberjar-exclusions (s/every regex? :min-count 1)
  "Files with names matching any of these patterns will be excluded from jars.

Example:

  :uberjar-exclusions [#\"META-INF/DUMMY.SF\"]")

(def-key ::auto-clean boolean?
  "By default Leiningen will run a clean before creating jars to prevent
undeclared AOT from leaking to downstream consumers; this disables
that behaviour.

Example:

  :auto-clean false")

(def-key ::uberjar-merge-with (s/map-of
                               (s/or :reg regex?
                                     :str string?)
                               (s/cat :fn1 symbol?
                                      :fn2 symbol?
                                      :fn3 symbol?))
  "Files to merge programmatically in uberjars when multiple same-named files
exist across project and dependencies.  Should be a map of filename strings
or regular expressions to a sequence of three functions:

 1. Takes an input stream; returns a parsed datum.
 2. Takes a new datum and the current result datum; returns a merged datum.
 3. Takes an output stream and a datum; writes the datum to the stream.

Resolved in reverse dependency order, starting with project.

Example:

  :uberjar-merge-with {#\"\\.properties$\" [slurp str spit]}")


(def-key ::validate boolean?
  "A boolean value that determines whether the project.clj is
validated against a schema. Set this to false to turn schema
validation off.
Default: true

Example:

  :validate false")

(defn validate-project [data-to-validate file-path]
  (when-not (false? (:validate data-to-validate))
    (let [data-to-validate (fuz/fuzzy-select-keys
                            data-to-validate
                            keys-to-validate)]
      (when-not (s/valid? ::leiningen-project-root data-to-validate)
        (let [first-error (-> (s/explain-data ::leiningen-project-root data-to-validate)
                              (ssp/prepare-errors data-to-validate
                                                  (when (.exists (io/file file-path))
                                                    file-path))
                              first)
              message (binding [ssp/*explain-header* "Leiningen Configuration Error"]
                        (-> first-error
                            ssp/error->display-data
                            ssp/explain-out*
                            with-out-str))]
          {:error first-error
           :message message})))))
