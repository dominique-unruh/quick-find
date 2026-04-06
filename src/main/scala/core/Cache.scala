package de.unruh.quickfind
package core

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.PersistenceOld.db
import org.sqlite.{SQLiteConnection, SQLiteException}

import java.nio.file.{Files, Path}
import java.security.MessageDigest
import java.sql.{Connection, DriverManager}
import javax.swing.JOptionPane

/** Persistent cache. To clear it, delete the `.cache` file. */
object Cache {
  val quickfindDir: Path = Path.of("/home/unruh/tmp/quickfind-data")
  private val cachePath = quickfindDir.resolve("cache")
  
  private val logger = Logger[this.type]

  private def sha256(bytes: Array[Byte]): Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest(bytes)

  private lazy val connection: Connection = synchronized {
    logger.debug("Opening SQLite cache")

    val conn = try
      DriverManager.getConnection(s"jdbc:sqlite:$cachePath")
      catch {
        case e: SQLiteException =>
          e.printStackTrace()
          throw RuntimeException(s"Error opening cache $cachePath. Maybe corrupted? Delete that file to fix.", e)
      }
    conn.setAutoCommit(true)

    conn.createStatement().execute(
      """CREATE TABLE IF NOT EXISTS cache (
        |  key_hash BLOB NOT NULL,
        |  key      BLOB NOT NULL,
        |  value    BLOB NOT NULL,
        |  PRIMARY KEY (key_hash, key)
        |)""".stripMargin
    )

    sys.addShutdownHook(cleanup())

    conn
  }

  def ensureAvailable(): Unit = {
    try
      connection
    catch
      case e: Throwable =>
        JOptionPane.showMessageDialog(null, s"Failed to open persistence DB:\n$e", "Failed to open persistence DB", JOptionPane.ERROR_MESSAGE)
        throw e
  }

  private lazy val getStatement = connection.prepareStatement(
    "SELECT value FROM cache WHERE key_hash = ? AND key = ?"
  )

  private def makeKey(table: Array[Byte], clazzKey: Class[?], key: Array[Byte]): Array[Byte] = {
    val builder = Array.newBuilder[Byte]
    builder ++= table += 0
    builder ++= clazzKey.getClass.getName.nn.getBytes += 0
    builder ++= key
    builder.result()
  }
  
  private def get(key: Array[Byte]): Option[Array[Byte]] = synchronized {
    getStatement.setBytes(1, sha256(key))
    getStatement.setBytes(2, key)
    val rs = getStatement.executeQuery()
    try if (rs.next()) Some(rs.getBytes(1)) else None
    finally rs.close()
  }

  def get(table: Array[Byte], clazzKey: Class[?], key: Array[Byte]): Option[Array[Byte]] =
    get(makeKey(table, clazzKey, key))


  def getOrCompute[A](key: Array[Byte], toBytes: A => Array[Byte], fromBytes: Array[Byte] => A)(body: => A): A = synchronized {
    get(key) match {
      case Some(cached) => fromBytes(cached)
      case None => 
        val result = body
        put(key, toBytes(result))
        result
    }
  }

  private lazy val putStatement = connection.prepareStatement(
    """INSERT INTO cache (key_hash, key, value) VALUES (?, ?, ?)
      |ON CONFLICT(key_hash, key) DO UPDATE SET value = excluded.value""".stripMargin
  )

  def put(table: Array[Byte], clazzKey: Class[?], key: Array[Byte], value: Array[Byte]): Unit =
    put(makeKey(table, clazzKey, key), value)

  private def put(key: Array[Byte], value: Array[Byte]): Unit = synchronized {
    putStatement.setBytes(1, sha256(key))
    putStatement.setBytes(2, key)
    putStatement.setBytes(3, value)
    putStatement.executeUpdate()
  }

  private lazy val deleteStatement = connection.prepareStatement(
    "DELETE FROM cache WHERE key_hash = ? AND key = ?"
  )


  def delete(key: Array[Byte]): Unit = synchronized {
    deleteStatement.setBytes(1, sha256(key))
    deleteStatement.setBytes(2, key)
    deleteStatement.executeUpdate()
  }

  def thinOutCache(): Unit = synchronized {
    val fraction: Int = 1000
    assert(fraction > 0)
    logger.info(s"Removing 1/$fraction of all cache entries")
    val statement = connection.prepareStatement(
      s"""DELETE FROM cache WHERE ABS(random()) % 1000 = 0;""")
    try
      statement.executeUpdate()
    finally
      statement.close()
  }

  private def cleanup(): Unit = {
    thinOutCache()
    connection.close()
  }
}