package de.unruh.quickfind
package core

import java.io.*
import java.nio.channels.{FileChannel, FileLock}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.net.{ServerSocket, Socket}
import scala.util.{Failure, Success, Try, boundary}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.stringSeqToProcess
import scala.util.boundary.break
import scala.util.control.NonFatal

/**
 * SingletonApplication ensures only one instance of the application runs at a time.
 * When a new instance is started, it will terminate any existing instance.
 *
 * Usage:
 * object MyApp extends App {
 *   val singleton = new SingletonApplication("MyAppName")
 *
 *   singleton.ensureSingleInstance()
 *
 *   // Register shutdown hook to clean up
 *   singleton.registerShutdownHook()
 * }
 */

class SingletonApplication(appName: String, lockDirectory: Path) {

//  private val systemTempDir = System.getProperty("java.io.tmpdir")
//  private val lockDirectory = Persistence.dbDirectory
  private val lockFileName = s"$appName.lock"
  private val lockFilePath = lockDirectory.resolve(lockFileName)
  private val pidFileName = s"$appName.pid"
  private val pidFilePath = lockDirectory.resolve(pidFileName)
//  private val portFileName = s"$appName.port"
//  private val portFilePath = lockDirectory.resolve(portFileName)

  private var fileLock: Option[FileLock] = None
  private var lockChannel: Option[FileChannel] = None

  /**
   * Ensures this is the only running instance of the application.
   * If another instance exists, it will be terminated.
   */
  def ensureSingleInstance(): Unit = {
    // Try to acquire the file lock
    boundary { for (i <- 1 to 10) {
      if (acquireFileLock()) break()
      terminatePreviousInstance()
      Thread.sleep(1000)
    }}
    Runtime.getRuntime.addShutdownHook(new Thread(() => cleanup()))
    // Write our PID
    writePidFile()
  }


  private def acquireFileLock() = {
    // Ensure the lock directory exists
    Files.createDirectories(lockFilePath.getParent)

    // Open file channel for the lock file
    val channel = FileChannel.open(
      lockFilePath,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE
    )

    // Try to acquire an exclusive lock
    val lock = channel.tryLock()
    if (lock == null) {
      channel.close()
      false
    } else {
      lockChannel = Some(channel)
      fileLock = Some(lock)
      true
    }
  }

  private def terminatePreviousInstance(): Unit = {
    // Check if there's a previous instance to terminate
    readPidFile() match {
      case Some(pid) =>
        println(s"Forceful termination of $pid")
        forceTerminateProcess(pid)
        println("Forced shutdown successful")
      case _ =>
        println("No previous instance")
        // No previous instance found
    }
  }

  private def forceTerminateProcess(pid: Long): Boolean = {
    try {
      val os = System.getProperty("os.name").toLowerCase
      val command = if (os.contains("windows")) {
        Seq("taskkill", "/F", "/PID", pid.toString)
      } else {
        Seq("kill", "-TERM", pid.toString)
      }

      command.! == 0
    } catch
      case NonFatal(_) => false
  }

  private def writePidFile(): Unit = {
    Try {
      val pid = ProcessHandle.current().pid()
      Files.write(pidFilePath, pid.toString.getBytes)
    }
  }

  private def readPidFile(): Option[Long] = {
    Try {
      val content = new String(Files.readAllBytes(pidFilePath)).trim
      content.toLong
    }.toOption
  }

  private def cleanup(): Unit = {
    // Release file lock
    fileLock.foreach { lock =>
      Try(lock.release())
      fileLock = None
    }

    // Close lock channel
    lockChannel.foreach { channel =>
      Try(channel.close())
      lockChannel = None
    }

    // Clean up temporary files
    Try(Files.deleteIfExists(pidFilePath))
    Try(Files.deleteIfExists(lockFilePath))
  }
}