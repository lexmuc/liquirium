package io.liquirium.connect

import io.liquirium.core.helpers.MarketHelpers
import org.mockito.Mockito.mock
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BasicExchangeConnectorTest_ExchangeId extends BasicExchangeConnectorTest {

  test("it exposes the given exchange id") {
    val id = MarketHelpers.exchangeId("ABC")
    val connector = makeConnector(mock(classOf[GenericExchangeApi]), id)
    connector.exchangeId shouldBe id
  }

}
