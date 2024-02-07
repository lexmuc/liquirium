package io.liquirium.util.helpers

import io.liquirium.util.FileIO

import java.nio.file.Path


class FakeFileIO(initialFileContents: Map[Path, String] = Map()) extends FileIO {

  private var _writes = Seq[(Path, String)]()

  def writes: Seq[(Path, String)] = _writes

  override def write(p: Path, s: String): Unit = _writes = _writes :+ (p, s)

  override def read(p: Path): String = initialFileContents(p)

  override def getFilesWithSuffix(p: Path, suffix: String): Iterable[Path] = ???

}
