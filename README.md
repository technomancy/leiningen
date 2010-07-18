# Leiningen

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight--they're an elemental--an 'act of God!' Ten miles long, two
> miles wide--ants, nothing but ants! And every single one of them a
> fiend from hell..."
> -- from Leiningen Versus the Ants by Carl Stephenson

Leiningen is a build tool for Clojure designed to not set your hair on fire.

<img src="http://github.com/technomancy/leiningen/raw/master/resources/leiningen.png" alt="Leiningen logo" title="The man himself" align="right" />

Building Clojure projects with tools designed for Java can be an
exercise in frustration. If you use Ant, you end up copying around a
lot of the same tasks between XML files on all your projects;
there's a lot of repetition. Maven avoids repetition, but provides
very little transparency into what's really going on behind the scenes
and forces you to become a Maven expert to script a nontrivial
build. Either way you must write far more XML than is decent.

With Leiningen, you describe your build with Clojure. Any code you
need goes in your project.clj file; the only requirement is calling
defproject. You can define your own tasks in your project if you need
to, but most projects get by on the tasks provided with Leiningen. If
you do find a common task that you need to add, you can implement it
as a plugin rather than copying and pasting among each of your
projects.

## Installation

Leiningen bootstraps itself using the <tt>lein</tt> shell script;
there is no separate 'install script'. It installs its dependencies in
$HOME/.m2/repository.

1. [Download the script](http://github.com/technomancy/leiningen/raw/stable/bin/lein).
2. Place it on your path and chmod it to be executable.
3. Run: <tt>lein self-install</tt>

For development versions you may use [the master version of the lein
script](http://github.com/technomancy/leiningen/raw/master/bin/lein) instead.

On Windows you can download
[lein.bat](http://github.com/technomancy/leiningen/raw/master/bin/lein.bat),
instead, though support on that platform is still experimental.

## Usage

The
[tutorial](http://github.com/technomancy/leiningen/blob/master/TUTORIAL.md)
has a detailed walk-through of the steps involved in creating a new
project, but here are the commonly-used tasks:

    $ lein new NAME # generate a new project skeleton

    $ lein deps # install dependencies in lib/

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch an interactive REPL session

    $ lein jar # package up the whole project as a .jar file

Use <tt>lein help</tt> to see a complete list. <tt>lein help
$TASK</tt> shows the usage for a specific one.

## Configuration

Place a project.clj file in the project root like this:

    (defproject leiningen "0.5.0-SNAPSHOT"
      :description "A build tool designed not to set your hair on fire."
      :url "http://github.com/technomancy/leiningen"
      :dependencies [[org.clojure/clojure "1.1.0]
                     [org.clojure/clojure-contrib "1.1.0"]
                     [ant/ant-launcher "1.6.2"]
                     [org.apache.maven/maven-ant-tasks "2.0.10"]]
      :dev-dependencies [[swank-clojure "1.2.1"]])

The <tt>lein new</tt> task generates a project skeleton with an
appropriate starting point from which you can work. See the
[sample.project.clj](http://github.com/technomancy/leiningen/blob/master/sample.project.clj)
file for a detailed listing of configuration options.

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
  [tutorial](http://github.com/technomancy/leiningen/blob/master/TUTORIAL.md)
  for background.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** Open-source jars can be uploaded to Clojars (see "Publishing"
  in the tutorial), though be sure to use the group-id of
  "org.clojars.$USERNAME" in order to avoid conflicts and to allow the
  original authors to claim it in the future once they get around to
  uploading. Alternatively you can do a one-off install into your
  local repository in ~/.m2 with Maven. Add a dependency to
  project.clj that doesn't exist in any remote repository and run
  <tt>lein deps</tt>. It won't succeed, but the output will include
  the <tt>mvn</tt> invocation to do this. It's _much_ better to get
  the dependency in a remote repository for repeatability reasons
  though. For teams working on private projects
  [Hudson](http://hudson-ci.org/) works well.

**Q:** How do I write my own tasks?  
**A:** If it's a task that may be useful to more than just your
  project, you should make it into a
  [plugin](http://github.com/technomancy/leiningen/blob/master/PLUGINS.md).
  You can also include one-off tasks in your src/leiningen/ directory
  if they're not worth spinning off; the plugin guide shows how.

**Q:** I want to hack two projects in parallel, but it's annoying to switch between them.  
**A:** Use a feature called _checkout dependencies_. If you create
  a directory called <tt>checkouts</tt> in your project root and
  symlink some other projects into it, Leiningen will allow you to
  hack on them in parallel. That means changes in the dependency will
  be visible in the main project without having to go through the
  whole install/switch-projects/deps/restart-swank cycle.

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

**Q:** What does java.lang.NoSuchMethodError: clojure.lang.RestFn.<init>(I)V mean?  
**A:** It means you have some code that was AOT (ahead-of-time)
  compiled with a different version of Clojure than the one you're
  currently using. If it persists after running <tt>lein clean</tt> then it
  is a problem with your dependencies. If you depend on contrib, make
  sure the contrib version matches the Clojure version. Also note for
  your own project that AOT compilation in Clojure is much less
  important than it is in other languages. There are a few
  language-level features that must be AOT-compiled to work, generally
  for Java interop. If you are not using any of these features, you
  should not AOT-compile your project if other projects may depend
  upon it.

## Hacking

Leiningen is very small. The latest release is only 890 lines of
Clojure; you could probably read through the whole project in an hour.

When you launch Leiningen, it must start an instance of Clojure to
load itself. But this instance must not effect the project that you're
building. It may use a different version of Clojure from Leiningen,
and the project should be in a fresh JVM. Leiningen uses ant's
<tt>java</tt> task to fork off a separate process for this
purpose. The <tt>leiningen.compile</tt> namespace implements this;
specifically the <tt>eval-in-project</tt> function. Any code that must
execute within the context of the project (AOT compilation, test runs)
needs to go through this function.

Leiningen is extensible; you can define new tasks in plugins. Add your
plugin as a dev-dependency of your project, and you'll be able to call
<tt>lein $YOUR_COMMAND</tt>. See the [plugins guide](http://github.com/technomancy/leiningen/blob/master/PLUGINS.md) for details.

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

See the [complete list of known issues](http://github.com/technomancy/leiningen/issues).

## License

Copyright (C) 2009-2010 Phil Hagelberg, Alex Osborne, and Dan Larkin

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
