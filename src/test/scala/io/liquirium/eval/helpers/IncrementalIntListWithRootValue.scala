package io.liquirium.eval.helpers

import io.liquirium.eval.BasicIncrementalValue
import io.liquirium.eval.helpers.IncrementalIntListWithRootValue.NonEmptyIntListWithRootValue

trait IncrementalIntListWithRootValue extends BasicIncrementalValue[Int, IncrementalIntListWithRootValue] {

  override def inc(increment: Int): IncrementalIntListWithRootValue =
    NonEmptyIntListWithRootValue(this, Some(increment))

  def rootValue: Int

}

object IncrementalIntListWithRootValue {

  def empty(baseValue: Int, sideEffectOnRead: () => Unit = () => ()): IncrementalIntListWithRootValue =
    EmptyIncrementalIntListWithRootValue(baseValue, sideEffectOnRead)

  def apply(root: Int)(ii: Int*): IncrementalIntListWithRootValue =
    ii.foldLeft(IncrementalIntListWithRootValue.empty(root)) (_.inc(_))

  private case class EmptyIncrementalIntListWithRootValue(
    rootValue: Int,
    sideEffectOnRead: () => Unit,
  ) extends IncrementalIntListWithRootValue {

    override def prev: Option[IncrementalIntListWithRootValue] = None

    override def lastIncrement: Option[Int] = None

  }

  private case class NonEmptyIntListWithRootValue(
    previousList: IncrementalIntListWithRootValue,
    lastElement: Option[Int] = None,
  ) extends IncrementalIntListWithRootValue {
    def prev: Option[IncrementalIntListWithRootValue] = Some(previousList)

    def lastIncrement: Option[Int] = {
      root.asInstanceOf[EmptyIncrementalIntListWithRootValue].sideEffectOnRead()
      lastElement
    }

    override def rootValue: Int = root.rootValue
  }

}
