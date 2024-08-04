package de.unruh.quickfind
package core

import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Using

object Utils {
  /** Shows `path` in the Thunar file manager. */
  def showInFileManager(path: Path): Unit = {
    import scala.sys.process._
    Seq("thunar", "--", path.toString).!
  }

  /** Opens `path` in Emacs.
   * @param path File to open
   * @param line Line where to place cursor (-1 to not jump to specific line)
   * @param column Column where to place cursor (-1 to not jump to specific column; must be -1 if `line` is -1)
   */
  def showInEditor(path: Path, line: Int = -1, column: Int = -1): Unit = {
    import scala.sys.process._
    val commandLine = ListBuffer[String]()
    commandLine += "emacsclient"
    (line, column) match
      case (-1, -1) =>
      case (line, -1) => commandLine += s"+$line"
      case (-1, _) => throw new IllegalArgumentException("column passed without line")
      case (line, column) => commandLine += s"+$line:$column"
    commandLine += "--"
    commandLine += path.toString
    commandLine.run()
  }

  /** Returns an iterator over all lines in a file, lineendings stripped. */
  def getLines(path: Path): Iterator[String] = {
    val source = Source.fromFile(path.toFile)
    val lines = source.getLines
    new Iterator[String] {
      override def hasNext: Boolean = {
        val has = lines.hasNext
        if (!has) source.close()
        has
      }
      override def next(): String = lines.next().stripLineEnd
    }
  }
}
