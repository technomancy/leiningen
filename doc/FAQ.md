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
**A:** The best thing to do is to get them in a repository. The
  [deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
  explains how to set up a private repository. In general it's easiest
  to deploy them to a static HTTP server or a private S3
  bucket with the
  [s3-wagon-private](https://github.com/technomancy/s3-wagon-private)
  plugin. If you are just doing exploratory coding and not
  collaborating with a team you could
  [install locally](https://github.com/kumarshantanu/lein-localrepo).

**Q:** I want to hack two projects in parallel, but it's annoying to switch between them.  
**A:** Leiningen provides a feature called *checkout dependencies*. See the [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)
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

**Q:** What does `java.lang.NoSuchMethodError: clojure.lang.RestFn.<init>(I)V` mean?  
**A:** It means you have some code that was AOT (ahead-of-time)
  compiled with a different version of Clojure than the one you're
  currently using. If it persists after running `lein clean` then it
  is a problem with your dependencies. Note that for
  your own project that AOT compilation in Clojure is much less
  important than it is in other languages. There are a few
  language-level features that must be AOT-compiled to work, generally
  for Java interop. If you are not using any of these features, you
  should not AOT-compile your project if other projects may depend
  upon it.

**Q:** I specified a dependency on version X but am getting version Y; what's up?  
**A:** One of your dependencies' dependencies has declared a
  dependency on a hard version range, which overrides your "soft"
  declaration. If you change yours to a hard version range, it will
  refuse to function due to conflicts, so it's best to find the
  dependency that's at fault via the
  [lein-pedantic plugin](https://github.com/xeqi/lein-pedantic) and
  add an `:exclusions` clause to it. See `lein help sample` for how
  exclusions work. You may also want to report a bug with the
  dependency that uses hard version ranges as they cause all kinds of
  problems and exhibit unintuitive behaviour.

**Q:** I'm behind an HTTP proxy; how can I fetch my dependencies?  
**A:** Set the `$http_proxy` environment variable in Leiningen 2.x. You can also
  set `$http_no_proxy` for a list of hosts that should be reached directly, bypassing
  the proxy. This is a list of patterns separated by `|` and may start or end with
  a `*` for wildcard, e.g. `localhost|*.mydomain.com`.
  For Leiningen 1.x versions, see the instructions for
  [configuring a Maven proxy](http://maven.apache.org/guides/mini/guide-proxies.html)
  using `~/.m2/settings.xml`.

**Q:** What can be done to speed up launch?  
**A:** The main delay involved in Leiningen comes from starting the
  JVM. Most people use a development cycle that involves keeping a
  single REPL process running for as long as you're working on that
  project. Depending on your editor you may be able to do this via its
  Clojure integration. (See
  [nrepl.el](https://github.com/kingtim/nrepl.el) or
  [foreplay](http://github.com/tpope/foreplay), for
  example.) Otherwise you can use the basic `lein repl`.

**Q:** Still too slow; what else can make startup faster?  
**A:** The `trampoline` task uses Leiningen to calculate the command
  needed to launch your project and then allows Leiningen's JVM to
  exit before launching your project. Exporting the
  `$LEIN_FAST_TRAMPOLINE` environment variable allows the command to
  be cached, making it possible to skip launching the Leiningen JVM
  entirely. You can set this on a per-project variable by creating a
  `.lein-fast-trampoline` file in the project root. In addition, you
  can usually set `:bootclasspath true` in project.clj to speed up
  project launches, though there are compatibility issues with some
  libraries.

**Q:** It looks like a different set of dependencies are being used in
  the repl vs other tasks.  
**A:** The repl needs to add a few extra implicit dependencies in
  order to function. For instance, it needs to load an
  [nREPL server](https://github.com/clojure/tools.nrepl), and it loads
  a client for fetching
  [ClojureDocs examples](http://clojuredocs.org), both which have
  dependencies that could interfere with the dependencies declared in
  your own project. You can declare a `:leiningen/reply` profile to
  override the clojuredocs client dependencies. While you can't run
  the repl task without nREPL in the dependencies, specifying your own
  version of it will override the version Leiningen adds. Another way
  around the problem would be to load a bare repl by invoking `lein
  trampoline run -m clojure.main/repl`, though you may want to use
  `rlwrap` on that to get proper key bindings and history.
