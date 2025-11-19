package de.unruh.quickfind
package core

import de.unruh.quickfind.core.Item.{countCreations, tableName}
import de.unruh.quickfind.items.{OrgFile, OrgHeading}

import java.nio.ByteBuffer
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

/** An item in the search results. May contain other items. */
trait Item {
  countCreations(this)

  val parentOption: Option[Item]

  /** A descriptive title of the item. Will be used for display and search. */
  def title: String

  /** Default action that will be taken when user presses enter. */
  def defaultAction(): Unit

  /** A single line preview of the item's content */
  def previewLine: String

  /** The children directly contained in this item. */
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
   * */
  //noinspection ScalaWeakerAccess
  def selfWeight: Double = 1

  private var _weightAdjustment: Double = Double.MinValue

  def weightAdjustment: Double = {
    if (_weightAdjustment == Double.MinValue) synchronized {
      if (_weightAdjustment == Double.MinValue) {
        Persistence.get(tableName, this.getClass, this.persistentKey) match
          case None => _weightAdjustment = 0
          case Some(encodedWeight) => _weightAdjustment = ByteBuffer.wrap(encodedWeight).getDouble()
      }
    }
    _weightAdjustment
  }

  def updateWeight(f: Double => Double): Unit = synchronized {
    val old = _weightAdjustment
    _weightAdjustment = f(_weightAdjustment)
    println(s"Adjusted weight of $this: $old -> $_weightAdjustment")
    val encodedWeight = ByteBuffer.allocate(8).putDouble(_weightAdjustment).array()
    Persistence.put(tableName, this.getClass, this.persistentKey, encodedWeight)
  }

  def prefer(factor: Double = 1): Unit =
    updateWeight(d => d - 0.1 / Math.ceil(Math.max(1, -d)) * factor)
    for (parent <- parentOption)
      parent.prefer(factor * 0.3)

  def weight: Double = {
    var weight = selfWeight
    for (parent <- parentOption) weight += parent.weight
    weight += weightAdjustment
    weight
  }

  /** Icon for this image. */
  def icon: ScalableImage /*= Item.defaultIcon*/

  def addChildren(builder: mutable.Growable[Item]): Unit = {
    for (child <- children)
      builder.addOne(child)
      child.addChildren(builder)
  }

  val persistentKey: Array[Byte]

  /** Does the predicate apply to this or an ancestor? */
  def hasAncestor(predicate: Item => Boolean): Boolean =
    predicate(this) || parentOption.exists(_.hasAncestor(predicate))

  def pathTo(root: Item | Null): Seq[Item] = {
    val builder = Seq.newBuilder[Item]
    var current = this
    boundary {
      while (true) {
        builder += current
        current.parentOption match {
          case Some(value) if value eq root => break()
          case Some(value) => current = value
          case None => break()
        }
      }
    }
    builder.result().reverse
  }

  /** This must be defined before `val children` is initialized.
   * Must be a normalized Path */
  val underlyingFile: Option[Path]
}

object Item {
  val defaultIcon: SVGImage = SVGImage.fromResource("/icons/arrow-interface-next-svgrepo-com.svg")
  private val count = AtomicInteger(0)
  def getCount: Int = count.get()
  private [Item] def countCreations(current: Item): Unit = {
    val c = count.incrementAndGet()
    if (c % 100000 == 0)
      println(s"Count: $c")
//      new Thread(() => { Thread.sleep(1000); println(current.pathTo(null).mkString(", ")) }).start()
  }

  private val tableName = "weight".getBytes
}

trait ChildItem extends Item {
  val parent: Item
  val parentOption: Some[Item] = Some(parent)
}
