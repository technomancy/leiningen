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
**A:** Use [semantic versioning](http://semver.org) to communicate
  intentions to downstream users of your library, but don't make
  assumptions that libraries you use stick with it consistently. Remember
  that the difference between a breaking change and a bug fix is often
  subjective.

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
  [configuring a Maven proxy](https://maven.apache.org/guides/mini/guide-proxies.html)
  using `~/.m2/settings.xml`.

**Q:** What can be done to speed up launch?  
**A:** The main delay involved in Leiningen comes from starting two
  JVMs: one for your project and one for Leiningen itself. Most people
  use a development cycle that involves keeping a single project REPL
  process running for as long as they're working on that project.
  Depending on your editor you may be able to do this via its Clojure
  integration. (See [cider](https://github.com/clojure-emacs/cider) or
  [fireplace](https://github.com/tpope/vim-fireplace), for example.)
  Otherwise you can use the basic `lein repl`.

**Q:** Version 2.8.0 seems a bit slower; why is that?  
**A:** We have long used a hack of putting Leiningen on the JVM's
  bootclasspath to speed up boot time, but the module system in Java 9
  breaks this. We have switched to another method of speeding it up
  (`-Xverify:none`) which gives anywhere from 95% to 70% of the same
  speed boost depending on the machine on which you're running it. So
  some users will notice a performance regression. We hope to go back
  to the old method once Clojure 1.9.0 is released with a workaround,
  but in the mean time if you are not using Java 9, you can go back to
  the bootclasspath hack with this setting:

    export LEIN_USE_BOOTCLASSPATH=y

**Q:** Still too slow; what else can make startup faster?  
**A:** The wiki has a page covering
  [ways to improve startup time](https://github.com/technomancy/leiningen/wiki/Faster).

**Q:** What if I care more about long-term performance than startup time?  
**A:** Leiningen 2.1.0 onward get a speed boost by disabling optimized
  compilation (which only benefits long-running processes).  This can
  negatively affect performance in the long run, or lead to inaccurate
  benchmarking results.  If want the JVM to fully optimize, you can
  you can switch profiles with `lein with-profiles production run ...`.

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
  `project.clj` is added as a resource in
  `META-INF/leiningen/group/artifact/project.clj` (replace "group" and
  "artifact" with values appropriate to your project). You can read it as
  follows:

```clj
(read-string (slurp (io/resource "META-INF/leiningen/group/artifact/project.clj")))
```

**Q:** I need to do AOT for an uberjar; can I avoid it during development?  
**A:** Yes, it is strongly recommended to do AOT only in the uberjar task
  if possible. But by default the AOT'd files will still be visible during 
  development unless you also change `:target-path` to something like
  `"target/uberjar"` in the `:uberjar` profile as well.

**Q:** Is there a way to use an uberjar without AOT?  
**A:** As of Leiningen 2.4.0, if you omit `:main` in `project.clj`,
  your uberjars will use `clojure.main` as their entry point. You can
  launch with `java -jar my-app-standalone.jar -m my.entry.namespace
  arg1 arg2 [...]` without any AOT, but it will take longer to launch.

**Q:** Why does `lein jar` package some namespaces from dependencies into my jar?  
**A:** This is likely because you have AOT-compiled its namespaces. An
  AOT-compiled namespace can only depend on AOT-compiled namespaces. Therefore,
  if you depend on a namespace in a dependency that is not AOT-compiled, it will
  be AOT-compiled and bundled with the jar. It is strongly recommended not to
  perform AOT other than during the creation of an uberjar.

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

**Q:** I get a `java.security.KeyException` or `sun.security.provider.certpath.SunCertPathBuilderException` when running `lein`  
**A:** The `java.security.KeyException` indicates an ssl error when trying to communicate with the HTTPS server via Java. This could be because you need to update the JDK, or some other package (e.g. with old versions of the nss package).

* On Fedora, you might just try running a `sudo yum update` to update all of your packages or `sudo yum update nss`.
* On Debian/Ubuntu, `sudo update-ca-certificates -f` might help, or `sudo /var/lib/dpkg/info/ca-certificates-java.postinst configure`
* You should also check your system clock and make sure the time is accurate; it's possible to run into SSL connection failures if your clock is way out of sync.
* If it still doesn't work, please see if any of [these 'ssl' labelled issues](https://github.com/technomancy/leiningen/issues?utf8=%E2%9C%93&q=is%3Aissue%20label%3Assl%20) might help

**Q:** I got `Tried to use insecure HTTP repository without TLS`, what is that about?  
**A:** This means your project was configured to download dependencies
from a repository that does not use TLS encryption. This is very
insecure and exposes you to trivially-executed man-in-the-middle attacks.
In the rare event that you don't care about the security of the machines
running your project, you can re-enable support for unprotected repositories
by putting this in your `project.clj` file:

    ;; never do this
    (require 'cemerick.pomegranate.aether)
    (cemerick.pomegranate.aether/register-wagon-factory!
     "http "#(org.apache.maven.wagon.providers.http.HttpWagon.))

It's also possible you have a dependency which includes a reference to
an insecure repository for retrieving its own dependencies. If this
happens it is strongly recommended to add an `:exclusion` and report a
bug with the dependency which does this.

**Q:** `lein`/`lein.bat` won't download `leiningen-x.y.z-SNAPSHOT.jar`  
**A:** You probably downloaded `lein`/`lein.bat` from the [master branch](https://github.com/technomancy/leiningen/tree/master/bin). Unless you plan to build leiningen yourself or help develop it, we suggest you use the latest stable version: [lein](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein)/[lein.bat](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.bat)

**Q:** What does `Unrecognized VM option 'UseCGroupMemoryLimitForHeap'` mean?  
**A:** Leiningen 2.8.0 and up use this Java flag which provides better default
memory settings when running in containerization. Older versions of Java do not
support this flag. If you see this, it is *strongly* recommended that you upgrade
Java, since the older versions have a large number of security flaws. If you
cannot for some reason and don't care about security, you can add
`:jvm-opts ^:replace []` to your `project.clj` file.

**Q:** I have a dependency whose group ID and/or artifact ID starts with a
  number (which is invalid for symbols in Clojure). How can I add it to my
  project's dependencies?  
**A:** As of version 2.8.0, Leiningen supports string dependency names like
  this:

```clj
:dependencies [["net.3scale/3scale-api" "3.0.2"]]
```

Prior to version 2.8.0, this is the workaround:

```clj
:dependencies [[~(symbol "net.3scale" "3scale-api") "3.0.2"]]
```
