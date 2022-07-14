package ru.skelantros.wikisearch

import java.sql.Timestamp

case class Quote(createTimestamp: Timestamp,
                 timestamp: Timestamp,
                 language: String,
                 wiki: String,
                 category: Seq[String],
                 title: String,
                 auxiliaryTest: Seq[String])