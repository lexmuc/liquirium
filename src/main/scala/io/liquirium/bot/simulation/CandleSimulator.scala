package io.liquirium.bot.simulation

import io.liquirium.core.{Candle, OrderSet, Trade}

trait CandleSimulator {

  def fillOrders(orders: OrderSet, candle: Candle): (Seq[Trade], OrderSet, CandleSimulator)

}
