package io.liquirium.core.helpers.async

import io.liquirium.core.helpers.BasicTest
import io.liquirium.util.async.helpers.FakeScheduler

import scala.concurrent.ExecutionContext

class AsyncTestWithScheduler extends BasicTest {

  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor((runnable: Runnable) => runnable.run())

  protected val scheduler = new FakeScheduler()

}
