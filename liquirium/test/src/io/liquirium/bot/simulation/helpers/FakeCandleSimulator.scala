package io.liquirium.bot.simulation.helpers

import io.liquirium.bot.simulation.CandleSimulator
import io.liquirium.core.{Candle, OrderSet, Trade}
import org.scalatest.Assertions.fail

case class FakeCandleSimulator(
  expectedInputs: Seq[(OrderSet, Candle)] = Seq(),
  outputs: Seq[(Seq[Trade], OrderSet)] = Seq(),
)
  extends CandleSimulator {

  override def fillOrders(orders: OrderSet, candle: Candle): (Seq[Trade], OrderSet, CandleSimulator) = {
    expectedInputs.headOption match {
      case Some((oo, c)) if (oo != orders || c != candle) =>
        fail(s"candle simulator expected orders $oo and candle $c but got $orders and $candle")
      case _ =>
    }
    val (trades, newOrders) = outputs.headOption getOrElse (Seq(), OrderSet.empty)
    (trades, newOrders, copy(
      outputs = outputs.drop(1),
      expectedInputs = expectedInputs.drop(1),
    ))
  }

  def addOutput(tt: Seq[Trade], os: OrderSet): FakeCandleSimulator =
    copy(outputs = outputs :+ (tt, os))

  def addExpectedInput(orders: OrderSet, candle: Candle): FakeCandleSimulator =
    copy(expectedInputs = expectedInputs :+ (orders, candle))

}
