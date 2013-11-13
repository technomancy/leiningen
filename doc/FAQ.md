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
  to deploy them to a static HTTP server or a private S3 bucket with the
  [s3-wagon-private](https://github.com/technomancy/s3-wagon-private)
  plugin. Once the repo is set up, `lein deploy private-repo com.mycorp/somejar
  1.0.0 somejar.jar pom.xml` will push the artifacts out. If you don't
  have a pom, you can create a dummy project with `lein new` and
  generate a pom from that. If you are just doing exploratory coding
  you can deploy to `file:///$HOME/.m2/repository` and the jars will
  be available locally.

**Q:** I want to hack two projects in parallel, but it's annoying to switch between them.  
**A:** Leiningen provides a feature called *checkout dependencies*.
  See the
  [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
  to learn more.

**Q:** Is it possible to exclude indirect dependencies?  
**A:** Yes. Some libraries, such as log4j, depend on projects that are
  not included in public repositories and unnecessary for basic
  functionality.  Projects listed as `:dependencies` may exclude
  any of their dependencies by using the `:exclusions` key. See
  `lein help sample` for details.

**Q:** Why doesn't `deps` task populate the `lib` directory in version 2?  
**A:** The only reason version 1 copied the jars around in the first
  place was to support existing tooling that needed a cheap way to
  calculate a project's classpath. Now that Leiningen has a mature
  plugin ecosystem, this is no longer needed; jars can be referenced
  directly out of the `~/.m2/repository` directory. If you need to see
  a listing of all the dependencies that will be used and their
  versions, use `lein deps :tree`. To get the classpath use `lein classpath`.

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
  integration. (See [nrepl.el](https://github.com/kingtim/nrepl.el) or
  [foreplay](https://github.com/tpope/vim-foreplay), for example.)
  Otherwise you can use the basic `lein repl`.

**Q:** Still too slow; what else can make startup faster?  
**A:** The wiki has a page covering
  [ways to improve startup time](https://github.com/technomancy/leiningen/wiki/Faster).

**Q:** What if I care more about long-term performance than startup time?  
**A:** Leiningen 2.1.0 onward get a speed boost by disabling optimized
  compilation (which only benefits long-running processes).  This can
  negatively affect performance in the long run, or lead to inaccurate
  benchmarking results.  If want the JVM to fully optimize, you can follow
  the instructions on the Wiki page covering
  [performance](https://github.com/technomancy/leiningen/wiki/Faster).

**Q:** What does "Unrecognized VM option 'TieredStopAtLevel=1'" mean?  
**A:** Old versions of the JVM do not support the directives Leiningen
  uses for tiered compilation which allow the JVM to boot more
  quickly. You can disable this behaviour with `export LEIN_JVM_OPTS=`
  or upgrade your JVM to something more recent. (newer than b25 of Java 6)

**Q:** What are the downsides of Tiered Compilation?  
**A:** Tiered Compilation sacrifices long-term JIT performance for
  improved boot time. Most uses of Leiningen are in a context where
  fast boot is more important, but in cases where this isn't the case
  you can switch profiles (`lein with-profiles production run ...`) to
  prevent the Tiered Compilation `:jvm-opts` setting from being used.

**Q:** I'm attempting to run a project as a background process (`lein run &`),
  but the process suspends until it is in the foreground. How do I run a program
  in the background?  
**A:** For long-lasting processes, use `lein trampoline run &` or consider to
  (uber)jar the program. For short-lived ones, both `lein run <&- &` and
  `bash -c "lein run &"` will work fine.

**Q:** I need to do AOT for an uberjar; can I avoid it during development?  
**A:** A reasonable request. Leiningen supports isolating different
  profiles by their target directory. Simply specify `:target-path
  "target/%s"` in order to have each profile set use a different
  directory for generated files. Then you can put your `:aot`
  settings in the `:uberjar` profile, and the .class files created
  from the AOT process will not affect normal development use. You can
  specify the profile-isolated `:target-path` in your `:user` profile if
  you want it applied across all the projects you work on.
