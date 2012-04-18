# Deploying Libraries

Getting your library into [Clojars](http://clojars.org) is fairly
straightforward as is documented near the end of
[the Leiningen tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md).
However, deploying is not always as straightforward as the process
described there.

## Clojars Forks

Sometimes you'll need to publish libraries that you don't directly
maintain, either because the original maintainer hasn't published it
or because you need some bugfixes that haven't been applied upstream
yet. Don't do this if it's at all possible to get the upstream project
to release a snapshot somewhere with the changes you need, but in some
cases it's unavoidable.

In this case you don't want to publish it under its original group-id,
since either you won't have permission to do so (if it's already on
Clojars) or it will conflict with the same artifact on other
repositories. You should use "org.clojars.$USERNAME" as the group-id
instead.

If it's a Clojure project that already has a project.clj file, it's
easy enough to just follow the regular <tt>lein jar, pom; scp
[...]</tt> path. If you don't have a readily-available pom, you can
create a dummy project with <tt>lein new</tt>. Edit project.clj to
include your org.clojars.$USERNAME group-id, the project's original
artifact name, and the version. Then you can use the output from
<tt>lein pom</tt> to upload to clojars.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private Maven repository. Both [Archiva](http://archiva.apache.org/)
and [Nexus](http://nexus.sonatype.org/) will allow you to set up
private, password-protected repositories. These also provide proxying
to other repositories, so you can set <tt>:omit-default-repositories</tt>
in project.clj, and dependency downloads will speed up by quite a bit
with only one server to check.

The private server will need to be added to the <tt>:repositories</tt>
listing in project.clj. Archiva and Nexus offer separate repositories
for snapshots and releases, so you'll want two entries for them:

```clj
:repositories {"snapshots" {:url "http://blueant.com/archiva/snapshots"
                            :username "milgrim" :password "locative.1"}
               "releases" "http://blueant.com/archiva/internal"}
```

If you are are deploying to a repository that is _only_ used for deployment
and never for dependency resolution, then it should be specified in a
`:deploy-repositories` slot instead of included in the more general-purpose
`:repositories` map; the former is checked by `lein deploy` before the latter.
Deployment-only repositories useful across a number of locally developed
projects may also be specified in the `settings` map in `~/.lein/init.clj`:

```clj
(def settings {:deploy-repositories { ... }})
```

### Authentication

Private repositories often need authentication credentials. You'll need to
provide either a <tt>:username</tt>/<tt>:password</tt> combination or
a <tt>:private-key</tt> location with or without a
<tt>:passwword</tt>. If you want to avoid putting sensitive
information into your project.clj file as in the <tt>releases</tt>
entry above, you can store authentication information in
<tt>~/.lein/init.clj</tt> as a <tt>leiningen-auth</tt> map keyed off
the repository's URL:

```clj
(def leiningen-auth {"http://localhost:8080/archiva/repository/internal/"
                     {:username "milgrim" :password "locative.2"}})
```

This also allows different users using the same checkout to upload
using different credentials.

### Deployment

Once you've set up a private repository and configured project.clj
appropriately, you can deploy to it:

    $ lein deploy

If the project's current version is a SNAPSHOT, it will deploy to the
<tt>snapshots</tt> repository; otherwise it will go to
<tt>releases</tt>. The <tt>deploy</tt> task also takes a repository
name as an argument that will be looked up in the
<tt>:deploy-repositories</tt> and <tt>:repositories</tt> maps
if you want to override this.
