enablePlugins(SbtPlugin)

name                := "sbt-scala-module"
sonatypeProfileName := "org.scala-lang"
organization        := "org.scala-lang.modules"
homepage            := Some(url("https://github.com/scala/sbt-scala-module"))
licenses            := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
developers          := List(Developer("", "", "", url("https://scala-lang.org")))

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.6")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.2.0")   // set scalaVersion and crossScalaVersions
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5") // set version, scmInfo, publishing settings
