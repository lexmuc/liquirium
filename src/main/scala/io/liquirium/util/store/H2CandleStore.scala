package io.liquirium.util.store

import io.liquirium.core.Candle

import java.sql.{Connection, ResultSet}
import java.time.{Duration, Instant}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}

class H2CandleStore(
  connection: Connection,
  val candleLength: Duration,
)(
  implicit val ec: ExecutionContext,
) extends CandleStore {

  connection.createStatement().execute(
    """CREATE TABLE IF NOT EXISTS CANDLES (
      |  startTimestamp INT,
      |  open DECIMAL,
      |  close DECIMAL,
      |  high DECIMAL,
      |  low DECIMAL,
      |  quoteVolume DECIMAL,
      |  PRIMARY KEY (startTimestamp)
      |)""".stripMargin)

  override def add(candles: Iterable[Candle]): Future[Unit] =
    Future {
      candles.foreach { c =>
        connection.createStatement().execute(
          s"""
             |MERGE INTO CANDLES VALUES(
             |  ${c.startTime.getEpochSecond},
             |  ${c.open},
             |  ${c.close},
             |  ${c.high},
             |  ${c.low},
             |  ${c.quoteVolume},
             |)
        """.stripMargin)
      }
    }

  override def get(
    from: Option[Instant] = None,
    until: Option[Instant] = None,
  ): Future[Iterable[Candle]] = Future {
    val optStartCondition = from.map(i => "startTimestamp >= " + i.getEpochSecond.toString)
    val optEndCondition = until.map(i => "startTimestamp < " + i.getEpochSecond.toString)
    val conditions = (optStartCondition ++ optEndCondition).mkString(" AND ")
    val whereClause = if (conditions.isEmpty) "" else "WHERE " + conditions
    val q = s"SELECT * FROM CANDLES $whereClause ORDER BY startTimestamp ASC"
    val rs = connection.createStatement().executeQuery(q)
    read(rs)
  }

  private def read(rs: ResultSet): Vector[Candle] = {
    val buf = new VectorBuilder[Candle]
    while (rs.next()) {
      buf += Candle(
        startTime = Instant.ofEpochSecond(rs.getInt("startTimestamp")),
        length = candleLength,
        open = rs.getBigDecimal("open").doubleValue,
        close = rs.getBigDecimal("close").doubleValue,
        high = rs.getBigDecimal("high").doubleValue,
        low = rs.getBigDecimal("low").doubleValue,
        quoteVolume = rs.getBigDecimal("quoteVolume").doubleValue
      )
    }
    buf.result()
  }

  override def clear(): Unit = {
    connection.createStatement().execute("""DELETE FROM CANDLES""")
  }

  override def deleteFrom(start: Instant): Future[Unit] =
    Future {
      connection.createStatement().execute(
        """DELETE FROM CANDLES WHERE startTimestamp >= """ + start.getEpochSecond.toString
      )
    }

}
