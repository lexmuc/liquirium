package io.liquirium.core.helpers

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.{Answer, OngoingStubbing}
import org.mockito.verification.VerificationMode
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.scalatest.mockito.MockitoSugar

import scala.reflect.ClassTag

trait TestWithMocks extends BasicTest with MockitoSugar {

  def *[T]: T = any()

  def verify[T](m: T): T = Mockito.verify(m)

  def verify[T](m: T, mode: VerificationMode): T = Mockito.verify(m, mode)

  def verifyNoMoreInteractions(mocks: AnyRef*): Unit = Mockito.verifyNoMoreInteractions(mocks: _*)

  def never: VerificationMode = Mockito.never()

  def times(n: Int): VerificationMode = Mockito.times(n)

  def eqTo[T](t: T): T = ArgumentMatchers.eq[T](t)

  def $[T](t: T): T = ArgumentMatchers.eq[T](t)

  def any[T](): T = ArgumentMatchers.any[T]()

  def reset[T](mocks: T*): Unit = Mockito.reset(mocks: _*)

  def argumentCaptor[T]()(implicit ct: ClassTag[T]): ArgumentCaptor[T] =
    ArgumentCaptor.forClass(ct.runtimeClass.asInstanceOf[Class[T]])

  // inspired by / taken from this project: https://github.com/markus1189/mockito-scala
  implicit class StubbingOps[A](mockee: => A) {

    def returns(r: A): OngoingStubbing[A] = Mockito.when(mockee).thenReturn(r)

    def returnsMulti(rr: Seq[A]): OngoingStubbing[A] = Mockito.when(mockee).thenReturn(rr.head, rr.tail: _*)

    def throws[E <: Throwable](e: E): OngoingStubbing[A] = Mockito.when(mockee).thenThrow(e)

    def answers(f: InvocationOnMock => A): OngoingStubbing[A] = {
      Mockito
        .when(mockee)
        .thenAnswer(new Answer[A] {
          override def answer(invocation: InvocationOnMock): A = f(invocation)
        })
    }

    def callsThrough(): OngoingStubbing[A] = Mockito.when(mockee).thenCallRealMethod()

  }

}
