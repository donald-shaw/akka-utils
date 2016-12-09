package org.shadowlands

import _root_.akka.actor.{ActorRef, ActorRefFactory, Props}
import java.time.ZonedDateTime

package object injections {
  // Injectable helpers that can be overridden in test code to enable easier testing

  trait Clock {
    def zdt_now: ZonedDateTime
  }

  object RealClock extends Clock {
    def zdt_now = ZonedDateTime.now
  }

  trait ChildProvider {
    def newChild(factory: ActorRefFactory, props: Props): ActorRef
  }

  object ChildProvider {
    val default = new ChildProvider {
      override def newChild(factory: ActorRefFactory, props: Props) = factory.actorOf(props)
    }
  }
}


