# Scala modules sbt plugin

This is an sbt 1.x plugin for building Scala modules.

## What modules use it?

* [travis-test](https://github.com/lrytz/travis-test) (example project for testing this plugin and tag-driven releases)
* [scala-async](https://github.com/scala/scala-async)
* [scala-collection-compat](https://github.com/scala/scala-collection-compat)
* [scala-collection-contrib](https://github.com/scala/scala-collection-contrib)
* [scala-java8-compat](https://github.com/scala/scala-java8-compat)
* [scala-library-next](https://github.com/scala/scala-library-next)
* [scala-parallel-collections](https://github.com/scala/scala-parallel-collections)
* [scala-parser-combinators](https://github.com/scala/scala-parser-combinators)
* [scala-swing](https://github.com/scala/scala-swing)
* [scala-xml](https://github.com/scala/scala-xml)

## Why this plugin?

Having a shared plugin reduces duplication between the above
repositories. Reducing duplication makes maintenance easier and
helps ensure consistency.

A major feature of the plugin is automated tag-based publishing using
sbt-ci-release. A release is made by pushing a tag to GitHub.

The plugin also brings in
  - sbt-travisci to set the `scalaVersion` and `crossScalaVersions`
  - sbt-dynver to set the `version` based on the git history
  - sbt-header to automate copyright header maintenance
  - sbt-mima-plugin to maintain binary compatibility
  - sbt-osgi, if enabled with `scalaModuleOsgiSettings`

## Usage

Add the plugin to the `project/plugins.sbt` file:

```
addSbtPlugin("org.scala-lang.modules" % "sbt-scala-module" % "2.2.3")
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

scalaModuleMimaPreviousVersion := Some("1.0.0") // enables MiMa (`None` by default, which disables it)

OsgiKeys.exportPackage := Seq(s"<exported package>;version=${version.value}")

// Other settings
```

Scala versions are defined in `.travis.yml`.

Cross-building with Scala.js and Scala Native is possible, see travis-test, scala-xml or scala-parser-combinators for example.

These additional settings are enabled by `scalaModuleSettings`:
  - `scalacOptions in (Compile, compile) ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint")`
  - A `projectName.properties` file is generated and packaged
  - `fork in Test := true` to work around some classpath clashes with scala-xml
  - POM metadata

The following settings are also available:
  - `enableOptimizer` adds `-opt-inline-from:<sources>` or `-opt:l:project` or `-optimize` to `scalacOptions in (Compile, compile)`,
    depending on the Scala version
  - `disablePublishing` is useful for multi-project builds for projects that should not be published

## Set up tag-based publishing

The instructions here are a summary of the readme in https://github.com/olafurpg/sbt-ci-release

  - Create a fresh GPG key: `gpg --gen-key`
    - Real name: use "project-name bot"
    - Email: "scala-internals@googlegroups.com"
    - Passphrase: generate one yourself
  - Get the key `LONG_ID` from the output and set `LONG_ID=6E8ED79B03AD527F1B281169D28FC818985732D9`
        pub   rsa2048 2018-06-10 [SC] [expires: 2020-06-09]
          $LONG_ID
  - Copy the public key to a key server
    - `gpg --armor --export $LONG_ID`
    - http://keyserver.ubuntu.com:11371/
  - Open the Settings panel on your project's travis, define four secret env vars
    - `PGP_PASSPHRASE` the passphrase you chose above
    - `PGP_SECRET` the secret key in base64
      - macOS: `gpg --armor --export-secret-keys $LONG_ID | base64`
      - ubuntu: `gpg --armor --export-secret-keys $LONG_ID | base64 -w0`
    - `SONATYPE_PASSWORD`: need that one
    - `SONATYPE_USERNAME`: that one too
