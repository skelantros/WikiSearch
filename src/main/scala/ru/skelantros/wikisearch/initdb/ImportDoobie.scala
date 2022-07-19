package ru.skelantros.wikisearch.initdb

import java.sql.Timestamp
import java.time.Instant

import cats.effect.IO
import doobie.{ConnectionIO, Update0}
import ru.skelantros.wikisearch.{Article, TransactorImpl}
import doobie.implicits._
import cats.implicits._
import doobie.implicits.javasql._

object ImportDoobie {
  private val transactor = TransactorImpl[IO]

  private def insertArticleQuery(article: Article): Update0 = {
    val Article(ct, t, l, w, _, title, _) = article
    sql"""insert into quote(title, create_timestamp, update_timestamp, wiki, language)
          values ($title, $ct, $t, $w, $l)
       """.update
  }

  private def insertAuxTextsQuery(article: Article, id: Int): Seq[Update0] =
    article.auxiliaryText.map { text =>
      sql"""insert into auxiliary_text(quote_id, create_timestamp, aux_text)
             values ($id, ${Timestamp.from(Instant.now())}, $text)
           """.update
    }

  private def insertCatQuery(category: String): Update0 =
    sql"insert into category(name) values ($category)".update

  private def insertCatsOfArticleQuery(categories: Map[String, Int])(article: Article, id: Int): Seq[Update0] =
    article.category.map { cat =>
      sql"insert into quote_to_category(quote_id, category_id) values ($id, ${categories(cat)})".update
    }

  private def finalConnection(quotes: Seq[Article]): ConnectionIO[Unit] =
    for {
      qts <- quotes.pure[ConnectionIO]
      categories = qts.flatMap(_.category).distinct
      categoriesIds <- categories.map(insertCatQuery(_).withUniqueGeneratedKeys[Int]("id")).sequence
      categoriesMap = (categories zip categoriesIds).toMap
      articlesIds <- qts.map(insertArticleQuery(_).withUniqueGeneratedKeys[Int]("id")).sequence
      articlesWithIds = qts zip articlesIds
      _ <- articlesWithIds.flatMap((insertAuxTextsQuery _).tupled).map(_.run).sequence
      _ <- articlesWithIds.flatMap((insertCatsOfArticleQuery(categoriesMap) _).tupled).map(_.run).sequence
    } yield ()

  def apply(quotes: Seq[Article]): IO[Unit] = finalConnection(quotes).transact[IO](transactor)
}
