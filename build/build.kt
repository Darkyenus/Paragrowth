@file:Suppress("unused")
@file:BuildDependency("com.github.Darkyenus:ResourcePacker:2.4")
@file:BuildDependencyRepository("jitpack", "https://jitpack.io")

import com.darkyen.resourcepacker.PackingOperation
import com.darkyen.resourcepacker.PreferSymlinks
import org.jline.utils.OSUtils
import wemi.Keys.runDirectory
import wemi.Keys.runOptions
import wemi.util.div
import wemi.*
import wemi.dependency.DefaultExclusions
import wemi.dependency.sonatypeOss

val packResources by key<Unit>("Packs resources")

val terrainTest by configuration("Terrain testing") {
    mainClass set { "com.darkyen.paragrowth.terrain.generator.TerrainTest" }
    runOptions modify { it - "-XstartOnFirstThread"}
}

val paragrowth by project {
    projectName set { "paragrowth" }
    projectGroup set { "com.darkyen" }
    projectVersion set { "0.1-SNAPSHOT" }

    repositories add { sonatypeOss("releases") }
    
    val gdxVersion = "1.9.7"

    libraryDependencies add { dependency("com.badlogicgames.gdx:gdx:$gdxVersion") }
    libraryDependencies add { dependency("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion") }
    libraryDependencies add { dependency("com.badlogicgames.gdx:gdx-platform:$gdxVersion", classifier = "natives-desktop") }
    libraryDependencies add { dependency("org.lwjgl:lwjgl-stb:3.1.3") }
    libraryDependencies add { dependency("org.lwjgl:lwjgl-stb:3.1.3", classifier = "natives-linux") }
    libraryDependencies add { dependency("org.lwjgl:lwjgl-stb:3.1.3", classifier = "natives-macos") }
    libraryDependencies add { dependency("org.lwjgl:lwjgl-stb:3.1.3", classifier = "natives-windows") }
    
    libraryDependencies add { Dependency(dependencyId("com.badlogicgames.gdx:gdx-ai:1.8.1"), DefaultExclusions + listOf(DependencyExclusion("com.badlogicgames.gdx", "gdx"))) }

    packResources set {
        resourcePack(PackingOperation((projectRoot.get() / "resources").toFile(), (projectRoot.get() / "assets").toFile(), listOf(PreferSymlinks to true)))
    }

    mainClass set { "com.darkyen.paragrowth.ParagrowthMain" }
    if (OSUtils.IS_OSX) {
        runOptions add { "-XstartOnFirstThread" }
    }
    runDirectory set { projectRoot.get() / "assets" }
}