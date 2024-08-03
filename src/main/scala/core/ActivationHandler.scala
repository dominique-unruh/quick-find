package de.unruh.quickfind
package core

import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.channels.ServerSocketChannel
import java.nio.file.{Files, Path}

/** Creates a unix socket for activating the program and waits for activation.
 * To activate, run, e.g., `socat /dev/null /var/run/user/ID/APPNAME/activate`
 * where `ID` is the numeric id of the current user, and `APPNAME` is `appName`.
 *
 * @param appName name of the app (for the socket file name)
 * @param command Code to run upon activation */
class ActivationHandler(appName: String, command: => Unit) {
  /** The path of the unix socket waiting for activation. */
  val socketPath: Path = Path.of("/var/run/user").resolve(userId.toString).resolve(appName).resolve("activate")

  /** Waits for activation. */
  def run(): Unit = {
    println(s"Listening for activation on $socketPath")
    Files.createDirectories(socketPath.getParent)
    Files.deleteIfExists(socketPath)
    val socketAddress = UnixDomainSocketAddress.of(socketPath)
    val serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
    serverChannel.bind(socketAddress)
    while (true) {
      val channel = serverChannel.accept()
      channel.close()
      try
        command
      catch
        case e: Exception => e.printStackTrace()
    }
  }

  private def userId: Int = {
    import scala.sys.process.*
    val str = Seq("id", "-u").!!
    Integer.parseInt(str.strip())
  }
}
