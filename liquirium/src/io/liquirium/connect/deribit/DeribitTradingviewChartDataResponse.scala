package io.liquirium.connect.deribit

object DeribitTradingviewChartDataResponse {
  sealed trait Status

  case object Status {
    case object Ok extends Status
    case object NoData extends Status
  }
}

case class DeribitTradingviewChartDataResponse
(
  status: DeribitTradingviewChartDataResponse.Status,
  candles: Seq[DeribitCandle]
)