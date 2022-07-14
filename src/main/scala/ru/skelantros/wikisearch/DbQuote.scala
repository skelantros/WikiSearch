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
  object Result {
    def apply[A](x: A): Result[A] = Right(x)
    def mistake[A](msg: String): Result[A] = Left(Mistake(msg))
    def thr[A](t: Throwable): Result[A] = Left(Thr(t))
  }
  sealed trait DbError
  case class Mistake(msg: String) extends DbError
  case class Thr(t: Throwable) extends DbError
}