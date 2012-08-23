# Deploying Libraries

Getting your library into [Clojars](http://clojars.org) is fairly
straightforward as is documented near the end of
[the Leiningen tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md).
However, deploying is not always that straightforward.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private repository. The simplest kind of private repository is a web
server pointed at a directory full of static files. You can use a
`file:///` URL in your `:repositories` to deploy that way if the
directory is local to the machine on which Leiningen is running.
[Amazon S3](http://aws.amazon.com/s3/) buckets are another simple
choice; you can deploy to S3 buckets using
[S3 wagon private](https://github.com/technomancy/s3-wagon-private).

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
:repositories [["snapshots" "http://blueant.com/archiva/snapshots"]
               ["releases" "http://blueant.com/archiva/internal"]]
```

If you are are deploying to a repository that is _only_ used for deployment
and never for dependency resolution, then it should be specified in a
`:deploy-repositories` slot instead of included in the more general-purpose
`:repositories` map; the former is checked by `lein deploy` before the latter.
Deployment-only repositories useful across a number of locally developed
projects may also be specified in the `:user` profile in `~/.lein/profiles.clj`:

```clj
{:user {:deploy-repositories [["internal" "http://blueant.com/archiva/internal"]]}}
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
:repositories [["releases" {:url "http://blueant.com/archiva/internal"
                           :username "milgrim" :password :gpg}]]
```

First write your credentials map to `~/.lein/credentials.clj` like so:

```clj
{#"blueant" {:password "locative1"}
 #"https://clojars.org/repo"
 {:username "milgrim" :password "locative1"}
 "s3p://s3-repo-bucket/releases"
 {:username "AKIAIN..." :passphrase "1TChrGK4s..."}}
```

If you don't have a key pair yet, it's easy to generate one. The
defaults should serve you well, but be sure to pick a strong passphrase.

    $ gpg --gen-key

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
functionality portably. Your key will also be used for signing
artifacts if the version is not a snapshot, so you may be asked for
the passphrase multiple times if the agent is not configured. To
disable signing of releases, set `:sign-releases` to false in the
`:repositories` entry you are targeting.

Unattended builds can specify `:env` instead of `:gpg` in the
repository specification to have credentials looked up in the
environment. For example, specifying `:password :env` will cause
Leiningen to look up `(System/getenv "LEIN_PASSWORD")` for that value.

## Deployment

Once you've set up a private repository and configured project.clj
appropriately, you can deploy to it:

    $ lein deploy [repository-name]

If the project's current version is a `SNAPSHOT`, it will default to
deploying to the `snapshots` repository; otherwise it will default to
`releases`.

## Deploying to Maven Central

Deploying your libraries and other artifacts to [Maven
Central](http://search.maven.org/) is often desirable.  Most tools that
use the Maven repository format (including Leiningen, Gradle, sbt, and
Maven itself) include Maven Central or one of its mirrors as a default
repository for resolving project dependencies.  So, deploying your
libraries to Maven Central offers the widest distribution, especially if
your users are likely to be in languages other than Clojure.

Thankfully, Leiningen can deploy your libraries to Maven Central, with
a few additional bits of configuration.  All of the guidance about
deploying to private repositories laid out above applies; but, here's a
step-by-step recipe from start to finish:

1. Register an account and groupId on `oss.sonatype.org`; refer to
[this](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide)
for details on how to get registered (you can ignore most of the info on
that page regarding configuring Maven and/or ant, since we'll not be
touching those tools).  Note that all artifacts you deploy to OSS will
need to use the groupId(s) you choose, so your project coordinates
should be set up to match; e.g.:
```clojure
(defproject your.group.id/projectname "x.y.z" ...)
```

2. Add your credentials for `oss.sonatype.org` to your
`~/.lein/credentials.clj.gpg` file.  Something like this will do:
```clojure
{#"https://oss.sonatype.org/.*"
 {:username "username" :password "password"}}
```
Refer to the instructions earlier on this page for how to encrypt a
plain-text `credentials.clj` using GPG.

3. Add the OSS deployment repository endpoints to your project.clj, e.g.:
```clojure
:deploy-repositories [["releases" {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                   :creds :gpg}
                       "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/"
                                    :creds :gpg}]]
```

4. Conform to OSS' requirements for uploaded artifacts' `pom.xml` files;
all you need to do is make sure the following slots are populated
properly in your `project.clj`:
```clojure
  :description
  :url
  :license
  :scm
  :pom-addition
```
Examples of OSS-acceptable values for these entries can be seen in this
[`project.clj`
file](https://github.com/cemerick/piggieback/blob/master/project.clj).
Note that all of them should be appropriate for *your* project; blind
copy/paste is not appropriate here.

5. Run `lein deploy`.  Leiningen will push all of the files it would
otherwise send to Clojars or your other private repository to the proper
OSS repository (either releases or snapshots depending on whether your
project's version number has `-SNAPSHOT` in it or not).

6. If you're deploying a release, log in to `oss.sonatype.org`, and
close and release/promote your staged repository.  (This manual step
will eventually be automated through the use of a plugin.) The release
will show up in OSS' releases repository immediately, and sync to Maven
Central on the next cycle (~ 1-4 hours usually). 

