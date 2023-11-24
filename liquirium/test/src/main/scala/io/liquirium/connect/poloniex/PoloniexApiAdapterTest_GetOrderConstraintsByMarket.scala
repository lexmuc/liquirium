package io.liquirium.connect.poloniex

import io.liquirium.connect.poloniex.helpers.PoloniexTestHelpers.symbolInfo
import io.liquirium.core.helpers.MarketHelpers.{market, orderConstraints}

class PoloniexApiAdapterTest_GetOrderConstraintsByMarket extends PoloniexApiAdapterTest {

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getOrderConstraints() = apiAdapter.getOrderConstraintsByMarket()

  private def replyWith(symbolInfos: Seq[PoloniexSymbolInfo]): Unit = fakeRestApi.completeNext(symbolInfos)

  test("the returned symbol infos are converted and returned as market infos") {
    val symbolInfos = Seq(symbolInfo(1), symbolInfo(2))
    fakeSymbolInfoConversion(symbolInfo(1), orderConstraints(1))
    fakeSymbolInfoConversion(symbolInfo(2), orderConstraints(2))
    fakeSymbolToMarketConversion(symbolInfo(1).symbol, market(1))
    fakeSymbolToMarketConversion(symbolInfo(2).symbol, market(2))

    val f = getOrderConstraints()
    replyWith(symbolInfos)
    f.value.get.get shouldEqual Map(
      market(1) -> orderConstraints(1),
      market(2) -> orderConstraints(2),
    )
  }

}
