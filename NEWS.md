# Leiningen News -- history of user-visible changes

## 2.0.0-RC1 / ???

* Don't AOT the `:main` namespace outside of uberjar task.
* Check Clojars releases repository by default instead of legacy Clojars.
* Allow hooks from profiles to apply with limited scope. (Hugo Duncan)
* Fix a bug where profile-specific paths were ignored in trampoline.
* Support reading from stdin inside project process.
* Add `:only` test selector. (Anthony Grimes)
* Support partial application for test selectors. (Anthony Grimes)
* Un-deprecate `:auth` profile for full-disk-encryption users.
* Add documentation for mixed-source projects. (Michael Klishin)
* Make later profiles take precedence in with-profile task. (Justin Balthrop)
* Improve help for subtasks. (Tobias Crawley)
* Allow vectors to specify multiple credential sources. (Chas Emerick)
* Look up credentials in environment using namespaced keywords. (Chas Emerick)
* Support overriding repl profile from project.clj or profiles.clj.
* Allow test selectors to operate on namespace. (Jim Crossley)
* Honor environment variables in project.clj. (Justin Balthrop)
* Allow searching over fields other than artifact id. (Michael Klishin)
* Honor per-project REPL history. (Michael Klishin, Colin Jones)
* Reduce output during dependency resolution. (Nelson Morris)
* Fix search task outside project. (Bruce Adams)

## 2.0.0-preview10 / 2012-08-25

* Fix a bug where repositories wouldn't be checked running outside a project.
* Make repl listen on 127.0.0.1 instead of localhost to address IPv6 issues.

## 2.0.0-preview9 / 2012-08-24

* Use provided profile by default everywhere except uberjar. (Marshall Vandegrift)
* Unify format for auto-loading middleware and hooks. (Justin Balthrop)
* Allow more declarative :nrepl-middleware settings. (Chas Emerick)
* Fix `:eval-in :classloader` for native dependencies. (Justin Balthrop)
* Support project and user leinrc file for shell-level customization. (Justin Balthrop)
* Cache trampoline commands for fast boot. Set `$LEIN_FAST_TRAMPOLINE` to enable.
* Support setting HTTPS proxies.
* Improved resilience when self-install is interrupted. (Bruce Adams)
* Fix a bug where profile dependencies weren't honored in trampoline task.

## 2.0.0-preview8 / 2012-08-16

* Place SCM revision in pom.properties in jar files.
* Allow middleware and hooks to be inferred from plugins. (Justin Balthrop)
* Offer similar suggestions when no task is found for input. (Joe Gallo)
* Support `TERM=dumb` in repl task. (Colin Jones)
* Fix reader mismatches between repl client and server. (Colin Jones)
* Use new search index format, support incremental updates. (Christoph Seibert)
* Accept nREPL handlers and middleware from project config.
* Support emitting arbitrary elements in pom.xml. (Esa Laine)
* Fix a bug where repl task was binding to 0.0.0.0.
* Honor `$http_no_proxy` host settings. (Jon Pither)
* Profiles can be specified as compositions of other profiles. (Justin Balthrop)
* Allow for `:prep-tasks` with arguments. (Anthony Marcar)
* Check for "help" after task name. (Bruce Adams)
* Read dependency transport wagons from plugins.
* Allow successive eval-in-project calls with trampoline.
* Bring back selective post-compile cleaning. (Arlen Cuss)
* Fix memory leak in repl task.

## 2.0.0-preview7 / 2012-06-27

* Fix a bug where failed javac wouldn't abort. (Michael Klishin)
* Check task aliases everywhere tasks are invoked.
* Sign jars and poms of releases upon deploy by default.
* Don't decrypt `credentials.clj.gpg` for every request.
* Support setting `:mirrors` in project.clj. (Chas Emerick, Nelson Morris)
* Allow aliases shadowing task names to invoke shadowed tasks.
* Emit `doc/intro.md` in new project templates.
* Allow `:scm` to be set in project.clj for pom inclusion. (Florian Anderiasch)
* Fix a bug where dependency `:classifier` and `:extension` would be ignored.
* Speed up subprocess launches when `:bootclasspath` is set.
* Set user agent for HTTP requests. (Bruce Adams)
* Verify signatures of dependencies with `lein deps :verify`.
* Move task chaining to `do` task in order to allow for higher-order use.

## 2.0.0-preview6 / 2012-06-01

* Allow lookup of `:repositories` credentials from environment variables.
* Perform more SSL certificate validity checks.
* Fix a bug where repl dependency was conflicting.
* Add certificate for Clojars to default project settings.
* Allow custom SSL `:certificates` to be specified for repositories.

