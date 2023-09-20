package io.liquirium.util.store

import java.sql.{Connection, ResultSet}
import java.time.Instant
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}

class H2TradeSegmentStartStore(
  connection: Connection,
)(
  implicit ec: ExecutionContext,
) extends TradeSegmentStartStore {

  connection.createStatement().execute(
    """CREATE TABLE IF NOT EXISTS TRADE_SEGMENT_META (
      |  id VARCHAR PRIMARY KEY,
      |  `value` BIGINT
      |);""".stripMargin)

  override def saveStart(start: Instant): Future[Unit] = Future {
    connection.createStatement().execute(
      s"""
         |MERGE INTO TRADE_SEGMENT_META KEY(id) VALUES(
         |  'START',
         |  ${start.toEpochMilli}
         |);
      """.stripMargin)
  }

  override def readStart: Future[Option[Instant]] = Future {
    val rs = connection.createStatement().executeQuery(
      s"""
         |SELECT `value` FROM TRADE_SEGMENT_META WHERE id = 'START';
         |""".stripMargin
    )
    read(rs).headOption
  }

  private def read(rs: ResultSet): Vector[Instant] = {
    val buf = new VectorBuilder[Instant]
    while (rs.next()) {
      buf += Instant.ofEpochMilli(rs.getLong("value"))
    }
    buf.result()
  }

}
