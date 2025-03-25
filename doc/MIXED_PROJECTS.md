<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Polyglot (Clojure, Java) Projects With Leiningen](#polyglot-clojure-java-projects-with-leiningen)
  - [Source Layout](#source-layout)
  - [Java Source Compilation](#java-source-compilation)
  - [Setting Java Compiler Options With Leiningen](#setting-java-compiler-options-with-leiningen)
  - [Interleaving Compilation Steps](#interleaving-compilation-steps)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Polyglot (Clojure, Java) Projects With Leiningen

Clojure is a hosted language that encourages interoperability with its
platform. It is not uncommon to find some amount of Java code in Clojure
projects managed by Leiningen.

This guide explains how you can control source code layout with Leiningen,
compile Java sources and other topics related to polyglot codebases.


## Source Layout

By default, Leiningen assumes your project only has Clojure source code under
`src`. When using both Clojure and Java in the same codebase, however, it is
necessary to tell Leiningen where to find Java sources.

To do so, use `:source-paths` and `:java-source-paths` options in the project
definition:

```clojure
(defproject megacorp/superservice "1.0.0-SNAPSHOT"
  :description "A Clojure project with a little bit of Java sprinkled here and there"
  :min-lein-version  "2.0.0"
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"])
```

Having one source root contain another (e.g. `src` and `src/java`) can
cause obscure problems.


## Java Source Compilation

To compile Java sources, you can explicitly run

    lein javac

However, it is usually not necessary because tasks that need to run
project code (`lein test`, `lein run`, etc.) will trigger compilation
automatically. Manually running `lein javac` may be necessary when
using `lein do`, `lein with-profiles` or `lein repl` actively while
also actively changing Java sources in the project.

Running

    lein clean

will remove all compilation artifacts.


## Setting Java Compiler Options With Leiningen

When compiling Java sources, it may be necessary to pass extra arguments to the
compiler. For example, it is very important to target the JVM version you are
going to deploy your project to.

This is done via the `:javac-options` which takes a vector of arguments as you
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

Leiningen 2 and later versions uses the [JDK compiler API](https://docs.oracle.com/javase/7/docs/technotes/guides/javac/index.html) to compile Java sources.

Failing to specify the target version will lead JDK compiler to target whatever JDK
Leiningen is running on. It is a good practice to explicitly specify target JVM
version in mixed Clojure/Java projects.

## Interleaving Compilation Steps

In some cases it may be necessary to alternate between compiling
different languages. For instance, systems that generate and
reference Java sources may also provide Clojure code for the generated
sources to use.

Any Clojure code referenced by Java sources must be
[AOT compiled](https://clojure.org/compilation) to make it available to
the Java compiler. Similarly, the Java classes produced by `javac`
must be available for Clojure code that depends on it. This results
in steps of `compile` `javac` `compile`, whereas the default task
order is simply `javac` `compile`.

This sequence can be accomplished by executing lein with different
profiles. A profile can be built to perform the initial steps, while
another profile continues to the final compilation stage. For
instance, the following is an example of a profile called `:precomp`
that AOT compiles the `ex.ast` namespace. The sources for this first
step are kept in separate directory from the source directory used by
the default profile:

```clojure
  :profiles { :precomp { :source-paths ["src/pre/clojure"]
                         :aot [ex.ast] } }
```

This profile can then be compiled using: `lein with-profile precomp compile`
n
Once this is done, the default profile can be used in a separate
invocation of `lein` to perform the `javac` and `compile` steps.

The following is a complete example of a project that interleaves
Clojure and Java compiling. The entire project uses Clojure, except
for generated Java sources. In this case, the project uses the Beaver
parser generator to create Java source code which calls code written
in Clojure. The resulting parser is then referenced by Clojure code.

```clojure
(defproject example/parser "0.0.1"
  :description "Parser written in Clojure, with generated Java sources"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [net.sf.beaver/beaver-ant "0.9.9"]]
  :plugins [[lein-beaver "0.1.2-SNAPSHOT"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["target/src"]
  :grammar-src-dir "src/grammar"
  :grammar-dest-dir "target/src"
  :profiles { :precomp { :prep-tasks ^:replace ["beaver" "compile"]
                         :source-paths ["src/pre/clojure"]
                         :aot [parser.ast] } })
 ```

The `:prep-tasks` attribute in the profile adds the source-code
generation step into the sequence of operations to be performed when
compiling (though this could have been added to the default profile
instead - so long as it occurs before the javac). The `:beaver` task
uses `:grammar-src-dir` to find the grammar files and creates the Java
sources in the directory specified by `:grammar-dest-dir` (this is
placed in "target/" to ensure that it gets removed during a clean). It
should be apparent that the generated code is going to use classes
and/or protocols found in the `parser.ast` namespace, which is why
this namespace is AOT compiled. Also, note that the target of the
`beaver` step matches the sources of the default profile for the
subsequent `javac` step.

Running the `precomp` profile generates the .java sources and compiles
the `parser.ast` namespace:

```bash
$ lein with-profile precomp compile
```

The project is now ready to complete compilation normally. For
instance, invoking `lein test` or `lein uberjar` will cause `javac`
and `compile` to run first.

## Other Languages

Java is not the only language you can mix with Leiningen, but it's the
only one supported out of the box. Plugins exist for
[Scala](https://github.com/technomancy/lein-scalac) and
[Groovy](https://github.com/kurtharriger/lein-groovyc) as well.
