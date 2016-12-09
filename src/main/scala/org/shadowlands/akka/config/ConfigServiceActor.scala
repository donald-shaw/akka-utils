package org.shadowlands.akka.config

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import akka.event.LoggingReceive
import com.typesafe.config.{ConfigFactory, ConfigValue, ConfigValueType}
import java.util.{List => JList}
import org.shadowlands.akka.config.ConfigMessages._
import org.shadowlands.akka.db.slick.{ConfigDao, Database}
import org.shadowlands.akka.errors.Err

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try
import spray.json._

import org.shadowlands.akka.ActorServices
import org.shadowlands.akka.json.JsHelper._


class ConfigServiceActor(actor_services: ActorServices) extends Actor with ActorLogging with Stash {

  def convert(cv: ConfigValue): JsValue = cv.valueType match {
    case ConfigValueType.BOOLEAN => JsBoolean(Boolean.unbox(cv.unwrapped))
    case ConfigValueType.NUMBER => JsNumber(cv.unwrapped.asInstanceOf[Number].longValue)
    case ConfigValueType.STRING => JsString(cv.unwrapped.asInstanceOf[String])
    case ConfigValueType.LIST =>
      val l = cv.unwrapped.asInstanceOf[JList[Object]].asScala
      JsArray(l.collect {
        case e if e.isInstanceOf[String] => JsString(e.asInstanceOf[String])
        case e if e.isInstanceOf[Number] => JsNumber(e.asInstanceOf[Number].longValue)
        case e if e.isInstanceOf[Boolean] => JsBoolean(e.asInstanceOf[Boolean])
      }.toVector)
    case other => ???
  }

  val appFilter = (in: String) => {
    val regex = "^(game|bet|ticket|liability|default|ge|pay-tables|report|admin|promotions).*".r
    PartialFunction.cond(in) {
      case regex(_) => true
    }
  }

  val conf = ConfigFactory.load
  var curr_cfg: Map[String, JsValue] = {
    log.debug(s"Config Service: full cfg listing = $conf")
    val cfg = conf.entrySet.asScala.collect { case e if appFilter(e.getKey) => e.getKey -> convert(e.getValue) }.toMap
    log.debug(s"Config Service: initial cfg = $cfg")
    cfg
  }

  private def processDefaultConfigFiles(fileNames: Seq[String]): Unit = {
    fileNames.foreach { fileName =>
      val data = Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).getLines().mkString("\n")
      //log.debug(s"\nData from file '$fileName':\n$data\n")
      import spray.json._
      data.parseJson match {
        case JsArray(vals) =>
          vals.flatMap(_.asJsObjectOpt).foreach { obj =>
            for (k <- obj.fields.get("key").flatMap(_.asString);
                 jsv <- obj.fields.get("value")) {
              curr_cfg += k -> jsv
            }
          }
        case JsObject(fields) =>
          for (k <- fields.get("key").flatMap(_.asString);
               jsv <- fields.get("value")) {
            curr_cfg += k -> jsv
          }
        case _ => self ! Abort(Err("Configuration load error", Vector(s"Unable to process Json configuration data: $data")))
      }
    }
  }

  val dbc = Database.databaseConfig
  val dao = ConfigDao.dao(dbc)

  import context.dispatcher
  import spray.json._

  case class Abort(err: Err)

  case class UpdateAndReply(upd: ConfigUpdate, reply_to: ActorRef, msg: Any)

  def processDb {
    dao.getAll.map {
      case Left(err) =>
        self ! Abort(err)
      case Right(cfgs) =>
        log.debug("Config Service - 'processDb' - cfg entries: " + cfgs)
        val kvs = cfgs.flatMap { entry =>
          Try { entry.value.parseJson }.toOption.map(entry.key -> _)
        }
        log.debug("Config Service - 'processDb' - key-value pairs: " + kvs.map { case (k, v) => s"$k -> ${v.getClass.getCanonicalName} - $v" }.mkString(" , "))
        self ! ConfigMap(kvs.toMap)
    }
  }

  processDefaultConfigFiles(conf.getStringList("default.config.files").asScala)

  processDb

  def receive = waitConfig

  def waitConfig(): Receive = LoggingReceive {
    case ConfigMap(kv_map) =>
      curr_cfg = kv_map.toList.foldLeft(curr_cfg) { case (cfg, (key, value)) => cfg.updated(key, value) }
      unstashAll
      context.become(processing)

    case Abort(err) =>
      log.error(s"Configuration load error - aborting start-up. Error: $err")
      actor_services.event_stream.publish( Err(s"Configuration load error - aborting start-up. Cause: ${err.msg}", err.params, true) )

      Thread.sleep(500) // Wait half a sec to give chance for log msg to pass through
      context.system.terminate() // Shutdown the system!

    case other => stash
  }

  def processing = LoggingReceive {
    case Abort(err) =>
      throw new Error("Error getting config from DB: " + err)

//    case GetBetTypes =>
//      sender !
//        StringSeq(Vector(
//          "WIN",
//          "PLACE",
//          "BUNDLE",
//          "EXACTA",
//          "TRIFECTA",
//          "FIRST4",
//          "EACHWAY",
//          "QUINELLA"))
//
//    case GetMysteryBets =>
//      sender !
//        StringSeq(Vector(
//          /*"TRIFECTA_$3_1g",
//          "TRIFECTA_$5_2g",
//          "FIRST4_$10_2g" */))

    case GetKeys =>
      sender ! StringSeq(curr_cfg.keys.toVector)

    case GetNum(key) =>
      sender ! curr_cfg.get(key).flatMap(_.asLong)

    case GetString(key) =>
      sender ! curr_cfg.get(key).flatMap(_.asLong)

    case GetStringSeq(key) =>
      sender ! StringSeq(curr_cfg.getOrElse(key, JsNull).toStringSeq)

    case GetValue(key) =>
      sender ! ConfigResponse(key, curr_cfg.get(key))

    case GetValues(keys) =>
      sender ! ConfigMap(keys.toList.flatMap(k => curr_cfg.get(k).map(k -> _)).toMap)

    case UpdateAndReply(upd@ConfigUpdate(k, opt_v, _), reply_to, msg) =>
      curr_cfg = opt_v.map(curr_cfg.updated(k, _)).getOrElse(curr_cfg - k)
      actor_services.event_stream.publish(upd)
      reply_to ! ConfigUpdateResponse(k)

    case upd@ConfigUpdate(k, opt_v, user) =>
      val reply_to = sender
      val fut = opt_v.map { v => dao.updateConfig(k, v.prettyPrint, user) }.getOrElse(dao.deleteConfig(k))
      fut.foreach { opt_err =>
        val resp = ConfigUpdateResponse(k, opt_err)
        opt_err match {
          case None => self ! UpdateAndReply(upd, reply_to, resp)
          case Some(_) => reply_to ! resp
        }
      }

    case upd@InternalConfigUpdate(k, opt_v, user) =>
      val reply_to = sender
      val fut = opt_v.map { v => dao.updateConfig(k, v.prettyPrint, user) }.getOrElse(dao.deleteConfig(k))
      fut.foreach { opt_err =>
        val resp = ConfigUpdateResponse(k, opt_err)
        opt_err match {
          case None => self ! UpdateAndReply(ConfigUpdate(upd.key,upd.value,upd.user), reply_to, resp)
          case Some(_) => reply_to ! resp
        }
      }
  }
}
