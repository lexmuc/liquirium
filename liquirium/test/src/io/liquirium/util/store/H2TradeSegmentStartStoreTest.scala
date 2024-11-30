package io.liquirium.util.store

import io.liquirium.core.helpers.CoreHelpers.sec
import io.liquirium.core.helpers.async.AsyncTestWithControlledTime
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.sql.DriverManager
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration._

class H2TradeSegmentStartStoreTest extends AsyncTestWithControlledTime {

  private val url = "jdbc:h2:mem:"
  private val connection = DriverManager.getConnection(url)

  private val store = new H2TradeSegmentStartStore(connection)

  private def saveStart(start: Instant): Unit = {
    Await.result(store.saveStart(start), 3.seconds)
  }

  private def assertCount(table: String, count: Int) = {
    val rs = connection.createStatement().executeQuery(s"SELECT COUNT(*) FROM $table")
    rs.next()
    rs.getInt(1) shouldEqual count
  }

  private def readStart(): Option[Instant] = {
    Await.result(store.readStart, 3.seconds)
  }

  test("the start is stored in a table 'TRADE_SEGMENT_META' which is initially empty") {
    assertCount("TRADE_SEGMENT_META", 0)
    saveStart(sec(123))
    assertCount("TRADE_SEGMENT_META", 1)
  }

  test("retrieving the start from an empty store yields None") {
    readStart() shouldEqual None
  }

  test("when a start is stored it can be retrieved") {
    saveStart(sec(123))
    readStart() shouldEqual Some(sec(123))
  }

  test("the start can be updated but there is never more than one row") {
    saveStart(sec(123))
    saveStart(sec(234))
    readStart() shouldEqual Some(sec(234))
    assertCount("TRADE_SEGMENT_META", 1)
  }

}
