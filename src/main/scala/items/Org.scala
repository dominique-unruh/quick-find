package de.unruh.quickfind
package items

import core.{Item, SVGImage, ScalableImage, Utils}

import java.nio.file.Path
import scala.collection.immutable.ArraySeq
import scala.collection.{IndexedSeqView, mutable}
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

/** A file in Emacs org-mode, with headings as children.
 * @param path Path of the org file
 * @param headings Toplevel headings in the file */
class OrgFile private (val path: Path, headings: Seq[OrgHeading], content: IndexedSeq[String]) extends Item {
  override lazy val children: Iterable[Item] = headings
  override def isFolder: Boolean = true
  override def defaultAction(): Unit =
    Utils.showInEmacs(path, elispCommands = Seq("(widen)"))
  override val title: String = path.getFileName.toString
  override val icon: ScalableImage = OrgFile.icon
  def preamble: IndexedSeqView[String] =
    if (headings.isEmpty)
      content.view
    else
      content.view.take(headings.head.firstLine - 1)
  override def previewLine: String = if (preamble.nonEmpty) preamble(0) else ""
}

object OrgFile {
  /** Parses a `.org` file and returns an [[OrgFile]].
   * @param path Location of the `.org` file */
  def apply(path: Path) : OrgFile = {
    final case class OrgHeadingBuilder(title: String, firstLine: Int, subheadings: mutable.Buffer[OrgHeading])
    val stack = mutable.Stack[OrgHeadingBuilder](OrgHeadingBuilder("", 1, new ListBuffer))
    val content = Utils.getLines(path).to(ArraySeq)
    var lineno = 0

    def getLineLevel(line: String): Int = {
      var level = 0
      while (line.nonEmpty && level <= line.length && line(level) == '*')
        level += 1
      level
    }

    def closeLastLevel() = {
      val last = stack.pop()
//      val headingContent = content.view.slice(last.firstLine-1, lineno-1)
      val heading = new OrgHeading(path=path, title=last.title, firstLine=last.firstLine, lastLine=lineno-1,
        subheadings=last.subheadings.toSeq, fileContent=content)
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
//      println(s"$lineno: $line")
      val level = getLineLevel(line)
      if (level > 0) {
        closeLevel(level)
        addLevel(title = line, level=level)
      }
    }
    lineno += 1
    closeLevel(1)
    assert(stack.size==1)

    new OrgFile(path=path, headings=stack.pop().subheadings.toSeq, content=content)
  }
  /** Like [[apply(path:Path)]], but the path is given as a string. */
  def apply(path: String): OrgFile = apply(Path.of(path))

  val icon: SVGImage = SVGImage.fromResource("/icons/org-mode-unicorn.svg")
}

/**
 * A heading inside an org-file
 * @param path Path to the file
 * @param firstLine Line containing the heading (starting from 1)
 * @param lastLine End of the text after the heading (incl. subheadings) (starting from 1)
 * @param title Title of the heading
 * @param subheadings Subheadings of this heading
 * @param fileContent content of the whole file
 */
class OrgHeading private[items] (path: Path, val firstLine: Int, lastLine: Int, val title: String,
                                 subheadings: Seq[OrgHeading], fileContent: IndexedSeq[String]) extends Item {
  override val children: Iterable[Item] = subheadings

  def content: IndexedSeqView[String] = fileContent.view.slice(firstLine - 1, lastLine)

  /** Content of this subheading, excluding the heading itself */
  def body: IndexedSeqView[String] = content.drop(1)

  def preamble: IndexedSeqView[String] =
    if (subheadings.isEmpty)
      body
    else
      content.view.slice(firstLine - 1, subheadings.head.firstLine - 1)

  override def defaultAction(): Unit =
    Utils.showInEmacs(path, line=firstLine, elispCommands = Seq("(org-narrow-to-subtree)"))

  override val icon: ScalableImage = OrgFile.icon

  override def previewLine: String =
    if (preamble.nonEmpty) preamble(1) else ""
}
