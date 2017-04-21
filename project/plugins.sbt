addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.2")

// incompatible with sbt-gpg (https://github.com/softprops/bintray-sbt/pull/10)
//   JZ: is this still true after updating to 0.3.0?
// lrytz: I tried upgrading to "org.foundweekends" % "sbt-bintray" % "0.4.0" but got a linkage error
//   [error] (*:bintrayEnsureBintrayPackageExists) java.lang.NoSuchMethodError: org.json4s.Formats.emptyValueStrategy()Lorg/json4s/prefs/EmptyValueStrategy;
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
