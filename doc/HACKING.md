# Hacking Leiningen

Leiningen is composed of a few layers. First we have the
`leiningen-core` library, which implements the basic project-centric
functionality that would be useful outside the context of Leiningen
itself, primarily for IDEs and other tools. This is available
independently on [Clojars](http://clojars.org/leiningen-core) and
[documented on Github](http://technomancy.github.com/leiningen).

The next layer is `leiningen.main`, which is the launcher responsible
for resolving and calling Leiningen task functions.

Finally we have the tasks themselves. Tasks are simply functions in
specially-named namespaces:

```clj
(ns leiningen.pprint
  (:require [clojure.pprint :as pp]))

(defn pprint
  "Print a readable representation of the current project."
  [project]
  (pp/pprint project))
```

Task creation is documented in the
[plugins guide](https://github.com/technomancy/leiningen/blob/stable/doc/PLUGINS.md).
The tasks that come with Leiningen are not plugins, but writing them
follows the exact same rules.

## Task Execution

When you launch Leiningen, it must start an instance of Clojure to
load itself. But this instance must not affect the project that you're
building. It may use a different version of Clojure from Leiningen,
and the project should be in a fresh JVM. Leiningen currently launches
this as a subprocess using `leiningen.core.eval/eval-in-project`. Any
code that must execute within the context of the project (AOT
compilation, test runs) needs to go through this function.

The exception to this rule is when `:eval-in-leiningen` in
`project.clj` is true, as is commonly used for Leiningen plugins.

# Release Checklist

* update NEWS, bin/lein, bin/lein.bat, project.clj, pom
* rm -rf lib classes, compile :all, generate uberjar, upload
* test self-install
* git tag
* push, push tags, update stable branch
* announce on mailing list
* bump version numbers (bin/lein, bin/lein.bat, project.clj)
* regenerate pom.xml
