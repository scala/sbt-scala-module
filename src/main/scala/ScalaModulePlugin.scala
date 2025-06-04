package com.lightbend.tools.scalamoduleplugin

import com.github.sbt.osgi.{OsgiKeys, SbtOsgi}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.{HeaderLicense, headerLicense}
import sbt.Keys._
import sbt.{Def, _}
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverGitDescribeOutput
import sbtversionpolicy.SbtVersionPolicyPlugin.autoImport.{Compatibility, versionPolicyCheck, versionPolicyIgnoredInternalDependencyVersions, versionPolicyIntention}

object ScalaModulePlugin extends AutoPlugin {
  object autoImport {
    val scalaModuleRepoName = settingKey[String]("The name of the repository under github.com/scala/.")
    val scalaModuleAutomaticModuleName = settingKey[Option[String]]("Automatic-Module-Name setting for manifest")
    @deprecated("Previous version is now automatically computed by sbt-version-policy. Setting this key has no effect", "2.4.0")
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

  // Settings added to the project scope
  override def projectSettings: Seq[Setting[_]] = Seq()

  // Global settings
  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    // Since we use sbt-dynver, see https://github.com/scalacenter/sbt-version-policy#how-to-integrate-with-sbt-dynver
    versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
  )

  /**
   * Enable `-opt:l:inline`, `-opt:l:project` or `-optimize`, depending on the Scala version.
   *
   * Note that the optimizer is only enabled in CI and not during local development.
   * Thus, for consistent results, release artifacts must only be built on CI --
   * which is the expected norm for Scala modules, anyway.
   */
  lazy val enableOptimizer: Setting[_] = Compile / compile / scalacOptions ++= {
    if (insideCI.value) {
      val log = sLog.value
      val inlineFrom = scalaModuleEnableOptimizerInlineFrom.value
      log.info(s"Running in CI, enabling Scala2 optimizer for module: ${name.value} with -opt-inline-from: $inlineFrom")
      val Ver = """(\d+)\.(\d+)\.(\d+).*""".r
      val Ver(epic, maj, min) = scalaVersion.value
      (epic, maj.toInt, min.toInt) match {
        case ("2", m, _) if m < 12 => Seq("-optimize")
        case ("2", 12, n) if n < 3 => Seq("-opt:l:project")
        case ("2", _, _)           => Seq("-opt:l:inline", "-opt-inline-from:" + inlineFrom)
        case ("3", _, _)           => Nil // Optimizer not yet available for Scala3, see https://docs.scala-lang.org/overviews/compiler-options/optimizer.html
      }
    } else Nil
  }

  /**
   * To be included in the main sbt project of a Scala module.
   */
  lazy val scalaModuleSettings: Seq[Setting[_]] = Seq(
    scalaModuleRepoName := name.value,

    organization := "org.scala-lang.modules",

    // don't use for doc scope, scaladoc warnings are not to be reckoned with
    Compile / compile / scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked"),
    Compile / compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) => Seq("-Xlint")
        case _ => Seq.empty
      }
    },

    // Generate $name.properties to store our version as well as the scala version used to build
    Compile / resourceGenerators += Def.task {
      val props = new java.util.Properties
      props.put("version.number", version.value)
      props.put("scala.version.number", scalaVersion.value)
      props.put("scala.binary.version.number", scalaBinaryVersion.value)
      val file = (Compile / resourceManaged).value / s"${name.value}.properties"
      IO.write(props, null, file)
      Seq(file)
    }.taskValue,

    // note that scalaModuleAutomaticModuleName has no default value, forcing
    // clients of this plugin to explicitly set it
    Compile / packageBin / packageOptions ++=
      (scalaModuleAutomaticModuleName.value match {
        case Some(name) => Seq(Package.ManifestAttributes("Automatic-Module-Name" -> name))
        case None       => Seq()
      }),

    Compile / packageBin / mappings += {
       (baseDirectory.value / s"${name.value}.properties") -> s"${name.value}.properties"
    },

    // needed to fix classloader issues (see scala/scala-xml#20)
    // essentially, the problem is that the run-time bootclasspath leaks into the compilation classpath,
    // so that scalac see classes used to run it, as classes used to compile against...
    // forking uses a minimal classpath, so this craziness is avoided
    // alternatively, manage the scala instance as shown at the end of this file (commented)
    Test / fork := true,

    headerLicense := Some(HeaderLicense.Custom(
      s"""|Scala (https://www.scala-lang.org)
          |
          |Copyright EPFL and Lightbend, Inc. dba Akka
          |
          |Licensed under Apache License 2.0
          |(http://www.apache.org/licenses/LICENSE-2.0).
          |
          |See the NOTICE file distributed with this work for
          |additional information regarding copyright ownership.
          |""".stripMargin)),

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
          <id>Akka</id>
          <name>Lightbend, Inc. dba Akka</name>
        </developer>
      </developers>
    )
  ) ++ mimaSettings

  @deprecated("use scalaModuleOsgiSettings instead", "2.2.0")
  lazy val scalaModuleSettingsJVM: Seq[Setting[_]] = scalaModuleOsgiSettings

  // enables the SbtOsgi plugin and defines some default settings
  lazy val scalaModuleOsgiSettings: Seq[Setting[_]] = SbtOsgi.projectSettings ++ SbtOsgi.autoImport.osgiSettings ++ Seq(
    OsgiKeys.bundleSymbolicName  := s"${organization.value}.${name.value}",
    OsgiKeys.bundleVersion       := osgiVersion.value,

    // Sources should also have a nice MANIFEST file
    packageSrc / packageOptions := Seq(Package.ManifestAttributes(
      ("Bundle-SymbolicName", s"${organization.value}.${name.value}.source"),
      ("Bundle-Name", s"${name.value} sources"),
      ("Bundle-Version", osgiVersion.value),
      ("Eclipse-SourceBundle", s"""${organization.value}.${name.value};version="${osgiVersion.value}";roots:="."""")
    ))
  )

  // a setting-transform to turn the regular version into something osgi can deal with
  private val osgiVersion = version(_.replace('-', '.'))

  // Internal task keys for the versionPolicy settings
  private val runVersionPolicyCheckIfEnabled = taskKey[Unit]("Run versionPolicyCheck if versionPolicyIntention is not set to Compatibility.None.")

  private lazy val mimaSettings: Seq[Setting[_]] = Seq(
    versionPolicyIntention := Compatibility.None,

    runVersionPolicyCheckIfEnabled := Def.taskDyn({
      if (versionPolicyIntention.value != Compatibility.None) Def.task { versionPolicyCheck.value }
      else Def.task {
        streams.value.log.warn("versionPolicyCheck will NOT run because versionPolicyIntention is set to Compatibility.None.")
      }
    }).value,

    Test / test := {
      runVersionPolicyCheckIfEnabled.value
      (Test / test).value
    }
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
