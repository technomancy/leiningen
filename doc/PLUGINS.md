<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Leiningen Plugins](#leiningen-plugins)
  - [Writing a Plugin](#writing-a-plugin)
    - [Local development](#local-development)
    - [Task Arguments](#task-arguments)
    - [Documentation and subtasks](#documentation-and-subtasks)
  - [Code Evaluation](#code-evaluation)
    - [Evaluating In Project Context](#evaluating-in-project-context)
  - [Other Plugin Contents](#other-plugin-contents)
    - [Profiles](#profiles)
    - [Hooks](#hooks)
    - [Project Middleware](#project-middleware)
    - [Maven Wagons](#maven-wagons)
    - [VCS Methods](#vcs-methods)
  - [Requiring Plugins](#requiring-plugins)
  - [Clojure Version](#clojure-version)
  - [Upgrading Existing Plugins](#upgrading-existing-plugins)
  - [Projects vs Standalone Execution](#projects-vs-standalone-execution)
  - [Overriding Built-in Tasks](#overriding-built-in-tasks)
  - [1.x Compatibility](#1x-compatibility)
    - [Project-specific Tasks](#project-specific-tasks)
  - [Have Fun](#have-fun)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

[Japanese](ja/PLUGINS_ja.md)

# Leiningen Plugins

Leiningen tasks are simply functions named `$TASK` in a `leiningen.$TASK`
namespace. So writing a Leiningen plugin is just a matter of creating
a project that contains such a function, but much of this
documentation applies equally to the tasks that ship with Leiningen
itself.

Using the plugin is a matter of declaring it in the `:plugins` entry
of the project map. If a plugin is a matter of user convenience rather
than a requirement for running the project, users should place the
plugin declaration in the `:user` profile in `~/.lein/profiles.clj`
instead of directly in the `project.clj` file.

## Not Writing a Plugin (无为)

The first thing to do when writing a plugin is to try to accomplish
what you're doing without a plugin.

Early on in the days of Leiningen many plugins were written which did
nothing but provide a short command to run a specific function using
`eval-in-project`. Once Leiningen added the support for
partially-applied aliases these became largely redundant, because you
can add an alias to the `run` task:

    :aliases {"mytest" ["run" "-m" "mylib.test/go"]}

Not only does this allow `lein mytest` to run the `mylib.test/go`
function inside the context of your project, it also passes additional
arguments (such as in the case of `lein run mytest :integration`) on
to the function specified. However, for some plugins this wasn't
enough as they needed access to values in the project map. For
instance, a test runner would need to know the value of `:test-paths`
to know which directory to scan for tests.

As of Leiningen 2.4.0 it's possible to get this data from an alias,
removing the need for a plugin.

    :aliases {"mytest" ["run" "-m" "mylib.test/go" :project/test-paths]}

This will load the `:test-paths` value from the project map and pass a
string representation of it as the first argument to the function
specified in the alias, followed by any command-line arguments given
to the `mytest` alias. It's up to the function to call `read-string`
on that argument.

However, if you need to call other Leiningen functions or have no need
to run anything inside the context of the project's own process,
making a plugin might be the right choice if one doesn't [exist already](https://codeberg.org/leiningen/leiningen/wiki/plugins),

## Writing a Plugin

Start by generating a new project with `lein new plugin
myplugin`, and edit the `myplugin` defn in the
`leiningen.myplugin` namespace. You'll notice the `project.clj` file
has `:eval-in-leiningen true`, which causes all tasks to operate
inside the leiningen process rather than starting a subprocess to
isolate the project's code. Plugins need not declare a dependency on
Clojure itself; in fact
[all of Leiningen's own dependencies](https://codeberg.org/leiningen/leiningen/src/stable/project.clj)
will be available.

See the `lein-pprint` directory
[in the Leiningen source](https://codeberg.org/leiningen/leiningen/src/stable/lein-pprint)
for a sample of a very simple plugin.

When emitting output, please use `leiningen.core.main/info`,
`leiningen.core.main/warn`, and `leiningen.core.main/debug` rather than
`println` since these will respect the user's output settings.

### Local development

When you're ready to test your plugin in a separate project you can include it via the following (example a plugin named sample-plugin):

```
lein install
Created ~/sample-plugin/target/sample-plugin-0.1.0-SNAPSHOT.jar
Wrote ~/sample-plugin/pom.xml
Installed jar and pom into local repo.
```

This will build a jar using the :version listed in the plugin's project.clj file (see above for example project.clj) and install it into your local m2 repository (~/.m2/repository)

After this step completes you can now list your plugin in your separate project with the version outputted from above. This example would look like this:

```
...
:plugins [[sample-plugin "0.1.0-SNAPSHOT"]]
...
```

During local development, having to re-run `lein install` in your
plugin project and then switch to a test project can be very
cumbersome. In order to avoid this annoyance, you can do the following:

 1. If you haven't done it yet, run `lein install` in the plugin's project
    directory.
 2. Just to make sure, run `lein help <plugin-name>` in your test project
    directory. A help message for your plugin should be displayed now. Or an
    exception originating in your plugin.
 3. Add the path to the `src` directory of your plugin to the file
    `.lein-classpath` in your test project directory. Probably you'll have to
    create that file.
 4. If your plugin depends on another library that you are also working on, then
    that needs to be added to `.lein-classpath` with the classpath separator,
    either `:` for unix, or `;` for Windows. The same goes for your plugin's
    other direct dependencies. Run `lein classpath` in order to get an idea how
    the contents of `.lein-classpath` are supposed to look.
 5. Remove the entry for your plugin from the test project's `project.clj`.
    Otherwise it would override what you've added to `.lein-classpath`, because
    Leiningen loads those things before it loads plugins.

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
rather than unixy traditional `--dashed` syntax. Note that all arguments are
still passed in as strings; it's up to your function to call `read-string`
on the arguments if you want keywords, symbols, integers, etc. Keep
this in mind when calling other tasks as functions too.

Most tasks may only be run in the context of a project. If your
task can be run outside a project directory, add `^:no-project-needed`
as metadata to your task defn to indicate so. Your task must still
accept a project as its first argument, but it will be allowed to be
nil. Leiningen will still pass you the project as first argument if
lein is called from within a project. If called outside of a project,
lein will send in profile information from `$HOME/.lein/profiles.clj`
and similar sources as a map similar to a project map. Other tools using
the `leiningen-core` library (IDE integration, etc) may decide to just
pass in nil. To distinguish between a project and non-project, check for
the `:root` key. If it's set, then you are in a project, otherwise you
are not.

### Documentation and subtasks

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

Note that Leiningen doesn't have a mechanism for automatically invoking
subtasks. You'll have to do that yourself in the main task. A dumb
implementation of it all might look like this:

```clojure
(defn my-task
  "Automatically write all the project's code."
  {:subtasks [#'my-subtask-0 #'my-subtask-1]}
  [project & [sub-name]]
  (case sub-name
    "my-subtask-0" (my-subtask-0 project args)
    "my-subtask-1" (my-subtask-1 project args)
    nil            :not-implemented-yet
    (leiningen.core.main/warn "Unknown task.")))
```

Leiningen will intercept calls to `lein $MYTASK help` by default and
turn them into `lein help $MYTASK`. If your task provides its own help
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
[Gilardi Scenario](https://technomancy.us/143).

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

## Other Plugin Contents

Plugins are primarily about providing tasks, but they can also contain
profiles, hooks, middleware, wagons (dependency transport methods),
and vcs methods.

### Profiles

If there is configuration that is likely to be used by many projects
using your plugin, yet for some reason you can't make that
configuration active by default, you can include profiles inside your
plugin.

Create a file called `resources/myplugin/profiles.clj` in your plugin that
contains a map:

```clj
{:default {:x "y and z"}
 :extra {:other "settings"}}
```

Each value here is a profile that your users can merge into their
project. You can do this explicitly on a per-invocation basis using
`with-profile`:

    $ lein with-profile plugin.myplugin/extra test

Users can also have profiles activated automatically by changing the `:default` profile:

```clj
:profiles {:default [:base :system :user :provided :dev :plugin.myplugin/default]
           :other {...}}
```

Everything in the `:default` profile is active for all
non-`with-profile` task invocations except for those which produce
downstream artifacts, like `jar`, `uberjar`, and `pom`.

### Hooks

**Note**: Leiningen supports loading hooks from plugins; however this
mechanism is extremely error-prone and difficult to debug. It should
be considered deprecated as of 2.8.0 onward and will continue to work
until version 3.0 but is strongly advised against.

You can modify the behaviour of built-in Leiningen tasks to a degree
using hooks. Hook functionality is provided by the
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

### Project Middleware

Project middleware is just a function that is called on a project map
returning a new project map. Middleware gives a plugin the power to do
any kind of transformation on the project map. However, problems with
middleware can be difficult to debug due to their flexibility and
opaqueness. If you can do what you need using profiles inside your
plugins instead, that is a much more declarative, introspectable way
to do things which will save a lot of headache down the line.

Projects use middleware by adding `:middleware` as a vector of var
names into their `project.clj`:

```clj
  :middleware [leiningen.inject/middleware]
```

Also note that the currently active middleware depends on which
profiles are active. This means we need to reapply the middleware
functions to the project map whenever the active profiles change. We
accomplish this by storing the fresh project map and starting from
that whenever we call `merge-profiles`, `unmerge-profiles` or
`set-profiles`. It also means your middleware functions shouldn't have
any non-idempotent side-effects since they could be called repeatedly.

If you need to include a profile in the project map, please add it as a plugin
profile and ask your users to add it to the `:base` profile as outlined in the
"Plugin" subsection of "Other Plugin Contents" in this document. This makes the
"injection" more explicit and easier to debug. The only times one should use
middleware to inject values into the project map is if the profiles has to be
programmatically computed, or if you have to modify the project map in a way
that is not possible with `merge-profiles`.

Note that middleware application will be memoized unless the
`:memoize-middleware?` key is set to `false`.

**Note**: Leiningen supports loading middleware implicitly when the
middleware is named `plugin-name.plugin/middleware`; however this
mechanism is even more difficult to debug than regular middleware. It
is strongly advised against using.

### Maven Wagons

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

### VCS Methods

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

Leiningen 2.7.0 and on uses Clojure 1.8.0. If you need to use a
different version of Clojure from within a Leiningen plugin, you can
use `eval-in-project` with a dummy project argument:

```clj
(eval-in-project {:dependencies '[[org.clojure/clojure "1.4.0"]]}
                 '(println "hello from" *clojure-version*))
```

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

## Overriding Built-in Tasks

Normally if you create a plugin containing (say) a `leiningen.compile`
namespace, it won't be used when `lein compile` is run; the built-in
task will override it. If you'd like to shadow a built-in task, you
can either create an alias or put it in the `leiningen.plugin.compile`
namespace.

## Project-specific Tasks

Occasionally, the need arises for a task to be included in a project's
codebase. However, this is much less common than people think. If you
simply have some code that needs to be invoked from the command-line
it's much simpler to have your code run in a `-main` function inside your
project and invoke it with an alias like `lein garble`:

```clj
:aliases {"garble" ["run" "-m" "myproject.garble" "supergarble"]}
```

Note that aliases vectors result in partially applied task functions,
so with the above config, `lein garble seventeen` would be equivalent
to `lein run -m myproject.garble supergarble seventeen` (or
`(myproject.garble/-main "supergarble" "seventeen")` from the
repl). The arguments in the alias are concatenated to the arguments
provided when it's invoked.

You only need to write a Leiningen task if you need to operate outside
the context of your project, for instance if you need to adjust the
project map before calling `eval-in-project` or some other task where
you need direct access to Leiningen internals. You can even read values
from the project map with an alias:

```clj
:aliases {"garble" ["run" "-m" "myproject.garble" :project/version]}
```

This will splice the value of the project map's `:version` field into
the argument list so that the `-main` function running inside the
project code gets access to it.

The vast majority of these cases are already covered by
[existing plugins](https://codeberg.org/leiningen/leiningen/wiki/plugins),
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
wiki](https://codeberg.org/leiningen/leiningen/wiki/plugins) once it's ready.

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
