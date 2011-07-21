# Leiningen Plugins

Leiningen tasks are simply functions named $TASK in a leiningen.$TASK
namespace. So writing a Leiningen plugin is pretty straightforward; as
long as the file containing the namespace is available on the
classpath, Leiningen will be able to use it.

Plugins may be installed on a per-project or user-wide basis. To use a
plugin in a single project, add it to your project.clj
:dev-dependencies and run "lein deps". To install it for your user,
run "lein plugin install ARTIFACT-ID VERSION". In general user-level
plugins are preferred when the plugin is a matter of user convenience,
and :dev-dependencies are better for plugins without which the tests
or packaging will not function.

For example,
[swank-clojure](https://github.com/technomancy/swank-clojure) should
be installed with "lein plugin install" while
[lein-tar](https://github.com/technomancy/lein-tar) should be in
:dev-dependencies.

## Writing a Plugin

Start by generating a new project with "lein new myplugin", and add a
leiningen.myplugin namespace with a myplugin function. Add
:eval-in-leiningen true to your project.clj so Leiningen knows to
execute its code inside the Leiningen process rather than spinning up
a project subprocess.

Some tasks may only be run in the context of a project. For tasks like
this, name the first argument <tt>project</tt>. Leiningen will inspect
the argument list and pass in the current project if needed.

Some tasks can be run inside a project or outside, but would benefit
from having the project argument if they're run from a project. For
these, name the first argument something like <tt>project-or-foo</tt>,
and it will be passed the project argument when appropriate.

The project is a map which is based on the project.clj file, but it
also has :name, :group, :version, and :root keys added in. If you want
it to take parameters from the command-line invocation, you can make
the function take more arguments.

Note that Leiningen is an implied dependency of all plugins; you
should not explicitly list it in the project.clj file. You also don't
need to list Clojure, but you will be locked into using the same
version of Clojure that Leiningen is using. For Leiningen 1.x, a
dependency on Clojure Contrib is also implied, though this is gone in
2.0.

The "lein help" task will display the first line of the task
function's docstring as a summary.  Then "lein help $TASK" will use
the task function's full docstring for detailed help. The function's
arglists will also be shown, so pick argument names that are clear and
descriptive. If you set :help-arglists in the function's metadata, it
will be used instead for those cases where alternate arities exist
that aren't intended to be exposed to the user.

If your task returns an integer, it will be used as the exit code for
the process. If tasks are chained together, a nonzero integer return
value will halt the chain and exit immediately. Throwing an exception
will also halt execution, but returning an integer will avoid showing
an unsightly stack trace.

## Threads

Leiningen contains a workaround for a flaw in Clojure's agent thread
pools that may cause confusion. The JVM will
[refuse to exit](http://tech.puredanger.com/2010/06/08/clojure-agent-thread-pools/)
if there are active non-daemon threads. Clojure agents and futures
start up a non-daemon thread pool, so if you call any code that uses
agents or futures (even code that isn't inherently asynchronous like
<tt>clojure.java.shell/sh</tt>), the JVM won't exit until
<tt>(shutdown-agents)</tt> is called.

In order to work around this, Leiningen appends a call to
<tt>(shutdown-agents)</tt> to any code that runs in a project
subprocess. If it didn't, many plugins would simply never finish. But
it has a few unfortunate side-effects. You may expect the fact that
your plugin starts its own thread using <tt>future</tt> or
<tt>send</tt> to keep its process alive, but this is not possible with
Leiningen's workaround.

On the other hand, you may start up your own threads outside Clojure's
thread pools. These threads <i>will</i> keep the process alive on
their own, but they will not prevent Leiningen from running
<tt>(shutdown-agents)</tt>. This will cause code that's running in
your threads to lose access to agents and futures since
[shutdown-agents is irreversible](http://p.hagelb.org/shutdown-agents.jpg).

The solution is to block in your main thread if you need other threads
to keep alive. However, if your task is being run from Leiningen's
<tt>interactive</tt> task, the project subprocess will not exit and
<tt>shutdown-agents</tt> will not be triggered, so it's best to block
conditionally. Here's an example:

    (when-not ~leiningen.core/*interactive?*
      (shutdown-agents))

Note again that this only applies for plugins that need to run code in
the project's process, so the code above would go inside the
<tt>form</tt> argument to <tt>eval-in-project</tt>. If your plugin
only runs in Leiningen's process then you don't need to
worry. Hopefully this will be
[fixed in future versions of Clojure](http://dev.clojure.org/jira/browse/CLJ-124),
but the workaround will remain necessary for backwards-compatibility.

## Hooks

You can modify the behaviour of built-in tasks to a degree using
hooks. Hook functionality is provided by the [Robert
Hooke](https://github.com/technomancy/robert-hooke) library. This is an
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

If you need to use hooks from code that runs inside the project's
process, you may use <tt>leiningen.util.injected/add-hook</tt>, which
is an isolated copy of <tt>robert.hooke/add-hook</tt> injected into
the project in order to support features like test selectors.

See [the documentation for
Hooke](https://github.com/technomancy/robert-hooke/blob/master/README.md)
for more details.

## Altering Leiningen's Classpath

Leiningen's classpath will include all plugins from :dev-dependencies
as well as user plugins. To further modify the classpath of Leiningen
itself, add a '.lein-classpath' file a project's root. Its contents
will be prepended to Leiningen's classpath when Leiningen is invoked
upon that project.

## Lancet

If your plugins need to do a fair amount of filesystem-y things, you
may want to take a look at using Ant tasks to do them since the JDK
lacks a lot of simple functionality of this kind. Using the Ant API
directly is a pain, but it can be eased to a degree using
[Lancet](https://github.com/stuarthalloway/lancet). Lancet is the
Clojure adapter for Ant that is developed as the sample project in the
[Programming
Clojure](http://www.pragprog.com/titles/shcloj/programming-clojure)
book.

You can look over the [Ant API documentation's listing of
tasks](http://www.jajakarta.org/ant/ant-1.6.1/docs/en/manual/api/org/apache/tools/ant/taskdefs/package-summary.html)
to find an appropriate task. See the <tt>deps</tt> task for an example
of how to call a task from Clojure.

## Have Fun

Please add your plugins to [the list on the
wiki](http://wiki.github.com/technomancy/leiningen/plugins).

Hopefully the plugin mechanism is simple and flexible enough to let
you bend Leiningen to your will.
