# Leiningen Plugins

Leiningen tasks are simply functions named $TASK in a leiningen.$TASK
namespace. So writing a Leiningen plugin is just a matter of creating
a project that contains such a function.

Using the plugin is a matter of declaring it in the `:plugins` entry
of the project map. If a plugin is a matter of user convenience rather
than a requirement for running the project, you should place the
plugin declaration in the `:user` profile in `~/.lein/profiles.clj`
instead of directly in the `project.clj` file.

## Writing a Plugin

Start by generating a new project with `lein new plugin
lein-myplugin`, and add a `leiningen.myplugin` namespace with a
`myplugin` defn. You'll notice the `project.clj` file has
`:eval-in-leiningen true`, which causes all tasks to operate inside
the leiningen process rather than starting a subprocess to isolate the
project's code. Plugins should not declare a dependency on Clojure
itself; in fact
[all of Leiningen's own dependencies](https://github.com/technomancy/leiningen/blob/master/project.clj)
should be considered implied dependencies of every plugin.

See the `lein-pprint` directory
[in the Leiningen source](https://github.com/technomancy/leiningen/tree/master/lein-pprinc)
for a sample of a very simple plugin.

The first argument to your task function should be the current
project. It will be a map which is based on the `project.clj` file,
but it also has :name, :group, :version, and :root keys added in,
among other things. To see what project maps look like, try using the
`lein-pprint` plugin; then you can run `lein pprint` to examine any
project. If you want it to take parameters from the command-line
invocation, you can make the function take more arguments.

Most tasks may only be run in the context of another project. If your
task can be run outside a project directory, add `^:no-project-needed`
metadata to your task defn to indicate it. If you are inside a
project, Leiningen should change to the root of that project before
launching the JVM, so `(System/getProperty "user.dir")` should be the
project root.

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

TODO: document subtasks and subtask help

If your task returns an integer, it will be used as the exit code for
the process. If tasks are chained together, a nonzero integer return
value will halt the chain and exit immediately. Throwing an exception
will also halt execution, but returning an integer will avoid showing
an unsightly stack trace.

## Code Evaluation

Plugin functions run inside Leiningen's process, so they have access
to all the existing Leiningen functions. The public API of Leiningen
should be considered all public functions inside the
`leiningen.core.*` prefix not labeled with `^:internal` metadata as
well as each individual task functions. Other functions in task
namespaces should be considered internal and may change inside point
releases.

Many tasks need to execute code inside the context of the project
itself. The `leiningen.core.eval/eval-in-project` function is used for
this purpose. It accepts a project argument as well as a form to
evaluate, and the final (optional) argument is another form called
`init` that is evaluated up-front before the main form. This may be
used to require a namespace earlier in order to avoid the
[Gilardi Scenario](http://technomancy.us/143). Inside the
`eval-in-project` call the project's own classpath will be active and
Leiningen's own internals and plugins will not be available.

TODO: mention associng :deps into the project that are only relevant
for that task.

The return value of the `eval-in-project` call is an integer that
represents the exit code of the project's process. Zero indicates
success.

## Hooks

You can modify the behaviour of built-in tasks to a degree using
hooks. Hook functionality is provided by the [Robert
Hooke](https://github.com/technomancy/robert-hooke) library. This is an
implied dependency; as long as Leiningen 1.2 or higher is used it will
be available.

Inspired by clojure.test's fixtures functionality, hooks are functions
which wrap other functions (often tasks) and may alter their behaviour
by using binding, altering the return value, only running the function
conditionally, etc. The `add-hook` function takes a var of the task it's
meant to apply to and a function to perform the wrapping:

```clj
(require 'robert.hooke)

(defn skip-integration-hook [task & args]
  (binding [clojure.test/test-var (test-var-skip :integration)]
    (apply task args)))

(robert.hooke/add-hook #'leiningen.test/test skip-integration-hook)
```

Hooks compose, so be aware that your hook may be running inside
another hook. To take advantage of your hooks functionality, projects
must set the `:hooks` key in project.clj to a seq of namespaces to load
that call add-hook.

If you need to use hooks from code that runs inside the project's
process, you may use `leiningen.core.injected/add-hook`, which is an
isolated copy of `robert.hooke/add-hook` injected into the project in
order to support features like test selectors.

See [the documentation for
Hooke](https://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details.

## Clojure Version

Leiningen 2.0 uses Clojure 1.3.0. If you need to use a different
version of Clojure from within a Leiningen plugin, you can use
`eval-in-project` with a dummy project argument:

```clj
(eval-in-project {:local-repo-classpath true
                  :dependencies '[[org.clojure/clojure "1.3.0"]] 
                  :native-path "/tmp" :root "/tmp" :compile-path "/tmp"}
                 '(println "hello from" *clojure-version*))
```

## 1.x Compatibility

Earlier versions of Leiningen had a few differences in the way plugins
worked, but compatibility can usually be achieved with a few
guidelines:

TODO: compatibility guidelines

## Have Fun

Please add your plugins to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins).

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
