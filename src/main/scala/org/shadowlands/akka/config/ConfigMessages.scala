package org.shadowlands.akka.config

import spray.json._

import org.shadowlands.akka.errors.Err

object ConfigMessages {
  case object GetBetTypes
  case object GetMysteryBets
  case class  GetNum(key: String)
  case class  GetString(key: String)
  case class  GetStringSeq(key: String)
  case class  GetValue(key: String)
  case class  GetValues(keys: Set[String])
  case object GetKeys

  case class ConfigResponse(key: String, value: Option[JsValue])
  case class ConfigMap(kvs: Map[String, JsValue]) {
    def getNum(k: String): Option[Long] = kvs.get(k).collect { case JsNumber(n) => n.toLong }
    def getNumOrElse(k: String, n: Long): Long = getNum(k).getOrElse(n)
    def getString(k: String): Option[String] = kvs.get(k).collect { case JsString(s) => s }
    def getStringOrElse(k: String, s: String): String = getString(k).getOrElse(s)
    def getStrings(k: String): IndexedSeq[String] = 
      kvs.get(k).collect { case JsArray(jvs) => jvs.collect { case JsString(s) => s } }.getOrElse(Vector())
    def getBoolean(k: String): Option[Boolean] = kvs.get(k).collect { case JsBoolean(b) => b }
    def getBooleanOrElse(k: String, b: Boolean): Boolean = getBoolean(k).getOrElse(b)
    }
  
  case class StringSeq(seq: IndexedSeq[String])
  
  case class ConfigUpdate(key: String, value: Option[JsValue], user: String)
  case class InternalConfigUpdate(key: String, value: Option[JsValue], user: String)
  case class ConfigUpdateResponse(key: String, error: Option[Err] = None)
  case object ConfigRequest
}
