import bintray.Keys._

git.baseVersion   := "1.0.0"

versionWithGit

name              := "scala-module-plugin"

organization      := "org.scala-lang.modules"

sbtPlugin         := true

// sbtVersion in Global := "0.13.1"

// scalaVersion in Global := "2.10.3"

// publishTo         := Some(if (version.value.trim.endsWith("SNAPSHOT")) Classpaths.sbtPluginSnapshots else Classpaths.sbtPluginReleases)

publishMavenStyle := false

bintrayPublishSettings

resolvers         += Classpaths.sbtPluginReleases

licenses          := Seq("BSD" -> url("http://opensource.org/licenses/BSD"))

repository in bintray          := "sbt-plugins"

bintrayOrganization in bintray := None

// this plugin depends on the sbt-osgi plugin -- 2-for-1!
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")
