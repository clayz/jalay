package core.db

import java.util.Date
import java.sql.Connection
import anorm._
import core.cache.ModelCache
import core.common._
import core.common.Constant.DB._

/**
 * Base model class, all models created in this project should inherit this class.
 * A model is a Java POJO, it should represent a database table, and all columns in that table should mapping to model attributes.
 *
 * The are some default attributes in base model, which means all tables must contain these columns.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class Model extends Serializable {
  def createDate: Date

  def updateDate: Option[Date]

  def del: Boolean

  def note: String
}

/**
 * Base model DAO class, all DAO object must inherit this class with a generic type of corresponding model.
 *
 * This class provide common database operate methods for DAO.
 * It support model load, save, remove automatic, and provide query and write functions.
 *
 * Memcached already been integrated in this class, all model will be cached automatically.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class Dao[T <: Model](val schema: String = AppConfig.appName.toLowerCase,
                               val table: String,
                               val cache: Boolean = true,
                               val cacheAutoRefresh: Boolean = true,
                               val id: (String, String) = ("id", "id")) {
  /**
   * Model mapper, which holds database table info for this model.
   */
  private val mapper: ModelMapper[T] = ModelMapper[T](
    schema = schema,
    table = table,
    cache = cache,
    id = id,
    columnMappings = collection.mutable.LinkedHashMap.empty[String, String],
    defaultValues = collection.mutable.Map.empty[String, Any])

  /**
   * Default model parser which contains all columns.
   */
  protected val parser: RowParser[T]

  /**
   * Default count parser, this parser only accept one result data which names "total".
   * In order to use this parser, the SQL must be something like:
   *
   * SELECT count(...) AS total FROM ...
   */
  protected val totalParser = {
    SqlParser.get[Long]("total") map {
      case total => total.toLong
    }
  }

  /**
   * Create new Sql object for database query and modifications.
   *
   * @return Sql[T] Sql object for generating SQL statement.
   */
  def SQL: Sql[T] = Sql(mapper)

  /**
   * Add model attribute and database column mappings info.
   * It used to generate RowParser for Anorm and save model-table relative info.
   *
   * @param columnName Database column name.
   * @param fieldName Model attribute name, leave it null if it is the same as column name.
   * @param defaultValue Default value for this field. In principle, all default value should be defined in Model's constructor,
   *                     but there are some special case such as datetime. The default value for datetime is "00-00-00 00:00:00",
   *                     It is a string, not Date type. So this default value can be defined here for using.
   * @param extractor Used by Anorm framework.
   * @return RowParser[A] Anorm RowParser.
   */
  def column[A](columnName: String, fieldName: String = null, defaultValue: Any = null)(implicit extractor: anorm.Column[A]): RowParser[A] = {
    mapper.addColumnMapping(
      columnName,
      if (fieldName != null) Some(fieldName) else None,
      if (defaultValue != null) Some(defaultValue) else None)

    SqlParser.get[A](columnName)
  }

  /**
   * Load model from cache or database by model Id.
   *
   * @param id Model unique Id.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[T] Retrieved model from cache or database.
   */
  def load(id: Any, useCache: Boolean = true)(implicit connection: Connection = null): Option[T] = {
    /**
     * Get model from database by Id.
     */
    def getFromDB: Option[T] = {
      DB.withReadConnection(mapper.schema) {
        implicit connection =>
          anorm.SQL(mapper.select).on(mapper.idName -> id).as(parser.singleOpt)
      }
    }

    if (useCache && isCacheEnabled) {
      // cache enabled, try to get model from cache first.
      // if model not found in cache, get from database and put it into cache.
      val key = ModelCache.getKey(this.getClass, id)

      ModelCache.get(key) match {
        case Some(value) => Some(value).asInstanceOf[Option[T]]
        case _ => getFromDB match {
          case Some(value) =>
            ModelCache.set(key, value)
            Some(value)
          case _ => None
        }
      }
    } else getFromDB
  }

  /**
   * Insert or update model.
   * If the id value in giving model is None, insert a new model.
   * If the id value in giving model is not None, update existing model by this Id and remove previous model from cache.
   *
   * @param model Model to be insert or update.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Int If insert, return new model's Id.
   *         If update, return how many rows effected.
   */
  def save(model: T)(implicit connection: Connection = null): Int = {
    DB.withWriteConnection(mapper.schema) {
      implicit connection =>
        if (mapper.getIdValue(model).isDefined) {
          // has id value, update this model
          val result = anorm.SQL(mapper.update).on(mapper.getParameters(model): _*).executeUpdate

          // update cache
          if (isCacheEnabled) {
            ModelCache.remove(this.getClass, mapper.getIdValue(model))
            if (cacheAutoRefresh) ModelCache.refreshCollectionTimestamp(this.getClass)
          }

          result
        } else {
          // id is undefined, insert this model
          val result: Option[Long] = anorm.SQL(mapper.insert).on(mapper.getParameters(model, isInsert = true): _*).executeInsert()

          // update cache
          if (isCacheEnabled && cacheAutoRefresh) ModelCache.refreshCollectionTimestamp(this.getClass)

          if (result.isDefined) result.get.toInt else -1
        }
    }
  }

  /**
   * Delete model by Id and remove it from cache.
   * The data will be logically deleted be default, which mean only change delete flag to true.
   *
   * @param id Model unique Id.
   * @param isPhysical If true, delete data physical.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Int Execution result value to indicate success or failed.
   */
  def remove(id: Any, isPhysical: Boolean = false)(implicit connection: Connection = null): Int = {
    val result = DB.withWriteConnection(mapper.schema) {
      implicit connection =>
        anorm.SQL(if (isPhysical) mapper.delete else mapper.expire).on(mapper.idName -> id).executeUpdate
    }

    // update cache
    if (isCacheEnabled) {
      ModelCache.remove(this.getClass, id)
      if (cacheAutoRefresh) ModelCache.refreshCollectionTimestamp(this.getClass)
    }

    result
  }

  /**
   * Get single model from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[T] Retrieved single model.
   * @throws Exception Duplicate data found.
   */
  protected def getModel(sql: Sql[T], useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Option[T] = {
    get(sql, Some(parser), useCache, false, params: _*)
  }

  /**
   * Get single row from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[List[Any] Retrieved single row.
   * @throws Exception Duplicate data found.
   */
  protected def getRow(sql: Sql[T], useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Option[List[Any]] = {
    get(sql, None, useCache, false, params: _*)
  }

  /**
   * Get single map from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[Map[String, Any] Retrieved single map.
   * @throws Exception Duplicate data found.
   */
  protected def getMap(sql: Sql[T], useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Option[Map[String, Any]] = {
    get(sql, None, useCache, true, params: _*)
  }

  /**
   * Get user defined object from cache or database according to parser.
   *
   * @param sql SQL query object to be executed.
   * @param parser Customized query result parser.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[A] Retrieved single object.
   * @throws Exception Duplicate data found.
   */
  protected def getObject[A](sql: Sql[T], parser: RowParser[A], useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Option[A] = {
    get(sql, Some(parser), useCache, false, params: _*)
  }

  /**
   * Get data from cache or database for different result type.
   *
   * @param sql SQL query object to be executed.
   * @param parser Customized query result parser.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param isReturnMap Is return map as result type
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Option[A] Retrieved object.
   * @throws Exception Duplicate data found.
   */
  private def get[A](sql: Sql[T], parser: Option[RowParser[A]], useCache: Boolean, isReturnMap: Boolean, params: (Symbol, Any)*)(
    implicit connection: Connection = null): Option[A] = {
    def getFromDB: Option[A] = {
      // no need to retrieve one more row for get query
      sql.limits = (sql.limits._1, sql.limits._2 - 1)
      val result = readFromDB(sql, parser, isReturnMap, params: _*)

      // data length check
      if (result.length > 1)
        throw new DBException("Duplicate data found: " + result.toString)
      else
        result.headOption
    }

    // get data from cache or database
    if (useCache && isCacheEnabled) {
      // generate query filter for cache key
      val filter = collection.mutable.ArrayBuffer.empty[(Symbol, Any)]
      filter ++= params.toList

      // cache enabled, try to get model from cache first.
      // if model not found in cache, get from database and put it into cache.
      val key = ModelCache.getKey(getClass, getCallerMethod, filter)

      ModelCache.get(key) match {
        case Some(value) => Some(value).asInstanceOf[Option[A]]
        case _ => getFromDB match {
          case Some(value) =>
            ModelCache.set(key, value, cacheAutoRefresh)
            Some(value)
          case _ => None
        }
      }
    } else getFromDB
  }

  /**
   * List models from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param isPaging Whether query one more row for pagination support.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return ListResult[T] Retrieved models list.
   */
  protected def listModel(sql: Sql[T], isPaging: Boolean = true, useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Results[T] = {
    list(sql, Some(parser), useCache, false, params: _*)
  }

  /**
   * List rows from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param isPaging Whether query one more row for pagination support.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[List[Any] Retrieved row list.
   */
  protected def listRow(sql: Sql[T], isPaging: Boolean = true, useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Results[List[Any]] = {
    list(sql, None, useCache, false, params: _*)
  }

  /**
   * List maps from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param isPaging Whether query one more row for pagination support.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[Map[String, Any] Retrieved map list.
   */
  protected def listMap(sql: Sql[T], isPaging: Boolean = true, useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Results[Map[String, Any]] = {
    list(sql, None, useCache, true, params: _*)
  }

  /**
   * List user defined objects from cache or database.
   *
   * @param sql SQL query object to be executed.
   * @param parser Customized query result parser.
   * @param isPaging Whether query one more row for pagination support.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[Map[String, Any] Retrieved object list.
   */
  protected def listObject[A](sql: Sql[T], parser: RowParser[A], isPaging: Boolean = true, useCache: Boolean = true)(params: (Symbol, Any)*)(
    implicit connection: Connection = null): Results[A] = {
    list(sql, Some(parser), useCache, false, params: _*)
  }

  /**
   * List data from cache or database for different result type.
   *
   * @param sql SQL query object to be executed.
   * @param parser Customized query result parser.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param isReturnMap Is return map as result type
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  private def list[A](sql: Sql[T], parser: Option[RowParser[A]], useCache: Boolean, isReturnMap: Boolean, params: (Symbol, Any)*)(
    implicit connection: Connection = null): Results[A] = {
    val (offset, limit) = sql.limits

    // list data from cache or database
    if (useCache && isCacheEnabled) {
      // generate query filter for cache key
      val filter = collection.mutable.ArrayBuffer[(Symbol, Any)]('offset -> offset, 'limit -> limit)
      filter ++= params.toList
      sql.sqlWrapper.map(sqlWrapper => sqlWrapper.sort.map(sort => filter += ('sort -> sort)))

      // cache enabled, try to get model from cache first.
      // if model not found in cache, get from database and put it into cache.
      val key = ModelCache.getKey(getClass, getCallerMethod, filter)

      ModelCache.get(key) match {
        case Some(value) => value.asInstanceOf[Results[A]]
        case _ =>
          val dbFound = readFromDB(sql, parser, isReturnMap, params: _*)
          val result = new Results(sql, dbFound.asInstanceOf[List[A]])
          ModelCache.set(key, result, cacheAutoRefresh)
          result
      }
    } else new Results[A](sql, readFromDB(sql, parser, isReturnMap, params: _*))
  }

  /**
   * List model with pagination info from cache or database.
   * It will execute two statement, first count all rows, then retrieve target list.
   *
   * @param sql SQL query object to be executed.
   * @param criteria Criteria object with paging info.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  protected def pagingModel(sql: Sql[T], criteria: Criteria, useCache: Boolean = true)(
    implicit connection: Connection = null): Results[T] = {
    this.paging(sql, criteria, Some(parser), useCache, isReturnMap = false)
  }

  /**
   * List row with pagination info from cache or database.
   * It will execute two statement, first count all rows, then retrieve target list.
   *
   * @param sql SQL query object to be executed.
   * @param criteria Criteria object with paging info.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  protected def pagingRow(sql: Sql[T], criteria: Criteria, useCache: Boolean = true)(
    implicit connection: Connection = null): Results[List[Any]] = {
    this.paging(sql, criteria, None, useCache, isReturnMap = false)
  }

  /**
   * List user defined objects with pagination info from cache or database.
   * It will execute two statement, first count all rows, then retrieve target list.
   *
   * @param sql SQL query object to be executed.
   * @param criteria Criteria object with paging info.
   * @param parser Customized query result parser.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  protected def pagingObject[A](sql: Sql[T], criteria: Criteria, parser: RowParser[A], useCache: Boolean = true)(
    implicit connection: Connection = null): Results[A] = {
    this.paging(sql, criteria, Some(parser), useCache, isReturnMap = false)
  }

  /**
   * List maps with pagination info from cache or database.
   * It will execute two statement, first count all rows, then retrieve target list.
   *
   * @param sql SQL query object to be executed.
   * @param criteria Criteria object with paging info.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  protected def pagingMap[A](sql: Sql[T], criteria: Criteria, useCache: Boolean = true)(
    implicit connection: Connection = null): Results[A] = {
    this.paging(sql, criteria, None, useCache, isReturnMap = true)
  }

  /**
   * List data with pagination info from cache or database.
   * It will execute two statement, first count all rows, then retrieve target list.
   *
   * @param sql SQL query object to be executed.
   * @param criteria Criteria object with paging info.
   * @param parser Customized query result parser.
   * @param useCache Whether use cache or retrieve from database directly.
   * @param isReturnMap Is return map as result type
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Results[A] Retrieved data results.
   */
  private def paging[A](sql: Sql[T], criteria: Criteria, parser: Option[RowParser[A]], useCache: Boolean, isReturnMap: Boolean)(
    implicit connection: Connection = null): Results[A] = {
    if (sql.nativeSql.isDefined) throw new DBException("Auto pagination for native SQL is not supported.")

    // add where statement in criteria into SQL object
    sql where criteria.where

    // add sort and limit statement
    sql order criteria.sort
    sql limit(criteria.offset, criteria.limit)

    val results: Results[A] = list(sql, parser, useCache, false, criteria.parameters: _*)
    results.total = this.countFromDB(sql, criteria.parameters: _*)
    results
  }

  /**
   * Execute the SQL statement in a read-only database with pagination support
   * and return results with corresponding data type.
   *
   * @param sql SQL query object to be executed.
   * @param parser Customized query result parser.
   * @param isReturnMap Is return map as result type
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return List[Any] A list of result rows.
   */
  private def readFromDB[A](sql: Sql[T], parser: Option[RowParser[_]], isReturnMap: Boolean, params: (Symbol, Any)*)(
    implicit connection: Connection = null): List[A] = {
    val simpleSql = getAnormSql(sql, false, params: _*)

    // get data from database
    DB.withReadConnection(mapper.schema) {
      implicit connection =>
        if (parser.isDefined)
          simpleSql.as(parser.get *).asInstanceOf[List[A]]
        else if (isReturnMap)
          simpleSql().map(row => row.asMap).toList.asInstanceOf[List[A]]
        else
          simpleSql().map(row => row.asList).toList.asInstanceOf[List[A]]
    }
  }

  /**
   * Count total rows for query.
   *
   * @param sql SQL query object to be executed.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Long Total rows in DB.
   */
  private def countFromDB(sql: Sql[T], params: (Symbol, Any)*)(implicit connection: Connection = null): Long = {
    val simpleSql = getAnormSql(sql, true, params: _*)

    // count data from database
    DB.withReadConnection(mapper.schema) {
      implicit connection =>
        simpleSql.as(totalParser.single)
    }
  }

  /**
   * Execute the giving SQL statement in a writable database.
   *
   * @param sql SQL object to be executed.
   * @param params Query parameters.
   * @param connection Reuse a provided connection instead of create a new one.
   * @return Int Number of rows been updated.
   */
  def write(sql: Sql[T])(params: (Symbol, Any)*)(implicit connection: Connection = null): Int = {
    val result = DB.withWriteConnection(mapper.schema) {
      implicit connection =>
        getAnormSql(sql, false, params: _*).executeUpdate
    }

    // update cache
    if (isCacheEnabled && cacheAutoRefresh) ModelCache.refreshCollectionTimestamp(this.getClass)

    result
  }

  /**
   * Execute a block of code within transaction.
   *
   * @param autoCommit Whether auto commit transaction after code finished.
   * @param block Code block to execute.
   * @return A Statement execution result.
   */
  def withTransaction[A](autoCommit: Boolean = true)(block: Connection => A): A = {
    val result = DB.withTransaction(mapper.schema, autoCommit)(block)

    // update cache
    if (isCacheEnabled && cacheAutoRefresh) ModelCache.refreshCollectionTimestamp(this.getClass)

    result
  }

  /**
   * Clear all specification testing data by certain note value.
   */
  def clearSpecData = {
    val note = "Specification testing data."
    this.listModel(SQL where "note = {note}" limit LIMIT_ALL)('note -> note).data.map(model => this.remove(mapper.getIdValue(model), isPhysical = true))
  }

  /**
   * Get sum value by database result data.
   *
   * @param value Database result data.
   * @return Long Sum value get from data.
   */
  def getSumValue(value: Any): Long = {
    value match {
      case v: Int => v
      case v: Long => v
      case Some(v: Int) => v
      case Some(v: Long) => v
      case Some(v: java.math.BigDecimal) => v.longValue
      case None | null => 0
      case _ => throw new DBException("Unknown sum data: " + value.toString)
    }
  }

  /**
   * Clear all cache for model collection data.
   */
  def clearCollectionCache = ModelCache.refreshCollectionTimestamp(this.getClass)

  /**
   * Clear all cache for entire model data.
   */
  def clearCache = ModelCache.refreshGlobalTimestamp(this.getClass)

  /**
   * Create Anorm SQL object by our Sql object and parameters.
   *
   * @param sql Sql object for model.
   * @param isCount Which SQL want to get, count rows SQL or select data SQL.
   * @param params Query parameters.
   * @return SimpleSql[Row] Anorm SimpleSql object.
   */
  private def getAnormSql(sql: Sql[T], isCount: Boolean, params: (Symbol, Any)*): SimpleSql[Row] = {
    val parameters = scala.collection.mutable.ListBuffer.empty[(Any, anorm.ParameterValue[_])]
    params.foreach(value => parameters += (value._1 -> value._2))
    anorm.SQL(if (isCount) sql.countStatement else sql.statement).on(parameters.toList: _*)
  }

  /**
   * Get Dao function's invoker method name.
   *
   * @return String Invoker method name.
   */
  private def getCallerMethod: String = Thread.currentThread.getStackTrace()(4).getMethodName

  /**
   * Check whether cache is enabled for this model. Cache enabled only when:
   * 1. Play is start
   * 2. Memcached is enabled in application config
   * 3. Cache for this model is enabled
   *
   * @return Boolean Whether cache enabled.
   */
  private def isCacheEnabled: Boolean = cache && ModelCache.isEnabled && AppConfig.isPlayStart
}
