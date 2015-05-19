<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Tutorial](#tutorial)
	- [What This Tutorial Covers](#what-this-tutorial-covers)
	- [Getting Help](#getting-help)
	- [Leiningen Projects](#leiningen-projects)
	- [Creating a Project](#creating-a-project)
		- [Directory Layout](#directory-layout)
		- [Filename-to-Namespace Mapping Convention](#filename-to-namespace-mapping-convention)
	- [project.clj](#projectclj)
	- [Dependencies](#dependencies)
		- [Overview](#overview)
		- [Artifact IDs, Groups, and Versions](#artifact-ids-groups-and-versions)
		- [Snapshot Versions](#snapshot-versions)
		- [Repositories](#repositories)
		- [Checkout Dependencies](#checkout-dependencies)
		- [Search](#search)
	- [Running Code](#running-code)
	- [Tests](#tests)
	- [Profiles](#profiles)
	- [What to do with it](#what-to-do-with-it)
		- [Uberjar](#uberjar)
		- [Framework (Uber)jars](#framework-uberjars)
		- [Server-side Projects](#server-side-projects)
		- [Publishing Libraries](#publishing-libraries)
	- [That's It!](#thats-it!)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Tutorial

Leiningen is for automating Clojure projects without setting your hair on fire.

It offers various project-related tasks and can:

 * create new projects
 * fetch dependencies for your project
 * run tests
 * run a fully-configured REPL
 * compile Java sources (if any)
 * run the project (if the project isn't a library)
 * generate a maven-style "pom" file for the project for interop
 * compile and package projects for deployment
 * publish libraries to repositories such as [Clojars](http://clojars.org)
 * run custom automation tasks written in Clojure (leiningen plug-ins)

If you come from the Java world, Leiningen could be thought of as
"Maven meets Ant without the pain". For Ruby and Python folks,
Leiningen combines RubyGems/Bundler/Rake and pip/Fabric in a single
tool.


## What This Tutorial Covers

This tutorial will briefly cover project structure, dependency
management, running tests, the REPL, and topics related to deployment.

For those of you new to the JVM who have never touched Ant or Maven in
anger: don't panic. Leiningen is designed with you in mind. This
tutorial will help you get started and explain Leiningen's take on
project automation and JVM-land dependency management.


## Getting Help

Also keep in mind that Leiningen ships with fairly comprehensive help;
`lein help` gives a list of tasks while `lein help $TASK` provides
details. Further documentation such as the readme, sample
configuration, and even this tutorial are also provided.


## Leiningen Projects

Leiningen works with *projects*. A project is a directory containing a
group of Clojure (and possibly Java) source files, along with a bit of
metadata about them. The metadata is stored in a file named
`project.clj` in the project's root directory, which is how you tell
Leiningen about things like

 * Project name
 * Project description
 * What libraries the project depends on
 * What Clojure version to use
 * Where to find source files
 * What's the main namespace of the app

and more.

Most Leiningen tasks only make sense in the context of a project. Some
(for example, `repl` or `help`) can also be called from any directory.

Next let's take a look at how projects are created.

## Creating a Project

We'll assume you've got Leiningen installed as per the
[README](https://github.com/technomancy/leiningen/blob/stable/README.md).
Generating a new project is easy:

    $ lein new app my-stuff

    Generating a project called my-stuff based on the 'app' template.

    $ cd my-stuff
    $ find .
    .
    ./.gitignore
    ./doc
    ./doc/intro.md
    ./LICENSE
    ./project.clj
    ./README.md
    ./resources
    ./src
    ./src/my_stuff
    ./src/my_stuff/core.clj
    ./test
    ./test/my_stuff
    ./test/my_stuff/core_test.clj

In this example we're using the `app` template, which is intended for
an application project rather than a library. Omitting the `app`
argument will use the `default` template, which is suitable for
libraries.

### Directory Layout

Here we've got your project's README, a `src/` directory containing the
code, a `test/` directory, and a `project.clj` file which describes your
project to Leiningen. The `src/my_stuff/core.clj` file corresponds to
the `my-stuff.core` namespace.

### Filename-to-Namespace Mapping Convention

Note that we use `my-stuff.core` instead of just `my-stuff` since
single-segment namespaces are discouraged in Clojure. Also note that
namespaces with dashes in the name will have the corresponding file
named with underscores instead since the JVM has trouble loading files
with dashes in the name. The intricacies of namespaces are a common
source of confusion for newcomers, and while they are mostly outside
the scope of this tutorial you can
[read up on them elsewhere](http://blog.8thlight.com/colin-jones/2010/12/05/clojure-libs-and-namespaces-require-use-import-and-ns.html).

## project.clj

Your `project.clj` file will start off looking something like this:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :main ^:skip-aot my-stuff.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
```

If you don't fill in the `:description` with a short sentence, your
project will be harder to find in search results, so start there. Be
sure to fix the `:url` as well. At some point you'll need to flesh out
the `README.md` file too, but for now let's skip ahead to setting
`:dependencies`. Note that Clojure is just another dependency here.
Unlike most languages, it's easy to swap out any version of Clojure.

## Dependencies

### Overview

Clojure is a hosted language and Clojure libraries are distributed the same
way as in other JVM languages: as jar files.

Jar files are basically just `.zip` files with a little extra JVM-specific
metadata. They usually contain `.class` files (JVM bytecode) and `.clj` source
files, but they can also contain other things like config
files, JavaScript files or text files with static data.

Published JVM libraries have *identifiers* (artifact group, artifact id) and
*versions*.

### Artifact IDs, Groups, and Versions

You can [search Clojars](http://clojars.org/search?q=clj-http) using
its web interface or via `lein search $TERM`. On the Clojars page for
`clj-http` at the time of this writing it shows this:

```clj
[clj-http "0.9.1"]
```

It also shows the Maven syntax for dependencies, which we'll skip for
now, though you'll need to learn to read it when looking for Java
libraries from [Central](http://search.maven.org). You can copy the
Leiningen version directly into the `:dependencies` vector in
`project.clj`.  So for instance, if you change the `:dependencies`
line in the example `project.clj` above to

```clj
:dependencies [[org.clojure/clojure "1.5.1"]
               [clj-http "0.9.1"]]
```

Leiningen will automatically download the `clj-http` jar and make sure
it is on your classpath. If you want to explicitly tell lein to
download new dependencies, you can do so with `lein deps`, but it will
happen on-demand if you don't.

Within the vector, "clj-http" is referred to as the "artifact id".
"0.9.1" is the version. Some libraries will also have "group ids",
which are displayed like this:

```clj
[com.cedarsoft.utils.legacy/hibernate "1.3.4"]
```

The group id is the part before the slash. Especially for Java
libraries, it's often a reversed domain name. Clojure libraries often
use the same group-id and artifact-id (as with clj-http), in which case
you can omit the group-id. If there is a library that's part of a
larger group (such as `ring-jetty-adapter` being part of the `ring`
project), the group-id is often the same across all the sub-projects.

### Snapshot Versions

Sometimes versions will end in "-SNAPSHOT". This means that it is not
an official release but a development build. Relying on snapshot
dependencies is discouraged but is sometimes necessary if you need bug
fixes, etc. that have not made their way into a release yet. However,
snapshot versions are not guaranteed to stick around, so it's
important that non-development releases never depend upon snapshot versions that
you don't control. Adding a snapshot dependency to your project will
cause Leiningen to actively go seek out the latest version of the
dependency daily (whereas normal release versions are cached in the local
repository) so if you have a lot of snapshots it will slow things
down.

Note that some libraries make their group-id and artifact-id
correspond with the namespace they provide inside the jar, but this is
just a convention. There is no guarantee they will match up at all, so
consult the library's documentation before writing your `:require`
and `:import` clauses.

### Repositories

Dependencies are stored in *artifact repositories*. If you are
familiar with Perl's CPAN, Python's Cheeseshop (aka PyPi), Ruby's
rubygems.org, or Node.js's NPM, it's the same thing. Leiningen reuses
existing JVM repository infrastructure. There are several popular
open source repositories. Leiningen by default will use two of them:
[clojars.org](http://clojars.org) and
[Maven Central](http://search.maven.org/).

[Clojars](https://clojars.org/) is the Clojure community's centralized
maven repository, while [Central](http://search.maven.org/) is for the
wider JVM community.

You can add third-party repositories by setting the `:repositories` key
in project.clj. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj).

### Checkout Dependencies

Sometimes it is necessary to develop two projects in parallel but it
is very inconvenient to run `lein install` and restart your repl all
the time to get your changes picked up. Leiningen provides a solution
called *checkout dependencies* (or just *checkouts*). To use it,
create a directory called `checkouts` in the project root, like so:

    .
    |-- project.clj
    |-- README.md
    |-- checkouts
    |-- src
    |   `-- my_stuff
    |       `-- core.clj
    `-- test
        `-- my_stuff
            `-- core_test.clj

Then, under the checkouts directory, create symlinks to projects you need.

    .
    |-- project.clj
    |-- README.md
    |-- checkouts
    |   `-- superlib2 [link to ~/code/oss/superlib2]
    |   `-- superlib3 [link to ~/code/megacorp/superlib3]
    |-- src
    |   `-- my_stuff
    |       `-- core.clj
    `-- test
        `-- my_stuff
            `-- core_test.clj

Libraries located under the `checkouts` directory take precedence
over libraries pulled from repositories, but this is not a replacement
for listing the project in your main project's `:dependencies`; it
simply supplements that for convenience. If you have a project in
`checkouts` without putting it in `:dependencies` then its source will
be visible but its dependencies will not be found. If you change the
dependencies of a checkout project you will still have to run `lein
install` and restart your repl; it's just that source changes will be
picked up immediately.

After you've updated `:dependencies`, `lein` will still need to be able
to find the library in some repository like clojars or your `~/.m2`
directory.  If `lein` complains that it could not find the library
artifact, you can install it locally by running `lein install` in the
checkout dependency project directory.

Checkouts are an opt-in feature; not everyone who is working on the
project will have the same set of checkouts, so your project should
work without checkouts before you push or merge.

#### FAQ for checkout dependencies

1. *I created a checkout dependency but I get a message like "Could not find artifact marick:suchwow:jar:2.1.2".*
   
   Most likely, you are working on "suchwow" while also working on the project that uses it, have bumped
   suchwow's version number, but not installed that new version in the local Maven repository. Run `lein install`
   in suchwow's repository. That is: the suchwow version number must be the same in *three* places:
   in suchwow's `project.clj`, in some repository (probably local), and in the project file for suchwow's client.

2. *I use Maven groupids, so my `defproject` looks like `(defproject marick/suchwow "2.2.1" ...)`. How does that affect the checkouts directory?*

   It doesn't. `checkouts/suchwow` should still point to the root of the "suchwow" project, just as if there would be no groupid.
   So the checkouts directory would look like this:

       .
       |-- project.clj
       |-- README.md
       |-- checkouts
       |   `-- suchwow [link to ~/code/oss/suchwow]

### Search

Leiningen supports searching remote Maven repositories for matching
jars with the command `lein search $TERM`. The first time `lein search`
is run, a set of indices are downloaded. Once this is finished, the query
is evaluated as a Lucene search. This allows for simple string matching
or strings prefixed with one of the following operators:

  * `artifact-id`, `artifact\_id`, `id`, `a`
  * `group-id`, `group\_id`, `group`, `g`
  * `description`, `desc`, `d`

These prefixes allow you to execute more advanced queries such as:

    $ lein search clojure
    $ lein search description:crawl
    $ lein search group:clojurewerkz
    $ lein search \"Riak client\"

`lein search` also accepts a second, optional parameter for fetching
successive pages, e.g. `lein search clojure 2`.

## Running Code

Enough setup; let's see some code running. Start with a REPL
(read-eval-print loop):

    $ lein repl
    nREPL server started on port 55568 on host 127.0.0.1 - nrepl://127.0.0.1:55568
    REPL-y 0.3.0
    Clojure 1.5.1
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
        Exit: Control+D or (exit) or (quit)
     Results: Stored in vars *1, *2, *3, an exception in *e

    user=>

The REPL is an interactive prompt where you can enter arbitrary code
to run in the context of your project. Since we've added `clj-http` to
`:dependencies`, we are able to load it here along with code from the
`my-stuff.core` namespace in your project's own `src/` directory:

    user=> (require 'my-stuff.core)
    nil
    user=> (my-stuff.core/-main)
    Hello, World!
    nil
    user=> (require '[clj-http.client :as http])
    nil
    user=> (def response (http/get "http://leiningen.org"))
    #'user/response
    user=> (keys response)
    (:trace-redirects :status :headers :body)

The call to `-main` shows both println output ("Hello, World!") and
the return value (nil) together.

Built-in documentation is available via `doc`, and you can examine the
source of functions with `source`:

    user=> (source my-stuff.core/-main)
    (defn -main
      "I don't do a whole lot."
      [& args]
      (println "Hello, World!"))

    user=> ; use control+d to exit

If you already have code in a `-main` function ready to go and don't
need to enter code interactively, the `run` task is simpler:

    $ lein run
    Hello, World!

Providing a `-m` argument will tell Leiningen to look for
the `-main` function in another namespace. Setting a default `:main` in
`project.clj` lets you omit `-m`.

For long-running `lein run` processes, you may wish to save memory
with the higher-order trampoline task, which allows the Leiningen JVM
process to exit before launching your project's JVM.

    $ lein trampoline run -m my-stuff.server 5000

If you have any Java to be compiled in `:java-source-paths` or Clojure
namespaces listed in `:aot`, they will always be compiled before
Leiningen runs any other code, via any `run`, `repl`,
etc. invocations.

## Tests

We haven't written any tests yet, but we can run the failing tests
included from the project template:

    $ lein test

    lein test my-stuff.core-test

    lein test :only my-stuff.core-test/a-test

    FAIL in (a-test) (core_test.clj:7)
    FIXME, I fail.
    expected: (= 0 1)
      actual: (not (= 0 1))

    Ran 1 tests containing 1 assertions.
    1 failures, 0 errors.
    Tests failed.

Once we fill it in the test suite will become more useful. Sometimes
if you've got a large test suite you'll want to run just one or two
namespaces at a time; `lein test my-stuff.core-test` will do that. You
also might want to break up your tests using test selectors; see `lein
help test` for more details.

Running `lein test` from the command-line is suitable for regression
testing, but the slow startup time of the JVM makes it a poor fit for
testing styles that require tighter feedback loops. In these cases,
either keep a repl open for running the appropriate call to
[clojure.test/run-tests](http://clojuredocs.org/clojure_core/1.3.0/clojure.test/run-tests)
or look into editor integration such as
[clojure-test-mode](https://github.com/technomancy/clojure-mode).

Keep in mind that while keeping a running process around is convenient,
it's easy for that process to get into a state that doesn't reflect
the files on disk—functions that are loaded and then deleted from the
file will remain in memory, making it easy to miss problems arising
from missing functions (often referred to as "getting
slimed"). Because of this it's advised to do a `lein test` run with a
fresh instance periodically in any case, perhaps before you commit.

## Profiles

Profiles are used to add various things into your project map in
different contexts. For instance, during `lein test` runs, the
contents of the `:test` profile, if present, will be merged into your
project map. You can use this to enable configuration that should only
be applied during test runs, either by adding directories containing
config files to your classpath via `:resource-paths` or by other
means. See `lein help profiles` for more details.

Unless you tell it otherwise, Leiningen will merge the default set of
profiles into the project map. This includes user-wide settings from
your `:user` profile, the `:dev` profile from `project.clj` if
present, and the built-in `:base` profile which contains dev tools
like nREPL and optimizations which help startup time at the expense of
runtime performance. Never benchmark with the default profiles. (See
the FAQ entry for "tiered compilation")

## What to do with it

Generally speaking, there are three different goals that are typical
of Leiningen projects:

* An application you can distribute to end-users
* A server-side application
* A library for other Clojure projects to consume

For the first, you typically build an uberjar. For libraries, you will
want to have them published to a repository like Clojars or a private
repository. For server-side applications it varies as described below.
Generating a project with `lein new app myapp` will start you out with
a few extra defaults suitable for non-library projects, or you can
browse the
[available templates on Clojars](https://clojars.org/search?q=lein-template)
for things like specific web technologies or other project types.

### Uberjar

The simplest thing to do is to distribute an uberjar. This is a single
standalone executable jar file most suitable for giving to
nontechnical users. For this to work you'll need to specify a
namespace as your `:main` in `project.clj` and ensure it's also AOT (Ahead Of Time)
compiled by adding it to `:aot`. By this point our `project.clj` file
should look like this:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.apache.lucene/lucene-core "3.0.2"]
                 [clj-http "0.9.1"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.2.0"]]}}
  :test-selectors {:default (complement :integration)
                  :integration :integration
                  :all (fn [_] true)}
  :main my.stuff
  :aot [my.stuff])
```

The namespace you specify will need to contain a `-main` function that
will get called when your standalone jar is run. This namespace should
have a `(:gen-class)` declaration in the `ns` form at the top. The
`-main` function will get passed the command-line arguments. Let's try
something simple in `src/my/stuff.clj`:

```clj
(ns my.stuff
  (:gen-class))

(defn -main [& args]
  (println "Welcome to my project! These are your args:" args))
```

Now we're ready to generate your uberjar:

    $ lein uberjar
    Compiling my.stuff
    Compilation succeeded.
    Created /home/phil/src/leiningen/my-stuff/target/my-stuff-0.1.0-SNAPSHOT.jar
    Including my-stuff-0.1.0-SNAPSHOT.jar
    Including clj-http-0.9.1.jar
    Including clojure-1.3.0.jar
    Including lucene-core-3.0.2.jar
    Created /home/phil/src/leiningen/my-stuff/target/my-stuff-0.1.0-SNAPSHOT-standalone.jar

This creates a single jar file that contains the contents of all your
dependencies. Users can run it with a simple `java` invocation,
or on some systems just by double-clicking the jar file.

    $ java -jar my-stuff-0.1.0-standalone.jar Hello world.
    Welcome to my project! These are your args: (Hello world.)

You can run a regular (non-uber) jar with the `java`
command-line tool, but that requires constructing the classpath
yourself, so it's not a good solution for end-users.

Of course if your users already have Leiningen installed, you can
instruct them to use `lein run` as described above.

### Framework (Uber)jars

Many Java frameworks expect deployment of a jar file or derived archive
sub-format containing a subset of the application's necessary
dependencies.  The framework expects to provide the missing dependencies
itself at run-time.  Dependencies which are provided by a framework in
this fashion may be specified in the `:provided` profile.  Such
dependencies will be available during compilation, testing, etc., but
won't be included by default by the `uberjar` task or plugin tasks
intended to produce stable deployment artifacts.

For example, Hadoop job jars may be just regular (uber)jar files
containing all dependencies except the Hadoop libraries themselves:

```clj
(project example.hadoop "0.1.0"
  ...
  :profiles {:provided
             {:dependencies
              [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]}}
  :main example.hadoop)
```

    $ lein uberjar
    Compiling example.hadoop
    Created /home/xmpl/src/example.hadoop/example.hadoop-0.1.0.jar
    Including example.hadoop-0.1.0.jar
    Including clojure-1.4.0.jar
    Created /home/xmpl/src/example.hadoop/example.hadoop-0.1.0-standalone.jar
    $ hadoop jar example.hadoop-0.1.0-standalone.jar
    12/08/24 08:28:30 INFO util.Util: resolving application jar from found main method on: example.hadoop
    12/08/24 08:28:30 INFO flow.MultiMapReducePlanner: using application jar: /home/xmpl/src/example.hadoop/./example.hadoop-0.1.0-standalone.jar
    ...

Plugins are required to generate framework deployment jar derivatives
(such as WAR files) which include additional metadata, but the
`:provided` profile provides a general mechanism for handling the
framework dependencies.

### Server-side Projects

There are many ways to get your project deployed as a server-side
application. Aside from the obvious uberjar approach, simple
programs can be packaged up as tarballs with accompanied shell scripts
using the [lein-tar plugin](https://github.com/technomancy/lein-tar)
and then deployed using
[pallet](http://hugoduncan.github.com/pallet/),
[chef](http://opscode.com/chef/), or other mechanisms.
Web applications may be deployed as uberjars using embedded Jetty with
`ring-jetty-adapter` or as .war (web application archive) files
created by the
[lein-ring plugin](https://github.com/weavejester/lein-ring). For
things beyond uberjars, server-side deployments are so varied that they
are better-handled using plugins rather than tasks that are built-in
to Leiningen itself.

It's possible to involve Leiningen during production, but there are
many subtle gotchas to that approach; it's strongly recommended to use
an uberjar if you can. If you need to launch with the `run` task, you
should use `lein trampoline run` in order to save memory, otherwise
Leiningen's own JVM will stay up and consume unnecessary memory.

In addition it's very important to ensure you take steps to freeze all
the dependencies before deploying, otherwise it could be easy to end
up with
[unrepeatable deployments](https://github.com/technomancy/leiningen/wiki/Repeatability).
Consider including `~/.m2/repository` in your unit of deployment
(tarball, .deb file, etc) along with your project code. It's
recommended to use Leiningen to create a deployable artifact in a
continuous integration setting. For example, you could have a
[Jenkins](http://jenkins-ci.org) CI server run your project's full
test suite, and if it passes, upload a tarball to S3.  Then deployment
is just a matter of pulling down and extracting the known-good tarball
on your production servers. Simply launching Leiningen from a checkout
on the server will work for the most basic deployments, but as soon as
you get a number of servers you run the risk of running with a
heterogeneous cluster since you're not guaranteed that each machine
will be running with the exact same codebase.

Also remember that the default profiles are included unless you
specify otherwise, which is not suitable for production. Using `lein
trampoline with-profile production run -m myapp.main` is
recommended. By default the production profile is empty, but if your
deployment includes the `~/.m2/repository` directory from the CI run
that generated the tarball, then you should add its path as
`:local-repo` along with `:offline? true` to the `:production`
profile. Staying offline prevents the deployed project from diverging
at all from the version that was tested in the CI environment.

Given these pitfalls, it's best to use an uberjar if possible.

### Publishing Libraries

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to
[maintain your own private repository](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
or get it into [Central](http://search.maven.org), the easiest way is
to publish it at [Clojars](http://clojars.org). Once you have
[created an account](https://clojars.org/register) there, publishing
is easy:

    $ lein deploy clojars
    Created ~/src/my-stuff/target/my-stuff-0.1.0-SNAPSHOT.jar
    Wrote ~/src/my-stuff/pom.xml
    No credentials found for clojars
    See `lein help deploying` for how to configure credentials.
    Username: me
    Password:
    Retrieving my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml (1k)
        from https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20120531.032047-14.jar (5k)
        to https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20120531.032047-14.pom (3k)
        to https://clojars.org/repo/
    Retrieving my-stuff/my-stuff/maven-metadata.xml (1k)
        from https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml (1k)
        to https://clojars.org/repo/
    Sending my-stuff/my-stuff/maven-metadata.xml (1k)
        to https://clojars.org/repo/

Once that succeeds it will be available as a package on which other
projects may depend. For instructions on storing your credentials so
they don't have to be re-entered every time, see `lein help
deploying`. When deploying a release that's not a snapshot, Leiningen
will attempt to sign it using [GPG](http://gnupg.org) to prove your
authorship of the release. See the
[deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md).
for details of how to set that up. The deploy guide includes
instructions for deploying to other repositories as well.

## That's It!

Now go start coding your next project!
