package org.shadowlands.akka.config

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import akka.event.{EventStream, LoggingReceive}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._

import org.shadowlands.akka.config.ConfigMessages._

class ConfigFetchActor[T](cfg_actor: ActorRef, ev_stream: EventStream, cfg_keys: Set[String],
                          fn: ConfigMap => T = identity[ConfigMap] _) extends Actor with ActorLogging with Stash {
  import context.dispatcher

  override def preStart {
    implicit val timeout = Timeout(5 seconds)

    (cfg_actor ? GetValues(cfg_keys)).mapTo[ConfigMap] pipeTo self

    ev_stream.subscribe(self, classOf[ConfigUpdate])
  }

  def receive = stashUpdates orElse processCfg

  def stashUpdates = LoggingReceive {
    case upd: ConfigUpdate =>
      stash
  }

  protected def processCfg = LoggingReceive {
    case cfg: ConfigMap =>
      context.parent ! fn(cfg)
      context.become(withConfig(cfg))
      unstashAll
  }

  def withConfig(cfg: ConfigMap): Receive = LoggingReceive {
    case ConfigUpdate(k, opt_v, _) =>
      if (cfg_keys.contains(k) && cfg.kvs.get(k) != opt_v) {
        val new_cfg = ConfigMap(opt_v.map(v => cfg.kvs.updated(k, v)).getOrElse(cfg.kvs - k))
        context.parent ! fn(new_cfg)
        context.become(withConfig(new_cfg))
      }
    case ConfigRequest => sender ! fn(cfg)
  }

}
