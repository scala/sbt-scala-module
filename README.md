# Scala modules sbt plugin
This is an sbt plugin for creating a scala module build.


# Usage

You must be using sbt 0.13 for your projects.  First create a `project/plugins.sbt` files:

    addSbtPlugin("org.scala-lang.modules" % "scala-module-plugin" % "1.0.0")

Then, in your `build.sbt` add:

    scalaModuleSettings
    
    name := "<your module name>"
    
    version := "<your module version>"
    
    // standard stuff follows:
    scalaVersion := "2.11.0-M8"
    
