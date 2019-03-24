@file:JvmName("Shaders")
package com.darkyen.paragrowth.render

/*
 * Shader singleton instances
 */

@JvmField
val DOODAD_SHADER = DoodadShader()

@JvmField
val SKYBOX_SHADER = SkyboxShader()

@JvmField
var TERRAIN_SHADER = TerrainShader()
