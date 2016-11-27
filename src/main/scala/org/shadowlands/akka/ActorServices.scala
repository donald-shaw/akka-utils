package org.shadowlands.akka

import akka.actor.{ActorSystem, Scheduler}
import akka.event.EventStream

case class ActorServices(scheduler: Scheduler, event_stream: EventStream)

object ActorServices {
  def apply(system: ActorSystem): ActorServices = ActorServices(system.scheduler, system.eventStream)
}
