package io.liquirium.util

trait KeyboardInputReader {

  def start(handleLine: String => Unit): Unit

}