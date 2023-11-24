package io.liquirium.bot.helpers

import io.liquirium.bot.BotInput
import io.liquirium.bot.BotInput.{CandleHistoryInput, TimeInput}
import io.liquirium.core.helpers.CoreHelpers.{sec, secs}
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.eval.InputEval

import java.time.Duration

object BotInputHelpers {

  case class TestBotInput[M](id: Int) extends BotInput[M]

  def botInput(n: Int): TestBotInput[Int] = TestBotInput[Int](n)

  def timeInput(d: Duration): TimeInput = TimeInput(d)

  def intInput(n: Int): BotInput[Int] = TestBotInput[Int](2 * n)

  def intInputMetric(n: Int): InputEval[Int] = InputEval(intInput(n))

  def stringInput(n: Int): TestBotInput[String] = TestBotInput[String](3 * n)

  def intConfigInput(s: String): BotInput.ConfigValue[Int] = BotInput.ConfigValue[Int](s)

  def stringInputMetric(n: Int): InputEval[String] = InputEval(stringInput(n))

  def candleHistoryInput(n: Int): CandleHistoryInput =
    CandleHistoryInput(market(n), candleLength = secs(n), start = sec(n))

  def configMetric[M](name: String): InputEval[M] = InputEval(BotInput.ConfigValue[M](name))

}
