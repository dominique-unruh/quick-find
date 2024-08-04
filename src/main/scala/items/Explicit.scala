package de.unruh.quickfind
package items

import core.{Item, Utils}

import java.io.IOException
import java.nio.file.Path

class Explicit(val title: String, val children: Item*) extends Item {
  override def defaultAction(): Unit = {}
  override def isFolder: Boolean = true
  override def previewLine: String = ""
}

object Explicit {
  private val types = Map[String, String => Item](
    "ORG" -> OrgFile.apply,
    "FILE" -> File.apply,
  )

  def fromFile(path: Path, text: String = null): Explicit = {
    val children: Iterator[Item] =
      for (line0 <- Utils.getLines(path);
           line = line0.strip()
           if line != ""
           if !line.startsWith("#")) yield {
        val index = line.indexOf(':')
        if (index == -1)
          throw new IOException(s"""Missing : in line "$line"""")
        val (typ, info0) = line.splitAt(index)
        val info = info0.stripPrefix(":").strip()
        val factory = types.getOrElse(typ,
          throw new IOException(s"""Unknown type $typ in line "$line""""))
        factory(info)
      }
    Explicit(title=Option(text).getOrElse(path.toString),
      children=children.toSeq*)
  }
}