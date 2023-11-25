package io.liquirium.bot

import io.liquirium.eval.{EvalResult, UpdatableContext}

trait BotEvaluator {

  def eval(context: UpdatableContext): (EvalResult[Seq[BotOutput]], UpdatableContext)

}
