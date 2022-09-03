package io.liquirium.bot.simulation

import io.liquirium.bot.simulation
import io.liquirium.core._
import io.liquirium.core.helpers.BasicTest
import io.liquirium.core.helpers.CandleHelpers.candle
import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.MarketHelpers.market
import io.liquirium.core.helpers.OrderHelpers._
import io.liquirium.core.helpers.TradeHelpers.tradeId

class ScaledCandleSimulatorTest_BasicMatching extends BasicTest {

  implicit class TupleConversion(tuple: (Seq[Trade], OrderSet, ScaledCandleSimulator)) {
    def trades: Seq[Trade] = tuple._1

    def orders: OrderSet = tuple._2

    def simulator: ScaledCandleSimulator = tuple._3
  }

  def dec(d: Double): BigDecimal = BigDecimal(d)

  def fillOrders(
    volumeReduction: Double = 1,
    tradeIds: Seq[TradeId] = null,
  ) (
    c: Candle,
    orders: Order*,
  ): (Seq[Trade], OrderSet, ScaledCandleSimulator) =
    simulation.ScaledCandleSimulator(
      feeLevel = ZeroFeeLevel,
      volumeReduction = volumeReduction,
      tradeIds = if (tradeIds == null) Stream.from(1).map(x => tradeId(x.toString)) else tradeIds.toStream
    )
      .fillOrders(OrderSet(orders.toSet), c)

  test("buy orders below the lowest rate are not matched") {
    fillOrders()(candle(low = 2, high = 3), exactBuy(1, at = 1)).trades should equal(Seq())
  }

  test("sell orders above the candle high are not matched") {
    fillOrders()(candle(low = 1, high = 2), exactSell(1, at = 3)).trades should equal(Seq())
  }

  test("buy or sell orders equal to the boundaries are not matched") {
    fillOrders()(candle(low = 1, high = 2), exactSell(1, at = 2), exactBuy(1, at = 1)).trades should equal(Seq())
  }

  test("trades are generated for buy orders above the candle low") {
    fillOrders()(candle(low = dec(1), high = dec(10), quoteVolume = 100),
      exactBuy(dec(1), at = dec(11), id = "o42"),
      exactBuy(dec(1), at = dec(2), id = "o43")
    )
      .trades.map(_.orderId.get) should contain theSameElementsAs Seq("o42", "o43")
  }

  test("trades are generated for sell orders below the candle high") {
    fillOrders()(candle(low = dec(2), high = dec(10), quoteVolume = 100),
      exactSell(dec(1), at = dec(1), id = "o42"),
      exactSell(dec(1), at = dec(5), id = "o43")
    )
      .trades.map(_.orderId.get) should contain theSameElementsAs Seq("o42", "o43")
  }

  test("buy orders within the candle range are only matched up to the reduced volume") {
    fillOrders(volumeReduction = 0.1)(candle(low = 0.5, high = 3.0, quoteVolume = 100),
      exactBuy(dec(2.5), at = dec(2.0)), exactBuy(dec(10), at = dec(1.0)))
      .trades.map { t => (t.quantity, t.price) } shouldEqual Seq((dec(2.5), dec(2.0)), (dec(5), dec(1)))
  }

  test("sell orders within the candle range are only matched up to the reduced volume") {
    fillOrders(volumeReduction = 0.1)(candle(low = 0.5, high = 3, quoteVolume = 100),
      exactSell(dec(5), at = dec(1)), exactSell(dec(5), at = dec(2)))
      .trades.map { t => (t.quantity, t.price) } shouldEqual Seq((dec(-5), dec(1)), (dec(-2.5), dec(2)))
  }

  test("for buy orders above the candle high the volume reduction does not apply") {
    fillOrders(volumeReduction = 0.5)(candle(price = dec(2), volume = dec(3)), exactBuy(dec(1), at = dec(3)))
      .trades.head.quantity shouldEqual dec(1)
  }

  test("buy orders above the candle high are matched at the order price") {
    val c = candle(price = dec(1), volume = dec(100)).copy(high = dec(2))
    val tt = fillOrders()(c, exactBuy(dec(2), at = dec(6))).trades
    tt.head.price shouldEqual dec(6)
    tt.head.quantity shouldEqual dec(2)
  }

