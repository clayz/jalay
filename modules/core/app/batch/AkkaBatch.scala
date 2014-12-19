package core.batch

import akka.actor._
import com.typesafe.config.ConfigFactory
import org.clapper.argot.ArgotConverters._

import core.common._

/**
 * Multiple threads batch process entrance.
 * This abstract class provides Akka support during batch execution.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class AkkaBatch extends Batch {
  /**
   * Command-line arguments and values.
   */
  val batchSizeArg = parser.option[Int](List("bs", "batchsize"), "0", "Batch data size to be executed within one Akka thread.")
  lazy val batchSize = batchSizeArg.value.getOrElse(1000)

  /**
   * Akka concurrent and asynchronous system and configurations.
   */
  protected var system: Option[ActorSystem] = None
  protected var segments: List[BatchSegment] = List()
  protected val actorFactor = 100
  protected val actorMax = 1000

  /**
   * Create new Akka actor for async process.
   * Support max actor amount: 100, this value can be changed by modify actorFactor and actorMax.
   *
   * @param params Parameters map for each processActor function.
   */
  def createActor(params: Map[Symbol, Any]) = {
    this.segments ::= BatchSegment(this.processActor, params)
  }

  /**
   * Start all Akka actor for async process.
   */
  def startActors {
    Log.info(s"Start all Akka actors, size: ${this.segments.size}")

    if (this.segments.size > this.actorFactor) {
      Log.warn("Current segment size is larger than actor factor, check the CPU amount and make sure all actors can be executed.")
    }

    this.segments.reverse.foreach {
      segment =>
        // create and execute actor with segment parameters
        system.get.actorOf(Props[BatchActor]) ! segment
        // if start all actors at same time, some actors may not be started as expected
        Thread.sleep(1000)
    }
  }

  /**
   * Execution logic for each actor.
   */
  def processActor(params: Map[Symbol, Any])

  /**
   * Execute a block of code within Akka actor system.
   * It will start Akka system before code execute and shutdown Akka system after code execute finished.
   *
   * @param block Block of code to be executed.
   */
  override def processHook(block: => Unit) {
    try {
      if (this.system.isEmpty) {
        // create the actor system with customized configuration
        val akkaConfig = ConfigFactory.parseString( s"""
          akka.actor {
            default-dispatcher {
              fork-join-executor {
                parallelism-factor = $actorFactor
                parallelism-max = $actorMax
              }
            }
          }""")

        Log.debug("Start Akka actor system.")
        system = Some(ActorSystem(this.getClass.getSimpleName.dropRight(1), ConfigFactory.load(akkaConfig)))
      }

      block
    } finally {
      // shutdown Akka system
      this.system match {
        case Some(akkaSystem) =>
          Log.debug("Shutdown Akka actor system.")
          akkaSystem.shutdown
          akkaSystem.awaitTermination
        case _ => Log.warn("Akka system is not started.")
      }
    }
  }
}

/**
 * Segment which holding all parameters for actor.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class BatchSegment(block: Map[Symbol, Any] => Unit, params: Map[Symbol, Any])

/**
 * Akka actor for handling each segment.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
class BatchActor extends Actor {
  /**
   * Execute actor logic after received message from supervisor.
   */
  def receive = {
    case BatchSegment(block, params) =>
      try {
        Log.info(s"Start new Akka actor, params: ${params.toString}")
        block(params)
      } catch {
        case e: Throwable => Log.error(s"Exception happened while process actor, params: ${params.toString}", e)
      }
    case _ => Log.error("Unknown receive type.")
  }
}
