package ru.skelantros.wikisearch

import java.sql.Timestamp
import java.text.SimpleDateFormat

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Quote(createTimestamp: Timestamp,
                 timestamp: Timestamp,
                 language: String,
                 wiki: String,
                 category: Seq[String],
                 title: String,
                 auxiliaryText: Seq[String])

object Quote {
  private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  private case class QuoteSnakeCase(create_timestamp: Timestamp,
                                    timestamp: Timestamp,
                                    language: String,
                                    wiki: String,
                                    category: Seq[String],
                                    title: String,
                                    auxiliary_text: Seq[String])

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder.decodeString.map(s => new Timestamp(formatter.parse(s).getTime))
  implicit val decoder: Decoder[Quote] = deriveDecoder[QuoteSnakeCase].map {
    case QuoteSnakeCase(ct, t, l, w, c, tit, at) => Quote(ct, t, l, w, c, tit, at)
  }
}