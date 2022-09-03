package io.liquirium.eval

import io.liquirium.eval.Eval.sequence

import scala.reflect.ClassTag


sealed trait Eval[+M] {

  def map[N](f: M => N): Eval[N] = MappedEval(this, f)

  def flatMap[N](f: M => Eval[N]): Eval[N] = FlattenedEval(this.map(f))

  def getMark: Option[String] = None

  def mark(m : String): Eval[M] = MarkedEval(this, m)

}

case class MarkedEval[M](baseEval: Eval[M], mark: String) extends DerivedEval[M] {
  override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) = {
    context.evaluate(baseEval)
  }

  override def getMark: Option[String] = Some(mark)
}

case class MappedEval[M, N](baseEval: Eval[M], f: M => N) extends DerivedEval[N] {

  override def eval(context: Context, oldValue: Option[N]): (EvalResult[N], Context) = {
    val (evalResult, newContext) = context.evaluate(baseEval)
    val mappedResult = evalResult match {
      case Value(v) => Value(f(v))
      case ir: InputRequest => ir
    }
    (mappedResult, newContext)
  }

  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

}

case class FlattenedEval[M](baseEval: Eval[Eval[M]]) extends DerivedEval[M] {

  override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) =
    context.evaluate(baseEval) match {
      case (Value(v), nextContext) => nextContext.evaluate(v) match {
        case (x: Value[M], finalContext) => (x, finalContext)
        case (ir: InputRequest, finalContext) => (ir, finalContext)
      }
      case (ir: InputRequest, nextContext) => (ir, nextContext)
    }

  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

}

case class SequenceEval[A](s: Iterable[Eval[A]]) extends DerivedEval[Iterable[A]] {

  override def eval(context: Context, oldValue: Option[Iterable[A]]): (EvalResult[Iterable[A]], Context) = {
    val z: (Either[InputRequest, List[A]], Context) = (Right(List()), context)
    val (intermediateResult, finalContext) = s.foldLeft(z) { case ((r, ctx), m) =>
      val (newResult, newContext) = ctx.evaluate(m)
      (combine(r, newResult), newContext)
    }
    val finalEvalResult = intermediateResult match {
      case Left(ir) => ir
      case Right(vv) => Value(vv.reverse)
    }
    (finalEvalResult, finalContext)
  }

  private def combine(acc: Either[InputRequest, List[A]], result: EvalResult[A]) =
    (acc, result) match {
      case (Left(ir), Value(_)) => Left(ir)
      case (Left(ir1), ir2: InputRequest) => Left(ir1.combine(ir2))
      case (Right(vv), Value(v)) => Right(v :: vv)
      case (Right(_), ir: InputRequest) => Left(ir)
    }

  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

}

case class Map2Eval[A, B, C](mA: Eval[A], mB: Eval[B])(f: (A, B) => C) extends DerivedEval[C] {

  private val s = sequence(List(mA, mB)).map { ll =>
    val a = ll.head.asInstanceOf[A]
    val b = ll.tail.head.asInstanceOf[B]
    f(a, b)
  }

  override def eval(context: Context, oldValue: Option[C]): (EvalResult[C], Context) = context.evaluate(s)

  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

}

object Eval {

  def sequence[A](s: Iterable[Eval[A]]): Eval[Iterable[A]] = SequenceEval(s)

  def map2[A, B, C](mA: Eval[A], mB: Eval[B])(f: (A, B) => C): Eval[C] = Map2Eval(mA, mB)(f)

  def unit[A](v: A)(implicit tag: ClassTag[A]): Eval[A] = Constant[A](v)

  def flatten[M](m: Eval[Eval[M]]): Eval[M] = FlattenedEval(m)

}


sealed trait BaseEval[M] extends Eval[M]


case class Constant[M](constant: M)(implicit val tag: ClassTag[M]) extends BaseEval[M] {
  override def equals(obj: Any): Boolean = obj match {
    case other: Constant[M] => constant == other.constant && tag == other.tag
    case _ => false
  }

  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

}


case class InputEval[M](input: Input[M]) extends BaseEval[M] {
  override val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)
}


trait DerivedEval[M] extends Eval[M] {
  def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context)
}


case class IncrementalFoldEval[I, IV <: IncrementalValue[I, IV], T](
  baseEval: Eval[IV],
  fold: IncrementalFold[I, IV, T],
) extends DerivedEval[T] {

  private val foldStateEval = new DerivedEval[IncrementalFold.State[I, IV, T]] {
    override def eval(
      context: Context,
      oldValue: Option[IncrementalFold.State[I, IV, T]],
    ): (EvalResult[IncrementalFold.State[I, IV, T]], Context) = {
      val (evalResult, ctx) = context.evaluate(baseEval)
      val newEr = evalResult match {
        case Value(x) =>
          if (oldValue.isEmpty) Value(fold.fold(x))
          else Value(oldValue.get.update(x))
        case ir: InputRequest => ir
      }
      (newEr, ctx)
    }
  }

  override def eval(context: Context, oldValue: Option[T]): (EvalResult[T], Context) = {
    val (er, c) = context.evaluate(foldStateEval)
    (er.map(_.value), c)
  }

}


trait CaseEval[M] extends DerivedEval[M] {

  protected def baseEval: Eval[M]

  final override def eval(context: Context, oldValue: Option[M]): (EvalResult[M], Context) =
    context.evaluate(baseEval)

}