# Leiningen Plugins

Leiningen tasks are simply functions named $TASK in a leiningen.$TASK
namespace. So writing a Leiningen plugin is pretty straightforward; as
long as it's available on the classpath, Leiningen will be able to use
it.

To use a plugin, add it to your project.clj :dev-dependencies and run
"lein deps". Then you'll be able to invoke the tasks it provides with
"lein $TASK". If you want plugins to be available for all the projects
you use, you can place their jars in ~/.lein/plugins.

## Writing a Plugin

Start by generating a new project with "lein new myplugin", and add a
leiningen.myplugin namespace with a myplugin function. 

Some tasks may only be run in the context of a project. For tasks like
this, name the first argument <tt>project</tt>. Leiningen will inspect
the argument list and pass in the current project if needed. The
project is a map which is based on the project.clj file, but it also
has :name, :group, :version, and :root keys added in. If you want it
to take parameters from the command-line invocation, you can make the
function take more arguments.

Tasks without a <tt>project</tt> argument will be able to be run from
anywhere.

Note that Leiningen is an implied dependency of all plugins; you don't
need to explicitly list it in the project.clj file. You also don't
need to list Clojure or Contrib, but you will be locked into using the
same version of Clojure that Leiningen is using. So for instance, if
your plugin depends on defprotocol, then you should make it clear in
your documentation that it only works with Leiningen 1.2.0 and higher.

The docstring from the plugin's namespace will be displayed by the
"lein help" task. The function's arglists will also be shown, so pick
argument names that are clear and descriptive.

If your task returns an integer, it will be used as the exit code for
the process.

You can set up aliases for your task by conjing a pair of strings with
alias->task-name mappings on to the leiningen.core/aliases atom:

    (swap! leiningen.core/aliases conj ["-v" "version"])

## Lancet

If your plugins need to do a fair amount of filesystem-y things, you
may want to take a look at using Ant tasks to do them since the JDK
lacks a lot of simple functionality of this kind. Using the Ant API
directly is a pain, but it can be eased to a degree using
[Lancet](http://github.com/stuarthalloway/lancet). Lancet is the
Clojure adapter for Ant that is developed as the sample project in the
[Programming
Clojure](http://www.pragprog.com/titles/shcloj/programming-clojure)
book.

You can look over the [Ant API documentation's listing of
tasks](http://www.jajakarta.org/ant/ant-1.6.1/docs/en/manual/api/org/apache/tools/ant/taskdefs/package-summary.html)
to find an appropriate task. See the <tt>deps</tt> task for an example
of how to call a task from Clojure.

## Hooks

You can modify the behaviour of built-in tasks to a degree using
hooks. Hook functionality is provided by the [Robert
Hooke](http://github.com/technomancy/robert-hooke) library. This is an
implied dependency; as long as Leiningen 1.2 or higher is used it will
be available.

Inspired by clojure.test's fixtures functionality, hooks are functions
which wrap tasks and may alter their behaviour by using binding,
altering the return value, only running the function conditionally,
etc. The add-hook function takes a var of the task it's meant to apply
to and a function to perform the wrapping:

    (use 'robert.hooke)

    (defn skip-integration-hook [task & args]
      (binding [clojure.test/test-var (test-var-skip :integration)]
        (apply task args)))

    (add-hook #'leiningen.test/test skip-integration-hook)

Hooks compose, so be aware that your hook may be running inside
another hook. To take advantage of your hooks functionality, projects
must set the :hooks key in project.clj to a seq of namespaces to load
that call add-hook. Note that in Leiningen 1.2, hooks get loaded and
used without being specified in project.clj; this is a bug. In 1.3 and
on they are opt-in only.

See [the documentation for
Hooke](http://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details.

## Have Fun

Please add your plugins to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins).

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
