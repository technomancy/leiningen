# Leiningen

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight--they're an elemental--an 'act of God!' Ten miles long, two
> miles wide--ants, nothing but ants! And every single one of them a
> fiend from hell..."
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

Leiningen bootstraps itself using the 'lein' shell script you
download, there is no separate 'install script'. It installs its
dependencies in $HOME/.m2/repository.

1. [Download the script](http://github.com/technomancy/leiningen/raw/stable/bin/lein).
2. Place it on your path and chmod it to be executable.
3. Run: <tt>lein self-install</tt>

This only works with stable versions of Leiningen; for development
versions see "Hacking" below.

On Windows you can download
[lein.bat](http://github.com/technomancy/leiningen/raw/stable/bin/lein.bat),
instead, though support on that platform is still experimental.

## Usage

    $ lein new NAME # generate a new project skeleton

    $ lein deps # install dependencies in lib/

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch a REPL with the project classpath configured

    $ lein clean # remove all build artifacts

    $ lein jar # create a jar of the project

    $ lein uberjar # create a standalone jar that contains all dependencies

    $ lein install # install in local repository

These are the most commonly-used tasks; you can use "lein help" to see
a complete list. "lein help $TASK" will show the usage for a specific one.

## Configuration

Place a project.clj file in the project root that looks something like this:

    (defproject leiningen "0.5.0-SNAPSHOT"
      :description "A build tool designed not to set your hair on fire."
      :url "http://github.com/technomancy/leiningen"
      :dependencies [[org.clojure/clojure "1.1.0]
                     [org.clojure/clojure-contrib "1.1.0"]
                     [ant/ant-launcher "1.6.2"]
                     [org.apache.maven/maven-ant-tasks "2.0.10"]]
      :dev-dependencies [[org.clojure/swank-clojure "1.2.1"]])

The "lein new" task will generate a project skeleton with an
appropriate starting point from which you can work. See the file
sample.project.clj for a detailed listing of all the configuration
options that Leiningen knows about.

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

**Q:** What's a group ID? How do snapshots work?  
**A:** See the
  [intro](http://github.com/technomancy/leiningen/blob/master/INTRO.md)
  for background on JVM dependency concepts.

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** Open-source jars can be uploaded to Clojars (see "Publishing"
  below), though be sure to use the group-id of "org.clojars.$USERNAME"
  in order to avoid conflicts and to allow the original authors to
  claim it in the future once they get around to uploading. 
  Alternatively you can do a one-off install into your local repository in
 ~/.m2 with Maven for Java libs or "lein install" for Clojure libs.

**Q:** What does java.lang.NoSuchMethodError: clojure.lang.RestFn.<init>(I)V mean?  
**A:** It means you have some code that was AOT (ahead-of-time)
  compiled with a different version of Clojure than the one you're
  currently using. If it persists after running "lein clean" then it
  is a problem with your dependencies. If you depend on contrib, make
  sure the contrib version matches the Clojure version. Also note for
  your own project that AOT compilation in Clojure is much less
  important than it is in other languages. There are a few
  language-level features that must be AOT-compiled to work, generally
  for Java interop. If you are not using any of these features, you
  should not AOT-compile your project if other projects may depend
  upon it.

**Q:** Is it possible to exclude indirect dependencies?  
**A:** Yes. Some libraries, such as log4j, depend on projects that are
  not included in public repositories and unnecessary for basic
  functionality.  Projects listed as :dependencies may exclude 
  any of their dependencies by using the :exclusions key. See
  sample.project.clj for details.

**Q:** It says a required artifact is missing for "super-pom". What's that?  
**A:** The Maven API that Leiningen uses refers to your project as
  "super-pom". It's just a quirk of the API. It probably means there
  is a typo in your :dependency declaration in project.clj.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

## Publishing

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to maintain your own or get
it into Maven central, the easiest way is to publish it at
[Clojars](http://clojars.org), which is a Clojure-specific repository
for open-source code. Once you have created an account there,
publishing is easy:

    $ lein jar && lein pom
    $ scp pom.xml $PROJECT.jar clojars@clojars.org:

Once that succeeds it will be available for other projects to depend
on. Leiningen adds Clojars and [the Clojure nightly build
snapshots](http://build.clojure.org) to the default repositories.

## Hacking

You'll need to bootstrap using a stable release before you can hack on
Leiningen. Grab the stable bin script (linked under "Installation"
above), put it on your $PATH as "lein-stable", and do "lein-stable
self-install". Then run "lein-stable deps" in your checkout. When that
finishes, symlink bin/lein from your checkout to your path.  This will
make "lein" run from your checkout while "lein-stable" uses the jar
self-installed in ~/.m2.

The [mailing list](http://groups.google.com/group/leiningen) and the
leiningen or clojure channels on Freenode are the best places to
bring up questions or suggestions. If you're planning on adding a
feature or fixing a nontrivial bug, please discuss it first to avoid
duplicating effort. If you haven't discussed it on the mailing list,
please include in your pull request details of what problem your patch
intendeds to solve as well as the approach you took.

Contributions are preferred as either Github pull requests or using
"git format-patch" and the mailing list as is requested [for
contributing to Clojure itself](http://clojure.org/patches). Please
use standard indentation with no tabs, trailing whitespace, or lines
longer than 80 columns. See [this post on submitting good
patches](http://technomancy.us/135) for some tips. If you've got some
time on your hands, reading this [style
guide](http://mumble.net/~campbell/scheme/style.txt) wouldn't hurt
either.

Leiningen is extensible; you can define new tasks in plugins. Add your
plugin as a dev-dependency of your project, and you'll be able to call
"lein $YOUR_COMMAND". See the file PLUGINS.md for details.

See the [complete list of known issues](http://github.com/technomancy/leiningen/issues).

## License

Copyright (C) 2009-2010 Phil Hagelberg, Alex Osborne, and Dan Larkin

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
