package io.liquirium.util

/**
 * A simple profiler that counts how often a method was called. Suggested by ChatGPT.
 */
object Profiler {

  private var counts: Map[String, Int] = Map()

  def count(methodName: String): Unit = synchronized {
    counts = counts + (methodName -> (counts.getOrElse(methodName, 0) + 1))
  }

  def getCount(methodName: String): Int = counts.getOrElse(methodName, 0)

  def reset(): Unit = synchronized {
    counts = Map()
  }

  def report(): Unit = {
    counts.foreach { case (method, count) =>
      println(s"Method $method was called $count times")
    }
  }

}

