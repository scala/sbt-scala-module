import sbt._
import Keys._
import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}

object ScalaModulePlugin extends Plugin {
  val snapshotScalaBinaryVersion = settingKey[String]("The Scala binary version to use when building against Scala SNAPSHOT.")
  val repoName                   = settingKey[String]("The name of the repository under github.com/scala/.")

  def deriveBinaryVersion(sv: String, snapshotScalaBinaryVersion: String) =
    if (sv.startsWith("2.12") && sv.contains("-SNAPSHOT")) // TODO this is brittle around the release of a major version
      snapshotScalaBinaryVersion
    else
      sbt.CrossVersion.binaryScalaVersion(sv)

  lazy val scalaModuleSettings = Seq(
    repoName            := name.value,

    organization        := "org.scala-lang.modules",

    scalaBinaryVersion  := deriveBinaryVersion(scalaVersion.value, snapshotScalaBinaryVersion.value),

    // so we don't have to wait for sonatype to synch to maven central when deploying a new module
    resolvers += Resolver.sonatypeRepo("releases"),

    // to allow compiling against snapshot versions of Scala
    resolvers += Resolver.sonatypeRepo("snapshots"),

    // resolvers += "scala-release-temp" at "http://private-repo.typesafe.com/typesafe/scala-release-temp/"

    // don't use for doc scope, scaladoc warnings are not to be reckoned with
    // TODO: turn on for nightlies, but don't enable for PR validation... "-Xfatal-warnings"
    scalacOptions in compile ++= Seq("-optimize", "-feature", "-deprecation", "-unchecked", "-Xlint"),

    // Generate $name.properties to store our version as well as the scala version used to build
    resourceGenerators in Compile <+= Def.task {
      val props = new java.util.Properties
      props.put("version.number", version.value)
      props.put("scala.version.number", scalaVersion.value)
      props.put("scala.binary.version.number", scalaBinaryVersion.value)
      val file = (resourceManaged in Compile).value / s"${name.value}.properties"
      IO.write(props, null, file)
      Seq(file)
    },

    mappings in (Compile, packageBin) += {
       (baseDirectory.value / s"${name.value}.properties") -> s"${name.value}.properties"
    },

    // needed to fix classloader issues (see scala/scala-xml#20)
    // essentially, the problem is that the run-time bootclasspath leaks into the compilation classpath,
    // so that scalac see classes used to run it, as classes used to compile against...
    // forking uses a minimal classpath, so this craziness is avoided
    // alternatively, manage the scala instance as shown at the end of this file (commented)
    fork in Test := true,

    publishArtifact in Test := false,

    // maven publishing
    publishTo := Some(
      if (version.value.trim.endsWith("SNAPSHOT")) Resolver.sonatypeRepo("snapshots")
      else Opts.resolver.sonatypeStaging
    ),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    publishMavenStyle    := true,
    scmInfo              := Some(ScmInfo(url(s"https://github.com/scala/${repoName.value}"),s"scm:git:git://github.com/scala/${repoName.value}.git")),
    homepage             := Some(url("http://www.scala-lang.org/")),
    organizationHomepage := Some(url("http://www.scala-lang.org/")),
    licenses             := Seq("BSD 3-clause" -> url("http://opensource.org/licenses/BSD-3-Clause")),
    startYear            := Some(2002),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <issueManagement>
        <system>JIRA</system>
        <url>https://issues.scala-lang.org/</url>
      </issueManagement>
      <developers>
        <developer>
          <id>epfl</id>
          <name>EPFL</name>
        </developer>
        <developer>
          <id>Typesafe</id>
          <name>Typesafe, Inc.</name>
        </developer>
      </developers>
    )
  )

  // a setting-transform to turn the regular version into something osgi can deal with
  val osgiVersion = version(_.replace('-', '.'))

  lazy val scalaModuleOsgiSettings = SbtOsgi.osgiSettings ++ Seq(
    OsgiKeys.bundleSymbolicName  := s"${organization.value}.${name.value}",
    OsgiKeys.bundleVersion       := osgiVersion.value,

    // Sources should also have a nice MANIFEST file
    packageOptions in packageSrc := Seq(Package.ManifestAttributes(
                          ("Bundle-SymbolicName", s"${organization.value}.${name.value}.source"),
                          ("Bundle-Name", s"${name.value} sources"),
                          ("Bundle-Version", osgiVersion.value),
                          ("Eclipse-SourceBundle", s"""${organization.value}.${name.value};version="${osgiVersion.value}";roots:="."""")
                      ))
  )



  // TODO: mima
  // resolvers += Classpaths.typesafeResolver
  // addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.5")
  // import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
  // import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
  // previousArtifact := Some(organization.value %% name.value % binaryReferenceVersion.value)
}


// ALTERNATIVE to fork in test for fixing classpath issues noted above:
// manage the Scala instance ourselves to exclude the published scala-xml (scala-compiler depends on it)
// since this dependency hides the classes we're testing
// managedScalaInstance := false
//
// ivyConfigurations    += Configurations.ScalaTool
//
// libraryDependencies ++= Seq(
//    "org.scala-lang" % "scala-library" % scalaVersion.value,
//    ("org.scala-lang" % "scala-compiler" % scalaVersion.value % "scala-tool").exclude("org.scala-lang.modules", s"scala-xml_${scalaBinaryVersion.value}")
// )
