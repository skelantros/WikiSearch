package ru.skelantros.wikisearch

import ru.skelantros.wikisearch.DbQuote.Result

trait DbQuote[F[_]] {
  def addQuote(quote: Quote): F[Result[Quote]]
  def updateQuote(quote: Quote): F[Result[Quote]]
  def removeQuote(quote: Quote): F[Result[Boolean]]
  def removeQuote(title: String): F[Result[Boolean]]
}

object DbQuote {
  type Result[A] = Either[DbError, A]
  sealed trait DbError
  case class Mistake(msg: String) extends DbError
  case class Thr(t: Throwable) extends DbError
}