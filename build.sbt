enablePlugins(JavaAppPackaging)

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys += idePackagePrefix

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "quickfind",
    scalacOptions ++= "-Yexplicit-nulls -deprecation".split(' ').toSeq,
    idePackagePrefix := Some("de.unruh.quickfind"),
    Compile / mainClass := Some("de.unruh.quickfind.Main"),
    Compile / discoveredMainClasses := Seq(),
//    Universal / javaOptions +=

    libraryDependencies ++= Seq(
      "org.apache.xmlgraphics" % "batik-transcoder" % "1.17",
      "org.apache.commons" % "commons-text" % "1.11.0",
      "org.nibor.autolink" % "autolink" % "0.11.0",
      "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.6",
      "org.rocksdb" % "rocksdbjni" % "10.2.1",
      "javax.mail" % "javax.mail-api" % "1.6.2",
      "com.sun.mail" % "javax.mail" % "1.6.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
      "ch.qos.logback" % "logback-classic" % "1.5.22",
      "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
      "org.aarboard.nextcloud" % "nextcloud-api" % "14.1.0",
      "com.lihaoyi" %% "sourcecode" % "0.4.4",
      "com.typesafe.play" %% "play-json" % "2.10.8",
      "org.mnode.ical4j" % "ical4j" % "4.2.2",
      "net.bytebuddy" % "byte-buddy" % "1.18.3",
      "net.bytebuddy" % "byte-buddy-agent" % "1.18.3",
    )
  )

//Compile / mainClass := Some("de.unruh.quickfind.Main")
