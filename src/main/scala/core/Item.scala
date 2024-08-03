package de.unruh.quickfind

import java.io.{IOException, UncheckedIOException}
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.StreamConverters.*

trait Item {
  val text: String
  def defaultAction(): Unit
}

final class ItemPath(foldersReverse: List[Folder], val item: Item) extends Iterable[Item] {
  def this(item: Item) = this(Nil, item)
  def append(item: Item): ItemPath = ItemPath(this.item.asInstanceOf[Folder] :: foldersReverse, item)
  override def iterator: Iterator[Item] = (item :: foldersReverse).reverseIterator
}


trait Folder extends Item {
  def children: Iterable[Item]
  def weight: Double = 1

  object recursiveIterable extends Iterable[ItemPath] {
    override def iterator: Iterator[ItemPath] = recursiveIterator
  }

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