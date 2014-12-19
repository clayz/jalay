package core.cache

import core.utils.StringUtil

/**
 * Cache to keep all form tokens.
 * This is used for preventing duplicate form submission and CSRF attack.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object TokenCache {
  /**
   * Default expired time for one time token.
   */
  private val EXPIRED_PERIOD = 60 * 60

  /**
   * Default value for all anonymous tokens.
   */
  private val ANONYMOUS_TOKEN_VALUE = 0

  /**
   * Generate user binding token and put into cache with giving expired period.
   *
   * @param userId Current login user Id.
   * @return Token value.
   */
  def create(userId: Long): String = {
    val token = StringUtil.random(32)
    this.set(Some(userId), token, EXPIRED_PERIOD)
    token
  }

  /**
   * Generate user binding token and put into cache with giving expired period.
   *
   * @param userId Current login user Id.
   * @param expired Token expired period.
   * @return Token value.
   */
  def create(userId: Long, expired: Int): String = {
    val token = StringUtil.random(32)
    this.set(Some(userId), token, expired)
    token
  }

  /**
   * Generate anonymous token and put into cache with giving expired period.
   *
   * @return Token value.
   */
  def create: String = {
    val token = StringUtil.random(32)
    this.set(None, token, EXPIRED_PERIOD)
    token
  }

  /**
   * Generate anonymous token and put into cache with giving expired period.
   *
   * @param expired Token expired period.
   * @return Token value.
   */
  def create(expired: Int): String = {
    val token = StringUtil.random(32)
    this.set(None, token, expired)
    token
  }

  /**
   * Check whether token exists for this user, then remove it.
   *
   * @param userId Current login user Id.
   * @param token Token value.
   * @return Boolean Whether token exists.
   */
  def check(userId: Long, token: String): Boolean =
    Cache.get(this.getKey(token)) match {
      case Some(value) =>
        if (value == userId) {
          this.remove(token)
          true
        } else false
      case _ => false
    }

  /**
   * Check whether anonymous token exists, then remove it.
   *
   * @param token Token value.
   * @return Boolean Whether token exists.
   */
  def check(token: String): Boolean =
    Cache.get(this.getKey(token)) match {
      case Some(value) =>
        if (value == this.ANONYMOUS_TOKEN_VALUE) {
          this.remove(token)
          true
        } else false
      case _ => false
    }

  /**
   * Check whether user binding token exists in cache.
   *
   * @param token Token value.
   * @return Boolean Whether token exists.
   */
  def exists(userId: Long, token: String): Boolean =
    Cache.get(this.getKey(token)) match {
      case Some(value) => value == userId
      case _ => false
    }

  /**
   * Check whether token exists in cache.
   *
   * @param token Token value.
   * @return Boolean Whether token exists.
   */
  def exists(token: String): Boolean = Cache.get(this.getKey(token)).isDefined

  /**
   * Remove token from cache.
   *
   * @param token Token value.
   */
  def remove(token: String) = Cache.remove(this.getKey(token))

  /**
   * Put anonymous token and user binding token into cache.
   *
   * @param userId Current login user Id.
   * @param token One time token value.
   * @param expired One time token expired period.
   */
  private def set(userId: Option[Long], token: String, expired: Int = EXPIRED_PERIOD) = userId match {
    case Some(id) => Cache.set(this.getKey(token), id, expired)
    case _ => Cache.set(this.getKey(token), this.ANONYMOUS_TOKEN_VALUE, expired)
  }

  /**
   * Generate cache key for token.
   *
   * @param token Token value.
   * @return String Cache key.
   */
  private def getKey(token: String): String = "Token:" + token
}