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
dummy project with `lein new`. Edit `project.clj` to include your
`org.clojars.$USERNAME` group-id, the project's original artifact
name, and the version. Then you can use the output from `lein pom` to
upload to clojars.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private Maven repository.

The simplest way to do this is by hosting a repository on
[Amazon's S3 service](http://aws.amazon.com/s3/). You can use the
[s3-wagon-private](https://github.com/technomancy/s3-wagon-private)
plugin to deploy and consume from S3. Once you've signed up for an
Amazon account, create a "bucket" to contain your repositories.

It's best to keep snapshots and released versions separate, so add
both to the `:repositories` in `project.clj`; in this example the
"s3p" URL scheme is used for S3 private repositories:

```clj
:repositories {"releases" "s3p://mybucket/releases/"
               "snapshots" "s3p://mybucket/snapshots/"}
```

If you don't mind running your own server, both
[Archiva](http://archiva.apache.org/) and
[Nexus](http://nexus.sonatype.org/) will allow you to set up private,
password-protected repositories. These also provide proxying to other
repositories, so you can set `:omit-default-repositories` in
`project.clj`, and dependency downloads will speed up by quite a bit
with only one server to check.

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

Private repositories will need authentication credentials. You'll need
to provide a `:username` and `:password` or `:passphrase` depending on
the repository. In order to avoid putting sensitive information into
your `project.clj` file, you should store authentication information
in `~/.lein/init.clj` as a `leiningen-auth` map keyed off the
repository's URL:

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
`snapshots` repository; otherwise it will go to `releases`. The
`deploy` task also takes a repository name as an argument that will be
looked up in the `:deploy-repositories` and `:repositories` maps if
you want to override this.
