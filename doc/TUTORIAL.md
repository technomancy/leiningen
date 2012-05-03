# Tutorial

For those of you new to the JVM who have never touched Ant or Maven in
anger: don't panic. Leiningen is designed with you in mind. This
tutorial will help you get started and explain Leiningen's take on
project automation and JVM-land dependency management.

## Creating a Project

We'll assume you've got Leiningen installed as per the
[README](https://github.com/technomancy/leiningen/blob/stable/README.md). 
Generating a new project is easy:

    $ lein new my-stuff

    Generating a project called my-stuff based on the 'default' template.

    $ cd my-stuff
    $ tree
    .
    |-- project.clj
    |-- README.md
    |-- src
    |   `-- my_stuff
    |       `-- core.clj
    `-- test
        `-- my_stuff
            `-- core_test.clj

Here we've got your project's README, a `src/` directory containing the
code, a `test/` directory, and a `project.clj` file which describes your
project to Leiningen. The `src/my_stuff/core.clj` file corresponds to
the `my-stuff.core` namespace.

Note that we use `my-stuff.core` instead of just `my-stuff` since
single-segment namespaces are discouraged in Clojure. Also note that
namespaces with dashes in the name will have the corresponding file
named with underscores instead since the JVM has trouble loading files
with dashes in the name.

## Packaging

You can package your project up now, even though at this stage it's
fairly useless:

    $ lein jar

    Created ~/src/my-stuff/target/my-stuff-0.1.0-SNAPSHOT.jar

Libraries for the JVM are packaged up as .jar files, which are
basically just .zip files with a little extra JVM-specific metadata.
They usually contain .class files (JVM bytecode) and .clj source
files, but they can also contain other things like config
files. Leiningen downloads jar files of dependencies from remote Maven
repositories for you.

## project.clj

    $ cat project.clj

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]])
```

Fill in the `:description` with a short sentence so that your project
will show up in search results once you publish it, and be sure to fix
the `:url` as well. At some point you'll need to flesh out the README
too, but for now let's skip ahead to setting `:dependencies`. Note
that Clojure is just another dependency here. Unlike most languages,
it's easy to swap out any version of Clojure.

## Dependencies

By default, Leiningen projects download dependencies from
[Clojars](http://clojars.org) and
[Maven Central](http://search.maven.org). Clojars is the Clojure
community's centralized jar repository, while Maven Central is for the
wider JVM community.

The `lein search` command will search each remote repository:

    $ lein search lancet
     == Results from clojars - Showing page 1 / 1 total
    [lancet "1.0.0"] Dependency-based builds, Clojure Style.
    [lancet "1.0.1"] Dependency-based builds, Clojure Style.

Note that this command will take many minutes to run the first time
you invoke it on a given machine; it needs to download a rather large
index.

This shows two versions available with the dependency vector notation
for each. You can copy one of these directly into the `:dependencies`
vector in `project.clj`.

Within the vector, "lancet" is what Maven calls the "artifact
id". "1.0.0" and "1.0.1" are distinct versions. Some
libraries will also have "group ids", which are displayed
like this:

    [com.cedarsoft.utils.legacy/hibernate "1.3.4"]

The group-id is the part before the slash. Especially for Java
libraries, it's often a reversed domain name. Clojure libraries often
use the same group-id and artifact-id (as with Lancet), in which case
you can omit the group-id. If there is a library that's part of a
larger group (such as `ring-jetty-adapter` being part of the `ring`
project), the group-id is often the same across all the sub-projects.

Sometimes versions will end in "-SNAPSHOT". This means that it is not
an official release but a development build. Relying on snapshot
dependencies is discouraged but is sometimes necessary if you need bug
fixes, etc. that have not made their way into a release yet. However,
snapshot versions are not guaranteed to stick around, so it's
important that released code never depends upon snapshot versions that
you don't control. Adding a snapshot dependency to your project will
cause Leiningen to actively go seek out the latest version of the
dependency (whereas normal release versions are cached in the local
repository) so if you have a lot of snapshots it will slow things
down.

Speaking of the local repository, all the dependencies you pull in
using Leiningen or Maven get cached in `$HOME/.m2/repository` since
Leiningen uses the same library as Maven under the covers. You can
install the current project in the local repository with this command:

    $ lein install

    Wrote ~/src/my-stuff/target/pom.xml
    [INFO] Installing my-stuff-0.1.0-SNAPSHOT.jar to ~/.m2/repository/myproject/myproject/0.1.0-SNAPSHOT/myproject-0.1.0-SNAPSHOT.jar

Note that some libraries make their group-id and artifact-id
correspond with the namespace they provide inside the jar, but this is
just a convention. There is no guarantee they will match up at all, so
consult the library's documentation before writing your `:require`
clauses.

You can add third-party repositories by setting the `:repositories` key
in project.clj. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj).

## Profiles

Sometimes you want to pull in dependencies that are really only
necessary while developing; they aren't required for the project to
function in production. You can do this by adding a `:dependencies`
entry to the `:dev` profile. These will be available unless you
specify different profiles using the `with-profiles` task, but they
are not brought along when another project depends on your project.

Using [midje](https://github.com/marick/Midje) for your tests would be
a typical example; you would not want it included in production, but it's
needed to run the tests:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :profiles {:dev {:dependencies [[midje "1.3.1"]]}})
```

Note that profile-specific dependencies are different from plugins in
context; plugins run in Leiningen's process while dependencies run in
your project itself. (Older versions of Leiningen lacked this distinction.)

If you have dependencies that are not _necessary_ for developing but
just for convenience (things like
[Swank Clojure](http://github.com/technomancy/swank-clojure) for Emacs
support or [clj-stacktrace](http://github.com/mmcgrana/clj-stacktrace)
you should add them to the `:user` profile in `~/.lein/profiles`
instead of the `:dev` profile. Both those profiles are active by
default; the difference is the convention for where they are specified.

## Writing the Code

This is the part Leiningen can't really help you with; you're on your
own here. Well—not quite. Leiningen can help you with running your
tests.

    $ lein test

    Testing my.test.stuff

    FAIL in (a-test) (stuff.clj:7)
    FIXME, I fail.
    expected: (= 0 1)
      actual: (not (= 0 1))

    Ran 1 tests containing 1 assertions.
    1 failures, 0 errors.

Of course, we haven't written any tests yet, so we've just got the
skeleton failing tests that Leiningen gave us with `lein new`. But
once we fill it in the test suite will become more useful. Sometimes
if you've got a large test suite you'll want to run just one or two
namespaces at a time:

    $ lein test my.test.stuff.parser

    Testing my.test.stuff.parser
    Ran 2 tests containing 10 assertions.
    0 failures, 0 errors.

Rather than running your whole suite or just a few namespaces at a
time, you can run a subset of your tests using test selectors. To do
this, you attach metadata to various deftests.

```clj
(deftest ^:integration network-heavy-test
  (is (= [1 2 3] (:numbers (network-operation)))))
```

Then add a `:test-selectors` map to project.clj:

```clj
:test-selectors {:default (complement :integration)
                 :integration :integration
                 :all (fn [_] true)}
```

Now if you run `lein test` it will only run deftests that don't have
`:integration` metadata, while `lein test :integration` will only run
the integration tests and `lein test :all` will run everything. You
can include test selectors and listing test namespaces in the same
run.

Running `lein test` from the command-line is suitable for regression
testing, but the slow startup time of the JVM makes it a poor fit for
testing styles that require tighter feedback loops. In these cases,
either keep a repl open for running the appropriate call to
[clojure.test/run-tests](http://clojuredocs.org/clojure_core/1.3.0/clojure.test/run-tests)
or look into editor integration such as
[clojure-test-mode](https://github.com/technomancy/clojure-mode).

Keep in mind that while keeping a single process around is convenient,
it's easy for that process to get into a state that doesn't reflect
the files on disk—functions that are loaded and then deleted from the
file will remain in memory, making it easy to miss problems arising
from missing functions (often referred to as "getting
slimed"). Because of this it's advised to do a `lein test` run with a
fresh instance periodically in any case, perhaps before you commit.

## AOT Compiling

If you're lucky you'll be able to get away without doing any AOT
(ahead-of-time) compilation. But there are some Java interop features
that require it, so if you need to use them you should add an `:aot`
option into your `project.clj` file. It should be a seq of namespaces
you want AOT-compiled. Again, the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
has example usage.

Like dependencies, this should happen for you automatically when
needed, but if you need to force it you can:

    $ lein compile

    Compiling my.stuff

For your code to compile, it must be run. This means that you
shouldn't have any code with side-effects in the top-level. Anything
outside a function definition that doesn't start with "def" is
suspect. If you have code that should run on startup, place it in a
`-main` function as explained below under "Uberjar".

<!-- TODO: this hasn't been ported to 2.x yet
There's
[a problem in Clojure](http://dev.clojure.org/jira/browse/CLJ-322)
where AOT-compiling a namespace will also AOT compile all the
namespaces it depends upon. This often causes unrelated compilation
artifacts to be included in the jars, but you can set
`:class-file-whitelist` to a regex which will be matched against
.class file names you want to keep in order to remove the unwanted
file.
-->

For projects that include some Java code, you can set the
`:java-source-paths` key in project.clj to a vector of directories
containing Java files. (You can set it to ["src"] to keep Java
alongside Clojure source or keep them in a separate directory.) Then
the `javac` compiler will run before your Clojure code is AOT-compiled,
or you can run it manually with the `javac` task.

## What to do with it

Generally speaking, there are three different goals that are typical
of Leiningen projects:

* An application you can distribute to end-users
* A server-side application
* A library for other Clojure projects to consume

For the first, you typically either build an uberjar. For libraries,
you will want to have them published to a repository like Clojars or a
private repository. For server-side applications it varies as
described below.

### Uberjar

The simplest thing to do is to distribute an uberjar. This is a single
standalone executable jar file most suitable for giving to
nontechnical users. For this to work you'll need to specify a
namespace as your `:main` in `project.clj`. By this point our
`project.clj` file should look like this:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.apache.lucene/lucene-core "3.0.2"]
                 [lancet "1.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.3.1"]]}}
  :test-selectors {:default (complement :integration)
                  :integration :integration
                  :all (fn [_] true)}
  :main my.stuff)
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
    Including lancet-1.0.0.jar
    Including clojure-1.2.0.jar
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

Invoking `lein run` will launch your project's `-main` function as if
from an uberjar, but without going through the packaging process. You
can also specify an alternate namespace in which to look for `-main`
with `lein run -m my.alternate.namespace ARG1 ARG2`.

For long-running `lein run` processes, you may wish to use the
trampoline task, which allows the Leiningen JVM process to exit before
launching your project's JVM. This can save memory:

    $ lein trampoline run -m my-stuff.server 5000

<!-- TODO: not ported to 2.x yet
### Shell Wrappers

There are a few downsides to uberjars. It's relatively awkward to
invoke them compared to other command-line tools. You also can't
control how the JVM is launched. To solve this, you can include a
shell script in your jar file that can be used to launch the
project. Leiningen places this shell script into the
`~/.lein/bin` directory at install time. Of course, this is
only suitable if your users already use Leiningen.

If you simply include `:shell-wrapper true` in your
project.clj, Leiningen automatically generates a simple shell script
wrapper when you create your jar file. However, if you need more
control you can provide a map instead:

```clj
    :shell-wrapper {:main my-stuff.core
                    :bin "bin/my-stuff"}
```

Normally the shell wrapper will invoke the -main function in your
project's :main namespace, but specifying this option triggers AOT for
uberjars, so if you wish to avoid this or use a different :main for
the shell wrapper vs uberjar you can specify a :main ns inside the
:shell-wrapper map. You may also specify a :bin key, which should
point to a file relative to the project's root to use as a shell
wrapper template instead of the default. The `format` function
is called with the contents of this file along with the necessary
classpath and the main namespace, so put %s in the right place. See
[the default
wrapper](https://github.com/technomancy/leiningen/blob/stable/resources/script-template)
for an example.
-->

### Server-side Projects

There are many ways to get your project deployed as a server-side
application. Simple programs can be packaged up as tarballs with
accompanied shell scripts using the
[lein-tar plugin](https://github.com/technomancy/lein-tar) and then
deployed using [pallet](http://hugoduncan.github.com/pallet/),
[chef](http://opscode.com/chef/), or other mechanisms. Debian packages
can be created with
[lein-deb](https://github.com/travis/lein-deb). Web applications may
be deployed using .war (web application archive) files created by the
[lein-ring plugin](https://github.com/weavejester/lein-ring). You
can even create
[Hadoop projects](https://github.com/ndimiduk/lein-hadoop). These
kinds of deployments are so varied that they are better-handled using
plugins rather than tasks that are built-in to Leiningen itself.

It may be tempting to deploy by just checking out your project and
using "lein run" on production servers. However, unless you take steps
to freeze all the dependencies before deploying, it could be easy to
end up with
[unrepeatable deployments](https://github.com/technomancy/leiningen/wiki/Repeatability).
It's much better to use Leiningen to create a deployable artifact in a
continuous integration setting instead. For example, you could have a
[Jenkins](http://jenkins-ci.org) CI server run your project's full
test suite, and if it passes, upload a tarball to S3. Then deployment
is just a matter of pulling down and extracting the known-good tarball
on your production servers.

### Publishing Libraries

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to
[maintain your own private repository](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
or get it into Maven central, the easiest way is to publish it at
[Clojars](http://clojars.org). Once you have created an account there,
publishing is easy:

    $ lein jar, pom
    $ scp target/pom.xml target/my-stuff-0.1.0.jar clojars@clojars.org:

Once that succeeds it will be available as a package on which other
projects may depend. You will need to have permission to publish to
the project's group-id under Clojars, though if that group-id doesn't
exist yet then Clojars will automatically create it and give you
permissions.

For further details about publishing including setting up private
repositories, see the [deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)

## That's It!

Now go start coding your next project!
