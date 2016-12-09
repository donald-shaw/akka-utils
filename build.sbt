name := "AkkaUtils"

version := "0.1.0"

scalaVersion := "2.11.8"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

val akka_version = "2.4.4"
val slick_version = "3.1.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akka_version,
  "com.typesafe.akka" %% "akka-http-core" % akka_version,
  //"io.spray" % "spray-json_2.11" % "1.3.2",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akka_version,
  "com.typesafe.slick" %% "slick" % slick_version,
  "com.typesafe.slick" %% "slick-hikaricp" % slick_version,
  "org.slf4j" % "slf4j-log4j12" % "1.7.18",
  "org.postgresql" % "postgresql" % "9.4.1207",
//  "org.apache.commons" % "commons-lang3" % "3.4",
//  "com.h2database" % "h2" % "1.4.187",
//  "ch.megard" %% "akka-http-cors" % "0.1.1" intransitive(),
  "com.typesafe.akka" %% "akka-testkit" % akka_version % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akka_version % "test",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
//  "com.miguno.akka" %% "akka-mock-scheduler" % "0.3.1" % "test"
)
