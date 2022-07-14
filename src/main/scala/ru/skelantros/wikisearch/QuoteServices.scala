package ru.skelantros.wikisearch

import cats.Monad
import cats.data.ValidatedNel
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, ParseFailure, Response}
import org.http4s.dsl.Http4sDsl
import ru.skelantros.wikisearch.DbQuote.{Mistake, Result, Thr}
import cats.implicits._
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}

class QuoteServices[F[_] : Monad](implicit db: DbQuote[F]) {
  private val dsl = new Http4sDsl[F] {}
  import dsl._
  import QuoteServices._

  private val wikiRoot = Root / "wiki"

  type Encoder[A] = EntityEncoder[F, A]
  private def response[A : Encoder](result: Result[A]): F[Response[F]] =
    result.fold(
      {
        case Mistake(msg) => BadRequest(msg)
        case Thr(t) => InternalServerError(t.getMessage)
      },
      res => Ok(res)
    )

  private def quoteResponse(isPretty: Boolean)(result: Result[Quote]): F[Response[F]] =
    response(result)(if(isPretty) ??? else ???)
  private def quoteResponse(isPrettyValidated: Option[ValidatedNel[ParseFailure, Boolean]])(result: Result[Quote]): F[Response[F]] =
    isPrettyValidated.fold(quoteResponse(false)(result))(_.fold(
      _ => BadRequest("unable to parse isPretty query param"),
      quoteResponse(_)(result)
    ))

  lazy val quoteByTitle = HttpRoutes.of[F] {
    case GET -> wikiRoot / title :? PrettyParam(isPretty) =>
      db.findQuote(title).flatMap(quoteResponse(isPretty))
  }

//  lazy val quotesByCategory = HttpRoutes.of[F] {
//    case GET -> wikiRoot :? CategoryParam(category) :? PrettyParam(isPretty) =>
//      db.quotesByCategory(category).flatMap(quoteResponse(isPretty))
//  }
}

object QuoteServices {
  object PrettyParam extends OptionalValidatingQueryParamDecoderMatcher[Boolean]("pretty")
  object CategoryParam extends QueryParamDecoderMatcher[String]("category")

  implicit def quoteEncoder[F[_]]: EntityEncoder[F, Quote] = ???
  implicit def quoteDecoder[F[_]]: EntityDecoder[F, Quote] = ???
}