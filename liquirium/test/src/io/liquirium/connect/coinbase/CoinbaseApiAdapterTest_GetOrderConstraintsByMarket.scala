package io.liquirium.connect.coinbase

import io.liquirium.connect.coinbase.helpers.CoinbaseTestHelpers.productInfo
import io.liquirium.core.helpers.MarketHelpers.{market, orderConstraints}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class CoinbaseApiAdapterTest_GetOrderConstraintsByMarket extends CoinbaseApiAdapterTest {

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getOrderConstraints() = apiAdapter.getOrderConstraintsByMarket()

  private def replyWith(productInfos: Seq[CoinbaseProductInfo]): Unit = fakeRestApi.completeNext(productInfos)

  test("the returned product infos are converted and returned as market infos") {
    val productInfos = Seq(productInfo(1), productInfo(2))
    fakeProductInfoConversion(productInfo(1), orderConstraints(1))
    fakeProductInfoConversion(productInfo(2), orderConstraints(2))
    fakeSymbolToMarketConversion(productInfo(1).symbol, market(1))
    fakeSymbolToMarketConversion(productInfo(2).symbol, market(2))

    val f = getOrderConstraints()
    replyWith(productInfos)
    f.value.get.get shouldEqual Map(
      market(1) -> orderConstraints(1),
      market(2) -> orderConstraints(2),
    )
  }

}
