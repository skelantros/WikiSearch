package ru.skelantros.wikisearch

// В этом пакете-объекте хранятся надстройки над Either, используемые для представления результатов работы с БД
package object db {
  type Result[A] = Either[DbError, A]
  object Result {
    def apply[A](x: A): Result[A] = Right(x)
    def mistake[A](msg: String): Result[A] = Left(Mistake(msg))
    def thr[A](t: Throwable): Result[A] = Left(Thr(t))
  }
  sealed trait DbError
  case class Mistake(msg: String) extends DbError
  case class Thr(t: Throwable) extends DbError
}
