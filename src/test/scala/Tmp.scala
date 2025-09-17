package de.unruh.quickfind

import items.DesktopApp


object Tmp {
  def main(args: Array[String]): Unit = {
    val apps = DesktopApp.getAllDesktopApps
    for (app <- apps)
      println(app)
  }
}
