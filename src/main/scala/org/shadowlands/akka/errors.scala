package org.shadowlands.akka

object errors {

  case class Err(msg: String, params: IndexedSeq[Any] = IndexedSeq(), critical: Boolean = false)

  type EitherErr[T] = Either[Err, T]
}
