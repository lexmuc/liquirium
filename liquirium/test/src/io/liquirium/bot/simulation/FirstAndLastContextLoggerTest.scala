package io.liquirium.bot.simulation

import io.liquirium.core.helpers.BasicTest
import io.liquirium.eval.{UpdatableContext, Value}
import io.liquirium.eval.helpers.ContextHelpers.context
import org.scalatest.matchers.should.Matchers.{a, an, convertToAnyShouldWrapper, thrownBy}

class FirstAndLastContextLoggerTest extends BasicTest {

  private def logger = FirstAndLastContextLogger()

  private def log(updates: UpdatableContext*) = updates.foldLeft(logger) { _.log(_)._1.get }

  test("the first and last logged contexts can be accessed") {
    val finalState = log(
      context(3),
      context(4),
      context(5),
    )
    finalState.firstContext shouldEqual context(3)
    finalState.lastContext shouldEqual context(5)
  }

  test("the final context can be accessed when there was only one update") {
    log(context(3)).lastContext shouldEqual context(3)
  }

  test("exceptions are thrown when first or last context are accessed but no update has taken place so far") {
    an[Exception] shouldBe thrownBy(logger.firstContext)
    an[Exception] shouldBe thrownBy(logger.lastContext)
  }

  test("the returned context is the one given and input requests are empty") {
    val result = logger.log(context(123))
    result._2 shouldEqual context(123)
    result._1 shouldBe a[Value[_]]
  }

}
