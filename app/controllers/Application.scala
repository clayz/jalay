package controllers

import play.api.mvc._
import core.cache.ModelCache
import core.common.Log

/**
 * Default controller for application.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Application extends Controller {
  def index = Action {
    Ok(views.html.main())
  }

  /**
   * Clear cache for target model.
   */
  def clearCache(model: String) = Action {
    Log.info("Clear cache for model: " + model)
    ModelCache.refreshGlobalTimestamp(Class.forName(model + "Dao$"))

    Ok("Cache cleared for model: " + model)
  }
}