package ru.skelantros.wikisearch

import java.sql.Timestamp
import java.text.SimpleDateFormat

import io.circe.Decoder
import io.circe.generic.semiauto._

case class Article(createTimestamp: Timestamp,
                   timestamp: Timestamp,
                   language: String,
                   wiki: String,
                   category: Seq[String],
                   title: String,
                   auxiliaryText: Seq[String])

object Article {
  private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  private case class ArticleSnakeCase(create_timestamp: Timestamp,
                                    timestamp: Timestamp,
                                    language: String,
                                    wiki: String,
                                    category: Seq[String],
                                    title: String,
                                    auxiliary_text: Seq[String])

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder.decodeString.map(s => new Timestamp(formatter.parse(s).getTime))
  implicit val decoder: Decoder[Article] = deriveDecoder[ArticleSnakeCase].map {
    case ArticleSnakeCase(ct, t, l, w, c, tit, at) => Article(ct, t, l, w, c, tit, at)
  }
}