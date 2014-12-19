package core.db

import scala.collection.mutable
import core.common.Constant.DB._

/**
 * Database query pagination wrapper, all list functions provided by base DAO will return this object.
 *
 * Each time when search a paginated data, it will find one more data than query limitation.
 * Then use this data to determine where it has next page.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class Results[T](offset: Int,
                      limit: Int,
                      original: Seq[T],
                      var total: Long = 0) {

  def this(sql: Sql[_], original: Seq[T]) = this(sql.limits._1, if (sql.limits._2 == LIMIT_ALL) LIMIT_ALL else sql.limits._2 - 1, original)

  /**
   * Validations
   */
  require(offset >= 0)
  require(limit >= 0 || limit == LIMIT_ALL)

  /**
   * Results data after pagination.
   */
  val data: Seq[T] = if ((limit != LIMIT_ALL) && (original.size > limit)) original.dropRight(1) else original

  /**
   * Whether next page exist.
   */
  val hasNext: Boolean = if (LIMIT_ALL == limit) false else original.size == limit + 1

  /**
   * Get results data size
   */
  def size: Int = data.size

  /**
   * Check whether results data is empty.
   */
  def isEmpty: Boolean = this.size == 0

  /**
   * Generate response list according to current results data.
   *
   * @param f Function to mapping one row to a Map.
   * @return Response data.
   */
  def map(f: T => Map[Symbol, Any]): Seq[Map[Symbol, Any]] = data.map(data => f(data))

  /**
   * Generate response map according to current results data with pagination info.
   *
   * @param f Function to mapping one row to a Map.
   * @return Map[Symbol, Any] Response data.
   */
  def mapPaging(f: T => Map[Symbol, Any]): Map[Symbol, Any] = {
    Map[Symbol, Any](
      'offset -> this.offset,
      'limit -> this.limit,
      'has_next -> this.hasNext,
      'data -> data.map(data => f(data)))
  }

  /**
   * Generate response map according to current results data with pagination info.
   * This function is required for using jqGrid, because it has certain response data format.
   *
   * @param f Function to mapping one row to a LinkedHashMap.
   * @return LinkedHashMap[Symbol, Any] Response data.
   */
  def mapGrid(f: T => mutable.LinkedHashMap[Symbol, Any]): mutable.LinkedHashMap[Symbol, Any] = {
    val currentPage = offset / limit + 1
    val totalPages = if (total % limit == 0) total / limit else total / limit + 1

    mutable.LinkedHashMap[Symbol, Any](
      'page -> currentPage,
      'total -> totalPages,
      'records -> total,
      'rows -> data.map(data => f(data)))
  }

  /**
   * Override object methods
   */
  override def toString: String = "[Results] offset: %s, limit: %s, hasNext: %s, data: %s".format(offset, limit, hasNext, data.toString)
}