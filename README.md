# Leiningen

<img src="https://github.com/downloads/technomancy/leiningen/leiningen-banner.png"
 alt="Leiningen logo" title="The man himself" align="right" />

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight&mdash;they're an elemental&mdash;an 'act of God!' Ten miles long, two
> miles wide&mdash;ants, nothing but ants! And every single one of them a
> fiend from hell..."
> - from Leiningen Versus the Ants by Carl Stephenson

Leiningen is for automating Clojure projects without setting your hair on fire.

## Installation

If your preferred
[package manager](https://github.com/technomancy/leiningen/wiki/Packaging)
has a relatively recent version of Leiningen, try that first.
Otherwise you can install by hand:

Leiningen bootstraps itself using the `lein` shell script;
there is no separate install script. It installs its dependencies
upon the first run on unix, so the first run will take longer.

1. [Download the script](https://raw.github.com/technomancy/leiningen/preview/bin/lein).
2. Place it on your `$PATH`. (I like to use `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/lein`)

The link above will get you the 2.x preview release. There is still a
lot of extant material on the Web concerning the older
[Leiningen 1.x](https://raw.github.com/technomancy/leiningen/stable/bin/lein)
version, which is still available if you need to work on older
projects that aren't compatible with 2.x yet. The
[upgrade guide](https://github.com/technomancy/leiningen/wiki/Upgrading)
has instructions on migrating to version 2.

On Windows most users can get
[the batch file](https://raw.github.com/technomancy/leiningen/preview/bin/lein.bat).
If you have wget.exe or curl.exe already installed and in PATH, you
can just run `lein self-install`, otherwise get the standalone jar from the
[downloads page](https://github.com/technomancy/leiningen/downloads).
If you have [Cygwin](http://www.cygwin.com/) you should be able to use
the shell script above rather than the batch file.

## Basic Usage

The
[tutorial](https://github.com/technomancy/leiningen/blob/preview/doc/TUTORIAL.md)
has a detailed walk-through of the steps involved in creating a new
project, but here are the commonly-used tasks:

    $ lein new [TEMPLATE] NAME # generate a new project skeleton

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch an interactive REPL session

    $ lein run -m my.namespace # run the -main function of a namespace

    $ lein uberjar # package the project and dependencies as standalone jar

Use `lein help` to see a complete list. `lein help $TASK` shows the
usage for a specific task.

You can also chain tasks together in a single command by using the
`do` task with comma-separated tasks:

    $ lein do clean, test foo.test-core, jar

Most tasks need to be run from somewhere inside a project directory to
work, but some (`new`, `help`, `search`, `version`, and `repl`) may
run from anywhere.

See the [FAQ](https://github.com/technomancy/leiningen/blob/master/doc/FAQ.md)
for more details.

## Configuration

The `project.clj` file in the project root should look like this:

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :url "http://github.com/technomancy/myproject"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :plugins [[lein-ring "0.4.5"]])
```

The `lein new` task generates a project skeleton with an appropriate
starting point from which you can work. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/preview/sample.project.clj)
file (also available via `lein help sample`) for a detailed listing of
configuration options.

The `project.clj` file can be customized further with the use of
[profiles](https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md).

## Plugins

Leiningen supports plugins which may contain both new tasks and hooks
that modify behaivour of existing tasks. See
[the plugins wiki page](https://github.com/technomancy/leiningen/wiki/Plugins)
for a full list. If a plugin is needed for successful test or build
runs, (such as `lein-tar`) then it should be added to `:plugins` in
project.clj, but if it's for your own convenience (such as
`swank-clojure`) then it should be added to the `:plugins` list in the
`:user` profile from `~/.lein/profiles.clj`. See the
[profiles guide](https://github.com/technomancy/leiningen/blob/preview/doc/PROFILES.md)
for details on how to add to your user profile. The
[plugin guide](https://github.com/technomancy/leiningen/blob/preview/doc/PLUGINS.md)
explains how to write plugins.

## Contributing

Please report issues on the
[GitHub issue tracker](https://github.com/technomancy/leiningen/issues)
or the [mailing list](http://librelist.com/browser/leiningen/).
Personal email addresses are **not** appropriate for bug reports. See
the
[readme for the leiningen-core library](https://github.com/technomancy/leiningen/blob/master/leiningen-core/README.md)
and `doc/PLUGINS.md` for more details on how Leiningen's codebase is
structured. Design discussions also occur in the
[#leiningen channel on Freenode](irc://chat.freenode.net#leiningen).

Patches are preferred as GitHub pull requests, though patches from
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
mailing list and mailing a self-addressed, stamped envelope.

## Building

You don't need to "build" Leiningen per se, but when you're using a
checkout you will need to get its dependencies in place.

For the master branch, use an existing Leiningen version (currently this means
Leiningen 1) to run `lein install` in the `leiningen-core` subproject directory. When the
dependencies change you will also have to do `rm .lein-classpath` in
the project root. It's not yet possible to bootstrap a development
version of Leiningen without having an older version installed.

Once you've done that, symlink `bin/lein` to somewhere on your
`$PATH`. Usually you'll want to rename your existing installation to
keep them from interfering.

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
