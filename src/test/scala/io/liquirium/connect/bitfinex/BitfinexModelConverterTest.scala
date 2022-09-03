package io.liquirium.connect.bitfinex

import io.liquirium.core.ExchangeId
import io.liquirium.core.helpers.BasicTest

class BitfinexModelConverterTest extends BasicTest {

  def converter(exchangeId: ExchangeId = ExchangeId("1")): BitfinexModelConverter = new BitfinexModelConverter(exchangeId)

}
