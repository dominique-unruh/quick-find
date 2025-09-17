package de.unruh.quickfind
package items


import core.{ChildItem, Item, SVGImage, ScalableImage}

import java.io.File
import java.nio.file.Path
import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters.given
import scala.sys.process.stringToProcess

case class DesktopApp(
  name: String,
  comment: String,
  exec: String,
  icon: String,
  categories: String,
  hidden: Boolean = false,
  noDisplay: Boolean = false
)


class DesktopAppItem(val parent: Item, app: DesktopApp) extends ChildItem {
  override def title: String = s"App: ${app.name}"
  override def defaultAction(): Unit = DesktopApp.launchApp(app)
  override def previewLine: String = s"${app.comment} \u2013 ${app.exec}"
  override val children: Iterable[ChildItem] = Seq.empty
  override def isFolder: Boolean = false
  // TODO Should come from the app itself, or just something suitable fixed
  override def icon: ScalableImage = SVGImage.fromResource("/icons/file-svgrepo-com.svg")
  override val persistentKey: String = app.toString
}

class DesktopAppCollection(val parent: Item) extends ChildItem {
  override val children: Seq[DesktopAppItem] = DesktopApp.getAllDesktopApps.map(DesktopAppItem(this, _))
  override def title: String = "Desktop apps"
  override def defaultAction(): Unit = {}
  override def previewLine: String = ""
  // TODO something suitable
  override def icon: ScalableImage = SVGImage.fromResource("/icons/file-svgrepo-com.svg")
  override val persistentKey: String = "DesktopAppCollection"
}

object DesktopApp {
  val desktopDirs = List(
    "/usr/share/applications/",
    "/usr/local/share/applications/",
    System.getProperty("user.home").nn + "/.local/share/applications/"
  )

  def parseDesktopFile(file: Path): Option[DesktopApp] = {
    val source = Source.fromFile(file.toFile.nn)
    val lines = source.getLines().toList
    source.close()

    var name = ""
    var comment = ""
    var exec = ""
    var icon = ""
    var categories = ""
    var hidden = false
    var noDisplay = false
    var inDesktopEntry = false

    for (line <- lines) {
      val trimmed = line.trim.nn

      if (trimmed == "[Desktop Entry]") {
        inDesktopEntry = true
      } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        inDesktopEntry = false
      } else if (inDesktopEntry && trimmed.contains("=")) {
        val parts = trimmed.split("=", 2).nn
        if (parts.length == 2) {
          val key = parts(0).nn.trim.nn
          val value = parts(1).nn.trim.nn

          key match {
            case "Name" if name.isEmpty => name = value
            case k if k.startsWith("Name[") => // Skip localized names if we have a default
            case "Comment" if comment.isEmpty => comment = value
            case k if k.startsWith("Comment[") => // Skip localized comments
            case "Exec" => exec = value
            case "Icon" => icon = value
            case "Categories" => categories = value
            case "Hidden" => hidden = value.toLowerCase == "true"
            case "NoDisplay" => noDisplay = value.toLowerCase == "true"
            case _ => // Ignore other fields
          }
        }
      }
    }

    if (name.nonEmpty && exec.nonEmpty && !hidden && !noDisplay) {
      Some(DesktopApp(name, comment, exec, icon, categories))
    } else {
      None
    }
  }

  def getAllDesktopApps: Seq[DesktopApp] = {
    val apps = Seq.newBuilder[DesktopApp]
    val seenNames = mutable.Set[String]()

    for (dir <- desktopDirs) {
      val dirFile = new File(dir)
      if (dirFile.exists() && dirFile.isDirectory) {
        val desktopFiles = dirFile.listFiles().nn.map(_.nn)
          .filter(_.getName.nn.endsWith(".desktop"))

        for (file <- desktopFiles) {
          parseDesktopFile(file.toPath.nn) match {
            case Some(app) if !seenNames.contains(app.name) =>
              apps += app
              seenNames += app.name
            case _ => // Skip duplicates or invalid entries
          }
        }
      }
    }

    apps.result().sortBy(_.name.toLowerCase.nn)
  }


  def sanitizeExecCommand(exec: String): String = {
    // Remove field codes like %f, %F, %u, %U, %i, %c, %k
    exec.replaceAll("%[fFuUick]", "").nn.trim.nn
  }

  def launchApp(app: DesktopApp): Unit = {
    try {
      val command = sanitizeExecCommand(app.exec)
      println(s"Launching: ${app.name}")
      println(s"Command: $command")

      command.run()

      // Don't wait for the process to complete (let it run in background)
      println(s"${app.name} launched successfully!")

    } catch {
      case e: Exception =>
        println(s"Failed to launch ${app.name}: ${e.getMessage}")
    }
  }
}
