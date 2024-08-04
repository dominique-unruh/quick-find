package de.unruh.quickfind
package core

import java.awt.Image
import java.io.{IOException, UncheckedIOException}
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
   * (Useful for items where it is resource-intensive to load children, for example.) */
  def weight: Double = 1

  /** Icon for this image. */
  def icon: ScalableImage = Item.defaultIcon

  /** An iterable that returns all descendants of this item. */
  object recursiveIterable extends Iterable[ItemPath]:
    override def iterator: Iterator[ItemPath] = recursiveIterator

  /** Same as [[recursiveIterable]], but as an iterator. */
  final def recursiveIterator : Iterator[ItemPath] = {
    type Entry = (Double, ItemPath)
    given Ordering[Entry] = Ordering.by[Entry, Double](_._1).reverse
    val queue = mutable.PriorityQueue[Entry]()
    for (child <- children)
      queue.enqueue((0, ItemPath(child)))
    new Iterator[ItemPath]() {
      override def hasNext: Boolean = queue.nonEmpty
      override def next(): ItemPath =
        if (Thread.interrupted) throw InterruptedException()
        val (weight, path) = queue.dequeue()
//        println(s" <- $path ($weight)")
        for (child <- path.last.children)
          queue.enqueue((weight + path.last.weight, path.append(child)))
        path
    }
  }
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