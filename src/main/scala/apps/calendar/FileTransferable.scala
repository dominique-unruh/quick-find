package de.unruh.quickfind
package apps.calendar

import java.awt.datatransfer.{DataFlavor, Transferable}
import java.io.File
import scala.jdk.CollectionConverters.given

object FileTransferable {
  def unapplySeq(transferable: Transferable): Option[Seq[File]] = {
    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      val files = transferable.getTransferData(DataFlavor.javaFileListFlavor)
        .asInstanceOf[java.util.List[File]]
      Some(files.asScala.toSeq)
    } else
      None
  }
}
