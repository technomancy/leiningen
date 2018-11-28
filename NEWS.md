# Leiningen News -- history of user-visible changes

## 2.8.2 / ???

* Add support for repository-overrides.clj to bootstrap repository info. (Greg Haskins)
* Use stderr consistently for diagnostics. (Rob Browning)
* Fix a bug in aliases that come from profiles. (Arnout Roemers)
* Fix TLS errors in self-install on Windows. (Florian Anderiasch)
* Templates use EPL-2.0 with GPL secondary license. (Yegor Timoshenko)
* Allow GPG to be invoked unattended with passphrase. (Neil Okamoto)
* Add pprint `--not-pretty` argument that prints instead of pprinting. (Rob Browning)
* Always send diagnostic messages to standard error. (Rob Browning)
* Add project coordinate data to jar metadata. (Conor McDermottroe)
S
## 2.8.1 / 2017-10-27

* Fix a bug where `lein help` couldn't list built-in tasks on Java 9. (Phil Hagelberg)
* Fix a bug where `lein` installed from package managers would obscure exit code. (Phil Hagelberg)
* Fix an errant reflection warning on Java 9. (Toby Crawley)
* Fix an error when no `:plugins` are specified. (Phil Hagelberg)
* Fix a bug where launching project subprocesses would encounter unreadable forms. (Phil Hagelberg)
* Remove auto-setting of cgroups memory limit. (Florian Anderiasch)

## 2.8.0 / 2017-10-17

* Support `LEIN_USE_BOOTCLASSPATH` for users on Java 8. (Phil Hagelberg)
* Disable bytecode verification in Leiningen's own JVM for Java 9 compatibility. (Ghadi Shayban)
* Infer values for pom `<scm>` tag from `.git` directory. (Nicolas Berger)

## 2.8.0-RC1 / 2017-09-18

* Project middleware, hooks, and the `:test` profile are considered deprecated. (Phil Hagelberg)
* Help output no longer includes TOC output. (Irina Renteria)
* The `vcs` task allows the commit message to be customized. (Toby Crawley)
* JVMs on 8u131 and newer will default to using cgroups settings for memory limits. (Phil Hagelberg)
* Add `:query` subcommand to `deps` to quickly find latest versions. (Phil Hagelberg)
* Fix a bug where dependency resolution wasn't cached correctly. (Phil Hagelberg)
* Support for HTTP nREPL has been moved out; requires drawbridge plugin now. (Phil Hagelberg)
* Warn when `$CLASSPATH` is set. (Phil Hagelberg)
* Default to requiring TLS for remote repositories. (Phil Hagelberg)
* Remove warning when running as root. (Phil Hagelberg)
* Add `:why` subtask to `deps` for tracing individual deps. (Phil Hagelberg)
* Remove clj-http and cheshire dependencies, reducing likelihood of conflict. (Phil Hagelberg)
* Warn when plugin dependencies conflict with Leiningen's own. (Phil Hagelberg)
* Fix a bug where repls outside a project were not run in Leiningen's own process. (Phil Hagelberg)
* Add `:plugin-tree` and `:tree-data` subtasks to `deps`. (Ken Restivo)
* Support skipping bootclasspath for Java 9 compatibility. (Phil Hagelberg)
* Allow `vcs` task to skip signing tags. (Nicolas Berger)
* The `search` task no longer downloads indices but hits live search APIs. (Phil Hagelberg)
* Remove duplicate exclusions in `lein deps`. (Emlyn Corrin)
* Leiningen is now installable again via chocolatey. (Florian Anderiasch)
* Dependency names can be specified as strings in addition to symbols. (Wes Morgan)

## 2.7.1 / 2016-09-22

