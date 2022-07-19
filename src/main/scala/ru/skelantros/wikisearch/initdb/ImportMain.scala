package ru.skelantros.wikisearch.initdb

import java.io.File

import cats.effect.kernel.{Resource, Sync}
import cats.effect.{ExitCode, IO, IOApp}
import ru.skelantros.wikisearch.Quote
import io.circe.parser.decode

import scala.io.Source

object ImportMain extends IOApp {
  def fileResource[F[_] : Sync](f: File): Resource[F, Source] =
    Resource.fromAutoCloseable(Sync[F].pure(Source.fromFile(f, "UTF8")))

  def parseJson(s: String): Either[io.circe.Error, Quote] = decode[Quote](s)

  def parseJsonsFromSource(src: Source): Seq[Quote] =
    src.getLines().toSeq.map(parseJson).collect {
      case Right(x) => x
    }

  def parseJsonsFromFile[F[_] : Sync](f: File): F[Seq[Quote]] =
    fileResource(f).use(s => Sync[F].pure(parseJsonsFromSource(s)))

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- if(args.length < 1) IO.raiseError(new IllegalArgumentException("Source file required."))
           else IO.unit
      srcFile = new File(args(0))
      jsons <- parseJsonsFromFile[IO](srcFile)
      uniqueQuotes = jsons.distinctBy(_.title.toLowerCase)
      _ <- IO.println(uniqueQuotes.size)
      _ <- ImportDoobie(uniqueQuotes)
    } yield ExitCode.Success
}
