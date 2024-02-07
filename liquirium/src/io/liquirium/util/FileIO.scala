package io.liquirium.util

import java.nio.file.Path

trait FileIO {

  def write(p: Path, s: String): Unit

  def read(p: Path): String

  def getFilesWithSuffix(p: Path, suffix: String): Iterable[Path]

}
