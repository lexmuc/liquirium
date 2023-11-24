package io.liquirium.bot.simulation

import io.liquirium.eval.{Context, EvalResult, UpdatableContext, Value}

trait FirstAndLastContextLogger extends SimulationLogger[FirstAndLastContextLogger] {

  def firstContext: Context

  def lastContext: Context

  override def log(context: UpdatableContext): (EvalResult[FirstAndLastContextLogger], UpdatableContext)

}

object FirstAndLastContextLogger {

  def apply(): FirstAndLastContextLogger = InitState

  private case object InitState extends FirstAndLastContextLogger {
    def firstContext: Context = throw new RuntimeException("no update has been logged so far")

    def lastContext: Context = throw new RuntimeException("no update has been logged so far")

    override def log(context: UpdatableContext): (EvalResult[FirstAndLastContextLogger], UpdatableContext) =
      MainImpl(
        firstContext = context,
        lastContext = context,
      ).log(context)
  }

  private case class MainImpl(
    firstContext: UpdatableContext,
    lastContext: UpdatableContext,
  ) extends FirstAndLastContextLogger {

    override def log(context: UpdatableContext): (EvalResult[FirstAndLastContextLogger], UpdatableContext) =
      (Value(copy(lastContext = context)), context)

  }

}
