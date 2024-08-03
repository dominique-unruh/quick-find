package de.unruh.quickfind
package core

import java.io.{IOException, UncheckedIOException}
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.StreamConverters.*

/** An item in the search results. */
trait Item {
  /** A descriptive text of the item. Will be used for display and search. */
  val text: String
  /** Default action that will be taken when user presses enter. */
  def defaultAction(): Unit
}

/** An item that can contain other items. */
trait Folder extends Item {
  /** The children directly contained in this folder.
   * Shall return the same children upon each invocation (e.g., `lazy val`). */
  def children: Iterable[Item]
  /** The weight of this folder.
   * Contents of folders with higher weights will be listed later.
   * (Useful for folders that are inefficient to load, for example.) */
  //noinspection ScalaWeakerAccess
  def weight: Double = 1

  /** An iterable that returns all descendants of this folder. */
  object recursiveIterable extends Iterable[ItemPath] {
    override def iterator: Iterator[ItemPath] = recursiveIterator
  }

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
        val (weight, path) = queue.dequeue()
        path.item match
          case folder: Folder =>
            for (child <- folder.children)
              queue.enqueue((weight + folder.weight, path.append(child)))
          case _ =>
        path
    }
  }
}

/** A path of items, i.e., a sequence of [[Folder]]s followed by an [[ItemPath]]. */
final class ItemPath private (foldersReverse: List[Folder], val item: Item) extends Iterable[Item] {
  /** Create a path with only a single item. */
  def this(item: Item) = this(Nil, item)
  /** Extend the path by one more item. (Last item of the current path must be a folder.) */
  def append(item: Item): ItemPath = ItemPath(this.item.asInstanceOf[Folder] :: foldersReverse, item)
  /** Iterates over the folders followed by the item */
  override def iterator: Iterator[Item] = (item :: foldersReverse).reverseIterator
}

/** For testing, will be removed */
class DemoFile(path: Path) extends Item {
  override val text: String =
    path.getFileName match
      case null => "null"
      case path => path.toString

  override def toString: String = path.toString

  override def defaultAction(): Unit =
    println(s"default ${path.toString}")
    import scala.sys.process._
    Seq("thunar", "--", path.toString).!
}

/** For testing, will be removed */
class DemoDirectory(path: Path) extends DemoFile(path), Folder {
  override lazy val children: Iterable[Item] =
    try
      for (file <- Files.list(path).toScala(List)) yield
        if Files.isDirectory(file) then
          DemoDirectory(file)
        else
          DemoFile(file)
    catch
      case _ : IOException => Nil
      case _ : UncheckedIOException => Nil
}