## 2.0.0-preview5 / 2012-05-31

* Fix a repl bug where namespaced keywords weren't read right. (Colin Jones)
* Prompt for credentials upon deploy when none are configured.
* Support encrypted deploy credentials using GPG.
* Warn about missing metadata when deploying.
* Default to refusing downloaded jars when checksums don't match.
* Apply middleware before calculating profiles so they work in with-profile.
* Allow reply dependency to be upgraded independently of Leiningen.
* Don't write "stale" directory when running outside a project.
* Proxy settings are passed on to project subprocesses. (Craig McDaniel)
* Revamp tutorial, spin off profiles guide and faq.
* Fix bug that would cause repl task to hang. (Colin Jones)

## 2.0.0-preview4 / 2012-05-11

* Checkout dependencies are not applied with production profile.
* Move pom.xml back to the project root.
* Add -U alias for forcing updates of snapshots.
* Support setting :update and :checksum profiles at top level of project.
* Blink matching parens in repl. (Colin Jones)
* Fix a bug where repl would interfere with project agents. (Chas Emerick)
* Show repl output that is emitted after return value. (Colin Jones)
* Make it easier for plugins to undo profile merging. (David Santiago)
* Add -o alias for activating offline profile.
* Ignore $CLASSPATH environment variable.
* Fix bug where repl task couldn't be trampolined. (Colin Jones)
* Allow jar manifest entries to be dynamically calculated.
* Support map-style :javac-opts like Leiningen 1.x used. (Michael Klishin)
* Allow group-id to be specified when creating new projects. (Michael Klishin)
* Fix a bug where :dev dependencies would be exposed in pom.
* Use Clojure 1.4.0 internally; plugins have access to new Clojure features.

## 2.0.0-preview3 / 2012-04-12

* Add HTTP nREPL support for repl task via :connect option. (Chas Emerick,
  Phil Hagelberg)
* Improve repl startup time, output consistency, Windows support. (Lee Hinman,
  Colin Jones)
* Stop using numeric exit codes for task failures.
* Dynamically resolve unknown templates in new task.
* Automatically activate offline profile when needed.
* Honor $http_proxy environment variable. (Juergen Hoetzel)
* Allow arbitrary :filespecs to be included in jars.
* Let custom :prep-tasks be specified in project.clj.
* Include :java-source-paths and dev/test deps in pom. (Nelson Morris)
* Add offline profile.
* Prevent project JVMs from outlasting Leiningen's process. (Colin Jones)
* Update lein.bat to work with version 2. (Andrew Kondratovich)
* Show a dependency tree in deps task. (Chas Emerick, Nelson Morris)
* Support connecting to nrepl server in repl task. (Chas Emerick, Colin Jones)
* Pretty-print pom.xml. (Nelson Morris)
* Display task aliases in help task. (Michael S. Klishin)
* Only compile stale java source files. (Stephen C. Gilardi)
* Respect :java-cmd in project.clj. (Michael S. Klishin)
* Show progress when downloading search indices. (Justin Kramer)

## 2.0.0-preview2 / 2012-03-08

* Honor :default and :user profiles when running outside a project.
* Fix a bug where subtask help wasn't showing.

## 2.0.0-preview1 / 2012-03-07

* Split out leiningen-core into independent library.
* Construct classpath out of ~/.m2 instead of copying jars to lib/.
* Replace maven-ant-tasks with Pomegranate library. (Chas Emerick,
  Nelson Morris)
* Move build artifacts to target/ directory.
* Add experimental support for running project code in-process with
  :eval-in :classloader. (Justin Balthrop)
* Support profiles for alternate project configurations.
* Switch to using plural :source-paths, :test-paths, and :resource-paths.
* Complete rewrite of repl task. (Colin Jones, Chas Emerick, Anthony Grimes)
* Remove special case of implicit org.clojure group-id in :dependencies.
* Replace :dev-dependencies with :dev profile.
* Support customized :source-paths with :eval-in :leiningen projects.
* Rewrite pom task. (Nelson Morris, Alan Malloy)
* Allow tasks and projects to add custom :injections into project code.
* Support changing :prep-tasks for running tasks other than javac and
  compile before eval-in-project calls.
* Rewrite new task. (Anthony Grimes)
* New check task for catching reflection and other issues. (David Santiago)
* Check project.clj for :aliases.
* Allow partial application of aliases.
* Drop :extra-classpath-dirs option.
* Load :plugins without trampolining the process.
* Remove plugin task in favour of :user profile.
* Allow :repository-auth to be specified using a regular expression.
* Support arbitrary project map transformation functions via :middleware.
* Support changing :local-repo path in project.clj.

