package de.unruh.quickfind
package items

import core.{ChildItem, Item, Utils}
import core.Utils.unreachable

import org.nibor.autolink.{LinkExtractor, LinkType}

import java.net.{URI, URL}
import java.nio.file.{Files, LinkOption, Path}
import java.util
import scala.collection.{IndexedSeqView, mutable}
import scala.collection.immutable.VectorBuilder
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala}
import scala.util.boundary
import scala.util.boundary.break
import scala.jdk.CollectionConverters.*

object ParseText {
  private val linkExtractor = LinkExtractor.builder()
    .linkTypes(util.EnumSet.of(LinkType.URL, LinkType.EMAIL))
    .build()

  private def parseLineLinkExtractor(parent: Item, line: String, items: VectorBuilder[ChildItem]): Unit = {
    val links = linkExtractor.extractLinks(line)
    for (link <- links.asScala) boundary {
      val prefix = line.substring(0, link.getBeginIndex)
      val linkText = line.substring(link.getBeginIndex, link.getEndIndex)
      val suffix = line.substring(link.getEndIndex)
//      if (seen contains linkText)
//        break()
//      seen.add(linkText)
      link.getType match
        case LinkType.URL =>
          val uri = URI(linkText)
          items += new Link(parent = parent, url = uri.toURL, preview = Some((prefix, linkText, suffix)))
        case LinkType.EMAIL =>
          if (Email.isMessageId(linkText))
            items += new MessageId(parent = parent, address = linkText, preview = Some((prefix, linkText, suffix)))
          else
            items += new Email(parent = parent, address = linkText, preview = Some((prefix, linkText, suffix)))
        case LinkType.WWW =>
          assert(false)
    }
  }

  private val orgLinkRegex = raw"\[\[([^\[\]]+)]]|\[\[([^\[\]]+)]\[([^\[\]]+)]]".r
  private def parseLineOrgLink(parent: Item, path: Path, line: String, items: VectorBuilder[ChildItem]): Unit = {
    for (m <- orgLinkRegex.findAllMatchIn(line)) boundary {
      val linkText: String = {
        val Seq(linkText1, linkText2, _) = m.subgroups : List[String | Null] // The type of Match.subgroups is wrong!
        if (linkText1 != null) linkText1
        else if (linkText2 != null) linkText2
        else unreachable
      }
//      println(s"LINKTEXT: $linkText")
      val (typ, linkBody) =
        if (linkText.startsWith("/") || linkText.startsWith("./"))
          ("file", linkText)
        else {
          val index = linkText.indexOf(':')
          if (index == -1) break()
          (linkText.substring(0, index), linkText.substring(index + 1))
        }

      lazy val (prefix, suffix) =
        (line.substring(0, m.start), line.substring(m.end))

//      println(("*****", typ, linkBody))

      typ match
        case "file" =>
          val filePath = path.getParent.resolve(linkBody).normalize
          if (!Files.exists(filePath)) break()
          items ++= FileItem.fileAsItem(parent = parent, path = filePath, trusted = Utils.trustedLocation(path))
        case "shell" =>
          if (!Utils.trustedLocation(path)) break()
          items += ShellCommand(parent = parent, command = linkBody, trust=ShellCommand.trusted,
            preview = Some((prefix, linkBody, suffix)))
        case "desktopapps" => // TODO could be done as a DirectItem
          items += DesktopAppCollection(parent)
        case "item" => // direct item
          if (!Utils.trustedLocation(path)) break()
          items += DirectItem.instantiate(parent = parent, clazz = linkBody, trust=ShellCommand.trusted)
        case _ =>
    }
  }

  def parseText(parent: Item, path: Path, lines: IndexedSeqView[String]): IndexedSeq[ChildItem] = {
    val items = VectorBuilder[ChildItem]()
//    val seen = mutable.HashSet[String]()
    for (line <- lines)
//      seen.clear()
      parseLineLinkExtractor(parent, line, items)
      parseLineOrgLink(parent, path, line, items)
    items.result
  }
}
