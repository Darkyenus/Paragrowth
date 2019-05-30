package com.darkyen.paragrowth.util

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.ObjectMap
import com.darkyen.paragrowth.render.ModelBuilder

private val OBJ_MTL_LINE_TOKENIZER = "\\s+".toPattern()

/**
 *
 */
fun ModelBuilder.loadObjModel(objFile: FileHandle, faceVertices:Int = 3, createVertex:ModelBuilder.(x:Float, y:Float, z:Float, material:ObjMaterial) -> Short) {
    objFile.reader(4096, "UTF-8").use {
        val materials = ObjectMap<String, ObjMaterial>()
        val vertices = GdxFloatArray(true, 512)
        val indicesForVerticesWithMaterial = ObjectMap<String, GdxIntArray>()

        var material = ObjMaterial("")
        var materialIndices = GdxIntArray(true, 128)
        indicesForVerticesWithMaterial.put(material.name, materialIndices)

        while (true) {
            val line = it.readLine()?.trim() ?: break
            if (line.isEmpty() || line.startsWith('#')) {
                continue
            }

            val tokens = line.split(OBJ_MTL_LINE_TOKENIZER)
            when (tokens[0].toLowerCase()) {
                "mtllib" -> {
                    materials.putAll(loadMtl(objFile.sibling(tokens[1])))
                }
                "v" -> {
                    vertices.add(tokens[1].toFloat())
                    vertices.add(tokens[2].toFloat())
                    vertices.add(tokens[3].toFloat())
                }
                "s" -> {} // Shading ignored
                "usemtl" -> {
                    material = materials.get(tokens[1]) ?: run {
                        System.err.println("Undefined material: $tokens, known materials: ${materials.keys().toList()}")
                        material
                    }
                    var indices = indicesForVerticesWithMaterial.get(material.name)
                    if (indices == null) {
                        indices = GdxIntArray(true, 128)
                        indicesForVerticesWithMaterial.put(material.name, indices)
                    }
                    materialIndices = indices
                }
                "f" -> { // face
                    val faceIndices = ShortArray(tokens.size - 1)
                    assert(faceVertices == faceIndices.size)

                    for (i in 1 until tokens.size) {
                        val vectorIndex = tokens[i].toInt() - 1 // 1-indexed
                        if (materialIndices.size <= vectorIndex) {
                            materialIndices.ensureCapacity(vectorIndex + 1 - materialIndices.size)
                            materialIndices.size = vectorIndex + 1
                        }
                        var generatedIndex = materialIndices[vectorIndex]
                        if (generatedIndex == 0) {
                            // Vector with this material does not yet exist
                            val x = vertices[vectorIndex * 3]
                            val y = vertices[vectorIndex * 3 + 1]
                            val z = vertices[vectorIndex * 3 + 2]
                            generatedIndex = (createVertex(x, y, z, material).toInt() and 0xFFFF) + 1
                            materialIndices[vectorIndex] = generatedIndex
                        }

                        faceIndices[i - 1] = (generatedIndex - 1).toShort()
                    }

                    index(*faceIndices)
                }
                else -> {
                    System.err.println("Unknown MTL parameter: $tokens")
                }
            }

        }
    }
}

private fun List<String>.mtlColor(to:GdxColor) {
    to.set(this[1].toFloat(), this[2].toFloat(), this[3].toFloat(), this.getOrNull(4)?.toFloat() ?: 1f)
}

fun loadMtl(mtlFile:FileHandle): ObjectMap<String, ObjMaterial> {
    val materials = ObjectMap<String, ObjMaterial>()

    var currentMaterial:ObjMaterial? = null

    mtlFile.reader(4096, "UTF-8").use {
        while (true) {
            val line = it.readLine()?.trim() ?: break
            if (line.isEmpty() || line.startsWith('#')) {
                continue
            }

            val tokens = line.split(OBJ_MTL_LINE_TOKENIZER)
            when (tokens[0].toLowerCase()) {
                "newmtl" -> {
                    val newMaterial = ObjMaterial(tokens[1])
                    currentMaterial = newMaterial
                    materials.put(tokens[1], newMaterial)
                }
                "ns" -> currentMaterial!!.specularExponent = tokens[1].toFloat()
                "ka" -> tokens.mtlColor(currentMaterial!!.ambient)
                "kd" -> tokens.mtlColor(currentMaterial!!.diffuse)
                "ks" -> tokens.mtlColor(currentMaterial!!.specular)
                "ke" -> {}//???
                "ni" -> currentMaterial!!.opticalDensity = tokens[1].toFloat()
                "d" -> currentMaterial!!.alpha = tokens[1].toFloat()
                "illum" -> currentMaterial!!.illuminationModel = tokens[1].toInt()
                else -> {
                    System.err.println("Unknown MTL parameter: $tokens")
                }
            }

        }
    }

    return materials
}

class ObjMaterial(val name:String) {
    val ambient = GdxColor()
    val diffuse = GdxColor()
    val specular = GdxColor()
    var specularExponent = 1f
    var opticalDensity = 1f
    var alpha = 1f
    var illuminationModel = 0
}