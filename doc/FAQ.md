<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [FAQ](#faq)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# FAQ

**Q:** How do you pronounce Leiningen?  
**A:** It's LINE-ing-en. ['laɪnɪŋən]

**Q:** What's a group ID? How do snapshots work?  
**A:** See the
  [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
  for background.

**Q:** How should I pick my version numbers?  
**A:** Use [semantic versioning](http://semver.org).

**Q:** What if my project depends on jars that aren't in any repository?  
**A:** You will need to get them in a repository. The
  [deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
  explains how to set up a private repository. In general it's easiest
  to deploy them to a static HTTP server or a private S3 bucket. Once
  the repo is set up, `lein deploy private-repo com.mycorp/somejar
  1.0.0 somejar.jar pom.xml` will push the artifacts out. If you don't
  have a pom, you can create a dummy project with `lein new` and
  generate a pom from that. If you are just doing exploratory coding
  you can deploy to `file:///$HOME/.m2/repository` and the jars will
  be available locally.

**Q:** I want to hack a project and one of its dependencies, but it's annoying to switch between them.  
**A:** Leiningen provides a feature called *checkout dependencies* to
  make this smoother.  See the
  [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
  to learn more.

**Q:** Is it possible to exclude indirect dependencies?  
**A:** Yes. Some libraries, such as log4j, depend on projects that are
  not included in public repositories and unnecessary for basic
  functionality.  Projects listed as `:dependencies` may exclude
  any of their dependencies by using the `:exclusions` key. See
  `lein help sample` for details.

**Q:** I specified a dependency on version X but am getting version Y; what's up?  
**A:** One of your dependencies' dependencies has declared a
  dependency on a hard version range, which overrides your "soft"
  declaration. Running `lein deps :tree` will identify which of your
  dependencies are responsible for the version range. You can add an
  `:exclusions` clause to prevent that from affecting the rest of your
  dependencies. See `lein help sample` for how exclusions work. You
  may also want to report a bug with the dependency that uses hard
  version ranges as they cause all kinds of problems and exhibit
  unintuitive behaviour.

**Q:** I have two dependencies, X and Y, which depends on Z. How is the version
  of Z decided?  
**A:** The decision depends on which depth and which order the dependencies come
  in the `:dependencies` vector: The dependency at the lowest depth will be
  picked. If there are multiple versions of a single group/artifact at that
  depth, the first of those will be picked. For instance, in the dependency
  graph

    [Z "1.0.9"]
    [X "1.3.2"]
      [Z "2.0.1"]

  the direct dependency (`[Z "1.0.9"]`) is picked, as it is closest to the root.
  For the dependency graph

    [X "1.3.2"]
      [Z "2.0.1"]
    [Y "1.0.5"]
      [Z "2.1.3"]

  the dependency X comes first, and therefore `[Z "2.0.1"]` is picked. If we
  place Y before X however, `[Z "2.1.3"]` will be picked.
  
  Note that this only applies to soft dependencies, and `lein deps :tree` will
  only warn if the latest version is not chosen.

**Q:** I'm behind an HTTP proxy; how can I fetch my dependencies?  
**A:** Set the `$http_proxy` environment variable in Leiningen 2.x. You can also
  set `$http_no_proxy` for a list of hosts that should be reached directly, bypassing
  the proxy. This is a list of patterns separated by `|` and may start or end with
  a `*` for wildcard, e.g. `localhost|*.mydomain.com`.
  For Leiningen 1.x versions, see the instructions for
  [configuring a Maven proxy](http://maven.apache.org/guides/mini/guide-proxies.html)
  using `~/.m2/settings.xml`.

**Q:** What can be done to speed up launch?  
**A:** The main delay involved in Leiningen comes from starting two
  JVMs: one for your project and one for Leiningen itself. Most people
  use a development cycle that involves keeping a single project REPL
  process running for as long as they're working on that project.
  Depending on your editor you may be able to do this via its Clojure
  integration. (See [nrepl.el](https://github.com/clojure-emacs/cider) or
  [fireplace](https://github.com/tpope/vim-fireplace), for example.)
  Otherwise you can use the basic `lein repl`.

**Q:** Still too slow; what else can make startup faster?  
**A:** The wiki has a page covering
  [ways to improve startup time](https://github.com/technomancy/leiningen/wiki/Faster).

**Q:** What if I care more about long-term performance than startup time?  
**A:** Leiningen 2.1.0 onward get a speed boost by disabling optimized
  compilation (which only benefits long-running processes).  This can
  negatively affect performance in the long run, or lead to inaccurate
  benchmarking results.  If want the JVM to fully optimize, you can
  you can switch profiles with `lein with-profiles production run ...`.

**Q:** What does "Unrecognized VM option 'TieredStopAtLevel=1'" mean?  
**A:** Old versions of the JVM do not support the directives Leiningen
  uses for tiered compilation which allow the JVM to boot more
  quickly. You can disable this behaviour with `export LEIN_JVM_OPTS=`
  or upgrade your JVM to something more recent. (newer than b25 of Java 6)

**Q:** I'm attempting to run a project as a background process (`lein run &`),
  but the process suspends until it is in the foreground. How do I run a program
  in the background?  
**A:** For long-lasting processes, it's better to create an uberjar
  and run that or use `lein trampoline run &`. For short-lived ones,
  both `lein run <&- &` and `bash -c "lein run &"` will work fine.

**Q:** I'm getting "could not transfer artifact ... peer not authenticated"  
**A:** This means that either your JVM is not configured with the
  correct certificate authorities, or you're experiencing a
  [man-in-the-middle attack](https://github.com/technomancy/leiningen/issues/1028#issuecomment-32732452)
  on your SSL connection. Leiningen ships with the current Clojars
  public certificate at the time of this writing, so you should be
  able to work around problems with your CA by putting `:certificates
  ["clojars.pem"]` in your `:user` profile, assuming the certificate
  that ships with Leiningen hasn't expired.

**Q:** How do I determine my project's version at runtime?  
**A:** Leiningen writes a file called `pom.properties` into
  `target/classes` which contains a the version number and current git
  revision of the project. In previous versions of Leiningen this was
  only available when running from jar files, but as of 2.4.1 it's
  available during `lein run ...`, etc. You can read it by running
  this code (replace "group" and "artifact" with values appropriate to
  your project):

```clj
(with-open [pom-properties-reader (io/reader (io/resource "META-INF/maven/group/artifact/pom.properties"))]
  (doto (java.util.Properties.)
    (.load pom-properties-reader)))
```

**Q:** How can I read my project map at runtime?  
**A:** Usually you do not need the complete project map, only a specific subset
  of some values. If you want different configuration based on different tasks,
  then [environ](https://github.com/weavejester/environ) is probably a good fit.
  If you want information like the project's version number or git revision,
  read the question and answer above.

  Generally those solutions are sufficient, but if you need more than this, you
  should rather read the `project.clj` yourself. The project map changes based
  on the task you use, and so different tasks (repl, jar, uberjar to name a few)
  will make it hard to make the testing- and production project map identical.
  To add `project.clj` to your classpath, you can add in
  [lein-shell](https://github.com/hyPiRion/lein-shell) and prepend `:prep-tasks`
  with `["shell" "cp" "project.clj" "resources/project.clj"]` and read it
  through e.g.

```clj
(read-string (slurp (io/resource "project.clj")))
```

**Q:** I need to do AOT for an uberjar; can I avoid it during development?  
**A:** A reasonable request. Leiningen supports isolating different
  profiles by their target directory. Simply specify `:target-path
  "target/%s"` in order to have each profile set use a different
  directory for generated files. Then you can put your `:aot`
  settings in the `:uberjar` profile, and the .class files created
  from the AOT process will not affect normal development use. You can
  specify the profile-isolated `:target-path` in your `:user` profile if
  you want it applied across all the projects you work on.

**Q:** Is there a way to use an uberjar without AOT?  
**A:** As of Leiningen 2.4.0, if you omit `:main` in `project.clj`,
  your uberjars will use `clojure.main` as their entry point. You can
  launch with `java -jar my-app-standalone.jar -m my.entry.namespace
  arg1 arg2 [...]` without any AOT, but it will take longer to launch.

**Q:** Why does `lein jar` package some namespaces from dependencies into my jar?  
**A:** This is likely because you want to AOT-compile namespaces. Any
  AOT-compiled namespace can only depend on AOT-compiled namespaces. Therefore,
  if you depend on a namespace in a dependency that is not AOT-compiled, it will
  be AOT-compiled and bundled with the jar.

**Q:** I'd like to have certain config active only on a certain OS.  
**A:** You can do this by using unquote in the `:dev` profile:

```clj
:profiles {:dev [:dev/all ~(leiningen.core.utils/get-os)]
           :dev/all {...}
           :linux {...}
           :windows {...}
           :macosx {...}}
```

You can also check things like `(System/getProperty
"java.specification.version")` to use the JVM version or any other
property.
