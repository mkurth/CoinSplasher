name := "CoinSplasher"
version := "0.0.1"

scalaVersion := "2.12.7"

dependsOn(RootProject(uri("https://github.com/oyvindberg/binance-scala-api.git#master")))

libraryDependencies += "com.olvind" %% "binance-scala-api" % "0.0.1-SNAPSHOT"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.10"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.10"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"