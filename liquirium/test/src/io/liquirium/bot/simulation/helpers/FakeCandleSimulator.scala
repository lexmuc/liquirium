package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.CandleSimulator
import io.liquirium.core.{Candle, Order, Trade}
import org.scalatest.Assertions.fail

case class FakeCandleSimulator(
  expectedInputs: Seq[(Set[Order], Candle)] = Seq(),
  outputs: Seq[(Seq[Trade], Set[Order])] = Seq(),
)
  extends CandleSimulator {

  override def fillOrders(orders: Set[Order], candle: Candle): (Seq[Trade], Set[Order], CandleSimulator) = {
    expectedInputs.headOption match {
      case Some((oo, c)) if (oo != orders || c != candle) =>
        fail(s"candle simulator expected orders $oo and candle $c but got $orders and $candle")
      case _ =>
    }
    val (trades, newOrders) = outputs.headOption getOrElse (Seq(), Set[Order]())
    (trades, newOrders, copy(
      outputs = outputs.drop(1),
      expectedInputs = expectedInputs.drop(1),
    ))
  }

  def addOutput(tt: Seq[Trade], os: Set[Order]): FakeCandleSimulator =
    copy(outputs = outputs :+ (tt, os))

  def addExpectedInput(orders: Set[Order], candle: Candle): FakeCandleSimulator =
    copy(expectedInputs = expectedInputs :+ (orders, candle))

}
