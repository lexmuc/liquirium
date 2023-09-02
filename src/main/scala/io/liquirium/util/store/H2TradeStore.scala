package io.liquirium.util.store

import io.liquirium.connect.TradeBatch
import io.liquirium.core.{LedgerRef, Market, StringTradeId, Trade}

import java.sql.{Connection, ResultSet}
import java.time.Instant
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}

class H2TradeStore(
  connection: Connection,
  market: Market
)(
  implicit ec: ExecutionContext,
) extends TradeStore {

  connection.createStatement().execute(
    """CREATE TABLE IF NOT EXISTS TRADES (
      |  id VARCHAR PRIMARY KEY,
      |  orderId VARCHAR,
      |  quantity DECIMAL,
      |  price DECIMAL,
      |  timestamp BIGINT,
      |  fee1 DECIMAL,
      |  fee1Symbol VARCHAR,
      |  fee2 DECIMAL,
      |  fee2Symbol VARCHAR
      |);""".stripMargin)

  connection.createStatement().execute("CREATE INDEX IF NOT EXISTS timestamp_index ON TRADES(timestamp)")

  override def add(trades: Iterable[Trade]): Future[Unit] =
    Future {
      trades.foreach { t =>
        if (t.fees.size > 2) throw new RuntimeException("Only two fees supported per trade")
        if (t.fees.map(_._1.exchangeId).exists(_ != market.exchangeId))
          throw new RuntimeException("Fee ledgers of different exchanges cannot be stored")
        val feeSeq = t.fees.toIndexedSeq
        val fee1 = if (feeSeq.nonEmpty) feeSeq(0)._2 else BigDecimal(0)
        val fee1Symbol = if (feeSeq.nonEmpty) feeSeq(0)._1.symbol else ""
        val fee2 = if (feeSeq.size > 1) feeSeq(1)._2 else BigDecimal(0)
        val fee2Symbol = if (feeSeq.size > 1) feeSeq(1)._1.symbol else ""
        connection.createStatement().execute(
          s"""
             |MERGE INTO TRADES KEY(id) VALUES(
             |  '${ t.id }',
             |  '${ t.orderId getOrElse "" }',
             |  ${ t.quantity },
             |  ${ t.price },
             |  ${ t.time.toEpochMilli },
             |  $fee1,
             |  '$fee1Symbol',
             |  $fee2,
             |  '$fee2Symbol'
             |);
          """.stripMargin)
      }
    }

  override def get(
    from: Option[Instant] = None,
    until: Option[Instant] = None
  ): Future[TradeBatch] = Future {
    val optStartCondition = from.map(i => "timestamp >= " + i.toEpochMilli.toString)
    val optEndCondition = until.map(i => "timestamp < " + i.toEpochMilli.toString)
    val conditions = (optStartCondition ++ optEndCondition).mkString(" AND ")
    val whereClause = if (conditions.isEmpty) "" else "WHERE " + conditions
    val q = s"SELECT * FROM TRADES $whereClause ORDER BY timestamp, id ASC;"
    val rs = connection.createStatement().executeQuery(q)

    val entries = read(rs)
    TradeBatch(from.getOrElse(Instant.ofEpochSecond(0)), entries, None)
  }

  private def read(rs: ResultSet): Vector[Trade] = {
    val buf = new VectorBuilder[Trade]
    while (rs.next()) {
      buf += Trade(
        id = StringTradeId(rs.getString("id")),
        market = market,
        orderId = if (rs.getString("orderId") == "") None else Some(rs.getString("orderId")),
        quantity = rs.getBigDecimal("quantity"),
        price = rs.getBigDecimal("price"),
        fees = getFees(rs),
        time = Instant.ofEpochMilli(rs.getLong("timestamp"))
      )
    }
    buf.result()
  }

  private def getFees(rs: ResultSet): Seq[(LedgerRef, BigDecimal)] = {
    var res = Seq[(LedgerRef, BigDecimal)]()
    if (rs.getString("fee1Symbol").nonEmpty) {
      res = res :+ (LedgerRef(market.exchangeId, rs.getString("fee1Symbol")), BigDecimal(rs.getBigDecimal("fee1")))
    }
    if (rs.getString("fee2Symbol").nonEmpty) {
      res = res :+ (LedgerRef(market.exchangeId, rs.getString("fee2Symbol")), BigDecimal(rs.getBigDecimal("fee2")))
    }
    res
  }

  override def deleteFrom(time: Instant): Future[Unit] =
    Future {
      connection.createStatement().execute(s"""DELETE FROM TRADES WHERE timestamp >= ${time.toEpochMilli };""")
    }

}
