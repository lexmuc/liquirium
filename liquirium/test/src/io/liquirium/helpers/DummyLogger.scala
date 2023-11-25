package io.liquirium.helpers

import io.liquirium.util.Logger

object DummyLogger extends Logger {
  override def warn(msg: String): Unit = ()

  override def warn(msg: String, throwable: Throwable): Unit = ()

  override def debug(msg: String): Unit = ()

  override def info(msg: String): Unit = ()

  override def error(msg: String, throwable: Throwable): Unit = ()

  override def error(msg: String): Unit = ()
}
