;; This is an annotated example of the options that may be set in a project.clj
;; file. It is a fairly contrived example in order to cover lots of different
;; options; it shouldn't be considered a representative configuration. For a
;; more detailed explanation of some of the terms run ;; "lein help tutorial".

;; These options apply to Leiningen 2.x. See the 1.x branch for older versions:
;; https://github.com/technomancy/leiningen/blob/1.x/sample.project.clj

;; The project is named "sample", and its group-id is "org.example".
(defproject org.example/sample "1.0.0-SNAPSHOT" ; version "1.0.0-SNAPSHOT"
  ;; Beyond this point you may prepend a form with unquote, or ~, to eval it.

  ;; The description text is searchable from repositories like Clojars.
  :description "A sample project"
  :url "http://example.org/sample-clojure-project"
  ;; The mailing list of the project. If the project has multiple mailing
  ;; lists, use the :mailing-lists key (bound to a seq of mailing list
  ;; descriptions as below).
  :mailing-list {:name "sample mailing list"
                 :archive "http://example.org/sample-mailing-list-archives"
                 :other-archives ["http://example.org/sample-list-archive2"
                                  "http://example.org/sample-list-archive3"]
                 :post "list@example.org"
                 :subscribe "list-subscribe@example.org"
                 :unsubscribe "list-unsubscribe@example.org"}
  ;; The project's license. :distribution should be :repo or :manual;
  ;; :repo means it is OK for public repositories to host this project's
  ;; artifacts. A seq of :licenses is also supported.
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  ;; Dependencies are listed as [group-id/name version]; in addition
  ;; to keywords supported by Pomegranate, you can use :native-prefix
  ;; to specify a prefix. This prefix is used to extract natives in
  ;; jars that don't adhere to the default "<os>/<arch>/" layout that
  ;; Leiningen expects.
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.jclouds/jclouds "1.0" :classifier "jdk15" :scope "test"]
                 [net.sf.ehcache/ehcache "2.3.1" :extension "pom"]
                 [log4j "1.2.15" :exclusions [[javax.mail/mail :extension "jar"]
                                              [javax.jms/jms :classifier "*"]
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.lwjgl.lwjgl/lwjgl "2.8.5"]
                 [org.lwjgl.lwjgl/lwjgl-platform "2.8.5"
                  :classifier "natives-osx"
                  ;; LWJGL stores natives in the root of the jar; this
                  ;; :native-prefix will extract them.
                  :native-prefix ""]]
  ;; Abort when version ranges or version overlaps are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  :pedantic? :abort
  ;; Global exclusions are applied across the board, as an alternative
  ;; to duplication for multiple dependencies with the same excluded libraries.
  :exclusions [org.apache.poi/poi
               org.apache.poi/poi-ooxml]
  ;; Plugins are code that runs in Leiningen itself and usually
  ;; provide new tasks or hooks.
  :plugins [[lein-pprint "1.1.1"]
            [lein-assoc "0.1.0"]
            [s3-wagon-private "1.1.1"]
            [lein-foo "0.0.1" :hooks false]
            [lein-bar "0.0.1" :middleware false]]
  ;; If you configure a custom repository with a self-signed SSL
  ;; certificate, you will need to add it here. Paths should either
  ;; be on Leiningen's classpath or relative to the project root.
  :certificates ["blueant.pem"]
  ;; Each active profile gets merged into the project map. The :dev
  ;; and :user profiles are active by default, but the latter should be
  ;; looked up in ~/.lein/profiles.clj rather than set in project.clj.
  ;; Use the with-profiles higher-order task to run a task with a
  ;; different set of active profiles.
  ;; See `lein help profiles` for a detailed explanation.
  :profiles {:dev {:resource-paths ["dummy-data"]
                   :dependencies [[clj-stacktrace "0.2.4"]]}
             :debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}}
  ;; Support project-specific task aliases. These are interpreted in
  ;; the same way as command-line arguments to the lein command. If
  ;; the alias points to a vector, it uses partial application. For
  ;; example, "lein with-magic run -m hi.core" would be equivalent to
  ;; "lein assoc :magic true run -m hi.core". Remember, commas are not
  ;; considered to be special by argument parsers, they're just part
  ;; of the preceding argument.
  ;; For complex aliases, a docstring may be attached. The docstring will be
  ;; printed instead of the expansion when running `lein help`.
  :aliases {"launch" "run"
            "dumbrepl" ["trampoline" "run" "-m" "clojure.main/main"]
            "test!" ^{:doc "Recompile sources and fetch deps before testing."}
            ["do" "clean," "deps," "test"]}
  ;; Normally Leiningen runs the javac and compile tasks before
  ;; calling any eval-in-project code, but you can override this with
  ;; the :prep-tasks key to do other things like compile protocol buffers.
  :prep-tasks [["protobuf" "compile"] "javac" "compile"]
  ;; Warns users of earlier versions of Leiningen.
  :min-lein-version "2.0.0"
  ;; Paths to include on the classpath from each project in the
  ;; checkouts/ directory. (See the FAQ in the Readme for more details
  ;; about checkout dependencies.) Set this to be a vector of
  ;; functions that take the target project as argument. Defaults to
  ;; [:source-paths :compile-path :resource-paths], but you could use
  ;; the following to share code from the test suite:
  :checkout-deps-shares [:source-paths :test-paths
                         ~(fn [p] (str (:root p) "/lib/dev/*"))]
  ;; Load these namespaces from within Leiningen to pick up hooks from them.
  :hooks [leiningen.hooks.difftest]
  ;; Apply these middleware functions from plugins to your project when it
  ;; loads. Both hooks and middleware can be loaded implicitly by giving them a
  ;; name matching a specific pattern as well as by listing them here.
  :middleware [lein-xml.plugin/middleware]
  ;; Predicates to determine whether to run a test or not, take test metadata
  ;; as argument. See Leiningen tutorial for more information.
  :test-selectors {:default (fn [m] (not (or (:integration m) (:regression m))))
                   :integration :integration
                   :regression :regression}
  ;; These namespaces will be AOT-compiled. Needed for gen-class and
  ;; other Java interop functionality. Put a regex here to compile all
  ;; namespaces whose names match.
  :aot [org.example.sample]
  ;; The -main function in this namespace will be run at launch
  ;; (either via `lein run` or from an uberjar). It should be variadic:
  ;;
  ;; (ns my.service.runner
  ;;   (:gen-class))
  ;;
  ;; (defn -main
  ;;   "Application entry point"
  ;;   [& args]
  ;;   (comment Do app initialization here))
  :main my.service.runner
  ;; Options to change the way the REPL behaves.
  :repl-options {;; Specify the string to print when prompting for input.
                 ;; defaults to something like (fn [ns] (str *ns* "=> "))
                 :prompt (fn [ns] (str "your command for <" ns ">, master? " ))
                 ;; What to print when the repl session starts.
                 :welcome (println "Welcome to the magical world of the repl!")
                 ;; Specify the ns to start the REPL in (overrides :main in
                 ;; this case only)
                 :init-ns foo.bar
                 ;; This expression will run when first opening a REPL, in the
                 ;; namespace from :init-ns or :main if specified.
                 :init (println "here we are in" *ns*)
                 ;; Customize the socket the repl task listens on and
                 ;; attaches to.
                 :host "0.0.0.0"
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
                                        (apply handler args)))]}
  ;; Forms to prepend to every form that is evaluated inside your project.
  ;; Allows working around the Gilardi Scenario: http://technomancy.us/143
  :injections [(require 'clojure.pprint)]
  ;; Emit warnings on all reflection calls. - DEPRECATED (see below)
  :warn-on-reflection true
  ;; Sets the values of global variables within Clojure
  ;;  This example disables all pre- and post-conditions and emits warnings
  ;;  on reflective calls. See the Clojure documentation for the list of valid
  ;;  global variables to set (and their meaningful values).
  :global-vars {*warn-on-reflection* true
                *assert* false}
  ;; These repositories will be searched for :dependencies and
  ;; :plugins and will also be available to deploy to.
  ;; Add ^:replace (:repositories ^:replace [...]) to only use repositories you list below.
  :repositories [["java.net" "http://download.java.net/maven/2"]
                 ["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                              ;; If a repository contains releases only setting
                              ;; :snapshots to false will speed up dependencies.
                              :snapshots false
                              ;; Disable signing releases for this repo.
                              ;; (Not recommended.)
                              :sign-releases false
                              ;; You can also set the policies for how to handle
                              ;; :checksum failures to :fail, :warn, or :ignore.
                              :checksum :fail
                              ;; How often should this repository be checked
                              ;; for updates? (:daily, :always, or :never)
                              :update :always
                              ;; You can also apply them to releases only:
                              :releases {:checksum :fail :update :always}}]
                 ;; Repositories named "snapshots" and "releases" automatically
                 ;; have their :snapshots and :releases disabled as appropriate.
                 ;; Credentials for repositories should *not* be stored
                 ;; in project.clj but in ~/.lein/credentials.clj.gpg instead,
                 ;; see `lein help deploying` under "Authentication".
                 "snapshots" "http://blueant.com/archiva/snapshots"
                 "releases" {:url "http://blueant.com/archiva/internal"
                             ;; Using :env as a value here will cause an
                             ;; environment variable to be used based on
                             ;; the key; in this case LEIN_PASSWORD.
                             :username "milgrim" :password :env}]
  ;; These repositories will be included with :repositories when loading plugins.
  ;; This would normally be set in a profile for non-public repositories.
  ;; All the options are the same as in the :repositories map.
  :plugin-repositories [["internal-plugin-repo" "http://example.org/repo"]]
  ;; You can set :update and :checksum policies here to have them
  ;; apply for all :repositories. Usually you will not set :update
  ;; directly but apply the "update" profile instead.
  :update :always
  :checksum :fail
  ;; the deploy task will give preference to repositories specified in
  ;; :deploy-repositories, and repos listed there will not be used for
  ;; dependency resolution.
  :deploy-repositories [["releases" {:url "http://blueant.com/archiva/internal/releases"
                                     ;; Select a GPG private key to use for
                                     ;; signing. (See "How to specify a user
                                     ;; ID" in GPG's manual.) GPG will
                                     ;; otherwise pick the first private key
                                     ;; it finds in your keyring.
                                     ;; Currently only works in :deploy-repositories
                                     ;; or as a top-level (global) setting.
                                     :signing {:gpg-key "0xAB123456"}}]
                        ["snapshots" "http://blueant.com/archiva/internal/snapshots"]]
  ;; Fetch dependencies from mirrors. Mirrors override repositories when the key
  ;; in the :mirrors map matches either the name or URL of a specified
  ;; repository. All settings supported in :repositories may be set here too.
  :mirrors {"central" {:name "Ibiblio"
                       :url "http://mirrors.ibiblio.org/pub/mirrors/maven2"}
            #"clojars" {:name "Internal nexus"
                        :url "http://mvn.local/nexus/releases"
                        :repo-manager true}}
  ;; Java agents can instrument and intercept certain VM features. Include
  ;; :bootclasspath true to place the agent jar on the bootstrap classpath.
  :java-agents [[nodisassemble "0.1.1" :options "extra"]]
  ;; Defaults for signing options. Defers to per-repository settings.
  :signing {:gpg-key "root@eruditorum.org"}
  ;; Prevent Leiningen from checking the network for dependencies.
  ;; This wouldn't normally be set in project.clj; it would come from a profile.
  :offline? true
  ;; Override location of the local maven repository. Relative to project root.
  :local-repo "local-m2"
  ;; If you'd rather use a different directory structure, you can set these.
  ;; Paths that contain "inputs" are vectors, "outputs" are strings.
  :source-paths ["src" "src/main/clojure"]
  :java-source-paths ["src/main/java"] ; Java source is stored separately.
  :test-paths ["test" "src/test/clojure"]
  :resource-paths ["src/main/resource"] ; Non-code files included in classpath/jar.
  ;; All generated files will be placed here. In order to avoid cross-profile
  ;; contamination, by default this includes the names of all active profiles.
  ;; Putting %s in your custom :target-path will splice in the profile names.
  :target-path "target/"
  ;; Directory in which to place AOT-compiled files. Including %s will
  ;; splice the :target-path into this value.
  :compile-path "%s/classy-files"
  ;; Directory in which to extract native components from inside dependencies.
  ;; Including %s will splice the :target-path into this value. Note that this
  ;; is not where to *look* for existing native libraries; use :jvm-opts with
  ;; -Djava.library.path=... instead for that.
  :native-path "%s/bits-n-stuff"
  ;; Directories under which `lein clean` removes files.
  ;; Specified by keyword or keyword-chain to get-in path in this defproject.
  ;; Both a single path and a collection of paths are accepted as each.
  ;; For example, if the other parts of project are like:
  ;;   :target-path "target"
  ;;   :compile-path "classes"
  ;;   :foobar-paths ["foo" "bar"]
  ;;   :baz-config {:qux-path "qux"}
  ;; :clean-targets below lets `lein clean` remove files under "target",
  ;; "classes", "foo", "bar" and "qux".
  :clean-targets [:target-path :compile-path :foobar-paths
                  [:baz-config :qux-path]]
  ;; Name of the jar file produced. Will be placed inside :target-path.
  ;; Including %s will splice the project version into the filename.
  :jar-name "sample.jar"
  ;; As above, but for uberjar.
  :uberjar-name "sample-standalone.jar"
  ;; Options to pass to java compiler for java source,
  ;; exactly the same as command line arguments to javac.
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  ;; Leave the contents of :source-paths out of jars (for AOT projects).
  :omit-source true
  ;; Files with names matching any of these patterns will be excluded from jars.
  :jar-exclusions [#"(?:^|/).svn/"]
  ;; Same thing, but for uberjars.
  :uberjar-exclusions [#"META-INF/DUMMY.SF"]
  ;; Add arbitrary jar entries. Supports :path, :paths, :bytes, and :fn types.
  :filespecs [{:type :path :path "config/base.clj"}
              ;; Directory paths are included recursively.
              {:type :paths :paths ["config/web" "config/cli"]}
              ;; Programmatically-generated content can use :bytes.
              {:type :bytes :path "project.clj"
               ;; Strings or byte arrays are accepted.
               :bytes ~(slurp "project.clj")}
              ;; :fn filespecs take the project as an argument and
              ;; should return a filespec map of one of the other types.
              {:type :fn :fn (fn [p]
                               {:type :bytes :path "git-log"
                                :bytes (:out (clojure.java.shell/sh
                                              "git" "log" "-n" "1"))})}]
  ;; Set arbitrary key/value pairs for the jar's manifest.
  :manifest {"Project-awesome-level" "super-great"
             ;; Function values will be called with the project as an argument.
             "Class-Path" ~#(clojure.string/join
                             \space
                             (leiningen.core.classpath/get-classpath %))
             ;; Symbol values will be resolved to find a function to call.
             "Grunge-level" my.plugin/calculate-grunginess}
  ;; Use a different `java` executable for project JVMs.
  :java-cmd "/home/phil/bin/java1.7"
  ;; You can set JVM-level options here.
  ;; It is also possible to use :java-opts, which is an alias for :jvm-opts.
  :jvm-opts ["-Xmx1g"]
  ;; Control the context in which your project code is evaluated.
  ;; Defaults to :subprocess, but can also be :leiningen (for plugins)
  ;; or :classloader (experimental) to avoid starting a subprocess.
  :eval-in :leiningen
  ;; Enable bootclasspath optimization. This improves boot time but interferes
  ;; with using things like pomegranate at runtime and using Clojure 1.2.
  :bootclasspath true
  ;; Set parent for working with in a multi-module maven project.
  :parent [org.example/parent "0.0.1" :relative-path "../parent/pom.xml"]
  ;; Extensions here will be propagated to the pom but not used by Leiningen.
  :extensions [[org.apache.maven.wagon/wagon-webdav "1.0-beta-2"]
               [foo/bar-baz "1.0"]]
  ;; Include <scm> tag in generated pom.xml file. All key/value pairs
  ;; appear exactly as configured. If absent, Leiningen will try to
  ;; use information from a .git directory.
  :scm {:name "git"
        :tag "098afd745bcd"
        :url "http://127.0.0.1/git/my-project"
        ;; Allows you to use a repository in a different directory than the
        ;; project's root, for instance, if you had multiple projects in a
        ;; single git repository.
        :dir ".."}

  ;; Include arbitrary xml in generated pom.xml file,
  ;; as parsed by clojure.data.xml/sexp-as-element.
  :pom-addition [:developers [:developer {:id "benbit"}
                              [:name "Ben Bitdiddle"]
                              [:url "http://www.example.com/benjamin"]]]

  ;; Indicate whether or not `lein install` should abort when trying to install
  ;; releases. When false, trying to run `lein install` in a project with a version
  ;; that isn't a snapshot will cause leiningen to abort with a descriptive error
  ;; message.
  :install-releases? false

  ;; Dictate which git branches deploys should be allowed from. When set,
  ;; `lein deploy` will only work from the git branches included and will
  ;; abort otherwise.
  :deploy-branches ["master"])

;;; Environment Variables used by Leiningen

;; JAVA_CMD - executable to use for java(1)
;; JVM_OPTS - extra options to pass to the java command
;; DEBUG - increased verbosity
;; LEIN_HOME - directory in which to look for user settings
;; LEIN_SNAPSHOTS_IN_RELEASE - allow releases to depend on snapshots
;; LEIN_JVM_OPTS - tweak speed of plugins or fix compatibility with old Java versions
;; LEIN_REPL_HOST - interface on which to connect to nREPL server
;; LEIN_REPL_PORT - port on which to start or connect to nREPL server
;; LEIN_OFFLINE - equivalent of :offline? true but works for plugins
;; LEIN_GPG - gpg executable to use for encryption/signing
;; LEIN_NEW_UNIX_NEWLINES - ensure that `lein new` emits '\n' as newlines
;; http_proxy - host and port to proxy HTTP connections through
;; http_no_proxy - pipe-separated list of hosts which may be accessed directly
