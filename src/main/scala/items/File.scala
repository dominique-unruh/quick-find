package de.unruh.quickfind
package items

import core.{Folder, Item}

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

  /** Show the file in Thunar */
  override def defaultAction(): Unit =
    println(s"default ${path.toString}")
    import scala.sys.process._
    Seq("thunar", "--", path.toString).!
}

object File {
  /** Create a [[File]] from a [[Path]].
   * May return a [[Directory]] */
  def apply(path: Path): File =
    if (Files.isDirectory(path))
      new Directory(path)
    else
      new File(path)
}

/** An item representing a folder in the file system. */
final class Directory private[items] (path: Path) extends File(path), Folder {
  override lazy val children: Iterable[Item] =
    try
      for (file <- Files.list(path).toScala(List)) yield
        if Files.isDirectory(file) then
          new Directory(file)
        else
          new File(file)
    catch
      case _ : IOException => Nil
      case _ : UncheckedIOException => Nil
}

object Directory {
  /** Create a [[Directory]] from a [[Path]] */
  def apply(path: Path): Directory =
    if (Files.isDirectory(path))
      new Directory(path)
    else
      throw new IOException("Not a directory")

  /** Create a [[Directory]] from a path string */
  def apply(path: String): Directory = apply(Path.of(path))
}