# Using GPG

This is an introduction to setting up and using
[GPG](http://www.gnupg.org/) keys with
[Leiningen](http://leiningen.org) to sign artifacts for publication to
[Clojars](http://clojars.org/) and to encrypt repository credentials.

There are two versions of GPG available: v1.x and v2.x. For our
purposes, they are functionally equivalent. Package managers generally
install v2.x as `gpg2`, and v1.x as `gpg`. By default, Leiningen
expects the GPG command to be `gpg`. You're welcome to use any version
you like, but this primer will only cover installing v1.x, and has
only been tested under v1.x.

## What is it?

[GPG](http://www.gnupg.org/) (or Gnu Privacy Guard) is a set of tools
for cryptographic key creation/management and encryption/signing of
data. If you are unfamiliar with the concepts of public key
cryptography, this
[Wikipedia entry](http://en.wikipedia.org/wiki/Public-key_cryptography)
serves as a good introduction.

An important concept to understand in public key cryptography is that
you are really dealing with two keys (a *keypair*): one public, the
other private (or secret). The public key can be freely shared, and is
used by yourself and others to encrypt data that only you, as the
holder of the private key, can decrypt. It can also be used to verify
the signature of a file, confirming that the file was signed by the
holder of the private key, and the contents of the file have not been
altered since it was signed. **You should guard your private key
and passphrase closely, and share them with no one.**

## Installing GPG

### Linux

##### Debian based distributions

Apt uses GPG v1, so it should already be installed. If not:

    apt-get install gnupg
    
#### Fedora based distributions

Fedora and friends may have GPG v2 installed by default, but GPG v1 is
available via:
    
    yum install gnupg
    
### Mac

There are several options here, depending on which package manager you
have installed (if any):

1. via [homebrew](http://mxcl.github.com/homebrew/): `brew install gnupg`
2. via [macports](http://www.macports.org/): `port install gnupg`
3. via a [binary installer](https://www.gpgtools.org/installer/index.html) 
   (this installs gpg2 as gpg)

### Windows

[GPG4Win](http://gpg4win.org/) provides a binary installer that
provides some possibly useful GUI tools in addition to providing the
`gpg` command.

## Creating a keypair

Create a keypair with:

    gpg --gen-key

This will prompt you for details about the keypair to be generated,
and store the resulting keypair in `~/.gnupg/`.

The default key type (RSA and RSA) is fine for our purposes, as is the
default keysize (2048). We recommend a validity period of 2 years. 

You'll be prompted for a passphrase to protect your private key - it's
important to use a strong one to help protect the integrity of your key.

## Listing keys

GPG stores keys in a keystore located in `~/.gnupg/`. This keystore
holds your keypair(s), along with any other public keys you have used.
    
To list all of the public keys in your keystore:

    gpg --list-keys
    
This will include any public keys you have used, including keys from
others (if you've never used GPG before and just created your first
keypair, you should just see your own key).

The output of the `--list-keys` option will include the id of your
public key in the 'pub' line in the key listing (you'll need that id 
for other commands described here):

    $ gpg --list-keys

                ↓↓↓↓↓↓↓↓
    pub   2048R/2ADFB13E 2013-03-16 [expires: 2014-03-16]
    uid                  Bob Bobson <bob@bobsons.net>
    sub   2048R/8D2344D0 2013-03-16 [expires: 2014-03-16]

The `--fingerprint` option will act just like `--list-keys`, but will
include the fingerprint of each certificate in the output. You can
filter the output of the `--fingerprint` option by providing a key id
or any substring from the uid (this trick also works for the
`--list-keys` option):

    $ gpg --fingerprint 2ADFB13E

    pub   2048R/2ADFB13E 2013-03-16 [expires: 2014-03-16]
          Key fingerprint = 3367 5FD0 D67B 3218 5815  51A3 97D4 06D0 2ADF B13E
    uid                  Bob Bobson <bob@bobsons.net>
    sub   2048R/8D2344D0 2013-03-16 [expires: 2014-03-16]

## Publishing your public key

To make it easier for others that need your public key to find it,
you can publish it to a key server with:

    gpg --send-keys 2ADFB13E # use your id instead

This pushes a copy of your public key to one of a cluster of free key
servers, and the key is propagated to all of the other servers in the
cluster in short order.

If your keypair is compromised, you can publish a revocation
certificate to the key server to let others know that your key can no
longer be trusted for any future signing or encryption. It's a good
idea to generate a revocation certificate whenever you create a new
keypair, and store it in a safe place. As long as you have that
revocation certificate, you can revoke a keypair even if you no longer
have the private key. You can generate a revocation certificate with:

    $ gpg --output 2ADFB13E-revoke.asc --gen-revoke 2ADFB13E

Be sure to protect your revocation certificate carefully - anyone who
gains access to it can use it to revoke your keypair. The GPG
maintainers recommend printing it out and storing the hardcopy in a
safe place.

To revoke your certificate **when the time comes (not now!)**, do the
following:

    $ gpg --import 2ADFB13E-revoke.asc  # ONLY WHEN YOU NEED TO REVOKE
    $ gpg --send-keys 2ADFB13E          # ONLY WHEN YOU NEED TO REVOKE

## How Leiningen uses GPG

Leiningen uses GPG for two things: decrypting credential files and
signing release artifacts. We'll focus on artifact signing here; for
information on credentials encryption/decryption, see the
[deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md).

On some systems you will be prompted for your GPG passphrase when it
is needed if you haven't entered it. If yours does not, you can
install [Keychain](https://github.com/funtoo/keychain), which provides
this functionality portably. 

### Signing a file

When you deploy a non-SNAPSHOT artifact via the `deploy`
task, Leiningen will attempt to create GPG signatures of the jar and
pom files. It does so by shelling out to `gpg` and using your default
private key to sign each artifact. This will create a signature file
for each artifact named by appending `.asc` to the artifact name.

Both signatures are then uploaded along with the artifacts. If you're
deploying to Clojars, you'll want to provide it with your *public* key
(see below) in order that the signatures can be verified.

To disable signing of releases, set `:sign-releases` to false in the
`:repositories` entry you are targeting. If you do this, everyone who
is depending on your library will not be able to confirm that the copy
they get has not been tampered with, so this is not recommended.

### Overriding the gpg defaults

By default, Leiningen will try to call GPG as `gpg`, which assumes
that `gpg` is in your path, and your GPG binary is actually called
`gpg`. If either of those are false, you can override the command
Leiningen uses for GPG by setting the `LEIN_GPG` environment variable.

GPG by default will select the first private key it finds (which will
be the first key listed by `gpg --list-secret-keys`). If you have
multiple keys and want to sign with one other than first, or want to
use specific keys for a particular release repository, you can specify
which key to use either globally, per-project, or
per-deploy-repository. All three places use the same configuration
syntax, it's all about where you put it. You can specify the key by id
or by the uid.

To set a key globally, add it to your user profile in
`~/.lein/profiles.clj`:

    {:user 
      ...
      {:signing {:gpg-key "2ADFB13E"}}} ;; using the key id
    
To set a key for a particular project, add it to the project
definition:

    (defproject ham-biscuit "0.1.0"
       ...
       {:signing {:gpg-key "bob@bobsons.net"}} ;; using the key uid
       ...)
    
To set a key for a particular deploy repository, add it to the
repository specification in your project definition:

    (defproject ham-biscuit "0.1.0"
       ...
       :deploy-repositories 
         [["releases" {:url "http://blueant.com/archiva/internal/releases"
                       :signing {:gpg-key "2ADFB13E"}}]
         ["snapshots" "http://blueant.com/archiva/internal/snapshots"]]
       ...)
       
## Clojars

Clojars requires that artifacts be signed and verified before being
promoted to the
[releases](https://github.com/ato/clojars-web/wiki/Releases)
repository. In order to verify the signature, it needs a copy of your
*public* key. To view your public key, use `gpg --export -a` giving it
the key id. Example:

    $ gpg --export -a 2ADFB13E
    -----BEGIN PGP PUBLIC KEY BLOCK-----
    Version: GnuPG v1.4.11 (GNU/Linux)
    
    mQENBFE/a/UBCAChmZrZWFFgzzYrhOVx0EiUa3S+0kV6UryqkxPASbHZLml3RlJI
    <snipped>
    =EaPb
    -----END PGP PUBLIC KEY BLOCK-----

Copy the entire output (including the BEGIN and END lines), and paste
it into the 'PGP public key' field of your Clojars profile.

### lein deploy clojars vs. scp

Currently, publishing signatures to Clojars only works if you are
using `lein deploy clojars`. If you are using `scp` to deploy, you can
copy signatures along with the artifacts, but they will be
ignored.

