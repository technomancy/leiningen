# Leiningen Plugins

Leiningen tasks are simply functions in a leiningen.task namespace. So
writing a Leiningen plugin is pretty straightforward; as long as it's
available on the classpath, Leiningen will be able to use it.

To use a plugin, add it to your project.clj :dev-dependencies and run
"lein deps". Then you'll be able to invoke it with "lein myplugin".

## Writing a Plugin

Start by generating a new project with "lein new myplugin", and add a
leiningen.myplugin namespace with a myplugin function. That function
should take at least one argument: the current project. The project is
a map which is based on the project.clj file, but it also has :name,
:group, :version, and :root keys added in. If you want it to take
parameters from the command-line invocation, you can make it take more
arguments.

Note that Leiningen is an implied dependency of all plugins; you don't
need to explicitly list it in the project.clj file.

The docstring from the plugin's namespace will be displayed by the
"lein help" task. The function's arglists will also be shown, so pick
argument names that are clear and descriptive.

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

## Leiningen 1.2

The rest of this document only applies to Leiningen version 1.2+. It
is subject to breaking change until 1.2.0 sees a stable release.

If your task returns an integer, it will be used as the exit code for
the process.

You can set up aliases for your task by conjing a pair of strings with
alias->task-name mappings on to the leiningen.core/aliases atom:

    (swap! leiningen.core/aliases conj ["-v" "version"])

## Hooks

You can modify the behaviour of built-in tasks to a degree using
hooks. Hook functionality is provided by [Robert
Hooke](http://github.com/technomancy/robert-hooke), a separate
library. Your plugin will have to declare it as a dependency in
project.clj if you want to use hooks:

    :dev-dependencies [[robert/hooke "1.0.1"]]

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
another hook. Hooks are loaded by looking for all namespaces under
leiningen.hooks.* on the classpath and loading them in alphabetical
order.

See [the documentation for
Hooke](http://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details.

## Have Fun

Please add your plugins to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins).

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
