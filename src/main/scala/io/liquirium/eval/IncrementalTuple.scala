package io.liquirium.eval

trait IncrementalTuple[A, B, VA <: IncrementalValue[A, VA], VB <: IncrementalValue[B, VB]]
  extends IncrementalValue[Either[A, B], IncrementalTuple[A, B, VA, VB]] {

  def _1: VA

  def _2: VB

}

object IncrementalTuple {

  def apply[A, B, VA <: IncrementalValue[A, VA], VB <: IncrementalValue[B, VB]](
    a: IncrementalValue[A, VA],
    b: IncrementalValue[B, VB],
  ) : IncrementalTuple[A, B, VA, VB] =
    TupleImpl[A, B, VA, VB](a.asInstanceOf[VA], b.asInstanceOf[VB])

  case class TupleImpl[A, B, VA <: IncrementalValue[A, VA], VB <: IncrementalValue[B, VB]](a: VA, b: VB)
    extends IncrementalTuple[A, B, VA, VB] {

    override def _1: VA = a

    override def _2: VB = b

    override def latestCommonAncestor(other: IncrementalTuple[A, B, VA, VB]): Option[IncrementalTuple[A, B, VA, VB]] =
      (a.latestCommonAncestor(other._1), b.latestCommonAncestor(other._2)) match {
        case (Some(aa), Some(ab)) => Some(IncrementalTuple(aa, ab))
        case _ => None
      }

    override def incrementsAfter(other: IncrementalTuple[A, B, VA, VB]): Iterable[Either[A, B]] =
      a.incrementsAfter(other._1).map(x => Left(x)) ++ b.incrementsAfter(other._2).map(x => Right(x))

    override def root: IncrementalTuple[A, B, VA, VB] = IncrementalTuple(a.root, b.root)

    override def isAncestorOf(other: IncrementalTuple[A, B, VA, VB]): Boolean =
      a.isAncestorOf(other._1) && b.isAncestorOf(other._2)

  }

}
