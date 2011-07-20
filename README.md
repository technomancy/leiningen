# Leiningen

> "Leiningen!" he shouted. "You're insane! They're not creatures you can
> fight--they're an elemental--an 'act of God!' Ten miles long, two
> miles wide--ants, nothing but ants! And every single one of them a
> fiend from hell..."
> -- from Leiningen Versus the Ants by Carl Stephenson

Leiningen is for automating Clojure projects without setting your hair on fire.

<img src="https://github.com/downloads/technomancy/leiningen/leiningen-banner.png" alt="Leiningen logo" title="The man himself" align="right" />

Working on Clojure projects with tools designed for Java can be an
exercise in frustration. With Leiningen, you just write Clojure.

## Installation

Leiningen bootstraps itself using the <tt>lein</tt> shell script;
there is no separate 'install script'. It installs its dependencies
upon the first run on unix, so the first run will take longer.

1. [Download the script](https://github.com/technomancy/leiningen/raw/stable/bin/lein).
2. Place it on your path and chmod it to be executable.

I like to place it in ~/bin, but it can go anywhere on the $PATH.

On Windows most users can

1. Download the Windows distribution
[leiningen-1.5.2-win.zip](https://github.com/downloads/technomancy/leiningen/leiningen-1.5.2-win.zip)
2. Unzip in a folder of choice.
3. Include the "lein" directory in PATH.

If you have wget.exe or curl.exe already installed and in PATH, you
can download either [the stable version
lein.bat](https://github.com/technomancy/leiningen/raw/stable/bin/lein.bat),
or [the development
version](https://github.com/technomancy/leiningen/raw/master/bin/lein.bat)
and use self-install.

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

Use <tt>lein help</tt> to see a complete list. <tt>lein help
$TASK</tt> shows the usage for a specific one.

You can also chain tasks together in a single command by using commas:

    $ lein clean, test foo.test-core, jar

Most tasks need to be run from somewhere inside a project directory to
work, but some (<tt>new</tt>, <tt>help</tt>, <tt>version</tt>,
<tt>plugin</tt>, and the two-argument version of <tt>install</tt>) may
run from anywhere.

The install task places shell scripts in the <tt>~/.lein/bin</tt>
directory for projects that include them, so if you want to take
advantage of this, you should put it on your <tt>$PATH</tt>.

## Configuration

The <tt>project.clj</tt> file in the project root should look like this:

    (defproject myproject "0.5.0-SNAPSHOT"
      :description "A project for doing things."
      :url "http://github.com/technomancy/myproject"
      :dependencies [[org.clojure/clojure "1.2.1"]
                     [org.clojure/clojure-contrib "1.2.0"]]
      :dev-dependencies [[lein-ring "0.4.5"]])

The <tt>lein new</tt> task generates a project skeleton with an
appropriate starting point from which you can work. See the
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
file for a detailed listing of configuration options.

You can also have user-level configuration that applies for all
projects. The <tt>~/.lein/init.clj</tt> file will be loaded every time
Leiningen launches; any arbitrary code may go there. This code is
executed inside Leiningen itself, not in your project. Set the
<tt>:repl-init</tt> key in project.clj to point to a namespace if
you want code executed inside your project.

You can also manage your plugins with the <tt>plugin</tt> task. Use
the same arguments you would put in the Leiningen :dev-dependencies if
you were only using the plugin on a single project.

    $ lein plugin install lein-clojars "0.6.0"

See the plugin task's help for more information.

    $ lein plugin help

## FAQ

**Q:** How do you pronounce Leiningen?  
**A:** It's LINE-ing-en. ['laɪnɪŋən]

**Q:** What does this offer over [Lancet](https://github.com/stuarthalloway/lancet)?  
**A:** Lancet is more of a library than a build tool. It doesn't predefine
   any tasks apart from what Ant itself offers, so there is nothing
   Clojure-specific in it. Leiningen builds on Lancet, but takes
   things further. In addition, it includes some Maven functionality
   for dependencies.

**Q:** But Maven is terrifying!  
**A:** That's not a question. Anyway, Leiningen only uses the dependency
   resolution parts of Maven, which are quite tame. For some other
   build-related functionality it uses Ant under the covers via Lancet.

**Q:** But Ant is terrifying!  
**A:** That's [true](http://www.defmacro.org/ramblings/lisp.html). Ant is
   an interpreter for a [procedural language with a regrettable 
   syntax](http://blogs.tedneward.com/2005/08/22/When+Do+You+Use+XML+Again.aspx).
   But if you treat it as a standard library of build-related
   functions and are able to write it with a more pleasing syntax, it's
   not bad.

**Q:** What's a group ID? How do snapshots work?  
**A:** See the
  [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
  for background.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

**Q:** It says a required artifact is missing for "super-pom". What's that?  
**A:** The Maven API that Leiningen uses refers to your project as
  "super-pom". It's just a quirk of the API. It probably means there
  is a typo in your :dependency declaration in project.clj.

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** The [deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
  explains how to set up a private repository. If you are not sharing
  them with a team you could also just [install locally.](http://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html).

**Q:** How do I write my own tasks?  
**A:** If it's a task that may be useful to more than just your
  project, you should make it into a
  [plugin](https://github.com/technomancy/leiningen/blob/stable/doc/PLUGINS.md).
  You can also include one-off tasks in your src/leiningen/ directory
  if they're not worth spinning off; the plugin guide shows how.

**Q:** I want to hack two projects in parallel, but it's annoying to switch between them.  
**A:** Use a feature called _checkout dependencies_. If you create a
  directory called <tt>checkouts</tt> in your project root and symlink
  some other project roots into it, Leiningen will allow you to hack
  on them in parallel. That means changes in the dependency will be
  visible in the main project without having to go through the whole
  install/switch-projects/deps/restart-repl cycle. Note that this is
  not a replacement for listing the project in :dependencies; it
  simply supplements that for tighter change cycles.

**Q:** Is it possible to exclude indirect dependencies?  
**A:** Yes. Some libraries, such as log4j, depend on projects that are
  not included in public repositories and unnecessary for basic
  functionality.  Projects listed as :dependencies may exclude 
  any of their dependencies by using the :exclusions key. See
  sample.project.clj for details.

**Q:** What does java.lang.NoSuchMethodError: clojure.lang.RestFn.<init>(I)V mean?  
**A:** It means you have some code that was AOT (ahead-of-time)
  compiled with a different version of Clojure than the one you're
  currently using. If it persists after running <tt>lein clean</tt> then it
  is a problem with your dependencies. Note that for
  your own project that AOT compilation in Clojure is much less
  important than it is in other languages. There are a few
  language-level features that must be AOT-compiled to work, generally
  for Java interop. If you are not using any of these features, you
  should not AOT-compile your project if other projects may depend
  upon it.

**Q:** I'm behind an HTTP proxy; how can I fetch my dependencies?  
**A:** Currently you need to configure the underlying Maven library by
  creating <tt>~/.m2/settings.xml</tt> as explained in the
  [Maven guide](http://maven.apache.org/guides/mini/guide-proxies.html).

**Q:** What can be done to speed up launch?  
**A:** The main delay involved in Leiningen comes from starting the
  JVM.  Launching "lein interactive" will give you an interactive
  session so you can run many tasks against the same process instead
  of launching a new one every time. Depending on your editor you may
  also be able to take advantage of its Clojure integration. (See
  [swank-clojure](http://github.com/technomancy/swank-clojure) or
  [VimClojure](https://bitbucket.org/kotarak/vimclojure), for example.)

**Q:** Still too slow; what else can make startup faster?  
**A:** There are two flavours of Hotspot (Oracle/OpenJDK's JVM),
  client and server. The server is optimized for long-running
  processes and has quite a poor startup time. Leiningen will try to
  launch a client JVM, but this only works on 32-bit Hotspot. If you
  are on a 64-bit machine you can still use a client JVM if you
  install 32-bit packages; on Debian try ia32-sun-java6-bin. Once
  you've installed it, run <tt>sudo update-java-alternatives -s
  ia32-java-6-sun</tt>.
  
**Q:** I don't have access to stdin inside my project.  
**A:** There's a bug in the Ant library that Leiningen uses to spawn
  new processes that blocks access to console input. This means that
  functions like <tt>read-line</tt> will not work as expected in most
  contexts, though the <tt>repl</tt> task necessarily includes a
  workaround. You can also use the <tt>trampoline</tt> task to
  launch your project's JVM after Leiningen's has exited rather than
  launching it as a subprocess

## Contributing

Please report issues on the [Github issue
tracker](https://github.com/technomancy/leiningen/issues) or the
[mailing list](http://groups.google.com/group/leiningen). Personal
email addresses are not appropriate for bug reports. See the file
HACKING.md for more details on how Leiningen's codebase is structured.

Patches are preferred as Github pull requests, though patches from
<tt>git format-patch</tt> are also welcome on the mailing list. Please
use topic branches when sending pull requests rather than committing
directly to master in order to minimize unnecessary merge commit
clutter.

Contributors who have had a single patch accepted may request commit
rights on the mailing list or in IRC. Please be careful with the
master branch and keep any potentially-destabilizing work on topic
branches. Other contributors will usually be glad to review topic
branches before merging if you ask on IRC or the mailing list.

Contributors are also welcome to request a free
[Leiningen sticker](http://twitpic.com/2e33r1) by asking on the
mailing list and mailing a SASE.

## Building

You don't need to "build" Leiningen per se, but when you're using a
checkout you will need to get its dependencies in place. In most cases
a <tt>lein self-install</tt> will usually get you what you
need. However, this will occasionally fail for very new SNAPSHOT
versions since the standalone jar will not have been uploaded yet. 

Alternatively if you have a copy of an older Leiningen version around
(at least 1.1.0, installed as lein-stable, for example), then you can
run "lein-stable deps" in your checkout. If Leiningen's dependencies
change it will be necessary to remove the lib/ directory entirely
before running "lein deps" again. (This is not necessary for most
projects, but Leiningen has unique bootstrapping issues when working
on itself.)

You can also use Maven, just for variety's sake:

    $ mvn dependency:copy-dependencies
    $ mv target/dependency lib

Symlink bin/lein from your checkout into a location on the $PATH. The
script can figure out when it's being called from inside a checkout
and use the checkout rather than the self-install uberjar if necessary.

## License

Source Copyright © 2009-2011 Phil Hagelberg, Alex Osborne, Dan Larkin, and
[other contributors](https://www.ohloh.net/p/leiningen/contributors). 
Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.

Thanks to Stuart Halloway for Lancet and Tim Dysinger for convincing
me that good builds are important.

Images Copyright © 2010 Phil Hagelberg. Distributed under the Creative
Commons Attribution + ShareAlike
License. [Full-size version](https://github.com/downloads/technomancy/leiningen/leiningen-full.jpg)
available.
