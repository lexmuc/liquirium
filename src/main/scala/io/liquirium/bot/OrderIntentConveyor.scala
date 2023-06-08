package io.liquirium.bot

import io.liquirium.core.OrderConstraints
import io.liquirium.eval.{Context, DerivedEval, Eval, EvalResult}

case class SingleMarketOrderIntentConveyor(
  orderConstraintsEval: Eval[OrderConstraints],
  orderSyncerEval: Eval[OrderIntentSyncer],
) extends DerivedEval[Iterable[BotOutput]] {

  override def eval(
    context: Context,
    oldValue: Option[Iterable[BotOutput]],
  ): (EvalResult[Iterable[BotOutput]], Context) = ???

}
