package de.unruh.quickfind

import core.{ActivationHandler, DemoDirectory, SearchWindow}

import java.nio.file.Path

object Main {
  lazy val ui = new SearchWindow(DemoDirectory(Path.of("/home/unruh")))

  def main(args: Array[String]): Unit = {
    ui.activate()
    new ActivationHandler(appName = "quick-find", command = ui.activate()).run()
  }
}

