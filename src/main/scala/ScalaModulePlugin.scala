package com.lightbend.tools.scalamoduleplugin

import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.{HeaderLicense, headerLicense}
import sbt.Keys._
import sbt._
import sbt.internal.librarymanagement.IvySbt
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbt.librarymanagement.{UnresolvedWarningConfiguration, UpdateConfiguration}
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverGitDescribeOutput
import xerial.sbt.Sonatype.autoImport.{sonatypeProfileName, sonatypeSessionName}

object ScalaModulePlugin extends AutoPlugin {
  object autoImport {
    val scalaModuleRepoName = settingKey[String]("The name of the repository under github.com/scala/.")
    val scalaModuleMimaPreviousVersion = settingKey[Option[String]]("The version of this module to compare against when running MiMa.")
    val scalaModuleEnableOptimizerInlineFrom = settingKey[String]("The value passed to -opt-inline-from by `enableOptimizer` on 2.13 and higher.")
  }
  import autoImport._

  // depend on DynVerPlugin to allow modifying dynverGitDescribeOutput in buildSettings below
  override def requires = DynVerPlugin

  override def trigger = allRequirements

  // Settings in here are implicitly `in ThisBuild`
  override def buildSettings: Seq[Setting[_]] = Seq(
    scalaModuleEnableOptimizerInlineFrom := "<sources>",

    // drop # suffix from tags
    dynverGitDescribeOutput ~= (_.map(dv =>
      dv.copy(ref = sbtdynver.GitRef(dv.ref.value.split('#').head)))),
  )

  /**
   * Enable `-opt:l:inline`, `-opt:l:classpath` or `-optimize`, depending on the scala version.
   */
  lazy val enableOptimizer: Setting[_] = scalacOptions in (Compile, compile) ++= {
    val Ver = """(\d+)\.(\d+)\.(\d+).*""".r
    val Ver("2", maj, min) = scalaVersion.value
    (maj.toInt, min.toInt) match {
      case (m, _) if m < 12 => Seq("-optimize")
      case (12, n) if n < 3 => Seq("-opt:l:project")
      case _                => Seq("-opt:l:inline", "-opt-inline-from:" + scalaModuleEnableOptimizerInlineFrom.value)
    }
  }

  lazy val disablePublishing: Seq[Setting[_]] = Seq(
    skip in publish := true // works in sbt 1+
  )

  /**
   * To be included in the main sbt project of a Scala module.
   */
  lazy val scalaModuleSettings: Seq[Setting[_]] = Seq(
    scalaModuleRepoName := name.value,

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

    headerLicense := Some(HeaderLicense.Custom(
      s"""|Scala (https://www.scala-lang.org)
          |
          |Copyright EPFL and Lightbend, Inc.
          |
          |Licensed under Apache License 2.0
          |(http://www.apache.org/licenses/LICENSE-2.0).
          |
          |See the NOTICE file distributed with this work for
          |additional information regarding copyright ownership.
          |""".stripMargin)),

    // The staging profile is called `org.scala-lang`, the default is `org.scala-lang.modules`
    sonatypeProfileName := "org.scala-lang",

    // The name of the staging repository. The default is `[sbt-sonatype] name version`.Since we
    // cross-build using parallel travis jobs, we include the Scala version to make them unique.
    sonatypeSessionName := { s"${sonatypeSessionName.value} Scala ${scalaVersion.value}" },

    scmInfo              := Some(ScmInfo(url(s"https://github.com/scala/${scalaModuleRepoName.value}"),s"scm:git:git://github.com/scala/${scalaModuleRepoName.value}.git")),
    homepage             := Some(url("http://www.scala-lang.org/")),
    organizationHomepage := Some(url("http://www.scala-lang.org/")),
    licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    startYear            := Some(2002),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <issueManagement>
        <system>GitHub</system>
        <url>https://github.com/scala/{scalaModuleRepoName.value}/issues</url>
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

  // adapted from https://github.com/lightbend/migration-manager/blob/0.3.0/sbtplugin/src/main/scala/com/typesafe/tools/mima/plugin/SbtMima.scala#L112
  private def artifactExists(organization: String, name: String, scalaBinaryVersion: String, version: String, ivy: IvySbt, s: TaskStreams): Boolean = {
    val moduleId = ModuleID(organization, s"${name}_$scalaBinaryVersion", version)
    val depRes = IvyDependencyResolution(ivy.configuration)
    val module = depRes.wrapDependencyInModule(moduleId)
    val updateConf = UpdateConfiguration() withLogging UpdateLogging.DownloadOnly
    val reportEither = depRes.update(module, updateConf, UnresolvedWarningConfiguration(), s.log)
    reportEither.fold(_ => false, _ => true)
  }

  // Internal task keys for the MiMa settings
  private val canRunMima       = taskKey[Boolean]("Decides if MiMa should run.")
  private val runMimaIfEnabled = taskKey[Unit]("Run MiMa if mimaPreviousVersion and the module can be resolved against the current scalaBinaryVersion.")

  private lazy val mimaSettings: Seq[Setting[_]] = MimaPlugin.mimaDefaultSettings ++ Seq(
    scalaModuleMimaPreviousVersion := None,

    // We're not using `%%` here in order to support both jvm and js projects (cross version `_2.12` / `_sjs0.6_2.12`)
    mimaPreviousArtifacts := scalaModuleMimaPreviousVersion.value.map(v => organization.value % moduleName.value % v cross crossVersion.value).toSet,

    canRunMima := {
      val log = streams.value.log
      scalaModuleMimaPreviousVersion.value match {
        case None =>
          log.warn("MiMa will NOT run because no mimaPreviousVersion is provided.")
          false
        case Some(mimaVer) =>
          val exists = artifactExists(organization.value, name.value, scalaBinaryVersion.value, mimaVer, ivySbt.value, streams.value)
          if (!exists)
            log.warn(s"""MiMa will NOT run because the previous artifact "${organization.value}" % "${name.value}_${scalaBinaryVersion.value}" % "$mimaVer" could not be resolved (note the binary Scala version).""")
          exists
      }
    },

    runMimaIfEnabled := Def.taskDyn({
      if (canRunMima.value) Def.task { mimaReportBinaryIssues.value }
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
