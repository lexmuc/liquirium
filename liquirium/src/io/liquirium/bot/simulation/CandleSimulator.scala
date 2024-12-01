package io.liquirium.bot.simulation

import io.liquirium.core.{Candle, Order, Trade}

trait CandleSimulator {

  def fillOrders(orders: Set[Order], candle: Candle): (Seq[Trade], Set[Order], CandleSimulator)

}
