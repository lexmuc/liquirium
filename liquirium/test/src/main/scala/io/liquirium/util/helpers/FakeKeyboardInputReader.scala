package io.liquirium.util.helpers

import io.liquirium.util.KeyboardInputReader

class FakeKeyboardInputReader extends KeyboardInputReader {

  private val lock = new Object()

  private var optHandler: Option[String => Unit] = None

  override def start(handleLine: String => Unit): Unit = lock.synchronized {
    optHandler = Some(handleLine)
  }

  def fakeLine(line: String): Unit = {
    optHandler.get.apply(line)
  }

  def isStarted: Boolean = lock.synchronized {
    return optHandler.isDefined
  }

}
