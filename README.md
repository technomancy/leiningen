# Leiningen

[![status-badge](https://ci.codeberg.org/api/badges/leiningen/leiningen/status.svg)](https://ci.codeberg.org/leiningen/leiningen)

<img src="https://leiningen.org/img/leiningen.jpg"
 alt="Leiningen logo" title="The man himself" align="right" />

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight&mdash;they're an elemental&mdash;an 'act of God!' Ten miles long, two
> miles wide&mdash;ants, nothing but ants! And every single one of them a
> fiend from hell..."
> - from [Leiningen Versus the Ants](http://www.classicshorts.com/stories/lvta.html) by Carl Stephenson

Leiningen is for automating Clojure projects without setting your hair on fire.

Note: the canonical repository for Leiningen is [on
Codeberg](https://codeberg.org/leiningen/leiningen) but we maintain [a
mirror on GitHub](https://github.com/technomancy/leiningen) for the
time being in order to ease the transition. Please update your links
and git remotes.

## Installation

If your preferred
[package manager](https://codeberg.org/leiningen/leiningen/wiki/Packaging)
offers a recent version of Leiningen, try that first.

Leiningen installs itself on the first run of the `lein` shell script; there is no
separate install script.  Follow these instructions to install Leiningen manually:

1. Make sure you have Java installed; [OpenJDK](https://adoptium.net) is recommended
2. [Download the `lein` script from the `stable` branch](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein)
 of this project
3. Place it on your `$PATH` (`/usr/local/bin` for example)
4. Set it to be executable. (`sudo chmod +x /usr/local/bin/lein`)
5. Run it.

Windows users can use the above script in the Linux subsystem or try
[the batch file](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.bat) or
[Powershell version](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.ps1)
instead.

## Basic Usage

The
[tutorial](https://codeberg.org/leiningen/leiningen/src/stable/doc/TUTORIAL.md)
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
  :url "http://codelab.org/technomancy/myproject"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-tar "3.2.0"]])
```

The `lein new` task generates a project skeleton with an appropriate
starting point from which you can work. See the
[sample.project.clj](https://codeberg.org/leiningen/leiningen/src/stable/sample.project.clj)
file (also available via `lein help sample`) for a detailed listing of
configuration options.

The `project.clj` file can be customized further with the use of
[profiles](https://codeberg.org/leiningen/leiningen/src/stable/doc/PROFILES.md).

## Documentation

Leiningen documentation is organized as a number of guides:

### Usage

 * [Tutorial](https://codeberg.org/leiningen/leiningen/src/stable/doc/TUTORIAL.md) (start here if you are new)
 * [FAQ](https://codeberg.org/leiningen/leiningen/src/stable/doc/FAQ.md)
 * [Profiles](https://codeberg.org/leiningen/leiningen/src/stable/doc/PROFILES.md)
 * [Deployment & Distribution of Libraries](https://codeberg.org/leiningen/leiningen/src/stable/doc/DEPLOY.md)
 * [Sample project.clj](https://codeberg.org/leiningen/leiningen/src/stable/sample.project.clj)
 * [Polyglot (e.g. Clojure/Java) projects](https://codeberg.org/leiningen/leiningen/src/stable/doc/MIXED_PROJECTS.md)

### Development

* [Writing Plugins](https://codeberg.org/leiningen/leiningen/src/stable/doc/PLUGINS.md)
* [Writing Templates](https://codeberg.org/leiningen/leiningen/src/stable/doc/TEMPLATES.md)
* [Contributing](https://codeberg.org/leiningen/leiningen/src/stable/CONTRIBUTING.md)
* [Building Leiningen](https://codeberg.org/leiningen/leiningen/src/stable/CONTRIBUTING.md#bootstrapping)

## Plugins

Leiningen supports plugins which may introduce new tasks. See
[the plugins wiki page](https://codeberg.org/leiningen/leiningen/wiki/Plugins)
for a full list. If a plugin is needed for successful test or build
runs, (such as `lein-tar`) then it should be added to `:plugins` in
project.clj, but if it's for your own convenience (such as
`lein-pprint`) then it should be added to the `:plugins` list in the
`:user` profile in `~/.lein/profiles.clj`. See the
[profiles guide](https://codeberg.org/leiningen/leiningen/src/stable/doc/PROFILES.md)
for details on how to add to your `:user` profile. The
[plugin guide](https://codeberg.org/leiningen/leiningen/src/stable/doc/PLUGINS.md)
explains how to write plugins.

## License

Source Copyright © 2009-2022 Phil Hagelberg, Alex Osborne, Dan Larkin, and
contributors.
Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Images Copyright © 2010 Phil Hagelberg. Distributed under the Creative
Commons Attribution + ShareAlike
License 4.0. [Full-size version](https://leiningen.org/img/leiningen-full.jpg)
available.
