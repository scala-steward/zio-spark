ThisBuild / organization := "io.univalence"

// Spark still uses 1.X.X version of scala-xml
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// Aliases
addCommandAlias("fmt", "scalafmt")
addCommandAlias("fmtCheck", "scalafmtCheckAll")
addCommandAlias("check", "; fmtCheck;")

lazy val plugin =
  (project in file("."))
    .enablePlugins(SbtPlugin)
    .settings(
      name := "zio-spark-codegen",
      libraryDependencies ++= Seq(
        "dev.zio"          %% "zio"              % "2.0.4",
        "dev.zio"          %% "zio-test"         % "2.0.4" % Test,
        "dev.zio"          %% "zio-test-sbt"     % "2.0.4" % Test,
        "org.scalameta"    %% "scalafmt-dynamic" % "3.4.3", // equals to sbt-scalafmt's scalfmt-dynamic version
        "org.scalameta"    %% "scalameta"        % "4.9.9",
        "org.apache.spark" %% "spark-core"       % "3.3.4" withSources (), // For tests only
        "org.apache.spark" %% "spark-sql"        % "3.3.4" withSources () // For tests only
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )
