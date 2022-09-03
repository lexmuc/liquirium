package io.liquirium.eval

import scala.concurrent.Future

trait InputProvider {

  def apply[I](input: Input[I]): Option[Future[I]]

}
