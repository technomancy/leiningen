# Contributing

Leiningen is the most active open-source Clojure project. We welcome
potential contributors and do our best to try to make it easy to help
out. Contributors who have had a single patch accepted may request
commit rights as well as a free [sticker](http://twitpic.com/2e33r1).

Discussion occurs both in the
[#leiningen channel on Freenode](irc://chat.freenode.net#leiningen)
and on the [mailing list](http://librelist.com/browser/leiningen/). To
join the mailing list, simply email `leiningen@librelist.org`; your
first message to that address will subscribe you without being posted.

Please report issues on the
[GitHub issue tracker](https://github.com/technomancy/leiningen/issues)
or the mailing list. Personal email addresses are inappropriate for
bug reports. Simpler issues appropriate for first-time contributors
looking to help out are tagged "newbie".

Patches are preferred as patches from `git format-patch` on the
mailing list or as GitHub pull requests. Please use topic branches
when sending pull requests rather than committing directly to master
in order to minimize unnecessary merge commit clutter.

Leiningen is [mirrored at Gitorious](https://gitorious.org/leiningen/leiningen)
and [tested on Travis](http://travis-ci.org/technomancy/leiningen).

## Codebase

The definitions of the various tasks reside in `src/leiningen` in the
top-level project. The underlying mechanisms for things like
`project.clj` parsing, classpath calculation, and subprocess launching
are implemented inside the `leiningen-core` subproject.

See the
[readme for the leiningen-core library](https://github.com/technomancy/leiningen/blob/master/leiningen-core/README.md)
and `doc/PLUGINS.md` for more details on how Leiningen's codebase is
structured.

While there is a test suite, it's not terribly thorough, so don't put
too much trust in it. Patches which add test coverage for the
functionality they change are especially welcome.

## Bootstrapping

You don't need to "build" Leiningen per se, but when you're using a
checkout you will need to get its dependencies in place.

Use Leiningen 1.x to run `lein1 install` in the `leiningen-core`
directory. If you don't have 1.x installed, simply check out the `1.x`
branch and copy `bin/lein` to `lein1` somewhere on your `$PATH`, then
switch your branch back. Alternately you can run `mvn
dependency:copy-dependencies` in the same directory followed by `cp -r
target/dependency lib`.

Once you've done that, symlink `bin/lein` to somewhere on your
`$PATH`. Usually you'll want to rename your existing installation to
keep them from interfering.

When the dependencies change you may have to do `rm .lein-classpath`
in the project root, though in most cases this can be done automatically.

Using `bin/lein` alone from the master branch without a full checkout
is not supported. If you want to just grab a shell script to work
with, use the `preview` branch.

