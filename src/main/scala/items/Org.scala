package de.unruh.quickfind
package items

import core.{Item, Utils}

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** A file in Emacs org-mode, with headings as children.
 * @param path Path of the org file
 * @param headings Toplevel headings in the file */
class OrgFile private (val path: Path, headings: Seq[OrgHeading]) extends Item {
  override lazy val children: Iterable[Item] = headings
  override def isFolder: Boolean = true
  override def defaultAction(): Unit =
    Utils.showInEditor(path, line=1)
  override val text: String = path.getFileName.toString
}

object OrgFile {
  private def getLineLevel(line: String): Int = {
    var level = 0
    while (line.nonEmpty && level <= line.length && line(level) == '*')
      level += 1
    level
  }

  def apply(path: Path) : OrgFile = {
    final case class OrgHeadingBuilder(title: String, firstLine: Int, subheadings: mutable.Buffer[OrgHeading])
    val stack = mutable.Stack[OrgHeadingBuilder](OrgHeadingBuilder("", 1, new ListBuffer))
    val content = Using (Source.fromFile(path.toFile)) { _.getLines.map(_.stripLineEnd).toVector }.get

    var lineno = 0

    def closeLastLevel() = {
      val last = stack.pop()
      val heading = new OrgHeading(path=path, title=last.title, startLine=last.firstLine, endLine=lineno-1, subheadings=last.subheadings.toSeq)
      stack.head.subheadings += heading
    }

    def closeLevel(level: Int): Unit =
      while (stack.size > level)
        closeLastLevel()

    def addLevel(title: String, level: Int): Unit = {
      assert(stack.size <= level, s"$level, $stack")
      while (stack.size < level)
        stack.push(OrgHeadingBuilder("<missing title>", lineno, new ListBuffer))
      stack.push(OrgHeadingBuilder(title, lineno, new ListBuffer))
      assert(stack.size == level+1)
    }

    for (line <- content) {
      lineno += 1
      println(s"$lineno: $line")
      val level = getLineLevel(line)
      if (level > 0) {
        closeLevel(level)
        addLevel(title = line, level=level)
      }
    }
    lineno += 1
    closeLevel(1)
    assert(stack.size==1)

    new OrgFile(path=path, headings=stack.pop().subheadings.toSeq)
  }
  def apply(string: String): OrgFile = apply(Path.of(string))
}

/**
 * A heading inside an org-file
 * @param path Path to the file
 * @param startLine Line containing the heading (starting from 1)
 * @param endLine End of the text after the heading (incl. subheadings) (starting from 1)
 * @param title Title of the heading
 * @param subheadings Subheadings of this heading
 */
class OrgHeading private[items] (path: Path, startLine: Int, endLine: Int, title: String,
                                 subheadings: Seq[OrgHeading]) extends Item {
  override val children: Iterable[Item] = subheadings
  override lazy val text: String = title

  override def defaultAction(): Unit =
    Utils.showInEditor(path, line=startLine)
}
