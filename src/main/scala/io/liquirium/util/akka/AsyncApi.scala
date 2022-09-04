package io.liquirium.util.akka

import scala.concurrent.Future

trait AsyncApi[T <: AsyncRequest[_]] {

  def sendRequest[R](request: AsyncRequest[R] with T): Future[R]

}
