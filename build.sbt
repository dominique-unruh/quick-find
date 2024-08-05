ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "quickfind",
    idePackagePrefix := Some("de.unruh.quickfind")
  )

// TODO figure out which of these can be omitted
libraryDependencies += "org.apache.xmlgraphics" % "batik-transcoder" % "1.17"
libraryDependencies += "org.apache.xmlgraphics" % "batik-codec" % "1.17"
libraryDependencies += "org.apache.xmlgraphics" % "batik-dom" % "1.17"
libraryDependencies += "org.apache.xmlgraphics" % "batik-svggen" % "1.17"
libraryDependencies += "org.apache.xmlgraphics" % "batik-util" % "1.17"
libraryDependencies += "org.apache.xmlgraphics" % "batik-bridge" % "1.17"

libraryDependencies += "org.apache.commons" % "commons-text" % "1.11.0"
