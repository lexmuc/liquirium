package io.liquirium.helpers

import io.liquirium.util.akka.AsyncRequest

object AsyncTestApi {

  type AsyncTestRequest = TypedTestRequest[_]

  sealed trait TypedTestRequest[T] extends AsyncRequest[T]

  case class RequestA(p: String) extends TypedTestRequest[Int]

  case class RequestB(p: String) extends TypedTestRequest[Unit]

  def reqA(n: Int): RequestA = RequestA(n.toString)

  def reqB(n: Int): RequestB = RequestB(n.toString)

}
