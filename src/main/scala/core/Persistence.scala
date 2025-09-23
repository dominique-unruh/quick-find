package de.unruh.quickfind
package core

import org.rocksdb.{Options, RocksDB}

import java.nio.ByteBuffer
import java.nio.file.Path
import javax.swing.JOptionPane

object Persistence {
  val quickfindDir = Path.of("/home/unruh/tmp/quickfind-data")
  val dbDirectory = quickfindDir.resolve("cache")

  RocksDB.loadLibrary()

  private val options = new Options().setCreateIfMissing(true)
  private lazy val db = RocksDB.open(options, dbDirectory.toString)

  def ensureAvailable(): Unit = {
    try
      db
    catch
      case e: Throwable =>
        JOptionPane.showMessageDialog(null, s"Failed to open persistence DB:\n$e", "Failed to open persistence DB", JOptionPane.ERROR_MESSAGE)
        throw e
  }

  private def makeKey(table: Array[Byte], clazzKey: Class[?], key: Array[Byte]): Array[Byte] = {
    val builder = Array.newBuilder[Byte]
    builder ++= table += 0
    builder ++= clazzKey.getClass.getName.nn.getBytes += 0
    builder ++= key
    builder.result()
  }

  def get(table: Array[Byte], clazzKey: Class[?], key: Array[Byte]): Option[Array[Byte]] =
    Option(db.get(makeKey(table, clazzKey, key)))

  def put(table: Array[Byte], clazzKey: Class[?], key: Array[Byte], value: Array[Byte]): Unit =
    db.put(makeKey(table, clazzKey, key), value)
}
