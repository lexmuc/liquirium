package io.liquirium.eval

object IncrementalFoldHelpers {

  implicit class IncrementalEval[E, IV <: IncrementalValue[E, IV]](m: Eval[IncrementalValue[E, IV]]) {

    def foldIncremental[F](startValue: IV => F)(step: (F, E) => F): Eval[F] =
      IncrementalFoldEval[E, IV, F](
        baseEval = m.asInstanceOf[Eval[IV]],
        fold = IncrementalFold[E, IV, F](startValue)(step)
      )

    def collectIncremental[F](pf: PartialFunction[E, F]): Eval[IncrementalSeq[F]] =
      IncrementalFoldEval[E, IV, IncrementalSeq[F]](
        baseEval = m.asInstanceOf[Eval[IV]],
        fold = IncrementalFold[E, IV, IncrementalSeq[F]](_ => IncrementalSeq.empty[F]) {
          case (acc, e) => if (pf.isDefinedAt(e)) acc.inc(pf(e)) else acc
        }
      )

    def mapIncremental[F](f: Function[E, F]): Eval[IncrementalSeq[F]] =
      m.collectIncremental { case e => f(e) }

    def filterIncremental(p: Function[E, Boolean]): Eval[IncrementalSeq[E]] =
      m.collectIncremental { case e if p(e) => e }

    def mergeFoldIncremental[E2, IV2 <: IncrementalValue[E2, IV2], F](other: Eval[IncrementalValue[E2, IV2]])(
      start: (IV, IV2) => F,
    )(
      stepLeft: (F, E) => F,
    )(
      stepRight: (F, E2) => F,
    ): Eval[F] =
      IncrementalFoldEval(
        baseEval = Eval.map2(m, other)(IncrementalTuple(_, _)),
        fold = IncrementalFold[Either[E, E2], IncrementalTuple[E, E2, IV, IV2], F](
          t => start(t._1, t._2)
        ) {
          case (agg, Left(e)) => stepLeft(agg, e)
          case (agg, Right(e)) => stepRight(agg, e)
        }
      )

  }

  implicit class IncrementalSeqEval[E](m: Eval[IncrementalSeq[E]]) {

    def groupByIncremental[K](getKey: E => K): Eval[IncrementalMap[K, IncrementalSeq[E]]] =
      IncrementalFoldEval[E, IncrementalSeq[E], IncrementalMap[K, IncrementalSeq[E]]](
        baseEval = m,
        fold = IncrementalFold[E, IncrementalSeq[E], IncrementalMap[K, IncrementalSeq[E]]](
          _ => IncrementalMap.empty[K, IncrementalSeq[E]]
        ) { case (acc, e) =>
          val key = getKey(e)
          val oldList = acc.mapValue.getOrElse(key, IncrementalSeq.empty[E])
          acc.update(key, oldList.inc(e))
        }
      )

  }

  implicit class IncrementalMapEval[K, V](m: Eval[IncrementalMap[K, V]]) {

    def filterValuesIncremental(predicate: V => Boolean): Eval[IncrementalMap[K, V]] =
      IncrementalFoldEval[(K, Option[V]), IncrementalMap[K, V], IncrementalMap[K, V]](
        baseEval = m,
        fold = IncrementalFold[(K, Option[V]), IncrementalMap[K, V], IncrementalMap[K, V]](
          (_: IncrementalMap[K, V]) => IncrementalMap.empty[K, V]
        ) {
          case (result, (k, Some(v))) if predicate(v) => result.update(k, v)
          case (result, (k, Some(v))) if !predicate(v) && result.mapValue.contains(k) => result.deleteKey(k)
          case (result, (k, None)) if result.mapValue.contains(k) => result.deleteKey(k)
          case (result, _) => result
        }
      )

  }

}
