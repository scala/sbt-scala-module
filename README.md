# Scala modules sbt plugin

This is an sbt 1.x plugin for building Scala modules.

## What modules use it?

### Former standard library

* [scala-parser-combinators](https://github.com/scala/scala-parser-combinators)
* [scala-swing](https://github.com/scala/scala-swing)
* [scala-xml](https://github.com/scala/scala-xml)

For historical reasons, these were originally part of the Scala standard library. They live on as modules since they are in the `scala.` namespace and keeping them there preserves source compatibility with old source code that uses them. They are now community-maintained and largely frozen in design, though still open to minor improvements.

### Standard library adjacent

* [scala-async](https://github.com/scala/scala-async)
* [scala-collection-compat](https://github.com/scala/scala-collection-compat)
* [scala-java8-compat](https://github.com/scala/scala-java8-compat)
* [scala-parallel-collections](https://github.com/scala/scala-parallel-collections)

These modules are maintained by the Scala organization, with community input and participation. They have an especially close relationship with the Scala standard library (or, in the case of scala-async, the Scala 2 compiler).

### Future standard library?

* [scala-library-next](https://github.com/scala/scala-library-next)
* [scala-collection-contrib](https://github.com/scala/scala-collection-contrib)

Code that could become part of the standard library in the future.

## Why this plugin?

Having a shared plugin reduces duplication between the above
repositories. Reducing duplication makes maintenance easier and
helps ensure consistency.

A major feature of the plugin is automated tag-based publishing using
sbt-ci-release. A release is made by pushing a tag to GitHub.

The plugin also brings in
  - sbt-dynver to set the `version` based on the git history
  - sbt-version-policy to check the versioning policy using MiMa
  - sbt-header to automate copyright header maintenance
  - sbt-osgi, if enabled with `scalaModuleOsgiSettings`

## Usage

Add the plugin to the `project/plugins.sbt` file:

```
addSbtPlugin("org.scala-lang.modules" % "sbt-scala-module" % <version>)
```

Then, in your `build.sbt` add:

```
// In a multi-project build, you might want to apply these settings only to the
// main project (see e.g. scala-parallel-collections)
ScalaModulePlugin.scalaModuleSettings

// If making an OSGi bundle
ScalaModulePlugin.scalaModuleOsgiSettings

name         := "<module name>"
repoName     := "<GitHub repo name>" // the repo under github.com/scala/, only required if different from name
organization := "<org>"              // only required if different from "org.scala-lang.modules"

versionPolicyIntention := Compatibility.BinaryAndSourceCompatible // enables MiMa (`Compatibility.None` by default, which disables it)

OsgiKeys.exportPackage := Seq(s"<exported package>;version=${version.value}")

// Other settings
```

Cross-building with Scala.js and Scala Native is possible.  See scala-xml or scala-parser-combinators for examples.

These additional settings are enabled by `scalaModuleSettings`:
  - `Compile / compile / scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint")`
  - A `projectName.properties` file is generated and packaged
  - `Test / fork := true` to work around some classpath clashes with scala-xml
  - POM metadata

The following settings are also available:
  - `enableOptimizer` adds `-opt-inline-from:<sources>` or `-opt:l:project` or `-optimize` to `Compile / compile / scalacOptions`,
    depending on the Scala version

## Set up tag-based publishing

The instructions here are a summary of the readme in https://github.com/olafurpg/sbt-ci-release and https://github.com/scalacenter/sbt-version-policy

  - Create a fresh GPG key: `gpg --gen-key`
    - Real name: use "project-name bot"
    - Email: "something@scala-lang.org"
    - Passphrase: generate one yourself
  - Get the key `LONG_ID` from the output and set `LONG_ID=6E8ED79B03AD527F1B281169D28FC818985732D9`. The output looks like this:

        pub   rsa2048 2018-06-10 [SC] [expires: 2020-06-09]
              $LONG_ID
  - Copy the public key to a key server
    - `gpg --armor --export $LONG_ID`
    - http://keyserver.ubuntu.com:11371/
  - In your repo's Actions settings, define four secret env vars
    - `PGP_PASSPHRASE` the passphrase you chose above
    - `PGP_SECRET` the secret key in base64
      - macOS: `gpg --armor --export-secret-keys $LONG_ID | base64`
      - ubuntu: `gpg --armor --export-secret-keys $LONG_ID | base64 -w0`
    - `SONATYPE_PASSWORD`: need that one
    - `SONATYPE_USERNAME`: that one too

  - Run `versionCheck` in the publishing process: `sbt versionCheck ci-release`
