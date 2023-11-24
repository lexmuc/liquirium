package io.liquirium.connect.coinbase

import io.liquirium.core.ExchangeId
import io.liquirium.core.helpers.BasicTest

class CoinbaseModelConverterTest extends BasicTest {

  def converter(exchangeId: ExchangeId = ExchangeId("1")): CoinbaseModelConverter =
    new CoinbaseModelConverter(exchangeId)

}
