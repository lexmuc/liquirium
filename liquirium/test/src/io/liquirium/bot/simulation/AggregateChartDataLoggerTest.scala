package io.liquirium.bot.simulation

import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.eval.{UpdatableContext, Value}
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class AggregateChartDataLoggerTest extends TestWithMocks {

  test("it is created from a map of market loggers and and updated map can be accessed after logging") {
    val subLogger1A = mock(classOf[ChartDataLogger])
    val subLogger1B = mock(classOf[ChartDataLogger])
    val subLogger2A = mock(classOf[ChartDataLogger])
    val subLogger2B = mock(classOf[ChartDataLogger])
    val logger = AggregateChartDataLogger(Seq(
      market(1) -> subLogger1A,
      market(2) -> subLogger2A,
    ))
    val context0 = mock(classOf[UpdatableContext])
    val context1 = mock(classOf[UpdatableContext])
    val context2 = mock(classOf[UpdatableContext])
    subLogger1A.log(context0) returns (Value(subLogger1B), context1)
    subLogger2A.log(context1) returns (Value(subLogger2B), context2)
    val (evalResult, finalContext) = logger.log(context0)
    evalResult.get.marketsWithMarketLoggers shouldEqual Seq(
      market(1) -> subLogger1B,
      market(2) -> subLogger2B,
    )
    finalContext shouldEqual context2
  }

}
