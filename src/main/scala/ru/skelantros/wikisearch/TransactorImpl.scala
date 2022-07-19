package ru.skelantros.wikisearch

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

object TransactorImpl {
  private val username = System.getenv("WIKISEARCH_USERNAME")
  private val password = System.getenv("WIKISEARCH_PASSWORD")
  private val dbName = System.getenv("WIKISEARCH_DATABASE")

  def apply[F[_] : Async]: Transactor[F] = Transactor.fromDriverManager[F] (
    "org.postgresql.Driver", s"jdbc:postgresql:$dbName", username, password
  )
}
