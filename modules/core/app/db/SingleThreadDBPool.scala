package core.db

import java.sql.Connection
import scala.collection.mutable
import core.common.Log
import core.common.DBException

/**
 * Database connection pool for single thread application.
 *
 * The purpose of this class is for batch database operations.
 * Each time when the code in batch processes try to connect database, it will get database connection from this class.
 * So only one connection will be created for one database during batch execution.
 *
 * Be careful, this class dose NOT support transaction connection, use DB.withTransaction instead.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object SingleThreadDBPool {
  /**
   * Is data source pool enabled.
   */
  var isEnabled = false

  /**
   * All connections holds in pool.
   */
  private val connections = new mutable.HashMap[String, Connection] with mutable.SynchronizedMap[String, Connection]

  /**
   * Retrieve read or write database connection for giving schema.
   * If connection already in pool, use it directly, otherwise create and maintain a new one.
   *
   * In order to avoid read/write database synchronize issue, always retrieve write connection for batch.
   *
   * @param schema Database schema name.
   * @param readOnly Is read or write database, it will always be false.
   * @return Connection Connection for this database.
   */
  def getConnection(schema: String, readOnly: Boolean = true): Connection = {
    if (!this.isEnabled) throw new DBException("Data source pool is disabled, cannot locate connection for: " + schema)
    val key = this.getKey(schema, false)

    connections.get(key) match {
      case Some(connection) =>
        if (connection.isClosed) {
          Log.debug("Connection [%s] is closed, re-create a new one.".format(key))
          this.binding(DBConfig.getDataSource(schema, false), true).get
        } else connection
      case None => this.binding(DBConfig.getDataSource(schema, false)).get
    }
  }

  /**
   * Retrieve read for write database connection for giving data source.
   * If connection already in pool, use it directly, otherwise create and maintain a new one.
   *
   * @param dataSource Target data source.
   * @return Connection Connection for this database.
   */
  def getConnection(dataSource: DataSource): Connection = {
    if (!this.isEnabled) throw new DBException("Data source pool is disabled, cannot locate connection for: " + dataSource.schema)
    val key = this.getKey(dataSource.schema, dataSource.readOnly)

    connections.get(key) match {
      case Some(connection) =>
        if (connection.isClosed) {
          Log.debug("Connection [%s] is closed, re-create a new one.".format(key))
          this.binding(dataSource, true).get
        } else connection
      case None => this.binding(dataSource).get
    }
  }

  /**
   * Binding one database to pool by giving data source.
   *
   * @param dataSource Information of target database.
   * @param refresh Whether to replace with a new connection if connection already exists.
   * @return Option[Connection] Established connection if connection does not exists in pool.
   */
  private def binding(dataSource: DataSource, refresh: Boolean = false): Option[Connection] = {
    if (!this.isEnabled) throw new DBException("Data source pool is disabled, cannot binding connection for: " + dataSource.toString)
    val key = this.getKey(dataSource)

    if (!connections.contains(key) || refresh) {
      Log.debug("Binding datasource: " + dataSource.toString)
      val connection = DB.createConnection(dataSource.url, dataSource.user, dataSource.password, dataSource.autoCommit)
      connections += (key -> connection)

      Some(connection)
    } else None
  }

  /**
   * Release and remove all connections holds in pool.
   */
  def releaseAllConnections {
    Log.debug("Release all connections in SingleThreadDBPool...")

    connections.foreach(entry => {
      try {
        entry._2.close
      } catch {
        case e: Exception => Log.error("Fail to close connection: " + entry._1, e)
      } finally {
        this.connections.remove(entry._1)
      }
    })
  }

  /**
   * Get pool key for giving data source.
   *
   * @param dataSource Target data source.
   * @return String Pool key.
   */
  private def getKey(dataSource: DataSource) = "%s_%s".format(dataSource.schema, if (dataSource.readOnly) "r" else "w")

  /**
   * Get pool key for giving schema and readability.
   *
   * @param schema Schema name.
   * @param readOnly Read or write database.
   * @return String Pool key.
   */
  private def getKey(schema: String, readOnly: Boolean) = "%s_%s".format(schema, if (readOnly) "r" else "w")
}