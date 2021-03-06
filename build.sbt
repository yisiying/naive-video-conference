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

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


lazy val webrtcMessage = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings: _*)

lazy val webrtcMessageJvm = webrtcMessage.jvm
lazy val webrtcMessageJs = webrtcMessage.js


lazy val protocol = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings: _*)

lazy val protocolJvm = protocol.jvm
lazy val protocolJs = protocol.js

lazy val shared = (project in file("shared"))
  .settings(name := "shared")
  .settings(commonSettings: _*)
  
lazy val rtpClient = (project in file("rtpClient"))
    .settings(name := "rtpClient")
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Dependencies.akkaSeq,
      libraryDependencies ++= Dependencies.backendDependencies)
    .dependsOn(shared)



lazy val phoneClient = (project in file("phoneClient"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "phoneClient")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1" withSources()
    )
  )
  .dependsOn(webrtcMessageJs, protocolJs)

lazy val webClient = (project in file("webClient"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "webClient")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1" withSources()
    )
  )
  .dependsOn(webrtcMessageJs, protocolJs)


val pcClientMain = "org.seekloud.theia.pcClient.Boot"
lazy val pcClient = (project in file("pcClient")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(pcClientMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "pcClient")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("pcClient" -> pcClientMain),
    packJvmOpts := Map("pcClient" -> Seq("-Xmx4096m", "-Xms128m")),
    packExtraClasspath := Map("pcClient" -> Seq("."))
  )
  .settings(
    //    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4PcClient.pcClientDependencies,
  )
  .dependsOn(protocolJvm, rtpClient, capture, player)

val captureMain = "org.seekloud.theia.capture.Boot"
lazy val capture = (project in file("capture")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(captureMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "capture")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("capture" -> pcClientMain),
    packJvmOpts := Map("capture" -> Seq("-Xmx256m", "-Xms64m")),
    packExtraClasspath := Map("capture" -> Seq("."))
  )
  .settings(
    //    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4Capture.captureDependencies,
  )
  .dependsOn(protocolJvm)



val playerMain = "org.seekloud.theia.player.Boot"
lazy val player = (project in file("player")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(playerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "player")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("player" -> playerMain),
    packJvmOpts := Map("player" -> Seq("-Xmx256m", "-Xms64m")),
    packExtraClasspath := Map("player" -> Seq("."))
  )
  .settings(
    //    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4Player.playerDependencies,
  )
  .dependsOn(protocolJvm, rtpClient)


lazy val statistics = (project in file("statistics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "statistics")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1" withSources()
    )
  )
  .dependsOn(protocolJs)


val roomManagerMain = "org.seekloud.theia.roomManager.Boot"

lazy val roomManager = (project in file("roomManager")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(roomManagerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "roomManager")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("roomManager" -> roomManagerMain),
    packJvmOpts := Map("roomManager" -> Seq("-Xmx64m", "-Xms32m")),
    packExtraClasspath := Map("roomManager" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  )
  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in webClient).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in webClient).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in webClient).value,
    watchSources ++= (watchSources in webClient).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm)

  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in phoneClient).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in phoneClient).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in phoneClient).value,
    watchSources ++= (watchSources in phoneClient).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm)

  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in statistics).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in statistics).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in statistics).value,
    watchSources ++= (watchSources in statistics).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm)





val processorMain = "org.seekloud.theia.processor.Boot"

lazy val processor = (project in file("processor")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(processorMain),
    javaOptions in reStart += "-Xmx4g"
  )
  .settings(name := "processor")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("processor" -> processorMain),
    packJvmOpts := Map("processor" -> Seq("-Xmx6g", "-Xms3g")),
    packExtraClasspath := Map("processor" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs
  ).dependsOn(protocolJvm, rtpClient)


val distributorMain = "org.seekloud.theia.distributor.Boot"

lazy val distributorPage = (project in file("distributorPage"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "distributorPage")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1" withSources()
    )
  )
  .dependsOn(protocolJs)

lazy val distributor = (project in file("distributor")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(distributorMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "distributor")
  .settings(
    packMain := Map("distributor" -> distributorMain),
    packJvmOpts := Map("distributor" -> Seq("-Xmx2g", "-Xms1g")),
    packExtraClasspath := Map("distributor" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs
  )
  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in distributorPage).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in distributorPage).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in distributorPage).value,
    watchSources ++= (watchSources in distributorPage).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm, rtpClient)


val aiMain = "org.seekloud.theia.ai.Boot"

lazy val ai = (project in file("ai")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(aiMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "processor")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("ai" -> aiMain),
    packJvmOpts := Map("ai" -> Seq("-Xmx64m", "-Xms32m")),
    packExtraClasspath := Map("ai" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  ).dependsOn(protocolJvm)


val rtmpServerMain = "org.seekloud.theia.rtmpServer.Boot"

lazy val rtmpServer = (project in file("rtmpServer")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(rtmpServerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "rtmpServer")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("rtmpServer" -> rtmpServerMain),
    packJvmOpts := Map("rtmpServer" -> Seq("-Xmx1024m", "-Xms1024m")),
    packExtraClasspath := Map("rtmpServer" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs
  ).dependsOn(protocolJvm, rtpClient)



val rtpServerMain = "org.seekloud.theia.rtpServer.Boot"

lazy val rtpServer = (project in file("rtpServer")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(rtpServerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "rtpServer")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("rtpServer" -> rtpServerMain),
    packJvmOpts := Map("rtpServer" -> Seq("-Xmx512m", "-Xms32m")),
    packExtraClasspath := Map("rtpServer" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  ).dependsOn(protocolJvm, shared, rtpClient)



val webrtcServerMain = "org.seekloud.theia.webrtcServer.Boot"

lazy val webrtcServer = (project in file("webrtcServer")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(webrtcServerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "webrtcServer")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("webrtcServer" -> webrtcServerMain),
    packJvmOpts := Map("webrtcServer" -> Seq("-Xmx1024m", "-Xms1024m")),
    packExtraClasspath := Map("webrtcServer" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies4WebRtc.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4WebRtc.dependencies4WebRtc
  ).dependsOn(webrtcMessageJvm, protocolJvm, rtpClient)


val faceAnalysisMain = "org.seekloud.theia.faceAnalysis.BootJFx"
resolvers in ThisBuild ++= Seq(
  "Spring Plugins Repository" at "https://repo.spring.io/plugins-release/"
)
lazy val faceAnalysis = (project in file("faceAnalysis")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(faceAnalysisMain),
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    javaOptions in reStart += "-Xmx3g"
  )
  .settings(name := "faceAnalysis")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("faceAnalysis" -> faceAnalysisMain),
    packJvmOpts := Map("faceAnalysis" -> Seq("-Xmx512m", "-Xms256m", "-XX:+HeapDumpOnOutOfMemoryError")),
    packExtraClasspath := Map("faceAnalysis" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4Face.jme3Libs,
    libraryDependencies += "org.tensorflow" % "tensorflow" % "1.15.0",
    libraryDependencies += "org.nd4j" % "nd4j-native-platform" % "1.0.0-beta4",
    libraryDependencies += "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )
  .dependsOn(protocolJvm, rtpClient, player)

