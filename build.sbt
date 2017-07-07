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

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.1")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.14")
