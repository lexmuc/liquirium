package io.liquirium.util.store

import io.liquirium.core.Candle
import io.liquirium.core.helpers.CandleHelpers.{candle, ohlc, ohlcCandle}
import io.liquirium.core.helpers.CoreHelpers.{dec, sec, secs}
import io.liquirium.core.helpers.async.AsyncTestWithControlledTime
import org.scalatest.Assertion

import java.sql.{Connection, DriverManager}
import java.time.{Duration, Instant}
import scala.concurrent.Await
import scala.concurrent.duration._

class H2CandleStoreTest extends AsyncTestWithControlledTime {

  val url = "jdbc:h2:mem:"
  val connection: Connection = DriverManager.getConnection(url)
  var candleLength: Duration = Duration.ofSeconds(1)

  def store = new H2CandleStore(connection, candleLength)

  val defaultLength: Duration = secs(1)

  def c(start: Long, length: Duration = defaultLength): Candle = candle(sec(start), length)

  def c(start: Long, n: Int): Candle = ohlcCandle(sec(start), candleLength, ohlc(n))

  def candleWithDifferentFieldValues(
    startTime: Instant,
    length: Duration,
    baseValue: Option[BigDecimal] = None,
  ): Candle = {
    val d = baseValue getOrElse BigDecimal(startTime.getEpochSecond)
    Candle(
      startTime = startTime,
      length = length,
      open = d - 0.1,
      close = d + 0.1,
      high = d + 0.2,
      low = d - 0.2,
      quoteVolume = d * 2
    )
  }

  private def add(candles: Candle*): Unit = {
    Await.ready(store.add(candles), 3.seconds)
  }

  private def deleteFrom(start: Instant): Unit = {
    Await.ready(store.deleteFrom(start), 3.seconds)
  }

  private def deleteBefore(start: Instant): Unit = {
    Await.ready(store.deleteBefore(start), 3.seconds)
  }

  private def getFirstStartAndLastEnd: Option[(Instant, Instant)] = {
    Await.result(store.getFirstStartAndLastEnd, 3.seconds)
  }

  private def retrieve(
    start: Option[Instant] = None,
    end: Option[Instant] = None
  ): Iterable[Candle] = {
    Await.result(store.get(start, end), 3.seconds)
  }

  def clear(): Unit = store.clear()

  def assertCount(table: String, count: Int): Assertion = {
    val query = s"SELECT COUNT(*) FROM $table"
    //noinspection SqlSourceToSinkFlow
    val rs = connection.createStatement().executeQuery(query)
    rs.next()
    rs.getInt(1) shouldEqual count
  }

  test("candles are stored in a table 'CANDLES'") {
    add(c(1), c(2))
    assertCount("CANDLES", 2)
  }

  test("candles are stored and can be retrieved with all fields and the given length") {
    candleLength = secs(123)
    val c1 = candle(
      start = sec(1),
      length = candleLength,
      open = dec("0.0002"),
      close = dec("0.0003"),
      high = dec("10.0001"),
      low = dec("0.0001"),
      quoteVolume = dec("1.2345"),
    )
    val c2 = c(2, secs(123))
    add(c1, c2)
    retrieve().toSet shouldEqual Set(c1, c2)
  }

  test("candles are returned in ascending order") {
    add(c(3), c(1), c(2))
    retrieve() shouldEqual Seq(c(1), c(2), c(3))
  }

  test("candles can be filtered by start time") {
    add(c(1), c(2), c(3))
    retrieve(start = Some(sec(2))) shouldEqual Seq(c(2), c(3))
  }

  test("candles can be filtered by end time where candles starting at the end second are excluded") {
    add(c(1), c(2), c(3))
    retrieve(end = Some(sec(3))) shouldEqual Seq(c(1), c(2))
  }

  test("the first candle start and latest candle end can be determined with one request") {
    add(
      c(2),
      c(3),
      c(4),
    )
    getFirstStartAndLastEnd shouldEqual Some((c(2).startTime, c(4).endTime))
  }

  test("first candle start and last candle end are None when the store is empty") {
    getFirstStartAndLastEnd shouldEqual None
  }

  test("candles can be updated simply by adding new candles for the same time") {
    add(c(1, 11), c(2, 22))
    add(c(1, 123))
    retrieve() shouldEqual Seq(c(1, 123), c(2, 22))
  }

  test("candles can be cleared") {
    add(c(1))
    clear()
    retrieve() shouldEqual Seq()
  }

  test("test candles can be deleted from a specific time") {
    add(c(1), c(2), c(3))
    deleteFrom(sec(2))
    retrieve() shouldEqual Seq(c(1))
  }

  test("test candles can be deleted before a specific time") {
    add(c(1), c(2), c(3))
    deleteBefore(sec(2))
    retrieve() shouldEqual Seq(c(2), c(3))
  }

  test("candles starting before the given start are not deleted") {
    candleLength = secs(10)
    add(c(10, secs(10)), c(20, secs(10)), c(30, secs(10)))
    deleteFrom(sec(21))
    retrieve() shouldEqual Seq(c(10, secs(10)), c(20, secs(10)))
  }

  test("a new store based on the same db reuses an existing table") {
    add(c(1))
    val f = new H2CandleStore(connection, candleLength).get()
    Await.result(f, 3.seconds) shouldEqual Seq(c(1))
  }

}
