;; This is an annotated example of the options that may be set in a
;; project.clj file. It is a fairly contrived example in order to
;; cover all options exhaustively; it shouldn't be considered a
;; representative configuration.

;; The project is named "sample", and its group-id is "org.example".
(defproject org.example/sample "1.0.0-SNAPSHOT" ; version "1.0.0-SNAPSHOT"
  ;; Beyond this point you may prepend a form with unquote, or ~, to eval it.

  ;; The descrption is used to allow searchability when uploaded to Clojars.
  :description "A sample project"
  ;; The URL is also metadata that Clojars uses.
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
  ;; artifacts. :licence can be used in place of :license, and for projects
  ;; which allow their users to choose among several licenses, :licenses
  ;; and :licences keys are supported. NB. all licenses mentioned in
  ;; project.clj under any of the supported keys will be put in pom.xml.
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  ;; Dependencies are listed as [group-id/name version].
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  ;; Before fetching dependencies, the contents of the lib/ directory
  ;; will get deleted unless this is set to true.
  :disable-implicit-clean false
  ;; Dev dependencies are intended for use only during
  ;; development. Projects that depend on this project will not pull
  ;; in its dev-dependencies, and they won't be included in the uberjar.
  :dev-dependencies [[org.clojure/swank-clojure "1.2.1"]]
  ;; These namespaces will be AOT-compiled. Needed for gen-class and
  ;; other Java interop functionality. :namespaces is an alias for this.
  :aot [org.example.sample.SampleClass]
  ;; This namespace will be used as the "main" in the uberjar.
  :main [org.example.sample]
  ;; Emit warnings on all reflection calls.
  :warn-on-reflection true
  ;; Set this in order to only use the :repositories you list below.
  :omit-default-repositories true
  :repositories { "java.net" "http://download.java.net/maven/2"
                  "jboss" "http://repository.jboss.com/maven2/"}
  ;; If you'd rather use a different directory structure, you can set these.
  :source-path "src/main/clojure"
  :library-path "target/dependency"
  :test-path "src/test/clojure"
  :resources-path "src/main/resources"
  :native-path "src/native" ; where to look for native dependencies
  :jar-dir "target/" ; where to place the project's jar file
  :jar-name "sample.jar" ; name of the jar produced by 'lein jar'
  :uberjar-name "sample-standalone.jar" ; as above for uberjar
  ;; You can set JVM-level options here.
  :jvm-opts "-Xmx1g")
