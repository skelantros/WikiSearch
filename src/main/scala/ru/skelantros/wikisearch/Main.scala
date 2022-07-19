package ru.skelantros.wikisearch

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import com.comcast.ip4s._
import cats.implicits._
import doobie.util.transactor.Transactor
import org.http4s.server.middleware.Logger
import ru.skelantros.wikisearch.db.{DbQuote, DoobieDbQuote}

object Main extends IOApp {
  private val port = port"8080"
  private val host = ipv4"127.0.0.1"

  implicit val transactor: Transactor[IO] = TransactorImpl[IO]
  implicit val database: DbQuote[IO] = new DoobieDbQuote
  val services = new QuoteServices[IO]

  private val app: HttpApp[IO] =
    (
      services.quoteByTitle <+> services.quotesByCategory <+> services.categoryStats <+>
      services.updateQuote
    ).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
    .default[IO]
    .withHost(host)
    .withPort(port)
    .withHttpApp(Logger.httpApp(true, true)(app))
    .build
    .use(_ => IO.never)
    .as(ExitCode.Success)
}
