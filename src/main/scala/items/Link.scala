package de.unruh.quickfind
package items

import core.{Item, LeafItem, SVGImage, ScalableImage, SnippetPreviewItem, Utils}

import java.net.URL

class Link(url: URL, preview: Option[(String,String,String)])
  extends SnippetPreviewItem(preview), LeafItem{
  override def title: String = url.toString
  override def icon: ScalableImage = Link.icon
  override def defaultAction(): Unit =
    Utils.showInBrowser(url)
}

object Link {
  val icon = SVGImage.fromResource("/icons/browser-google-chrome-svgrepo-com.svg")
}