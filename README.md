# Scala modules sbt plugin

This is an sbt plugin for building Scala modules (scala-xml,
scala-parser-combinators, and so on).

The major benefit of the plugin is to provide automated tag-based
publishing.  A release is made by pushing a tag to GitHub.  Travis
then stages artifacts on Sonatype.  Pressing "Close" and "Release" in
the Sonatype web UI will then send the artifacts to Maven Central.

## Usage

Add the plugin to the `project/plugins.sbt` file:

```
addSbtPlugin("org.scala-lang.modules" % "sbt-scala-module" % "1.0.13")
```

Then, in your `build.sbt` add:

```
import ScalaModulePlugin._

scalaModuleSettings // in a multi-project build, you might want to apply these settings only to the
                    // main project (example: scala-parallel-collections)

name         := "<module name>"
repoName     := "<GitHub repo name>" // the repo under github.com/scala/, only required if different from name
organization := "<org>"              // only required if different from "org.scala-lang.modules"
version      := "<module version>"

// The plugin uses `scalaVersionsByJvm` to set `crossScalaVersions in ThisBuild` according to the JVM major version.
// The `scalaVersion in ThisBuild` is set to `crossScalaVersions.value.head`.
scalaVersionsByJvm in ThisBuild := {
  val v211 = "2.11.11"
  val v212 = "2.12.2"
  val v213 = "2.13.0-M1"

  // Map[JvmMajorVersion, List[(ScalaVersion, UseForPublishing)]]
  Map(
    6 -> List(v211 -> true),
    7 -> List(v211 -> false),
    8 -> List(v212 -> true, v213 -> true, v211 -> false),
    9 -> List(v212, v213, v211).map(_ -> false))
}

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
      [info] 1.0.13

- Run `publish` in sbt. If you don't have a `~/.bintray/.credentials` file, the sbt-bintray plugin will ask you for your
  username and API key. The API key can be obtained under "Edit Profile" (https://bintray.com/profile/edit). The sbt-bintray
  plugin saves credentials to `~/.bintray/.credentials` for future use.
- If you haven't done so before, add your package for this plugin (https://bintray.com/YOUR_USERNAME/sbt-plugins/sbt-scala-module)
  to the community sbt repository (https://bintray.com/sbt/sbt-plugin-releases). Otherwise you're done, the release is available.
  - Check if you added your package by searching for "sbt-scala-module" in the repository.
  - If you cannot find your package, click "Include My Package"
  - Search for your plugin (`sbt-scala-module`)
  - Click "Send" to send the request

The above instructions are a short version of http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html.
