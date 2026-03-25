package de.unruh.quickfind
package items

import core.{Item, SVGImage}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path

class ObsidianVault(parent: Item, path: Path) extends FileItem(parent, path) {
  assert(path.getFileName.toString == ".obsidian")
//  override val icon: SVGImage = SVGImage.fromResource("/icons/obsidian.svg")

  override def defaultAction(): Unit =
    import sys.process.*
    val url = s"obsidian://open?path=${URLEncoder.encode(path.getParent.toString, UTF_8)}"
    Seq("/usr/bin/obsidian", url).run()
}
