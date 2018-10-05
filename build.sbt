name := "CoinSplasher"
version := "0.0.1"

scalaVersion := "2.12.7"

dependsOn(RootProject(uri("https://github.com/oyvindberg/binance-scala-api.git#master")))

libraryDependencies += "com.olvind" %% "binance-scala-api" % "0.0.1-SNAPSHOT"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.3.5"
libraryDependencies += "com.typesafe" % "config" % "1.3.3"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime