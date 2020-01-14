name := "naive-video-conference"

val scalaV = "2.12.10"

val projectName = "naive-video-conference"

val projectVersion = "19.11.1"

def commonSettings = Seq(
  version := projectVersion,
  scalaVersion := scalaV,
  scalacOptions ++= Seq(
    //"-deprecation",
    "-feature"
  ),
  javacOptions ++= Seq("-encoding", "UTF-8")
)