package io.liquirium.connect.deribit

case class DeribitSettlementsResponse
(
  settlements: Seq[DeribitSettlement],
  continuationToken: Option[DeribitContinuationToken]
)
