package io.liquirium.bot

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.typed.scaladsl.ActorSink
import io.liquirium.bot.BotRunner.ShutdownReason
import io.liquirium.bot.BotRunner.ShutdownReason.EvaluationFailure
import io.liquirium.eval.{Input, InputRequest, InputUpdate, UpdatableContext, Value}
import io.liquirium.util.akka.ActorSpawner


object BotRunner {

  sealed trait ShutdownReason

  object ShutdownReason {

    case class EvaluationFailure(exception: Throwable) extends ShutdownReason

    case class UnknownInput(input: Input[_]) extends ShutdownReason

    case class InputStreamFailure(input: Input[_], exception: Throwable) extends ShutdownReason

    case class InputStreamCompletion(input: Input[_]) extends ShutdownReason

    case class UnprocessableOutput(out: BotOutput) extends ShutdownReason

  }

}

case class BotRunner(actorSpawner: ActorSpawner) {

  private sealed trait Protocol

  private case class SourceFailed(i: Input[_], t: Throwable) extends Protocol

  private case class SourceComplete(i: Input[_]) extends Protocol

  private case class GotInputUpdate[T](input: Input[T], value: T) extends Protocol

  def run(
    evaluator: BotEvaluator,
    inputProvider: BotInputProvider,
    outputProcessor: BotOutputProcessor,
    shutdownHandler: ShutdownReason => Unit,
    context: UpdatableContext,
  ): Unit =
    actorSpawner.spawnAsync(
      behavior(
        evaluator = evaluator,
        inputProvider = inputProvider,
        outputProcessor = outputProcessor,
        shutdownHandler = shutdownHandler,
        initialContext = context,
      )
    )

  private def behavior(
    evaluator: BotEvaluator,
    inputProvider: BotInputProvider,
    outputProcessor: BotOutputProcessor,
    shutdownHandler: ShutdownReason => Unit,
    initialContext: UpdatableContext,
  ): Behavior[Protocol] =

    Behaviors.setup { actorContext =>
      implicit val system: ActorSystem[Nothing] = actorContext.system

      def evalAndProcessResult(
        alreadyRequestedInputs: Set[Input[_]],
        context: UpdatableContext,
      ): Behavior[Protocol] = try {
        val (evalResult, newContext) = evaluator.eval(context)
        evalResult match {
          case Value(outputs) =>
            val processResults = outputs.map(o => (o, outputProcessor.processOutput(o)))
            processResults.collectFirst { case (o, r) if !r => o } match {
              case Some(o) =>
                shutdown(ShutdownReason.UnprocessableOutput(o))
              case None => running(newContext, alreadyRequestedInputs)
            }

          case InputRequest(requestedInputs) =>
            subscribeToNewInputs(
              newContext = newContext,
              alreadyRequestedInputs = alreadyRequestedInputs,
              newInputs = requestedInputs -- alreadyRequestedInputs,
            )
        }
      } catch {
        case t: Throwable =>
          shutdown(EvaluationFailure(t))
      }

      def subscribeToNewInputs(
        newInputs: Iterable[Input[_]],
        newContext: UpdatableContext,
        alreadyRequestedInputs: Set[Input[_]],
      ): Behavior[Protocol] = {
        val sourcesByInput = newInputs.toSeq.map(i => (i, inputProvider.getInputUpdateStream(i.asInstanceOf[BotInput[_]])))
        sourcesByInput.collectFirst { case (i, None) => i } match {
          case Some(unknownInput) =>
            shutdown(ShutdownReason.UnknownInput(unknownInput))
          case None =>
            sourcesByInput.foreach { case (i, source) =>
              val sink = ActorSink.actorRef[Protocol](actorContext.self, SourceComplete(i), t => SourceFailed(i, t))
              source.get.map(v => GotInputUpdate(i, v)).runWith(sink)
            }
            running(newContext, alreadyRequestedInputs ++ newInputs)
        }
      }

      def shutdown(reason: ShutdownReason): Behavior[Protocol] = {
        shutdownHandler(reason)
        Behaviors.stopped
      }

      def running(
        context: UpdatableContext,
        alreadyRequestedInputs: Set[Input[_]],
      ): Behavior[Protocol] =
        Behaviors.receiveMessage[Protocol] {
          case GotInputUpdate(input, value) =>
            val updatedContext = context.update(InputUpdate(Map(input -> value)))
            evalAndProcessResult(alreadyRequestedInputs, updatedContext)
          case SourceFailed(input, throwable) =>
            shutdown(ShutdownReason.InputStreamFailure(input, throwable))
          case SourceComplete(input) =>
            shutdown(ShutdownReason.InputStreamCompletion(input))
        }

      evalAndProcessResult(Set(), initialContext)
    }

}
