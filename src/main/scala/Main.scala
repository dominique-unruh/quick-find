package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.{Explicit, File, OrgFile}

object Main {
  def main(args: Array[String]): Unit = {
    val root1 = File("/home/unruh")
    val root2 = OrgFile("/home/unruh/r/home/misc/private-processes.org")
    val root3 = OrgFile("/home/unruh/r/home/misc/work-processes.org")
    val root = Explicit("Main Menu", root1, root2, root3)
    val ui = new SearchWindow(root)
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate()).run()
  }
}

