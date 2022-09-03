package io.liquirium.util

import com.typesafe.scalalogging

trait Logger {

  def info(msg: String): Unit

  def warn(msg: String): Unit

  def warn(msg: String, throwable: Throwable): Unit

  def debug(msg: String): Unit

  def error(msg: String): Unit

  def error(msg: String, throwable: Throwable): Unit

}

object Logger {

  def apply(name: String): Logger = new LoggerImpl(name)

  private class LoggerImpl(name: String) extends Logger {

    private val logger = scalalogging.Logger(name)

    def info(msg: String) = logger.info(msg)

    def warn(msg: String) = logger.warn(msg)

    def warn(msg: String, throwable: Throwable) = logger.warn(msg, throwable)

    def debug(msg: String) = logger.debug(msg)

    def error(msg: String) = logger.error(msg)

    def error(msg: String, throwable: Throwable) = logger.error(msg, throwable)

  }

}