## 1.7.1 / 2012-03-27

* Fix a bug where the repl task left JVM processes running.
* Make upgrade task accept arbitrary versions.
* Fix a bug where javac classes would get removed before AOT compilation.
* Allow :aot to contain both symbols and regexes. (Dan Lidral-Porter)
* Fix bug where clean task would be incredibly slow.
* Apply :jvm-opts with :eval-in-leiningen.
* Prevent misbehaving plugins from pulling in conflicting Clojure versions.

## 1.7.0 / 2012-02-06

* Allow any task to perform trampolining.
* Fix a bug where JVM_OPTS with spaces would cause failures.
* Keep pom dependencies off the classpath.
* Block plugins from erroneously including their own Clojure version.
* Allow poms to set parent element. (Nelson Morris)
* Support emitting Maven extensions in pom. (Max Penet)
* Allow faster booting on 64-bit JVMs with tiered compilation.
* Fix a bug where shell wrappers had the wrong classpath. (Tavis Rudd)
* Exclude all signature files from uberjars. (Tim McCormack)
* Allow test selectors to apply to entire namespaces. (Kevin Downey)
* Use LEIN_JAVA_CMD to allow different JVM for Leiningen itself. (Tavis Rudd)
* Honor :plugins key inside project.clj.
* Accept :repl-init namespace as argument to repl task.
* Allow :java-source-path to be nested inside :source-path. (Anthony Grimes)
* Fix a bug where native deps weren't made available. (Anthony Grimes)

## 1.6.2 / 2011-11-11

* Let run task work with main functions from Java classes.
* Fix bug where exceptions would break interactive task.
* Default to Clojure 1.3.0 for new projects.
* Allow Leiningen home to exist inside project directory. (Heinz N. Gies)
* Remove old versions of plugins when upgrading.
* Add user-level :deploy-repositories list. (Michał Marczyk)
* Fix a bug where class files from proxy objects weren't considered
  part of the project. (Stephen Compall)
* Make deps cause implicit clean to avoid AOT version mismatches.
* Include Java source files in jar. (Nathan Marz)
* Add separate :deploy-repositories list. (Chas Emerick)
* Maintain order in repositories list. (Colin Jones)
* Fix a bug where :omit-default-repos wouldn't skip Maven Central. (Chas Emerick)
* Make deps extract native dependencies for all architectures, not just current.
* Fix page count on search results.
* Fix a bug where "lein plugin install" could skip dependencies.
* Reimplement eval-in-project to use clojure.java.shell instead of Ant.
* Separate LEIN\_JVM\_OPTS from JVM_OPTS.

## 1.6.1.1 / 2011-09-06

* Turn off workaround for Clojure's agent thread pool keeping the JVM alive
  by default. Use :shutdown-agents in project.clj to enable it.

## 1.6.1 / 2011-07-06

* Allow alternate main namespace to be used during uberjar creation.
* Add :checkout-deps-shares to share more directories in checkout dependencies.
* Fix a bug where agent thread pool would be shut down in repl task.
* Support :project-init in project.clj to allow pprint to be used in :repl-options.
* Fix a bug where tests would not run using Clojure 1.3.
* Support for .classpath file to include context specific classpath elements.

## 1.6.0 / 2011-06-29

* Enforce project names as readable symbols.
* Add trampoline task.
* Fix a bug where plugins would be unavailable in MinGW.
* Allow functions other than -main to be called using run task.
* Support constructing classpath out of ~/.m2 instead of copying to lib/.
* Fix a bug where help output could be truncated by plugin issues.
* Support native dependencies.
* Test selectors no longer require additional hooke dependency.
* Add retest task.
* Add search task.
* Remove deprecated build.clojure.org repositories.
* Remove user/\*classpath\* var.
* Support :extra-classpath-dirs in project.clj.

## 1.5.2 / 2011-04-13

* Check rlwrap for support of custom quotes before using.
* Improve Solaris support. (Donald Clark Jackson)
* Fix curl error relating to missing $https_proxy. (Pirmin Fix)

## 1.5.1 / 2011-04-12

* Improve rlwrap quote support. (Ambrose Bonnaire-Sergeant)
* Prevent ns load exceptions from halting help.
* Fix :repl-init namespace handling.
* Make deps for :eval-in-leiningen projects available to lein process.
* Pass $https_proxy environment variable to curl.
* Fix :eval-in-leiningen when used with init arg.
* Pom now includes dev-dependencies as test-scoped. (Thomas Engelschmidt)
* Fix handling of arguments with spaces. (Stuart Fehr)
* Fix a plugin bug where it would look for dev-dependencies.
* Fix :min-lein-version checking. (Colin Jones)
* Honor user settings in more places.
* Fix running-as-root warning.
* Revert back to warning when repository checksums don't match.

