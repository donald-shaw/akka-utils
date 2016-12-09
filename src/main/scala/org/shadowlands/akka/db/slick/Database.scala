package org.shadowlands.akka.db.slick

import com.typesafe.config.ConfigFactory
import java.sql
import java.sql.Timestamp
import java.time.{LocalDate, ZoneId, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import org.shadowlands.akka.errors.{EitherErr, Err}


object Database {
  lazy val databaseConfig =
    DatabaseConfig.forConfig[JdbcProfile](ConfigFactory.load.getString("db.config"))
    
  // This execution context is used for running the functions given to map after calling db.run
  // It is NOT used for running the database queries themselves as this is managed internally in Slick
  // All the functions here passed to map are trivial and non-blocking so the global Execution Context
  // is sufficient
  import ExecutionContext.Implicits.global

  def errRecover[T]: PartialFunction[Throwable, EitherErr[T]] = {
     case ex: Throwable => Left(dbErr(ex.getMessage))
  }
  
  def run[T](db: JdbcProfile#Backend#DatabaseDef, query: slick.dbio.DBIO[T]): Future[EitherErr[T]] =
    db.run(query).map(Right(_)) recover { errRecover[T] }

  def firstQ[T](query: slick.dbio.DBIO[Seq[T]]) = query.map(_.headOption)
  def runFirst[T](db: JdbcProfile#Backend#DatabaseDef, query: slick.dbio.DBIO[Seq[T]]): Future[EitherErr[Option[T]]] =
    run(db, firstQ(query))

  def runEitherErr[T](db: JdbcProfile#Backend#DatabaseDef, query: slick.dbio.DBIO[EitherErr[T]]): Future[EitherErr[T]] =
    db.run(query) recover { errRecover[T] }

  def runOptionErr(db: JdbcProfile#Backend#DatabaseDef, update: slick.dbio.DBIO[_]): Future[Option[Err]] =
    run(db, update).map(_.fold(err => Some(err), _ => None))
  
  def dbErr(reason: String): Err = Err("DB Error", Vector(reason))
  def dbErr(ex: Throwable): Err = dbErr(ex.getMessage)

  def toTimestamp(zdt: ZonedDateTime) = Timestamp.from(zdt.toInstant)

  def toZonedDateTime(ts: Timestamp): ZonedDateTime =
    ZonedDateTime.ofInstant(ts.toInstant, ZoneId.systemDefault)

  def toSqlDate(ld: LocalDate) = sql.Date.valueOf(ld)

  def toLocalDate(sd: sql.Date) = sd.toLocalDate

  def toLocalDate(ts: sql.Timestamp) = ts.toLocalDateTime.toLocalDate

    
    
}
