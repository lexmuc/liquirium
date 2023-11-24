package io.liquirium.bot

import io.liquirium.bot.BotInput.BotOutputHistory
import io.liquirium.eval._

object SimpleBotEvaluator {

  private val historyEval = InputEval(BotOutputHistory)

  def apply(botEval: Eval[Iterable[BotOutput]]): BotEvaluator = {
    Impl(botEval, IncrementalSeq.empty[BotOutput])
  }

  case class Impl(
    botEval: Eval[Iterable[BotOutput]],
    outputHistory: IncrementalSeq[BotOutput],
  ) extends BotEvaluator {

    override def eval(context: UpdatableContext): (EvalResult[Seq[BotOutput]], UpdatableContext) = {

      context.evaluate(historyEval) match {
        case (Value(h), context2) =>
          context2.evaluate(botEval) match {
            case (Value(oo), context3) =>
              if (oo.isEmpty) (Value(oo.toSeq), context3)
              else {
                val finalContext = context3.update(InputUpdate(Map(BotOutputHistory -> oo.foldLeft(h)(_.inc(_)))))
                (Value(oo.toSeq), finalContext)
              }
            case (ir: InputRequest, context3) => (ir, context3)
          }
        case (ir: InputRequest, context2) => (ir, context2)
      }
    }

  }

}
