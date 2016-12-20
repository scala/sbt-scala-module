git.baseVersion   := "1.0.0"

versionWithGit

name              := "scala-module-plugin"

organization      := "org.scala-lang.modules"

sbtPlugin         := true

// sbtVersion in Global := "0.13.1"

// scalaVersion in Global := "2.10.3"

// publishTo         := Some(if (version.value.trim.endsWith("SNAPSHOT")) Classpaths.sbtPluginSnapshots else Classpaths.sbtPluginReleases)

publishMavenStyle := false

resolvers         += Classpaths.sbtPluginReleases

licenses          := Seq("BSD" -> url("http://opensource.org/licenses/BSD"))

bintrayRepository := "sbt-plugins"

bintrayOrganization := Some("typesafe")

// this plugin depends on the sbt-osgi plugin -- 2-for-1!
// TODO update to 0.8.0
//      this might require us to modify the downstream project to enable the AutoPlugin
//      See code changes and docs: https://github.com/sbt/sbt-osgi/commit/e3625e685b8d1784938ec66067d629251811a9d1
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.13")
