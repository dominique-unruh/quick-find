package de.unruh.quickfind
package core

import de.unruh.quickfind.items.{OrgFile, OrgHeading}

import java.nio.file.{Files, Path}
import scala.collection.mutable

/** An item in the search results. May contain other items. */
trait Item {
  /** A descriptive title of the item. Will be used for display and search. */
  def title: String
  /** Default action that will be taken when user presses enter. */
  def defaultAction(): Unit

  /** A single line preview of the item's content */
  def previewLine: String

  /** The children directly contained in this item.
   * Shall return the same children upon each invocation (e.g., `lazy val`). */
  def children: Iterable[Item]
  /** Indicates whether this is a folder.
   * If it has nonempty [[children]], this must return true.
   * If it has empty [[children]], it should return false,
   * unless there is some reason why this item should still be presented
   * as an empty folder to the user.
   * */
  def isFolder: Boolean = children.nonEmpty

  /** The weight of this item.
   * Contents of item with higher weights will be listed later.
   **/
  //noinspection ScalaWeakerAccess
  def selfWeight: Double = 0

  /** The weight of the children item.
   * Will be effectively added onto the weights of all children.
   * (Useful for items where it is resource-intensive to load children, for example.) */
  //noinspection ScalaWeakerAccess
  def childrenWeight: Double = 1

  /** Icon for this image. */
  def icon: ScalableImage /*= Item.defaultIcon*/

  /** An iterable that returns all descendants of this item.
   * May return additional None elements inside the iteration.
   **/
  //noinspection ScalaWeakerAccess
  object recursiveIterable extends Iterable[Option[ItemPath]]:
    override def iterator: Iterator[Option[ItemPath]] = recursiveIterator

  /** Same as [[recursiveIterable]], but as an iterator. */
  final def recursiveIterator : Iterator[Option[ItemPath]] = {
    final case class Entry(weight: Double, returnMyself: Boolean, path: ItemPath)
    given Ordering[Entry] = Ordering.by[Entry, Double](_.weight).reverse
    val unpackQueue = mutable.PriorityQueue[Entry]()
    for (child <- children)
      unpackQueue.enqueue(Entry(child.selfWeight, true, ItemPath(child)))
    new Iterator[Option[ItemPath]]() {
      override def hasNext: Boolean = unpackQueue.nonEmpty
      override def next(): Option[ItemPath] =
        if (Thread.interrupted) throw InterruptedException()
        val entry = unpackQueue.dequeue()
        val path = entry.path
        if (entry.returnMyself)
          unpackQueue.enqueue(Entry(entry.weight + path.last.childrenWeight, false, path))
          Some(path)
        else
          for (child <- entry.path.last.children)
            unpackQueue.enqueue(Entry(entry.weight + child.selfWeight, true, path.append(child)))
          None
    }
  }

  /** Must implement an equals method such that two items are equal iff they represent the same item from the
   * users point of view, even if their internal state (such as cached children) changes, or their content changes.
   * (As a rule of thumb, two objects are the same, if after refreshing their content, and loading all children,
   * they are essentially the same.)
   *
   * The main purpose is for [[Refreshable]], so that [[Refreshable]] can tell which
   * children are new (instead of invalidating the whole subtree).
   **/
  override def equals(obj: Any): Boolean = obj match
    case other: Item => (getClass == other.getClass) && (equalityKey == other.equalityKey)
    case _ => false
  /** Must match [[equals]]. */
  override def hashCode(): Int = (getClass,equalityKey).hashCode
  /** Must be defined to make the contract of [[equals]] and [[hashCode]] true. */
  val equalityKey: AnyRef
}

/** A path of items, i.e., a nonempty sequence of [[Item]]s. */
final class ItemPath private (itemsReversed: List[Item]) extends Iterable[Item] {
  /** Create a path with only a single item. */
  def this(item: Item) = this(List(item))
  /** Extend the path by one more item. (Last item of the current path must be a folder.)
   * O(1). */
  def append(item: Item): ItemPath = ItemPath(item :: itemsReversed)
  /** Iterates over the folders followed by the item */
  override def iterator: Iterator[Item] = itemsReversed.reverseIterator
  override def last: Item = itemsReversed.head
}

object Item {
  val defaultIcon: SVGImage = SVGImage.fromResource("/icons/arrow-interface-next-svgrepo-com.svg")
}