## 1.5.0 / 2011-03-22

* New projects now use Clojure 1.2.1.
* Honor per-repository :update/:checksum policies.
* Allow some repositories to be releases/snapshots-only.
* Honor global :exclusions. (Joe Gallo)
* Honor :class-file-whitelist to make classes/ deletion more manageable.
* Accept :repl-init namespace in project.clj.
* Warn when falling back to jline if rlwrap is not found.
* Add prepend-task macro for simple hook usage.
* Add flexibility to clean task with :extra-files-to-clean
  and :regex-to-clean.
* Fix bug in interactive task that would cause infinite loop.
* Add version into shell wrapper template.
* Add pcmpl-lein.el for eshell completion.
* Skip fetching dependencies when they haven't changed in project.clj
  if :checksum-deps is set.
* Add system property for $PROJECT.version.
* Add deploy task.
* Reload tests in interactive mode.
* Make test! task accept namespace list as argument. (Joe Gallo)
* Use current year in readme for project skeleton. (Joe Gallo)

## 1.4.2 / 2010-12-31

* Fix a bug where init to eval-in-project was ignored in interactive task.
* Fix a bug in path calculation for native dependencies. (wburke)
* Fix a bug where built-in tasks shadowed plugins (javac, run).
* Allow a seq of regexes in :clean-non-project-classes for more flexibility.
* Fix a bug where the first argument to run would be parsed wrong. (Alex Osborne)
* Use JVM\_OPTS environment variable instead of JAVA\_OPTS, though the latter
  is still supported for backwards-compatibility.

## 1.4.1 / 2010-12-16

* Allow boosting :repl-retry-limit in project.clj for slow-starting projects.
* Turn :clean-non-project-classes off by default.
* Support :skip-aot metadata on :main in project.clj.
* Alias :deps/:dev-deps to :dependencies/:dev-dependencies in project.clj.
* Support setting clojure.debug property.
* Don't allow stable versions to depend upon snapshots.
* Fix exit code for chained tasks.

## 1.4.0 / 2010-12-02

* Support readme, tutorial, news, and copying in help task.
* Show short help summaries in help task overview.
* Keep project JVM running between task runs in interactive task.
* Support :uberjar-exclusions as a seq of regexes in project.clj.
* Support :repl-options in project.clj that get passed to clojure.main/repl.
* Shell wrappers are installed on Windows. (Matjaz Gregoric)
* Windows and Cygwin path fixes. (Matjaz Gregoric)
* Solaris compatibility fixes. (Heinz Gies)
* Deprecated :jar-dir in favour of :target-dir.
* Deprecated unused eval-in-project arguments. (handler, skip-auto-compile)
* Deprecated :namespaces and :test-resources-path in project.clj.
* Delete non-project .class files after AOT compilation. (Luc Prefontaine)
* Merge run task from lein-run plugin. (Siddhartha Reddy)
* Improve subtask help output. (Colin Jones)
* Support :eval-in-leiningen for easier testing of plugins.
* Merge javac task from lein-javac plugin. (Antonio Garrote)
* Add init argument to eval-in-project to help with the Gilardi Scenario.
  See http://technomancy.us/143 for details.
* Fix bug involving repl I/O flushing.
* Run subset of test suite using test selector predicates.
* Specify what file patterns to exclude from jars. (Zehua Liu)
* Sort and de-dupe help output. (Sergio Arbeo)
* Add plugin task: easily install user-level plugins (Colin Jones, Michael Ivey)

## 1.3.1 / 2010-09-07

* Support regex matching in :aot list. (Alex Ott)
* Run self-install automatically if uberjar is missing.
* Fix bugs that caused standalone install task to fail.
* Allow dependency type to be specified in project.clj. (John Sanda)
* Stop jar/uberjar task if compile fails. (Alan Dipert)
* Support :min-lein-version in project.clj so if a project uses newer Leiningen
  features it will warn users of old lein versions. (Isaac Hodes)
* Fix a bug where tests would get skipped if their first form was not ns.
* Fix a bug where "lein help" would hang if run from a dir with a large src/.
* Fix a bug where repl task would hang on unreadable input. (Isaac Hodes)
* Allow repl task to work outside project. (Colin Jones)
* If curl/wget is found, self-install works on Windows. (Shantanu Kumar)
* Fix bug causing standalone install task to fail.
* Allow custom shell-wrappers.
* Start repls in user ns if no :main is in project.clj.

