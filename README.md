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

Leiningen bootstraps itself using the `lein` shell script;
there is no separate install script. It installs its dependencies
upon the first run on unix, so the first run will take longer.

1. [Download the script](https://raw.github.com/technomancy/leiningen/stable/bin/lein).
2. Place it on your path. (I like to use `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/lein`)

On Windows most users can get
[the batch file](https://raw.github.com/technomancy/leiningen/stable/bin/lein.bat).
If you have wget.exe or curl.exe already installed and in PATH, you
can just run `lein self-install`, otherwise get the standalone jar from the
[downloads page](https://github.com/technomancy/leiningen/downloads).
If you have [Cygwin](http://www.cygwin.com/) you should be able to use
the shell script above rather than the batch file.

The `master` branch is currently undergoing massive changes for
Leiningen 2.0; you should not expect it to work. If you want to build
from source for everyday use, use the `1.x` branch.

## Usage

The
[tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
has a detailed walk-through of the steps involved in creating a new
project, but here are the commonly-used tasks:

    $ lein new NAME # generate a new project skeleton

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch an interactive REPL session and socket server

    $ lein jar # package up the whole project as a .jar file

    $ lein install [NAME VERSION] # install a project

    $ lein search ... # find jars for your project.clj dependencies

Use `lein help` to see a complete list. `lein help $TASK` shows the
usage for a specific task.

You can also chain tasks together in a single command by using commas:

    $ lein clean, test foo.test-core, jar

Most tasks need to be run from somewhere inside a project directory to
work, but some (`new`, `help`, `version`, and the
two-argument version of `install`) may run from anywhere.

The install task places shell scripts in the `~/.lein/bin`
directory for projects that include them, so if you want to take
advantage of this, you should put it on your `$PATH`.

## Configuration

The `project.clj` file in the project root should look like this:

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :url "http://github.com/technomancy/myproject"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :plugins [[lein-ring "0.4.5"]])
```

To find specific versions of a dependency, use `lein search`.

The `lein new` task generates a project skeleton with an
appropriate starting point from which you can work. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
file for a detailed listing of configuration options.

You can also have user-level configuration that applies for all
projects. The `~/.lein/init.clj` file will be loaded every time
Leiningen launches; any arbitrary code may go there. This code is
executed inside Leiningen itself, not in your project. Set the
`:repl-init` key in project.clj to point to a namespace if
you want code executed inside your project.

### Profiles

You can change the configuration of your project by applying various
profiles. Each profile is defined as a map which gets merged into your
project map.

Profiles are read from 3 different locations: (in order of precedence)

* the `:profiles` entry in the project map
* the `~/.lein/profiles.clj` file
* the `leiningen.core.project/default-profiles` atom

Each of these should be a map of profile names to profile maps.

Note that profiles have special logic when they are merged into your
project map: maps get merged recursively, but sets are `union`ed and
other collections are `concat`enated. Other values are simply
replaced. Profiles take precedence in the order they are specified.

To activate a profile for a given run, use the `with-profile`
higher-order task:

    $ lein with-profile qa test :database

A single `with-profile` call does not apply across task comma-chains.
Outside `with-profile` calls, the `:dev` and `:user` profiles are
active by default.

### Leiningen Plugins 

Leiningen supports plugins which may contain both new tasks and hooks
that modify behaivour of existing tasks. See
[the plugins wiki page](https://github.com/technomancy/leiningen/wiki/Plugins)
for a full list. If a plugin is needed for successful test or build
runs, (such as `lein-tar`) then it should be added to `:plugins` in
project.clj, but if it's for your own convenience (such as
swank-clojure) then it should be added to the `:plugins` list in the
`:user` profile from `~/.lein/profiles.clj`. The
[plugin guide](https://github.com/technomancy/leiningen/blob/stable/doc/PLUGINS.md)
explains how to write plugins.

## FAQ

**Q:** How do you pronounce Leiningen?  
**A:** It's LINE-ing-en. ['laɪnɪŋən]

**Q:** What's a group ID? How do snapshots work?  
**A:** See the
  [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
  for background.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** The [deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
  explains how to set up a private repository. If you are not sharing
  them with a team you could also just [install locally](https://github.com/kumarshantanu/lein-localrepo).

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
**A:** TODO: document aether proxy setup.

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
**A:** There are two flavours of Hotspot (Oracle/OpenJDK's JVM),
  client and server. The server is optimized for long-running
  processes and has quite a poor startup time. Leiningen will try to
  launch a client JVM, but this only works on 32-bit Hotspot. If you
  are on a 64-bit machine you can still use a client JVM if you
  install 32-bit packages. TODO: document on wiki.
  
**Q:** I don't have access to stdin inside my project.  
**A:** There's a problem in the library that Leiningen uses to spawn
  new processes that blocks access to console input. This means that
  functions like `read-line` will not work as expected in most
  contexts, though the `repl` task necessarily includes a
  workaround. You can also use the `trampoline` task to
  launch your project's JVM after Leiningen's has exited rather than
  launching it as a subprocess. TODO: document in-process classloader

## Contributing

Please report issues on the [Github issue
tracker](https://github.com/technomancy/leiningen/issues) or the
[mailing list](http://groups.google.com/group/leiningen). Personal
email addresses are **not** appropriate for bug reports. See the file
`HACKING.md` for more details on how Leiningen's codebase is structured.

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

Using Leiningen 1.x, run `lein deps` in the `leiningen-core`
subproject directory. Once you do that in most cases a `bin/lein
self-install` will usually get you what you need. However, this will
occasionally fail for very new SNAPSHOT versions since the standalone
jar will not have been uploaded yet.

## License

Source Copyright © 2009-2012 Phil Hagelberg, Alex Osborne, Dan Larkin, and
[other contributors](https://www.ohloh.net/p/leiningen/contributors). 
Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Images Copyright © 2010 Phil Hagelberg. Distributed under the Creative
Commons Attribution + ShareAlike
License. [Full-size version](https://github.com/downloads/technomancy/leiningen/leiningen-full.jpg)
available.
