package de.unruh.quickfind
package core

import de.unruh.quickfind.core.Item.countCreations
import de.unruh.quickfind.items.{OrgFile, OrgHeading}

import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

/** An item in the search results. May contain other items. */
trait Item {
  countCreations()

  val parentOption: Option[Item]
  /** A descriptive title of the item. Will be used for display and search. */
  def title: String
  /** Default action that will be taken when user presses enter. */
  def defaultAction(): Unit

  /** A single line preview of the item's content */
  def previewLine: String

  /** The children directly contained in this item.
   * Shall return the same children upon each invocation (e.g., `lazy val`). */
  val children: Iterable[ChildItem]
  /** Indicates whether this is a folder.
   * If it has nonempty [[children]], this must return true.
   * If it has empty [[children]], it should return false,
   * unless there is some reason why this item should still be presented
   * as an empty folder to the user.
   * */
  def isFolder: Boolean = children.nonEmpty

  /** The weight of this item.
   * Contents of item with higher weights will be listed later.
   * (In the final ordering, weights from the parents will be added to this.)
   **/
  //noinspection ScalaWeakerAccess
  def selfWeight: Double = 0

  def weight: Double = parentOption match {
    case Some(parent) => selfWeight + parent.weight
    case None => selfWeight
  }

  /** Icon for this image. */
  def icon: ScalableImage /*= Item.defaultIcon*/

  def addThisAndChildren(builder: mutable.Growable[Item]): Unit = {
    builder.addOne(this)
    for (child <- children)
      child.addThisAndChildren(builder)
  }

  val persistentKey: String
}

object Item {
  val defaultIcon: SVGImage = SVGImage.fromResource("/icons/arrow-interface-next-svgrepo-com.svg")
  private val count = AtomicInteger(0)
  def getCount: Int = count.get()
  private [Item] def countCreations(): Unit = {
    val c = count.incrementAndGet()
    if (c % 10000 == 0)
      println(s"Count: $c")
  }
}

trait ChildItem extends Item {
  val parent: Item
  val parentOption = Some(parent)
}
