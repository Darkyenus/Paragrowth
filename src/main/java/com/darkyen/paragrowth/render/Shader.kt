package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GLTexture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ObjectIntMap
import com.darkyen.paragrowth.util.GdxArray
import com.darkyen.paragrowth.util.alpha
import com.darkyen.paragrowth.util.blue
import com.darkyen.paragrowth.util.green
import com.darkyen.paragrowth.util.red
import com.darkyen.paragrowth.util.stack
import java.io.File

typealias LocalSetter = (uniform: Shader.Uniform, camera:Camera, renderable: RenderModel) -> Unit
typealias GlobalSetter = (uniform: Shader.Uniform, camera:Camera, attributes:Attributes) -> Unit

abstract class Shader(val order:Int,
                      val name:String,
                      val vertexAttributes:VertexAttributes,
                      vertexShaderName:String = name,
                      fragmentShaderName:String = name,
                      /** Set to the size of arrays for instanced uniforms */
                      internal val maxInstances:Int = 0,
                      internal val defines:Map<String, String> = emptyMap()) {

    private var program = 0

    private val vertexShaderFile: FileHandle = Gdx.files.local("${vertexShaderName}_vert.glsl")
    private var vertexShader = 0

    private val fragmentShaderFile: FileHandle = Gdx.files.local("${fragmentShaderName}_frag.glsl")
    private var fragmentShader = 0

    private val uniforms = GdxArray<Uniform>()
    private val globalUniforms = GdxArray<Uniform>()
    private val localUniforms = GdxArray<Uniform>()
    private val instancedUniforms = GdxArray<Uniform>()

    val hasLocalUniforms:Boolean
        get() = localUniforms.size > 0

    val hasInstancedUniforms:Boolean
        get() = instancedUniforms.size > 0

    /** Compile this shader program. Call while not bound!
     * Can be called repeatedly for shader hotswapping.  */
    private fun compile() {
        stack {
            val gl = Gdx.gl30
            val vertexShader = createShader(vertexShaderFile, GL20.GL_VERTEX_SHADER, defines)
            val fragmentShader = createShader(fragmentShaderFile, GL20.GL_FRAGMENT_SHADER, defines)

            val program = gl.glCreateProgram()
            gl.glAttachShader(program, vertexShader)
            gl.glAttachShader(program, fragmentShader)

            for (i in vertexAttributes.attributes.indices) {
                gl.glBindAttribLocation(program, vertexAttributes.locations[i], vertexAttributes.attributes[i].name)
            }

            gl.glLinkProgram(program)

            for (i in vertexAttributes.attributes.indices) {
                val expectedLocation = vertexAttributes.locations[i]
                val attrName = vertexAttributes.attributes[i].name
                val foundLocation = gl.glGetAttribLocation(program, attrName)
                if (expectedLocation != foundLocation) {
                    Gdx.app.error(LOG, "Shader $name ($defines) did not bind attribute $attrName correctly, expected: $expectedLocation, got: $foundLocation")
                }
            }

            val status = mallocInt(1)
            gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, status)

            if (status.get(0) == GL20.GL_FALSE) {
                val log = gl.glGetProgramInfoLog(program)
                Gdx.app.error(LOG, "Failed to compile shader $name:\n$log")
                gl.glDeleteShader(vertexShader)
                gl.glDeleteShader(fragmentShader)
                gl.glDeleteProgram(program)
                return
            }

            if (this@Shader.program != 0) {
                gl.glDeleteShader(this@Shader.vertexShader)
                gl.glDeleteShader(this@Shader.fragmentShader)
                gl.glDeleteProgram(this@Shader.program)
            }

            this@Shader.program = program
            this@Shader.vertexShader = vertexShader
            this@Shader.fragmentShader = fragmentShader
            for (uniform in uniforms) {
                uniform.init()
            }
        }
    }

    fun init() {
        if (program == 0) {
            if (order == NEVER_INIT) {
                throw IllegalStateException("This is a null shader")
            }

            compile()

            synchronized(reloadedShaders) {
                reloadedShaders.add(this)
            }
        }
    }

    private var context: RenderContext? = null
    private var camera: Camera? = null

    fun begin(camera: Camera, context: RenderContext, attributes:Attributes) {
        this.camera = camera
        this.context = context
        init()
        Gdx.gl30.glUseProgram(program)

        adjustContext(context)
        for (uniform in globalUniforms) {
            if (uniform.location < 0) continue
            uniform.globalSetter!!.invoke(uniform, camera, attributes)
        }
    }

    fun updateLocalUniforms(renderable:RenderModel) {
        val camera = camera!!
        for (uniform in localUniforms) {
            if (uniform.location < 0) continue
            uniform.localSetter!!.invoke(uniform, camera, renderable)
        }
    }

    fun updateInstancedUniforms(renderables:Array<RenderModel>, from:Int, to:Int) {
        val camera = camera!!
        for (uniform in instancedUniforms) {
            if (uniform.location < 0) continue
            val originalLocation = uniform.location
            assert(uniform.location1 > originalLocation)
            val stride = uniform.location1 - originalLocation
            try {
                var instance = 0
                @Suppress("UseWithIndex")//Not optimized, creates garbage
                for (renderableI in from until to) {
                    uniform.location = originalLocation + instance * stride
                    uniform.localSetter!!.invoke(uniform, camera, renderables[renderableI])
                    instance++
                }
            } finally {
                uniform.location = originalLocation
            }
        }
    }

    fun end() {
        context = null
    }

    fun dispose() {
        Gdx.gl30.apply {
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            glDeleteProgram(program)
        }
        uniforms.clear()
        localUniforms.clear()
        globalUniforms.clear()
        synchronized(reloadedShaders) {
            reloadedShaders.remove(this)
        }
    }

    /** Called after shader is activated. Modify [context] with values which are common for all rendered objects. */
    open fun adjustContext(context:RenderContext) {}

    fun localUniform(name:String, setter: LocalSetter) {
        val uniform = Uniform(this, name, setter, null)
        uniforms.add(uniform)
        localUniforms.add(uniform)
    }

    fun instancedUniform(name:String, setter: LocalSetter) {
        assert(maxInstances > 1)

        val uniform = Uniform(this, name, setter, null)
        uniforms.add(uniform)
        instancedUniforms.add(uniform)
    }

    fun globalUniform(name:String, setter: GlobalSetter) {
        val uniform = Uniform(this, name, null, setter)
        uniforms.add(uniform)
        globalUniforms.add(uniform)
    }

    class Uniform internal constructor(
            private val shader: Shader,
            private val name: String,
            internal val localSetter: LocalSetter?,
            internal val globalSetter: GlobalSetter?) {

        private var shaderProgram:Int = 0
        internal var location = -2
        /** Location of the second array position, for stride calculation. */
        internal var location1 = -1

        internal fun init() {
            val currentShaderProgram = shader.program
            if (currentShaderProgram == 0)
                throw IllegalStateException("Shader not initialized yet")
            if (location == -2 || shaderProgram != currentShaderProgram) {
                shaderProgram = currentShaderProgram
                location = Gdx.gl30.glGetUniformLocation(currentShaderProgram, name)
                if (location < 0) {
                    Gdx.app.log(LOG, "${shader.name} (${shader.defines}): Location of $name uniform not found")
                } else {
                    location1 = Gdx.gl30.glGetUniformLocation(currentShaderProgram, "$name[1]")
                }
            }
        }

        fun set(value: Matrix4) {
            Gdx.gl30.glUniformMatrix4fv(location, 1, false, value.`val`, 0)
        }

        fun set(value: Matrix3) {
            Gdx.gl30.glUniformMatrix3fv(location, 1, false, value.`val`, 0)
        }

        fun set(value: Vector3) {
            set(value.x, value.y, value.z)
        }

        fun set(value: Vector2) {
            set(value.x, value.y)
        }

        fun set(value: Color) {
            set(value.r, value.g, value.b, value.a)
        }

        fun setColor(value: com.darkyen.paragrowth.util.Color) {
            set(value.red, value.green, value.blue, value.alpha)
        }

        fun set(value: Float) {
            Gdx.gl30.glUniform1f(location, value)
        }

        fun set(v1: Float, v2: Float) {
            Gdx.gl30.glUniform2f(location, v1, v2)
        }

        fun set(v1: Float, v2: Float, v3: Float) {
            Gdx.gl30.glUniform3f(location, v1, v2, v3)
        }

        fun set(v1: Float, v2: Float, v3: Float, v4: Float) {
            Gdx.gl30.glUniform4f(location, v1, v2, v3, v4)
        }

        fun set(value: Int) {
            Gdx.gl30.glUniform1i(location, value)
        }

        fun set(v1: Int, v2: Int) {
            Gdx.gl30.glUniform2i(location, v1, v2)
        }

        fun set(v1: Int, v2: Int, v3: Int) {
            Gdx.gl30.glUniform3i(location, v1, v2, v3)
        }

        fun set(v1: Int, v2: Int, v3: Int, v4: Int) {
            Gdx.gl30.glUniform4i(location, v1, v2, v3, v4)
        }

        fun set(textureDesc: TextureDescriptor<*>) {
            Gdx.gl30.glUniform1i(location, shader.context!!.textureBinder.bind(textureDesc))
        }

        fun set(texture: GLTexture) {
            Gdx.gl30.glUniform1i(location, shader.context!!.textureBinder.bind(texture))
        }
    }

    companion object {
        const val ANIMALS = 21
        const val DOODADS = 20
        const val TERRAIN_OCEAN = 11
        const val TERRAIN = 10
        const val SKYBOX = -1000

        private const val NEVER_INIT = Int.MIN_VALUE

        internal val NULL_SHADER: Shader
        /** Here, for lack of better place to put it. */
        internal val NULL_VAO: GlVertexArrayObject

        private const val LOG = "Shader"

        init {
            val EMPTY_VERTEX_ATTRIBUTES = VertexAttributes()
            NULL_SHADER = object : Shader(NEVER_INIT, "", EMPTY_VERTEX_ATTRIBUTES) {}
            NULL_VAO = GlVertexArrayObject(null, EMPTY_VERTEX_ATTRIBUTES)
        }

        /** Create a shader of given [type] from the contents of [shaderSource].
         * @return -1 on failure */
        private fun createShader(shaderSource: FileHandle, type: Int, defines:Map<String, String>): Int {
            val gl = Gdx.gl30
            val source = StringBuilder()
            shaderSource.reader("UTF-8").use {
                val buffer = CharArray(4096)
                while (true) {
                    val read = it.read(buffer)
                    if (read < 0) {
                        break
                    }
                    source.append(buffer, 0, read)
                }
            }

            var definesInsertIndex = source.indexOf("#version")
            if (definesInsertIndex >= 0) {
                definesInsertIndex = source.indexOf('\n', definesInsertIndex)
                if (definesInsertIndex < 0) {
                    definesInsertIndex = source.length
                } else {
                    definesInsertIndex++
                }
            }

            val definesSb = StringBuilder()
            for ((key, value) in defines) {
                definesSb.append("#define ").append(key).append(' ').append(value).append('\n')
            }
            source.insert(definesInsertIndex, definesSb)


            val shader = gl.glCreateShader(type)
            gl.glShaderSource(shader, source.toString())
            gl.glCompileShader(shader)

            val status = stack {
                val status = mallocInt(1)
                gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, status)
                status.get(0)
            }

            if (status == GL20.GL_FALSE) {
                val log = gl.glGetShaderInfoLog(shader)
                Gdx.app.error(LOG, "Failed to compile shader " + shaderSource.name() + ":\n" + log)
                gl.glDeleteShader(shader)
                return -1
            }

            return shader
        }

        private val reloadedShaders = HashSet<Shader>()

        init {
            Thread({
                val lastSeen = ObjectIntMap<File>()

                while (true) {
                    synchronized(reloadedShaders) {
                        for (shader in reloadedShaders) {
                            val frag = shader.fragmentShaderFile.file()
                            val vert = shader.vertexShaderFile.file()
                            val fragTime = frag.lastModified().toInt()
                            val vertTime = vert.lastModified().toInt()
                            val oldFragTime = lastSeen.get(frag, 0)
                            val oldVertTime = lastSeen.get(vert, 0)

                            if (oldFragTime == 0 || oldVertTime == 0) {
                                lastSeen.put(frag, fragTime)
                                lastSeen.put(vert, vertTime)
                            } else if (oldFragTime != fragTime || oldVertTime != vertTime) {
                                lastSeen.put(frag, fragTime)
                                lastSeen.put(vert, vertTime)
                                Gdx.app.postRunnable {
                                    shader.compile()
                                    println("${shader.name} reloaded (${System.currentTimeMillis()})")
                                }
                            }
                        }
                    }

                    Thread.sleep(2000)
                }
            }, "ShaderAutoReloader").apply {
                isDaemon = true
                start()
            }
        }
    }
}