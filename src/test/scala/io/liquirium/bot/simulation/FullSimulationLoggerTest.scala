package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.Value
import io.liquirium.eval.helpers.ContextHelpers.context

class FullSimulationLoggerTest extends BasicTest {

  test("after logging several times the contexts can be accessed in the logging order") {
    FullSimulationLogger().log(context(1))._1.get.log(context(2))._1.get.allUpdates shouldEqual
      List(context(1), context(2))
  }

  test("the returned context is the one that was given and input requests are empty") {
    FullSimulationLogger().log(context(1))._2 shouldEqual context(1)
    FullSimulationLogger().log(context(1))._1 shouldBe a[Value[_]]
  }

}
