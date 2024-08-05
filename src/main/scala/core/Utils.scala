package de.unruh.quickfind
package core

import org.apache.commons.text.StringEscapeUtils

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
