# Scala modules sbt plugin

This is an sbt plugin for creating a scala module build.

## Usage

Add the plugin to the `project/plugins.sbt` file:

```
addSbtPlugin("org.scala-lang.modules" % "scala-module-plugin" % "1.0.6")
```

Then, in your `build.sbt` add:

```
scalaModuleSettings

name         := "<module name>"
repoName     := "<GitHub repo name>" // the repo under github.com/scala/, only required if different from name
organization := "<org>"              // only required if different from "org.scala-lang.modules"
version      := "<module version>"

// The plugin uses `scalaVersionsByJvm` to set `crossScalaVersions` according to the JVM major version.
// The `scalaVersion` is set to `crossScalaVersions.value.head`.
scalaVersionsByJvm := {
  val v211 = "2.11.11"
  val v212 = "2.12.2"
  val v213 = "2.13.0-M1"

  // Map[JvmMajorVersion, List[(ScalaVersion, UseForPublishing)]]
  Map(
    6 -> List(v211 -> true),
    7 -> List(v211 -> false),
    8 -> List(v212 -> true, v213 -> true, v211 -> false),
    9 -> List(v212, v213, v211).map(_ -> false)
  )
}

mimaPreviousVersion := Some("1.0.3") // enables MiMa (`None` by default, which disables it)

OsgiKeys.exportPackage := Seq(s"<exported package>;version=${version.value}")

// Other settings
```

## Cutting a new release

- Sign in to Bintray (https://bintray.com/login) or create an "Open Source" account (https://bintray.com/signup/oss)
- Check if you have a repository named `sbt-plugins`. If not, create it (Name: sbt-plugins, Type: Generic).
- Make sure the current `HEAD` is a tagged revision. In sbt, `version` (set by sbt-git) should be according to a tag.

      > version
      [info] 1.0.6

- Run `publish` in sbt. If you don't have a `~/.bintray/.credentials` file, the sbt-bintray plugin will ask you for your
  username and API key. The API key can be obtained under "Edit Profile" (https://bintray.com/profile/edit). The sbt-bintray
  plugin saves credentials to `~/.bintray/.credentials` for future use.
- If you haven't done so before, add your package for this plugin (https://bintray.com/YOUR_USERNAME/sbt-plugins/scala-module-plugin)
  to the community sbt repository (https://bintray.com/sbt/sbt-plugin-releases). Otherwise you're done, the release is available.
  - Check if you added your package by searching for "scala-module-plugin" in the repository.
  - If you cannot find your package, click "Include My Package"
  - Search for your plugin (`scala-module-plugin`)
  - Click "Send" to send the request

The above instructions are a short version of http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html.
