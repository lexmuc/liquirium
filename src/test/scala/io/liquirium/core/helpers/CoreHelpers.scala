package io.liquirium.core.helpers

import io.liquirium.core.Asset
import java.time.{Duration, Instant}


object CoreHelpers {

  def asset(n: Int): Asset = Asset("A" + n.toString)

  def asset(s: String): Asset = Asset(s)

  def dec(n: Int): BigDecimal = BigDecimal(n)

  def dec(s: String): BigDecimal = BigDecimal(s)

  case class TestFailure(s: String) extends RuntimeException(s)

  def ex(s: String): TestFailure = failure(s)

  def ex(n: Int): TestFailure = failure(n.toString)

  def failure(s: String): TestFailure = TestFailure(s)

  def sec(n: Long): Instant = Instant.ofEpochSecond(n)

  def secs(l: Long): Duration = Duration.ofSeconds(l)

  def milli(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)

  def millis(l: Long): Duration = Duration.ofMillis(l)

}