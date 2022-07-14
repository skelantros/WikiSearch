package ru.skelantros.wikisearch

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  val app: HttpApp[IO] = ???
  private val port = ???
  private val host = ???

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
    .default[IO]
    .withHost(host)
    .withPort(port)
    .withHttpApp(app)
    .build
    .use(_ => IO.never)
    .as(ExitCode.Success)
}
