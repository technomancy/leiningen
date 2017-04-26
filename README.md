# Leiningen

<img src="http://leiningen.org/img/leiningen.jpg"
 alt="Leiningen logo" title="The man himself" align="right" />

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight&mdash;they're an elemental&mdash;an 'act of God!' Ten miles long, two
> miles wide&mdash;ants, nothing but ants! And every single one of them a
> fiend from hell..."
> - from [Leiningen Versus the Ants](http://www.classicshorts.com/stories/lvta.html) by Carl Stephenson

Leiningen is for automating Clojure projects without setting your hair on fire.

## Installation

If your preferred
[package manager](https://github.com/technomancy/leiningen/wiki/Packaging)
offers a recent version of Leiningen, try that first.  Many package managers still
include version 1.x, which is rather outdated, so you may be better off installing manually.

Leiningen installs itself on the first run of the `lein` shell script; there is no
separate install script.  Follow these instructions to install Leiningen manually:

1. Make sure you have a Java JDK version 6 or later.
2. [Download the `lein` script from the `stable` branch](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein)
 of this project.
3. Place it on your `$PATH`. (`~/bin` is a good choice if it is on your path.)
4. Set it to be executable. (`chmod 755 ~/bin/lein`, rwx-r-x-r-x)
5. Run it.

### Windows

There is
[an installer](http://leiningen-win-installer.djpowell.net/) which
will handle downloading and placing Leiningen and its dependencies.

The manual method of putting
[the batch file](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.bat).
on your `PATH` and running `lein self-install` should still work for
most users. If you have [Cygwin](http://www.cygwin.com/) you should be
able to use the shell script above rather than the batch file.

N.B.: Avoid using a LEIN_JAR environment variable.
If you have one instance of Leiningen on your PATH as well as a LEIN_JAR
pointing to a different instance, there is a known problem where `lein -v` and
`lein update` will refer to the first instance, while usage of Leiningen, such
as `lein new foobar`, will refer to the latter.

## Basic Usage

The
[tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
has a detailed walk-through of the steps involved in creating a new
project, but here are the commonly-used tasks:

    $ lein new [TEMPLATE] NAME # generate a new project skeleton

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch an interactive REPL session

    $ lein run -m my.namespace # run the -main function of a namespace

    $ lein uberjar # package the project and dependencies as standalone jar

    $ lein deploy clojars # publish the project to Clojars as a library

Use `lein help` to see a complete list. `lein help $TASK` shows the
usage for a specific task.

You can also chain tasks together in a single command by using the
`do` task with comma-separated tasks:

    $ lein do clean, test foo.test-core, jar

Most tasks need to be run from somewhere inside a project directory to
work, but some (`new`, `help`, `search`, `version`, and `repl`) may
run from anywhere.

## Configuration

The `project.clj` file in the project root should look like this:

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :license "Eclipse Public License 1.0"
  :url "http://github.com/technomancy/myproject"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-tar "3.2.0"]])
```

The `lein new` task generates a project skeleton with an appropriate
starting point from which you can work. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
file (also available via `lein help sample`) for a detailed listing of
configuration options.

The `project.clj` file can be customized further with the use of
[profiles](https://github.com/technomancy/leiningen/blob/stable/doc/PROFILES.md).

## Documentation

Leiningen documentation is organized as a number of guides:

### Usage

 * [Tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md) (start here if you are new)
 * [FAQ](https://github.com/technomancy/leiningen/blob/stable/doc/FAQ.md)
 * [Profiles](https://github.com/technomancy/leiningen/blob/stable/doc/PROFILES.md)
 * [Deployment & Distribution of Libraries](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
 * [Sample project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
 * [Polyglot (e.g. Clojure/Java) projects](https://github.com/technomancy/leiningen/blob/stable/doc/MIXED_PROJECTS.md)

### Development

* [Writing Plugins](https://github.com/technomancy/leiningen/blob/stable/doc/PLUGINS.md)
* [Writing Templates](https://github.com/technomancy/leiningen/blob/stable/doc/TEMPLATES.md)
* [Contributing](https://github.com/technomancy/leiningen/blob/stable/CONTRIBUTING.md)
* [Building Leiningen](https://github.com/technomancy/leiningen/blob/stable/CONTRIBUTING.md#bootstrapping)

## Plugins

Leiningen supports plugins which may contain both new tasks and hooks
that modify behaviour of existing tasks. See
[the plugins wiki page](https://github.com/technomancy/leiningen/wiki/Plugins)
for a full list. If a plugin is needed for successful test or build
runs, (such as `lein-tar`) then it should be added to `:plugins` in
project.clj, but if it's for your own convenience (such as
`lein-pprint`) then it should be added to the `:plugins` list in the
`:user` profile in `~/.lein/profiles.clj`. See the
[profiles guide](https://github.com/technomancy/leiningen/blob/stable/doc/PROFILES.md)
for details on how to add to your `:user` profile. The
[plugin guide](https://github.com/technomancy/leiningen/blob/stable/doc/PLUGINS.md)
explains how to write plugins.

## License

Source Copyright © 2009-2016 Phil Hagelberg, Alex Osborne, Dan Larkin, and
[contributors](https://github.com/technomancy/leiningen/contributors).
Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Images Copyright © 2010 Phil Hagelberg. Distributed under the Creative
Commons Attribution + ShareAlike
License. [Full-size version](http://leiningen.org/img/leiningen-full.jpg)
available.
