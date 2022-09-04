package io.liquirium.helper

import scala.concurrent.ExecutionContext

object CallingThreadExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = throw cause
}
