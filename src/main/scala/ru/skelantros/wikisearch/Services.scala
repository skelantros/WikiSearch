package ru.skelantros.wikisearch

import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s._
import ru.skelantros.wikisearch.db.Database._
import ru.skelantros.wikisearch.db._

//noinspection TypeAnnotation
class Services[F[_] : Concurrent](implicit db: Database[F]) {
  private val dsl = new Http4sDsl[F] {}
  import Services._
  import dsl._

  type EntEnc[A] = EntityEncoder[F, A]
  // функция высшего порядка, создающая HTTP-response на основе результата вычислений из БД
  private def response[A, B : EntEnc](f: A => B)(result: Result[A]): F[Response[F]] =
    result.fold(
      {
        case Mistake(msg) => BadRequest(msg)
        case Thr(t) => InternalServerError(t.getMessage)
      },
      res => Ok(f(res), "Content-Type" -> "application/json")
    )

  // Функции для осуществления отформатированного вывода статей
  private def prettyResponse[A : Encoder](isPretty: Boolean)(result: Result[A]) =
    response[A, String](x => if(isPretty) x.asJson.toString else x.asJson.noSpaces)(result)
  private def prettyResponse[A : Encoder](isPrettyValidated: Option[ValidatedNel[ParseFailure, Boolean]])(result: Result[A]): F[Response[F]] =
    isPrettyValidated.fold(prettyResponse(false)(result))(_.fold(
      _ => BadRequest("unable to parse pretty query param"),
      prettyResponse(_)(result)
    ))

  val articleByTitle = HttpRoutes.of[F] {
    case GET -> Root / "wiki" / title :? PrettyParam(isPretty) =>
      db.findArticle(title).flatMap(prettyResponse[Article](isPretty))
  }

  val articlesByCategory = HttpRoutes.of[F] {
    case GET -> Root / "wiki" :? CategoryParam(category) :? PrettyParam(isPretty) =>
      db.articlesByCategory(category).flatMap(prettyResponse[Seq[Article]](isPretty))
  }

  val categoryStats = HttpRoutes.of[F] {
    case GET -> Root / "categories" =>
      db.categoriesStats.flatMap(response(identity))
  }

  val updateArticle = HttpRoutes.of[F] {
    case req @ POST -> Root / "wiki" / title =>
      for {
        updateInfo <- req.as[Update]
        dbResult <- db.updateArticle(updateInfo.toArticleUpdate(title))
        resp <- response(identity[Article])(dbResult)
      } yield resp
  }

  val removeArticle = HttpRoutes.of[F] {
    case DELETE -> Root / "wiki" / title =>
      db.removeArticle(title).flatMap(response(identity))
  }

  val createArticle = HttpRoutes.of[F] {
    case req @ PUT -> Root / "wiki" =>
      for {
        createInfo <- req.as[Create]
        dbResult <- db.createArticle(createInfo.toArticleCreate)
        resp <- response(identity[Article])(dbResult)
      } yield resp
  }
}

object Services {
  object PrettyParam extends OptionalValidatingQueryParamDecoderMatcher[Boolean]("pretty")
  object CategoryParam extends QueryParamDecoderMatcher[String]("category")

  private case class SimplifiedArticle(auxiliary_text: Seq[String], category: Seq[String], create_timestamp: Long, timestamp: Long)

  // Энкодер "полных" объектов Article в "неполные" объекты SimplifiedArticle, которые возвращаются в ответах HTTP
  implicit lazy val simplifiedJsonEncoder: Encoder[Article] =
    Encoder[SimplifiedArticle].contramap(q => SimplifiedArticle(q.auxiliaryText, q.category, q.createTimestamp.getTime, q.timestamp.getTime))

  implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Article] = jsonOf[F, Article]

  case class Update(new_title: Option[String], auxiliary_text: Option[Seq[String]], category: Option[Seq[String]], language: Option[String], wiki: Option[String]) {
    def toArticleUpdate(title: String): ArticleUpdate = ArticleUpdate(title, new_title, auxiliary_text, category, wiki, language)
  }
  object Update {
    implicit def encoder[F[_] : Concurrent]: EntityEncoder[F, Update] = jsonEncoderOf
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Update] = jsonOf
  }

  case class Create(title: String, auxiliary_text: Seq[String], category: Seq[String], language: String, wiki: String) {
    def toArticleCreate: ArticleCreate = ArticleCreate(title, auxiliary_text, category, wiki, language)
  }
  object Create {
    implicit def encoder[F[_] : Concurrent]: EntityEncoder[F, Create] = jsonEncoderOf
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, Create] = jsonOf
  }
}