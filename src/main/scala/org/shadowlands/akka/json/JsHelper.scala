package org.shadowlands.akka.json

import spray.json._
import scala.{ PartialFunction => PF }

object JsHelper {

  implicit class RichJsValue(jsv: JsValue) {
    def asBigDecimal: Option[BigDecimal] = PF.condOpt(jsv) { case JsNumber(n) => n }
    def asLong: Option[Long] = asBigDecimal.map(_.toLong)
    def asInt: Option[Int] = asBigDecimal.map(_.toInt)
    def asDouble: Option[Double] = asBigDecimal.map(_.toDouble)
    def asString: Option[String] = PF.condOpt(jsv) { case JsString(s) => s }
    def asBoolean: Option[Boolean] = PF.condOpt(jsv) { case JsBoolean(b) => b }

    def asJsArray: Option[JsArray] = PF.condOpt(jsv) { case arr: JsArray => arr }
    // Slight diversion from naming pattern since asJsObject is method in JsValue which
    // throws an exception if value is not a JsObject
    def asJsObjectOpt: Option[JsObject] = PF.condOpt(jsv) { case obj: JsObject => obj }
     
    def toJsArray: JsArray = asJsArray.getOrElse(JsArray())
    def toIndexedSeq: IndexedSeq[JsValue] = asJsArray.map(_.elements).getOrElse(Vector())
    def toSeq: Seq[JsValue] = toIndexedSeq
     
    def toStringSeq: IndexedSeq[String] = toIndexedSeq.flatMap(_.asString)
    def toLongSeq: IndexedSeq[Long] = toIndexedSeq.flatMap(_.asLong)
    def toIntSeq: IndexedSeq[Int] = toIndexedSeq.flatMap(_.asInt)
    def toBooleanSeq: IndexedSeq[Boolean] = toIndexedSeq.flatMap(_.asBoolean)
   }

}
