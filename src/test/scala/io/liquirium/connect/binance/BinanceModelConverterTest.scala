package io.liquirium.connect.binance

import io.liquirium.core.ExchangeId
import io.liquirium.core.helper.BasicTest

class BinanceModelConverterTest extends BasicTest {

  def converter(exchangeId: ExchangeId = ExchangeId("1")): BinanceModelConverter = new BinanceModelConverter(exchangeId)

}
