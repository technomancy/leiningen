# Deploying Libraries

Getting your library into [Clojars](http://clojars.org) is fairly
straightforward as is documented near the end of
[the Leiningen tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md).
However, deploying elsewhere is not always that straightforward.

## Private Repositories

There may be times when you want to make a library available to your
team without making it public. This is best done by setting up a
private repository. There are several types of repositories.

### Static HTTP

The simplest kind of private repository is a web server pointed at a
directory full of static files. You can use a `file:///` URL in your
`:repositories` to deploy that way if the directory is local to the
machine on which Leiningen is running.

### SCP

If you already have a server set up with your SSH public keys, the
`scp` transport is a simple way to publish and consume private
dependencies. Place the following inside `defproject`:

```clj
:plugins [[org.apache.maven.wagon/wagon-ssh-external "2.6"]]
:repositories [["releases" "scp://somerepo.com/home/repo/"]]
```

Then place the following outside the `defproject`:

```clj
(cemerick.pomegranate.aether/register-wagon-factory!
 "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
          (clojure.lang.Reflector/invokeConstructor c (into-array []))))
```

It's also possible to deploy to a repository using the `scp` transport
and consume from it over `http` if you set up nginx or something
similar to serve the repository directory over HTTP.

N.B. SCP deploys to Clojars are no longer supported.

### S3

If you don't already have a server running,
[Amazon S3](http://aws.amazon.com/s3/) is a low-maintenance choice;
you can deploy to S3 buckets using the
[S3 wagon private](https://github.com/technomancy/s3-wagon-private) plugin.

### Artifactory/Nexus/Archiva

The most full-featured and complex route is to run a full-fledged
repository manager. Both [Artifactory](http://www.jfrog.com/open-source/#os-arti), [Archiva](http://archiva.apache.org/) and
[Nexus](http://nexus.sonatype.org/) provide this. They also proxy to
other repositories, so you can set `^:replace` metadata on
`:repositories` in project.clj, and dependency downloads will speed up
by quite a bit since Clojars and Maven Central won't need to be
checked.

The private server will need to be added to the `:repositories`
listing in project.clj. Artifactory, Archiva and Nexus offer separate repositories
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

### Other Non-standard Repository Protocols

If you are deploying to a repository that doesn't use one of the
standard protocols (`file:`, `http:`, `https:`), you may need to
provide a wagon factory for that protocol. You can do so by specifying
the wagon provider as a plugin dependency:

```clj
:plugins [[org.apache.maven.wagon/wagon-webdav-jackrabbit "2.4"]]
```

then registering a wagon factory function at the bottom of your project.clj:

```clj
(cemerick.pomegranate.aether/register-wagon-factory! "dav"
  #(eval '(org.apache.maven.wagon.providers.webdav.WebDavWagon.)))
```

This step is unnecessary for plugins that include explicit Leiningen
support like
[S3 wagon private](https://github.com/technomancy/s3-wagon-private)
and [lein-webdav](https://github.com/tobias/lein-webdav) as these declare
their wagons in ways that can be inferred automatically.

## Authentication

Deploying and reading from private repositories needs authentication
credentials. Check your repository's documentation for details, but
you'll usually need to provide a `:username` and `:password` or
`:passphrase`. Leiningen will prompt you for a password if you haven't
set up credentials, but it's convenient to set it so you don't have to
re-enter it every time you want to deploy. You will need
[gpg](http://www.gnupg.org/) installed and a key pair configured.  If
you need help with either of those, see the
[GPG guide](https://github.com/technomancy/leiningen/blob/stable/doc/GPG.md).

### GPG

If you specify a `:creds :gpg` entry in one of your `:repositories` settings
maps, Leiningen will decrypt `~/.lein/credentials.clj.gpg` and use that to find
the proper credentials for the given repository.

```clj
:repositories [["releases" {:url "http://blueant.com/archiva/internal"
                            :creds :gpg}]]
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
periodically; it will keep it cached for a given period.

Note to windows users: Be sure to download the full version of 
[Gpg4win](http://gpg4win.org/download.html) and
select Gpg for installation. You then need to run 
`gpg-connect-agent /bye` from the command line before starting lein.

### Full-disk Encryption

If you use full-disk encryption, it may be safe to store your
credentials without using GPG. In this case, you can create an `:auth`
profile containing a `:repository-auth` key mapping URL regexes to
credentials. Your `~/.lein/profiles.clj` file would look something
like this:

```clj
{:user {...}
 :auth {:repository-auth {#"blueant" {:username "milgrim"
                                      :password "locative1"}}}}
```

### Credentials in the Environment

Unattended builds can specify `:env` instead of `:gpg` in the
repository specification to have credentials looked up in the
environment. For example, specifying `:password :env` will cause
Leiningen to look up `(System/getenv "LEIN_PASSWORD")` for that value.
You can control which environment variable is looked up for each value
by using a namespaced keyword, like so:

```clj
:repositories [["releases" {:url "http://blueant.com/archiva/internal"
                            :username :env/archiva_username
                            :passphrase :env/archiva_passphrase}]]
```

Finally, you can opt to load credentials from the environment _or_ GPG credentials
by using a vector of `:gpg` and `:env/*` values to define the priority of each:

```clj
:repositories [["releases" {:url "http://blueant.com/archiva/internal"
                            :username [:gpg :env/archiva_username]
                            :passphrase [:gpg :env/archiva_passphrase]}]]
```

In this example, both `:username` and `:password` will be looked up in
`~/.lein/credentials.clj.gpg` first, and only if a value is not available there will
the `ARCHIVA_*` env vars be checked.  This allows you to avoid creating profiles
just to use different credential sources in e.g. a local development environment
vs. a centralized build environment.

## Deployment

Once you've set up a private repository and configured project.clj
appropriately, you can deploy to it:

    $ lein deploy [repository-name]

If the project's current version is a `SNAPSHOT`, it will default to deploying
to the `"snapshots"` repository; otherwise it will default to `"releases"`. In
order to make `lein deploy` with no argument target Clojars, include this in
your `project.clj`:

```clj
{:deploy-repositories [["releases" :clojars]]}
```

You can use this to alias any `:repositories` entry; Clojars is just the most
common use case.

## Releasing Simplified

Once you have your repositories and user credentials configured for deploying,
much of the work involved in actually deploying a release version can be tedious
and difficult to perform in a consistent fashion from one release to the next.
To simplify the release process, there is a `lein release [$LEVEL]` task where
`$LEVEL` can be refer to any of `:major`, `:minor`, `:patch`, `:alpha`, `:beta`,
or `:rc`. The simplification lies in the list of `:release-tasks` that get run
on each call to `lein release`. For example, suppose that your `project.clj`
starts off as follows:

```clojure
(defproject leiningen "2.4.0-SNAPSHOT" ...)
```

Using the default `:release-tasks` and the following command line:

    $ lein release :patch

The following events will happen:

1. The `change` task is run to remove whatever qualifier is currently on
   the version in `project.clj`. In this case, `project.clj` should
   look something like ```(defproject leiningen "2.4.0" ...)```.

2. `vcs` tasks will be run to commit this change and then tag the repository
   with the `release` version number.

3. The `deploy` task will be the same as if `lein deploy` had been run from the
   command line. **NOTE** This will require a valid `"releases"` entry either in
   `:deploy-repositories` or `:repositories`

4. The `change` task is run once more to "bump" the version number in
   `project.clj`. Which version level is decided by the argument
   passed to `lein release`, in this case `:patch`. Afterword, `project.clj` will
   look something like ```(defproject leiningen "2.4.1-SNAPSHOT" ...)```.

5. Finally, `vcs` tasks will be run once more to commit the new change to
   `project.clj` and then push these two new commits to the default remote
   repository.

The release process will fail if there are uncommitted changes.

### Overriding the default `:release-tasks`

You can use the `lein-pprint` plugin to see the default value of `:release-tasks`:

```
$ lein pprint :release-tasks
[["vcs" "assert-committed"]
 ["change" "version" "leiningen.release/bump-version" "release"]
 ["vcs" "commit"]
 ["vcs" "tag"]
 ["deploy"]
 ["change" "version" "leiningen.release/bump-version"]
 ["vcs" "commit"]
 ["vcs" "push"]]
```

This `:release-tasks` value can be overridden in `project.clj`. An example might
be a case in which you want the default workflow up to `lein deploy` but don't
want to automatically bump the version in `project.clj`:

```clojure
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]]
```

The `:release-tasks` vector should have every element be either a task name or a
collection in which the first element is a task name and the rest are arguments
to that task, just like `:prep-tasks` or `:aliases` entries.

Of course, `:release-tasks` doesn't have to look anything like the
default, the default is just an assumed convention among most Clojure
libraries using Leiningen. Applications will have different requirements
that are varied enough that Leiningen doesn't attempt to support them
out of the box.

## Deploying to Maven Central

Deploying your libraries and other artifacts to [Maven
Central](http://search.maven.org/) is often desirable.  Most tools that
use the Maven repository format (including Leiningen, Gradle, sbt, and
Maven itself) include Maven Central or one of its mirrors as a default
repository for resolving project dependencies.  So deploying your
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
touching those tools).  Note that all artifacts you deploy to Sonatype OSS will
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

3. Add the Sonatype OSS deployment repository endpoints to your project.clj, e.g.:
```clojure
:deploy-repositories [["releases" {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                   :creds :gpg}
                       "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/"
                                    :creds :gpg}]]
```

4. Conform to Sonatype OSS' requirements for uploaded artifacts' `pom.xml` files;
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
