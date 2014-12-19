package core.db

import collection.mutable.ListBuffer
import core.common.DBException

/**
 * SQL statement constants.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Criteria {
  val AND = "and"
  val OR = "or"
  val EQUAL = "="
  val NOT_EQUAL = "<>"
  val GREATER_THAN = ">"
  val GREATER_EQUAL_THAN = ">="
  val LESS_THAN = "<"
  val LESS_EQUAL_THAN = "<="
  val BETWEEN = "between"
  val LIKE = "like"
  val IN = "in"
  val ASC = "asc"
  val DESC = "desc"
}

/**
 * This class provides database query and sort support.
 * All query filters and sorts can be dynamic added into criteria.
 *
 * Then it can be used to generate SQL WHERE and ORDER BY statement.
 * It also holds all data which can be used when adding parameters to query.
 *
 * Some examples for using this class:
 *
 * 1. Common usage
 * 1.1 One parameter:
 * criteria.andEqual('columnName -> value)
 *
 * 1.2 Multiple parameters sample 1:
 * criteria.andEqual('columnName -> value, 'columnName2 -> value2)
 *
 * 1.3 Multiple parameters sample 2:
 * criteria.andEqual('columnName -> value).andEqual('columnName2 -> value2)
 *
 * 2. Equals
 * criteria.andEqual('columnName -> value)
 * criteria.orEqual('columnName -> value)
 * criteria.andNotEqual('columnName -> value)
 *
 * 3. Like
 * criteria.andLike('columnName -> value)
 * criteria.orLike('columnName -> value)
 *
 * 4. GreaterThan
 * criteria.andGreaterThan('columnName -> value)
 * criteria.orGreaterThan('columnName -> value)
 *
 * 5. LessThan
 * criteria.andLessThan('columnName -> value)
 * criteria.orLessThan('columnName -> value)
 *
 * 6. Between
 * criteria.andBetween('columnName, ('alias1 -> smallValue), ('alias2 -> largeValue))
 * criteria.orBetween('columnName, ('alias1 -> smallValue), ('alias2 -> largeValue))
 *
 * 7. ASC, DESC
 * criteria.asc('columnName)
 * criteria.desc('columnName)
 * criteria.asc('columnName1).desc('columnName2)
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class Criteria(offset: Int, limit: Int) {
  /**
   * All criteria filter statement terms
   */
  val conditions = ListBuffer[CriteriaTerm]()

  /**
   * All sort conditions
   */
  val sorts = ListBuffer[(Symbol, String)]()

  /**
   * Data holds for all filter conditions
   */
  private val data = scala.collection.mutable.Map[Symbol, Any]()

  /**
   * Get query data according to column name.
   *
   * @param name Column name.
   * @return Any Corresponding query data.
   */
  def get(name: Symbol): Any = data(name)

  /**
   * Set query data into filter data map.
   *
   * @param name Column name.
   * @param value Data for filter.
   */
  def set(name: Symbol, value: Any) = data += (name -> value)

  /**
   * Get all query data.
   *
   * @return List[(Symbol, Any)] All query data holds in this criteria.
   */
  def parameters: List[(Symbol, Any)] = data.toList

  /**
   * Add equal filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def equal(filters: (Symbol, Any)*): Criteria = this.andEqual(filters: _*)

  /**
   * Add not equal filters with AND restriction for query.
   *
   * @param filters Multiple filter with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def notEqual(filters: (Symbol, Any)*): Criteria = this.andNotEqual(filters: _*)

  /**
   * Add equal filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andEqual(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.EQUAL, filter._2))
    this
  }

  /**
   * Add not equal filters with AND restriction for query.
   *
   * @param filters Multiple filter with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andNotEqual(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.NOT_EQUAL, filter._2))
    this
  }

  /**
   * Add equal filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def orEqual(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.EQUAL, filter._2))
    this
  }

  /**
   * Add like filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def like(filters: (Symbol, Any)*): Criteria = this.andLike(filters: _*)

  /**
   * Add like filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andLike(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.LIKE, {
      filter._2 match {
        case Some(value) => "%" + value.toString + "%"
        case None => None
        case _ => "%" + filter._2.toString + "%"
      }
    }))

    this
  }

  /**
   * Add like filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value).
   * @return Criteria Current query criteria.
   */
  def orLike(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.LIKE, {
      filter._2 match {
        case Some(value) => "%" + value.toString + "%"
        case None => None
        case _ => "%" + filter._2.toString + "%"
      }
    }))
    this
  }

  /**
   * Add greater than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def greaterThan(filters: (Symbol, Any)*): Criteria = this.andGreaterThan(filters: _*)

  /**
   * Add greater than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andGreaterThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.GREATER_THAN, filter._2))
    this
  }

  /**
   * Add greater than filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def orGreaterThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.GREATER_THAN, filter._2))
    this
  }


  /**
   * Add greater or equals than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def greaterEqualThan(filters: (Symbol, Any)*): Criteria = this.andGreaterEqualThan(filters: _*)

  /**
   * Add greater or equals than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andGreaterEqualThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.GREATER_EQUAL_THAN, filter._2))
    this
  }

  /**
   * Add greater or equals than filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def orGreaterEqualThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.GREATER_EQUAL_THAN, filter._2))
    this
  }

  /**
   * Add less than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def lessThan(filters: (Symbol, Any)*): Criteria = this.andLessThan(filters: _*)

  /**
   * Add less than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andLessThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.LESS_THAN, filter._2))
    this
  }

  /**
   * Add less than filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def orLessThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.LESS_THAN, filter._2))
    this
  }

  /**
   * Add less or equals than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def lessEqualThan(filters: (Symbol, Any)*): Criteria = this.andLessEqualThan(filters: _*)

  /**
   * Add less or equals than filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def andLessEqualThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.LESS_EQUAL_THAN, filter._2))
    this
  }

  /**
   * Add less or equals than filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, value)
   * @return Criteria Current query criteria.
   */
  def orLessEqualThan(filters: (Symbol, Any)*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.LESS_EQUAL_THAN, filter._2))
    this
  }

  /**
   * Add between filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, (alias, data), (alias2, data2))
   * @return Criteria Current query criteria.
   */
  def between(filters: (Symbol, (Symbol, Any), (Symbol, Any))*): Criteria = this.andBetween(filters: _*)

  /**
   * Add between filters with AND restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, (alias, data), (alias2, data2))
   * @return Criteria Current query criteria.
   */
  def andBetween(filters: (Symbol, (Symbol, Any), (Symbol, Any))*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.BETWEEN, filter._2, filter._3))
    this
  }

  /**
   * Add between filters with OR restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, (alias, data), (alias2, data2))
   * @return Criteria Current query criteria.
   */
  def orBetween(filters: (Symbol, (Symbol, Any), (Symbol, Any))*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.BETWEEN, filter._2, filter._3))
    this
  }

  /**
   * Add in filters with and restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, List(values))
   * @return Criteria Current query criteria.
   */
  def in(filters: (Symbol, List[Any])*): Criteria = this.andIn(filters: _*)

  /**
   * Add in filters with and restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, List(values))
   * @return Criteria Current query criteria.
   */
  def andIn(filters: (Symbol, List[Any])*): Criteria = {
    filters.foreach(filter => this.and(filter._1, Criteria.IN, filter._2.mkString(",")))
    this
  }

  /**
   * Add in filters with or restriction for query.
   *
   * @param filters Multiple filters with format: (columnName, List(values))
   * @return Criteria Current query criteria.
   */
  def orIn(filters: (Symbol, List[Any])*): Criteria = {
    filters.foreach(filter => this.or(filter._1, Criteria.IN, filter._2.mkString(",")))
    this
  }

  /**
   * Add ascending order by condition.
   *
   * @param column Database column name.
   * @return Criteria Current query criteria.
   */
  def asc(column: Symbol): Criteria = {
    sorts += (column -> Criteria.ASC)
    this
  }

  /**
   * Add descending order by condition.
   *
   * @param column Database column name.
   * @return Criteria Current query criteria.
   */
  def desc(column: Symbol): Criteria = {
    sorts += (column -> Criteria.DESC)
    this
  }

  /**
   * Get SQL WHERE statement which contains all current filters.
   *
   * @return String SQL WHERE statement.
   */
  def where: String = {
    var statement = ""

    if (!conditions.isEmpty)
      conditions.zipWithIndex.foreach(value => {
        if (value._2 == 0)
          statement += value._1.getStatement
        else
          statement += value._1.getRestrictStatement
      })

    statement
  }

  /**
   * Get SQL ORDER BY statement which contains all current sorts.
   *
   * @return String SQL ORDER BY statement.
   */
  def sort: String = {
    var statement = ""

    if (!sorts.isEmpty) {
      val iterator = sorts.iterator

      while (iterator.hasNext) {
        val sort = iterator.next
        statement += sort._1.name + " " + sort._2
        statement += (if (iterator.hasNext) "," else " ")
      }
    }

    statement
  }

  /**
   * Add AND filter for query.
   *
   * @param name Database column name.
   * @param operator EQUAL | NOT_EQUAL | LIKE | GREATER_THAN | LESS_THAN
   * @param value Filter data.
   */
  private def and(name: Symbol, operator: String, value: Any) {
    data += (name -> value)

    value match {
      case Some(_) => conditions += CriteriaTerm(Criteria.AND, operator, name, value = value.asInstanceOf[Option[_]])
      case None => // nothing need to do here
      case _ => conditions += CriteriaTerm(Criteria.AND, operator, name, value = Some(value))
    }
  }

  /**
   * Add AND filter for query.
   *
   * @param name Database column name.
   * @param operator BETWEEN
   * @param mapping1 First filter data.
   * @param mapping2 Second filter data.
   */
  private def and(name: Symbol, operator: String, mapping1: (Symbol, Any), mapping2: (Symbol, Any)) {
    data += (mapping1._1 -> mapping1._2)
    data += (mapping2._1 -> mapping2._2)

    conditions += CriteriaTerm(Criteria.AND, operator, name, Some(mapping1._1), Some(mapping2._1))
  }

  /**
   * Add OR condition for query.
   *
   * @param name Database column name.
   * @param operator EQUAL | NOT_EQUAL | LIKE | GREATER_THAN | LESS_THAN
   * @param value Filter data.
   */
  private def or(name: Symbol, operator: String, value: Any) {
    data += (name -> value)

    value match {
      case Some(_) => conditions += CriteriaTerm(Criteria.OR, operator, name, value = value.asInstanceOf[Option[_]])
      case None => // nothing need to do here
      case _ => conditions += CriteriaTerm(Criteria.OR, operator, name, value = Some(value))
    }
  }

  /**
   * @param name Database column name.
   * @param operator BETWEEN
   * @param mapping1 First filter data.
   * @param mapping2 Second filter data.
   */
  private def or(name: Symbol, operator: String, mapping1: (Symbol, Any), mapping2: (Symbol, Any)) {
    data += (mapping1._1 -> mapping1._2)
    data += (mapping2._1 -> mapping2._2)

    conditions += CriteriaTerm(Criteria.OR, operator, name, Some(mapping1._1), Some(mapping2._1))
  }
}

