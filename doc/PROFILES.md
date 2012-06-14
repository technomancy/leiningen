# Profiles

In Leiningen 2.x you can change the configuration of your project by
applying various profiles. For instance, you may want to have a few
extra test data directories on the classpath during development
without including them in the jar, or you may want to have Swank
Clojure available in every project you hack on without modifying every
single project.clj you use.

By default the `:dev`, `:user`, and `:default` profiles are activated
for each task, but the settings they provide are not propagated
downstream to projects that depend upon yours. Each profile is defined
as a map which gets merged into your project map.

The example below adds a "dummy-data" resources directory during
development and a dependency upon "midje" that's only used for tests.

```clj
(defproject myproject "0.5.0-SNAPSHOT"
  :description "A project for doing things."
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:resources-path ["dummy-data"]
                   :dependencies [[midje "1.4.0"]]}})
```

You can place any arbitrary defproject entries into a given profile
and they will be merged into the project map when that profile is
active.

## Declaring Profiles

In addition to `project.clj`, profiles specified in
`~/.lein/profiles.clj` will be available in all projects, though those
from `profiles.clj` will be overridden by profiles of the same name in
the `project.clj` file. This is why the `:user` profile is separate
from `:dev`; the latter is intended to be specified in the project
itself. In order to avoid collisions, the project should never define
a `:user` profile, nor should `profiles.clj` define a `:dev` profile.
Use the `show-profiles` task to see what's available.

If you want to access dependencies during development time for any
project place them in your `:user` profile. Your
`~/.lein/profiles.clj` file could look something like this:

```clj
{:user {:plugins [[lein-swank "1.4.0"]
                  [lein-pprint "1.1.1"]]}}
```

Profiles are merged by taking each key and combining the value if it's
a collection and replacing it if it's not. Profiles specified earlier
take precedence when replacing. The dev profile takes precedence over
user by default. Maps are merged recursively, sets are combined with
`clojure.set/union`, and lists/vectors are concatenated. You can add
hints via metadata that a given value should take precedence or be
displaced if you want to override this logic:

```clj
{:profiles {:dev {:prep-tasks ^:replace ["clean" "compile"]
                  :aliases ^:displace {"launch" "run"}}}}
```

The exception to this merge logic is that plugins and dependencies
have custom de-duplication logic since they must be specified as
vectors even though they behave like maps (because it only makes sense
to have a single version of a given dependency present at once). The
replace/displace metadata hints still apply though.

## Activating Profiles

Another use of profiles is to test against various sets of dependencies:

```clj
(defproject swank-clojure "1.5.0-SNAPSHOT"
  :description "Swank server connecting Clojure to Emacs SLIME"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [clj-stacktrace "0.2.4"]
                 [cdt "1.2.6.2"]]
  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0-beta1"]]}})
```

To activate other profiles for a given run, use the `with-profile`
higher-order task:

    $ lein with-profile 1.3 test :database

Multiple profiles may be combined with commas:

    $ lein with-profile qa,user test :database

Multiple profiles may be executed in series with colons:

    $ lein with-profile 1.3:1.4 test :database

## Debugging

To see how a given profile affects your project map, use the
[lein-pprint](https://github.com/technomancy/leiningen/tree/master/lein-pprint)
plugin:

    $ lein with-profile 1.4 pprint
    {:compile-path "/home/phil/src/leiningen/lein-pprint/classes",
     :group "lein-pprint",
     :source-path ("/home/phil/src/leiningen/lein-pprint/src"),
     :dependencies
     ([org.clojure/tools.nrepl "0.0.5" :exclusions [org.clojure/clojure]]
      [clojure-complete "0.1.4" :exclusions [org.clojure/clojure]]
      [org.thnetos/cd-client "0.3.3" :exclusions [org.clojure/clojure]]),
     :target-path "/home/phil/src/leiningen/lein-pprint/target",
     :name "lein-pprint",
     [...]
     :description "Pretty-print a representation of the project map."}

In order to prevent profile settings from being propagated to other
projects that depend upon yours, the default profiles are removed from
your project when generating the pom, jar, and uberjar. Profiles
activated through an explicit `with-profile` invocation will be
preserved. The `repl` task uses its own profile in order to inject
dependencies needed for the repl to function.
