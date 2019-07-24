# Scala modules sbt plugin

This is an sbt 1.x plugin for building Scala modules.

## What modules use it?

* [scala-async](https://github.com/scala/scala-async)
* [scala-collection-compat](https://github.com/scala/scala-collection-compat)
* [scala-java8-compat](https://github.com/scala/scala-java8-compat)
* [scala-parallel-collections](https://github.com/scala/scala-parallel-collections)
* [scala-parser-combinators](https://github.com/scala/scala-parser-combinators)
* [scala-partest](https://github.com/scala/scala-partest)
* [scala-swing](https://github.com/scala/scala-swing)
* [scala-xml](https://github.com/scala/scala-xml)

## Why this plugin?

Having a shared plugin reduces duplication between the above
repositories.  Reducing duplication makes maintenance easier and
helps ensure consistency.

A major feature of the plugin is automated tag-based publishing.  A
release is made by pushing a tag to GitHub.  Travis-CI then stages
artifacts on Sonatype.  Pressing "Close" and "Release" in the Sonatype
web UI will then send the artifacts to Maven Central.

## Usage

Add the plugin to the `project/plugins.sbt` file:

```
addSbtPlugin("org.scala-lang.modules" % "sbt-scala-module" % "2.0.0")
```

Then, in your `build.sbt` add:

```
import ScalaModulePlugin._

scalaModuleSettings // in a multi-project build, you might want to apply these settings only to the
                    // main project (see e.g. scala-parallel-collections)

name         := "<module name>"
repoName     := "<GitHub repo name>" // the repo under github.com/scala/, only required if different from name
organization := "<org>"              // only required if different from "org.scala-lang.modules"
version      := "<module version>"

mimaPreviousVersion := Some("1.0.0") // enables MiMa (`None` by default, which disables it)

OsgiKeys.exportPackage := Seq(s"<exported package>;version=${version.value}")

// Other settings
```

These additional settings are enabled by `scalaModuleSettings`:
  - `scalacOptions in (Compile, compile) ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint")`
  - A `projectName.properties` file is generated and packaged
  - `fork in Test := true` to work around some classpath clashes with scala-xml
  - `publishTo` sonatype, credentials file expected in `~/.ivy2/.credentials`
  - POM and OSGi metadata

The following settings are also available:
  - `enableOptimizer` adds `-opt-inline-from:<sources>` or `-opt:l:project` or `-optimize` to `scalacOptions in (Compile, compile)`,
    depending on the Scala version
  - `disablePublishing` is useful for multi-project builds for projects that should not be published

## Cutting a new release (of this plugin)

### Release notes

Tag the release and add release notes to https://github.com/scala/sbt-scala-module/releases

### Publishing via Bintray

- Sign in to Bintray (https://bintray.com/login) or create an "Open Source" account (https://bintray.com/signup/oss)
- Check if you have a repository named `sbt-plugins`. If not, create it (Name: sbt-plugins, Type: Generic).
- Make sure the current `HEAD` is a tagged revision. In sbt, `version` (set by sbt-git) should be according to a tag.

      > version
      [info] 2.3.4

- Run `publish` in sbt. If you don't have a `~/.bintray/.credentials` file, the sbt-bintray plugin will ask you for your
  username and API key. The API key can be obtained under "Edit Profile" (https://bintray.com/profile/edit). The sbt-bintray
  plugin saves credentials to `~/.bintray/.credentials` for future use.
- If you haven't done so before, add your package for this plugin (https://bintray.com/YOUR_USERNAME/sbt-plugins/sbt-scala-module)
  to the community sbt repository (https://bintray.com/sbt/sbt-plugin-releases). Otherwise you're done, the release is available.
  - Check if you added your package by searching for "sbt-scala-module" in the repository.
  - If you cannot find your package, click "Include My Package"
  - Search for your plugin (`sbt-scala-module`)
  - Click "Send" to send the request

The above instructions are a short version of https://www.scala-sbt.org/1.x/docs/Bintray-For-Plugins.html.
