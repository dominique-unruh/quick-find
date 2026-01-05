package de.unruh.quickfind

import items.{DesktopApp, DirectItem, ShellCommand}

import de.unruh.quickfind.core.{ChildItem, Item, ScalableImage}

import java.nio.file.Path


object Tmp {
  def main(args: Array[String]): Unit = {
    val root = new Item {
      override val parentOption: Option[Item] = None

      override def title: String = "fakeroot"

      /** Default action that will be taken when user presses enter. */
      override def defaultAction(): Unit = {}

      /** A single line preview of the item's content */
      override def previewLine: String = "fakeroot"

      /** The children directly contained in this item.
       * Shall return the same children upon each invocation (e.g., `lazy val`). */
      override val children: Iterable[ChildItem] = Seq.empty

      /** Icon for this image. */
      override def icon: ScalableImage = Item.defaultIcon

      override val persistentKey: Array[Byte] = Array.empty
      /** This must be defined before `val children` is initialized.
       * Must be a normalized Path */
      override val underlyingFile: Option[Path] = None
    }
    val item = DirectItem.instantiate(root, "de.unruh.quickfind.ReloadQuickfind", ShellCommand.trusted)
    println(item)
  }
}

/*
package utils

import com.typesafe.scalalogging.Logger
import org.rocksdb.{Options, RocksDB, RocksDBException}

import java.io.File
import java.nio.file.{Files, Path}
import scala.annotation.tailrec

/** Persistent cache. To clear it, delete the `.cache` directory. */
object Cache {
  RocksDB.loadLibrary()

  private val options = new Options().setCreateIfMissing(true)
  private var _cache: Option[RocksDB] = None
  private var lastAccess: Long = 0

  @tailrec
  private def openAvailableCache(count: Int = 1, max: Int = 10): RocksDB = {
    logger.debug(s"Trying to open cache .cache/$count")
    Files.createDirectories(Path.of(".cache"))
    if (count >= max)
      RocksDB.open(options, s".cache/$count")
    else
      try
        RocksDB.open(options, s".cache/$count")
      catch
        case e: RocksDBException if e.getMessage.contains("lock") =>
          openAvailableCache(count + 1, max)
  }

  def cache: RocksDB = synchronized {
    _cache.getOrElse {
      logger.debug("Opening cache")
      val db = openAvailableCache()
      _cache = Some(db)

      // Register shutdown hook to clean up
      sys.addShutdownHook {
        synchronized {
          _cache.foreach(_.close())
          _cache = None
          options.close()
        }
      }

      db
    }
  }



  // Call this when Play reloads (in Global or ApplicationLifecycle)
  def close(): Unit = synchronized {
    _cache match
      case Some(c) =>
        logger.debug("Closing cache")
        c.close()
        _cache = None
      case None =>
  }

  private val logger = Logger[this.type]
}

 */