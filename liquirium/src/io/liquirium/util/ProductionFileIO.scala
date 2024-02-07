package io.liquirium.util


import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._

object ProductionFileIO extends FileIO {

  override def write(p: Path, s: String): Unit = Files.write(p, s.getBytes(StandardCharsets.UTF_8))

  override def read(p: Path): String = {
    val source = scala.io.Source.fromFile(p.toString)
    val result = source.mkString
    source.close()
    result
  }

  override def getFilesWithSuffix(p: Path, suffix: String): Iterable[Path] =
    Files.newDirectoryStream(p).asScala
      .filter(f => Files.isRegularFile(f) && f.getFileName.toString.endsWith(suffix))

}
