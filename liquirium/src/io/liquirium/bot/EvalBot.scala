package io.liquirium.bot

import io.liquirium.eval.Eval

trait EvalBot {

  def eval: Eval[Iterable[BotOutput]]

}
