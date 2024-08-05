ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "quickfind",
    idePackagePrefix := Some("de.unruh.quickfind")
  )

libraryDependencies += "org.apache.xmlgraphics" % "batik-transcoder" % "1.17"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.11.0"
