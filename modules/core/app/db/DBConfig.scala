package core.db

import scala.util.Random
import core.common._

/**
 * This class handled all databases configuration and database route.
 *
 * It will load all database configurations from application config file, maintenance a datasource mapping,
 * separate read/write databases, and determine which database to be used for each query at runtime.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object DBConfig {
  /**
   * Random number generator.
   */
  private val RANDOM: Random = new Random

  /**
   * Database mappings with format like:
   * (dbName -> (List(dbName_r1, dbName_r2), dbName_w))
   */
  private var db = Map.empty[String, (List[String], String)]

  /**
   * Get certain DB config name with 'r*' or 'w' postfix which has been defined in application.conf by schema name.
   * This method support multiple readonly database and only one write database.
   *
   * If the readOnly is true, return a random readonly database connection.
   *
   * @param name Database schema name.
   * @param readOnly Is read or write database to connect.
   * @return String Database config name for establish connection.
   */
  def getDB(name: String, readOnly: Boolean = true): String = {
    val dbConfig = if (db.contains(name)) db(name) else initDB(name)

    if (readOnly) {
      if (dbConfig._1.isEmpty)
        throw new DBException("No read database config found for schema: %s, mode: %s".format(name, AppMode.getCurrentMode), Log.Level.CRIT)

      val index = RANDOM.nextInt % dbConfig._1.length
      dbConfig._1(if (index >= 0) index else -index)
    } else {
      dbConfig._2
    }
  }

  /**
   * Get data source configurations from application config file.
   * For example, if we defined a database config in application.conf like:
   *
   * db.demo_r1.driver = com.mysql.jdbc.Driver
   * db.demo_r1.url = "jdbc:mysql://localhost/demo?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull"
   * db.demo_r1.user = root
   * db.demo_r1.password = password
   *
   * The data source name is demo, r1 means it is the first readonly database, so you can get data source by:
   * getDataSource("demo")
   *
   * @param name Data source name.
   * @param readOnly Is read or write database to connect.
   * @return DataSource Data source configurations.
   */
  def getDataSource(name: String, readOnly: Boolean = true, autoCommit: Boolean = true): DataSource = {
    val nameWithPostfix = this.getDB(name, readOnly)

    DataSource(name, readOnly,
      AppConfig.get("db.%s.url".format(nameWithPostfix)),
      AppConfig.get("db.%s.user".format(nameWithPostfix)),
      AppConfig.get("db.%s.password".format(nameWithPostfix)),
      autoCommit)
  }

  /**
   * Initialize and return new DB configurations if it does not exist in DB mappings map.
   *
   * @param name Database schema name.
   * @return (List[String], String) Database read/write configurations.
   */
  private def initDB(name: String): (List[String], String) = {
    this.synchronized {
      db.get(name) match {
        case Some(value) => value
        case _ => {
          Log.debug("Initialize database config for schema: " + name)

          // generate datasource name according to current application mode
          val configName =
            if (AppMode.isAdmin) "admin_" + name
            else if (AppMode.isBatch) "batch_" + name
            else name

          // find datasource by giving name directly
          AppConfig.getOption("db.%s.url".format(configName)) match {
            case Some(datasource) => {
              // no salve database config been defined, use it as both read and write directly
              db += (name ->(List(configName), configName))
            }
            case None => {
              // parse write and multiple read database config
              val reads = collection.mutable.ListBuffer.empty[String]

              List.range(1, 100).takeWhile(index => {
                AppConfig.getOption("db.%s_r%d.url".format(configName, index)).isDefined
              }).foreach(index => {
                val dbKey = "%s_r%d".format(configName, index)
                reads += dbKey
                Log.debug("Added read database: " + dbKey)
              })

              // currently only support one write DB
              db += (name ->(reads.toList, configName + "_w"))
              Log.debug("Added write database: " + configName + "_w")
            }
          }

          db(name)
        }
      }
    }
  }
}

/**
 * Data source configuration values holder.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class DataSource(schema: String, readOnly: Boolean, url: String, user: String, password: String, autoCommit: Boolean = true) {
  val host = {
    val tmp = url.substring(13)
    tmp.substring(0, tmp.indexOf("/"))
  }
}