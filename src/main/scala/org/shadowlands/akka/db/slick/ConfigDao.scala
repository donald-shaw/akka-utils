package org.shadowlands.akka.db.slick

import java.sql.Timestamp
import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import org.shadowlands.akka.errors.{EitherErr, Err}
import Database._


object ConfigDao {
  
  case class ConfigEntry(key: String, value: String)
  
  class ConfigSchemaV1(val dbc: DatabaseConfig[JdbcProfile]) {
    import dbc.driver.api._
    // Slick has its own internal ExecutionContext for managing the database queries.
    // This EC is used to combine the futures
    import ExecutionContext.Implicits.global

    val db = dbc.db
    
    class DbConfigs(tag: Tag) extends Table[ConfigEntry](tag, "Configs") {
      def key = column[String]("key", O.PrimaryKey)
      def value = column[String]("value")
      def * = (key, value) <> (ConfigEntry.tupled, ConfigEntry.unapply)
    }

    val db_configs = TableQuery[DbConfigs]
    
    case class ConfigEvent(id: Int, time: Timestamp, key: String, value: String, user: String)
        
    class DbConfigEvents(tag: Tag) extends Table[ConfigEvent](tag, "ConfigEvents") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def time = column[Timestamp]("time")
      def key = column[String]("key")
      def value = column[String]("value")
      def user = column[String]("user")
      def * = (id, time, key, value, user) <> (ConfigEvent.tupled, ConfigEvent.unapply)
    }

    val db_config_events = TableQuery[DbConfigEvents]
    
    def getConfigQ(key: String) = db_configs.filter(_.key === key).result
    def getConfig(key: String): Future[EitherErr[Option[ConfigEntry]]] = runFirst(db, getConfigQ(key))
      
    def getAll: Future[EitherErr[Seq[ConfigEntry]]] = run(db, db_configs.result)

    def writeConfig(key: String, value: String): Future[Option[Err]] = updateConfig(key, value)
      
    def updateConfigQ(key: String, value: String, user: String) = 
      DBIO.seq(
        db_configs.insertOrUpdate(ConfigEntry(key, value)),
        db_config_events += ConfigEvent(0, toTimestamp(ZonedDateTime.now), key, value, user))
        
    def updateConfig(key: String, value: String, user: String = ""): Future[Option[Err]] = 
      runOptionErr( db, updateConfigQ(key, value, user) )
   
    def deleteConfigQ(key: String, user: String) = 
      DBIO.seq(
        db_configs.filter(_.key === key).delete,
        db_config_events += ConfigEvent(0, toTimestamp(ZonedDateTime.now), key, "** DELETED **", user))
        
    def deleteConfig(key: String, user: String = ""): Future[Option[Err]] = runOptionErr( db,  deleteConfigQ(key, user))

    def allSchema = db_configs.schema ++ db_config_events.schema
    
    def create = allSchema.create
    def drop = allSchema.drop
  }
  
  def dao(dbc: DatabaseConfig[JdbcProfile]) = new ConfigSchemaV1(dbc)

}
