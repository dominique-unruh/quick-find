package de.unruh.quickfind
package items

import core.{Item, Utils}

import com.orgzly.org.parser.{OrgNode, OrgParsedFile, OrgParser, OrgParserSettings}

import java.io.FileReader
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

/** A file in Emacs org-mode, with headings as children. */
class OrgFile private (val path: Path, orgFile: OrgParsedFile) extends Item {
  override lazy val children: Iterable[Item] =
    orgFile.getHeadsInList.asScala.map(new OrgHeading(this, _))
  override def isFolder: Boolean = true
  override def defaultAction(): Unit =
    Utils.showInEditor(path, line=1)
  override val text: String = path.getFileName.toString
}

object OrgFile {
  private val settings = OrgParserSettings.getBasic
  def apply(path: Path) : OrgFile = {
    val builder = OrgParser.Builder()
    val orgFile = builder.setInput(new FileReader(path.toFile)).build().parse()
    new OrgFile(path, orgFile)
  }
  def apply(string: String): OrgFile = apply(Path.of(string))
}

class OrgHeading private[items] (file: OrgFile, node: OrgNode) extends Item {
  override val children: Iterable[Item] = Nil
  override lazy val text: String = node.getHead.getTitle

  override def defaultAction(): Unit =
    Utils.showInEditor(file.path, line=1)
}
