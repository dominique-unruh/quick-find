package de.unruh.quickfind
package apps.calendar

import java.awt.datatransfer.{DataFlavor, Transferable}
import java.io.File
import scala.jdk.CollectionConverters.given

object StringTransferable {
  def unapply(transferable: Transferable): Option[String] = {
    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
      Some(transferable.getTransferData(DataFlavor.stringFlavor).asInstanceOf[String])
    else
      None
  }
}

