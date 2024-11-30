package io.liquirium.connect.binance

import io.liquirium.connect.binance.helpers.BinanceTestHelpers.symbolInfo
import io.liquirium.connect.binance.{exchangeId => binanceExchangeId}
import io.liquirium.core.helpers.MarketHelpers.{market, orderConstraints}

class BinanceApiAdapterTest_GetOrderConstraintsByMarket extends BinanceApiAdapterTest {

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getOrderConstraints() = apiAdapter.getOrderConstraintsByMarket()

  private def replyWith(symbolInfos: Seq[BinanceSymbolInfo]): Unit = fakeRestApi.completeNext(symbolInfos)

  test("the returned symbol infos are converted and returned by the markets extracted from the infos") {
    val adaSymbolInfo = symbolInfo(symbol="A", baseAsset = "ADA", quoteAsset = "ETH")
    val manaSymbolInfo = symbolInfo(symbol="A", baseAsset = "MANA", quoteAsset = "ETH")
    val symbolInfos = Seq(adaSymbolInfo, manaSymbolInfo)
    fakeSymbolInfoConversion(adaSymbolInfo, orderConstraints(1))
    fakeSymbolInfoConversion(manaSymbolInfo, orderConstraints(2))

    val f = getOrderConstraints()
    replyWith(symbolInfos)
    f.value.get.get shouldEqual Map(
      market(binanceExchangeId, "ADA", "ETH") -> orderConstraints(1),
      market(binanceExchangeId, "MANA", "ETH") -> orderConstraints(2),
    )
  }

}
