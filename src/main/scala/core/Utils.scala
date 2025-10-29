package de.unruh.quickfind
package core

import org.apache.commons.io.FilenameUtils
import org.apache.commons.text.StringEscapeUtils
import org.apache.xmlgraphics.io.Resource

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.{BufferedReader, File, FileReader}
import java.lang.ref.Cleaner
import java.net.URL
import java.nio.file.{Files, Path}
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Using
import scala.util.Using.Releasable
import scala.util.control.NonFatal

object Utils {
  /** Shows `path` in the Thunar file manager. */
  def showInFileManager(path: Path): Unit = {
    import scala.sys.process._
    Seq("thunar", "--", path.toString).!
  }

  /** Opens `path` in Emacs.
   * @param path File to open
   * @param line Line where to place cursor (-1 to not jump to specific line)
   */
  def showInEmacs(path: Path, line: Int = -1, elispCommands: Seq[String] = Seq.empty): Unit = {
    import scala.sys.process._
    val commandLine = ListBuffer[String]()
    val quotedPath = StringEscapeUtils.escapeJava(path.toString)
    commandLine += "emacsclient"
    commandLine += "--eval"
    commandLine += s"""(find-file "$quotedPath")"""
    if (line > 0)
      commandLine += s"""(goto-line $line)"""
    commandLine += "(raise-frame)"
    for (elisp <- elispCommands)
      assert(elisp.startsWith("("), elisp)
      assert(elisp.endsWith(")"), elisp)
      commandLine += elisp
    println(s"Invoking ${commandLine.mkString(" ")}")
    commandLine.run()
  }

  private val cleaner = Cleaner.create()

  def registerWithCleaner(obj: Any, cleanup: => Unit): Unit =
    cleaner.register(obj, () => cleanup)

  /** Returns an iterator over all lines in a file, lineendings stripped.
   *
   * Closes the file automatically when all lines are read,
   * and when the iterator is garbage collected,
   * and the iterator can also be used with [[Using]].
   * */
  def getLines(path: Path): Iterator[String] & AutoCloseable = {
    val source = Source.fromFile(path.toFile)
    val lines = source.getLines
    object iterator extends Iterator[String], AutoCloseable:
      override def hasNext: Boolean = {
        val has = lines.hasNext
        if (!has) source.close()
        has
      }
      override def next(): String = lines.next()
      override def close(): Unit =
        source.close()
    registerWithCleaner(iterator, source.close())
    iterator
  }
  
  def showInBrowser(url: URL): Unit =
    import scala.sys.process._
    Seq("firefox", "--", url.toString).run()

  def unreachable: Nothing =
    throw AssertionError("unreachable code")

  private val trustedLocations = Seq(
    Path.of("/home/unruh/r/home/misc")
  )
  def trustedLocation(path: Path): Boolean = {
    val absPath = path.normalize().toAbsolutePath
    trustedLocations.exists(dir => absPath.startsWith(dir))
  }

  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

  def usingWithTimeout[R : Releasable, A](resource: R, duration: Duration)(body: R => A) : A = {
    scheduledExecutor.schedule((() => implicitly[Releasable[R]].release(resource)) : Runnable,
      duration.toMicros, TimeUnit.MICROSECONDS)
    Using.resource(resource)(body)
  }

  def firstLine(file: File): Option[String] = {
    val reader = BufferedReader(FileReader(file))
    try {
      try {
        Some(reader.readLine())
      } catch {
        case NonFatal(_) => None
      }
    } finally {
      reader.close()
    }
  }

  def uniqueFileName(path: java.nio.file.Path): java.nio.file.Path = {
    if (!Files.exists(path)) {
      return path
    }

    val fileName = path.getFileName.toString
    val parent = path.getParent

    // Split filename and extension
    val lastDot = fileName.lastIndexOf('.')
    val (baseName, extension) = if (lastDot > 0) {
      (fileName.substring(0, lastDot), fileName.substring(lastDot))
    } else {
      (fileName, "")
    }

    // Find unique name
    var counter = 1
    var newPath = parent.resolve(s"$baseName-$counter$extension")
    while (Files.exists(newPath)) {
      counter += 1
      newPath = parent.resolve(s"$baseName-$counter$extension")
    }

    newPath
  }

  def copyToClipboard(string: String): Unit = {
    val selection = StringSelection(string)
    Toolkit.getDefaultToolkit.getSystemClipboard.setContents(selection, null)
  }
}
