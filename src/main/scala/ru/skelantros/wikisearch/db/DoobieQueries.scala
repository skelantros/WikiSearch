package ru.skelantros.wikisearch.db

import java.sql.Timestamp
import java.time.Instant

import doobie.implicits._
import doobie._
import doobie.implicits.javasql._
import doobie.util.log.LogHandler
import ru.skelantros.wikisearch.Quote
import ru.skelantros.wikisearch.db.DbQuote.{CategoryStats, QuoteUpdate}
import cats.implicits._

object DoobieQueries {
  private implicit val log: LogHandler = LogHandler.jdkLogHandler

  private def now: Timestamp = Timestamp.from(Instant.now())

  type QuoteNote = (Int, String, Timestamp, Timestamp, String, String)
  implicit class QuoteNoteOps(x: QuoteNote) {
    def toQuote(categories: Seq[String], auxTexts: Seq[String]): Quote =
      Quote(x._3, x._4, x._6, x._5, categories, x._2, auxTexts)
    def title: String = x._2
    def id: Int = x._1
  }

  def quoteByTitleQuery(title: String): Query0[QuoteNote] =
    sql"select * from quote where lower(title) = lower($title)".query

  def categoriesOfQuoteQuery(title: String): Query0[String] =
    sql"""
         select c.name
         from quote as q join quote_to_category as qc on qc.quote_id = q.id
         join category as c on c.id = qc.category_id
         where lower(q.title) = lower($title)
       """.query

  def auxTextsOfQuoteQuery(title: String): Query0[String] =
    sql"""
         select aux_text
         from quote as q join auxiliary_text as at on at.quote_id = q.id
         where lower(q.title) = lower($title)
         order by at.create_timestamp
       """.query

  def quotesByCategoryQuery(category: String): Query0[QuoteNote] =
    sql"""
         select q.id, q.title, q.create_timestamp, q.update_timestamp, q.wiki, q.language
         from quote as q join quote_to_category as qc on qc.quote_id = q.id
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
         select c.name, count(qtc.quote_id) from
         category as c join quote_to_category as qtc on c.id = qtc.category_id
         group by c.name
       """.query

  def updateQuoteQuery(update: QuoteUpdate): Update0 = {
    val QuoteUpdate(oldTitle, newTitle, _, _, wiki, language) = update

    val titleUpdate = newTitle.map(t => fr"title = $t")
    val wikiUpdate = wiki.map(w => fr"wiki = $w")
    val languageUpdate = language.map(l => fr"language = $l")
    val updates = (titleUpdate :: wikiUpdate :: languageUpdate :: Nil).collect {
      case Some(x) => x
    }

    if(updates.nonEmpty)
      sql"""
            update quote
            set ${updates.intercalate(fr",")}
            where lower(title) = lower($oldTitle)
         """.update
    else sql"update quote set title = '' where 0 = 1".update
  }

  def insertAuxTextsQuery(auxiliaryText: String, id: Int): Update0 =
      sql"""insert into auxiliary_text(quote_id, create_timestamp, aux_text)
             values ($id, $now, $auxiliaryText)
           """.update

  def updateQuoteTextQueries(id: Int, auxTexts: Seq[String]): Seq[Update0] =
    sql"delete from auxiliary_text where quote_id = $id".update +: auxTexts.map(insertAuxTextsQuery(_, id))


  def addCategoryQuery(category: String): Update0 =
    sql"insert into category(name) values ($category)".update

  def updateQuoteCategoryQueries(id: Int, categoriesIds: Seq[Int]): Seq[Update0] =
    sql"delete from quote_to_category where quote_id = $id".update +:
    categoriesIds.map(catId => sql"insert into quote_to_category(quote_id, category_id) values ($id, $catId)".update)

  def updateQuoteTimestampQuery(title: String): Update0 =
    sql"update quote set update_timestamp = $now where lower(title) = lower($title)".update
}