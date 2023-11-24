package io.liquirium.util

trait NonceGenerator {

  def next(): Long

}
