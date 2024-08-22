package de.unruh.quickfind
package items

import core.{Item, SVGImage, Utils}

import java.io.{FileReader, IOException, UncheckedIOException}
import java.nio.file.{Files, Path}
import scala.collection.IterableOnce
import scala.io.Source
import scala.jdk.StreamConverters.*
import scala.util.Using

/** An item representing a file in the file system. */
sealed class File protected (path: Path) extends Item {
  /** The file name part of the path */
  override val title: String =
    path.getFileName match
      case null => "/"
      case path => path.toString

  override def toString: String = path.toString

  /** Show the file in Thunar file manager */
  override def defaultAction(): Unit =
    Utils.showInFileManager(path)

  override val isFolder: Boolean = Files.isDirectory(path)

  override val icon: SVGImage = if (isFolder) File.folderIcon else File.fileIcon

  override lazy val children: Iterable[Item] =
    if (isFolder) {
      try
        Using.resource (Files.list(path)) { files =>
          for (file <- files.toScala(List))
            yield new File(file) }
      catch
        case _: IOException => Nil
        case _: UncheckedIOException => Nil
    } else
      Nil

  override lazy val previewLine: String =
    try
      if (Files.isRegularFile(path) && Files.isReadable(path))
        Using.resource(Utils.getLines(path)) { _.nextOption.getOrElse("") }
      else
        ""
    catch
      case _ : IOException => ""
}

object File {
  /** Create a [[File]] from a [[Path]]. */
  def apply(path: Path): File = new File(path)
  /** Create a [[File]] from a path string */
  def apply(path: String): File = apply(Path.of(path))
  private[items] val fileIcon = SVGImage.fromResource("/icons/file-svgrepo-com.svg")
  private[items] val folderIcon = SVGImage.fromResource("/icons/file-part-2-svgrepo-com.svg")
}
