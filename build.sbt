enablePlugins(SbtPlugin)

name                := "sbt-scala-module"
organization        := "org.scala-lang.modules"
licenses            := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.4.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.3")
addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.2.0")   // set scalaVersion and crossScalaVersions
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.2") // set version, scmInfo, publishing settings
