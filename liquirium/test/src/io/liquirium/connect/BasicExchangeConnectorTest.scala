package io.liquirium.connect

import io.liquirium.core.ExchangeId
import io.liquirium.core.helpers.{MarketHelpers, TestWithMocks}
import io.liquirium.core.helpers.async.AsyncTestWithScheduler

class BasicExchangeConnectorTest extends AsyncTestWithScheduler with TestWithMocks {

  protected def makeConnector(
    api: GenericExchangeApi,
    exchangeId: ExchangeId = MarketHelpers.exchangeId("")
  ) : BasicExchangeConnector =
    BasicExchangeConnector.fromExchangeApi(exchangeId, api)

}
