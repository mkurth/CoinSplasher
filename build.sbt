name := "CoinSplasher"
scalaVersion := "2.13.3"
val commonSettings = Seq(
  scalaVersion := "2.13.3",
  version := "0.0.1",
  libraryDependencies += "com.typesafe.play"     %% "play-json"      % "2.9.1",
  libraryDependencies += "com.softwaremill.sttp" %% "core"           % "1.7.2",
  libraryDependencies += "com.typesafe"          % "config"          % "1.4.0",
  libraryDependencies += "org.typelevel"         %% "cats-core"      % "2.2.0",
  libraryDependencies += "org.typelevel"         %% "cats-effect"    % "2.2.0",
  libraryDependencies += "org.scalatest"         %% "scalatest"      % "3.2.2" % Test,
  libraryDependencies += "ch.qos.logback"        % "logback-classic" % "1.2.3" % Runtime
)

lazy val core = project
  .in(new File("core"))
  .settings(commonSettings)

lazy val console = project
  .in(new File("console"))
  .dependsOn(core)
  .settings(commonSettings)
