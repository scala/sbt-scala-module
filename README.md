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
organization := "<org>"
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
