# Leiningen Plugins

Leiningen tasks are simply functions named $TASK in a leiningen.$TASK
namespace. So writing a Leiningen plugin is just a matter of creating
a project that contains such a function, but much of this
documentation applies equally to the tasks that ship with Leiningen
itself.

Using the plugin is a matter of declaring it in the `:plugins` entry
of the project map. If a plugin is a matter of user convenience rather
than a requirement for running the project, users should place the
plugin declaration in the `:user` profile in `~/.lein/profiles.clj`
instead of directly in the `project.clj` file.

## Writing a Plugin

Start by generating a new project with `lein new plugin
myplugin`, and edit the `myplugin` defn in the
`leiningen.myplugin` namespace. You'll notice the `project.clj` file
has `:eval-in-leiningen true`, which causes all tasks to operate
inside the leiningen process rather than starting a subprocess to
isolate the project's code. Plugins need not declare a dependency on
Clojure itself; in fact
[all of Leiningen's own dependencies](https://github.com/technomancy/leiningen/blob/stable/project.clj)
will be available. However, it doesn't hurt to be specific since
Leiningen's other dependencies may change in future versions.

See the `lein-pprint` directory
[in the Leiningen source](https://github.com/technomancy/leiningen/tree/stable/lein-pprint)
for a sample of a very simple plugin.

During plugin development, having to re-run `lein install` in your
plugin project and then switch to a test project can be very
cumbersome. Once you've installed the plugin once, you can avoid this
annoyance by creating a `.lein-classpath` file in your test project
containing the path to the `src` directory of your plugin.

### Task Arguments

The first argument to your task function should be the current
project. It will be a map which is based on the `project.clj` file,
but it also has `:name`, `:group`, `:version`, and `:root` keys added
in, among other things. Try using the `lein-pprint` plugin to see what
project maps look like; you can invoke the `pprint` task to examine
any project or combination of profiles.

If you want your task to take parameters from the command-line
invocation, you can make the function take more than one argument. In
order to underscore the fact that tasks are just Clojure functions,
arguments which act as flags are usually accepted as `:keywords`
rather than traditional `--dashed` syntax. Note that all arguments are
still passed in as strings; it's up to your function to call `read-string`
on the arguments if you want keywords, symbols, integers, etc. Keep
this in mind when calling other tasks as functions too.

Most tasks may only be run in the context of another project. If your
task can be run outside a project directory, add `^:no-project-needed`
metadata to your task defn to indicate so. Your task should still
accept a project as its first argument, but it will be allowed to be
nil if it's run outside a project directory. If you are inside a
project, Leiningen should `cd` to the root of that project before
launching the JVM, but some other tools using the `leiningen-core`
library (IDE integration, etc) may not behave the same way, so for
greatest portability check the `:root` key of the project map and work
from there.

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
This list of subtasks will show the first line of the docstring for each
subtask. The full help for a subtask can be viewed via 
`lein help $TASK_CONTAINING_SUBTASKS $SUBTASK`. 

Leiningen will intercept calls to `lein $MYTASK help` by default and
turn it into `lein help $MYTASK`. If your task provides its own help
subtask you can add `^:pass-through-help` metadata to your task defn
to opt-out of this behaviour.

## Code Evaluation

Plugin functions run inside Leiningen's process, so they have access
to all the existing Leiningen functions. The public API of Leiningen
should be considered all public functions inside the
`leiningen.core.*` namespaces not labeled with `^:internal` metadata
as well as each individual task functions. Other non-task functions in
task namespaces should be considered internal and may change inside
point releases.

### Evaluating In Project Context

Many tasks need to execute code inside the context of the project
itself. The `leiningen.core.eval/eval-in-project` function is used for
this purpose. It accepts a project argument as well as a form to
evaluate, and the final (optional) argument is another form called
`init` that is evaluated up-front before the main form. This may be
used to require a namespace earlier in order to avoid the
[Gilardi Scenario](http://technomancy.us/143).

Inside the `eval-in-project` call the project's own classpath will be
active and Leiningen's own internals and plugins will not be
available.

You can modify the project map before you pass it into `eval-in-project`.
However, it's recommended that you make your modifications by merging a
profile in so users can override your changes if necessary. Use
`leiningen.core.project/merge-profiles` to make your changes:

```clj
(def swank-profile {:dependencies [['swank-clojure "1.4.3"]]})

(defn swank
  "Launch swank server for Emacs to connect. Optionally takes PORT and HOST."
  [project port host & opts]
    (let [profile (or (:swank (:profiles project)) swank-profile)
          project (project/merge-profiles project [profile])]
      (eval-in-project project 
                       `(swank.core/-main ~@opts)
                       '(require 'swank.core))))
