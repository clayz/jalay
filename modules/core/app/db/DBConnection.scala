package core.db

import java.sql._

import play.api.Play.current
import play.api.db.{DB => PlayDB}

import scala.util.control.Breaks._

import core.common._

/**
 * Provides a high-level API for getting JDBC connections and execute database operations.
 * Play already provided such features by default, but it does not meet our expectation.
 * So we rewrote play's DB API, added more logic for our own purposes:
 *
 * 1. One connection can be reused across multiple DAO methods invocations.
 * 2. Support transaction establish outside DAO module.
 * 3. Support creating database connection without Play framework.
 * 4. Support retry if database connect failed.
 * 5. Support multiple readonly databases and random choose one to use.
 * 6. Divide read and write connections.
 *
 * More play database API information can by found in its source code: play.api.db.DB
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object DBConnection {
  /**
   * Retry times when connect to database failed
   */
  private val CONNECT_FAILED_RETRY_TIMES = 3

  /**
   * Retrieves a JDBC connection from Play DB with retry times if connect failed.
   * Don’t forget to release the connection at some point by calling close().
   *
   * @param schema The database schema name.
   * @param readOnly Get read or write connection.
   * @param autoCommit When `true`, sets this connection to auto-commit.
   * @return Connection Established JDBC connection
   * @throws Exception error if the required data source is not registered
   */
  private def getConnection(schema: String, readOnly: Boolean = true, autoCommit: Boolean = true): Connection = {
    var connection: Connection = null

    breakable {
      for (i <- 0 to CONNECT_FAILED_RETRY_TIMES) {
        val dbName = DBConfig.getDB(schema, readOnly)
        try {
          connection = new AutoCleanConnection(PlayDB.getConnection(dbName, autoCommit))
          break
        } catch {
          case e: Exception =>
            if (i < CONNECT_FAILED_RETRY_TIMES) {
              Log.error("Connect database '" + dbName + "' failed, retried times: " + (i + 1))
              // retry after 1 second
              Thread.sleep(1000)
            } else {
              throw new DBException("Get database connection failed, schema name: " + schema, Log.Level.CRIT)
            }
        }
      }
    }

    connection.setAutoCommit(autoCommit)
    connection
  }

  /**
   * Create a JDBC connection without play framework.
   * Don’t forget to release the connection at some point by calling close().
   *
   * @param url Database URL to connect.
   * @param username Database access user.
   * @param password Database access password.
   * @param autoCommit when `true`, sets this connection to auto-commit.
   * @return Connection Established JDBC connection.
   * @throws Exception error if create database connection failed.
   */
  def createConnection(url: String, username: String, password: String, autoCommit: Boolean = true): Connection =
    try {
      Log.debug(s"Create connection, url: $url, username: $username, password: $password, autoCommit: $autoCommit")
      val connection = new AutoCleanConnection(DriverManager.getConnection(url, username, password))
      connection.setAutoCommit(autoCommit)
      connection
    } catch {
      case e: Throwable => throw new DBException("Create database connection failed: " + url, e, Log.Level.CRIT)
    }

  /**
   * Execute a block of code within a JDBC connection.
   *
   * If it received the implicit connection parameter, it will use it directly instead of create a new one.
   * But in that case, the invoker have to release this connection manually.
   *
   * If there is no active connection, provide one and it will be automatically released.
   *
   * @param schema The schema name.
   * @param readOnly Get read or write connection.
   * @param block Code block to execute.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return [A] Return generic type value.
   */
  private def withConnection[A](schema: String, readOnly: Boolean)(block: Connection => A)(implicit connection: Connection = null): A =
    if (connection != null)
      block(connection)
    else if (SingleThreadDBPool.isEnabled) {
      val connection = SingleThreadDBPool.getConnection(schema, readOnly)
      val result = block(connection)
      connection.asInstanceOf[AutoCleanConnection].releaseStatements
      result
    } else {
      // open an auto commit connection
      val conn = getConnection(schema, readOnly)
      try block(conn) finally conn.close
    }

  /**
   * Execute a block of database read-only code, providing a JDBC connection.
   *
   * If it received the implicit connection parameter, it will use it directly instead of create a new one.
   * But in that case, the invoker have to release this connection manually.
   *
   * If there is no active connection, provide one and it will be automatically released.
   *
   * @param schema The schema name.
   * @param block Code block to execute.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return [A] Return generic type value.
   */
  def withReadConnection[A](schema: String)(block: Connection => A)(implicit connection: Connection = null): A =
    withConnection(schema, readOnly = true)(block)(connection)

  /**
   * Execute a block of database writable code, providing a JDBC connection.
   *
   * If it received the implicit connection parameter, it will use it directly instead of create a new one.
   * But in that case, the invoker have to release this connection manually.
   *
   * If there is no active connection, provide one and it will be automatically released.
   *
   * @param schema The schema name.
   * @param block Code block to execute.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return [A] Return generic type value.
   */
  def withWriteConnection[A](schema: String = AppConfig.appName.toLowerCase)(block: Connection => A)(implicit connection: Connection = null): A =
    withConnection(schema, readOnly = false)(block)(connection)

  /**
   * Execute a block of database code with giving data source, providing a JDBC connection.
   *
   * The data source can be get from core.db.DBConfig#getDataSource function by providing data source name.
   * For example, if we defined a database config in application.conf like:
   *
   * db.demo_r1.driver = com.mysql.jdbc.Driver
   * db.demo_r1.url = "jdbc:mysql://localhost/demo?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull"
   * db.demo_r1.user = root
   * db.demo_r1.password = password
   *
   * The data source name is demo, r1 means it is the first readonly database, so you can get data source by:
   * core.db.DBConfig.getDataSource("demo")
   *
   * If it received the implicit connection parameter, it will use it directly instead of create a new one.
   * But in that case, the invoker have to release this connection manually.
   *
   * If there is no active connection, provide one and it will be automatically released.
   *
   * @param dataSource The target data source to be connected.
   * @param block Code block to execute.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return [A] Return generic type value.
   */
  def withDataSourceConnection[A](dataSource: DataSource)(block: Connection => A)(implicit connection: Connection = null): A =
    if (connection != null)
      block(connection)
    else if (SingleThreadDBPool.isEnabled) {
      val connection = SingleThreadDBPool.getConnection(dataSource)
      val result = block(connection)
      connection.asInstanceOf[AutoCleanConnection].releaseStatements
      result
    } else {
      // open an auto commit connection
      val conn = getConnection(dataSource.schema, dataSource.readOnly)
      try block(conn) finally conn.close
    }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
   *
   * @param schema The database schema name.
   * @param autoCommit Whether auto commit transaction after code executed.
   * @param block Code block to execute.
   * @return [A] Return generic type value.
   */
  def withTransaction[A](schema: String = AppConfig.appName.toLowerCase, autoCommit: Boolean = true)(block: Connection => A): A = {
    Log.debug("Transaction beginning, schema: %s, autoCommit: %s".format(schema, autoCommit.toString))

    // open a non-auto commit connection
    val conn = if (AppConfig.isPlayStart) getConnection(schema, readOnly = false, autoCommit = false)
    else {
      val dataSource = DBConfig.getDataSource(schema, readOnly = false)
      createConnection(dataSource.url, dataSource.user, dataSource.password, autoCommit = false)
    }

    try {
      // execute database operations
      val r = block(conn)
      if (autoCommit) {
        conn.commit
        Log.debug("Transaction commit successfully.")
      }

      Log.debug("Transaction finished, schema: %s, autoCommit: %s".format(schema, autoCommit.toString))
      r
    } catch {
      // rollback transaction
      case e: Exception =>
        conn.rollback
        throw new DBException("Connection rollback.", e)
    } finally conn.close
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction which created by data source.
   * The connection and all created statements are automatically released.
   *
   * @param dataSource The target data source to be connected.
   * @param block Code block to execute.
   * @return [A] Return generic type value.
   */
  def withTransaction[A](dataSource: DataSource)(block: Connection => A): A = {
    Log.debug("Transaction beginning: " + dataSource.toString)

    // open a non-auto commit connection
    val conn = createConnection(dataSource.url, dataSource.user, dataSource.password, autoCommit = false)

    try {
      // execute database operations
      val r = block(conn)
      if (dataSource.autoCommit) {
        conn.commit
        Log.debug("Transaction commit successfully.")
      }

      Log.debug("Transaction finished: " + dataSource.toString)
      r
    } catch {
      // rollback transaction
      case e: Throwable =>
        conn.rollback
        throw new DBException("Connection rollback.", e)
    } finally conn.close
  }
}

