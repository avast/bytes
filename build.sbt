lazy val commonSettings = List(
  sonatypeProfileName := "com.avast",
  organization := "com.avast.bytes",
  organizationName := "Avast",
  organizationHomepage := Some(url("https://avast.com")),
  homepage := Some(url("https://github.com/avast/bytes")),
  description := "Provides universal interface for having an immutable representation of sequence of bytes",
  licenses ++= Seq("MIT" -> url(s"https://github.com/avast/bytes/blob/${version.value}/LICENSE")),
  developers := List(Developer("jakubjanecek", "Jakub Janecek", "jakub.janecek@avast.com", url("https://www.avast.com"))),
  libraryDependencies ++= List(
    "junit" % "junit" % "4.12" % Test,
    "com.novocode" % "junit-interface" % "0.11" % Test, // Required by sbt to execute JUnit tests
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "javax.annotation" % "javax.annotation-api" % "1.3.2" % Test // for compatibility with JDK >8
  ),
  Test / publishArtifact := false,
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
  crossPaths := false,
  autoScalaLibrary := false,
  javacOptions ++= List("-source", "1.8"),
  compile / javacOptions ++= List("-target", "1.8") // only `compile`, not `doc`, because Javadoc doesn't accept flag `-target`
)

lazy val root = project
  .in(file("."))
  .aggregate(core, gpb, gpbv3)
  .settings(commonSettings)
  .settings(
    name := "bytes",
    publish / skip := true
  )

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(name := "bytes-core")

lazy val gpb = project
  .in(file("gpb"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "bytes-gpb",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.6.1"
  )

lazy val gpbv3 = project
  .in(file("gpb-v3"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "bytes-gpb-v3",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.15.0"
  )
