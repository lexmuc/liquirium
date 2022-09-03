package io.liquirium.bot

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import io.liquirium.bot.BotInput.{KeyboardInput, KeyboardInputEvent}
import io.liquirium.eval.IncrementalSeq
import io.liquirium.util.{Clock, KeyboardInputReader}
import io.liquirium.util.akka.ActorSpawner

class KeyboardInputProvider(
  inputReader: KeyboardInputReader,
  clock: Clock,
  spawner: ActorSpawner,
  queueBufferSize: Int,
) extends BotInputProvider {

  private sealed trait Protocol

  private case class NewLine(line: String) extends Protocol

  private def behavior(queue: SourceQueueWithComplete[IncrementalSeq[KeyboardInputEvent]]): Behavior[Protocol] =
    Behaviors.setup { context =>
      queue.offer(IncrementalSeq.empty[KeyboardInputEvent])
      inputReader.start { line =>
        context.self ! NewLine(line)
      }

      def running(history: IncrementalSeq[KeyboardInputEvent]): Behavior[Protocol] =
        Behaviors.receiveMessage {
          case NewLine(l) =>
            val newHistory = history.inc(KeyboardInputEvent(l, clock.getTime))
            queue.offer(newHistory)
            running(newHistory)
        }

      running(IncrementalSeq.empty)
    }

  override def getInputUpdateStream[T](input: BotInput[T]): Option[Source[T, NotUsed]] =
    input match {
      case KeyboardInput => Some(
        Source.queue[IncrementalSeq[KeyboardInputEvent]](queueBufferSize, OverflowStrategy.fail)
          .mapMaterializedValue { q =>
            spawner.spawnAsync(behavior(q))
            NotUsed
          }.asInstanceOf[Source[T, NotUsed]]
      )
      case _ => None
    }

}
