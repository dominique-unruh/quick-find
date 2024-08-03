package de.unruh.quickfind
package items

import core.{Item, Utils}

import java.io.{IOException, UncheckedIOException}
import java.nio.file.{Files, Path}
import scala.jdk.StreamConverters.*

/** An item representing a file in the file system. */
sealed class File protected (path: Path) extends Item {
  /** The file name part of the path */
  override val text: String =
    path.getFileName match
      case null => "/"
      case path => path.toString

  override def toString: String = path.toString

  /** Show the file in Thunar file manager */
  override def defaultAction(): Unit =
    Utils.showInFileManager(path)

  override val isFolder: Boolean = Files.isDirectory(path)

  override lazy val children: Iterable[Item] = {
    try
      for (file <- Files.list(path).toScala(List))
        yield new File(file)
    catch
      case _: IOException => Nil
      case _: UncheckedIOException => Nil
  }
}

object File {
  /** Create a [[File]] from a [[Path]]. */
  def apply(path: Path): File = new File(path)
  /** Create a [[File]] from a path string */
  def apply(path: String): File = apply(Path.of(path))
}
