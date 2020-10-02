name := "CoinSplasher"
scalaVersion := "2.13.3"

val circeVersion = "0.12.3"

val commonSettings = Seq(
  scalaVersion := "2.13.3",
  version := "0.0.1",
  libraryDependencies += "org.typelevel" %% "cats-core"    % "2.2.0",
  libraryDependencies += "org.typelevel" %% "cats-effect"  % "2.2.0",
  libraryDependencies += "eu.timepit"    %% "refined"      % "0.9.17",
  libraryDependencies += "eu.timepit"    %% "refined-cats" % "0.9.17",
  libraryDependencies += "org.scalatest" %% "scalatest"    % "3.2.2" % Test
)

lazy val core = project
  .in(new File("core"))
  .settings(commonSettings)

lazy val console = project
  .in(new File("console"))
  .dependsOn(core)
  .settings(
    commonSettings,
    libraryDependencies += "io.circe"                     %% "circe-core"                     % circeVersion,
    libraryDependencies += "io.circe"                     %% "circe-generic"                  % circeVersion,
    libraryDependencies += "io.circe"                     %% "circe-parser"                   % circeVersion,
    libraryDependencies += "io.circe"                     %% "circe-refined"                  % circeVersion,
    libraryDependencies += "com.softwaremill.sttp.client" %% "core"                           % "2.2.9",
    libraryDependencies += "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % "2.2.9",
    libraryDependencies += "com.softwaremill.sttp.client" %% "circe"                          % "2.2.9"
  )
