package ru.skelantros.wikisearch.db

import java.sql.Timestamp
import java.time.Instant

import doobie.implicits._
import doobie._
import doobie.implicits.javasql._
import doobie.util.log.LogHandler
import ru.skelantros.wikisearch.Article
import ru.skelantros.wikisearch.db.Database._
import cats.implicits._

object DoobieQueries {
  private implicit val log: LogHandler = LogHandler.jdkLogHandler

  private def now: Timestamp = Timestamp.from(Instant.now())

  type ArticleNote = (Int, String, Timestamp, Timestamp, String, String)
  implicit class ArticleNoteOps(x: ArticleNote) {
    def toArticle(categories: Seq[String], auxTexts: Seq[String]): Article =
      Article(x._3, x._4, x._6, x._5, categories, x._2, auxTexts)
    def title: String = x._2
    def id: Int = x._1
  }

  def articleByTitleQuery(title: String): Query0[ArticleNote] =
    sql"select * from article where lower(title) = lower($title)".query

  def categoriesOfArticleQuery(title: String): Query0[String] =
    sql"""
         select c.name
         from article as q join article_to_category as qc on qc.article_id = q.id
         join category as c on c.id = qc.category_id
         where lower(q.title) = lower($title)
       """.query

  def auxTextsOfArticleQuery(title: String): Query0[String] =
    sql"""
         select aux_text
         from article as q join auxiliary_text as at on at.article_id = q.id
         where lower(q.title) = lower($title)
         order by at.create_timestamp
       """.query

  def articlesByCategoryQuery(category: String): Query0[ArticleNote] =
    sql"""
         select q.id, q.title, q.create_timestamp, q.update_timestamp, q.wiki, q.language
         from article as q join article_to_category as qc on qc.article_id = q.id
         join category as c on c.id = qc.category_id
         where lower(c.name) = lower($category)
       """.query

  def categoriesQuery: Query0[String] =
    sql"select name from category".query

  def idOfCategoryQuery(category: String): Query0[Int] =
    sql"select id from category where lower(name) = lower($category)".query

  def createCategoryQuery(category: String): Update0 =
    sql"insert into category(name) values ($category)".update

  def categoriesStatsQuery: Query0[CategoryStats] =
    sql"""
         select c.name, count(qtc.article_id) from
         category as c join article_to_category as qtc on c.id = qtc.category_id
         group by c.name
       """.query

  def updateArticleQuery(update: ArticleUpdate): Update0 = {
    val ArticleUpdate(oldTitle, newTitle, _, _, wiki, language) = update

    val titleUpdate = newTitle.map(t => fr"title = $t")
    val wikiUpdate = wiki.map(w => fr"wiki = $w")
    val languageUpdate = language.map(l => fr"language = $l")
    val updates = (titleUpdate :: wikiUpdate :: languageUpdate :: Nil).collect {
      case Some(x) => x
    }

    if(updates.nonEmpty)
      sql"""
            update article
            set ${updates.intercalate(fr",")}
            where lower(title) = lower($oldTitle)
         """.update
    else sql"update article set title = '' where 0 = 1".update
  }

  def insertAuxTextsQuery(auxiliaryText: String, id: Int): Update0 =
      sql"""insert into auxiliary_text(article_id, create_timestamp, aux_text)
             values ($id, $now, $auxiliaryText)
           """.update

  def updateArticleTextQueries(id: Int, auxTexts: Seq[String]): Seq[Update0] =
    sql"delete from auxiliary_text where article_id = $id".update +: auxTexts.map(insertAuxTextsQuery(_, id))


  def addCategoryQuery(category: String): Update0 =
    sql"insert into category(name) values ($category)".update

  def updateArticleCategoryQueries(id: Int, categoriesIds: Seq[Int]): Seq[Update0] =
    sql"delete from article_to_category where article_id = $id".update +:
    categoriesIds.map(catId => sql"insert into article_to_category(article_id, category_id) values ($id, $catId)".update)

  def updateArticleTimestampQuery(title: String): Update0 =
    sql"update article set update_timestamp = $now where lower(title) = lower($title)".update

  def deleteArticleQuery(title: String): Update0 =
    sql"delete from article where title = $title".update

  def createArticleQuery(title: String, wiki: String, language: String): Update0 =
    sql"insert into article(title, wiki, language, create_timestamp, update_timestamp) values ($title, $wiki, $language, $now, $now)".update
}