/**
 * Provides an interface for retrieving the jdbc driver's implementation of java.sql.Connection
 * from a "decorated" Connection (such as the Connection that DB.withConnection provides). Upcasting
 * to this trait should be used with caution since exposing the internal jdbc connection can violate the
 * guarantees Play otherwise makes (like automatically closing jdbc statements created from the connection)
 *
 * This class was copied and modified from Play source code play.api.db.HasInternalConnection.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
trait HasInternalConnection {
  def getInternalConnection(): Connection
}

/**
 * A connection releasing automatically statements on close.
 * This class was copied and modified from Play source code play.api.db.AutoCleanConnection.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
private class AutoCleanConnection(connection: Connection) extends Connection with HasInternalConnection {

  private val statements = scala.collection.mutable.ListBuffer.empty[Statement]

  private def registering[T <: Statement](b: => T) = {
    val statement = b
    statements += statement
    statement
  }

  def releaseStatements() {
    statements.foreach { statement =>
      statement.close()
    }
    statements.clear()
  }

  override def getInternalConnection(): Connection = connection match {
    case bonecpConn: com.jolbox.bonecp.ConnectionHandle =>
      bonecpConn.getInternalConnection()
    case x => x
  }

  def createStatement() = registering(connection.createStatement())

  def createStatement(resultSetType: Int, resultSetConcurrency: Int) = registering(connection.createStatement(resultSetType, resultSetConcurrency))

  def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))

  def prepareStatement(sql: String) = registering(connection.prepareStatement(sql))

  def prepareStatement(sql: String, autoGeneratedKeys: Int) = registering(connection.prepareStatement(sql, autoGeneratedKeys))

  def prepareStatement(sql: String, columnIndexes: scala.Array[Int]) = registering(connection.prepareStatement(sql, columnIndexes))

  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency))

  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))

  def prepareStatement(sql: String, columnNames: scala.Array[String]) = registering(connection.prepareStatement(sql, columnNames))

  def prepareCall(sql: String) = registering(connection.prepareCall(sql))

  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency))

  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))

  def close() {
    releaseStatements()
    connection.close()
  }

  def clearWarnings() {
    connection.clearWarnings()
  }

  def commit() {
    connection.commit()
  }

  def createArrayOf(typeName: String, elements: scala.Array[AnyRef]) = connection.createArrayOf(typeName, elements)

  def createBlob() = connection.createBlob()

  def createClob() = connection.createClob()

  def createNClob() = connection.createNClob()

  def createSQLXML() = connection.createSQLXML()

  def createStruct(typeName: String, attributes: scala.Array[AnyRef]) = connection.createStruct(typeName, attributes)

  def getAutoCommit() = connection.getAutoCommit()

  def getCatalog() = connection.getCatalog()

  def getClientInfo() = connection.getClientInfo()

  def getClientInfo(name: String) = connection.getClientInfo(name)

  def getHoldability() = connection.getHoldability()

  def getMetaData() = connection.getMetaData()

  def getTransactionIsolation() = connection.getTransactionIsolation()

  def getTypeMap() = connection.getTypeMap()

  def getWarnings() = connection.getWarnings()

  def isClosed() = connection.isClosed()

  def isReadOnly() = connection.isReadOnly()

  def isValid(timeout: Int) = connection.isValid(timeout)

  def nativeSQL(sql: String) = connection.nativeSQL(sql)

  def releaseSavepoint(savepoint: Savepoint) {
    connection.releaseSavepoint(savepoint)
  }

  def rollback() {
    connection.rollback()
  }

  def rollback(savepoint: Savepoint) {
    connection.rollback(savepoint)
  }

  def setAutoCommit(autoCommit: Boolean) {
    connection.setAutoCommit(autoCommit)
  }

  def setCatalog(catalog: String) {
    connection.setCatalog(catalog)
  }

  def setClientInfo(properties: java.util.Properties) {
    connection.setClientInfo(properties)
  }

  def setClientInfo(name: String, value: String) {
    connection.setClientInfo(name, value)
  }

  def setHoldability(holdability: Int) {
    connection.setHoldability(holdability)
  }

  def setReadOnly(readOnly: Boolean) {
    connection.setReadOnly(readOnly)
  }

  def setSavepoint() = connection.setSavepoint()

  def setSavepoint(name: String) = connection.setSavepoint(name)

  def setTransactionIsolation(level: Int) {
    connection.setTransactionIsolation(level)
  }

  def setTypeMap(map: java.util.Map[String, Class[_]]) {
    connection.setTypeMap(map)
  }

  def isWrapperFor(iface: Class[_]) = connection.isWrapperFor(iface)

  def unwrap[T](iface: Class[T]) = connection.unwrap(iface)

  // JDBC 4.1
  def getSchema() = {
    connection.asInstanceOf[ {def getSchema(): String}].getSchema()
  }

  def setSchema(schema: String) {
    connection.asInstanceOf[ {def setSchema(schema: String): Unit}].setSchema(schema)
  }

  def getNetworkTimeout() = {
    connection.asInstanceOf[ {def getNetworkTimeout(): Int}].getNetworkTimeout()
  }

  def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int) {
    connection.asInstanceOf[ {def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int): Unit}].setNetworkTimeout(executor, milliseconds)
  }

  def abort(executor: java.util.concurrent.Executor) {
    connection.asInstanceOf[ {def abort(executor: java.util.concurrent.Executor): Unit}].abort(executor)
  }

}
