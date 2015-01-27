package com.example.android.opengl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import static android.util.FloatMath.sqrt;

public class Figure
{
    private static final String TAG = "Figure";

    protected float[] mModelMatrix = new float[16];

    protected float[] mViewMatrix = new float[16];

    protected float[] mProjectionMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];

    protected float[] mLightModelMatrix = new float[16];

    /** Store our model data in a float buffer. */
    private final FloatBuffer mPositions;
    private final ShortBuffer mDrawOrder;
    private final FloatBuffer mColors;
    private final FloatBuffer mNormals;

    /** This will be used to pass in the transformation matrix. */
    protected int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    protected int mMVMatrixHandle;

    /** This will be used to pass in the light position. */
    protected int mLightPosHandle;

    /** This will be used to pass in model position information. */
    protected int mPositionHandle;

    /** This will be used to pass in model color information. */
    protected int mColorHandle;

    /** This will be used to pass in model normal information. */
    protected int mNormalHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** How many bytes per ahort. */
    private final int mBytesPerShort = 2;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    /** Size of the normal data in elements. */
    private final int mNormalDataSize = 3;

    protected final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    protected final float[] mLightPosInWorldSpace = new float[4];

    protected final float[] mLightPosInEyeSpace = new float[4];

    /** This is a handle to our per-vertex cube shading program. */
    protected int mPerVertexProgramHandle;

    /** This is a handle to our light point program. */
    protected int mPointProgramHandle;
    
    private float[] positionData;
    private float[] colorData;
    private float[] normalData;
    private short[] drawOrderData;

    public Figure()
    {
        // Define points for a figure.
        parseFile();

        // Initialize the buffers.
        mPositions = ByteBuffer.allocateDirect(positionData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPositions.put(positionData).position(0);

        mDrawOrder = ByteBuffer.allocateDirect(drawOrderData.length * mBytesPerShort)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mDrawOrder.put(drawOrderData).position(0);

        mColors = ByteBuffer.allocateDirect(colorData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(colorData).position(0);

        mNormals = ByteBuffer.allocateDirect(normalData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormals.put(normalData).position(0);
    }

    protected String getVertexShader()
    {
        final String vertexShader =
                          "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
                        + "uniform mat4 u_MVMatrix;       \n"		// A constant representing the combined model/view matrix.
                        + "uniform vec3 u_LightPos;       \n"	    // The position of the light in eye space.

                        + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
                        + "attribute vec3 a_Normal;       \n"		// Per-vertex normal information we will pass in.

                        + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.

                        + "void main()                    \n"
                        + "{                              \n"
                        // Transform the vertex into eye space.
                        + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
                        // Transform the normal's orientation into eye space.
                        + "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
                        + "   float distance = length(u_LightPos - modelViewVertex);             \n"
                        // Get a lighting direction vector from the light to the vertex.
                        + "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
                        + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);       \n"
                        // Attenuate the light based on distance.
                        + "   diffuse = diffuse * (1.0 / (1.0 + (0.1 * distance * distance )));  \n"
                        + "   v_Color = a_Color * diffuse;                                       \n"
                        + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                        + "}                                                                     \n";

        return vertexShader;
    }

    protected String getFragmentShader()
    {
        final String fragmentShader =
                        "varying vec4 v_Color;          \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = v_Color;     \n"
                        + "}                              \n";

        return fragmentShader;
    }



    /**
     * Draws a cube.
     */
    protected void drawFigure()
    {
        // Pass in the position information
        mPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mPositions);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        mColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mColors);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the normal information
        mNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, mNormals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Draw the cube.
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrderData.length,
                GLES20.GL_UNSIGNED_SHORT, mDrawOrder);
    }

    /**
     * Draws a point representing the position of the light.
     */
    protected void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    protected int compileShader(final int shaderType, final String shaderSource)
    {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0)
        {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    protected int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes)
    {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null)
            {
                final int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    private void parseFile() {
        // parse file
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("res/raw/arm.off");
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

            positionData = new float[numVertex*mPositionDataSize];
            int cordNum = 0;

            // parse vertex
            for (int i=0; i < numVertex; i++){
                line = reader.readLine();
                for (String cord : line.split(" ")){
                    positionData[cordNum++] = Float.parseFloat(cord.trim());
                }
            }

            drawOrderData = new short[numFaces*3];
            int orderNum = 0;

            // parse faces
            for (int i=0; i < numFaces; i++){
                line = reader.readLine();
                String order[] = line.split(" ");
                for (int j=1; j < 4; j++){ // descartamos el primer número que siempre va a ser 3 para triangulos
                    drawOrderData[orderNum++] = Short.parseShort(order[j].trim());
                }
            }

            // Pintamos de blanco
            colorData = new float[numVertex*4];
            for (int i=0; i < numVertex*4; i++) {
                colorData[i] = 1.0f;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        float a[] = new float[3];
        float b[] = new float[3];
        float c[] = new float[3];
        float v1[] = new float[3];
        float v2[] = new float[3];
        float normals[] = new float[drawOrderData.length * 3];

        // Calculate normal vectors
        for (int i=0; i < drawOrderData.length; i+=3){
            // set cord for first point of the face
            a[0] = positionData[drawOrderData[i] * mPositionDataSize];
            a[1] = positionData[drawOrderData[i] * mPositionDataSize + 1];
            a[2] = positionData[drawOrderData[i] * mPositionDataSize + 2];
            // set cord for second point of the face
            b[0] = positionData[drawOrderData[i+1] * mPositionDataSize];
            b[1] = positionData[drawOrderData[i+1] * mPositionDataSize + 1];
            b[2] = positionData[drawOrderData[i+1] * mPositionDataSize + 2];
            // set cord for third point of the face
            c[0] = positionData[drawOrderData[i+2] * mPositionDataSize];
            c[1] = positionData[drawOrderData[i+2] * mPositionDataSize + 1];
            c[2] = positionData[drawOrderData[i+2] * mPositionDataSize + 2];

            // calculate vector ab
            v1[0] = b[0] - a[0];
            v1[1] = b[1] - a[1];
            v1[2] = b[2] - a[2];

            // calculate vector ac
            v2[0] = c[0] - a[0];
            v2[1] = c[1] - a[1];
            v2[2] = c[2] - a[2];

            // Multiply both vectors to get the normal vector of the face
            normals[i] = v1[1] * v2[2] - v1[2] * v2[1];
            normals[i + 1] = v1[2] * v2[0] - v1[0] * v2[2];
            normals[i + 2] = v1[0] * v2[1] - v1[1] * v2[0];

            // make it unitary
            float length = sqrt(normals[i]*normals[i] + normals[i+1]*normals[i+1] + normals[i+2]*normals[i+2]);
            normals[i] /= length;
            normals[i+1] /= length;
            normals[i+2] /= length;
        }

        /*for (int i=0; i<18; i++) {
            Log.d("NORMALS", normals[i]+"");
        }*/

        // aqui guardamos una normal por vertice (3 coordenadas cada 1)
        normalData = new float[positionData.length /* / positionData_PER_VERTEX * 3 */];

        // Calculate vertex normals by the average of contiguous face normals.
        for (int i=0; i < positionData.length; i += mPositionDataSize) {
            float[] vertexNormal = new float[3];
            int sum = 0;
            for (int j=0; j < drawOrderData.length; j++) {
                // buscamos las caras que contienen el vertice "i" y en caso afirmativo lo sumamos para hacer la media
                if (drawOrderData[j] == i/3) {
                    int t = j % 3; // primera coordenada de la cara
                    vertexNormal[0] += normals[j-t];
                    vertexNormal[1] += normals[j-t+1];
                    vertexNormal[2] += normals[j-t+2];
                    sum++;
                }
            }
            normalData[i] = vertexNormal[0] / sum;
            normalData[i+1] = vertexNormal[1] / sum;
            normalData[i+2] = vertexNormal[2] / sum;
        }

        /*for (int i=0; i<3; i++) {
            Log.d("VERTEX NORMALS", vertexNormals[i]+"");
        }*/
    }
}