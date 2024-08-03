package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.File

object Main {
  def main(args: Array[String]): Unit = {
    val ui = new SearchWindow(File("/home/unruh"))
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate()).run()
  }
}

