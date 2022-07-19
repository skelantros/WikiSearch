package ru.skelantros.wikisearch

import cats.Monad
import cats.data.{EitherT, ValidatedNel}
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, ParseFailure, Response}
import org.http4s.dsl.Http4sDsl
import ru.skelantros.wikisearch.db.{Mistake, Result, Thr}
import cats.implicits._
import io.circe.Encoder
import io.circe.syntax._
import io.circe._
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import ru.skelantros.wikisearch.db.DbQuote
import ru.skelantros.wikisearch.db.DbQuote.{QuoteCreate, QuoteUpdate}

class QuoteServices[F[_] : Concurrent](implicit db: DbQuote[F]) {
  private val dsl = new Http4sDsl[F] {}
  import dsl._
  import QuoteServices._

  private val wikiRoot = Root / "wiki"

  type EntEnc[A] = EntityEncoder[F, A]
  private def response[A, B : EntEnc](f: A => B)(result: Result[A]): F[Response[F]] =
    result.fold(
      {
        case Mistake(msg) => BadRequest(msg)
        case Thr(t) => InternalServerError(t.getMessage)
      },
      res => Ok(f(res), "Content-Type" -> "application/json")
    )

  private def prettyResponse[A : Encoder](isPretty: Boolean)(result: Result[A]) =
    response[A, String](x => if(isPretty) x.asJson.toString else x.asJson.noSpaces)(result)
  private def prettyResponse[A : Encoder](isPrettyValidated: Option[ValidatedNel[ParseFailure, Boolean]])(result: Result[A]): F[Response[F]] =
    isPrettyValidated.fold(prettyResponse(false)(result))(_.fold(
      _ => BadRequest("unable to parse pretty query param"),
      prettyResponse(_)(result)
    ))

  lazy val quoteByTitle = HttpRoutes.of[F] {
    case GET -> wikiRoot / "quote" / title :? PrettyParam(isPretty) =>
      db.findQuote(title).flatMap(prettyResponse[Quote](isPretty))
  }

  lazy val quotesByCategory = HttpRoutes.of[F] {
    case GET -> wikiRoot / "quote" :? CategoryParam(category) :? PrettyParam(isPretty) =>
      db.quotesByCategory(category).flatMap(prettyResponse[Seq[Quote]](isPretty))
  }

  lazy val categoryStats = HttpRoutes.of[F] {
    case GET -> wikiRoot / "categories" =>
      db.categoriesStats.flatMap(response(identity))
  }

  lazy val updateQuote = HttpRoutes.of[F] {
    case req @ POST -> wikiRoot / "quote" / title =>
      for {
        updateInfo <- req.as[Update]
        dbResult <- db.updateQuote(updateInfo.toQuoteUpdate(title))
        resp <- response(identity[Quote])(dbResult)
      } yield resp
  }

  lazy val removeQuote = HttpRoutes.of[F] {
    case DELETE -> wikiRoot / "quote" / title =>
      db.removeQuote(title).flatMap(response(identity))
  }

  lazy val createQuote = HttpRoutes.of[F] {
    case req @ PUT -> wikiRoot / "quote" =>
      for {
        createInfo <- req.as[Create]
        dbResult <- db.createQuote(createInfo.toQuoteCreate)
        resp <- response(identity[Quote])(dbResult)
      } yield resp
  }
}

object QuoteServices {
  object PrettyParam extends OptionalValidatingQueryParamDecoderMatcher[Boolean]("pretty")
  object CategoryParam extends QueryParamDecoderMatcher[String]("category")

  private case class SimplifiedQuote(auxiliary_text: Seq[String], category: Seq[String], create_timestamp: Long, timestamp: Long)

  implicit lazy val jsonEncoder: Encoder[Quote] =
    Encoder[SimplifiedQuote].contramap(q => SimplifiedQuote(q.auxiliaryText, q.category, q.createTimestamp.getTime, q.timestamp.getTime))
  implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Quote] = jsonOf[F, Quote]

  case class Update(new_title: Option[String], auxiliary_text: Option[Seq[String]], category: Option[Seq[String]], language: Option[String], wiki: Option[String]) {
    def toQuoteUpdate(title: String): QuoteUpdate = QuoteUpdate(title, new_title, auxiliary_text, category, wiki, language)
  }
  object Update {
    implicit def encoder[F[_] : Concurrent]: EntityEncoder[F, Update] = jsonEncoderOf
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Update] = jsonOf
  }

  case class Create(title: String, auxiliary_text: Seq[String], category: Seq[String], language: String, wiki: String) {
    def toQuoteCreate: QuoteCreate = QuoteCreate(title, auxiliary_text, category, wiki, language)
  }
  object Create {
    implicit def encoder[F[_] : Concurrent]: EntityEncoder[F, Create] = jsonEncoderOf
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Create] = jsonOf
  }
}