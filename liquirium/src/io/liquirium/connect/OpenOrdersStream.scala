package io.liquirium.connect

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.liquirium.core.{Market, Order}
import io.liquirium.util.Logger
import io.liquirium.util.akka.StreamExtensions.SourceLazyOps

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object OpenOrdersStream {

  case class Update(openOrders: Set[Order])

  trait Factory {
    def getOrderStream(optMarket: Option[Market]): Source[Update, NotUsed]
  }

  def fromOrdersProvider(
    optMarket: Option[Market],
    interval: FiniteDuration,
    retryDelay: FiniteDuration,
    openOrdersProvider: Option[Market] => Future[Set[Order]],
    logger: Logger,
  ): Source[Update, NotUsed] = {

    val singleUpdateSource = Source.lazyFuture[Set[Order]] { () =>
      openOrdersProvider.apply(optMarket)
    }.map(oo => Update(oo))
    val updateWithRetriesSource = singleUpdateSource.recoverWithRetries[Update](-1, {
      case e: Throwable =>
        logger.warn("Failed to get new orders via open orders provider", e)
        singleUpdateSource.initialDelay(retryDelay)
    })
    Source.repeat(()).flatMapConcat { _ =>
      updateWithRetriesSource.concatLazily(Source.single(()).initialDelay(interval).flatMapConcat(_ => Source.empty))
    }

  }

}
