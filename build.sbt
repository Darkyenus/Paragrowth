import darkyenus.resourcepacker.{LWJGLLauncher, PackingOperation}

name := "LowscapeEngine"

version := "1.0"

scalaVersion := "2.11.5"

organization := "darkyenus"

crossPaths := false

val gdxVersion = "1.5.0"

baseDirectory in Compile := baseDirectory.value / "assets"

fork in run := true

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.esotericsoftware" % "kryonet" % "2.22.0-RC1",
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop"
)

scalacOptions ++= Seq("-deprecation","-feature")

TaskKey[Unit]("packResources") := {
  LWJGLLauncher.launch(new PackingOperation(file("./resources"), file("./assets")))
}