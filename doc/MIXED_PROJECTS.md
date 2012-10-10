# Polyglot (Clojure, Java) Projects With Leiningen

Clojure is a hosted language that encourages interoperability with its
platform. It is not uncommon to find some amount of Java code in Clojure
projects managed by Leiningen.

This guide explains how you can control source code layout with Leiningen,
compile Java sources and other topics related to polyglot codebases.


## Source Layout

By default, Leiningen assumes your project only has Clojure source code under
`src`. When using both Clojure and Java in the same codebase, however, it is
necessary to tell Leiningen where to find Java sources and *use a separate
directory for Clojure code*. The latter is also a highly recommended practice
if you use an IDE such as IntelliJ IDEA or Eclipse.

To do so, use `:source-paths` and `:java-source-path` options in the project
definition:

```clojure
(defproject megacorp/superservice "1.0.0-SNAPSHOT"
  :description "A Clojure project with a little bit of Java sprinkled here and there"
  :min-lein-version  "2.0.0"
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"])
```

Overlapping source roots (e.g. `src` and `src/java`) can cause obscure problems.


## Java Source Compilation

To compile Java sources, you can explicitly run

    lein javac

However, it is usually not necessary because tasks such as `lein test` will
trigger compilation automatically. Manually running `lein javac` may be necessary
when using `lein do`, `lein with-profiles` or `lein repl` actively while also
actively changing Java sources in the project.

Running

    lein clean

will clean all compilation artifacts.


## Setting Java Compiler Options With Leiningen

When compiling Java sources, it may be necessary to pass extra arguments to the
compiler. For example, it is very important to target the JVM version you are
going to deploy your project to.

This is done via the `;javac-options` which takes a vector of arguments as you
would pass them to `javac` on the command line. In this case we say that Java
sources use features up to JDK 6 and target JVM is also version 6:

```clojure
(defproject megacorp/superservice "1.0.0-SNAPSHOT"
  :description "A Clojure project with a little bit of Java sprinkled here and there"
  :min-lein-version "2.0.0"
    :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"])
```

Leiningen 2 and later versions uses the [JDK compiler API](http://docs.oracle.com/javase/7/docs/technotes/guides/javac/index.html) to compile Java sources.

Failing to specify the target version will lead JDK compiler to target whatever JDK
Leiningen is running on. It is a good practice to explicitly specify target JVM
version in mixed Clojure/Java projects.
