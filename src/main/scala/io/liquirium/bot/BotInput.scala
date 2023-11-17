package io.liquirium.bot

import io.liquirium.core._
import io.liquirium.core.orderTracking.OpenOrdersHistory
import io.liquirium.eval.{IncrementalSeq, Input}

import java.time.{Duration, Instant}

trait BotInput[+T] extends Input[T]

object BotInput {

  case class OrderSnapshotHistoryInput(market: Market) extends BotInput[OpenOrdersHistory]

  case class ConfigValue[T](name: String) extends BotInput[T]

  case class CandleHistoryInput(
    market: Market,
    candleLength: Duration,
    start: Instant,
  ) extends BotInput[CandleHistorySegment]

  case class TradeHistoryInput(market: Market, start: Instant) extends BotInput[TradeHistorySegment]

  case class SimulatedOpenOrdersInput(market: Market) extends BotInput[Set[Order]]

  case class TimeInput(resolution: Duration) extends BotInput[Instant]

  case class KeyboardInputEvent(line: String, time: Instant)

  case object KeyboardInput extends BotInput[IncrementalSeq[KeyboardInputEvent]]

  object ConfigValue {
    val start: ConfigValue[Instant] = ConfigValue[Instant]("start")
  }

  case class CompletedOperationRequest(
    completionTime: Instant,
    requestMessage: OperationRequestMessage,
    response: Either[Throwable, OperationRequestSuccessResponse[_ <: OperationRequest]]
  )

  case object CompletedOperationRequestsInSession extends BotInput[IncrementalSeq[CompletedOperationRequest]]

  case object BotOutputHistory extends BotInput[IncrementalSeq[BotOutput]]

}
