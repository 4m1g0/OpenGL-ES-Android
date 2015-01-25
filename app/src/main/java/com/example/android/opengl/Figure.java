package com.example.android.opengl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Figure {
    /*private final String vertexShaderCode =
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
                    "}";*/

    final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
            + "uniform mat4 uMVMatrix;       \n"		// A constant representing the combined model/view matrix.
            + "uniform vec3 uLightPos;       \n"	    // The position of the light in eye space.

            + "attribute vec4 vPosition;     \n"		// Per-vertex position information we will pass in.
            + "attribute vec4 aColor;        \n"		// Per-vertex color information we will pass in.
            + "attribute vec3 aNormal;       \n"		// Per-vertex normal information we will pass in.

            + "varying vec4 vColor;          \n"		// This will be passed into the fragment shader.

            + "void main()                    \n" 	// The entry point for our vertex shader.
            + "{                              \n"
            // Transform the vertex into eye space.
            + "   vec3 modelViewVertex = vec3(uMVMatrix * vPosition);              \n"
            // Transform the normal's orientation into eye space.
            + "   vec3 modelViewNormal = vec3(uMVMatrix * vec4(normalize(aNormal), 0.0));     \n"
            // Will be used for attenuation.
            + "   float distance = length(uLightPos - modelViewVertex);             \n"
            // Get a lighting direction vector from the light to the vertex.
            + "   vec3 lightVector = normalize(uLightPos - modelViewVertex);        \n"
            // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
            // pointing in the same direction then it will get max illumination.
            + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);       \n"
            // Attenuate the light based on distance.
            + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
            // Multiply the color by the illumination level. It will be interpolated across the triangle.
            + "   vColor = aColor * diffuse;                                         \n"
            // gl_Position is a special variable used to store the final position.
            // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
            + "   gl_Position = uMVPMatrix * vPosition;                              \n"
            + "}                                                                     \n";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    //Recibimos el color del otro shader en vColor
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final FloatBuffer vertexBuffer;
    //Añadido
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandler;
    private int mLightPosHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;
    //Coordenadas de la figura, cada vértice
    float[] coords;

    float mLightPos[] = {};

    // order to draw vertices
    short[] drawOrder;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private final int colorStride = COLORS_PER_VERTEX * 4; // 4 bytes per vertex
    private final int normalStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Colores RGB
    float[] color;

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Figure() {
        // parse file
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("res/raw/bunny.off");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            line = reader.readLine();
            if (!line.trim().equals("OFF")) {
                throw new IOException();
            }
            line = reader.readLine();
            String token[] = line.split(" ");
            int numVertex = Integer.parseInt(token[0].trim());
            int numFaces = Integer.parseInt(token[1].trim());

            coords = new float[numVertex*COORDS_PER_VERTEX];
            int cordNum = 0;

            // parse vertex
            for (int i=0; i < numVertex; i++){
                line = reader.readLine();
                for (String cord : line.split(" ")){
                    coords[cordNum++] = Float.parseFloat(cord.trim());
                }
            }

            drawOrder = new short[numFaces*3];
            int orderNum = 0;

            // parse faces
            for (int i=0; i < numFaces; i++){
                line = reader.readLine();
                String order[] = line.split(" ");
                for (int j=1; j < 4; j++){ // descartamos el primer número que siempre va a ser 3 para triangulos
                    drawOrder[orderNum++] = Short.parseShort(order[j].trim());
                }
            }

            // Pintamos de blanco
            color = new float[numVertex*COLORS_PER_VERTEX];
            for (int i=0; i < numVertex*COLORS_PER_VERTEX; i++) {
                color[i] = 1.0f;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        float a[] = new float[3];
        float b[] = new float[3];
        float c[] = new float[3];
        float v1[] = new float[3];
        float v2[] = new float[3];
        float normals[] = new float[drawOrder.length * 3];

        // Calculate normal vectors
        for (int i=0; i < drawOrder.length; i+=3){
            // set cord for first point of the face
            a[0] = coords[drawOrder[i] * COORDS_PER_VERTEX];
            a[1] = coords[drawOrder[i] * COORDS_PER_VERTEX + 1];
            a[2] = coords[drawOrder[i] * COORDS_PER_VERTEX + 2];
            // set cord for second point of the face
            b[0] = coords[drawOrder[i+1] * COORDS_PER_VERTEX];
            b[1] = coords[drawOrder[i+1] * COORDS_PER_VERTEX + 1];
            b[2] = coords[drawOrder[i+1] * COORDS_PER_VERTEX + 2];
            // set cord for third point of the face
            c[0] = coords[drawOrder[i+2] * COORDS_PER_VERTEX];
            c[1] = coords[drawOrder[i+2] * COORDS_PER_VERTEX + 1];
            c[2] = coords[drawOrder[i+2] * COORDS_PER_VERTEX + 2];

            // calculate vector ac
            v1[0] = c[0] - a[0];
            v1[1] = c[1] - a[1];
            v1[2] = c[2] - a[2];

            // calculate vector bc
            v2[0] = c[0] - b[0];
            v2[1] = c[1] - b[1];
            v2[2] = c[2] - b[2];

            // Multiply both vectors to get the normal vector of the face
            normals[i] = a[1] * b[2] - a[2] * b[1];
            normals[i + 1] = (a[0] * b[2] - a[2] * b[0]) * -1;
            normals[i + 2] = (a[0] * b[1] - a[1] * b[0]) * -1;
        }

        /*for (int i=0; i<18; i++) {
            Log.d("NORMALS", normals[i]+"");
        }*/

        // aqui guardamos una normal por vertice (3 coordenadas cada 1)
        float[] vertexNormals = new float[coords.length /* / COORDS_PER_VERTEX * 3 */];

        // Calculate vertex normals by the average of contiguous face normals.
        for (int i=0; i < coords.length; i += COORDS_PER_VERTEX) {
            float[] vertexNormal = new float[3];
            int sum = 0;
            for (int j=0; j < drawOrder.length; j++) {
                // buscamos las caras que contienen el vertice "i" y en caso afirmativo lo sumamos para hacer la media
                if (drawOrder[j] == i/3) {
                    int t = j % 3; // primera coordenada de la cara
                    vertexNormal[0] += normals[j-t];
                    vertexNormal[1] += normals[j-t+1];
                    vertexNormal[2] += normals[j-t+2];
                    sum++;
                }
            }
            vertexNormals[i] = vertexNormal[0] / sum;
            vertexNormals[i+1] = vertexNormal[1] / sum;
            vertexNormals[i+2] = vertexNormal[2] / sum;
        }

        /*for (int i=0; i<3; i++) {
            Log.d("VERTEX NORMALS", vertexNormals[i]+"");
        }*/

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        vertexBuffer.put(coords);
        vertexBuffer.position(0);


        //Inicializar vertex byte buffer para color
        ByteBuffer bbc = ByteBuffer.allocateDirect( color.length*4);
        bbc.order(ByteOrder.nativeOrder());
        colorBuffer = bbc.asFloatBuffer();
        colorBuffer.put(color);
        colorBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // initialize byte buffer for the normals list
        ByteBuffer bbn = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                coords.length * 4);
        bbn.order(ByteOrder.nativeOrder());
        normalBuffer = bbn.asFloatBuffer();
        normalBuffer.put(vertexNormals);
        normalBuffer.position(0);


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
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        // Handler para pasar el color al shader
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        // Handler para pasar las normales al shader
        mNormalHandler = GLES20.glGetAttribLocation(mProgram, "aNormal");
        GLES20.glEnableVertexAttribArray(mNormalHandler);
        GLES20.glVertexAttribPointer(mNormalHandler, 3, GLES20.GL_FLOAT, false, normalStride, normalBuffer);

        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPos");
        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, 1.0f, 0.5f, 0.7f);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // get handle to shape's transformation matrix
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // La matriz model * view es la misma que la matriz view, por que el objeto esta en el centro y no sufre ninguna transformacion
        float[] mvMatrix = new float[16];
        Matrix.setLookAtM(mvMatrix, 0, 0, 0, -4, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
