package de.unruh.quickfind
package items

import core.Item

import org.nibor.autolink.{LinkExtractor, LinkSpan, LinkType}

import java.net.{URI, URL}
import java.nio.file.Path
import java.util
import scala.collection.IndexedSeqView
import scala.collection.immutable.VectorBuilder
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala}

object ParseText {
  private val linkExtractor = LinkExtractor.builder()
    .linkTypes(util.EnumSet.of(LinkType.URL, LinkType.EMAIL))
    .build();

  def parseText(path: Path, lines: IndexedSeqView[String]): IndexedSeq[Item] = {
    val items = VectorBuilder[Item]()
    for (line <- lines)
      val links = linkExtractor.extractLinks(line)
      for (link <- links.asScala)
        val prefix = line.substring(0, link.getBeginIndex)
        val linkText = line.substring(link.getBeginIndex, link.getEndIndex)
        val suffix = line.substring(link.getEndIndex)
//        println(s"$prefix##$linkText##$suffix  (${link.getType})")
        link.getType match
          case LinkType.URL =>
            val uri = URI(linkText)
            items += new Link(url=uri.toURL, preview=Some((prefix,linkText,suffix)))
          case LinkType.EMAIL =>
            if (Email.isMessageId(linkText))
              items += new Email(linkText, preview=Some((prefix,linkText,suffix)))
            else
              items += new MessageId(linkText, preview=Some((prefix,linkText,suffix)))
          case LinkType.WWW =>
            assert(false)

    items.result
  }
}