* Add support for SDKMAN! as installation alternative. (Jean Niklas L'orange)
* Improved explanation in some errors. (Jean Niklas L'orange)
* Don't require `nil` for version in managed deps. (Chris Price)
* Fix a bug with snapshot dependencies for managed deps. (Chris Price)

## 2.7.0 / 2016-08-24

* Add PowerShell script for Windows users. (Brian Lalonde)
* Run `:prep-tasks` before `lein test`, so generated test namespaces will be tested. (Martin Reck)
* Better error message when attempting to do `lein run` without `project.clj`. (Eduardo Seabra Silva)
* Add support for `:managed-dependencies`. (Chris Price)
* Provide the current clojars certificate. (Toby Crawley)
* Add `*eval-print-dup*` to evaluate forms passed to `eval-in-leiningen` with `*print-dup*`. (Eduardo Seabra Silva)
* Update bash completions. (Zack Dever)
* Respect `:scm :dir` in `lein vcs` commands. (Ian Kerins)
* Improve whitespace handling from `JVM_OPTS`. (Stephen Nelson)
* Catch and handle fixture errors during `lein test`. (Alex Hall)
* Fix a bug where spaces in directory names on Windows caused crashes. (Leon Mergen, Tobias Kiertscher, Jean Niklas L'orange)
* Fix a bug where `lein search` would take forever downloading clojars.org. (Paul Dorman)
* Retain user defined private repositories when building jars, uberjars and deploy. (Rick Moynihan)
* Honor whitelist settings when `lein javac` is called via `lein jar`. (Chris Price)
* `lein vsc push` for git will now only push branch-related tags. (Łukasz Klich)

## 2.6.1 / 2016-02-08

* Fix a bug where some plugins crashed when used. (Jean Niklas L'orange)

## 2.6.0 / 2016-02-05

* The templates, repl and Leiningen itself now use Clojure 1.8.
* Support for Clojure 1.1.0 and older is now dropped.
* Warn if possibly stale native dependencies end up in `:native-path`. (Jean Niklas L'orange)
* Speed up restarts after `:dependency` changes. (Jean Niklas L'orange)
* `lein release` now supports SNAPSHOT on qualifiers. (Chris Price)
* Synchronise `lein-pkg` and `lein` scripts. (Thu Trang Pham)
* Decrease timeout for the Clojure compiler agent thread pool. (Ryan Fowler)
* Fix a bug where implicit resource directories were created by default. (Jean Niklas L'orange)
* Avoid optimizing away stack traces by default. (solicode)
* Fix a bug where duplicate profiles were merged when profile merging. (Jean Niklas L'orange)
* Improved GPG artifact signing feedback. (Jean Niklas L'orange, Andrea Richiardi)
* Add function to support binary files with `lein-new`. (Sergiy Bondaryev)
* Show better error message when java is not found on the path. (Pavel Prokopenko, Jürgen Hötzel)
* Fix a bug with non-GitHub SCM urls in pom files. (Ralf Schmitt)
* Don't send aot warning if `:aot` contains regex matching the main namespace. (Emlyn Corrin)

## 2.5.3 / 2015-09-21

* Add CHANGELOG.md to default lein templates. (Daniel Compton)
* `lein vcs tag` now supports the `--no-sign` flag. (Daniel Compton)
* Fix a bug where javac errors were not printed to terminal. (Brandon Shimanek)
* Fix a bug where environment variables were not propagated down to GPG. (Brandon Shimanek)
* `lein retest` now saves information on which tests that fail. (Shalaka Patil)
* `lein release` now honors exit codes from `git` and throws if non-0 occurs. (Tim Visher)

## 2.5.2 / 2015-08-09

* Allow repl dependencies to be specified in default user profiles. (Jean Niklas L'orange)
* Fix a bug where transitive dependencies on tools.nrepl failed. (Jean Niklas L'orange)
* Fix a bug preventing custom certificates to work. (Jean Niklas L'orange)
* Add support for reader conditional files. (Stephen Nelson)
* Add `--template-version` flag to `lein new`. (Ohta Shogo)
* Bail immediately if snapshot dependencies are discovered during uberjaring. (Justin Smith)
* Use powershell by default in `lein.bat`. (Frederick Giasson, Florian Anderiasch)
* Fix bug where manifest files could contain duplicate entries. (Michael Blume)
* Allow template designers to use a custom rendering function. (Dmitri Sotnikov)
* Fix a bug where `:uberjar-name` wasn't used when inside the `:uberjar` profile. (Kyle Harrington)

## 2.5.1 / 2015-01-09

* No longer skip certificate checking when upgrading on Windows. (Phil Hagelberg)
* Fix password prompt for Cygwin users. (Carsten Behring)
* Fix a bug where `lein pom` did not add the project's SCM URL to pom.xml. (Fredrick Giasson)
* `lein clean` now cleans up all profile targets. (Jeb Beich, Jim Crossley)
* The order included profiles are merged in is now retained. (Jim Crossley)
* Fix a bug preventing `update-in` to use functions not yet required. (Phil Hagelberg)
* Allow multiple `:repl` profiles. (Hugo Duncan)
* Fix an infinite recursion bug with aliases and `with-profile`. (Hugo Duncan)
* Add flexibility in jar manifest declarations. (Fabio Tudone)
* Fix a bug preventing extra profiles from being included in jars. (Hugo Duncan)
* Fix a bug in self-install on Windows. (Sindunata Sudarmaji)

## 2.5.0 / 2014-09-14

* Allow certain profiles to be `^:leaky` and affect downstream. (Hugo Duncan)
* Allow profiles to be loaded out of plugins. (Phil Hagelberg, Hugo Duncan)
* Make `leiningen.core.project/read` init the project and merge default profiles. (Phil Hagelberg)
* Move auto-clean to jar task for consistency. (Phil Hagelberg)
* Make compilation messages honor `$LEIN_SILENT` (Jean Niklas L'orange)
* Fix a bug around EOF in the repl. (Colin Jones)
* Add `:implicits` subtask to `deps` task. (Phil Hagelberg)
* Update zsh completion rules. (Joshua Davey)
* Fix a stack overflow with :pedantic. (Nelson Morris)
* Fix a bug where repls outside of a project were broken. (Phil Hagelberg)

## 2.4.3 / 2014-08-05

* Allow implicit hooks/middleware to be disabled. (Phil Hagelberg)
* Print compilation errors as they occur. (Paul Legato)
* Switch Central repository to using HTTPS. (Manfred Moser)
* Add `LEIN_NO_USER_PROFILES` to avoid loading user profile. (Hugo Duncan)
* Fix deploy task to work with signature files. (Phil Hagelberg)
* Allow vcs tags to be created with a prefix. (Yannick Scherer)
* Default to warning when version ranges are present. (Phil Hagelberg)
* Let templates be loaded from `:plugin-repositories`. (Jason Felice)

## 2.4.2 / 2014-06-15

* Fix a bug preventing out-of-project runs. (Phil Hagelberg)
* Only load Clojars SSL cert on-demand to improve boot time. (Phil Hagelberg)

## 2.4.1 / 2014-06-15

* Don't commit untracked files in `lein vcs commit`. (Phil Hagelberg)
* Fix a bug where `:mirrors` could not be configured. (Phil Hagelberg)
* Expose `pom.properties` for access to version number during development. (Phil Hagelberg)
* Fix a bug preventing the release task from loading. (Phil Hagelberg)

## 2.4.0 / 2014-06-09

* Allow aliases to splice in values from the project map. (Phil Hagelberg)
* Allow plugins to override built-in tasks. (Phil Hagelberg)
* Add `release` task for automating common release steps. (Wayne Warren, Chris Truter, Phil Hagelberg)
* Add `change` task for programmatc `project.clj` manipulation. (Chris Truter, Max Barnash)
* Abort when `defproject` contains duplicate keys. (Peter Garbers)
* Add `vcs` task to automate version control. (Phil Hagelberg, Wayne Warren)
* Automatically `clean` before `deploy` to avoid AOT in libraries. (Phil Hagelberg)
* Emit warnings to stderr. (Andy Chambers)
* Use `clojure.main` for uberjars that don't declare their own `:main`. (Phil Hagelberg)
* Allow templates to load from `:plugin-repositories`. (Phil Hagelberg)
* Fix a race condition on printing during dependency resolution. (Phil Hagelberg)
* Allow `new` templates to operate on existing directories with `--force` option. (Matthew Blair)
* Fix `search` task to allow queries on multiple fields. (Colin Jones)
* Fix a bug where errors in `run` task were mis-reported. (Gary Fredericks)
* Report download progress of search indices. (Matthew Blair)
* Protection from harmful `:clean-targets` settings. (Craig McDaniel)
* Faster loading of help text. (David Grayson, Ryan Mulligan)
* Add `LEIN_SILENT` option to suppress `*info*` output. (Phil Hagelberg)

## 2.3.4 / 2013-11-18

* Suggest `:exclusions` to possibly confusing `:pedantic?` dependencies. (Nelson Morris, Phil Hagelberg)
* Optionally look for snapshot templates in `new` task. (Travis Vachon)
* Allow task chains to be declared without commas in project.clj. (Jean Niklas L'orange)
* Support extra configurability in `:pom-plugins`. (Dominik Dziedzic)
* Fix a bug where implicit :aot warning triggered incorrectly. (Jean Niklas L'orange)
* Fix a bug where `lein repl connect` ignored port argument. (Toby Crawley)

## 2.3.3 / 2013-10-05

* Add support for `:uberjar-merge-with`. (Marshall Bockrath-Vandegrift)
* Better error message for `-m` arg in `run` task. (Aleksandar Simic)
* Support stdin when using `:eval-in :nrepl`. (Phil Hagelberg)
* Add directory entries to jar files. (Vadim Platonov)
* Fix a bug where `-main` was hard-coded to initial directory. (Phil Hagelberg)

## 2.3.2 / 2013-08-19

* Write `.nrepl-port` file for better tool interoperability. (Phil Hagelberg)
* Support targeted upgrades in `lein.bat`. (Shantanu Kumar)
* Warn when projects rely on implicit AOT of `:main`. (Phil Hagelberg)
* Fix a bug where implicit AOT of `:main` was disabled. (Phil Hagelberg)
* Disable profile isolation by default. Will be back in 3.x. (Phil Hagelberg)

## 2.3.1 / 2013-08-13

* Fix self-install bug. (Sam Aaron, Steven Harms)
* Fix bug with AOT classes not being included in jars. (Phil Hagelberg)
* Support disabling test task's monkeypatch of `clojure.test`. (Phil Hagelberg)
* Allow project map to be readable. (Phil Hagelberg)

## 2.3.0 / 2013-08-08

* Add `:eval-in :pprint` for debugging. (Phil Hagelberg)
* Support cleaning extra dirs with `:clean-targets`. (Yoshinori Kohyama)
* Test-selectors skip fixtures too, not just running tests. (Gary Fredericks)
* Place licenses and readmes into jars. (Phil Hagelberg)
* Include LICENSE as separate file in templates. (Wolodja Wentland)
* Allow aborting on ambiguous version resolution with `:pedantic`. (Nelson Morris, Phil Hagelberg)
* Scope `:compile-path` and `:native-path` under profile-specific target dir. (Phil Hagelberg)
* Fix bug where uberjar filename would include provided profile. (Phil Hagelberg)
* Deprecate explicit `self-install` command. (Phil Hagelberg)
* Fix bugs around long lines in jar manifests. (Leon Barrett)
* Support nested checkout dependencies. (Phil Hagelberg)
* Fix bugs around `:filespecs`. (Jean Niklas L'orange)

## 2.2.0 / 2013-05-28

* Support setting custom welcome message when repl connects. (Colin Jones)
* Fix a bug where old template versions were always fetched. (Nelson Morris)
* Support `:java-agents` for tooling and instrumenting. (Phil Hagelberg)
* Allow checkout dependencies to operate recursively. (Phil Hagelberg)
* Introduce `:uberjar` profile. (Phil Hagelberg)
* Isolate target paths by profiles. (Phil Hagelberg)
* Support deploying ad-hoc files. (Phil Hagelberg)
* Set `*command-line-args*` in run task. (Anthony Grimes)
* Allow templates to specify executable files. (Joe Littlejohn)
* Remove clojuredocs repl support to reduce dependency conflicts. (Phil Hagelberg)

## 2.1.3 / 2013-04-12

* Fix fast trampoline to work without user profiles. (Malcolm Sparks)
* Fix a bug where duplicate files in jars would blow up. (Phil Hagelberg)
* Fix a bug where cyclical dependencies could cause a crash. (Nelson Morris)
* Allow aliases to have docstrings. (Jean Niklas L'orange)
* Read credentials from GPG for mirrors. (bdollard)
* Fix bugs in `update-in` around profiles and more. (Marko Topolnik)

## 2.1.2 / 2013-02-28

* Add new way to specify no-proxy hosts. (Joe Littlejohn)
* Allow TieredCompilation to be disabled for old JVMs. (Phil Hagelberg)
* Fix a bug merging keywords in profiles. (Jean Niklas L'orange)
* Fix a bug where tests wouldn't run under with-profiles. (Phil Hagelberg)
* Support for calling set! on arbitrary vars on startup. (Gary Verhaegen)
* Allow update-in to work on top-level keys. (Marko Topolnik)
* Fix a bug breaking certain templates. (Colin Jones)
* Fix a bug where trampolined repl would hang. (Marko Topolnik)

## 2.1.1 / 2013-03-21

* Add `:test-paths` to directories shared by checkout deps. (Phil Hagelberg)
* Allow `run` task to function outside projects. (Phil Hagelberg)
* Fix a bug preventing `with-profiles` working outside projects. (Colin Jones)
* Fix a bug in trampolined `repl`. (Colin Jones)
* Fix a bug in `update-in` task causing stack overflow. (David Powell)
* Fix a bug in `lein upgrade`. (Phil Hagelberg)

## 2.1.0 / 2013-03-19

* Compile task accepts regexes as command-line args. (Joshua P. Tilles)
* Allow key to be specified to use when signing artifacts. (Tim McCormack)
* Added GPG introductory guide. (Toby Crawley)
* Many bug fixes in batch file launcher. (David Powell)
* Self install via lein.bat no longer requires curl/wget. (slahn)
* Allow stdin of project processes to be closed. (Jean Niklas L'orange)
* Better behaviour when GPG or keys are missing. (Toby Crawley)
* Support customizing key-managers for SSL. (Stephen Nelson)
* Add update-in task for arbitrary project map changes. (Phil Hagelberg)
* Warn when version ranges are detected. (Nelson Morris)
* Add support for msys on Windows machines. (megri)
* Allow use of :mirrors when building jars/uberjars. (Tim McCormack)
* Dependencies may include native components more flexibly. (Marc Liberatore)
* Implement system-level profiles. (Phil Hagelberg)
* Accept repo credentials on the CLI for deploy. (Max Prokopiev)
* Fix a bug breaking recursive aliases. (Hugo Duncan)
* Add support for preventing deployment of branches. (Anthony Grimes)
* Improve boot time by limiting tiered compilation in dev. (Phil Hagelberg)
* Allow building jars with classifiers. (Hugo Duncan)
* Allow :init-ns to be honored by other nrepl clients. (Marko Topolnik)
* Add experimental support for :eval-in :nrepl. (Phil Hagelberg)
* Don't follow symlinks in clean task. (Jean Niklas L'orange)
* Add support for ~/.lein/profiles.d. (Jean Niklas L'orange)
* Allow ctrl-c to interrupt repl input (Colin Jones)
* Allow `lein test` to take files as arguments (Gabriel Horner)

## 2.0.0 / 2013-01-19

* Allow implicit repl profiles to be overridden.
* Accept `:main` as an alias for `-m` in `run` task.
* Reader fixes for `repl`. (Colin Jones, Chas Emerick)
* Fix bug around stdin for subprocesses that have stopped. (Jean Niklas L'orange)
* Warn when `:user` profile is found in `project.clj`. (Michael Grubb)
* Treat `:user` profile as project map outside of project. (Jean Niklas L'orange)

## 2.0.0-RC2 / 2013-01-12

* Fix bug where newnew wouldn't be loaded from outside a project.
* Fix Windows bug in project generation.
* Fix `lein upgrade` bug.

## 2.0.0-RC1 / 2013-01-10

* Fix some reader bugs in repl task. (Colin Jones)
* Fix a bug where Leiningen's deps could affect javac. (Jean Niklas L'orange)
* Test selectors may allow entire namespaces to be skipped. (Anthony Grimes)
* Allow project's git repo to be different than project root. (David Greenberg)
* Don't AOT the `:main` namespace outside of jar/uberjar task.
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
