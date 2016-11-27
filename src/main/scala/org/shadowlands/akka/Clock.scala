package org.shadowlands.akka

import java.time.ZonedDateTime

trait Clock {
  def zdt_now: ZonedDateTime
}

object RealClock extends Clock {
  def zdt_now = ZonedDateTime.now
}
