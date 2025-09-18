enablePlugins(JavaAppPackaging)

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys += idePackagePrefix

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "quickfind",
    scalacOptions += "-Yexplicit-nulls",
    idePackagePrefix := Some("de.unruh.quickfind"),
    Compile / mainClass := Some("de.unruh.quickfind.Main"),
    Compile / discoveredMainClasses := Seq(),
//    Universal / javaOptions +=

    libraryDependencies ++= Seq(
      "org.apache.xmlgraphics" % "batik-transcoder" % "1.17",
      "org.apache.commons" % "commons-text" % "1.11.0",
      "org.nibor.autolink" % "autolink" % "0.11.0",
      "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.6",
      "org.rocksdb" % "rocksdbjni" % "10.4.2",
    )
  )

//Compile / mainClass := Some("de.unruh.quickfind.Main")
