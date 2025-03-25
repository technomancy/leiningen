<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [BEGINNER_MISTAKES](#beginner_mistakes)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Beginner Mistakes

**Q:** When I have no `project.clj` the command `lein repl` works, but I
       added one and now `lein repl` crashes with:
```
Error: Could not find or load main class clojure.main
Caused by: java.lang.ClassNotFoundException: clojure.main
Subprocess failed (exit code: 1)
```
**A:** When you don't have a `project.clj` Leiningen automatically includes
      the core Clojure language for you.  When you add a `project.clj`,
      Leiningen expects you to specify which version of Clojure you'll
      use. If you're just experimenting with Clojure, remove `project.clj`.
      Otherwise, make sure your `project.clj` has a `:dependencies` section
      with at least `[org.clojure/clojure "1.11.1"]` in it. (Change the version
      from `1.11.1` as necessary.)

