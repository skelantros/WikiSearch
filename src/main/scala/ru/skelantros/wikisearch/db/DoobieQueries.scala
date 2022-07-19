package ru.skelantros.wikisearch.db

import java.sql.Timestamp

import doobie.implicits._
import doobie._
import doobie.implicits.javasql._
import ru.skelantros.wikisearch.Quote

object DoobieQueries {
  type QuoteNote = (Int, String, Timestamp, Timestamp, String, String)
  implicit class QuoteNoteOps(x: QuoteNote) {
    def toQuote(categories: Seq[String], auxTexts: Seq[String]): Quote =
      Quote(x._3, x._4, x._6, x._5, categories, x._2, auxTexts)
    def title: String = x._2
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
}