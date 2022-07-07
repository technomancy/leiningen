<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Writing Templates](#writing-templates)
  - [Structure](#structure)
  - [Templating System](#templating-system)
      - [A warning about Mustache tag delimiters](#a-warning-about-mustache-tag-delimiters)
  - [Distributing your Template](#distributing-your-template)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Writing Templates

Suppose you've written a fabulously popular library, used the world
over by adoring fans. For the purposes of this document, let's say
this library is called "liquid-cool". If using liquid-cool takes a bit
of setup, or if you'd just like to give your users a little guidance
on how one might best create a new project which uses liquid-cool, you
might want to provide a template for it (just like how `lein` already
provides built-in templates for "app", "plugin", and so on).

Let's assume you have a library called "liquid cool" which lives on
Clojars as `us.technomancy/liquid-cool`. You can name your template
after your library. You could also use a different name, but note that
in order to comply with the new [Clojars
policies](https://github.com/clojars/clojars-web/wiki/Verified-Group-Names)
you can't create a new group-id unless you can verify ownership of it.

Create a template for it like so:

    lein new template us.technomancy/liquid-cool --to-dir liquid-cool-template

Your new template would look like:

    liquid-cool-template/
    ├── CHANGELOG.md
    ├── LICENSE
    ├── project.clj
    ├── README.md
    ├── resources
    │   └── leiningen
    │       └── new
    │           └── liquid_cool
    │               └── foo.clj
    └── src
        └── leiningen
            └── new
                └── liquid_cool.clj

Note that you'll now have a new and separate project named
"liquid-cool-template". It will have a group-id of "us.technomancy", and
an artifact-id of "lein-template.liquid-cool".

## Structure

The files that your template will provide to users are in
`resources/leiningen/new/liquid_cool`. The template generator
starts you off with just one, named "foo.clj". You can see it referenced in
`src/leiningen/new/liquid_cool.clj`, right underneath the
`->files data` line.

You can delete `foo.clj` if you like (and its corresponding line in
`liquid_cool.clj`), and start populating that
`resources/leiningen/new/liquid_cool` directory with the files
you wish to be part of your template. For everything you add, make sure the
`liquid_cool.clj` file receives corresponding entries in that `->files`
call. For examples to follow, have a look inside [the \*.clj files for the
built-in
templates](https://codeberg.org/leiningen/leiningen/tree/stable/resources/leiningen/new).

## Testing Your Template

While developing a template, if you're in the template project directory, 
leiningen will pick it up and you'll be able to test it.  e.g. from the
`liquid-cool-template` dir:

    $ lein new us.technomancy/liquid-cool myproject

will create a directory called `myproject`, built from your template.
Alternately, if you want to test your template from another directory on
your system (without publishing your template to clojars yet), just run:

    $ lein install

You should then be able to run `lein new us.technomancy/liquid-cool myproject`
from any directory on your system.

## Templating System

The default generated template uses [stencil][] for templating, which implements the
language-agnostic templating system [Mustache][]. All the available tag types
can be found in the [Mustache manual][mustache-manual]; we will only go through
the most common tag type here.

Suppose we want to add in a standard markdown readme file where the input name
is the main header of the file. To be able to do so, we must do two things:
Ensure that the input name is contained within the `data` mapped to the key X,
and that we have a template file which looks up the key X by wrapping it in
double mustaches like so: `{{X}}`. As for our input name, `data` already
contains the line `:name name`, which means we can lookup the input name by
writing `{{name}}` in the template file. To try it out, save the following
contents in the file `resources/leiningen/new/liquid_cool/README.md`:

```markdown
# {{name}}

This is our readme!
```

And add the following line right underneath the `->files data` line:

```clj
["README.md" (render "README.md" data)]
```

Now, if we for instance say `lein new us.technomancy/liquid-cool
liquid-cool-app`, the newly generated project will contain a file named
`README.md` where the header is `liquid-cool-app`.

[stencil]: https://github.com/davidsantiago/stencil
[Mustache]: https://mustache.github.io/
[mustache-manual]: https://mustache.github.io/mustache.5.html

#### A warning about Mustache tag delimiters
Clojure syntax can conflict with the default mustache tag delimiter. For 
example, when destructuring a nested map:

```clj
(let [{{:keys [a b]} :ab} some-map]
  (do-something a b))
```

Stencil will interpret the `{{` as the start of a mustache tag, but since the
contents are not valid mustache, the render fails. To get around this, we can 
change the mustache delimiter temporarily, like so:

```clj
{{! Change mustache delimiter to <% and %> }}
{{=<% %>=}}

(let [{{:keys [a b]} :ab} some-map]
  (do-something a b))

<%! Reset mustache delimiter %>
<%={{ }}=%>
```

## Distributing your Template

Templates are just maven artifacts, aka dependencies. Particularly,
they need only be on the classpath when `lein new` is called. So, as a
side-effect, you can just put your templates in a jar and toss them on
Clojars and have people install them like normal Leiningen
plugins. Templates get fetched on demand if they're not found. So
for instance `lein new com.heroku/hello myproject` will find the
latest version of the `com.heroku/lein-template.hello` project from
Clojars and use that.

## Legacy Templates

Prior to Leiningen 2.9.6, templates defaulted to using the template
name as the group-id and "lein-template" as the artifact-id. Changes
to [Clojars policy](https://github.com/clojars/clojars-web/wiki/Verified-Group-Names)
have necessitated using a new style where every template name includes
a group-id and an artifact-id. The template artifact takes the
group-id but prepends "lein-template." to the artifact-id given in the
template name.

The old style is still supported when using an existing template, but it is not
recommended for creating new templates.
