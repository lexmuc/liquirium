package io.liquirium.connect.deribit

import io.liquirium.connect.deribit
import io.liquirium.connect.deribit.DeribitDirection.{Buy, Sell}
import play.api.libs.json.{JsArray, JsNumber, JsValue}

import java.time.{Duration, Instant}
import scala.collection.mutable.ArrayBuffer

class DeribitJsonConverter() {

  def convertTrade(t: JsValue): DeribitTrade =
    DeribitTrade(
      id = t("trade_id").as[String],
      sequenceNumber = t("trade_seq").as[Long],
      direction = if (t("direction").as[String] == "buy") Buy else Sell,
      orderId = t("order_id").as[String],
      instrument = t("instrument_name").as[String],
      quantity = t("amount").as[JsNumber].value,
      price = t("price").as[JsNumber].value,
      indexPrice = t("index_price").as[JsNumber].value,
      fee = t("fee").as[JsNumber].value,
      feeCurrency = t("fee_currency").as[String],
      timestamp = t("timestamp").as[Long]
    )

  def convertTrades(v: JsValue): Iterable[DeribitTrade] = v.as[Seq[JsValue]].map(convertTrade)

  def convertTradesResponse(v: JsValue): DeribitTradesResponse =
    DeribitTradesResponse(
      hasMore = v("has_more").as[Boolean],
      trades = convertTrades(v("trades").as[JsArray])
    )

  def convertOrder(o: JsValue): DeribitOrder = DeribitOrder(
    id = o("order_id").as[String],
    direction = if (o("direction").as[String] == "buy") Buy else Sell,
    price = o("price").as[BigDecimal],
    quantity = o("amount").as[BigDecimal],
    filledQuantity = o("filled_amount").as[BigDecimal],
    instrument = o("instrument_name").as[String],
    state = parseOrderState(o("order_state").as[String])
  )

  def convertOrders(v: JsValue): Iterable[DeribitOrder] = v.as[Seq[JsValue]].map(convertOrder)

  private def parseOrderState(s: String): DeribitOrder.State = s match {
    case "open" => DeribitOrder.State.Open
    case "filled" => DeribitOrder.State.Filled
    case "cancelled" => DeribitOrder.State.Cancelled
    case "rejected" => DeribitOrder.State.Rejected
    case "untriggered" => DeribitOrder.State.Untriggered
    case s => throw new RuntimeException(s"Unknown order state '$s'")
  }

  def convertTradingviewCharDataResponse(o: JsValue): DeribitTradingviewChartDataResponse = {
    import DeribitTradingviewChartDataResponse.Status
    if (o("status").as[String] == "no_data") DeribitTradingviewChartDataResponse(Status.NoData, Seq())
    else {
      val ticks = o("ticks").as[ArrayBuffer[Long]]
      val opens = o("open").as[ArrayBuffer[BigDecimal]]
      val closes = o("close").as[ArrayBuffer[BigDecimal]]
      val highs = o("high").as[ArrayBuffer[BigDecimal]]
      val lows = o("low").as[ArrayBuffer[BigDecimal]]
      val volumes = o("volume").as[ArrayBuffer[BigDecimal]]
      val costs = o("cost").as[ArrayBuffer[BigDecimal]]

      def c(index: Int) = DeribitCandle(
        tick = Instant.ofEpochMilli(ticks(index)),
        open = opens(index),
        close = closes(index),
        high = highs(index),
        low = lows(index),
        volume = volumes(index),
        cost = costs(index)
      )

      val candles = ticks.indices.map(x => c(x))
      DeribitTradingviewChartDataResponse(Status.Ok, candles)
    }
  }

  def convertAuthConfirmation(o: JsValue): DeribitAuthConfirmation =
    deribit.DeribitAuthConfirmation(
      token = DeribitAccessToken(o("access_token").as[String]),
      expiresIn = Duration.ofMillis(o("expires_in").as[Long]),
      scopes = o("scope").as[String].split(" ").toSet
    )

  def convertSettlement(o: JsValue): DeribitSettlement = DeribitSettlement(
    indexPrice = o("index_price").as[BigDecimal],
    instrumentName = o("instrument_name").as[String],
    markPrice = o("mark_price").as[BigDecimal],
    position = o("position").as[BigDecimal],
    profitLoss = o("profit_loss").as[BigDecimal],
    sessionProfitLoss = o("session_profit_loss").as[BigDecimal],
    timestamp = o("timestamp").as[Long],
    `type` = o("type").as[String] match {
      case "settlement" => DeribitSettlement.Settlement
      case "delivery" => DeribitSettlement.Delivery
      case x => throw new RuntimeException(s"Failure when parsing settlement: Unknown type '$x'")
    }
  )

  def convertSettlements(o: JsArray): Seq[DeribitSettlement] = o.as[Seq[JsValue]].map(convertSettlement)

  def convertSettlementResponse(o: JsValue): DeribitSettlementsResponse =
    DeribitSettlementsResponse(
      continuationToken = o("continuation").as[String] match {
        case "none" => None
        case t => Some(DeribitContinuationToken(t))
      },
      settlements = convertSettlements(o("settlements").as[JsArray])
    )

  def convertOrderRequestResponse(o: JsValue): DeribitOrderRequestResponse =
    DeribitOrderRequestResponse(convertOrder(o("order")), convertTrades(o("trades")).toSeq)

  def convertCancelRequestResponse(o: JsValue): DeribitCancelRequestResponse =
    DeribitCancelRequestResponse(convertOrder(o))

  def convertInstrumentInfo(v: JsValue): DeribitInstrumentInfo = {
    DeribitInstrumentInfo(
      instrumentName = v("instrument_name").as[String],
      tickSize = v("tick_size").as[JsNumber].value,
      minTradeAmount = v("min_trade_amount").as[JsNumber].value,
      contractSize = v("contract_size").as[JsNumber].value
    )
  }

  def convertInstrumentInfos(v: JsValue): Seq[DeribitInstrumentInfo] = {
    v.as[Seq[JsValue]].map(convertInstrumentInfo)
  }

}