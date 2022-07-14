package ru.skelantros.wikisearch

case class Quote(createTimestamp: String,
                 timestamp: String,
                 language: String,
                 wiki: String,
                 category: String,
                 title: String,
                 auxiliaryTest: Seq[String])