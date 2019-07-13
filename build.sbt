enablePlugins(SbtPlugin)

git.baseVersion   := "2.0.0"
versionWithGit

name                := "sbt-scala-module"
organization        := "org.scala-lang.modules"
licenses            := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.5")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.5.0")
