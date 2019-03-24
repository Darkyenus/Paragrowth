/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntArray as GdxIntArray
import com.badlogic.gdx.utils.IntIntMap
import com.darkyen.paragrowth.util.AutoReloadShaderProgram
import com.darkyen.paragrowth.util.PrioritizedShader
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias LocalSetter = (uniform: ParaShader.Uniform, camera:Camera, renderable: Renderable) -> Unit
typealias GlobalSetter = (uniform: ParaShader.Uniform, camera:Camera) -> Unit

abstract class ParaShader(private val priority:Int,
                          private val name:String,
                          private val vertexAttributes:VertexAttributes) : Shader, PrioritizedShader {

    private var shaderProgram:ShaderProgram? = null

    class Uniform internal constructor(
            private val shader: ParaShader,
            internal val localSetter: LocalSetter?,
            internal val globalSetter: GlobalSetter?) : ReadOnlyProperty<ParaShader, Uniform> {

        internal var name:String = ""

        private var shaderProgram:ShaderProgram? = null
        internal var location = -2

        internal fun init() {
            val currentShaderProgram = shader.shaderProgram ?: throw IllegalStateException("Shader not initialized yet")
            if (location == -2 || shaderProgram !== currentShaderProgram) {
                shaderProgram = currentShaderProgram
                location = currentShaderProgram.fetchUniformLocation(name, false)
                if (location < 0) {
                    Gdx.app.log("ParaShader", "${shader.name}: Location of $name uniform not found")
                }
            }
        }

        operator fun provideDelegate(thisRef: ParaShader, prop: KProperty<*>): ReadOnlyProperty<ParaShader, Uniform> {
            assert(shader === thisRef)
            name = prop.name
            return this
        }

        override fun getValue(thisRef: ParaShader, property: KProperty<*>): Uniform {
            assert(shader === thisRef)
            init()
            return this
        }

        fun set(value: Matrix4) {
            shaderProgram!!.setUniformMatrix(location, value)
        }

        fun set(value: Matrix3) {
            shaderProgram!!.setUniformMatrix(location, value)
        }

        fun set(value: Vector3) {
            shaderProgram!!.setUniformf(location, value)
        }

        fun set(value: Vector2) {
            shaderProgram!!.setUniformf(location, value)
        }

        fun set(value: Color) {
            shaderProgram!!.setUniformf(location, value)
        }

        fun set(value: Float) {
            shaderProgram!!.setUniformf(location, value)
        }

        fun set(v1: Float, v2: Float) {
            shaderProgram!!.setUniformf(location, v1, v2)
        }

        fun set(v1: Float, v2: Float, v3: Float) {
            shaderProgram!!.setUniformf(location, v1, v2, v3)
        }

        fun set(v1: Float, v2: Float, v3: Float, v4: Float) {
            shaderProgram!!.setUniformf(location, v1, v2, v3, v4)
        }

        fun set(value: Int) {
            shaderProgram!!.setUniformi(location, value)
        }

        fun set(v1: Int, v2: Int) {
            shaderProgram!!.setUniformi(location, v1, v2)
        }

        fun set(v1: Int, v2: Int, v3: Int) {
            shaderProgram!!.setUniformi(location, v1, v2, v3)
        }

        fun set(v1: Int, v2: Int, v3: Int, v4: Int) {
            shaderProgram!!.setUniformi(location, v1, v2, v3, v4)
        }

        fun set(textureDesc: TextureDescriptor<*>) {
            shaderProgram!!.setUniformi(location, shader.context!!.textureBinder.bind(textureDesc))
        }

        fun set(texture: GLTexture) {
            shaderProgram!!.setUniformi(location, shader.context!!.textureBinder.bind(texture))
        }
    }

    private val uniforms = Array<Uniform>()
    private val globalUniforms = Array<Uniform>()
    private val localUniforms = Array<Uniform>()

    private val attributes = IntIntMap()
    private val attributeLocations = IntArray(vertexAttributes.size())

    protected fun uniform(): Uniform {
        val uniform = Uniform(this, null, null)
        uniforms.add(uniform)
        return uniform
    }

    protected fun localUniform(name:String, setter: LocalSetter) {
        val uniform = Uniform(this, setter, null)
        uniform.name = name
        uniforms.add(uniform)
        localUniforms.add(uniform)
    }

    protected fun globalUniform(name:String, setter: GlobalSetter) {
        val uniform = Uniform(this, null, setter)
        uniform.name = name
        uniforms.add(uniform)
        globalUniforms.add(uniform)
    }

    override fun init() {
        var shaderProgram = shaderProgram
        if (shaderProgram == null) {
            // Todo reload positions as well
            shaderProgram = AutoReloadShaderProgram(Gdx.files.local("${name}_vert.glsl"), Gdx.files.local("${name}_frag.glsl"))

            this.shaderProgram = shaderProgram

            for (uniform in uniforms) {
                uniform.init()
            }

            val attributes = attributes
            attributes.clear()

            val attrs = vertexAttributes
            val attributeLocations = attributeLocations
            for (i in attributeLocations.indices) {
                val attr = attrs.get(i)
                attributeLocations[i] = shaderProgram.getAttributeLocation(attr.alias)
            }
        }
    }

    private var context: RenderContext? = null
    private var camera: Camera? = null
    private var currentMesh: Mesh? = null

    final override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.context = context
        this.currentMesh = null
        init()
        shaderProgram!!.begin()

        adjustContext(context)
        for (uniform in globalUniforms) {
            if (uniform.location < 0) continue
            uniform.globalSetter!!.invoke(uniform, camera)
        }
    }

    open fun adjustContext(context:RenderContext) {}
    open fun adjustContext(context:RenderContext, renderable:Renderable) {}

    final override fun render(renderable: Renderable) {
        adjustContext(context!!, renderable)

        for (uniform in localUniforms) {
            if (uniform.location < 0) continue
            uniform.localSetter!!.invoke(uniform, camera!!, renderable)
        }

        var currentMesh = currentMesh
        val shaderProgram = shaderProgram!!

        if (currentMesh !== renderable.meshPart.mesh) {
            currentMesh?.unbind(shaderProgram, attributeLocations)
            currentMesh = renderable.meshPart.mesh!!
            assert(currentMesh.vertexAttributes == vertexAttributes) { "Expected $vertexAttributes, got ${currentMesh.vertexAttributes}" }
            currentMesh.bind(shaderProgram, attributeLocations)
        }
        this.currentMesh = currentMesh

        renderable.meshPart.render(shaderProgram, false)
    }

    override fun end() {
        val shaderProgram = shaderProgram!!
        currentMesh?.unbind(shaderProgram, attributeLocations)
        currentMesh = null
        shaderProgram.end()
    }

    override fun dispose() {
        shaderProgram?.dispose()
        shaderProgram = null
        uniforms.clear()
        localUniforms.clear()
        globalUniforms.clear()
    }

    override fun compareTo(other: Shader?): Int = throw NotImplementedError("Not used")

    override fun canRender(instance: Renderable?): Boolean = true

    override fun priority(): Int = priority
}