  test("for sell orders below the candle low the volume reduction does not apply") {
    fillOrders(volumeReduction = 0.5)(candle(price = dec(2), volume = dec(1)), exactSell(dec(1), at = dec(1)))
      .trades.head.volume shouldEqual dec(1)
  }

  test("sell orders below the candle low are matched at the order price") {
    val c = candle(price = dec(4), volume = dec(100)).copy(low = dec(1))
    val tt = fillOrders()(c, exactSell(dec(2), at = dec(0.25))).trades
    tt.head.price shouldEqual dec(0.25)
    tt.head.quantity shouldEqual dec(-2)
  }

  test("the available rest volumes for orders within the candle range is reduced after exceeding orders are filled") {
    fillOrders(volumeReduction = 0.1)(candle(low = dec(2), high = dec(3), quoteVolume = dec(10)),
      exactSell(dec(2), at = dec(1)),
      exactBuy(dec(2), at = dec(2.5))
    ) .trades.filter(_.isBuy)(0).volume shouldEqual dec(0.8)
  }

  test("the trade price, market and orderId are taken from the order") {
    val tt = fillOrders()(candle(price = dec(2), volume = dec(3)),
      exactBuy(dec(1), at = dec(3), id = "bo123", market = market("b")),
      exactSell(dec(1), at = dec(1), id = "so123", market = market("s"))
    ).trades
    val b = tt.filter(_.isBuy)(0)
    b.price shouldEqual dec(3)
    b.orderId shouldEqual Some("bo123")
    b.market shouldEqual market("b")
    val s = tt.filter(_.isSell)(0)
    s.price shouldEqual dec(1)
    s.orderId shouldEqual Some("so123")
    s.market shouldEqual market("s")
  }

  test("the trade time is the candle time") {
    val tt = fillOrders()(candle(high = dec(2), low = dec(2), quoteVolume = dec(3), start = sec(123)),
      exactBuy(dec(1), at = dec(3)), exactSell(dec(1), at = dec(1))
    ).trades
    tt.filter(_.isBuy)(0).time shouldEqual sec(123)
    tt.filter(_.isBuy)(0).time shouldEqual sec(123)
  }

  test("the available volume is distributed among buys and sells with respect to matching order volumes") {
    val tt = fillOrders(volumeReduction = 0.1)(
      candle(high = dec(3), low = dec(0.5), quoteVolume = dec(150)),
      exactBuy(dec(10), at = dec(1)),
      exactSell(dec(10), at = dec(2))
    ).trades
    tt.filter(_.isBuy)(0).volume shouldEqual dec(5)
    tt.filter(_.isSell)(0).volume shouldEqual dec(10)
  }

  test("it uses the given trade ids") {
    fillOrders(tradeIds = List(tradeId("t1")))(candle(price = dec(0.5), volume = dec(100)), exactBuy(dec(5), at = dec(1)))
      .trades.head.id should equal(tradeId("t1"))
    fillOrders(tradeIds = List(tradeId("t2"), tradeId("t3")))(candle(price = dec(0.5), volume = dec(100)),
      exactBuy(dec(5), at = dec(1)),
      exactSell(dec(5), at = dec(0.25))
    ).trades.map(_.id) shouldEqual Seq(tradeId("t2"), tradeId("t3"))
  }

  test("the id stream of the returned marketplace is updated by dropping the used ids") {
    val fullList = List(tradeId("A"),tradeId("B"), tradeId("C"))
    fillOrders(tradeIds = fullList.toStream)(candle(price = dec(0.5), volume = dec(100)),
      exactBuy(dec(5), at = dec(1)),
      exactSell(dec(5), at = dec(0.25))
    ).simulator.tradeIds shouldEqual List(tradeId("C")).toStream
  }

  test("the returned orders are the given orders reduced by the trades") {
    val newOrders = fillOrders()(
      candle(high = dec(3), low = dec(0.5), quoteVolume = dec(150)),
      exactBuy(dec(10), at = dec(0.1)),
      exactBuy(dec(10), at = dec(1)),
      exactSell(dec(10), at = dec(2)),
      exactSell(dec(10), at = dec(4)),
    ).orders
    newOrders shouldEqual orders(
      exactBuy(dec(10), at = dec(0.1)),
      exactSell(dec(10), at = dec(4)),
    )
  }

}