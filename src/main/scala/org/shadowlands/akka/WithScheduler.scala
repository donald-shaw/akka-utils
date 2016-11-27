package org.shadowlands.akka

import akka.actor.Scheduler

trait WithScheduler {
  def scheduler: Scheduler
}
