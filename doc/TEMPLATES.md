# Writing Templates

Suppose you've written a fabulously popular library, used the world
over by adoring fans. For the purposes of this document, let's say
this library is called "liquid-cool". If using liquid-cool takes a bit
of setup, or if you'd just like to give your users a little guidance
on how one might best create a new project which uses liquid-cool, you
might want to provide a template for it (just like how `lein` already
provides built-in templates for "app", "plugin", and so on).

Let's assume your library's project dir is `~/dev/liquid-cool`. Create
a template for it like so:

    cd ~/dev
    lein new template liquid-cool --to-dir liquid-cool-template

Note that you'll now have a new and separate project named
"liquid-cool-template". It will have a group-id of "liquid-cool", and
an artifact-id of "lein-template".

> All lein templates have an artifact-id of "lein-template", and are
> differentiated by their group-id, which always should match the
> project for which they provide a template.

## Structure

The files that your template will provide to users are in
`src/leiningen/new/liquid_cool`. lein-newnew starts you off with just
one, named "foo.clj". You can see it referenced in
`src/leiningen/new/liquid_cool.clj`, right underneath the
`->files data` line.

You can delete `foo.clj` if you like (and it's corresponding line in
`liquid_cool.clj`), and start populating that
`src/leiningen/new/liquid_cool` directory with the files you wish to be
part of your template. For everything you add, make sure the
`liquid_cool.clj` file receives corresponding entries in that `->files`
call. For examples to follow, have a look inside [the \*.clj files for
the built-in
templates](https://github.com/technomancy/leiningen/tree/master/resources/leiningen/new).

While developing a template, if you're in the template project
lein-newnew will pick it up and you'll be able to test it. However, if
you want to use it on your system without putting it on clojars, just
`lein install` your template.

## Distributing your template

Templates are just maven artifacts. Particularly, they need only be on
the classpath when `lein new` is called. So, as a side-effect, you
can just put your templates in a jar and toss them on clojars and have
people install them like normal Leiningen plugins.

In Leiningen 2.x, templates get dynamically fetched if they're not
found. So for instance `lein new heroku myproject` will find the
latest version of the `heroku/lein-template` project from Clojars and
use that.

Users of Leiningen 1.x (1.6.2 or later) can also use the template if
they install the `lein-newnew` plugin:

    $ lein plugin install lein-newnew 0.3.6
    $ lein new foo
    $ lein new plugin lein-foo
