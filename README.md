# Leiningen

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight--they're an elemental--an 'act of God!' Ten miles long, two
> miles wide--ants, nothing but ants! And every single one of them a
> fiend from hell...
> -- from Leiningen Versus the Ants by Carl Stephenson

Leiningen is a build tool for Clojure designed to not set your hair on fire.

Building Clojure projects with tools designed for Java can be an
exercise in frustration. If you use Ant, you end up copying around a
lot of the same tasks around between XML files on all your projects;
there's a lot of repetition. Maven avoids repetition, but provides
very little transparency into what's really going on behind the scenes
and forces you to become a Maven expert to script a nontrivial
build. Either way you end up writing far more XML than is necessary.

With Leiningen, your build is described using Clojure. You can put any
code you like in your project.clj file; the only requirement is that
it includes a call to defproject. You can define your own tasks in
there if you need to, but the majority of projects should be able to
get by on the tasks that are provided with Leiningen. If you do find a
common task that you need to add, you can implement it as a plugin
rather than copying and pasting among each of your projects.

## Installation

1. Download the script: http://github.com/technomancy/leiningen/raw/stable/bin/lein
2. Place it on your path and chmod it to be executable.
3. Run: <tt>lein self-install</tt>

This works best with stable versions of Leiningen; for development
versions see "Hacking" below.

## Usage

    $ lein deps # install dependencies in lib/

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein compile # ahead-of-time compile into classes/

    $ lein repl # launch a REPL with the project classpath configured

    $ lein clean # remove all build artifacts

    $ lein jar # create a jar of the project

    $ lein uberjar # create a standalone jar that contains all dependencies

    $ lein pom # output a pom.xml file for interop with Maven

    $ lein install # install in local repository

    $ lein help [TASK] # show a list of tasks or help for a given TASK

    $ lein new NAME # generate a new project skeleton

## Configuration

Place a project.clj file in the project root that looks something like this: 

    (defproject leiningen "0.5.0-SNAPSHOT"
      :description "A build tool designed not to set your hair on fire."
      :dependencies [[org.clojure/clojure "1.1.0-alpha-SNAPSHOT"]
                     [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
                     [ant/ant-launcher "1.6.2"]
                     [org.apache.maven/maven-ant-tasks "2.0.10"]]
      :dev-dependencies [[org.clojure/swank-clojure "1.0"]])

Other keys you can set are :namespaces to compile if you don't want
all of them AOT'd as well as a :main namespace for building executable jars.

Currently Leiningen can only be used to compile projects that use the
same version of Clojure as it uses, though this restriction should go
away soon.

## FAQ

**Q:** How do you pronounce Leiningen?  
**A:** It's LINE-ing-en. I think.

**Q:** What does this offer over [Lancet](http://github.com/stuarthalloway/lancet)?  
**A:** Lancet is more of a library than a build tool. It doesn't predefine
   any tasks apart from what Ant itself offers, so there is nothing
   Clojure-specific in it. Leiningen builds on Lancet, but takes
   things further. In addition, it includes some Maven functionality
   for dependencies.

**Q:** But Maven is terrifying!  
**A:** That's not a question. Anyway, Leiningen only uses the dependency
   resolution parts of Maven, which are quite tame. For the actual
   task execution cycles it uses Ant under the covers via Lancet.

**Q:** But Ant is terrifying!  
**A:** That's [true](http://www.defmacro.org/ramblings/lisp.html). Ant is
   an interpreter for a [procedural language with a regrettable 
   syntax](http://blogs.tedneward.com/2005/08/22/When+Do+You+Use+XML+Again.aspx).
   But if you treat it as a standard library of build-related
   functions and are able to write it with a more pleasing syntax, it's
   not bad.

**Q:** What happened to [Corkscrew](http://github.com/technomancy/corkscrew)?  
**A:** I tried, but I really couldn't make the wine metaphor work. That,
   and the Plexus Classworlds container was an ornery beast causing
   much frustration. The maven-ant-tasks API is much more manageable.

**Q:** What about Windows?  
**A:** Patches welcome.

## Publishing

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to maintain your own or get
it into Maven central, the easiest way is to publish it at
[Clojars](http://clojars.org), which is a Clojure-specific repository
for open-source code. Once you have created an account there,
publishing is easy:

    $ lein pom
    $ scp pom.xml $PROJECT.jar clojars@clojars.org:

Once that succeeds it will be available for other projects to depend on.

## Hacking

You'll need to bootstrap using a stable release before you can hack on
Leiningen. Grab the stable bin script (linked under "Installation"
above), put it on your $PATH as "lein-stable", and do "lein-stable
self-install". Then run "lein-stable deps" in your checkout. When that
finishes, symlink bin/lein from your checkout to your path.  This will
make "lein" run from your checkout while "lein-stable" uses the jar
self-installed in ~/.m2.

The [mailing list](http://groups.google.com/group/clojure) and the
leiningen or clojure channels on Freenode are the best places to
bring up questions or suggestions. If you're planning on adding a
feature or fixing a nontrivial bug, please discuss it first to avoid
duplicating effort.

Contributions are preferred as either Github pull requests or using
"git format-patch" as is requested [for contributing to Clojure
itself](http://clojure.org/patches). Please use standard indentation
with no tabs, trailing whitespace, or lines longer than 80 columns. If
you've got some time on your hands, reading this [style
guide](http://mumble.net/~campbell/scheme/style.txt) wouldn't hurt
either.

Leiningen is extensible; you can define new tasks in plugins. Add your
plugin as a dev-dependency of your project, and you'll be able to call
"lein $YOUR_COMMAND". See the lein-swank directory for an example of a
plugin.

## License

Copyright (C) 2009 Phil Hagelberg, Alex Osborne, and Dan Larkin

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
