package io.liquirium.util

case class AbsoluteQuantity(value: BigDecimal) {
  if (value.signum == -1) throw new RuntimeException("absolute quantity value may not be negative")
}