/**
 * Data holder for one filter condition.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class CriteriaTerm(restrict: String, operator: String, column: Symbol, alias1: Option[Symbol] = None,
                        alias2: Option[Symbol] = None, value: Option[_] = None) {
  private val columnName = column.name

  def getRestrictStatement: String = {
    operator match {
      case Criteria.EQUAL => " %s %s = {%s} ".format(restrict, columnName, columnName)
      case Criteria.NOT_EQUAL => " %s %s <> {%s} ".format(restrict, columnName, columnName)
      case Criteria.GREATER_THAN => " %s %s > {%s} ".format(restrict, columnName, columnName)
      case Criteria.GREATER_EQUAL_THAN => " %s %s >= {%s} ".format(restrict, columnName, columnName)
      case Criteria.LESS_THAN => " %s %s < {%s} ".format(restrict, columnName, columnName)
      case Criteria.LESS_EQUAL_THAN => " %s %s <= {%s} ".format(restrict, columnName, columnName)
      case Criteria.BETWEEN => " %s %s between {%s} and {%s} ".format(restrict, columnName, alias1.get.name, alias2.get.name)
      case Criteria.LIKE => " %s %s like {%s} ".format(restrict, columnName, columnName)
      case Criteria.IN => " %s %s in (%s) ".format(restrict, columnName, value.get)
      case _ => throw new DBException("Unsupported query operator: " + operator)
    }
  }

  def getStatement: String = {
    operator match {
      case Criteria.EQUAL => " %s = {%s} ".format(columnName, columnName)
      case Criteria.NOT_EQUAL => " %s <> {%s} ".format(columnName, columnName)
      case Criteria.GREATER_THAN => " %s > {%s} ".format(columnName, columnName)
      case Criteria.GREATER_EQUAL_THAN => " %s >= {%s} ".format(columnName, columnName)
      case Criteria.LESS_THAN => " %s < {%s} ".format(columnName, columnName)
      case Criteria.LESS_EQUAL_THAN => " %s <= {%s} ".format(columnName, columnName)
      case Criteria.BETWEEN => " %s between {%s} and {%s} ".format(columnName, alias1.get.name, alias2.get.name)
      case Criteria.LIKE => " %s like {%s} ".format(columnName, columnName)
      case Criteria.IN => " %s in (%s) ".format(columnName, value.get)
      case _ => throw new DBException("Unsupported query operator: " + operator)
    }
  }
}