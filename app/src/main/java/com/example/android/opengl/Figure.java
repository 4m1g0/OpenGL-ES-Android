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

public class Figure
{
    private static final String TAG = "Figure";

    protected float[] mModelMatrix = new float[16];

    protected float[] mViewMatrix = new float[16];

    protected float[] mProjectionMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];

    /** Store our model data in a float buffer. */
    private final FloatBuffer mPositions;
    private final ShortBuffer mDrawOrder;
    private final FloatBuffer mColors;
    private final FloatBuffer mTexCoordinates;

    /** This will be used to pass in the transformation matrix. */
    protected int mMVPMatrixHandle;

    /** This will be used to pass in model position information. */
    protected int mPositionHandle;

    /** This will be used to pass in model color information. */
    protected int mColorHandle;;

    protected int mTexCoordinateHandle;

    public int mTextureUniformHandle;

    public int mTextureDataHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** How many bytes per ahort. */
    private final int mBytesPerShort = 2;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    /** Size of the normal data in elements. */
    private final int mTexturePositionDataSize = 2;

    /** This is a handle to our per-vertex cube shading program. */
    protected int mPerVertexProgramHandle;
    
    private float[] positionData;
    private float[] texturePositionData;
    private float[] colorData;
    private short[] drawOrderData;

    /**
     * Initialize the model data.
     */
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

        mTexCoordinates = ByteBuffer.allocateDirect(texturePositionData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoordinates.put(texturePositionData).position(0);
    }

    protected String getVertexShader()
    {
        final String vertexShader =
                  "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
                + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
                + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
                + "attribute vec2 a_TexCoordinate;\n"      // Per-vertex texture coordinate information we will pass in.

                + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
                + "varying vec2 v_TexCoordinate;  \n"		// This will be passed into the fragment shader.

                + "void main()                    \n"
                + "{                              \n"
                // Pass through the texture coordinate.
                + "   v_TexCoordinate = a_TexCoordinate;                                      \n"
                + "   v_Color = a_Color;                                       \n"
                // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                + "}                                                                     \n";

        return vertexShader;
    }

    protected String getFragmentShader()
    {
        final String fragmentShader =
                          "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                        + "uniform sampler2D u_Texture;   \n"       // The input texture.
                        + "varying vec2 v_TexCoordinate;  \n"       // Interpolated texture coordinate per fragment.
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = (v_Color * texture2D(u_Texture, v_TexCoordinate));     \n"
                        + "}                              \n";

        return fragmentShader;
    }



    /**
     * Draws a figure.
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

        // Pass the texture information
        mTexCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTexCoordinateHandle, mTexturePositionDataSize, GLES20.GL_FLOAT, false,
                0, mTexCoordinates);

        GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);


        // Draw the cube.
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrderData.length,
                GLES20.GL_UNSIGNED_SHORT, mDrawOrder);
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
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
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("res/raw/helens.off");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while (!reader.readLine().trim().equals("STOFF")) ;
            line = reader.readLine();
            String token[] = line.split(" ");
            int numVertex = Integer.parseInt(token[0].trim());
            int numFaces = Integer.parseInt(token[1].trim());

            positionData = new float[numVertex*mPositionDataSize];
            texturePositionData = new float[numVertex*mTexturePositionDataSize];
            int cordNum = 0;
            int texNum = 0;

            // parse vertex
            for (int i=0; i < numVertex; i++){
                line = reader.readLine();
                String[] cord = line.split(" ");
                // Vertex coords
                for (int j=0; j < 3; j++){
                    positionData[cordNum++] = Float.parseFloat(cord[j].trim()) / 1000 - 0.132f;
                }
                // texture coords for each vertex
                for (int j=3; j < 5; j++){
                    texturePositionData[texNum++] = Float.parseFloat(cord[j].trim());
                }
            }

            drawOrderData = new short[numFaces*3*2]; // 2 triangulos por cara, dividimos los cuadrados en 2 triangulos
            int orderNum = 0;

            // parse faces
            for (int i=0; i < numFaces; i++){
                line = reader.readLine();
                String order[] = line.split(" ");
                for (int j=3; j > 0; j--){ // descartamos el primer número que siempre va a ser 4
                    drawOrderData[orderNum++] = Short.parseShort(order[j].trim());
                }
                // generamos el segundo triangulo del cuadrado a partir del ultimo vertice.
                drawOrderData[orderNum] = drawOrderData[orderNum-3];
                drawOrderData[orderNum+1] = drawOrderData[orderNum-1];
                drawOrderData[orderNum+2] = Short.parseShort(order[4].trim());
                orderNum += 3;
            }
            /*for (int i=0; i < 6; i++) {

                Log.d("ORDER", drawOrderData[i]+"");
            }*/

            // Pintamos de blanco
            colorData = new float[numVertex*4];
            for (int i=0; i < numVertex*4; i++) {
                colorData[i] = 1.0f;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}