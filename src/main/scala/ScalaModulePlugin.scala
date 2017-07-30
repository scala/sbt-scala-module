import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt.Keys._
import sbt.{Def, _}

object ScalaModulePlugin extends AutoPlugin {
  val repoName            = settingKey[String]("The name of the repository under github.com/scala/.")
  val mimaPreviousVersion = settingKey[Option[String]]("The version of this module to compare against when running MiMa.")
  val scalaVersionsByJvm  = settingKey[Map[Int, List[(String, Boolean)]]]("For a Java major version (6, 8, 9), a list of a Scala version and a flag indicating whether to use this combination for publishing.")

  // See https://github.com/sbt/sbt/issues/2082
  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  // Settings in here are implicitly `in ThisBuild`
  override def buildSettings: Seq[Setting[_]] = Seq(
    scalaVersionsByJvm := Map.empty,

    crossScalaVersions := {
      val OneDot = """1\.(\d).*""".r // 1.6, 1.8
      val Maj    = """(\d+).*""".r   // 9
      val javaVersion = System.getProperty("java.version") match {
        case OneDot(n) => n.toInt
        case Maj(n)    => n.toInt
        case v         => throw new RuntimeException(s"Unknown Java version: $v")
      }

      val isTravis = Option(System.getenv("TRAVIS")).exists(_ == "true") // `contains` doesn't exist in Scala 2.10
      val isTravisPublishing = Option(System.getenv("TRAVIS_TAG")).exists(_.trim.nonEmpty)

      val byJvm = scalaVersionsByJvm.value
      if (byJvm.isEmpty)
        throw new RuntimeException(s"Make sure to define `scalaVersionsByJvm in ThisBuild` in `build.sbt` in the root project, using the `ThisBuild` scope.")

      val scalaVersions = byJvm.getOrElse(javaVersion, Nil) collect {
        case (v, publish) if !isTravisPublishing || publish => v
      }
      if (scalaVersions.isEmpty) {
        if (isTravis) {
          sLog.value.warn(s"No Scala version in `scalaVersionsByJvm` in build.sbt needs to be released on Java major version $javaVersion.")
          // Exit successfully, don't fail the (travis) build. This happens for example if `openjdk7`
          // is part of the travis configuration for testing, but it's not used for releasing against
          // any Scala version.
          System.exit(0)
        } else
          throw new RuntimeException(s"No Scala version for Java major version $javaVersion. Change your Java version or adjust `scalaVersionsByJvm` in build.sbt.")
      }
      scalaVersions
    },

    scalaVersion := crossScalaVersions.value.head
  )

  /**
   * Enable `-opt:l:inline`, `-opt:l:classpath` or `-optimize`, depending on the scala version.
   */
  lazy val enableOptimizer: Setting[_] = scalacOptions in (Compile, compile) ++= {
    val Ver = """(\d+)\.(\d+)\.(\d+).*""".r
    val Ver("2", maj, min) = scalaVersion.value
    (maj.toInt, min.toInt) match {
      case (m, _) if m < 12 => Seq("-optimize")
      case (12, n) if n < 3 => Seq("-opt:l:classpath")
      case _                => Seq("-opt:l:inline", "-opt-inline-from:scala/**")
    }
  }

