package de.unruh.quickfind
package items

import core.{ChildItem, Item, SVGImage, Utils}

import de.unruh.quickfind.items.FileItem.mtimeOf

import java.io.{FileReader, IOException, UncheckedIOException}
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import scala.collection.IterableOnce
import scala.concurrent.duration.Duration
import scala.jdk.StreamConverters.*
import scala.util.Using

/** An item representing a file in the file system. */
sealed class FileItem protected (val parent: Item, path: Path) extends ChildItem {
  if (Item.getCount % 10000 == 0)
    println(path)

  override val persistentKey: String = path.toString

  /** The file name part of the path */
  override val title: String =
    path.getFileName match
      case null => "/"
      case path => path.toString

  override def toString: String = path.toString

  /** Show the file in Thunar file manager */
  override def defaultAction(): Unit =
    Utils.showInFileManager(path)

  override val children: Iterable[ChildItem] = {
    val folder = Files.isDirectory(path) && !Files.isSymbolicLink(path)

    if (folder) {
      try
        val files = Utils.usingWithTimeout(Files.list(path), Duration("60s")) {
          _.toScala(List)
        }
        for (file <- files)
          yield new FileItem(this, file)
      catch
        case _: IOException => Nil
        case _: UncheckedIOException => Nil
    } else
      Nil
  }

  override val isFolder: Boolean = children.nonEmpty

  override val icon: SVGImage = if (isFolder) FileItem.folderIcon else FileItem.fileIcon

  override lazy val previewLine: String =
    try
      if (Files.isRegularFile(path) && Files.isReadable(path))
        Using.resource(Utils.getLines(path)) { _.nextOption.getOrElse("") }
      else
        ""
    catch
      case _ : IOException => ""
}

object FileItem {
  /** Create a [[FileItem]] from a [[Path]]. */
  def apply(parent: Item, path: Path): FileItem = new FileItem(parent, path)
  /** Create a [[FileItem]] from a path string */
  def apply(parent: Item, path: String): FileItem = apply(parent, Path.of(path))
  private[items] val fileIcon = SVGImage.fromResource("/icons/file-svgrepo-com.svg")
  private[items] val folderIcon = SVGImage.fromResource("/icons/file-part-2-svgrepo-com.svg")

  private def mtimeOf(path: Path): Long =
    try Files.getLastModifiedTime(path).toMillis
    catch case _ => -1
}
