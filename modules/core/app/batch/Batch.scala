package core.batch

import java.io.{File, IOException}

import org.clapper.argot.ArgotConverters._
import org.clapper.argot._
import org.slf4j.MDC

import core.common._
import core.db.SingleThreadDBPool

/**
 * System batch process entrance.
 * This abstract class should be extended by certain batch class for execution.
 *
 * How to run batch:
 * 1. Execute play dist command.
 * 2. Go to dist folder and unzip [systemName]-[version].zip file.
 * 3. Go into lib folder in [systemName]-[version], execute batch command.
 *
 * The batch command will looks like: scala -cp '*.jar' package.BatchClass [OPTIONS]
 *
 * This class used Argot to parse command-line arguments, more information can be found on:
 * http://software.clapper.org/argot
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class Batch(isCritical: Boolean = false) {
  /**
   * Command usage help info.
   */
  private val executeCommand = "scala -cp '*.jar' package.BatchClass"

  /**
   * Default batch lock location.
   */
  private val defaultLockPath = "/var/lock/jalay/"

  /**
   * If any of these batches are running, current batch will not be executed.
   */
  private val blocks = collection.mutable.ArrayBuffer.empty[Class[_]]

  /**
   * Command-line arguments parser.
   */
  protected val parser = new ArgotParser(executeCommand, preUsage = Some(AppConfig.appName + " batch version 1.0."))

  /**
   * Define all default options and parameters.
   */
  private val lockPath = parser.option[String](List("lp", "lockpath"), defaultLockPath, "Directory to save all batch lock files.")

  /**
   * As batch entry, this method will be invoked during batch execution.
   */
  def process

  /**
   * Add extra logic which can be executed before and after process.
   *
   * @param block Batch execution logic.
   */
  def processHook(block: => Unit) = block

  /**
   * Batch command execution static entrance.
   * This is the entrance for all batches, it will do following things:
   *
   * 1. Parse arguments from command line.
   * 2. Check and add lock for current batch.
   * 3. Enabled data source pool for DB operations.
   * 4. Execute customized batch logic.
   * 5. Unlock current batch and do other ending works.
   *
   * @param args Arguments from command-line.
   */
  def main(args: Array[String]) = {
    // set log file name
    MDC.put("fileName", this.getClass.getSimpleName)
    val start = System.currentTimeMillis

    Log.info("-----------------------------------------------------------------------")
    Log.info("-- Batch execution start")
    Log.info("-----------------------------------------------------------------------")

    try {
      // parse command line arguments
      parser.parse(args)

      // prepare batch execution environment
      SingleThreadDBPool.isEnabled = true

      // create lock and run batch
      lock
      processHook(process)
      unlock
    } catch {
      case e: ArgotUsageException => Log.error("Argot usage exception. ", e)
      case e: BatchException =>
        if (this.isCritical) Log.crit(s"Batch [${this.getClass.getSimpleName}] process failed: ${e.message}", e)
        else Log.error("Batch process failed.", e)
      case e: Throwable =>
        if (this.isCritical) Log.crit(s"Batch [${this.getClass.getSimpleName}] process failed: ${e.getMessage}", e)
        else Log.error("Batch process failed, unknown exception happened.", e)
    } finally {
      SingleThreadDBPool.isEnabled = false
      SingleThreadDBPool.releaseAllConnections

      Log.info("-----------------------------------------------------------------------")
      Log.info(s"-- Batch execution finish, take time: ${(System.currentTimeMillis - start) / 1000} seconds.")
      Log.info("-----------------------------------------------------------------------")
    }
  }

  /**
   * Add blocks classes for current batch.
   * If any one of the block classes is running, current batch cannot be executed.
   *
   * @param block Target block class.
   */
  protected def addBlocks(block: Class[_]*) = blocks ++= block

  /**
   * Check and create lock file for current batch.
   * It will find the lock file under lock path which is the same name as batch class.
   *
   * If lock file already exists, which means this batch is running now.
   * This is used to prevent same batch been executed twice at the same time.
   *
   * It also check block batch, if any of current batch's block batch is running now,
   * This batch cannot been executed till the block batch is finished.
   */
  private def lock {
    val batchName = this.getClass.getSimpleName
    Log.info("Check and lock batch: " + batchName)

    val path = lockPath.value.getOrElse(defaultLockPath)
    val folder = new File(path)

    // check lock file directory
    if (!folder.exists) {
      if (!folder.mkdir) throw new BatchException("Create lock file directory failed: " + folder.getPath)
    }

    // check dependents batches
    this.blocks.foreach(block => {
      if (new File("%s/%s.lock".format(path, block.getSimpleName)).exists)
        throw new BatchException("Batch cannot be executed because block batch %s is running.".format(block.getSimpleName))
    })

    // check and create lock file
    val file = new File("%s/%s.lock".format(path, batchName))

    if (file.exists) {
      throw new BatchException("This batch is currently running, please execute it later.")
    } else {
      try {
        if (!file.createNewFile) throw new BatchException("Create lock file failed: " + file.getPath)
      } catch {
        case e: IOException => throw new BatchException("Create lock file failed: " + file.getPath + ", caused by: " + e.getMessage)
      }
    }
  }

  /**
   * Release batch lock, remove lock file from lock path.
   */
  private def unlock {
    try {
      val batchName = this.getClass.getSimpleName
      val file = new File("%s/%s.lock".format(lockPath.value.getOrElse(defaultLockPath), batchName))

      if (file.exists) {
        Log.info("Release batch lock: " + batchName)
        if (!file.delete) throw new BatchException("Lock file delete failed, please remove it manually: " + file.getPath)
      }
    } catch {
      case e: Throwable => Log.error("Release batch lock file failed.", e)
    }
  }
}