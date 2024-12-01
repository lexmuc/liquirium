package io.liquirium.bot.simulation

import io.liquirium.core
import io.liquirium.core._

import java.time.Instant
import scala.annotation.tailrec

case class ScaledCandleSimulator(
  feeLevel: FeeLevel,
  volumeReduction: Double,
  tradeIds: Stream[TradeId],
) extends CandleSimulator {

  import io.liquirium.core.OrderSetOps.OrderSetOps

  private case class FillIntent(order: Order, maxVolume: Double) {
    def makeTrade(id: TradeId, time: Instant): Trade = {
      val actualVolume = Math.min(order.volume.toDouble, maxVolume)
      val actualQuantity = order.openQuantity.toDouble * (actualVolume / order.volume.toDouble)
      core.Trade(
        id = id,
        market = order.market,
        orderId = Some(order.id),
        quantity = BigDecimal(actualQuantity),
        price = order.price,
        fees = feeLevel.apply(order.market, quantity = actualQuantity, price = order.price),
        time = time,
      )
    }
  }

  override def fillOrders(orders: Set[Order], candle: Candle) : (Seq[Trade], Set[Order], ScaledCandleSimulator) = {

    val sortedBuyOrders = orders.filter(_.isBuy).toSeq.sortBy(_.price * -1)
    val sortedSellOrders = orders.filter(_.isSell).toSeq.sortBy(_.price)

    val (exceededBuyMatches, restBuyMatches) = matchingBuyOrders(sortedBuyOrders, candle)
    val (exceededSellMatches, restSellMatches) = matchingSellOrders(sortedSellOrders, candle)

    val (availableExceededBuyVolume, availableExceededSellVolume) =
      availableVolumes(exceededBuyMatches, exceededSellMatches, candle.quoteVolume)

    val totalExceededVolume = (exceededBuyMatches ++ exceededSellMatches).iterator.map(_.volume).sum.toDouble
    val restVolume = Math.max(0, candle.quoteVolume.toDouble - totalExceededVolume) * volumeReduction
    val (restBuyVolume, restSellVolume) = availableVolumes(restBuyMatches, restSellMatches, restVolume)

    val fills = fillOrders(exceededBuyMatches, availableExceededBuyVolume) ++
      fillOrders(exceededSellMatches, availableExceededSellVolume) ++
      fillOrders(restBuyMatches, restBuyVolume) ++
      fillOrders(restSellMatches, restSellVolume)

    val trades = getTrades(fills, candle.startTime)
    val newOrders = trades.foldLeft(orders)(_.record(_))
    (trades, newOrders, this.copy(tradeIds = tradeIds.drop(trades.size)))
  }

  def getTrades(fills: Seq[FillIntent], time: Instant): Seq[Trade] =
    fills.zip(tradeIds).map { case (f, tid) => f.makeTrade(tid, time) }

  private def matchingBuyOrders(sortedBuyOrders: Seq[Order], c: Candle)
  : (Iterable[Order], Iterable[Order]) = {
    val exceededBuyMatches = sortedBuyOrders.takeWhile(_.price > c.high)
    val restBuyMatches = sortedBuyOrders.dropWhile(_.price > c.high).takeWhile(_.price > c.low)
    (exceededBuyMatches, restBuyMatches)
  }

  private def matchingSellOrders(sortedSellOrders: Seq[Order], c: Candle)
  : (Iterable[Order], Iterable[Order]) = {
    val exceededSellMatches = sortedSellOrders.takeWhile(_.price < c.low)
    val restSellMatches = sortedSellOrders.dropWhile(_.price < c.low).takeWhile(_.price < c.high)
    (exceededSellMatches, restSellMatches)
  }

  private def availableVolumes(buyOrders: Iterable[Order], sellOrders: Iterable[Order], volume: BigDecimal)
  : (Double, Double) = {
    val buyVolume = buyOrders.iterator.map(_.volume).sum
    val sellVolume = sellOrders.iterator.map(_.volume).sum

    val total = buyVolume + sellVolume
    if (total == 0) (0, 0)
    else (volume.toDouble * buyVolume.toDouble / total.toDouble, volume.toDouble * sellVolume.toDouble / total.toDouble)
  }

  private def fillOrders(orders: Iterable[Order], volume: Double): Seq[FillIntent] = {
    @tailrec
    def go(restOrders: Iterable[Order], restVolume: Double, acc: Seq[FillIntent]): Seq[FillIntent] =
      if (restVolume > 0 && restOrders.nonEmpty) {
        val newRestVolume = restVolume - restOrders.head.volume.toDouble
        go(restOrders.drop(1), newRestVolume, acc :+ FillIntent(restOrders.head, restVolume))
      }
      else acc

    go(orders, volume, Seq())
  }

}
