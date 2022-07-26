name := "WikiSearch"

version := "0.1"

scalaVersion := "2.13.8"

lazy val http4sVersion = "0.23.13"
lazy val http4sDependencies = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion
)

lazy val circeVersion = "0.14.1"
lazy val circeDependencies = Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

lazy val doobieVersion = "1.0.0-RC2"
lazy val doobieDependencies = Seq(
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-scalatest"   % doobieVersion
)

lazy val scalatestVersion = "3.2.12"
lazy val scalatestDependencies = Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

libraryDependencies ++= http4sDependencies ++ doobieDependencies ++ scalatestDependencies ++ circeDependencies

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"