package de.unruh.quickfind

import java.awt.Font
import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.channels.ServerSocketChannel
import java.nio.file.{Files, Path, Paths}
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import scala.collection.AbstractIterable

object Main {
  lazy val ui = new SearchWindow(DemoDirectory(Path.of("/home/unruh")))

  def main(args: Array[String]): Unit = {
/*
    def generator(prefix: String): Iterable[String] =
      new AbstractIterable[String]():
        override def iterator: Iterator[String] =
          for (i <- Iterator.from(1)) yield
            Thread.sleep(100)
            s"$prefix#$i"
*/

    ui.activate()
    waitForSignal()
  }


  def waitForSignal() = {
    val socketPath = Path.of("/var/run/user/1000/quick-find/socket")
    Files.createDirectories(socketPath.getParent)
    Files.deleteIfExists(socketPath)
    val socketAddress = UnixDomainSocketAddress.of(socketPath)
    val serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
    serverChannel.bind(socketAddress)
    while (true) {
      val channel = serverChannel.accept()
      channel.close()
      ui.activate()
    }
  }
}
