(defproject alternating-language-compilation "0.1.0-SNAPSHOT"
  :description "Demonstrate compilation of Java that depends on Clojure that depends on Java..."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["src/java"]
  :source-paths      ["src/clojure"]
  ;; Direct Linking allows us to call into Clojure code from Java
  ;; without having to trigger var initialization in the runtime.
  ;; In this particular test case, we need it.
  :jvm-opts          ["-Dclojure.compiler.direct-linking=true"]
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :prep-tasks [["javac" "src/java/leiningen/test/alc"]
               ["compile" "leiningen.test.alc.naturalexpander"]
               "javac"
               "compile"]
  :main leiningen.test.euler65.Euler65)
