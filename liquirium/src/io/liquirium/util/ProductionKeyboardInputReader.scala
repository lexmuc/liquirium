package io.liquirium.util


class ProductionKeyboardInputReader extends KeyboardInputReader {

  private val lock = new Object()
  private var started = false

  override def start(handleLine: String => Unit): Unit = lock.synchronized {
    if (started) {
      throw new RuntimeException("Keyboard reader has already been started")
    }
    started = true
    new Thread(new InnerRunnable(handleLine)).start()
  }

  private class InnerRunnable(handleLine: String => Unit) extends Runnable {

    def run(): Unit = {
      println("starting to read keyboard input")
      while (true) {
        try {
          val line = scala.io.StdIn.readLine()
          handleLine(line);
        }
        catch {
          case e: Throwable => println("failed to read from console: " + e)
        }
      }
    }

  }

}

