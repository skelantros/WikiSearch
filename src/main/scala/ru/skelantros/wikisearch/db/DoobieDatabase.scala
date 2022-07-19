package ru.skelantros.wikisearch.db

import cats.effect.MonadCancelThrow
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import ru.skelantros.wikisearch.Article
import doobie.implicits._
import cats.implicits._
import DoobieQueries._
import Database._

// Реализация Database[F] на основе БД под управлением Postgres
class DoobieDatabase[F[_] : MonadCancelThrow](implicit val transactor: Transactor[F]) extends Database[F] {
  // Функция, преобразующая ConnectionIO в итоговые значения типа Result
  private def processConnection[A, B](behavior: A => Result[B])(x: ConnectionIO[A]): F[Result[B]] =
    x.attempt.map {
      case Left(t) => Result.thr[B](t)
      case Right(x) => behavior(x)
    }.transact(transactor)

  // Запись о статье (может отсутствовать)
  private def articleNoteConn(title: String): ConnectionIO[Option[ArticleNote]] =
    articleByTitleQuery(title).option

  override def findArticle(title: String): F[Result[Article]] =
    processConnection[Option[Article], Article] {
      case None => Result.mistake[Article](s"Article with title '$title' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        articleBase <- articleNoteConn(title)
        categories <- categoriesOfArticleQuery(title).to[List]
        auxTexts <- auxTextsOfArticleQuery(title).to[List]
      } yield articleBase.map(_.toArticle(categories, auxTexts))
    )

  override def articlesByCategory(category: String): F[Result[Seq[Article]]] =
    processConnection[Seq[Article], Seq[Article]](Result(_))(
      for {
        articlesBase <- articlesByCategoryQuery(category).to[List]
        categories <- articlesBase.map(x => categoriesOfArticleQuery(x.title).to[List]).sequence
        auxTexts <- articlesBase.map(x => auxTextsOfArticleQuery(x.title).to[List]).sequence
      } yield articlesBase.indices.map(i => articlesBase(i).toArticle(categories(i), auxTexts(i)))
    )

  // Транзакция, возвращающая ИД категории в БД (если категории нет в справочнике, она ее создает)
  private def findOrCreateCategoryConn(category: String): ConnectionIO[Int] =
    for {
      curIdOpt <- idOfCategoryQuery(category).option
      res <- curIdOpt.fold(createCategoryQuery(category).withUniqueGeneratedKeys[Int]("id"))(_.pure[ConnectionIO])
    } yield res

  // Транзакция, обновляющая существующую статью (т.е. существует строка ArticleNote) на основе объекта ArticleUpdate
  private def updateExistingArticleConn(update: ArticleUpdate, note: ArticleNote): ConnectionIO[Article] =
    for {
      _ <- updateArticleQuery(update).run
      catsIds <- update.categories.fold(Seq[Int]().pure[ConnectionIO])(_.map(findOrCreateCategoryConn).sequence)
      _ <- if(catsIds.nonEmpty) updateArticleCategoryQueries(note.id, catsIds).map(_.run).sequence else ().pure[ConnectionIO]
      _ <- update.auxiliaryText.fold(().pure[ConnectionIO])(updateArticleTextQueries(note.id, _).map(_.run).sequence.void)

      updatedTitle = update.newTitle.getOrElse(update.title)
      updatedArticleBase <- articleByTitleQuery(updatedTitle).unique
      updatedCategories <- categoriesOfArticleQuery(updatedTitle).to[List]
      updatedAuxTexts <- auxTextsOfArticleQuery(updatedTitle).to[List]
    } yield updatedArticleBase.toArticle(updatedCategories, updatedAuxTexts)

  override def updateArticle(update: ArticleUpdate): F[Result[Article]] =
    processConnection[Option[Article], Article] {
      case None => Result.mistake[Article](s"Article with title '${update.title}' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        articleNote <- articleNoteConn(update.title)
        res <- articleNote.map(updateExistingArticleConn(update, _)).sequence
        _ <- res.map(x => updateArticleTimestampQuery(x.title).run).sequence
      } yield res
    )

  override def removeArticle(title: String): F[Result[Article]] =
    processConnection[Option[Article], Article] {
      case None => Result.mistake[Article](s"Article with title '$title' is not found.")
      case Some(x) => Result(x)
    }(
      for {
        articleNote <- articleNoteConn(title)
        auxTexts <- auxTextsOfArticleQuery(title).to[Seq]
        categories <- categoriesOfArticleQuery(title).to[Seq]
        _ <- articleNote.map(x => deleteArticleQuery(x.title).run).sequence
      } yield articleNote.map(_.toArticle(categories, auxTexts))
    )
  override def createArticle(create: ArticleCreate): F[Result[Article]] = {
    val ArticleCreate(title, auxText, cats, wiki, lang) = create

    processConnection[Article, Article](Result(_))(
      for {
        id <- createArticleQuery(title, wiki, lang).withUniqueGeneratedKeys[Int]("id")
        _ <- updateArticleTextQueries(id, auxText).map(_.run).sequence
        catsIds <- cats.map(findOrCreateCategoryConn).sequence
        _ <- updateArticleCategoryQueries(id, catsIds).map(_.run).sequence

        articleBase <- articleByTitleQuery(title).unique
      } yield articleBase.toArticle(cats, auxText)
    )
  }

  override def categories: F[Result[Seq[String]]] =
    processConnection[Seq[String], Seq[String]](Result(_))(categoriesQuery.to[Seq])

  override def categoriesStats: F[Result[Seq[CategoryStats]]] =
    processConnection[Seq[CategoryStats], Seq[CategoryStats]](Result(_))(categoriesStatsQuery.to[Seq])
}