package io.liquirium.util.akka

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}

trait SourceQueueFactory {

  def getWithActor[T, P](makeBehavior: SourceQueueWithComplete[T] => Behavior[P]): Source[T, NotUsed]

  def getWithRegistration[T](register: SourceQueueWithComplete[T] => Unit): Source[T, NotUsed]

}

object SourceQueueFactory {

  def apply(spawner: ActorSpawner, bufferSize: Int, overflowStrategy: OverflowStrategy): SourceQueueFactory =
    new SourceQueueFactory {

      override def getWithActor[T, P](makeBehavior: SourceQueueWithComplete[T] => Behavior[P]): Source[T, NotUsed] =
        Source.queue[T](bufferSize = bufferSize, overflowStrategy = overflowStrategy)
          .mapMaterializedValue { q => spawner.spawnAsync(makeBehavior(q)); NotUsed }

      override def getWithRegistration[T](register: SourceQueueWithComplete[T] => Unit): Source[T, NotUsed] =
        Source.queue[T](bufferSize = bufferSize, overflowStrategy = overflowStrategy)
          .mapMaterializedValue { q => register(q); NotUsed }

    }
}
