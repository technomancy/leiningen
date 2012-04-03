;; This is an annotated example of the options that may be set in a
;; project.clj file. It is a fairly contrived example in order to
;; cover all options exhaustively; it shouldn't be considered a
;; representative configuration. For a more detailed explanation of
;; some of the terms run "lein help tutorial".

;; The project is named "sample", and its group-id is "org.example".
(defproject org.example/sample "1.0.0-SNAPSHOT" ; version "1.0.0-SNAPSHOT"
  ;; Beyond this point you may prepend a form with unquote, or ~, to eval it.

  ;; The descrption text is searchable from repositories like Clojars.
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
  ;; :repo means it is ok for public repositories to host this project's
  ;; artifacts. A seq of :licenses is also supported.
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  ;; Dependencies are listed as [group-id/name version].
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.jclouds/jclouds "1.0" :classifier "jdk15"]
                 [net.sf.ehcache/ehcache "2.3.1" :extension "pom"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  ;; Global exclusions are applied across the board, as an alternative
  ;; to duplication for multiple depedencies with the same excluded libraries.
  :exclusions [org.apache.poi/poi
               org.apache.poi/poi-ooxml]
  :plugins [[lein-pprint "1.1.1"]
            [lein-assoc "0.1.0"]]
  ;; Each active profile gets merged into the project map. The :dev
  ;; and :user profiles are active by default, but the latter should be
  ;; looked up in ~/.lein/profiles.clj rather than set in project.clj.
  ;; Use the with-profiles higher-order task to run a task with a
  ;; different set of active profiles.
  :profiles {:dev {:resource-paths ["dummy-data"]
                   :dependencies [[clj-stacktrace "0.2.4"]]}
             :debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0-alpha1"]]}}
  :aliases {"launch" "run"
            "with-magic" ["assoc" ":magic" "true"]}
  ;; Warns users of earlier versions of Leiningen.
  :min-lein-version "2.0.0"
  ;; Paths to include on the classpath from each project in the
  ;; checkouts/ directory. (See the FAQ in the Readme for more details
  ;; about checkout dependencies.) Set this to be a vector of
  ;; functions that take the target project as argument. Defaults to
  ;; [:source-path :compile-path :resources-path], but you could use
  ;; the following to share code from the test suite:
  :checkout-deps-shares [:source-path :test-path
                         ~(fn [p] (str (:root p) "/lib/dev/*"))]
  ;; Load these namespaces on startup to pick up hooks from them.
  :hooks [leiningen.hooks.difftest]
  ;; Predicates to determine whether to run a test or not. See tutorial.
  :test-selectors {:default (fn [t] (not (or (:integration v) (:regression v))))
                   :integration :integration
                   :regression :regression}
  ;; These namespaces will be AOT-compiled. Needed for gen-class and
  ;; other Java interop functionality. :namespaces is an alias for this.
  ;; Put a regex here to compile all namespaces whose names match.
  :aot [org.example.sample]
  ;; The -main function in this namespace will be run at launch if you
  ;; create an uberjar. Repl sessions will start in this namespace as well.
  ;; Set :skip-aot metadata on this symbol to use it for other things like the
  ;; run task or shell wrappers without bringing in AOT if you don't need an
  ;; executable uberjar.
  :main org.example.sample
  ;; This namespace will get loaded automatically when you launch a repl.
  :repl-init sample.repl-helper
  ;; These will get passed to clojure.main/repl; see its docstring for details.
  :reply-options [:prompt (fn [] (print "your command, master? ") (flush))]
  ;; Customize the socket the repl task listens on.
  :repl-port 4001
  :repl-host "0.0.0.0"
  ;; If your -main namespace takes a long time to load, it could time out the
  ;; repl connection. Increase this to give it more time. Defaults to 100.
  :repl-retry-limit 1000
  ;; Forms to prepend to every form that is evaluated inside your project.
  ;; Allows working around the Gilardi Scenario: http://technomancy.us/143
  :injections [(require 'clojure.pprint)]
  ;; Emit warnings on all reflection calls.
  :warn-on-reflection true
  ;; Set this in order to only use the :repositories you list below.
  :omit-default-repositories true
  ;; These repositories will be searched for :dependencies and
  ;; :plugins and will also be available to deploy to.
  :repositories {"java.net" "http://download.java.net/maven/2"
                 "sonatype"
                 {:url "http://oss.sonatype.org/content/repositories/releases"
                  ;; If a repository contains  releases only; setting :snapshots
                  ;; to false will speed up dependency checking.
                  :snapshots false
                  ;; You can also set the policies for how to handle :checksum
                  ;; failures to :fail, :warn, or :ignore. In :releases, :daily,
                  ;; :always, and :never are supported.
                  :releases {:checksum :fail :update :always}}
                 ;; Repositories named "snapshots" and "releases" automatically
                 ;; have their :snapshots and :releases disabled as appropriate.
                 ;; Credentials for repositories should *not* be stored
                 ;; in project.clj but in ~/.lein/profiles.clj instead:
                 ;; {:auth {:repository-auth {#"http://blueant.com/archiva/"
                 ;;                           {:username "milgrim"
                 ;;                            :password "locative.1"}}}}
                 "snapshots" "http://blueant.com/archiva/snapshots"
                 "releases" "http://blueant.com/archiva/internal"}
  ;; the deploy task will give preference to repositories specified in
  ;; :deploy-repositories, and repos listed there will not be used for
  ;; dependency resolution.
  :deploy-repositories {"releases" "http://blueant.com/archiva/internal/releases"
                        "snapshots" "http://blueant.com/archiva/internal/snapshots"}
  ;; Prevent Leiningen from checking the network for dependencies.
  ;; This wouldn't normally be set in project.clj; it would come from a profile.
  :offline? true
  ;; Override the location of the local maven repository.
  :local-repo "/home/dude/.lein/repo"
  ;; If you'd rather use a different directory structure, you can set these.
  ;; Paths that contain "inputs" are vectors, "outputs" are strings.
  :source-paths ["src" "src/main/clojure"]
  :compile-path "target/classes" ; for .class files
  :test-paths ["test" "src/test/clojure"]
  :resource-paths ["src/main/resource"] ; non-code files included in classpath/jar
  :native-path "src/native"        ; where to extract native dependencies
  :target-path "target/"           ; where to place the project's jar file
  :jar-name "sample.jar"           ; name of the jar produced by 'lein jar'
  :uberjar-name "sample-standalone.jar" ; as above for uberjar
  ;; Options to pass to java compiler for java source
  ;; See http://ant.apache.org/manual/Tasks/javac.html
  :javac-options {:destdir "classes/"}
  :java-source-paths ["src/main/java"] ; location of Java source
  ;; Leave the contents of :source-path out of jars (for AOT projects)
  :omit-source true
  ;; Files with names matching any of these patterns will be excluded from jars
  :jar-exclusions [#"(?:^|/).svn/"]
  ;; Same thing, but for uberjars.
  :uberjar-exclusions [#"META-INF/DUMMY.SF"]
  ;; Add arbitrary jar entries. Supports :path, :paths, :bytes, and :fn types.
  :filespecs [{:type :path :path "config/base.clj"}
              ;; directory paths are included recursively
              {:type :paths :paths ["config/web" "config/cli"]}
              ;; programmatically-generated content can use :bytes
              {:type :bytes :path "project.clj"
               ;; strings or byte arrays are accepted
               :bytes ~(slurp "project.clj")}
              ;; :fn filespecs take the project as an argument and
              ;; should return a filespec map of one of the other types.
              {:type :fn :fn (fn [p]
                               {:type :bytes :path "git-log"
                                :bytes (:out (clojure.java.shell/sh
                                              "git" "log" "-n" "1"))})}]
  ;; Set arbitrary key/value pairs for the jar's manifest.
  :manifest {"Project-awesome-level" "super-great"}
  ;; You can set JVM-level options here.
  :jvm-opts ["-Xmx1g"]
  ;; If your project is a Leiningen plugin, set this to skip the subprocess step
  :eval-in-leiningen false
  ;; Set parent for working with in a multi-module maven project
  :parent [org.example/parent "0.0.1" :relative-path "../parent/pom.xml"]
  ;; Extensions here will be propagated to the pom.
  :extensions [[org.apache.maven.wagon/wagon-webdav "1.0-beta-2"]
               [foo/bar-baz "1.0"]])
