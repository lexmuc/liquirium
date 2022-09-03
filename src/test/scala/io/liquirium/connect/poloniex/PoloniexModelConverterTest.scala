package io.liquirium.connect.poloniex

import io.liquirium.core.ExchangeId
import io.liquirium.core.helpers.BasicTest

class PoloniexModelConverterTest extends BasicTest {

  def converter(exchangeId: ExchangeId = ExchangeId("1")): PoloniexModelConverter = new PoloniexModelConverter(exchangeId)

}
