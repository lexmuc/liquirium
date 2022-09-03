package io.liquirium.connect.deribit

case class DeribitInstrument(name: String) {

  def numOfDashes: Int = name.count(_ == '-')

  def isFuture: Boolean = numOfDashes == 1

  def isOption: Boolean = numOfDashes == 3

}
