package com.mindinventory.shape

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class uses for render shape using OpenGLES
 */
class ShapeActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        private const val VERTEX_SHADER_NAME = "shaders/vertexShader.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/fragmentShader.frag"
        private const val COORDINATES_PER_VERTEX = 2
        private const val VERTEX_STRIDE: Int = COORDINATES_PER_VERTEX * 4

        private val TAG = this::class.java.simpleName

        private val QUADRANT_COORDINATES = floatArrayOf(
            //x,    y
            -0.5f, 0.5f,
            -0.5f, -0.5f,
            0.5f, -0.5f,
            0.5f, 0.5f
        )

        private val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
    }

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private var quadPositionHandle = -1
    private var viewProjectionMatrixHandle: Int = -1
    private var program: Int = -1

    /**
     * Convert float array to float buffer
     */
    private val quadrantCoordinatesBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUADRANT_COORDINATES.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(QUADRANT_COORDINATES)
            position(0)
        }
    }

    /**
     * Convert short array to short buffer
     */
    private val drawOrderBuffer: ShortBuffer = ByteBuffer.allocateDirect(DRAW_ORDER.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(DRAW_ORDER)
            position(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shape)

        findViewById<GLSurfaceView>(R.id.glSurfaceView).setConfiguration()
    }

    /**
     * Uses for set configuration of GlSurfaceView
     */
    @SuppressLint("ClickableViewAccessibility")
    fun GLSurfaceView.setConfiguration() {
        keepScreenOn = true // Keep screen awake till ARCore performs detection
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(this@ShapeActivity)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    /**
     * Called when surface is created or recreated
     */
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        // Set GL clear color to black.
        GLES20.glClearColor(0f, 0f, 0f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            val vertexShader =
                ShaderUtil.loadGLShader(
                    TAG, this, GLES20.GL_VERTEX_SHADER,
                    VERTEX_SHADER_NAME
                )
            val fragmentShader =
                ShaderUtil.loadGLShader(
                    TAG, this, GLES20.GL_FRAGMENT_SHADER,
                    FRAGMENT_SHADER_NAME
                )

            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            GLES20.glUseProgram(program)

            ShaderUtil.checkGLError(TAG, "Program created")

            //Quadrant position handler
            quadPositionHandle = GLES20.glGetAttribLocation(program, "a_Position")

            // View projection transformation matrix handler
            viewProjectionMatrixHandle = GLES20.glGetUniformLocation(program, "uVPMatrix")

            ShaderUtil.checkGLError(TAG, "Program parameters")
        } catch (e: IOException) {
        }
    }

    /**
     * Called after the surface is crated and whenever surface size changes
     */
    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    /**
     * Uses for draw the current frame
     */
    override fun onDrawFrame(p0: GL10?) {
        // Use the cGL clear color specified in onSurfaceCreated() to erase the GL surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        try {
            GLES20.glUseProgram(program)

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(viewProjectionMatrixHandle, 1, false, vPMatrix, 0)

            //Pass quadrant position to shader
            GLES20.glVertexAttribPointer(
                quadPositionHandle,
                COORDINATES_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                quadrantCoordinatesBuffer
            )

            // Enable vertex arrays
            GLES20.glEnableVertexAttribArray(quadPositionHandle)

            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                DRAW_ORDER.size,
                GLES20.GL_UNSIGNED_SHORT,
                drawOrderBuffer
            )

            // Disable vertex arrays
            GLES20.glDisableVertexAttribArray(quadPositionHandle)

            ShaderUtil.checkGLError(TAG, "After draw")
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
        }
    }
}