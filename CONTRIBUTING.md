# Contributing

Leiningen is the most widely-contributed-to Clojure project a the time
of this writing. We welcome potential contributors and do our best to
try to make it easy to help out.

Discussion occurs primarily in the #leiningen channel on [Libera
chat](https://libera.chat).

Please report issues on the
[issue
tracker](https://codeberg.org/leiningen/leiningen/issues). Issues used
to be reported in the [GitHub
tracker](https://github.com/technomancy/leiningen/issues) so you may
want to check there to see if things have already been reported.

Code submissions should be sent as [pull
requests](https://codeberg.org/leiningen/leiningen/pulls). Please
use topic branches when sending pull requests rather than committing
directly to `main` in order to minimize unnecessary merge commit
clutter. Direct pull requests towards the `main` branch, not the
stable branch.

Note: the canonical repository for Leiningen is [on
Codeberg](https://codeberg.org/leiningen/leiningen) but we maintain [a
mirror on GitHub](https://github.com/technomancy/leiningen) for the
time being in order to ease the transition. Please update your links
and git remotes.

## Codebase

The definitions of the various tasks reside in `src/leiningen` in the
top-level project. The underlying mechanisms for things like
`project.clj` parsing, classpath calculation, and subprocess launching
are implemented inside the `leiningen-core` subproject.

See the
[readme for the leiningen-core library](https://codeberg.org/leiningen/leiningen/src/main/leiningen-core/README.md)
and `doc/PLUGINS.md` for more details on how Leiningen's codebase is
structured.

Try to be aware of the conventions in the existing code, except the
one where we don't write tests. Make a reasonable attempt to avoid
lines longer than 80 columns or function bodies longer than 20
lines. Don't use `when` unless it's for side-effects. Don't introduce
new protocols. Use `^:internal` metadata to mark vars which can't be
private but shouldn't be considered part of the public API.

## Bootstrapping

You don't need to "build" Leiningen per se, but when you're developing on a
checkout you will need to get its dependencies in place and compile some of the
tasks. Assuming you are in Leiningen's project root, you can do that like this:

```bash
$ cd leiningen-core
$ lein bootstrap # or lein.bat on Windows.
```

The `lein` command is a stable release of Leiningen on your `$PATH` – preferably
the newest one. If you don't have a stable `lein` installed, simply check out
the `stable` branch and copy `bin/lein` to somewhere on your `$PATH`, then
switch your branch back.

If you want to use your development copy for everyday usage, symlink
`bin/lein` to somewhere on your `$PATH`. You'll want to rename your
stable installation to keep them from interfering; typically you can
name that `lein2` or `lein-stable`.

When dependencies in Leiningen change, you may have to do `rm .lein-classpath`
in the project root, though in most cases this will be done automatically. If
dependencies in leiningen-core change, you have to redo the `lein bootstrap`
step mentioned earlier.

Using `bin/lein` alone from the main branch without a full checkout
is not supported. If you want to just grab a shell script to work
with, use the `stable` branch.

### Uberjar from Master

Since a development version is not uberjared, it can be rather slow compared to
a stable release. If this is annoying and you depend on a recent fix or
enhancement, you can build an uberjar from main as follows:

```bash
# NB! You have to use *bin*/lein to build the uberjar
$ bin/lein uberjar
# ^ Last line printed from this command will tell the location of the standalone
$ cp target/leiningen-2.5.2-SNAPSHOT-standalone.jar $HOME/.lein/self-installs
$ cp bin/lein $HOME/bin/lein-main
```

Here, 2.5.2-SNAPSHOT is the version we've built, and we have `$HOME/bin` on our
$PATH.

Note that changes on main won't be visible in the uberjared version unless you
overwrite both the lein script and a freshly created uberjar.

## Tests

Before you submit a pull request, we would be very happy if you ensure
that the changes you've done doesn't break any of the existing test cases. While
there is a test suite, it's not terribly thorough, so don't put too much trust
in it. Patches which add test coverage for the functionality they change are
especially welcome.

To run the test cases, run `bin/lein test` in the root directory: This will test
both `leiningen-core` and `leiningen` itself. Do not attempt to run the tests
with a stable version of Leiningen, as the namespaces conflict and you may end
up with errors during the test run.
