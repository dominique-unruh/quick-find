package de.unruh.quickfind
package items

import core.Item

class Explicit(val text: String, val children: Item*) extends Item {
  override def defaultAction(): Unit = {}
  override def isFolder: Boolean = true
}
