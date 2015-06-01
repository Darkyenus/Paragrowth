import darkyenus.resourcepacker.{LWJGLLauncher, PackingOperation}

name := "LowscapeEngine"

version := "1.0"

scalaVersion := "2.11.5"

organization := "darkyenus"

crossPaths := false

val gdxVersion = "1.6.1"

baseDirectory in (Compile, run) := baseDirectory.value / "assets"

baseDirectory in (Compile, hotswap_i) := baseDirectory.value / "assets"

fork in run := true

libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.esotericsoftware" % "kryonet" % "2.22.0-RC1",
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop"
)

autoScalaLibrary := false

TaskKey[Unit]("packResources") := {
  LWJGLLauncher.launch(new PackingOperation(file("./resources"), file("./assets")))
}