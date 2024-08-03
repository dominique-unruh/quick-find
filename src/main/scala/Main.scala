package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.{File, OrgFile}

object Main {
  def main(args: Array[String]): Unit = {
//    val root = File("/home/unruh")
    val root = OrgFile("/home/unruh/r/home/misc/private-processes.org")
    val ui = new SearchWindow(root)
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate()).run()
  }
}

