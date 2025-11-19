package de.unruh.quickfind

import core.{ActivationHandler, ChildItem, Item, Persistence, ScalableImage, SearchWindow, SingletonApplication}
import items.{DirectItem, OrgFile, OrgRoot}

import java.nio.file.Path
import scala.compiletime.uninitialized

object Main {
  private var ui: SearchWindow = uninitialized

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (thread, throwable) =>
      println(s"Uncaught exception (in $thread): $throwable")
      throwable.printStackTrace()
    }

    val singleton = SingletonApplication("quickfind", Persistence.quickfindDir)
    singleton.ensureSingleInstance()

    Persistence.ensureAvailable()

    def loadRoot(): OrgRoot = OrgRoot(Path.of("/home/unruh/r/home/misc/quick-find-menu.org"))
    ui = new SearchWindow(loadRoot)
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate).run()
  }

  def reloadRoot(): Unit = {
    ui.reloadRoot()
    ui.activate()
  }
}

class ReloadQuickfind(val parent: Item) extends DirectItem {
  override val underlyingFile: Option[Path] = None
  
  override def title: String = "Reload quickfind"

  override def defaultAction(): Unit = {
    println("reload quickfind default action")
    try {
      Main.reloadRoot()
    } catch {
      case e : Throwable => println(e)
    }
  }

  override def previewLine: String = "Scan for changed files in this search app"
  
  // TODO
  override def icon: ScalableImage = Item.defaultIcon

  override val persistentKey: Array[Byte] = Array.empty
}