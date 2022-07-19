package ru.skelantros.wikisearch.db

import ru.skelantros.wikisearch.Quote

trait DbQuote[F[_]] {
  def addQuote(quote: Quote): F[Result[Quote]]
  // this method is expected to be case-insensitive
  def findQuote(title: String): F[Result[Quote]]
  def quotesByCategory(category: String): F[Result[Seq[Quote]]]
  def updateQuote(quote: Quote): F[Result[Quote]]
  def removeQuote(quote: Quote): F[Result[Quote]]
  // this method is expected to be case-insensitive
  def removeQuote(title: String): F[Result[Quote]]
}