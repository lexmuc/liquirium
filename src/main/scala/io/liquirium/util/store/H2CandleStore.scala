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
      |  open DECIMAL(30, 10),
      |  close DECIMAL(30, 10),
      |  high DECIMAL(30, 10),
      |  low DECIMAL(30, 10),
      |  quoteVolume DECIMAL(30, 10),
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
             |  ${c.quoteVolume}
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

  override def deleteFrom(time: Instant): Future[Unit] =
    Future {
      connection.createStatement().execute(
        """DELETE FROM CANDLES WHERE startTimestamp >= """ + time.getEpochSecond.toString
      )
    }

  override def deleteBefore(time: Instant): Future[Unit] =
    Future {
      connection.createStatement().execute(
        """DELETE FROM CANDLES WHERE startTimestamp < """ + time.getEpochSecond.toString
      )
    }

  private def getFirstStart: Future[Option[Instant]] =
    Future {
      val rs = connection.createStatement().executeQuery(
        """SELECT startTimestamp FROM CANDLES ORDER BY startTimestamp ASC LIMIT 1"""
      )
      if (rs.next()) Some(Instant.ofEpochSecond(rs.getInt("startTimestamp"))) else None
    }

  private def getLastEnd: Future[Option[Instant]] =
    Future {
      val rs = connection.createStatement().executeQuery(
        """SELECT startTimestamp FROM CANDLES ORDER BY startTimestamp DESC LIMIT 1"""
      )
      if (rs.next()) Some(Instant.ofEpochSecond(rs.getInt("startTimestamp")) plus candleLength) else None
    }

  override def getFirstStartAndLastEnd: Future[Option[(Instant, Instant)]] =
    for {
      maybeFirstStart <- getFirstStart
      maybeLastEnd <- getLastEnd
    } yield {
      for {
        firstStart <- maybeFirstStart
        lastEnd <- maybeLastEnd
      } yield {
        (firstStart, lastEnd)
      }
    }

}
