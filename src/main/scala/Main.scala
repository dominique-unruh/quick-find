package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.{OrgFile, OrgRoot}

import java.nio.file.Path

object Main {
  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (thread, throwable) =>
      println(s"Uncaught exception (in $thread): $throwable")
      throwable.printStackTrace()
    }

    println("Scanning")
    val root: OrgRoot = OrgRoot(Path.of("/home/unruh/r/home/misc/quick-find-menu.org"))
    println("Done")
//    for (path <- root.recursiveIterable)
//      println(path)
    val ui = new SearchWindow(root)
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate).run()
  }
}

