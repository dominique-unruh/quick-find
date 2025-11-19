package de.unruh.quickfind
package items

import core.{ChildItem, Item, ScalableImage}

import java.nio.file.Path

class FakeRoot(create: Item => ChildItem) extends Item {
  override val underlyingFile: Option[Path] = None
  override def defaultAction(): Unit = {}
  override val children: Iterable[ChildItem] = Seq(create(this))
  override def title: String = "fake root"
  override def previewLine: String = "fake root"
  override val parentOption: Option[Item] = None
  override def icon: ScalableImage = Item.defaultIcon
  override val persistentKey: Array[Byte] = Array.empty
}

object FakeRoot {
  /** Allows to instantiate an item for testing purpuses. */
  def itemInRoot(create: Item => ChildItem): ChildItem =
    FakeRoot(create).children.head
  /** Allows to instantiate and run ([[defaultAction]]) an item for testing purposes. */
  def runItemInRoot(create: Item => ChildItem): Unit =
    itemInRoot(create).defaultAction()
}