## 1.3.0 / 2010-08-19

* Add :omit-source option to project.clj for shipping aot-only jars.
* Make repl task listen on a socket as well as the command-line.
* Write shell wrapper scripts at installation time. See TUTORIAL.md.
* Put user-level plugins in ~/.lein/plugins on the classpath.
* Load ~/.lein/init.clj on startup.
* Execution of per-project initialization script, specified in :repl-init-script option.
  (Alex Ott)
* Switch to /bin/sh instead of bash. (Mike Meyer)
* Allow multiple tasks to be chained from the command-line. (Colin Jones)
* Add test! task that cleans and does deps before testing.
* Add interactive task for entering tasks in a shell-like environment.
* Work around argument escaping bug on Windows. (Laurence Hygate)
* Require hooks to be specified in project.clj.
* Detect download failures in self-install.
* Add resources and test-resources paths to pom. (Brian Weber)
* Fix bug causing crash if OS name wasn't recognized.
* Improve AOT staleness determination heuristic.
* Fix bug where uberjar left out dependencies for non-AOT'd projects. (Alex Ott)

## 1.2.0 / 2010-07-18

* Don't enable repl rlwrap when unnecessary. (dumb terms, Emacs, etc.)
* Add support for password-protected repositories.
* Allow :jar-name and :uberjar-name to be customized.
* Allow unquoting in defproject form.
* Support classifiers in dependencies.
* Clean before running uberjar task.
* Implicitly clean lib/ before running deps.
* Add support for test-resources directory.
* Fix help output that AOT sometimes drops.
* Clear out lib/dev on lein clean even if :library-path is customized.
* Some tasks suppress useless output.
* Snapshot versions now allow self-install.
* Allow compile task to take a list of namespaces overriding project.clj.
* Handle more types of project metadata.
* Add plugin creation guide.
* Include arglists in help output.
* Make lein script usable from any subdirectory in the project root.
* Fix repl task to work with forked subprocess.
* Fork subprocess unconditionally for greater compatibility.
* Allow $JAVA_CMD to be customized.
* Fix a bug causing everything to recompile in tests. Thanks, Stuart!
* Fix exit code for test runs.
* Automatically compile and fetch deps when needed.
* Allow :jvm-opts and :warn-on-reflection to be set in project.clj.
* Merge lein-swank plugin into swank-clojure.
* Add :aot as an alias in project.clj for :namespaces to AOT-compile.
* Add option to omit-default-repositories.
* Allow group-id to be omitted when depending on Clojure and Contrib.
* Keep dev-dependencies in lib/dev, exclude them from uberjars.
* Include version numbers in jar filenames.
* Fix repl task to use project subclassloader.
* Don't allow "new" task to create *jure names.
* Add classpath command.
* Implement Checkout Dependencies. See README.
* Add option to symlink deps into lib/ instead of copying.
* Fixed bug for file timestamps inside jars.
* Generated poms should work in Java IDEs.
* Improved Cygwin support.
* Added TUTORIAL.md file for introductory concepts.

## 1.1.0 / 2010-02-16

* Added "lein upgrade" task
* Don't download snapshot releases unless actually needed.
* Make subclassloader's classpath available to projects.
* Fixed "install" task to place pom in local repository.
* Bug fixes to "new" task.
* Only AOT-compile namespaces specified in project.clj.
* Better error handling.
* Add exclusions support for dependencies.
* Support dependencies with native code.
* Added experimental Windows support.

## 1.0.1 / 2009-12-10

* Added bash completion.
* Honor $JAVA_OPTS.
* Fix new task.
* Add version task.
* Use jline for repl task.
* Fix pom task for Java 1.5 compatibility.

## 1.0.0 / 2009-12-05

* Source, test, and compilation paths can be set in project.clj.
* Project code runs in an isolated classloader; can now compile/test
  projects that require a different version of Clojure from
  Leiningen. (Does not support 1.0's test-is yet.)
* Install task no longer requires maven to be installed.
* Only compile namespaces whose .class files are older than .clj files.
* Add "new" task for generating blank projects.
* Set <scm> tag when generating pom.xml.
* Include pom.xml, pom.properties, and more detailed manifest in jars.
* Summarize pass/fail counts from test runs across all namespaces.
* Accept a list of namespaces for test task rather than testing all.
* Create $PROJECT-standalone.jar file from uberjar to distinguish from
  regular jar files.
* Plugins have more flexibility to set the classpath and other
  arguments for running project code.
* Add resources/ directory to classpath and generated jars.
* Start Leiningen faster by using -Xbootclasspath argument.

## 0.5.0 / 2009-11-17

* Initial release!
