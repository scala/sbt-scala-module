git.baseVersion   := "1.0.0"

versionWithGit

name              := "sbt-scala-module"

organization      := "org.scala-lang.modules"

sbtPlugin         := true

// sbtVersion in Global := "0.13.1"

// scalaVersion in Global := "2.10.3"

// publishTo         := Some(if (version.value.trim.endsWith("SNAPSHOT")) Classpaths.sbtPluginSnapshots else Classpaths.sbtPluginReleases)

publishMavenStyle := false

resolvers         += Classpaths.sbtPluginReleases

licenses          := Seq("BSD" -> url("http://opensource.org/licenses/BSD"))

bintrayRepository := "sbt-plugins"

bintrayOrganization := None

// Version 0.9.1 requires Java 8 (on 6 we get NoClassDefFoundError: java/util/function/Predicate).
// We still run our plugin builds for 2.11 on Java 6, so we cannot upgrade.
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.8.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.14")
