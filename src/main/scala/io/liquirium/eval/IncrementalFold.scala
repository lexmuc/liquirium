package io.liquirium.eval

import io.liquirium.eval.IncrementalFold.{ConcreteState, State}

import scala.annotation.tailrec


object IncrementalFold {

  trait State[I, IV <: IncrementalValue[I, IV], T] extends IncrementalValue[I, State[I, IV, T]] {

    def baseValue: IV

    def value: T

    def prev: Option[State[I, IV, T]]

    def update(input: IV): State[I, IV, T]

  }

  def apply[I, IV <: IncrementalValue[I, IV], T](startValue: IV => T)(step: (T, I) => T): IncrementalFold[I, IV, T] =
    Impl(startValue, step)

  // folds should be a case classes so they can be compared for equality
  private case class Impl[I, IV <: IncrementalValue[I, IV], T](
    sv: IV => T,
    s: (T, I) => T,
  ) extends IncrementalFold[I, IV, T] {
    override def startValue(baseValue: IV): T = sv(baseValue)

    override def step(oldValue: T, increment: I): T = s(oldValue, increment)
  }

  private case class ConcreteState[I, IV <: IncrementalValue[I, IV], T](
    fold: IncrementalFold[I, IV, T],
    baseValue: IV,
    value: T,
    prev: Option[ConcreteState[I, IV, T]],
  ) extends State[I, IV, T] {

    override def update(input: IV): State[I, IV, T] = findBacktrackBaseState(input) match {

      case Some(baseState) if baseState.baseValue eq input => this

      case Some(baseState) =>
        val newValues = input.incrementsAfter(baseState.baseValue)
        val newValue = newValues.foldLeft(baseState.value) {
          case (v, e) => fold.step(v, e)
        }
        ConcreteState(fold, input, newValue, Some(this))

      case None => fold.fold(input)

    }

    @tailrec
    final protected def findBacktrackBaseState(il: IV): Option[ConcreteState[I, IV, T]] =
      if (baseValue.isAncestorOf(il)) Some(this)
      else prev match {
        case Some(p) => p.findBacktrackBaseState(il)
        case None => None
      }

    override def latestCommonAncestor(other: State[I, IV, T]): Option[State[I, IV, T]] = {
      if (other.isAncestorOf(this)) Some(other)
      else findBacktrackBaseState(other.baseValue)
    }

    override def incrementsAfter(other: State[I, IV, T]): Iterable[I] = baseValue.incrementsAfter(other.baseValue)

    override val root: State[I, IV, T] = prev match {
      case Some(s) => s.root
      case None => this
    }

    override def isAncestorOf(other: State[I, IV, T]): Boolean = baseValue.isAncestorOf(other.baseValue)

  }

}

trait IncrementalFold[I, IV <: IncrementalValue[I, IV], T] {

  // the start value may depend on the baseValue but if it does it must depend only on the root of the base value
  // so it must not change for related incremental values
  def startValue(baseValue: IV): T

  def step(oldValue: T, increment: I): T

  def fold(iv: IV): State[I, IV, T] = ConcreteState(this, iv.root, startValue(iv.root), None).update(iv)

}
