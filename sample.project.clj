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
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [org.jclouds/jclouds "1.0-RC6" :classifier "jdk15"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  ;; Dev dependencies are intended for use only during
  ;; development. Projects that depend on this project will not pull
  ;; in its dev-dependencies, and they won't be included in the uberjar.
  :dev-dependencies [[org.clojure/swank-clojure "1.2.1"]]
  ;; Global exclusions are applied across the board, as an alternative
  ;; to duplication for multiple depedencies with the same excluded libraries.
  :exclusions [org.apache.poi/poi
               org.apache.poi/poi-ooxml]
  ;; Only re-fetch dependencies when they change in project.clj or
  ;; when :library-path directory is empty.
  :checksum-deps true
  ;; Warns users of earlier versions of Leiningen.
  :min-lein-version "1.3.0"
  ;; Before fetching dependencies, the contents of the lib/ directory
  ;; will get deleted unless this is set to true.
  :disable-deps-clean false
  ;; Delete .class files that do not have a correspoinding package in
  ;; the src/ directory. Workaround for Clojure bug CLJ-322. Causes problems
  ;; with protocols in upstream libraries; false by default. Set to
  ;; true to delete all non-project classes or set to a seq of regexes
  ;; to only delete class files that match one of the regexes.
  :clean-non-project-classes true
  ;; If :clean-non-project-classes is set to true, you can set this to
  ;; be a regex; all class filenames that match this regex will be preserved.
  :class-file-whitelist #"^(org/example|clojure)"
  ;; Additional files (besides :compile-path contents and jars/uberjars)
  ;; to be deleted during clean phase. May contain %s, which will be replaced
  ;; with the project's current version number.
  :extra-files-to-clean ["tmp" "sample-%s.tar"]
  ;; If the files you want to delete can't be exact matches, you can
  ;; use a regex that will be matched against filenames in the project root.
  ;; Defaults to #"^$NAME-.*\.jar$".
  :regex-to-clean #"hs_err_pid.*"
  ;; Paths to include on the classpath from each project in the
  ;; checkouts/ directory. (See the FAQ in the Readme for more details
  ;; about checkout dependencies.) Set this to be a vector of
  ;; functions that take the target project as argument. Defaults to
  ;; [:source-path :compile-path :resources-path], but you could use
  ;; the following to share code from the test suite:
  :checkout-deps-shares [:source-path :test-path
                         ~(fn [p] (str (:root p) "/lib/dev/*"))]
  ;; Load these namespaces on startup to pick up hooks from them. Hooks
  ;; generally come from plugins, but may be included in your project source.
  :hooks [leiningen.hooks.difftest]
  ;; Predicates to determine whether to run a test or not. See tutorial.
  :test-selectors {:default (fn [t] (not (or (:integration v) (:regression v))))
                   :integration :integration
                   :regression :regression}
  ;; Set this to true to search the classpath for hooks. Will load all
  ;; namespaces matching leiningen.hooks.*. Warning: this will cause
  ;; Leiningen to start slowly, especially with many dependencies.
  :implicit-hooks false
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
  ;; This file will get loaded automatically when you launch a repl,
  ;; but it's deprecated; use :repl-init above instead.
  :repl-init-script "src/main/clojure/init.clj"
  ;; These will get passed to clojure.main/repl; see its docstring for details.
  :repl-options [:prompt (fn [] (print "your command, master? ") (flush))]
  ;; Customize the socket the repl task listens on.
  :repl-port 4001
  :repl-host "0.0.0.0"
  ;; A form to prepend to every form that is evaluated inside your project.
  ;; Allows working around the Gilardi Scenario: http://technomancy.us/143
  :project-init (require 'clojure.pprint)
  ;; If your -main namespace takes a long time to load, it could time out the
  ;; repl connection. Increase this to give it more time. Defaults to 100.
  :repl-retry-limit 1000
  ;; Emit warnings on all reflection calls.
  :warn-on-reflection true
  ;; Set this in order to only use the :repositories you list below. Note that 
  ;; a bug in maven-ant-tasks prevents Leiningen from excluding Maven Central,
  ;; so in effect this simply omits Clojars.
  :omit-default-repositories true
  :repositories {"java.net" "http://download.java.net/maven/2"
                 "sonatype"
                 {:url "http://oss.sonatype.org/content/repositories/releases"
                  ;; If a repository contains  releases only; setting :snapshots
                  ;; to false will speed up dependency checking.
                  :snapshots false
                  ;; You can also set the policies for how to handle :checksum
                  ;; failures to :fail, :warn, or :ignore. In :releases, :daily,
                  ;; :always, and :never are supported.
                  :releases {:checksum :fail
                             :update :always}}
                 ;; Repositories named "snapshots" and "releases" automatically
                 ;; have their :snapshots and :releases disabled as appropriate.
                 "snapshots" {:url "http://blueant.com/archiva/snapshots"
                              ;; Also supports :private-key and :passphrase.
                              :username "milgrim" :password "locative.1"}
                 "releases" {:url "http://blueant.com/archiva/internal"
                             :username "milgrim" :password "locative.1"}}
  ;; If you'd rather use a different directory structure, you can set these.
  :source-path "src/main/clojure"
  :library-path "target/dependency"
  :test-path "src/test/clojure"
  :resources-path "src/main/resource" ; non-code files included in classpath/jar
  :dev-resources-path "src/test/resource" ; added to dev classpath but not jar
  :native-path "src/native"        ; where to look for native dependencies
  :target-dir "target/  "          ; where to place the project's jar file
  :extra-classpath-dirs ["script"] ; more classpath entries not included in jar
  :jar-name "sample.jar"           ; name of the jar produced by 'lein jar'
  :uberjar-name "sample-standalone.jar" ; as above for uberjar
  ;; Construct classpath from jars in ~/.m2 rather than copying to :library-path
  :local-repo-classpath true
  ;; Options to pass to java compiler for java source
  ;; See http://ant.apache.org/manual/Tasks/javac.html
  :javac-options {:destdir "classes/"}
  :java-source-path "src/main/java" ; location of Java source
  ;; Leave the contents of :source-path out of jars (for AOT projects)
  :omit-source true
  ;; Files with names matching any of these patterns will be excluded from jars
  :jar-exclusions [#"(?:^|/).svn/"]
  ;; Same thing, but for uberjars.
  :uberjar-exclusions [#"META-INF/DUMMY.SF"]
  ;; Set arbitrary key/value pairs for the jar's manifest.
  :manifest {"Project-awesome-level" "super-great"}
  ;; You can set JVM-level options here.
  :jvm-opts ["-Xmx1g"]
  ;; If your project is a Leiningen plugin, set this to skip the subprocess step
  :eval-in-leiningen false)

;; You can use Robert Hooke to modify behaviour of any task function,
;; but the prepend-tasks function is shorthand that is more convenient
;; on tasks that take a single project argument.
(use '[leiningen.core :only [prepend-tasks]]
     '[leiningen.deps :only [deps]]
     '[leiningen.clean :only [clean]]
     '[leiningen.pom :only [pom]])

(prepend-tasks #'deps clean pom)
