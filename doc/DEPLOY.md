# Deploying Libraries

Getting your library into [Clojars](http://clojars.org) is fairly
straightforward as is documented near the end of
[the Leiningen tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md).
However, deploying is not always that straightforward.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private repository. The simplest kind of private repository is an
[Amazon S3](http://aws.amazon.com/s3/) bucket. You can deploy to S3
buckets using [S3 wagon private](https://github.com/technomancy/s3-wagon-private).

Alternatively you can run a private repository on your own server.
Both [Archiva](http://archiva.apache.org/) and
[Nexus](http://nexus.sonatype.org/) provide this as well as proxying
to other repositories, so you can set `:omit-default-repositories` in
project.clj, and dependency downloads will speed up by quite a bit
with only one server to check.

The private server will need to be added to the `:repositories`
listing in project.clj. Archiva and Nexus offer separate repositories
for snapshots and releases, so you'll want two entries for them:

```clj
:repositories {"snapshots" "http://blueant.com/archiva/snapshots"
               "releases" "http://blueant.com/archiva/internal"}
```

If you are are deploying to a repository that is _only_ used for deployment
and never for dependency resolution, then it should be specified in a
`:deploy-repositories` slot instead of included in the more general-purpose
`:repositories` map; the former is checked by `lein deploy` before the latter.
Deployment-only repositories useful across a number of locally developed
projects may also be specified in the `:user` profile in `~/.lein/profiles.clj`:

```clj
{:user {:deploy-repositories {"internal" "http://blueant.com/archiva/internal"}}}
```

## Authentication

Deploying and reading from private repositories needs authentication
credentials. Check your repository's documentation for details, but
you'll usually need to provide a `:username` and `:password` or
`:passphrase`. Leiningen will prompt you for a password if you haven't
set up credentials, but it's convenient to set it so you don't have to
re-enter it every time you want to deploy. You will need
[gpg](http://www.gnupg.org/) installed and a key pair configured.

If you specify `:gpg` in one of your `:repositories` settings maps,
Leiningen will decrypt `~/.lein/credentials.clj.gpg` and use that to
find the proper credentials for the given repository.

```clj
:repositories {"releases" {:url "http://blueant.com/archiva/internal"
                           :username "milgrim" :password :gpg}}
```

First write your credentials map to `~/.lein/credentials.clj` like so:

```clj
{#"blueant" {:password "locative1"}
 #"https://clojars.org/repo"
 {:username "milgrim" :password "locative1"}
 "s3p://s3-repo-bucket/releases"
 {:username "AKIAIN..." :passphrase "1TChrGK4s..."}}
```

Then encrypt it with `gpg`:

    $ gpg --default-recipient-self -e \
        ~/.lein/credentials.clj > ~/.lein/credentials.clj.gpg

Remember to delete the plaintext `credentials.clj` once you've
encrypted it. Due to a bug in `gpg` you currently need to use
`gpg-agent` and have already unlocked your key before Leiningen
launches, but with `gpg-agent` you only have to enter your passphrase
once per login.

On some systems you will be prompted for your GPG passphrase if you
haven't entered it. If yours does not, you can install
[Keychain](https://github.com/funtoo/keychain), which provides this
functionality portably.

Unattended builds can specify `:env` instead of `:gpg` in the
repository specification to have credentials looked up in the
environment. For example, specifying `:password :env` will cause
Leiningen to look up `(System/getenv "LEIN_PASSWORD")` for that value.

## Deployment

Once you've set up a private repository and configured project.clj
appropriately, you can deploy to it:

    $ lein deploy [repository-name]

If the project's current version is a SNAPSHOT, it will default to
deploying to the `snapshots` repository; otherwise it will default to
`releases`.
