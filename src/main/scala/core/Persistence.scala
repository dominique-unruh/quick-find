package de.unruh.quickfind
package core

import org.rocksdb.{Options, RocksDB}

import java.nio.ByteBuffer

object Persistence {
  val dbDirectory = "/home/unruh/tmp/quickfind-cache"

  RocksDB.loadLibrary()

  private val options = new Options().setCreateIfMissing(true)
  private lazy val db = RocksDB.open(options, dbDirectory)

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
