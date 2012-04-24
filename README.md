# Leiningen

<img src="https://github.com/downloads/technomancy/leiningen/leiningen-banner.png"
 alt="Leiningen logo" title="The man himself" align="right" />

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight--they're an elemental--an 'act of God!' Ten miles long, two
> miles wide--ants, nothing but ants! And every single one of them a
> fiend from hell..."
> -- from Leiningen Versus the Ants by Carl Stephenson

Leiningen is for automating Clojure projects without setting your hair on fire.

## Installation

If your preferred
[package manager](https://github.com/technomancy/leiningen/wiki/Packaging)
has a relatively recent version of Leiningen, try that first.
Otherwise you can install by hand:

Leiningen bootstraps itself using the `lein` shell script;
there is no separate install script. It installs its dependencies
upon the first run on unix, so the first run will take longer.

1. [Download the script](https://raw.github.com/technomancy/leiningen/stable/bin/lein).
2. Place it on your `$PATH`. (I like to use `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/lein`)

The instructions above will install the stable release. The Leiningen 2
[preview release](https://raw.github.com/technomancy/leiningen/preview/bin/lein)
has some great new features, but not all projects and plugins have
been upgraded to work with it yet. Please see the
[upgrade guide](https://github.com/technomancy/leiningen/wiki/Upgrading)
for instructions on migrating to version 2.

On Windows most users can get
[the batch file](https://raw.github.com/technomancy/leiningen/stable/bin/lein.bat).
If you have wget.exe or curl.exe already installed and in PATH, you
can just run `lein self-install`, otherwise get the standalone jar from the
[downloads page](https://github.com/technomancy/leiningen/downloads).
If you have [Cygwin](http://www.cygwin.com/) you should be able to use
the shell script above rather than the batch file.

## Usage

The
[tutorial](https://github.com/technomancy/leiningen/blob/preview/doc/TUTORIAL.md)
has a detailed walk-through of the steps involved in creating a new
project, but here are the commonly-used tasks:

    $ lein new [TEMPLATE] NAME # generate a new project skeleton

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch an interactive REPL session

    $ lein jar # package up the whole project as a .jar file

    $ lein install # install a project

    $ lein search [TERM] # find jars for your project.clj dependencies

Use `lein help` to see a complete list. `lein help $TASK` shows the
usage for a specific task.

You can also chain tasks together in a single command by using commas:

    $ lein clean, test foo.test-core, jar

Most tasks need to be run from somewhere inside a project directory to
work, but some (`new`, `help`, `search`, `version`, and `repl`) may
run from anywhere.

## Configuration

The `project.clj` file in the project root should look like this:

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :url "http://github.com/technomancy/myproject"
  :dependencies [[org.clojure/clojure "1.2.1"]]
  :plugins [[lein-ring "0.4.5"]])
```

To find specific versions of a dependency, use `lein search`.

The `lein new` task generates a project skeleton with an
appropriate starting point from which you can work. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/preview/sample.project.clj)
file for a detailed listing of configuration options.

You can also have user-level configuration that applies for all
projects. The `~/.lein/init.clj` file will be loaded every time
Leiningen launches; any arbitrary code may go there. This code is
executed inside Leiningen itself, not in your project. Set the
`:init-ns` key in `:repl-options` in project.clj to point to a
namespace if you want code executed inside your project in the repl.

### Profiles

In Leiningen 2.x you can change the configuration of your project by
applying various profiles. For instance, you may want to have a few
extra test data directories on the classpath during development
without including them in the jar, or you may want to have Swank
Clojure available in every project you hack on without modifying every
single project.clj you use.

By default the `:dev`, `:user`, and `:default` profiles are activated
for each task. Each profile is defined as a map which gets merged into
your project map. To add resources directories during development, add
a `:profiles` key to project.clj like so:

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :dependencies [[org.clojure/clojure "1.2.1"]]
  :profiles {:dev {:resources-path ["dummy-data"]}})
```

You can place any arbitrary defproject entries into a given profile
and they will be merged into the project map when that profile is
active. In addition to `project.clj`, profiles specified in
`~/.lein/profiles.clj` will be available in all projects, though those
from `profiles.clj` will be overridden by profiles of the same name in
the `project.clj` file. This is why the `:user` profile is separate
from `:dev`; the latter is intended to be specified in the project
itself. In order to avoid collisions, the project should never define
a `:user` profile, nor should `profiles.clj` define a `:dev` profile.
If you want to access dependencies during development time for any
project place them in your `:user` profile.

```clj
{:user {:plugins [[lein-swank "1.4.0"]
                  [lein-pprint "1.1.1"]]}}
```
Another use of profiles is to test against various sets of dependencies:

```clj
(defproject swank-clojure "1.5.0-SNAPSHOT"
  :description "Swank server connecting Clojure to Emacs SLIME"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [clj-stacktrace "0.2.4"]
                 [cdt "1.2.6.2"]]
  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0-beta1"]]}})
```

To activate other profiles for a given run, use the `with-profile`
higher-order task:

    $ lein with-profile 1.3 test :database

Multiple profiles may be combined with commas:

    $ lein with-profile qa,user test :database

Multiple profiles may be executed in series with colons:

    $ lein with-profile 1.3:1.4 test :database

A single `with-profile` call does not apply across task comma-chains.

To see how a given profile affects your project map, use the
[lein-pprint](https://github.com/technomancy/leiningen/tree/master/lein-pprint)
plugin:

    $ lein with-profile 1.4 pprint
    {:compile-path "/home/phil/src/leiningen/lein-pprint/classes",
     :group "lein-pprint",
     :source-path ("/home/phil/src/leiningen/lein-pprint/src"),
     :dependencies
     ([org.clojure/tools.nrepl "0.0.5" :exclusions [org.clojure/clojure]]
      [clojure-complete "0.1.4" :exclusions [org.clojure/clojure]]
      [org.thnetos/cd-client "0.3.3" :exclusions [org.clojure/clojure]]),
     :target-path "/home/phil/src/leiningen/lein-pprint/target",
     :name "lein-pprint",
     [...]
     :description "Pretty-print a representation of the project map."}

### Leiningen Plugins 

Leiningen supports plugins which may contain both new tasks and hooks
that modify behaivour of existing tasks. See
[the plugins wiki page](https://github.com/technomancy/leiningen/wiki/Plugins)
for a full list. If a plugin is needed for successful test or build
runs, (such as `lein-tar`) then it should be added to `:plugins` in
project.clj, but if it's for your own convenience (such as
`swank-clojure`) then it should be added to the `:plugins` list in the
`:user` profile from `~/.lein/profiles.clj`. The
[plugin guide](https://github.com/technomancy/leiningen/blob/preview/doc/PLUGINS.md)
explains how to write plugins.

## FAQ

**Q:** How do you pronounce Leiningen?  
**A:** It's LINE-ing-en. ['laɪnɪŋən]

**Q:** What's a group ID? How do snapshots work?  
**A:** See the
  [tutorial](https://github.com/technomancy/leiningen/blob/preview/doc/TUTORIAL.md)
  for background.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** The [deploy guide](https://github.com/technomancy/leiningen/blob/preview/doc/DEPLOY.md)
  explains how to set up a private repository. If you are not sharing
  them with a team you could also just
  [install locally](https://github.com/kumarshantanu/lein-localrepo).
  In general it's easiest to deploy them to a private S3 bucket with
  the [s3-wagon-private](https://github.com/technomancy/s3-wagon-private) plugin.

**Q:** I want to hack two projects in parallel, but it's annoying to switch between them.  
**A:** If you create a directory called `checkouts` in your project
  root and symlink some other project roots into it, Leiningen will
  allow you to hack on them in parallel. That means changes in the
  dependency will be visible in the main project without having to go
  through the whole install/switch-projects/deps/restart-repl cycle,
  and the copy in `checkouts` will take precedence over the dependency
  declared in project.clj. Note that this is not a replacement for
  listing the project in `:dependencies`; it simply supplements that for
  convenience.

**Q:** Is it possible to exclude indirect dependencies?  
**A:** Yes. Some libraries, such as log4j, depend on projects that are
  not included in public repositories and unnecessary for basic
  functionality.  Projects listed as `:dependencies` may exclude 
  any of their dependencies by using the `:exclusions` key. See
  `lein help sample` for details.

**Q:** What does `java.lang.NoSuchMethodError: clojure.lang.RestFn.<init>(I)V` mean?  
**A:** It means you have some code that was AOT (ahead-of-time)
  compiled with a different version of Clojure than the one you're
  currently using. If it persists after running `lein clean` then it
  is a problem with your dependencies. Note that for
  your own project that AOT compilation in Clojure is much less
  important than it is in other languages. There are a few
  language-level features that must be AOT-compiled to work, generally
  for Java interop. If you are not using any of these features, you
  should not AOT-compile your project if other projects may depend
  upon it.

**Q:** I'm behind an HTTP proxy; how can I fetch my dependencies?  
**A:** Set the `$http_proxy` environment variable in Leiningen 2.x.
  For Leiningen 1.x versions, see the instructions for
  [configuring a Maven proxy](http://maven.apache.org/guides/mini/guide-proxies.html)
  using `~/.m2/settings.xml`.

**Q:** What can be done to speed up launch?  
**A:** The main delay involved in Leiningen comes from starting the
  JVM. Most people use a development cycle that involves keeping a
  single process running for as long as you're working on that
  project. Depending on your editor you may be able to do this via its
  Clojure integration. (See
  [swank-clojure](http://github.com/technomancy/swank-clojure) or
  [VimClojure](https://bitbucket.org/kotarak/vimclojure), for
  example.) Otherwise you can use the basic `lein repl`.

**Q:** Still too slow; what else can make startup faster?  
**A:** If you are running an older version of Leiningen (before 1.7)
  you can `export LEIN_JVM_OPTS=-XX:+TieredCompilation` to improve
  boot time. This requires Hotspot version 20 or newer. On newer versions
  of Leiningen it is enabled automatically.

**Q:** Why is Leiningen 2 still in a preview release?  
**A:** As of the preview3 release, Leiningen 2 is very stable and
  recommended for general use. The main thing keeping it from a final
  release is the fact that the Clojars repository
  [mingles snapshots with releases](https://github.com/ato/clojars-web/issues/24),
  which is undesirable. Since switching the default repositories to a
  releases-only Clojars (which hasn't been implemented yet) would be a
  breaking change, a series of previews is being released in the mean time.

**Q:** I don't have access to stdin inside my project.  
**A:** This is a limitation of the JVM's process-handling methods;
  none of them expose stdin correctly. This means that functions like
  `read-line` will not work as expected in most contexts, though the
  `repl` task necessarily includes a workaround. You can also use the
  `trampoline` task to launch your project's JVM after Leiningen's has
  exited rather than launching it as a subprocess.

## Contributing

Please report issues on the
[Github issue tracker](https://github.com/technomancy/leiningen/issues)
or the [mailing list](http://groups.google.com/group/leiningen).
Personal email addresses are **not** appropriate for bug reports. See
the readme for the `leiningen-core` library and `doc/PLUGINS.md` for
more details on how Leiningen's codebase is structured. Design
discussions also occur in the
[#leiningen channel on Freenode](irc://chat.freenode.net#leiningen).

Patches are preferred as Github pull requests, though patches from
`git format-patch` are also welcome on the mailing list. Please use
topic branches when sending pull requests rather than committing
directly to master in order to minimize unnecessary merge commit
clutter.

Contributors who have had a single patch accepted may request commit
rights on the mailing list or in IRC. Please use your judgment
regarding potentially-destabilizing work and branches. Other
contributors will usually be glad to review topic branches before
merging if you ask on IRC or the mailing list.

Contributors are also welcome to request a free
[Leiningen sticker](http://twitpic.com/2e33r1) by asking on the
mailing list and mailing a SASE.

## Building

You don't need to "build" Leiningen per se, but when you're using a
checkout you will need to get its dependencies in place.

For the master branch, use Leiningen 1.x to run `lein install` in the
`leiningen-core` subproject directory. When the dependencies change
you will also have to do `rm .lein-classpath` in the project root.

Once you've done that, symlink `bin/lein` to somewhere on your
`$PATH`, usually as `lein2` in order to keep it distinct from your
existing installation.

If you want to develop on the 1.x branch, this should be unnecessary.

## License

Source Copyright © 2009-2012 Phil Hagelberg, Alex Osborne, Dan Larkin, and
[contributors](https://github.com/technomancy/leiningen/contributors). 
Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Images Copyright © 2010 Phil Hagelberg. Distributed under the Creative
Commons Attribution + ShareAlike
License. [Full-size version](https://github.com/downloads/technomancy/leiningen/leiningen-full.jpg)
available.
