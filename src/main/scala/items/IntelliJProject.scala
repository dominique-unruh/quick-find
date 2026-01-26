package de.unruh.quickfind
package items

import core.{Item, SVGImage}

import java.nio.file.Path

class IntelliJProject(parent: Item, path: Path) extends FileItem(parent, path) {
//  println(path)
  // TODO better icon
  override val icon: SVGImage = SVGImage.fromResource("/icons/execute-svgrepo-com.svg")

  override def defaultAction(): Unit =
    import sys.process.*
    Seq("/opt/idea/bin/idea", path.getParent.toString).run()
}
