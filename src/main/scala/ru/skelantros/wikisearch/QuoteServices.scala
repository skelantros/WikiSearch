package ru.skelantros.wikisearch

import cats.Monad
import cats.data.ValidatedNel
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, ParseFailure, Response}
import org.http4s.dsl.Http4sDsl
import ru.skelantros.wikisearch.DbQuote.{Mistake, Result, Thr}
import cats.implicits._
import io.circe.Encoder
import io.circe.syntax._
import io.circe._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}

class QuoteServices[F[_] : Monad](implicit db: DbQuote[F]) {
  private val dsl = new Http4sDsl[F] {}
  import dsl._
  import QuoteServices._

  private val wikiRoot = Root / "wiki"

  type EntEnc[A] = EntityEncoder[F, A]
  private def response[A, B : EntEnc](result: Result[A])(f: A => B): F[Response[F]] =
    result.fold(
      {
        case Mistake(msg) => BadRequest(msg)
        case Thr(t) => InternalServerError(t.getMessage)
      },
      res => Ok(f(res))
    )

  private def prettyResponse[A : Encoder](isPretty: Boolean)(result: Result[A]) =
    response(result)(x => if(isPretty) x.asJson.toString else x.asJson.noSpaces)
  private def prettyResponse[A : Encoder](isPrettyValidated: Option[ValidatedNel[ParseFailure, Boolean]])(result: Result[A]): F[Response[F]] =
    isPrettyValidated.fold(prettyResponse(false)(result))(_.fold(
      _ => BadRequest("unable to parse isPretty query param"),
      prettyResponse(_)(result)
    ))

  lazy val quoteByTitle = HttpRoutes.of[F] {
    case GET -> wikiRoot / title :? PrettyParam(isPretty) =>
      db.findQuote(title).flatMap(prettyResponse[Quote](isPretty))
  }

  lazy val quotesByCategory = HttpRoutes.of[F] {
    case GET -> wikiRoot :? CategoryParam(category) :? PrettyParam(isPretty) =>
      db.quotesByCategory(category).flatMap(prettyResponse[Seq[Quote]](isPretty))
  }
}

object QuoteServices {
  object PrettyParam extends OptionalValidatingQueryParamDecoderMatcher[Boolean]("pretty")
  object CategoryParam extends QueryParamDecoderMatcher[String]("category")

  private case class SimplifiedQuote(auxiliary_text: Seq[String], create_timestamp: Long, timestamp: Long)

  implicit lazy val jsonEncoder: Encoder[Quote] =
    Encoder[SimplifiedQuote].contramap(q => SimplifiedQuote(q.auxiliaryText, q.createTimestamp.getTime, q.timestamp.getTime))
  implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Quote] = jsonOf[F, Quote]
}