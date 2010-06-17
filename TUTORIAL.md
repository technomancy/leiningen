# Tutorial

For those of you new to the JVM who have never touched Ant or Maven in
anger: don't panic. Leiningen is designed with you in mind. This
tutorial will help you get started and explain Leiningen's take on
project building and JVM-land dependency management.

## Creating a Project

We'll assume you've got Leiningen installed as per the
[readme](http://github.com/technomancy/leiningen/blob/master/README.md). 
Generating a new project is easy:

    $ lein new myproject

    Created new project in: myproject

    $ cd myproject
    $ tree
    .
    |-- project.clj
    |-- README
    |-- src
    |   `-- myproject
    |       `-- core.clj
    `-- test
        `-- myproject
            `-- core_test.clj

Here we've got your project's README, a src/ directory containing
implementation code, a test/ directory, and a project.clj file which
describes your project to Leiningen. The src/myproject/core.clj file
corresponds to the myproject.core namespace.

Note that we use that instead of just myproject since single-segment
namespaces are discouraged in Clojure. Also the file
test/myproject/core_test.clj corresponds with the myproject.core-test
namespace--you need to remember to replace dashes in namespace names
with underscores in file names on disk since the JVM has trouble
loading files with dashes in the name.

## Packaging

You can package your project up now, even though at this stage it's
fairly useless:

    $ lein jar

    Created ~/src/myproject/myproject-1.0.0-SNAPSHOT.jar

Libraries for the JVM are packaged up as .jar files, which are
basically just .zip files with a little extra JVM-specific metadata
that contain either .class files (bytecode) or .clj source
files. These jar files are available through repositories that serve
them up over HTTP alongside their metadata.

## project.clj

    $ cat project.clj

    (defproject myproject "1.0.0-SNAPSHOT"
      :description "FIXME: write"
      :dependencies [[org.clojure/clojure "1.1.0"]
                     [org.clojure/clojure-contrib "1.1.0"]])

Fill in the :description with a short paragraph so that your project
will show up in search results. At some point you'll need to flesh out
the README too, but for now let's skip ahead to setting :dependencies.
If you've got a simple pure-clojure project, you will be fine with the
default of Clojure and Contrib, but otherwise you'll need to list
other dependencies.

## Dependencies

[Clojars](http://clojars.org) is the Clojure community's centralized
jar repository, and it's where you'll find Clojure dependencies for your
project. Each dependency even lists out the snippet you'll need to put
in your project.clj to use it. Java libraries can be found by
searching [Jarvana](http://jarvana.com), though you'll need to
translate their notation into Leiningen's. Leiningen describes
packages using identifiers that look like this:

    [org.clojure/swank-clojure "1.2.1"]

* "org.clojure" is called the 'group-id'
* "swank-clojure" is called the 'artifact-id'
* "1.2.1" is the version of the jar file you require

Sometimes versions will end in "-SNAPSHOT". This means that it is not
an official release but a development build. In general relying on
snapshot dependencies is discouraged, but sometimes its necessary if
you need bug fixes etc. that have not made their way into a release
yet. Adding a snapshot dependency to your project will cause Leiningen
to actively go seek out the latest version of the dependency every
time you run <tt>lein deps</tt>, (whereas normal release versions are
cached in the local repository) so if you have a lot of snapshots it
will slow things down.

Speaking of the local repository, all the dependencies you pull in
using Leiningen or Maven get cached in $HOME/.m2/repository. You can
install the current project in there:

    $ lein install

    Wrote pom.xml
    [INFO] Installing myproject-1.0.0-SNAPSHOT.jar to ~/.m2/repository/myproject/myproject/1.0.0-SNAPSHOT/myproject-1.0.0-SNAPSHOT.jar

Generally Leiningen will fetch your dependencies on-demand, but if you
have just added a new dependency and you want to force it to fetch it,
you can do that too:

    $ lein deps

    Copying 2 files to ~/src/myproject/lib
    Copied :dependencies into ~/src/myproject/lib.

Dependencies are downloaded from Clojars, the central Maven (Java)
repository, the [official Clojure build
server](http://build.clojure.org), and any other repositories that you
add to your project.clj file; see the
[sample.project.clj](http://github.com/technomancy/leiningen/blob/master/sample.project.clj).

If you've confirmed that your project will work with a number of
different versions of a given dependency, you can provide a range
instead of a single version:

    [org.clojure/clojure "[1.1,1.2]"] ; <= will match 1.1.0 through 1.2.0.

See [Maven's version range
specification](http://maven.apache.org/plugins/maven-enforcer-plugin/rules/versionRanges.html)
for details.

## Dev Dependencies

Sometimes you want to pull in dependencies that are really only for
your convenience while developing; they aren't strictly required for
the project to function. Leiningen calls these
:dev-dependencies. They're listed in project.clj alongside regular
dependencies and downloaded when you run <tt>lein deps</tt>, but they
are not brought along when another project depends on your
project. Using [swank-clojure](http://github.com/technomancy/swank-clojure)
for Emacs support would be a typical example; you may not want it
included at runtime, but it's useful while you're hacking on the project.

## Writing the Code

This is the part Leiningen can't really help you with; you're on your
own here. Well--not quite. Leiningen can help you with running your
tests.

    $ lein test

    Testing myproject.core-test
    FAIL in (replace-me) (core_test.clj:6)
    No tests have been written.
    expected: false
      actual: false
    Ran 1 tests containing 1 assertions.
    1 failures, 0 errors.

Of course, we haven't written any tests yet, so we've just got the
skeleton failing tests that Leiningen gave us with <tt>lein
new</tt>. But once we fill it in the test suite will become more
useful. Sometimes if you've got a large test suite you'll want to run
just one or two namespaces worth:

    $ lein test myproject.parser-test

    Testing myproject.parser-test
    Ran 2 tests containing 10 assertions.
    0 failures, 0 errors.

When naming your test namespaces, it's a good idea to pick a name that
matches the name of the namespace it's testing, but with a "-test"
suffix. In stack traces that come from test failures you'll only see
the last segment of the namespace that caused the exception, so it's
convenient to have it distinguishable from the implementation
namespace.

## Compiling

If you're lucky you'll be able to get away without doing any AOT
(ahead-of-time) compilation. But there are some Java interop features
that require it, so if you need to use them you should add an :aot
option into your project.clj file. It should be a seq of namespaces
you want AOT-compiled. Again, the
[sample.project.clj](http://github.com/technomancy/leiningen/blob/master/sample.project.clj)
has example usage.

Like dependencies, this should happen for you automatically, but if
you need to force it you can:

    $ lein compile

    Compiling myproject.core

## Publishing

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to maintain your own or get
it into Maven central, the easiest way is to publish it at
[Clojars](http://clojars.org). Once you have created an account there,
publishing is easy:

    $ lein jar && lein pom
    $ scp pom.xml myproject-1.0.0.jar clojars@clojars.org:

Once that succeeds it will be available for other projects to depend
on.

## Uberjar

Not all Leiningen projects are libraries though--sometimes you want to
distribute your project to end-users who don't want to worry about
having a copy of Clojure lying around. You can use the
<tt>uberjar</tt> task to create a standalone executable jar.

For this to work you'll need to specify in project.clj a namespace as
your :main that contains a <tt>-main</tt> function which will get
called when your standalone jar is run. This namespace should have a
<tt>(:gen-class)</tt> declaration in the <tt>ns</tt> form at the
top. The <tt>-main</tt> function will get passed the command-line
arguments.

    $ lein uberjar
    Created ~/src/myproject/myproject-1.0.0.jar
    Including myproject-1.0.0.jar
    Including clojure-contrib-1.1.0.jar
    Including clojure-1.1.0.jar
    Created myproject-1.0.0-standalone.jar

This creates a single jar file that contains the contents of all your
dependencies. Users can run it with a simple <tt>java</tt> invocation,
or on some systems just by double-clicking the jar file.

    $ java -jar myproject-1.0.0-standalone.jar

## That's It!

If you prefer a visual introduction, try the Full Disclojure
screencast on [project management](http://vimeo.com/8934942). Now go
start coding your next project!
