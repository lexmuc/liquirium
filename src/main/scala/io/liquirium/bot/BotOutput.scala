package io.liquirium.bot

import io.liquirium.core.{OperationRequest, TradeRequestId}

sealed trait BotOutput

case class OperationRequestMessage(id: TradeRequestId, request: OperationRequest) extends BotOutput

trait BotLogEntry extends BotOutput
