package io.liquirium.bot

import io.liquirium.core.{OperationRequest, OperationRequestId}

sealed trait BotOutput

case class OperationRequestMessage(id: OperationRequestId, request: OperationRequest) extends BotOutput

trait BotLogEntry extends BotOutput
