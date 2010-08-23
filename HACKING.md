# Hacking Leiningen

Leiningen is very small. The latest release is only around a thousand
lines of Clojure; you could probably read through the whole project in
an hour.

TODO: what goes where? a tour through the launching of a task

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

TODO: integrate with plugin guide

# Release Checklist

* update NEWS, bin/lein, project.clj, pom
* rm -rf lib, generate uberjar, upload
* test self-install
* git tag
* push, push tags, update stable branch
* announce on mailing list
* bump version numbers (bin/lein and project.clj)
* regenerate pom.xml
