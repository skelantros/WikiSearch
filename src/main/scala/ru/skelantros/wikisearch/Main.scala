package ru.skelantros.wikisearch

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import com.comcast.ip4s._

object Main extends IOApp {
  private val port = port"8080"
  private val host = ipv4"127.0.0.1"

  private val app: HttpApp[IO] = ???

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
