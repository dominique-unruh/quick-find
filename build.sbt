ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "quickfind",
    idePackagePrefix := Some("de.unruh.quickfind")
  )

libraryDependencies += "org.apache.xmlgraphics" % "batik-transcoder" % "1.17"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.11.0"
libraryDependencies += "org.nibor.autolink" % "autolink" % "0.11.0"
// https://mvnrepository.com/artifact/nz.ac.waikato.cms.weka/weka-stable
libraryDependencies += "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.6"
