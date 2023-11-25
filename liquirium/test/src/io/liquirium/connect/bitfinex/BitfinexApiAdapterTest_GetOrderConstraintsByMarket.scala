package io.liquirium.connect.bitfinex

import io.liquirium.connect.bitfinex.helpers.BitfinexTestHelpers.pairInfo
import io.liquirium.core.helpers.MarketHelpers.{market, orderConstraints}

class BitfinexApiAdapterTest_GetOrderConstraintsByMarket extends BitfinexApiAdapterTest {

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getOrderConstraints() = apiAdapter.getOrderConstraintsByMarket()

  private def replyWith(pairInfos: Seq[BitfinexPairInfo]): Unit = fakeRestApi.completeNext(pairInfos)

  test("the returned pair infos are converted and returned as market infos") {
    val pairInfos = Seq(pairInfo(1), pairInfo(2))
    fakePairInfoConversion(pairInfo(1), orderConstraints(1))
    fakePairInfoConversion(pairInfo(2), orderConstraints(2))
    fakePairToMarketConversion(pairInfo(1).pair, market(1))
    fakePairToMarketConversion(pairInfo(2).pair, market(2))

    val f = getOrderConstraints()
    replyWith(pairInfos)
    f.value.get.get shouldEqual Map(
      market(1) -> orderConstraints(1),
      market(2) -> orderConstraints(2),
    )
  }

}
