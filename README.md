# Scala modules sbt plugin
This is an sbt plugin for use in defining scala modules.  It tries to simplify the act of creating
a scala module build, ensure their are hooks for breaking cycles when doing a "universal rebuild" of the scala ecosystem, and
provide hooks for using the partest testing framework.


# Usage

You must be using sbt 0.13 for your projects.  First create a `project/plugins.sbt` files:

    addSbtPlugin("org.scala-lang.modules" % "scala-module-plugin" % "0.1")

Then, in your `build.sbt` add:

    scalaModuleSettings
    
    name := "<your module name>"
    
    version := "<your module version>"
    
    // standard stuff follows:
    scalaVersion := "2.11.0-M5"
    
    // NOTE: not necessarily equal to scalaVersion
    // (e.g., during PR validation, we override scalaVersion to validate,
    // but don't rebuild scalacheck, so we don't want to rewire that dependency)
    scalaBinaryVersion := "2.11.0-M5"
