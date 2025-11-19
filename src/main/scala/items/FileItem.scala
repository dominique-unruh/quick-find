package de.unruh.quickfind
package items

import core.{ChildItem, Item, SVGImage, Utils}

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.items.FileItem.{fileAsItem, logger, mtimeOf}

import java.io.{FileReader, IOException, UncheckedIOException}
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, LinkOption, Path}
import java.util
import scala.collection.IterableOnce
import scala.concurrent.duration.Duration
import scala.jdk.StreamConverters.*
import scala.util.{Random, Using, boundary}

/** An item representing a file in the file system. */
sealed class FileItem protected (val parent: Item, path: Path) extends ChildItem {
  //  if (Item.getCount % 100000 == 0)
  //    println(s"FileItem: $path")
  override val underlyingFile: Option[Path] = Some(path.normalize())
  
  override val persistentKey: Array[Byte] = path.toString.getBytes

  /** The file name part of the path */
  override val title: String =
    path.getFileName match
      case null => "/"
      case path => path.toString

  override def toString: String = path.toString

  /** Show the file in Thunar file manager */
  override def defaultAction(): Unit =
//    logger.debug(s"weight=$weight, self=$selfWeight, adj=$weightAdjustment, parent=${parent.weight}")
    Utils.showInFileManager(path)

  def ignoredPath(file: Path): Boolean = {
    if (file.endsWith(".git/objects") && Files.isDirectory(file))
      //      println(s"GIT: $file")
      return true
    if (file.toString == "/home/unruh/.cache")
      return true
    false
  }

  override val children: Iterable[ChildItem] = {
    val folder = Files.isDirectory(path) && !Files.isSymbolicLink(path)

    if (folder) {
      try
        for (file <- Using.resource(Files.list(path))(_.toScala(List))
             if !ignoredPath(file)
             if !parent.hasAncestor(p => p.underlyingFile.contains(file.normalize()))
             )
          yield fileAsItem(parent=this, path=file)
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
        Using.resource(Utils.getLines(path)) { line => Utils.truncate(line.nextOption.getOrElse(""), 500) }
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

  def fileAsItem(parent: Item, path: Path, trusted: Boolean = false): ChildItem = {
//    val item =
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !trusted)
        FileItem(parent, path)
      else if (path.getFileName.toString.endsWith(".org") && Files.isRegularFile(path))
        OrgFile(parent, path)
      else
        FileItem(parent, path)
//    if (Random.between(0, 100000) == 0)
//      println(item.toString)
//    if (item.isInstanceOf[OrgFile])
//      println(s"ORG: $item")
//    if (item.toString.contains("quick-find-menu.org"))
//      println("XXX")
//    if (parent.hasAncestor(p => util.Arrays.equals(p.persistentKey, item.persistentKey)))
//      None
//    else
//      Some(item)
  }

  private val logger = Logger[FileItem]
}
