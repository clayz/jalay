import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = {
    organization := "jp.ameba.meister"
    version := "1.0"
  }
}