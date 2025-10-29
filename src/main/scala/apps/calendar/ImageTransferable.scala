package de.unruh.quickfind
package apps.calendar

import java.awt.Image
import java.awt.datatransfer.{DataFlavor, Transferable}
import java.awt.image.BufferedImage
import java.io.File
import scala.jdk.CollectionConverters.given

/*
Not sure this always is a BufferedImage. If not, we could convert it using this code:

                BufferedImage buffered = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = buffered.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

(untested AI code)
*/

object ImageTransferable {
  def unapply(transferable: Transferable): Option[BufferedImage] = {
    if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      val image = transferable.getTransferData(DataFlavor.imageFlavor)
        .asInstanceOf[BufferedImage]
      Some(image)
    } else
      None
  }
}
