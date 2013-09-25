git.baseVersion := "1.0"

versionWithGit

name := "scala-module-plugin"

organization := "org.scala-lang.modules"

sbtPlugin := true

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false