```

The code in the `swank-clojure` dependency is needed inside the
project, so it's declared in its own profile map and merged
in. However, we defer to the `:swank` profile in the project map if
it's present so that the user can pick their own version of the
dependency if they like rather than relying on the hard-coded profile
in the plugin.

Note that the snippet above is not a good example of a plugin since it
simply wraps `eval-in-project` and `merge-profiles`. If that is all
you want, you can do it without implementing a plugin; just define an
alias that uses the `with-profiles` and `run` tasks to call the
function you need.

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
hooks. Hook functionality is provided by the
[Robert Hooke](https://github.com/technomancy/robert-hooke) library,
which is included with Leiningen.

Inspired by clojure.test's fixtures functionality, hooks are functions
which wrap other functions (often tasks) and may alter their behaviour
by binding other vars, altering the return value, only running the function
conditionally, etc. The `add-hook` function takes a var of the task it's
meant to apply to and a function to perform the wrapping:

```clj
(ns lein-integration.plugin
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
                         #'add-test-var-println))
```

Hooks compose, so be aware that your hook may be running inside
another hook. See
[the documentation for Hooke](https://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details. Note that calls to `add-hook` should use the var for
both the first and second argument so that hooks can be loaded
repeatedly without re-adding the hook. This is because in Clojure
bare functions cannot be compared for equality, but vars can.

If you want your hooks to be loaded automatically when other projects
include your plugin, activate them in a function called
`plugin-name.plugin/hooks`. So in the example above the plugin is
called `lein-integration`, and the function
`lein-integration.plugin/hooks` is automatically called to activate
hooks when the `lein-integration` plugin is loaded.

Hooks can also be loaded manually by setting the `:hooks` key in project.clj to
a seq of vars to call to activate your hooks. For backward compatibility, you
can also specify namespaces instead of vars in `:hooks`, and the `activate`
function in that namespace will be called. Note: automatic hooks are activated
before manually specified hooks.

## Project Middleware

Project middleware is just a function that is called on a project map
returning a new project map. Middleware gives a plugin the power to do
almost anything.  For example, a plugin could use middleware to
reimplement Leiningen's profiles functionality.

The following middleware injects the contents of project map into your
project's resources folder so it can be read from the project code:

```clj
(ns lein-inject.plugin)

(defn middleware [project]
  (update-in project [:injections] concat
             `[(spit "resources/project.clj" ~(prn-str project))]))
```


Like hooks, middleware will be applied automatically for plugins if you put it
in `plugin-name.plugin/middleware`. You can also load middleware manually by
setting the `:middleware` key in project.clj to a seq of vars to call to
transform your project map. Note that automatic middleware is applied before
manually specified middleware.

Also note that the currently active middleware depends on which
profiles are active. This means we need to reapply the middleware
functions to the project map whenever the active profiles change. We
accomplish this by storing the fresh project map and starting from
that whenever we call `merge-profiles`, `unmerge-profiles` or
`set-profiles`. It also means your middleware functions shouldn't have
any non-idempotent side-effects since they could be called repeatedly.

## Maven Wagons

[Pomegranate](https://github.com/cemerick/pomegranate) (the library
used by Leiningen to resolve dependencies) supports registering
"wagon" factories. Wagons are used to handle non-standard transport
protocols for repositories, and are looked up based on the protocol of
the repository url. If your plugin needs to register a wagon factory,
it can do so by providing a `leiningen/wagons.clj` file containing a
map of protocols to functions that return wagon instances for the
protocol. For example, the following `wagons.clj` will register a
wagon factory function for `dav:` urls:

```clj
{"dav" #(org.apache.maven.wagon.providers.webdav.WebDavWagon.)}
```

See [S3 wagon private](https://github.com/technomancy/s3-wagon-private) or
[lein-webdav](https://github.com/tobias/lein-webdav) for full examples of
plugins using this technique.

## VCS Methods

Leiningen ships with a `vcs` task which performs a handful of
release-related version control tasks via multimethods. Out of the box
it contains implementations for Git, but plugins can add support for
more systems by including a `leiningen.vcs.$SYSTEM` namespace. All
namespaces under the `leiningen.vcs.` prefix will be loaded when the
`vcs` task is invoked. These namespaces should simply define methods
for the `defmulti`s in `leiningen.vcs` that invoke the specific
version control system.

## Requiring Plugins

To use a plugin in your project, just add a `:plugins` key to your project.clj
with the same format as `:dependencies`. In addition to the options allowed by
`:dependencies`, `:plugins` also allows you to disable auto-loading of hooks or
middleware.

```clj
(defproject foo "0.1.0"
  :plugins [[lein-pprint "1.1.1"]
            [lein-foo "0.0.1" :hooks false]
            [lein-bar "0.0.1" :middleware false]])
```

## Clojure Version

Leiningen 2.3.4 uses Clojure 1.5.1. If you need to use a different
version of Clojure from within a Leiningen plugin, you can use
`eval-in-project` with a dummy project argument:

```clj
(eval-in-project {:dependencies '[[org.clojure/clojure "1.4.0"]]}
                 '(println "hello from" *clojure-version*))
```

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

## Projects vs Standalone Execution

Some Leiningen tasks can be executed from any directory (e.g. `lein repl`).
Some only make sense in the context of a project.

To check whether Leiningen is running in the context of a project
(that is, if a `project.clj` is present in the current directory),
check for the `:root` key in the project map:

``` clojure
(if (:root project)
  (comment "Running in a project directory")
  (comment "Running standalone"))
```

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
`:source-paths`, `:resource-paths`, `:java-source-paths`, and
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

### Project-specific Tasks

Occasionally, the need arises for a task to be included in a project's
codebase. However, this is much less common than people think. If you
simply have some code that needs to be invoked from the command-line
via `lein foo`, it's much simpler to have your code run inside your
project and alias `foo` to `run -m myproject.foo`:

```clj
:aliases {"foo" ["run" "-m" "myproject.foo"]}
```

You only need to write a Leiningen task if you need to operate outside
the context of your project, for instance if you need to adjust the
project map before calling `eval-in-project` or some other task where
you need direct access to Leiningen internals. The vast majority of
these cases are already covered by
[existing plugins](http://wiki.github.com/technomancy/leiningen/plugins),
but if you have a case that doesn't exist and for some reason can't
spin it off into its own separate plugin, you can enable this behavior
by placing the `foo.clj` file defining the new task in
`tasks/leiningen/` and add `tasks` to your `.lein-classpath`:

```
$ ls
README.md project.clj src tasks test
$ ls -R tasks
leiningen

tasks/leiningen:
foo.clj
$ echo -ne ":tasks" | cat >> .lein-classpath
$ lein foo
Hello, Foo!
```

Note that in most cases it's better to spin off tasks into their own
plugin projects; using `.lein-classpath` is mainly appropriate for
experimentation or cases when there isn't enough time to create a
proper plugin.

## Have Fun

Please add your plugin to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins) once it's ready.

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
