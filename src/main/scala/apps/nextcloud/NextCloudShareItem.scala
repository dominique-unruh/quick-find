package de.unruh.quickfind
package apps.nextcloud

import items.DirectItem

import de.unruh.quickfind.core.{Item, ScalableImage}

class NextCloudShareItem(val parent: Item) extends DirectItem {
  private lazy val app = new NextCloudShareApp
  override val persistentKey: Array[Byte] = Array.empty

  override def title: String = "Share via NextCloud"

  override def defaultAction(): Unit =
    app.showApp()

  override def previewLine: String = "Convert files and images to public links via drag&drop, copy&paste"

  // TODO
  override def icon: ScalableImage = Item.defaultIcon
}
