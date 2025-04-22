package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.OrgFile

object Main {
  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (thread, throwable) =>
      println(s"Uncaught exception (in $thread): $throwable")
      throwable.printStackTrace()
    }

    val root = OrgFile("/home/unruh/r/home/misc/quick-find-menu.org")
//    for (path <- root.recursiveIterable)
//      println(path)
    val ui = new SearchWindow(root)
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate).run()
  }
}

