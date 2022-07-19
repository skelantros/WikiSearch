package ru.skelantros.wikisearch.db

import ru.skelantros.wikisearch.Quote
import ru.skelantros.wikisearch.db.DbQuote._

trait DbQuote[F[_]] {
  def addQuote(quote: Quote): F[Result[Quote]]
  // this method is expected to be case-insensitive
  def findQuote(title: String): F[Result[Quote]]
  def quotesByCategory(category: String): F[Result[Seq[Quote]]]
  def updateQuote(update: QuoteUpdate): F[Result[Quote]]
  // this method is expected to be case-insensitive
  def removeQuote(title: String): F[Result[Quote]]
  def createQuote(create: QuoteCreate): F[Result[Quote]]

  def categories: F[Result[Seq[String]]]
  def categoriesStats: F[Result[Seq[CategoryStats]]]
}

object DbQuote {
  case class CategoryStats(name: String, count: Int)
  case class QuoteUpdate(title: String, newTitle: Option[String], auxiliaryText: Option[Seq[String]], categories: Option[Seq[String]], wiki: Option[String], language: Option[String])
  case class QuoteCreate(title: String, auxiliaryText: Seq[String], categories: Seq[String], wiki: String, language: String)
}