package io.liquirium.core.orderTracking

import io.liquirium.core.Order
import io.liquirium.core.orderTracking.OrderTrackingEvent.{Disappearance, ObservationChange, OrderObservationEvent}
import io.liquirium.eval.{BasicIncrementalValue, IncrementalMap}

trait OpenOrdersHistory extends BasicIncrementalValue[OpenOrdersSnapshot, OpenOrdersHistory] {

  def lastSnapshot: OpenOrdersSnapshot

  def appendIfChanged(snapshot: OpenOrdersSnapshot): OpenOrdersHistory

  def observedIds: Set[String]

  def singleOrderHistory(orderId: String): SingleOrderObservationHistory

  def definedHistoriesById: IncrementalMap[String, SingleOrderObservationHistory]

}

object OpenOrdersHistory {

  def start(snapshot: OpenOrdersSnapshot): OpenOrdersHistory =
    OpenOrdersHistoryImpl(
      firstSnapshot = snapshot,
      lastSnapshot = snapshot,
      observedIds = snapshot.orders.map(_.id),
      previousHistory = None,
      changes = firstChanges(snapshot),
      definedHistoriesById = firstHistories(snapshot),
    )

  private def firstChanges(snapshot: OpenOrdersSnapshot): Map[String, IndexedSeq[OrderObservationEvent]] = {
    def changes(o: Order) = IndexedSeq(ObservationChange(snapshot.timestamp, o))

    snapshot.orders.map(o => (o.id, changes(o))).toMap
  }

  private def firstHistories(snapshot: OpenOrdersSnapshot): IncrementalMap[String, SingleOrderObservationHistory] = {
    val emptyMap = IncrementalMap.empty[String, SingleOrderObservationHistory]
    snapshot.orders.foldLeft(emptyMap) {
      case (m, order) =>
        val sh = SingleOrderObservationHistory(IndexedSeq(ObservationChange(snapshot.timestamp, order)))
        m.update(order.id, sh)
    }
  }

  private case class OpenOrdersHistoryImpl(
    firstSnapshot: OpenOrdersSnapshot,
    lastSnapshot: OpenOrdersSnapshot,
    observedIds: Set[String],
    previousHistory: Option[OpenOrdersHistoryImpl],
    changes: Map[String, Seq[OrderObservationEvent]],
    definedHistoriesById: IncrementalMap[String, SingleOrderObservationHistory],
  ) extends OpenOrdersHistory {

    previousHistory match {
      case Some(s) if s.lastSnapshot.timestamp.isAfter(lastSnapshot.timestamp) =>
        throw new RuntimeException(s"Order snapshot history not properly ordered. "
          + s"Got timestamp ${lastSnapshot.timestamp} after ${s.lastSnapshot.timestamp}")
      case _ => ()
    }

    def appendIfChanged(s: OpenOrdersSnapshot): OpenOrdersHistoryImpl =
      if (s.orders == lastSnapshot.orders) this
      else copy(
        lastSnapshot = s,
        observedIds = observedIds ++ s.orders.map(_.id),
        previousHistory = Some(this),
        changes = updatedChanges(s),
        definedHistoriesById = updatedHistoriesById(s),
      )

    private def updatedChanges(newSnapshot: OpenOrdersSnapshot): Map[String, Seq[OrderObservationEvent]] = {
      val relevantIds = newSnapshot.orderIds ++ lastSnapshot.orderIds
      relevantIds.foldLeft(changes) { (c, id) =>
        if (newSnapshot.get(id) != lastSnapshot.get(id)) {
          val newChange = newSnapshot.get(id) match {
            case Some(o) => ObservationChange(newSnapshot.timestamp, o)
            case None => Disappearance(newSnapshot.timestamp, id)
          }
          c.updated(id, singleOrderHistory(id).changes :+ newChange)
        }
        else c
      }
    }

    private def updatedHistoriesById(newSnapshot: OpenOrdersSnapshot)
    : IncrementalMap[String, SingleOrderObservationHistory] = {
      val relevantIds = newSnapshot.orderIds ++ lastSnapshot.orderIds
      relevantIds.foldLeft(definedHistoriesById) { (c, id) =>
        val oldHist = definedHistoriesById.mapValue.getOrElse(id, SingleOrderObservationHistory(Seq()))
        val newChange = newSnapshot.get(id) match {
          case Some(o) => ObservationChange(newSnapshot.timestamp, o)
          case None => Disappearance(newSnapshot.timestamp, id)
        }
        val newHist = oldHist.append(newChange)
        if (oldHist != newHist) c.update(id, newHist) else c
      }
    }

    @deprecated
    override def singleOrderHistory(orderId: String): SingleOrderObservationHistory =
      SingleOrderObservationHistory(
        changes.getOrElse(orderId, IndexedSeq())
      )

    override def prev: Option[OpenOrdersHistory] = previousHistory

    override def lastIncrement: Option[OpenOrdersSnapshot] = if (prev.isEmpty) None else Some(lastSnapshot)

    override def inc(increment: OpenOrdersSnapshot): OpenOrdersHistory = appendIfChanged(increment)

  }

}
