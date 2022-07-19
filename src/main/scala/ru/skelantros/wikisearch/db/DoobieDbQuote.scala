package ru.skelantros.wikisearch.db

import cats.effect.MonadCancelThrow
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import ru.skelantros.wikisearch.Quote
import doobie.implicits._
import cats.implicits._
import DoobieQueries._

class DoobieDbQuote[F[_] : MonadCancelThrow](implicit val transactor: Transactor[F]) extends DbQuote[F] {
  private def processConnection[A, B](behavior: A => Result[B])(x: ConnectionIO[A]): F[Result[B]] =
    x.attempt.map {
      case Left(t) => Result.thr[B](t)
      case Right(x) => behavior(x)
    }.transact(transactor)

  override def addQuote(quote: Quote): F[Result[Quote]] = ???

  override def findQuote(title: String): F[Result[Quote]] =
    processConnection[Option[Quote], Quote] {
      case None => Result.mistake[Quote](s"Quote with title '$title' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        quoteBase <- quoteByTitleQuery(title).option
        categories <- categoriesOfQuoteQuery(title).to[List]
        auxTexts <- auxTextsOfQuoteQuery(title).to[List]
      } yield quoteBase.map(_.toQuote(categories, auxTexts))
    )

  override def quotesByCategory(category: String): F[Result[Seq[Quote]]] =
    processConnection[Seq[Quote], Seq[Quote]](Result(_))(
      for {
        quotesBase <- quotesByCategoryQuery(category).to[List]
        categories <- quotesBase.map(x => categoriesOfQuoteQuery(x.title).to[List]).sequence
        auxTexts <- quotesBase.map(x => auxTextsOfQuoteQuery(x.title).to[List]).sequence
      } yield quotesBase.indices.map(i => quotesBase(i).toQuote(categories(i), auxTexts(i)))
    )

  override def updateQuote(quote: Quote): F[Result[Quote]] = ???

  override def removeQuote(quote: Quote): F[Result[Quote]] = ???

  override def removeQuote(title: String): F[Result[Quote]] = ???

  override def categories: F[Result[Seq[String]]] =
    processConnection[Seq[String], Seq[String]](Result(_))(categoriesQuery.to[Seq])

  override def categoriesStats: F[Result[Seq[DbQuote.CategoryStats]]] =
    processConnection[Seq[DbQuote.CategoryStats], Seq[DbQuote.CategoryStats]](Result(_))(categoriesStatsQuery.to[Seq])
}