package core.common

import com.typesafe.config._
import play.api.Play

/**
 * Application environment enumeration object, each environment has different application conf properties.
 * There are four application environment currently support: Local, Development, Staging, Production.
 *
 * In order to enable this configuration, an extra system environment has to be added.
 * {appName}_ENV = [ dev | staging | production ]
 *
 * If there is no such system environment been found, the application environment will be Local as default,
 * and the default application.conf file will be used as configurations.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AppEnv extends Enumeration {
  /**
   * Supported application environments, each environment has corresponding config file.
   */
  val Local, Development, Staging, Production = Value

  /**
   * System environment argument name for the target environment of current application.
   */
  val SYSTEM_ENV = "JALAY_ENV"

  /**
   * Get current application environment according to system environment argument.
   * If there is no such system environment argument been defined, will use local environment.
   *
   * @return AppEnv.Value Current running environment type.
   */
  def getCurrentEnv: AppEnv.Value = {
    if (System.getenv.containsKey(SYSTEM_ENV))
      System.getenv(SYSTEM_ENV) match {
        case "dev" | "DEV" | "Dev" => Development
        case "staging" | "STAGING" | "Staging" => Staging
        case "production" | "PRODUCTION" | "Production" => Production
      }
    else Local
  }

  /**
   * Get application conf file according to current running environment.
   *
   * @return String Application configuration file name.
   */
  def getConfigFile: String = {
    getCurrentEnv match {
      case Local => "application.conf"
      case Development => "application-dev.conf"
      case Staging => "application-stg.conf"
      case Production => "application-prd.conf"
    }
  }

  def isLocal: Boolean = Local.equals(getCurrentEnv)

  def isDev: Boolean = Development.equals(getCurrentEnv)

  def isStaging: Boolean = Staging.equals(getCurrentEnv)

  def isProduction: Boolean = Production.equals(getCurrentEnv)
}

/**
 * Application mode enumeration object, each mode has corresponding datasource configuration.
 * The purpose of this class is to divide database for web, admin and batch application.
 *
 * For example, there are three datasource configurations in application.conf, the first one is for web,
 * the second is for admin system, the last one is for batch.
 *
 * db.demo_w.url = "jdbc:mysql://localhost/demo?characterEncoding=UTF-8"
 * db.admin_demo_w.url = "jdbc:mysql://localhost/demo_admin?characterEncoding=UTF-8"
 * db.batch_demo_w.url = "jdbc:mysql://localhost/demo_batch?characterEncoding=UTF-8"
 *
 * After we deployed our application to target server, system will use the mode value to decide which datasource
 * to be used for current application.
 *
 * In order to enable this configuration, an extra system environment has to be added.
 * {appName}_MODE = [ web | admin | batch ]
 *
 * If there is no such system environment been found, the application mode will be web as default.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AppMode extends Enumeration {
  /**
   * Supported application mode types.
   */
  val Web, Admin, Batch = Value

  /**
   * System environment argument name for the target mode of current application.
   */
  val SYSTEM_MODE = "JALAY_MODE"

  /**
   * Get current application mode according to system environment argument.
   * If there is no such system environment argument been defined, will use web mode.
   *
   * @return AppMode.Value Current running mode type.
   */
  def getCurrentMode: AppMode.Value = {
    if (System.getenv.containsKey(SYSTEM_MODE))
      System.getenv(SYSTEM_MODE) match {
        case "web" => Web
        case "admin" => Admin
        case "batch" => Batch
      }
    else Web
  }

  def isWeb: Boolean = Web.equals(getCurrentMode)

  def isAdmin: Boolean = Admin.equals(getCurrentMode)

  def isBatch: Boolean = Batch.equals(getCurrentMode)
}

/**
 * System application configuration properties reader.
 *
 * It loads configuration file according to current application running mode,
 * all properties defined in that file can be get by this class.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AppConfig {
  /**
   * Application name.
   */
  lazy val appName: String = this.get("application.name")

  /**
   * Load application configuration according to current mode if play server is not start.
   */
  lazy val conf: Option[Config] = if (!isPlayStart) Some(ConfigFactory.load(AppEnv.getConfigFile)) else None

  /**
   * Check whether play application started.
   */
  val isPlayStart = try {
    Play.current.configuration
    true
  } catch {
    case e: Exception => false
  }

  /**
   * Get string value from configuration properties file.
   *
   * @param name Property name.
   * @param default Default value if property does not defined.
   * @return String Property value.
   * @throws RuntimeException Read property failed.
   */
  def get(name: String, default: Option[String] = None): String = {
    if (isPlayStart) {
      if (default.isDefined) Play.current.configuration.getString(name).getOrElse(default.get)
      else Play.current.configuration.getString(name).get
    } else if (conf.isDefined) {
      if (default.isDefined)
        try {
          conf.get.getString(name)
        } catch {
          case e: ConfigException => default.get
        }
      else conf.get.getString(name)
    } else {
      throw new RuntimeException("Cannot read system property, please check your system environment and application conf file.")
    }
  }

  /**
   * Get string value from configuration properties file as an Option object.
   *
   * @param name Property name.
   * @return Option[String] Property value.
   */
  def getOption(name: String): Option[String] = {
    if (isPlayStart) {
      Play.current.configuration.getString(name)
    } else if (conf.isDefined) {
      try {
        Some(conf.get.getString(name))
      } catch {
        case e: ConfigException => None
      }
    } else None
  }

  /**
   * Get digital value from configuration properties file.
   *
   * @param name Property name.
   * @param default Default value if property does not defined.
   * @return Int Property value.
   * @throws RuntimeException Read property failed.
   */
  def getInt(name: String, default: Option[Int] = None): Int = {
    if (default.isDefined) this.get(name, Some(default.get.toString)).toInt else this.get(name).toInt
  }

  /**
   * Get boolean value from configuration properties file.
   *
   * @param name Property name.
   * @param default Default value if property does not defined.
   * @return Boolean Property value.
   * @throws RuntimeException Read property failed.
   */
  def getBoolean(name: String, default: Option[Boolean] = None): Boolean = {
    if (default.isDefined) this.get(name, Some(default.get.toString)).toBoolean else this.get(name).toBoolean
  }
}