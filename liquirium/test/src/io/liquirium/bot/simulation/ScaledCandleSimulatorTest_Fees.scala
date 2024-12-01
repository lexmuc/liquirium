package io.liquirium.bot.simulation

import io.liquirium.bot.simulation
import io.liquirium.core.Trade.Fees
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.dec
import io.liquirium.core.helpers.FeeHelper.fees
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.OrderHelpers.{exactBuy, exactSell}
import io.liquirium.core.helpers.TradeHelpers.tradeId
import io.liquirium.core._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class ScaledCandleSimulatorTest_Fees extends BasicTest {

  implicit class ResultConversion(t: (Seq[Trade], Set[Order], ScaledCandleSimulator)) {
    def firstTrade: Trade = t._1.head
  }

  case class FakeFeeLevel(market: Market, amount: BigDecimal, price: BigDecimal, fees: Fees) extends FeeLevel {
    override def apply(market: Market, amount: BigDecimal, price: BigDecimal): Fees =
      if (market == this.market && amount == this.amount && price == this.price) fees
      else fail(s"unexpected input for fee structure: $market amount $amount at price $price")
  }

  def fillOrders(fees: FeeLevel)(orders: Set[Order], candle: Candle) : (Seq[Trade], Set[Order], ScaledCandleSimulator) =
    simulation.ScaledCandleSimulator(
      feeLevel = fees,
      volumeReduction = 1.0,
      tradeIds = Stream.from(1).map(x => tradeId(x.toString))
    ).fillOrders(orders, candle)

  test("buy fees are based only on the filled fraction of an order") {
    val feeLevel = FakeFeeLevel(m(123), amount = dec("5"), price = dec("1"), fees(123))
    fillOrders(feeLevel)(Set(exactBuy(quantity = dec("10"), at = dec("1"), market = m(123))),
      candle(price = dec("0.3"), volume = dec("5")))
      .firstTrade.fees shouldEqual fees(123)
  }

  test("sell fees are based only on the filled fraction of an order") {
    val feeLevel = FakeFeeLevel(m(123), amount = dec("-5"), price = dec("1"), fees(123))
    fillOrders(feeLevel)(Set(exactSell(quantity = dec("10"), at = dec("1"), market = m(123))),
      candle(price = dec("2"), volume = dec("5")))
      .firstTrade.fees shouldEqual fees(123)
  }

}