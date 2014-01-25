git.baseVersion   := "1.0.0"

versionWithGit

name              := "scala-module-plugin"

organization      := "org.scala-lang.modules"

sbtPlugin         := true

publishTo         := Some(if (version.value.trim.endsWith("SNAPSHOT")) Classpaths.sbtPluginSnapshots else Classpaths.sbtPluginReleases)

publishMavenStyle := false

resolvers         += Classpaths.sbtPluginReleases

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")

