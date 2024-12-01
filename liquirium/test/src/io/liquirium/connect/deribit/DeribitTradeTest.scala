package io.liquirium.connect.deribit

import io.liquirium.connect.deribit.DeribitDirection.{Buy, Sell}
import io.liquirium.connect.deribit.helpers.DeribitTestHelpers.deribitTrade
import io.liquirium.core.helpers.BasicTest
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.Instant

class DeribitTradeTest extends BasicTest {

  test("the history id is the id") {
    deribitTrade(id = "id123").historyId shouldEqual "id123"
  }

  test("the history timestamp is the timestamp interpreted as milliseconds") {
    deribitTrade(timestamp = 1234567).historyTimestamp shouldEqual Instant.ofEpochMilli(1234567)
  }

  test("the volume is the product of quantity and price") {
    deribitTrade(price = BigDecimal("1.2"), quantity = BigDecimal("5.5")).volume shouldEqual BigDecimal("6.6")
  }

  test("the trades is considered to be a future trade when the instrument has only one hyphen") {
    deribitTrade(instrument = "BTC-28MAR19").isFutureTrade shouldBe true
    deribitTrade(instrument = "BTC-28MAR19-XXX").isFutureTrade shouldBe false
    deribitTrade(instrument = "BTC-28MAR19-9000-C").isFutureTrade shouldBe false
  }

  test("the position change is the positive amount for buys and the negative amount for sells") {
    deribitTrade(quantity = BigDecimal("1.23"), direction = Buy).positionChange shouldEqual BigDecimal("1.23")
    deribitTrade(quantity = BigDecimal("1.23"), direction = Sell).positionChange shouldEqual BigDecimal("-1.23")
  }

}
