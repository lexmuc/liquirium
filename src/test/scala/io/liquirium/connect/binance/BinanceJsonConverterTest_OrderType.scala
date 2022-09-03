package io.liquirium.connect.binance

import io.liquirium.core.helpers.BasicTest

class BinanceJsonConverterTest_OrderType extends BasicTest {

  def convert(bot: BinanceOrderType): String = new BinanceJsonConverter().convertOrderType(bot)

  test("all order types are properly converted to strings") {
    convert(BinanceOrderType.LIMIT) shouldEqual "LIMIT"
    convert(BinanceOrderType.MARKET) shouldEqual "MARKET"
    convert(BinanceOrderType.STOP_LOSS) shouldEqual "STOP_LOSS"
    convert(BinanceOrderType.STOP_LOSS_LIMIT) shouldEqual "STOP_LOSS_LIMIT"
    convert(BinanceOrderType.TAKE_PROFIT) shouldEqual "TAKE_PROFIT"
    convert(BinanceOrderType.TAKE_PROFIT_LIMIT) shouldEqual "TAKE_PROFIT_LIMIT"
    convert(BinanceOrderType.LIMIT_MAKER) shouldEqual "LIMIT_MAKER"
  }

}
