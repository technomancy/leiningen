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

Copy bin/lein to a location on your $PATH and run: $ lein self-install

## Usage

    $ lein deps # install dependencies in lib/

    $ lein test [PRED] # run the project's tests, optionally filtered on PRED

    $ lein compile # ahead-of-time compile into classes/

    $ lein repl # launch a REPL with the project classpath configured

    $ lein clean # remove all build artifacts

    $ lein jar # create a jar of the project

    $ lein uberjar # create a standalone jar that contains all dependencies

    $ lein swank [PORT] # launch swank server for Emacs to connect

TODO: install, new, help, deploy, pom

## Configuration

Place a project.clj file in the project root that looks something like this: 

    (defproject leiningen "0.5.0-SNAPSHOT"
      :dependencies [[org.clojure/clojure "1.1.0-alpha-SNAPSHOT"]
                     [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
                     [ant/ant-launcher "1.6.2"]
                     [org.apache.maven/maven-ant-tasks "2.0.10"]]
      :dev-dependencies [[org.clojure/swank-clojure "1.0"]])

Other keys you can set are :namespaces to compile if you don't want
all of them AOT'd as well as a :main namespace for building executable jars.

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
   But if you're able to write it with a more pleasing syntax, it's
   not so bad.

**Q:** What happened to [Corkscrew](http://github.com/technomancy/corkscrew)?  
**A:** I tried, but I really couldn't make the wine metaphor work. That,
   and the Plexus Classworlds container was an ornery beast causing
   much frustration.

**Q:** What about Windows?  
**A:** Patches welcome.

## Hacking

Working on the Leiningen codebase has a few unique challenges since
there's a bit of a chicken-and-egg bootstrap problem. To go from a
clean checkout to a working environment, the following steps are
necessary:

0. Place bin/lein on your $PATH somewhere.
1. Do a self-install of leiningen (from outside the checkout tree).
2. Place ~/.leiningen.jar in lib.
3. Invoke "lein compile" followed by "lein deps".
4. Remove .leiningen.jar from lib.
5. Invoke "lein uberjar", and place the jar in ~/.leiningen.jar for
   future use.

Leiningen is extensible, you can define new tasks in plugins. Add your
plugin as a dev-dependency of your project, and you'll be able to call
"lein $YOUR_COMMAND". See the lein-swank directory for an example of a
plugin.

## License

Copyright (C) 2009 Phil Hagelberg

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
