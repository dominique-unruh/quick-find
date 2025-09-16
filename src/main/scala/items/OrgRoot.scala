package de.unruh.quickfind
package items

import core.{ChildItem, Item, ScalableImage, Utils}

import de.unruh.quickfind.items.OrgFile.parseOrgFile

import java.nio.file.Path
import scala.collection.IndexedSeqView

class OrgRoot(val path: Path) extends Item {
  private val (headings: Seq[OrgHeading], content: IndexedSeq[String]) =
    parseOrgFile(this, path)
  override val parentOption: Option[Item] = None
  override def toString: String = s"[OrgRoot $path]"
  override val children: Iterable[ChildItem] =
    ParseText.parseText(this, path, preamble) ++ headings
  override val isFolder: Boolean = true
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
  override val persistentKey: String = path.toString
}
