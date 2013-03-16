# Using GPG

This document aims to be "just enough" for setting up and using
[GPG](http://www.gnupg.org/) keys for signing artifacts with
[Leiningen](http://leiningen.org) for publication to
[Clojars](http://clojars.org/).  

There are two versions of GPG available: v1.x and v2.x. For our
purposes, they are functionally equivalent. Package managers generally
install v2.x as `gpg2`, and v1.x as `gpg`. By default, Leiningen
expects the GPG command to be `gpg`. You're welcome to use any version
you like, but this primer will only cover installing v1.x, and has
only been tested under v1.x.

## What is it?

GPG(http://www.gnupg.org/) (or Gnu Privacy Guard) is a set of tools
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
closely, and share it with no one.**

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

To list your private keys:

    gpg --list-secret-keys
    
To list all of the public keys:

    gpg --list-keys
    
This will produce similar output, but will include any public keys you
have used (if you've never used GPG before and just created your first
keypair, you should just see your own key).

## How Leiningen uses GPG

Leiningen uses gpg for two things: decrypting credential files, and
signing release artifacts. We'll focus on artifact singing here; for
information on credentials encryption/decryption, see
[the Leiningen deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md).

### Signing a file

When you deploy a non-SNAPSHOT artifact to Clojars via the `deploy`
task, Leiningen will attempt to create GPG signatures of the jar and
pom files. It does so by shelling out to `gpg` and using your default
private key to sign each artifact. This will create a signature file
for each artifact named by appending `.asc` to the artifact name.

Both signatures are then uploaded to Clojars along with the
artifacts. In order for Clojars to verify the signatures, you'll need
to provide it with your *public* key (see below).

### Overriding the gpg defaults

By default, Leiningen will try to call GPG as `gpg`, which assumes
that `gpg` is in your path, and your GPG binary is actually called
`gpg`. If either of those are false, you can override the command
Leiningen uses for GPG by setting the `LEIN_GPG` environment variable.

Leiningen currently makes no effort to select a private key to use for
signing, and leaves that up to GPG. GPG by default will select the
first private key it finds (which will be the first key listed by `gpg
--list-secret-keys`). If you have multiple keys and want to sign with
one other than first, you'll need to set a default key for GPG. To do
so, edit `~/.gnupg/gpg.conf` and set the `default-key` option to the
id of the key you want to use. You can get the key id from the 'pub'
line in the key listing:

    $ gpg --list-keys

                ↓↓↓↓↓↓↓↓
    pub   2048R/2ADFB13E 2013-03-16 [expires: 2014-03-16]
    uid                  Bob Bobson <bob@bobsons.net>
    sub   2048R/8D2344D0 2013-03-16 [expires: 2014-03-16]

## Clojars 

Clojars requires that artifacts be signed and verified before being
promoted to the
[releases](https://github.com/ato/clojars-web/wiki/Releases)
repository. In order to verify the signature, it needs a copy of your
*public* key. To view your public key, use `gpg --export -a` giving it
either the key id. Example:

```
$ gpg --export -a 2ADFB13E
-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: GnuPG v1.4.11 (GNU/Linux)

mQENBFE/a/UBCAChmZrZWFFgzzYrhOVx0EiUa3S+0kV6UryqkxPASbHZLml3RlJI
<snipped>
=EaPb
-----END PGP PUBLIC KEY BLOCK-----
```

Copy the entire output (including the BEGIN and END lines), and paste
it into the 'PGP public key' field of your Clojars profile.

### lein deploy clojars vs. scp

Currently, publishing signatures to Clojars only works if you are
using `lein deploy clojars`. If you are using `scp` to deploy, you can
copy signatures along with the artifacts, but they will be
ignored.

