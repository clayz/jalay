package core.thread

import compat.Platform
import collection._
import core.common._

/**
 * This class provides concurrent compute and synchronized control functions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Concurrent {
  /**
   * Global concurrent control map for all customized operations.
   * It contains a TrieMap for each operation, this TrieMap holds operation locks for all users which userId as key.
   */
  private val operationsMap = new mutable.HashMap[Operations.Value, concurrent.TrieMap[Long, Object]]

  /**
   * Default wait time out for loose exclusive synchronize.
   */
  private val WAIT_TIME_OUT = 1000 * 3

  /**
   * Add all customized operations into global operations lock map.
   */
  Operations.values.foreach(option => {
    Log.info("Initialize synchronized set: " + option.toString)
    this.operationsMap += (option -> new concurrent.TrieMap[Long, Object])
  })

  /**
   * Execute block of code with synchronized support by using exclusive lock.
   * Each time only one thread with the same operation and userId can execute the block of code.
   *
   * If there already has one thread acquired this lock, other threads with same operation and userId
   * will not execute the block code, it will throw ConcurrentException or return the defaultReturnValue directly.
   *
   * @param operation Defined concurrent operation.
   * @param userId Current login user Id.
   * @param defaultReturnValue Default return value if other thread already holds lock.
   * @param block Block of synchronized code to be executed.
   * @return Return generic type value.
   * @throws Exception If lock exists and default return value is undefined.
   */
  def strictExclusiveSynchronized[A](operation: Operations.Value, userId: Long, defaultReturnValue: Option[A] = None)(block: => A): A = {
    val currentOperationMap = this.operationsMap(operation)
    Log.debug("[%d] Before strict exclusive operation: %s".format(userId, operation.toString))

    currentOperationMap.putIfAbsent(userId, new Object) match {
      case Some(_) =>
        // lock object already exists
        defaultReturnValue.getOrElse(throw new ConcurrentException("Concurrent operation blocked: %s".format(operation.toString)))
      case None =>
        // execute block of code then release lock
        try {
          block
        } finally {
          currentOperationMap -= userId
        }
    }
  }

  /**
   * Execute block of code with synchronized support by using exclusive lock.
   * Each time only one thread with same operation and userId can execute the block of code.
   *
   * If there already has one thread acquired this lock, other threads with same operation and userId
   * have to wait to be executed after previous operation finish.
   *
   * @param operation Defined concurrent operation.
   * @param userId Current login user Id.
   * @param block Block of synchronized code to be executed.
   * @return Return generic type value.
   * @throws Exception Wait for concurrent lock timeout.
   */
  def looseExclusiveSynchronized[A](operation: Operations.Value, userId: Long)(block: => A): A = {
    val currentOperationMap = this.operationsMap(operation)
    val timeout = Platform.currentTime + this.WAIT_TIME_OUT
    Log.debug("[%d] Before loose exclusive operation: %s".format(userId, operation.toString))

    while (Platform.currentTime < timeout) {
      if (currentOperationMap.putIfAbsent(userId, new Object).isEmpty) {
        // execute block of code then release lock
        try {
          return block
        } finally {
          currentOperationMap -= userId
        }
      }
    }

    throw new ConcurrentException("Concurrent operation timeout: %s".format(operation.toString))
  }
}