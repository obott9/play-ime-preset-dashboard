name := "play-ime-preset-dashboard"
organization := "com.github.obott9"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.18"

// Play Framework 3.0.10 (Pekko-based) + Slick
libraryDependencies ++= Seq(
  guice,
  "org.playframework" %% "play-slick"            % "6.1.1",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",
  "org.postgresql"     % "postgresql"             % "42.7.5",
  "com.typesafe.play" %% "play-json"             % "2.10.6",
  filters
)

// Scala compiler options
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)
