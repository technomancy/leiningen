# Introduction to JVM Packaging Concepts

For those of you new to the JVM who have never touched Maven in anger:
don't panic. Leiningen is designed with you in mind. Leiningen is
built on some Maven libraries; specifically the dependency resolution
parts. These aspects of Maven are not nearly as complex as the build
features that have given it its somewhat fearsome reputation. Think of
it as the package manager of the JVM world, much like Rubygems, CPAN,
etc. There is a central canonical repository, a Clojure-specific
repository called Clojars, and projects often also make their jars
available through project-specific public repositories. It allows you
to define the dependencies of your project, which it then goes and
retrieves, stores in a local repository ("~/.m2" on Unix by default),
and copies into your project's "lib/" directory. This makes it so that
you don't have to check your dependent jar files into your SCM or make
developers hunt down dependency jars by hand.

Leiningen describes packages using identifiers that look like:

    org.clojure/swank-clojure "1.0"

* "org.clojure" is called the 'group-id'
* "swank-clojure" is called the 'artifact-id'
* "1.0" is the version of the jar file you require

Sometimes versions will end in "-SNAPSHOT". This means that it is not
an official release but a development build. In general relying on
snapshot dependencies is discouraged, but sometimes its necessary if
you need bug fixes etc. that have not made their way into a release
yet. Adding a snapshot dependency to your project will cause Leiningen
to actively go seek out the latest version of the dependency every
time you run "lein deps", (whereas normal release versions just use
the local cache) so if you have a lot of snapshots it will slow things
down.

While it's customary to make group IDs match either artifact IDs or
the Java package, it's not required. Let's take for an example the
[JYaml jar](http://jyaml.sourceforge.net). Looking at their javadocs,
the Java package name is "org.ho.yaml". If you try to add the
following dependency in your project.clj file:

    [org.ho.yaml/jyaml "1.3"]

You'll get an error when you run "lein deps". One way to find the
correct group id is to use a maven search web site like
[Jarvana](http://jarvana.com). In the "project search" box, enter
"jyaml", and you'll see results that indicate the correct group-id
("org.jyaml") and artifact id ("jyaml").

If the jar you're looking for is published in a project's own public
repository instead of the central one, you can use the :repositories
option in project.clj. For instance, if we wanted to use the jars from
the HornetQ project:

    (defproject com.motionbox.hornetq/dashboard "0.0.1-SNAPSHOT"
      :description "a simple web interface to the hornetq jmx API"
      ;; this repository will be referred to in log messages as "jboss-release"
      :repositories {"jboss-release" "http://repository.jboss.org/maven2"}
      :dependencies [[org.clojure/clojure "1.1.0"]
             [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
             [commons-logging    "1.1.1"]
             [org.hornetq/hornetq-core       "2.0.0.GA"]
             [org.hornetq/hornetq-jms-client "2.0.0.GA"]
             [org.hornetq/hornetq-transports "2.0.0.GA"]
             [org.hornetq/hornetq-logging    "2.0.0.GA"]
             [org.jboss.netty/netty          "3.1.0.GA"]
             [org.jboss.javaee/jboss-jms-api "1.1.0.GA"]
             [log4j "1.2.15" :exclusions [javax.mail/mail
                              javax.jms/jms
                              com.sun.jdmk/jmxtools
                              com.sun.jmx/jmxri]]
             ]
      :dev-dependencies [[swank-clojure "1.1.0"]]
      :namespaces   [com.motionbox.hornetq])

For an in-depth walkthrough see the Full Disclojure screencast:
http://vimeo.com/8934942