  /**
   * Practical for multi-project builds.
   */
  lazy val disablePublishing: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    // The above is enough for Maven repos but it doesn't prevent publishing of ivy.xml files
    publish := {},
    publishLocal := {},
    publishTo := Some(Resolver.file("devnull", file("/dev/null")))
  )

  /**
   * To be included in the main sbt project of a Scala module.
   */
  lazy val scalaModuleSettings: Seq[Setting[_]] = Seq(
    repoName := name.value,

    organization := "org.scala-lang.modules",

    // don't use for doc scope, scaladoc warnings are not to be reckoned with
    scalacOptions in (Compile, compile) ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint"),

    // Generate $name.properties to store our version as well as the scala version used to build
    resourceGenerators in Compile += Def.task {
      val props = new java.util.Properties
      props.put("version.number", version.value)
      props.put("scala.version.number", scalaVersion.value)
      props.put("scala.binary.version.number", scalaBinaryVersion.value)
      val file = (resourceManaged in Compile).value / s"${name.value}.properties"
      IO.write(props, null, file)
      Seq(file)
    }.taskValue,

    mappings in (Compile, packageBin) += {
       (baseDirectory.value / s"${name.value}.properties") -> s"${name.value}.properties"
    },

    // needed to fix classloader issues (see scala/scala-xml#20)
    // essentially, the problem is that the run-time bootclasspath leaks into the compilation classpath,
    // so that scalac see classes used to run it, as classes used to compile against...
    // forking uses a minimal classpath, so this craziness is avoided
    // alternatively, manage the scala instance as shown at the end of this file (commented)
    fork in Test := true,

    // maven publishing
    publishTo := Some(
      if (version.value.trim.endsWith("SNAPSHOT")) Resolver.sonatypeRepo("snapshots")
      else Opts.resolver.sonatypeStaging
    ),
    credentials ++= {
      val file = Path.userHome / ".ivy2" / ".credentials"
      if (file.exists) List(new FileCredentials(file)) else Nil
    },

    publishMavenStyle    := true,
    scmInfo              := Some(ScmInfo(url(s"https://github.com/scala/${repoName.value}"),s"scm:git:git://github.com/scala/${repoName.value}.git")),
    homepage             := Some(url("http://www.scala-lang.org/")),
    organizationHomepage := Some(url("http://www.scala-lang.org/")),
    licenses             := Seq("BSD 3-clause" -> url("http://opensource.org/licenses/BSD-3-Clause")),
    startYear            := Some(2002),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <issueManagement>
        <system>GitHub</system>
        <url>https://github.com/scala/{repoName.value}/issues</url>
      </issueManagement>
      <developers>
        <developer>
          <id>lamp</id>
          <name>LAMP/EPFL</name>
        </developer>
        <developer>
          <id>Lightbend</id>
          <name>Lightbend, Inc.</name>
        </developer>
      </developers>
    )
  ) ++ mimaSettings

  lazy val scalaModuleSettingsJVM: Seq[Setting[_]] = scalaModuleOsgiSettings

  lazy val scalaModuleSettingsNative: Seq[Settings[_]] = Seq(
    scalaVersion := "2.11.11",
    skip in compile := System.getProperty("java.version").startsWith("1.6"),
    test := {},
    libraryDependencies := {
      if (!scalaVersion.value.startsWith("2.11"))
        libraryDependencies.value.filterNot(_.organization == "org.scala-native")
      else libraryDependencies.value
    }
  )

  // adapted from https://github.com/typesafehub/migration-manager/blob/0.1.6/sbtplugin/src/main/scala/com/typesafe/tools/mima/plugin/SbtMima.scala#L69
  private def artifactExists(organization: String, name: String, scalaBinaryVersion: String, version: String, ivy: IvySbt, s: TaskStreams): Boolean = {
    val moduleId = new ModuleID(organization, s"${name}_$scalaBinaryVersion", version)
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(moduleId))
    val ivyModule = new ivy.Module(moduleSettings)
    try {
      IvyActions.update(
        ivyModule,
        new UpdateConfiguration(
          retrieve = None,
          missingOk = false,
          logging = UpdateLogging.DownloadOnly),
        s.log)
      true
    } catch {
      case _: ResolveException => false
    }
  }

  // Internal task keys for the MiMa settings
  private val canRunMima       = taskKey[Boolean]("Decides if MiMa should run.")
  private val runMimaIfEnabled = taskKey[Unit]("Run MiMa if mimaPreviousVersion and the module can be resolved against the current scalaBinaryVersion.")

  private lazy val mimaSettings: Seq[Setting[_]] = MimaPlugin.mimaDefaultSettings ++ Seq(
    mimaPreviousVersion := None,

    // We're not using `%%` here in order to support both jvm and js projects (cross version `_2.12` / `_sjs0.6_2.12`)
    mimaPreviousArtifacts := Set(organization.value % moduleName.value % mimaPreviousVersion.value.getOrElse("dummy") cross crossVersion.value),

    canRunMima := {
      val mimaVer = mimaPreviousVersion.value
      val s = streams.value
      if (mimaVer.isEmpty) {
        s.log.warn("MiMa will NOT run because no mimaPreviousVersion is provided.")
        false
      } else if (!artifactExists(organization.value, name.value, scalaBinaryVersion.value, mimaVer.get, ivySbt.value, s)) {
        s.log.warn(s"""MiMa will NOT run because the previous artifact "${organization.value}" % "${name.value}_${scalaBinaryVersion.value}" % "${mimaVer.get}" could not be resolved (note the binary Scala version).""")
        false
      } else {
        true
      }
    },

    runMimaIfEnabled := Def.taskDyn({
      if(canRunMima.value) Def.task { mimaReportBinaryIssues.value }
      else Def.task { () }
    }).value,

    test in Test := {
      runMimaIfEnabled.value
      (test in Test).value
    }
  )

  // a setting-transform to turn the regular version into something osgi can deal with
  private val osgiVersion = version(_.replace('-', '.'))

  private lazy val scalaModuleOsgiSettings = SbtOsgi.projectSettings ++ SbtOsgi.autoImport.osgiSettings ++ Seq(
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
