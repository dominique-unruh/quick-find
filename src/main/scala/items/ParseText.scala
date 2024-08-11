package de.unruh.quickfind
package items

import core.{Item, Utils}

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

  private def parseLineLinkExtractor(line: String, items: VectorBuilder[Item]): Unit = {
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
          items += new Link(url = uri.toURL, preview = Some((prefix, linkText, suffix)))
        case LinkType.EMAIL =>
          if (Email.isMessageId(linkText))
            items += new MessageId(linkText, preview = Some((prefix, linkText, suffix)))
          else
            items += new Email(linkText, preview = Some((prefix, linkText, suffix)))
        case LinkType.WWW =>
          assert(false)
    }
  }

  private val orgLinkRegex = raw"\[\[([^\[\]]+)]]|\[\[([^\[\]]+)]\[([^\[\]]+)]]".r
  private def parseLineOrgLink(path: Path, line: String, items: VectorBuilder[Item]): Unit = {
    for (m <- orgLinkRegex.findAllMatchIn(line)) boundary {
      val linkText = {
        val Seq(linkText1, linkText2, _) = m.subgroups
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
          val filePath = path.getParent.resolve(linkBody).normalize()
          if (!Files.exists(filePath)) break()
          if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)
            && !Utils.trustedLocation(path))
            break()
          if (linkBody.endsWith(".org") && Files.isRegularFile(filePath))
            items += OrgFile(filePath)
          else
            items += File(filePath)
        case "shell" =>
          if (!Utils.trustedLocation(path)) break()
          items += ShellCommand(linkBody, trust=ShellCommand.trusted,
            preview = Some((prefix, linkBody, suffix)))
        case _ =>
    }
  }

  // TODO REMOVE
  def main(args: Array[String]): Unit = {
    val text =
      """* https://hello.com  x@unruh.de
        | [[file:test.org]]  [[./test2.org][hello]] [[shell:ls]]
        |  [[https://hello2.com]]  https://hello2.com
        | """.stripMargin
    val items = parseText(
      path = Path.of("/tmp/test.txt"),
      lines = text.lines.toList.asScala.map(_.stripLineEnd).toArray.view)
    for (item <- items)
      println(s"Item: $item")
  }

  def parseText(path: Path, lines: IndexedSeqView[String]): IndexedSeq[Item] = {
    val items = VectorBuilder[Item]()
//    val seen = mutable.HashSet[String]()
    for (line <- lines)
//      seen.clear()
      parseLineLinkExtractor(line, items)
      parseLineOrgLink(path, line, items)
    items.result
  }
}
