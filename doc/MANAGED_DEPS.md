<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Managed Dependencies With Leiningen

Maven (and now Leiningen) provides a capability called "Dependency Management".
The idea is to provide a way to specify a version number for common library
dependencies in a single location, and re-use those version numbers from other
discrete maven/lein projects.  This makes it easy to, e.g., update your `clj-time`
dependency across a large number of projects without having to be mindful
of every common dependency version across all of your libraries.

When using `:pedantic? :abort` in your projects, to ensure that you are producing
a consist and predictable build, it can be very cumbersome to play the "dependency
version whack-a-mole" game that arises whenever an upstream library bumps a version
of one of its dependencies.  `:managed-dependencies` can help alleviate this issue
by allowing you to keep the dependency version numbers centralized.

## `:managed-dependencies`

The `:managed-dependencies` section of your `project.clj` file is just like the
regular `:dependencies` section, with two exceptions:

1. It does not actually introduce any dependencies to your project.  It only says,
  "hey leiningen, if you encounter one of these dependencies later, here are the
  versions that you should fall back to if the version numbers aren't explicitly
  specified."
2. It allows the version number to be omitted from the `:dependencies` section,
  for any artifact that you've listed in your `:managed-dependencies` section.

Here's an example:

```clj
(defproject superfun/happyslide "1.0.0-SNAPSHOT"
  :description "A Clojure project with managed dependencies"
  :min-lein-version  "2.7.0"
  :managed-dependencies [[clj-time "0.12.0"]
                         [me.raynes/fs "1.4.6"]
                         [ring/ring-codec "1.0.1"]]
  :dependencies [[clj-time]
                 [me.raynes/fs]])
```

In the example above, the final, resolved project will end up using the specified
 versions of `clj-time` and `me.raynes/fs`.  It will not have an actual dependency
 on `ring/ring-codec` at all, since that is not mentioned in the "real" `:dependencies`
 section.

This feature is not all that useful on its own, because in the example above,
we're specifying the `:managed-dependencies` and `:dependencies` sections right
alongside one another, and you could just as easily include the version numbers
directly in the `:dependencies` section.  The feature becomes more powerful
when your build workflow includes some other way of sharing the `:managed-dependencies`
section across multiple projects.

## A note on modifiers (`:exclusions`, `:classifier`, etc.)

The managed dependencies support in leiningen *does* work with modifiers such as `:exclusions` and `:classifier`.  However, at present, because of the way that lein and pomegranate process the args in the dependencies vector, you will need to explicitly specify `nil` as the value for the version arg in order to achieve this:

```clj
(defproject superfun/happyslide "1.0.0-SNAPSHOT"
  :description "A Clojure project with managed dependencies"
  :min-lein-version  "2.7.0"
  :managed-dependencies [[clj-time "0.12.0"]]
  :dependencies [[clj-time nil :exclusions [foo]]])
```

Issue #2195 covers the possibility of doing some future work to allow omission of the `nil` in the example above.

## Lein "parent" projects

One way of leveraging `:managed-dependencies` across multiple projects is to use
the [`lein-parent` plugin](https://github.com/achin/lein-parent).  This plugin
will allow you to define a single "parent" project that is inherited by multiple
"child" projects; e.g.:

```clj
(defproject superfun/myparent "1.0.0"
   :managed-dependencies [[clj-time "0.12.0"]
                            [me.raynes/fs "1.4.6"]
                            [ring/ring-codec "1.0.1"]])

(defproject superfun/kid-a "1.0.0-SNAPSHOT"
   :parent-project [:coords [superfun/myparent "1.0.0"]
                    :inherit [:managed-dependencies]]
   :dependencies [[clj-time]
                  [me.raynes/fs]])

(defproject superfun/kid-b "1.0.0-SNAPSHOT"
 :parent-project [:coords [superfun/myparent "1.0.0"]
                  :inherit [:managed-dependencies]]
 :dependencies [[clj-time]
                [ring/ring-codec]])
```

In this example, we've consolidated the task of managing common version dependencies
in the parent project, and defined two child projects that will inherit those
dependency versions from the parent without needing to specify them explicitly.

This makes it easier to ensure that all of your projects are using the same versions
of your common dependencies, which can help make sure that your uberjar builds are
more predictable and repeatable.

## Other ways to share 'managed-dependencies'

Since the `defproject` form is a macro, it would be possible to write other plugins
that generated the value for a `:managed-dependencies` section dynamically.  That
could provide other useful ways to take advantage of the `:managed-dependencies`
functionality without needing to explicitly populate that section in all of your
`project.clj` files.

## Future integration

It is likely that the functionality provided by the `lein-parent` plugin may integrated
into the leiningen core in a future release; for now we have added only the `:managed-dependencies`
functionality because it is necessary in order for the plugin to leverage it.  We
will be experimenting with different ideas for implementation / API in plugins and
making sure that we find an API that works well before submitting for inclusion
into core leiningen.
