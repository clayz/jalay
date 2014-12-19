package core.db

import java.util.Date
import collection._
import anorm._
import core.common._

/**
 * This class holds model-table mappings information.
 * It also contains SQL for CRUD operations and provides common functions for SQL usage.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class ModelMapper[T <: Model](schema: String,
                                   table: String,
                                   cache: Boolean,
                                   id: (String, String),
                                   columnMappings: mutable.LinkedHashMap[String, String],
                                   defaultValues: mutable.Map[String, Any]) {

  /**
   * Id column name in database table.
   */
  val idColumn: String = this.id._1

  /**
   * Id attribute name in model.
   */
  val idName: String = this.id._2

  /**
   * SQL select model statement.
   */
  lazy val select: String = "SELECT %s FROM %s WHERE %s = {%s} AND del = false".format(getColumns, table, idColumn, idName)

  /**
   * SQL insert model statement.
   */
  lazy val insert: String = "INSERT INTO %s SET %s".format(table, getSetStatement)

  /**
   * SQL update model statement.
   */
  lazy val update: String = "UPDATE %s SET %s WHERE %s = {%s}".format(table, getSetStatement, idColumn, idName)

  /**
   * SQL change model del to true statement.
   */
  lazy val expire: String = "UPDATE %s SET del = true WHERE %s = {%s}".format(table, idColumn, idName)

  /**
   * SQL delete model statement.
   */
  lazy val delete: String = "DELETE FROM %s WHERE %s = {%s}".format(table, idColumn, idName)

  /**
   * Add mapping between table column name and model attribute name.
   *
   * @param columnName Table column name.
   * @param fieldName Model attribute name, leave None if it is the same as column name.
   */
  def addColumnMapping(columnName: String, fieldName: Option[String] = None, defaultValue: Option[Any]) = {
    val fieldNameValue = fieldName.getOrElse(columnName)
    Log.debug("Add column mapping for table [%s]: %s -> %s".format(this.table, columnName, fieldNameValue))

    if (!this.columnMappings.contains(columnName))
      this.columnMappings += (columnName -> fieldNameValue)

    defaultValue.map {
      value => this.defaultValues += (columnName -> value)
    }
  }

  /**
   * Get all columns as a string separated by comma.
   *
   * @return String All columns separated by comma.
   */
  def getColumns: String = {
    val value = collection.mutable.ListBuffer.empty[String]
    this.columnMappings.keySet.foreach(key => value += "`%s`".format(key))
    value.mkString(",")
  }

  /**
   * Get model attribute value according to attribute name.
   *
   * @param model Target model.
   * @param name Model attribute name.
   * @return Any Model attribute value.
   */
  def getField(model: T, name: String): Any = {
    model.getClass.getMethods.find(_.getName == name).get.invoke(model)
  }

  /**
   * Get model'd Id value.
   *
   * @param model Target model.
   * @return Option[_] Model Id value.
   */
  def getIdValue(model: T): Option[_] = {
    model.getClass.getMethods.find(_.getName == this.idName).get.invoke(model).asInstanceOf[Option[_]]
  }

  /**
   * Get model's all attributes values as Anorm ParameterValue list.
   * This parameters list will be used when update model, all table columns will be updated with model's new data.
   *
   * @param model Target model.
   * @param isInsert If true, set updateDate to "0000-00-00 00:00:00".
   *                 If false, set updateDate to now.
   * @return List[(Any, anorm.ParameterValue[_])] Model's attributes list.
   */
  def getParameters(model: T, isInsert: Boolean = false): List[(Any, anorm.ParameterValue[_])] = {
    val parameters = mutable.ListBuffer.empty[(Any, anorm.ParameterValue[_])]

    this.columnMappings.foreach(mapping => {
      mapping._2 match {
        case "updateDate" => {
          if (isInsert) parameters += (mapping._2 -> Constant.DB.DEFAULT_DATETIME)
          else parameters += (mapping._2 -> new Date)
        }
        case _ => {
          val value = this.getField(model, mapping._2)

          if (value.isInstanceOf[Option[_]] && value.asInstanceOf[Option[_]].isEmpty && this.defaultValues.contains(mapping._1)) {
            // set default value if exists
            parameters += (mapping._2 -> this.defaultValues(mapping._1))
          } else {
            parameters += (mapping._2 -> value)
          }
        }
      }
    })

    parameters.toList
  }

  /**
   * Get the set statement when insert or update model, it will not include id column and should be something like:
   * ... `create_date` = {createDate}, `update_date` = {updateDate}, `del` = {del} ...
   *
   * @return String Set statement with all columns.
   */
  private def getSetStatement: String = {
    val statement = new StringBuilder
    this.columnMappings.foreach(mapper => if (mapper._1 != idColumn) statement.append("`%s` = {%s},".format(mapper._1, mapper._2)))
    statement.dropRight(1).toString
  }
}