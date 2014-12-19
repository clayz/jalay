package core.cache

import play.api.Play.current
import play.api.cache.{Cache => PlayCache}
import core.common.AppConfig
import core.common.Log
import core.common.CacheException

/**
 * Cache management class. It contains all operations for Memcached interaction.
 * If Memcached is enabled, use it to get and set objects for cache purpose.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Cache {
  /**
   * Memcached usable definition in application.conf
   */
  val IS_MEMCACHED_ENABLED: Boolean = "enabled".equalsIgnoreCase(AppConfig.get("memcachedplugin", Some("disabled")))

  /**
   * Default expired definition in application.conf
   */
  val DEFAULT_EXPIRED_PERIOD: Int = AppConfig.getInt("memcached.default.expired", Some(600))

  /**
   * Whether batch log been enabled in application.conf
   */
  val IS_LOG_ENABLED: Boolean = "enabled".equalsIgnoreCase(AppConfig.get("memcached.log", Some("disabled")))

  /**
   * Put data into cache.
   *
   * @param key Cache key.
   * @param value Cache value.
   * @param expired Expired period with seconds.
   * @throws CacheException If cache is disabled.
   */
  def set(key: String, value: Any, expired: Int = DEFAULT_EXPIRED_PERIOD) =
    if (IS_MEMCACHED_ENABLED) {
      if (IS_LOG_ENABLED) Log.debug("[Cache] SET, key: " + key + "\nvalue: " + value.toString)
      PlayCache.set(key, value, expired)
    } else throw new CacheException("Memcached is disabled now, enable it in application conf before use.")

  /**
   * Retrieve data from cache.
   *
   * @param key Cache key.
   * @return Option[Any] Cache value.
   * @throws CacheException If cache is disabled.
   */
  def get(key: String): Option[Any] =
    if (IS_MEMCACHED_ENABLED) {
      PlayCache.get(key) match {
        case Some(value) =>
          if (IS_LOG_ENABLED) Log.debug("[Cache] GET, key: " + key + "\nvalue: " + value.toString)
          Some(value)
        case _ => None
      }
    } else throw new CacheException("Memcached is disabled now, enable it in application conf before use.")

  /**
   * Delete data from cache.
   *
   * @param key Cache key.
   * @throws CacheException If cache is disabled.
   */
  def remove(key: String) =
    if (IS_MEMCACHED_ENABLED) {
      if (IS_LOG_ENABLED) Log.debug("[Cache] DELETE, key: " + key)
      PlayCache.remove(key)
    } else throw new CacheException("Memcached is disabled now, enable it in application conf before use.")
}