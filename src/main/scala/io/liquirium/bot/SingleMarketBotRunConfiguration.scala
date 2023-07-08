package io.liquirium.bot

import io.liquirium.core.{ExactResources, Market}

import java.time.Instant

case class SingleMarketBotRunConfiguration(
  market: Market,
  startTime: Instant,
  endTimeOption: Option[Instant],
  initialResources: ExactResources,
)
