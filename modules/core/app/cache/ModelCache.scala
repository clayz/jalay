package core.cache

import java.util.Date
import core.common.AppConfig

/**
 * Model cache support class, it contains all cache functions for model and responsible for cache key generation.
 * It also contains model cache timestamp for expiration functionality with two approach:
 *
 * 1. Global timestamp
 *
 * Every cache key contains this timestamp, keys for same model has same timestamp value.
 * Once we want to expire all cache for this model, refresh this timestamp.
 *
 * The purpose of this value is for batch, because currently batch cannot connect to cache during running.
 * We do not know how many rows been modified, so expire all cache for this model after batch finished.
 *
 * 2. Collection timestamp
 *
 * We have many many list result in cache, once a row been modified, we do not know how many list contains this row.
 * If the list cache still works, some list may contains the old data, so user cannot see the changes immediately.
 *
 * In order to avoid this delay, once any data been modified, expire all collection cache immediately.
 * This will cause cache inefficient. If you want to disable it, use extends Dao[...](..., cacheAutoRefresh = false)
 * when define your Dao class.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object ModelCache {
  /**
   * Default collection data expired definition in application.conf
   */
  private val COLLECTION_EXPIRED_PERIOD: Int = AppConfig.getInt("memcached.collection.expired", Some(600))

  /**
   * Is memcached enabled.
   */
  val isEnabled = Cache.IS_MEMCACHED_ENABLED

  /**
   * Put model data into cache.
   *
   * @param key Cache key.
   * @param value Cache value.
   * @param isCacheAutoRefresh If cache auto refresh, we can put data into cache for a longer time.
   *                           Otherwise, in order to let user see latest data as soon as possible, use shorter expire time.
   */
  def set(key: String, value: Any, isCacheAutoRefresh: Boolean = false) =
    if (isCacheAutoRefresh) Cache.set(key, value) else Cache.set(key, value, COLLECTION_EXPIRED_PERIOD)

  /**
   * Retrieve model data from cache.
   *
   * @param key Cache key.
   * @return Option[Any] Cache value.
   */
  def get(key: String): Option[Any] = Cache.get(key)

  /**
   * Delete model data from cache.
   *
   * @param key Cache key.
   */
  def remove(key: String) = Cache.remove(key)

  /**
   * Delete model data from cache.
   *
   * @param invokeClass Invoker class.
   * @param id Cached model id.
   */
  def remove(invokeClass: Class[_], id: Any) = Cache.remove(this.getKey(invokeClass, id))

  /**
   * Generate cache key for single model by id.
   *
   * @param invokeClass Invoker class.
   * @param id Cached model id.
   * @return String Cache key.
   */
  def getKey(invokeClass: Class[_], id: Any): String = {
    val idValue = if (id.isInstanceOf[Some[_]]) id.asInstanceOf[Some[_]].get else id
    invokeClass.getSimpleName + ":" + idValue.toString + ":" + this.getGlobalTimestamp(invokeClass)
  }

  /**
   * Generate cache key for customized query functions.
   *
   * @param invokeClass Invoker class.
   * @param invokeMethod Invoker method.
   * @param params Parameters to identify an unique results.
   * @return String Cache key.
   */
  def getKey(invokeClass: Class[_], invokeMethod: String, params: Seq[(Symbol, Any)]): String = {
    val timestamp = this.getTimestamp(invokeClass)
    invokeClass.getSimpleName + ":" + invokeMethod + ":" + getParamsString(params) + ":" + timestamp._1 + ":" + timestamp._2
  }

  /**
   * Get global timestamp for target model.
   *
   * @param invokeClass Target model Dao class.
   * @return Long Model global timestamp.
   */
  def getGlobalTimestamp(invokeClass: Class[_]): Long = {
    this.getTimestamp(invokeClass)._1
  }

  /**
   * Get collection timestamp for target model.
   *
   * @param invokeClass Target model Dao class.
   * @return Long Model collection timestamp.
   */
  def getCollectionTimestamp(invokeClass: Class[_]): Long = {
    this.getTimestamp(invokeClass)._2
  }

  /**
   * Refresh global timestamp, expire all cache for target model.
   *
   * @param invokeClass Target model Dao class.
   */
  def refreshGlobalTimestamp(invokeClass: Class[_]) = {
    this.synchronized {
      val timestamp = System.currentTimeMillis
      this.setTimestamp(invokeClass, (timestamp, this.getCollectionTimestamp(invokeClass)))
    }
  }

  /**
   * Refresh collection timestamp, expire all cache of collection data for target model.
   *
   * @param invokeClass Target model Dao class.
   */
  def refreshCollectionTimestamp(invokeClass: Class[_]) = {
    this.synchronized {
      val timestamp = System.currentTimeMillis
      this.setTimestamp(invokeClass, (this.getGlobalTimestamp(invokeClass), timestamp))
    }
  }

  /**
   * Get both global and collection timestamp.
   *
   * @param invokeClass Target model Dao class.
   * @return (Long, Long) (global timestamp, collection timestamp)
   */
  private def getTimestamp(invokeClass: Class[_]): (Long, Long) = {
    Cache.get(invokeClass.getSimpleName) match {
      case Some(data) => data.asInstanceOf[(Long, Long)]
      case _ => this.setTimestamp(invokeClass)
    }
  }

  /**
   * Initial global and collection timestamp.
   *
   * @param invokeClass Target model Dao class.
   * @return (Long, Long) (global timestamp, collection timestamp)
   */
  private def setTimestamp(invokeClass: Class[_]): (Long, Long) = {
    val timestamp = System.currentTimeMillis
    Cache.set(invokeClass.getSimpleName, (timestamp, timestamp), Cache.DEFAULT_EXPIRED_PERIOD)
    (timestamp, timestamp)
  }

  /**
   * Reset global and collection timestamp.
   *
   * @param invokeClass Target model Dao class.
   * @param timestamp (global timestamp, collection timestamp)
   */
  private def setTimestamp(invokeClass: Class[_], timestamp: (Long, Long)) = {
    Cache.set(invokeClass.getSimpleName, timestamp, Cache.DEFAULT_EXPIRED_PERIOD)
  }

  /**
   * Generate parameters string value, all blank space will be removed.
   *
   * @param params Parameters to identify an unique results.
   * @return String String value of parameters.
   */
  private def getParamsString(params: Seq[(Symbol, Any)]): String = params match {
    case Nil => ""
    case _ =>
      val builder = new StringBuilder

      params.foreach(entry => {
        if (entry._2 != null) {
          val value = if (entry._2.isInstanceOf[Option[_]]) entry._2.asInstanceOf[Option[_]].getOrElse("") else entry._2

          // generate cache key for all parameters
          builder.append(entry._1.name).append("=").append({
            value match {
              case v: Date => v.getTime.toString
              case v: Int => v
              case v: Long => v
              case v: Float => v
              case v: Double => v
              case _ => value.hashCode
            }
          })

          builder.append(",")
        }
      })

      builder.toString.dropRight(1).replaceAll(" ", "")
  }

}