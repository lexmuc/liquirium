package io.liquirium.util.akka

import akka.NotUsed
import akka.stream.scaladsl.Source

object StreamExtensions {

  // taken from here: https://github.com/akka/akka/issues/23044#issuecomment-378990945
  // required because the normal concat is eager and immediately signals demand for both sources
  implicit class SourceLazyOps[E, M](val src: Source[E, M]) {
    def concatLazily[M1](src2: => Source[E, M1]): Source[E, NotUsed] =
      Source(List(() => src, () => src2)).flatMapConcat(_ ())
  }

}
