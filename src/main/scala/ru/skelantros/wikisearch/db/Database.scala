package ru.skelantros.wikisearch.db

import ru.skelantros.wikisearch.Article
import ru.skelantros.wikisearch.db.Database._

trait Database[F[_]] {
  // this method is expected to be case-insensitive
  def findArticle(title: String): F[Result[Article]]
  def articlesByCategory(category: String): F[Result[Seq[Article]]]
  def updateArticle(update: ArticleUpdate): F[Result[Article]]
  // this method is expected to be case-insensitive
  def removeArticle(title: String): F[Result[Article]]
  def createArticle(create: ArticleCreate): F[Result[Article]]

  def categories: F[Result[Seq[String]]]
  def categoriesStats: F[Result[Seq[CategoryStats]]]
}

object Database {
  case class CategoryStats(name: String, count: Int)
  case class ArticleUpdate(title: String, newTitle: Option[String], auxiliaryText: Option[Seq[String]], categories: Option[Seq[String]], wiki: Option[String], language: Option[String])
  case class ArticleCreate(title: String, auxiliaryText: Seq[String], categories: Seq[String], wiki: String, language: String)
}