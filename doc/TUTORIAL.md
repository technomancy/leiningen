# Tutorial

For those of you new to the JVM who have never touched Ant or Maven in
anger: don't panic. Leiningen is designed with you in mind. This
tutorial will help you get started and explain Leiningen's take on
project automation and JVM-land dependency management.

Also keep in mind that Leiningen ships with fairly comprehensive help;
`lein help` gives a list of tasks while `lein help task` provides
details. Further documentation such as the readme, sample
configuration, and even this tutorial are also provided.

## Creating a Project

We'll assume you've got Leiningen installed as per the
[README](https://github.com/technomancy/leiningen/blob/preview/README.md).
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
with dashes in the name. The intricacies of namespaces are a common
source of confusion for newcomers, and while they are mostly outside
the scope of this tutorial, you can
[read up on them elsewhere](http://blog.8thlight.com/colin-jones/2010/12/05/clojure-libs-and-namespaces-require-use-import-and-ns.html).

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

If you don't fill in the `:description` with a short sentence, your
project will be harder to find in search results, so start there. Be
sure to fix the `:url` as well. At some point you'll need to flesh out
the README too, but for now let's skip ahead to setting
`:dependencies`. Note that Clojure is just another dependency here.
Unlike most languages, it's easy to swap out any version of Clojure.

## Dependencies

By default, Leiningen projects download dependencies from
[Clojars](http://clojars.org) and [Central](http://search.maven.org).
Clojars is the Clojure community's centralized jar repository, while
Central is for the wider JVM community.

Libraries for the JVM are packaged up as .jar files, which are
basically just .zip files with a little extra JVM-specific metadata.
They usually contain .class files (JVM bytecode) and .clj source
files, but they can also contain other things like config
files.

You can [search Clojars](http://clojars.org/search?q=clj-http) using its
web interface. On the page for `clj-http` it shows this:

    [clj-http "0.4.1"]

There are two different ways of specifying a dependency on the latest
stable version of the `clj-http` library, one in Leiningen format
shown above and one in Maven format. We'll skip the Maven one for now,
though you'll need to learn to read it for Java libraries from
[Central](http://search.maven.org). You can copy the Leiningen version
directly into the `:dependencies` vector in `project.clj`.

Within the vector, "clj-http" is referred to as the "artifact id".
"0.4.1" is the version. Some libraries will also have "group ids",
which are displayed like this:

    [com.cedarsoft.utils.legacy/hibernate "1.3.4"]

The group-id is the part before the slash. Especially for Java
libraries, it's often a reversed domain name. Clojure libraries often
use the same group-id and artifact-id (as with clj-http), in which case
you can omit the group-id. If there is a library that's part of a
larger group (such as `ring-jetty-adapter` being part of the `ring`
project), the group-id is often the same across all the sub-projects.

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

You can add third-party repositories by setting the `:repositories` key
in project.clj. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/preview/sample.project.clj).

## Running Code

Enough setup; let's see some code running. Start with a REPL
(read-eval-print loop):

    $ lein repl
    nREPL server started on port 40612
    Welcome to REPL-y!
    Clojure 1.4.0
        Exit: Control+D or (exit) or (quit)
    Commands: (user/help)
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
              (user/sourcery function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
    Examples from clojuredocs.org: [clojuredocs or cdoc]
              (user/clojuredocs name-here)
              (user/clojuredocs "ns-here" "name-here")

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

Built-in documentation is available via `doc`, while `clojuredocs`
offers more thorough examples from the
[ClojureDocs](http://clojuredocs.org) site:

    user=> (doc reduce)
    -------------------------
    clojure.core/reduce
    ([f coll] [f val coll])
      f should be a function of 2 arguments. If val is not supplied,
      returns the result of applying f to the first 2 items in coll, then
      applying f to that result and the 3rd item, etc. If coll contains no
      items, f must accept no arguments as well, and reduce returns the
      result of calling f with no arguments.  If coll has only 1 item, it
      is returned and f is not called.  If val is supplied, returns the
      result of applying f to val and the first item in coll, then
      applying f to that result and the 2nd item, etc. If coll contains no
      items, returns val and f is not called.

    user=> (user/clojuredocs pprint)
    Loading clojuredocs-client...
    ========== vvv Examples ================
      user=> (def *map* (zipmap
                          [:a :b :c :d :e]
                          (repeat
                            (zipmap [:a :b :c :d :e]
                              (take 5 (range))))))
      #'user/*map*
      user=> *map*
      {:e {:e 4, :d 3, :c 2, :b 1, :a 0}, :d {:e 4, :d 3, :c 2, :b 1, :a 0}, :c {:e 4, :d 3, :c 2, :b 1, :a 0}, :b {:e 4, :d 3, :c 2, :b 1, :a 0}, :a {:e 4, :d 3, :c 2, :b 1, :a 0}}

      user=> (clojure.pprint/pprint *map*)
      {:e {:e 4, :d 3, :c 2, :b 1, :a 0},
       :d {:e 4, :d 3, :c 2, :b 1, :a 0},
       :c {:e 4, :d 3, :c 2, :b 1, :a 0},
       :b {:e 4, :d 3, :c 2, :b 1, :a 0},
       :a {:e 4, :d 3, :c 2, :b 1, :a 0}}
      nil
    ========== ^^^ Examples ================
    1 example found for clojure.pprint/pprint

You can even examine the source of functions:

    user=> (source my-stuff.core/-main)
    (defn -main
      "I don't do a whole lot."
      [& args]
      (println "Hello, World!"))

    user=> ; use control+d to exit

If you already have code in a `-main` function ready to go and don't
need to enter code interactively, the `run` task is simpler:

    $ lein run -m my-stuff.core
    Hello, World!

Providing an alternate `-m` argument will tell Leiningen to look for
the `-main` function in another namespace. Setting a default `:main` in
`project.clj` lets you omit `-m`.

For long-running `lein run` processes, you may wish to save memory
with the trampoline higher-order task, which allows the Leiningen JVM
process to exit before launching your project's JVM.

    $ lein trampoline run -m my-stuff.server 5000

## Tests

It's easy to kick off a test run:

    $ lein test

    lein test my.test.stuff

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
namespaces at a time; `lein test my.test.stuff` will do that.. You
also might want to break up your tests using test selectors; see `lein
help test` for more details.

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

## What to do with it

Generally speaking, there are three different goals that are typical
of Leiningen projects:

* An application you can distribute to end-users
* A server-side application
* A library for other Clojure projects to consume

For the first, you typically build an uberjar. For libraries,
you will want to have them published to a repository like Clojars or a
private repository. For server-side applications it varies as
described below. Generating a project with `lein new app myapp` will
start you out with a few extra defaults suitable for non-library projects.

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
                 [clj-http "0.4.1"]]
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
    Including clj-http-0.4.1.jar
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

### Server-side Projects

There are many ways to get your project deployed as a server-side
application. Aside from the obvious uberjar approach, simple
programs can be packaged up as tarballs with accompanied shell scripts
using the [lein-tar plugin](https://github.com/technomancy/lein-tar)
and then deployed using
[pallet](http://hugoduncan.github.com/pallet/),
[chef](http://opscode.com/chef/), or other mechanisms. Debian packages
can be created with [lein-deb](https://github.com/travis/lein-deb).
Web applications may be deployed as uberjars using embedded Jetty with
`ring-jetty-adapter` or as .war (web application archive) files
created by the
[lein-ring plugin](https://github.com/weavejester/lein-ring). For
things beyond uberjars, server-side deployments are so varied that they
are better-handled using plugins rather than tasks that are built-in
to Leiningen itself.

If you do end up involving Leiningen in production via something like
`lein trampoline run`, it's very important to ensure you take steps to
freeze all the dependencies before deploying, otherwise it could be
easy to end up with
[unrepeatable deployments](https://github.com/technomancy/leiningen/wiki/Repeatability).
Consider including `~/.m2/repository` in your unit of deployment along
with your project code. It's recommended to use Leiningen to create a
deployable artifact in a continuous integration setting. For example,
you could have a [Jenkins](http://jenkins-ci.org) CI server run your
project's full test suite, and if it passes, upload a tarball to S3.
Then deployment is just a matter of pulling down and extracting the
known-good tarball on your production servers. Over time this could
cause unused dependencies to accumulate in the local repo, bloating up
deploy artifact size, which the
[lein-clean-m2](https://github.com/technomancy/lein-clean-m2) plugin
can help with.

Also remember that the `run` task defaults to including the `user`, `dev`,
and `default` profiles, which are not suitable for production. Using
`lein trampoline with-profile production run -m myapp.main` is
recommended.

### Publishing Libraries

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to
[maintain your own private repository](https://github.com/technomancy/leiningen/blob/preview/doc/DEPLOY.md)
or get it into Central, the easiest way is to publish it at
[Clojars](http://clojars.org). Once you have
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
they don't have to be re-entered every time, see `lein help deploying`.
For further details about publishing including setting up private
repositories, see the
[deploy guide](https://github.com/technomancy/leiningen/blob/preview/doc/DEPLOY.md).

## That's It!

Now go start coding your next project!
