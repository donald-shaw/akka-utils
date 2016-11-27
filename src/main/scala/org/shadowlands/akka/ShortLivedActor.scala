package org.shadowlands.akka

import akka.actor.{Actor, PoisonPill}
import scala.concurrent.duration.FiniteDuration

trait ShortLivedActor extends Actor { this: WithScheduler =>

  def max_time_to_live: FiniteDuration

  override def preStart {
    import context.dispatcher
    scheduler.scheduleOnce(max_time_to_live, self, PoisonPill)
  }

}
