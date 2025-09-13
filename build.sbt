enablePlugins(SbtPlugin)

name         := "sbt-scala-module"
organization := "org.scala-lang.modules"
homepage     := Some(url("https://github.com/scala/sbt-scala-module"))
licenses     := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
developers   := List(Developer("", "", "", url("https://scala-lang.org")))

addSbtPlugin("com.github.sbt" % "sbt-osgi" % "0.10.0")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2") // set version, scmInfo, publishing settings
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.1")  // brings in MiMa
