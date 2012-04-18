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
easy enough to just follow the regular `lein jar, pom; scp [...]`
path. If you don't have a readily-available pom, you can create a
dummy project with `lein new`. Edit project.clj to include your
`org.clojars.$USERNAME` group-id, the project's original artifact name,
and the version. Then you can use the output from `lein pom` to upload
to Clojars.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private Maven repository. Both [Archiva](http://archiva.apache.org/)
and [Nexus](http://nexus.sonatype.org/) will allow you to set up
private, password-protected repositories. These also provide proxying
to other repositories, so you can set `:omit-default-repositories`
in project.clj, and dependency downloads will speed up by quite a bit
with only one server to check.

The private server will need to be added to the `:repositories`
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

Private repositories often need authentication credentials. Check your
repository's documentation for details, but you'll usually need to
provide either a `:username`/`:password` combination or a
`:private-key` location with or without a `:passphrase`. Since you
should avoid putting sensitive information into your project.clj file
as in the `releases` entry above, authentication information is
looked up in the `:repository-auth` key of the `:auth` profile in
`~/.lein/profiles.clj`; see `lein help deploy` for further details.

```clj
{:user {:plugins [...]}
 :auth {:repository-auth {#"https://internal.repo/.*"
                          {:username "milgrim" :password "locative"}
                          "s3://s3-repo-bucket/releases"
                          {:username "AKIAIN..." :password "1TChrGK4s..."}}}}
```

This also allows different users using the same checkout to upload
using different credentials.

### Deployment

Once you've set up a private repository and configured project.clj
appropriately, you can deploy to it:

    $ lein deploy

If the project's current version is a SNAPSHOT, it will deploy to the
`snapshots` repository; otherwise it will go to `releases`. The
`deploy` task also takes a repository name as an argument that will be
looked up in the `:deploy-repositories` and `:repositories` maps if
you want to override this.
