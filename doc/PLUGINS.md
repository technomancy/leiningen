# Leiningen Plugins

Leiningen tasks are simply functions named $TASK in a leiningen.$TASK
namespace. So writing a Leiningen plugin is just a matter of creating
a project that contains such a function.

Using the plugin is a matter of declaring it in the `:plugins` entry
of the project map. If a plugin is a matter of user convenience rather
than a requirement for running the project, users should place the
plugin declaration in the `:user` profile in `~/.lein/profiles.clj`
instead of directly in the `project.clj` file.

## Writing a Plugin

Start by generating a new project with `lein new plugin
lein-myplugin`, and edit the `myplugin` defn in the
`leiningen.myplugin` namespace. You'll notice the `project.clj` file
has `:eval-in-leiningen true`, which causes all tasks to operate
inside the leiningen process rather than starting a subprocess to
isolate the project's code. Plugins need not declare a dependency on
Clojure itself; in fact
[all of Leiningen's own dependencies](https://github.com/technomancy/leiningen/blob/master/project.clj)
will be available. However, it doesn't hurt to be specific since
Leiningen's other dependencies may change in future versions.

See the `lein-pprint` directory
[in the Leiningen source](https://github.com/technomancy/leiningen/tree/master/lein-pprint)
for a sample of a very simple plugin.

During plugin development, having to re-run `lein install` in your
plugin project and then switch to a test project can be very
cumbersome. You can avoid this annoyance by creating a
`.lein-classpath` file in your test project containing the path to the
`src` directory of your plugin.

### Task Arguments

The first argument to your task function should be the current
project. It will be a map which is based on the `project.clj` file,
but it also has `:name`, `:group`, `:version`, and `:root` keys added
in, among other things. To see what project maps look like, try using
the `lein-pprint` plugin; you can invoke the `pprint` task to examine
any project.

If you want your task to take parameters from the command-line
invocation, you can make the function take more than one argument. In
order to underscore the fact that tasks are just Clojure functions,
arguments which act as flags are usually accepted as `:keywords`
rather than traditional `--dashed` syntax. Note that all arguments are
passed in as strings; it's up to your function to call `read-string`
on the arguments if you want keywords, symbols, integers, etc. Keep
this in mind when calling other tasks as functions too.

Most tasks may only be run in the context of another project. If your
task can be run outside a project directory, add `^:no-project-needed`
metadata to your task defn to indicate so. Your task should still
accept a project as its first argument, but it will be allowed to be
nil if it's run outside a project directory. If you are inside a
project, Leiningen should change to the root of that project before
launching the JVM, but some other tools using the `leiningen-core`
library may not behave the same way, so for greatest portability check
the `:root` key of the project map and work from there.

### Documentation

The `lein help` task uses docstrings. A namespace-level docstring will
be used as the short summary if present; if not then it will take the
first line of your function's docstring. Try to keep the summary under
68 characters for formatting purposes. The full docstring can of
course be much longer but should still be wrapped at 80 columns. The
function's arglists will also be shown, so pick argument names that
are clear and descriptive. If you set `:help-arglists` in the
function's metadata, it will be used instead for those cases where
alternate arities exist that aren't intended to be exposed to the
user. Be sure to explain all these arguments in the docstring. Note
that all your arguments will be strings, so it's up to you to call
`read-string` on them if you want keywords, numbers, or symbols.

Often more complicated tasks get divided up into subtasks. Placing
`:subtasks` metadata on a task defn which contains a vector of subtask
vars will allow `lein help $TASK_CONTAINING_SUBTASKS` to list them.

## Code Evaluation

Plugin functions run inside Leiningen's process, so they have access
to all the existing Leiningen functions. The public API of Leiningen
should be considered all public functions inside the
`leiningen.core.*` namespaces not labeled with `^:internal` metadata
as well as each individual task functions. Other non-task functions in
task namespaces should be considered internal and may change inside
point releases.

Many tasks need to execute code inside the context of the project
itself. The `leiningen.core.eval/eval-in-project` function is used for
this purpose. It accepts a project argument as well as a form to
evaluate, and the final (optional) argument is another form called
`init` that is evaluated up-front before the main form. This may be
used to require a namespace earlier in order to avoid the
[Gilardi Scenario](http://technomancy.us/143). 

Inside the `eval-in-project` call the project's own classpath will be
active and Leiningen's own internals and plugins will not be
available. However, it's easy to update the project map
that's passed to `eval-in-project` to add in the dependencies you
need. For example, this is done in the `lein-swank` plugin like so:

```clj
(defn swank
  "Launch swank server for Emacs to connect. Optionally takes PORT and HOST."
  ([project port host & opts]
     (eval-in-project (update-in project [:dependencies] 
                                 conj ['swank-clojure "1.4.0"])
                      (swank-form project port host opts))))
```

TODO: switch to profiles for this

The code in the `swank-clojure` dependency is needed inside the
project, so it's `conj`ed into the `:dependencies`.

Before `eval-in-project` is invoked, Leiningen must "prep" a project,
usually by ensuring that all Java code and all necessary Clojure code
has been AOT compiled to bytecode. This is done by running all the
tasks in the `:prep-tasks` key of the project, which defaults to
`["javac" "compile"]`. If your plugin requires another kind of
prepping, (for instance, compiling protocol buffers) you can instruct
users to add another entry to `:prep-tasks`. Note that this task will
be invoked for **every** `eval-in-project`, so take care that it runs
quickly if nothing has changed since the last run.

## Hooks

You can modify the behaviour of built-in tasks to a degree using
hooks. Hook functionality is provided by the [Robert
Hooke](https://github.com/technomancy/robert-hooke) library. This is an
implied dependency; as long as Leiningen 1.2 or higher is used it will
be available.

Inspired by clojure.test's fixtures functionality, hooks are functions
which wrap other functions (often tasks) and may alter their behaviour
by binding other vars, altering the return value, only running the function
conditionally, etc. The `add-hook` function takes a var of the task it's
meant to apply to and a function to perform the wrapping:

```clj
(ns leiningen.hooks.integration
  (:require [robert.hooke]
            [leiningen.test]))

(defn add-test-var-println [f & args]
  `(binding [~'clojure.test/assert-expr
             (fn [msg# form#]
               (println "Asserting" form#)
               ((.getRawRoot #'clojure.test/assert-expr) msg# form#))]
     ~(apply f args)))

;; Place the body of the activate function at the top-level for
;; compatibility with Leiningen 1.x
(defn activate []
  (robert.hooke/add-hook #'leiningen.test/form-for-testing-namespaces
                         add-test-var-println))
```

Hooks compose, so be aware that your hook may be running inside
another hook. To take advantage of your hooks functionality, projects
must set the `:hooks` key in project.clj to a seq of namespaces to load
that call `add-hook`. You may place calls to `add-hook` at the
top-level of the namespace, but if an `activate` defn is present it
will be called; this is the best place to put `add-hook` invocations.

See [the documentation for
Hooke](https://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details.

## Clojure Version

Leiningen 2.0.0 uses Clojure 1.4.0. If you need to use a different
version of Clojure from within a Leiningen plugin, you can use
`eval-in-project` with a dummy project argument:

```clj
(eval-in-project {:dependencies '[[org.clojure/clojure "1.5.0-alpha"]]}
                 '(println "hello from" *clojure-version*))
```

In Leiningen 1.x, plugins had access to monolithic Clojure Contrib.
This is no longer true in 2.x.

## Upgrading Existing Plugins

Earlier versions of Leiningen had a few differences in the way plugins
worked, but upgrading shouldn't be too difficult.

The biggest difference between 1.x and 2.x is that `:dev-dependencies`
have been done away with. There are no longer any dependencies that
exist both in Leiningen's process and the project's process; Leiningen
only sees `:plugins` and the project only sees `:dependencies`, though
both these maps can be affected by the currently-active profiles.

If your project doesn't need to use `eval-in-project` at all, it
should be relatively easy to port; it's just a matter of updating any
references to Leiningen functions which may have moved. All
`leiningen.utils.*` namespaces have gone away, and `leiningen.core`
has become `leiningen.core.main`. For a more thorough overview see the
[published documentation on leiningen-core](http://technomancy.github.com/leiningen/).

Plugins that do use `eval-in-project` should just be aware that the
plugin's own dependencies and source will not be available to the
project. If your plugin currently has code that needs to run in both
contexts it must be split into multiple projects, one for `:plugins`
and one for `:dependencies`. See the example of `lein-swank` above to
see how to inject `:dependencies` in `eval-in-project` calls.

If your plugin may run outside the context of the project entirely,
you should still leave room in the arguments list for a project map;
just expect that it will be nil if there's no project present. Use
`^:no-project-needed` metadata to indicate this is acceptable.

In Leiningen 1.x, having a task function return a numeric value was a
way to signal the process's exit code. In Leiningen 2.x, tasks should
call the `leiningen.core.main/abort` function when a fatal error is
encountered. If the `leiningen.core.main/*exit-process?*` var is bound
to true, then this will trigger an exit, but in some contexts (like
`with-profiles`) it will simply trigger an exception and go on to the
next task.

## 1.x Compatibility

Once you've identified the changes necessary to achieve compatibility
with 2.x, you can decide whether you'd like to support 1.x and 2.x in
the same codebase. In some cases it may be easier to simply keep them
in separate branches, but sometimes it's better to support both.
Luckily the strategy of using `:plugins` and adding in `:dependencies`
just for calls to `eval-in-project` works fine in Leiningen 1.7.

If you use functions that moved in 2.x, you can try requiring and
resolving at runtime rather than compile time and falling back to the
1.x versions of the function if it's not found. Again the `lein-swank`
plugin provides an example of a compatibility shim:

```clj
(defn eval-in-project
  "Support eval-in-project in both Leiningen 1.x and 2.x."
  [project form init]
  (let [[eip two?] (or (try (require 'leiningen.core.eval)
                            [(resolve 'leiningen.core.eval/eval-in-project)
                             true]
                            (catch java.io.FileNotFoundException _))
                       (try (require 'leiningen.compile)
                            [(resolve 'leiningen.compile/eval-in-project)]
                            (catch java.io.FileNotFoundException _)))]
    (if two?
      (eip project form init)
      (eip project form nil nil init))))
```

Of course if the function has changed arities or has disappeared
entirely this may not be feasible, but it should suffice in most
cases.

Most widely-used functions which have changed in 2.x can be used from
the [leinjacker](https://github.com/sattvik/leinjacker) project, which
provides a compatibility shim supporting both 1.x and 2.x.

Another key change is that `:source-path`, `:resources-path`,
`:java-source-path`, and `:test-path` have changed to
`:sources-paths`, `:resource-paths`, `:java-source-paths`, and
`:test-paths`, and they should be vectors now instead of single
strings. The old `:dev-resources` key is now just another entry to the
`:resource-paths` vector that's only present when the `:dev` profile
is active.

Allowing the task to run outside a project directory is tricky to do
in a backwards-compatible way since 1.x is overly-clever and actually
inspects your argument list to figure out if it should pass in a
project argument, while 2.x simply always passes it in and just allows
it to be nil if it's not present. You can try checking the first
argument to see if it's a project map, but if you have more than two
arities this can get very tricky; it may just be better to maintain
separate branches of your codebase in this situation.

## Templates

You can also publish templates for generating project skeletons that
work with `lein new`. See
[the documentation for the new task](https://github.com/Raynes/lein-newnew/)
for details on how to build templates.

## Have Fun

Please add your plugin to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins) once it's ready.

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
