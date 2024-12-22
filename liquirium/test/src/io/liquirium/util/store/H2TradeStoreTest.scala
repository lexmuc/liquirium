package io.liquirium.util.store

import io.liquirium.connect.TradeBatch
import io.liquirium.core.helpers.CoreHelpers.{dec, milli}
import io.liquirium.core.helpers.MarketHelpers.m
import io.liquirium.core.helpers.TradeHelpers.{trade => t}
import io.liquirium.core.helpers.async.AsyncTestWithScheduler
import io.liquirium.core.{ExchangeId, LedgerRef, Trade}
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper, thrownBy}

import java.sql.DriverManager
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration._

class H2TradeStoreTest extends AsyncTestWithScheduler {

  private val url = "jdbc:h2:mem:"
  private val connection = DriverManager.getConnection(url)
  private val market = m(123)

  private def store = new H2TradeStore(connection, market)

  private def add(trades: Trade*): Unit = {
    Await.result(store.add(trades), 3.seconds)
  }

  private def retrieve(start: Option[Instant] = None, end: Option[Instant] = None) = {
    Await.result(store.get(start, end), 3.seconds).trades
  }

  private def retrieveBatch(from: Option[Instant] = None, until: Option[Instant] = None) = {
    Await.result(store.get(from, until), 3.seconds)
  }

  private def deleteFrom(time: Instant): Unit = store.deleteFrom(time)

  private def assertCount(table: String, count: Int) = {
    val rs = connection.createStatement().executeQuery(s"SELECT COUNT(*) FROM $table")
    rs.next()
    rs.getInt(1) shouldEqual count
  }

  private def tradeWithTime(n: Int, id: String = null) = {
    t(id = if (id == null) n.toString else id, time = milli(n), market = market)
  }

  test("trades are stored in a table 'TRADES'") {
    add(t(1), t(2))
    assertCount("TRADES", 2)
  }

  test("it immediately returns when adding an empty batch") {
    add() shouldEqual ()
  }

  test("trades are stored with id, quantity, price and timestamp") {
    add(t(id = "123", quantity = dec(12), price = dec(34), time = milli(123)))
    val tradesById = retrieve().groupBy(_.id.toString).mapValues(_.head)
    tradesById("123").quantity shouldEqual dec(12)
    tradesById("123").price shouldEqual dec(34)
    tradesById("123").time shouldEqual milli(123)
  }

  test("sells are stored with negative quantity") {
    add(t(quantity = dec(-12)))
    retrieve().head.quantity shouldEqual dec(-12)
  }

  test("the market is set to the store market when trades are retrieved") {
    add(t())
    retrieve().head.market shouldEqual market
  }

  test("the optional order id is stored") {
    add(
      t(id = "1", orderId = None),
      t(id = "2", orderId = Some("ABC"))
    )
    val tradesById = retrieve().groupBy(_.id.toString).mapValues(_.head)
    tradesById("1").orderId shouldEqual None
    tradesById("2").orderId shouldEqual Some("ABC")
  }

  test("up to two fees with ledger refs of the market exchange can be stored") {
    val la = LedgerRef(market.exchangeId, "A")
    val lb = LedgerRef(market.exchangeId, "BBB")
    add(
      t(id = "1", fees = Seq(la -> dec(1))),
      t(id = "2", fees = Seq(la -> dec(1), lb -> dec(-2)))
    )
    val tradesById = retrieve().groupBy(_.id.toString).mapValues(_.head)
    tradesById("1").fees shouldEqual Seq(la -> dec(1))
    tradesById("2").fees shouldEqual Seq(la -> dec(1), lb -> dec(-2))
  }

  test("an exception is thrown when more than 2 fees are given") {
    val la = LedgerRef(market.exchangeId, "A")
    val lb = LedgerRef(market.exchangeId, "B")
    val lc = LedgerRef(market.exchangeId, "C")
    a[RuntimeException] shouldBe thrownBy(add(t(id = "1", fees = Seq(la -> dec(1), lb -> dec(1), lc -> dec(1)))))
  }

  test("an exception is thrown when a fee with ledger ref of a different exchange is given") {
    val la = LedgerRef(ExchangeId(market.exchangeId.value + "X"), "A")
    a[RuntimeException] shouldBe thrownBy(add(t(id = "1", fees = Seq(la -> dec(1)))))
  }

  test("quantity, price and fees are stored with sufficient precision") {
    val la = LedgerRef(market.exchangeId, "A")
    val lb = LedgerRef(market.exchangeId, "B")
    val trade = t(
      id = "123",
      market = market,
      quantity = dec("-10987654321.00000123"),
      price = dec("1234567890.00000123"),
      fees = Seq(la -> dec("1234567890.00000123"), lb -> dec("-1234567890.00000123")),
    )
    add(trade)
    val tradesById = retrieve().groupBy(_.id.toString).mapValues(_.head)
    tradesById("123") shouldEqual trade
  }

  test("trades are returned in a complete batch with given start") {
    add(tradeWithTime(5), tradeWithTime(3), tradeWithTime(4))
    retrieveBatch(Some(milli(2))) shouldEqual TradeBatch(
      start = milli(2),
      trades = Seq(tradeWithTime(3), tradeWithTime(4), tradeWithTime(5)),
      nextBatchStart = None,
    )
  }

  test("batch start is zero when from parameter is not given") {
    add(tradeWithTime(5), tradeWithTime(3), tradeWithTime(4))
    retrieveBatch(from = None).start shouldEqual milli(0)
  }

  test("trades with the same timestamp are ordered by id") {
    val (t1, t2, t3) = (tradeWithTime(1, id = "A"), tradeWithTime(1, id = "B"), tradeWithTime(1, id = "C"))
    add(t2, t3, t1)
    retrieveBatch().trades shouldEqual Seq(t1, t2, t3)
  }

  test("trades can be filtered by start time") {
    add(tradeWithTime(1), tradeWithTime(2), tradeWithTime(3))
    retrieve(start = Some(milli(2))) shouldEqual Seq(tradeWithTime(2), tradeWithTime(3))
  }

  test("trades can be filtered by end time where trades starting at the end milliseconds are excluded") {
    add(tradeWithTime(1), tradeWithTime(2), tradeWithTime(3))
    retrieve(end = Some(milli(3))) shouldEqual Seq(tradeWithTime(1), tradeWithTime(2))
  }

  test("a new store based on the same db reuses an existing table") {
    add(tradeWithTime(1))
    val result = Await.result(new H2TradeStore(connection, market).get(), 3.seconds)
    result.trades shouldEqual Seq(tradeWithTime(1))
  }

  test("trades can be deleted from a certain time on") {
    add(tradeWithTime(1))
    add(tradeWithTime(2, "A"))
    add(tradeWithTime(2, "B"))
    add(tradeWithTime(3))
    deleteFrom(milli(2))
    retrieve() shouldEqual Seq(tradeWithTime(1))
  }

}
