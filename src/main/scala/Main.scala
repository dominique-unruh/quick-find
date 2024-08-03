package de.unruh.quickfind

import core.{ActivationHandler, SearchWindow}
import items.Directory

object Main {
  lazy val ui = new SearchWindow(Directory("/home/unruh"))

  def main(args: Array[String]): Unit = {
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate()).run()
  }
}

