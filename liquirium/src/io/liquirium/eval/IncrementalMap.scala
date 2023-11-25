package io.liquirium.eval

import io.liquirium.eval.IncrementalMap.MapFoldState


trait IncrementalMap[K, V] extends IncrementalValue[(K, Option[V]), IncrementalMap[K, V]] {

  def mapValue: Map[K, V]

  def update(k: K, v: V): IncrementalMap[K, V]

  def deleteKey(k: K): IncrementalMap[K, V]

  protected def updates: IncrementalSeq[(K, Option[V])]

  def apply(key: K): V

  protected def foldState: MapFoldState[K, V]

}

object IncrementalMap {

  type MapFoldState[K, V] = IncrementalFold.State[(K, Option[V]), IncrementalSeq[(K, Option[V])], Map[K, V]]

  case class MapFold[K, V]() extends IncrementalFold[(K, Option[V]), IncrementalSeq[(K, Option[V])], Map[K, V]] {

    override def startValue(baseValue: IncrementalSeq[(K, Option[V])]): Map[K, V] = Map()

    override def step(oldValue: Map[K, V], increment: (K, Option[V])): Map[K, V] = {
      increment._2 match {
        case Some(v) => oldValue.updated(increment._1, v)
        case None => oldValue - increment._1
      }
    }

  }

  // it is currently required by the incremental fold that the empty map is always the same instance
  private val emptyMap = MapImpl(MapFold[Unit, Unit]().fold(IncrementalSeq()))

  def empty[K, V]: IncrementalMap[K, V] = {
    if (emptyMap == null) throw new RuntimeException("empty map is null")
    emptyMap.asInstanceOf[IncrementalMap[K, V]]
  }

  case class MapImpl[K, V](foldState: MapFoldState[K, V]) extends IncrementalMap[K, V] {

    override def latestCommonAncestor(other: IncrementalMap[K, V]): Option[IncrementalMap[K, V]] =
      foldState.latestCommonAncestor(other.foldState).map(MapImpl.apply)

    override def incrementsAfter(other: IncrementalMap[K, V]): Iterable[(K, Option[V])] =
      updates.incrementsAfter(other.updates)

    override lazy val root: IncrementalMap[K, V] = empty

    override def isAncestorOf(other: IncrementalMap[K, V]): Boolean = updates.isAncestorOf(other.updates)

    override def apply(key: K): V = mapValue(key)

    override def mapValue: Map[K, V] = foldState.value

    override def update(k: K, v: V): IncrementalMap[K, V] = copy(foldState.update(updates.inc((k, Some(v)))))

    override def deleteKey(k: K): IncrementalMap[K, V] = copy(foldState.update(updates.inc((k, None))))

    override protected def updates: IncrementalSeq[(K, Option[V])] = foldState.baseValue

  }

}

