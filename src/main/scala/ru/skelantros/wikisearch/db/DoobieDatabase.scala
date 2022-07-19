package ru.skelantros.wikisearch.db

import cats.effect.MonadCancelThrow
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import ru.skelantros.wikisearch.Quote
import doobie.implicits._
import cats.implicits._
import DoobieQueries._
import Database._

class DoobieDatabase[F[_] : MonadCancelThrow](implicit val transactor: Transactor[F]) extends Database[F] {
  private def processConnection[A, B](behavior: A => Result[B])(x: ConnectionIO[A]): F[Result[B]] =
    x.attempt.map {
      case Left(t) => Result.thr[B](t)
      case Right(x) => behavior(x)
    }.transact(transactor)

  override def addQuote(quote: Quote): F[Result[Quote]] = ???

  private def quoteNoteConn(title: String): ConnectionIO[Option[QuoteNote]] =
    quoteByTitleQuery(title).option

  override def findQuote(title: String): F[Result[Quote]] =
    processConnection[Option[Quote], Quote] {
      case None => Result.mistake[Quote](s"Quote with title '$title' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        quoteBase <- quoteNoteConn(title)
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

  private def findOrCreateCategoryConn(category: String): ConnectionIO[Int] =
    for {
      curIdOpt <- idOfCategoryQuery(category).option
      res <- curIdOpt.fold(createCategoryQuery(category).withUniqueGeneratedKeys[Int]("id"))(_.pure[ConnectionIO])
    } yield res

  private def updateExistingQuoteConn(update: QuoteUpdate, note: QuoteNote): ConnectionIO[Quote] =
    for {
      _ <- updateQuoteQuery(update).run
      catsIds <- update.categories.fold(Seq[Int]().pure[ConnectionIO])(_.map(findOrCreateCategoryConn).sequence)
      _ <- if(catsIds.nonEmpty) updateQuoteCategoryQueries(note.id, catsIds).map(_.run).sequence else ().pure[ConnectionIO]
      _ <- update.auxiliaryText.fold(().pure[ConnectionIO])(updateQuoteTextQueries(note.id, _).map(_.run).sequence.void)

      updatedTitle = update.newTitle.getOrElse(update.title)
      updatedQuoteBase <- quoteByTitleQuery(updatedTitle).unique
      updatedCategories <- categoriesOfQuoteQuery(updatedTitle).to[List]
      updatedAuxTexts <- auxTextsOfQuoteQuery(updatedTitle).to[List]
    } yield updatedQuoteBase.toQuote(updatedCategories, updatedAuxTexts)

  override def updateQuote(update: QuoteUpdate): F[Result[Quote]] =
    processConnection[Option[Quote], Quote] {
      case None => Result.mistake[Quote](s"Quote with title '${update.title}' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        quoteNote <- quoteNoteConn(update.title)
        res <- quoteNote.map(updateExistingQuoteConn(update, _)).sequence
        _ <- res.map(x => updateQuoteTimestampQuery(x.title).run).sequence
      } yield res
    )

  override def removeQuote(title: String): F[Result[Quote]] =
    processConnection[Option[Quote], Quote] {
      case None => Result.mistake[Quote](s"Quote with title '$title' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        quoteNote <- quoteNoteConn(title)
        auxTexts <- auxTextsOfQuoteQuery(title).to[Seq]
        categories <- categoriesOfQuoteQuery(title).to[Seq]
        _ <- quoteNote.map(x => deleteQuoteQuery(x.title).run).sequence
      } yield quoteNote.map(_.toQuote(categories, auxTexts))
    )
  override def createQuote(create: QuoteCreate): F[Result[Quote]] = {
    val QuoteCreate(title, auxText, cats, wiki, lang) = create

    processConnection[Quote, Quote](Result(_))(
      for {
        id <- createQuoteQuery(title, wiki, lang).withUniqueGeneratedKeys[Int]("id")
        _ <- updateQuoteTextQueries(id, auxText).map(_.run).sequence
        catsIds <- cats.map(findOrCreateCategoryConn).sequence
        _ <- updateQuoteCategoryQueries(id, catsIds).map(_.run).sequence

        quoteBase <- quoteByTitleQuery(title).unique
      } yield quoteBase.toQuote(cats, auxText)
    )
  }

  override def categories: F[Result[Seq[String]]] =
    processConnection[Seq[String], Seq[String]](Result(_))(categoriesQuery.to[Seq])

  override def categoriesStats: F[Result[Seq[CategoryStats]]] =
    processConnection[Seq[CategoryStats], Seq[CategoryStats]](Result(_))(categoriesStatsQuery.to[Seq])
}