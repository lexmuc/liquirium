package io.liquirium.bot.helpers

import io.liquirium.bot.{BotEvaluator, BotOutput}
import io.liquirium.eval.{EvalResult, UpdatableContext}

case class TestBotEvaluator(
  getResultForContext: UpdatableContext => (EvalResult[Seq[BotOutput]], UpdatableContext)
) extends BotEvaluator {

  override def eval(context: UpdatableContext): (EvalResult[Seq[BotOutput]], UpdatableContext) =
    getResultForContext(context)

}
