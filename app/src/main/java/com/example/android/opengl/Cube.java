package com.example.android.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by masual on 12/12/2014.
 */
public class Cube {
    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    //Añadido color a pasar al fragment
                    //lo recibe en aColor y lo pasa al siguiente shader en vColor
                    "attribute vec4 aColor;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    //Añadido pasamos color
                    "  vColor=aColor;" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    //Recibimos el color del otro shader en vColor
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final FloatBuffer vertexBuffer;
    //Añadido
    private final FloatBuffer vertexBufferC;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;
    //Coordenadas del cubo, cada uno de los 8 vértices
    static float cubeCoords[] = {
            -0.5f,  0.5f, 0.5f,   // top left
            -0.5f, -0.5f, 0.5f,   // bottom left
            0.5f, -0.5f, 0.5f,   // bottom right
            0.5f,  0.5f, 0.5f,  // top right
            -0.5f,  0.5f, -0.5f,   // top left back
            -0.5f, -0.5f, -0.5f,   // bottom left back
            0.5f, -0.5f, -0.5f,   // bottom right back
            0.5f,  0.5f, -0.5f  // top right back
    };

    // Orden para dibujar los vértices, cada línea establece una cara que estará formada por dos
    // triángulos
    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3,
                                        3, 2, 6, 3, 6, 7,
                                        0, 3, 7, 0, 7, 4,
                                        1, 5, 6, 1, 6, 2,
                                        4, 5, 1, 4, 1, 0,
                                        7, 6, 5, 7, 5, 4  }; // order to draw vertices

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private final int vertexStrideC = COLORS_PER_VERTEX * 4; // 4 bytes per vertex


    //Colores del cubo RGB, cada vértice se diferencia de sus dos vecinos en solo un bit
    float color[] = { 1f, 1f, 1f, 1.0f,
                    1f, 0f, 1f, 1.0f,
                    1f, 0f, 0f, 1.0f,
                    1f, 1f, 0f, 1.0f,
                    0f, 1f, 1f, 1.0f,
                    0f, 1f, 0f, 1.0f,
                    0f, 0f, 0f, 1.0f,
                    0f, 0f, 1f, 1.0f,};



    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Cube() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);


        //Inicializar vertex byte buffer para color
        ByteBuffer bbc = ByteBuffer.allocateDirect( color.length*4);
        bbc.order(ByteOrder.nativeOrder());
        vertexBufferC = bbc.asFloatBuffer();
        vertexBufferC.put(color);
        vertexBufferC.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // Handler para pasar el color al shader
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");

        GLES20.glEnableVertexAttribArray(mColorHandle);

        GLES20.glVertexAttribPointer(
            mColorHandle, COLORS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStrideC, vertexBufferC
        );
        //2AÑADIDO
        // get handle to fragment shader's vColor member
        //mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        //GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
