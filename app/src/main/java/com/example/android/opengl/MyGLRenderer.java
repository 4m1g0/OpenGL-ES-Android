/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.opengl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";
    //private Triangle mTriangle;
    //private Cube mCube;
    private Figure mFigure;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    /*private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix_x = new float[16];
    private final float[] mRotationMatrix_y = new float[16];
    private final float[] mRotationMatrix = new float[16];*/



    private float mXAngle;
    private float mYAngle;


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        mFigure = new Figure();

        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mFigure.mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = mFigure.getVertexShader();
        final String fragmentShader = mFigure.getFragmentShader();

        final int vertexShaderHandle = mFigure.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = mFigure.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mFigure.mPerVertexProgramHandle = mFigure.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_Normal"});

        // Define a simple shader program for our point.
        final String pointVertexShader =
                "uniform mat4 u_MVPMatrix;      \n"
                        +	"attribute vec4 a_Position;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix   \n"
                        + "               * a_Position;   \n"
                        + "   gl_PointSize = 5.0;         \n"
                        + "}                              \n";

        final String pointFragmentShader =
                "precision mediump float;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = vec4(1.0,    \n"
                        + "   1.0, 1.0, 1.0);             \n"
                        + "}                              \n";

        final int pointVertexShaderHandle = mFigure.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = mFigure.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mFigure.mPointProgramHandle = mFigure.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[] {"a_Position"});
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mFigure.mPerVertexProgramHandle);

        // Set program handles for cube drawing.
        mFigure.mMVPMatrixHandle = GLES20.glGetUniformLocation(mFigure.mPerVertexProgramHandle, "u_MVPMatrix");
        mFigure.mMVMatrixHandle = GLES20.glGetUniformLocation(mFigure.mPerVertexProgramHandle, "u_MVMatrix");
        mFigure.mLightPosHandle = GLES20.glGetUniformLocation(mFigure.mPerVertexProgramHandle, "u_LightPos");
        mFigure.mPositionHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_Position");
        mFigure.mColorHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_Color");
        mFigure.mNormalHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_Normal");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mFigure.mLightModelMatrix, 0);
        Matrix.translateM(mFigure.mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mFigure.mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mFigure.mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mFigure.mLightPosInWorldSpace, 0, mFigure.mLightModelMatrix, 0, mFigure.mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mFigure.mLightPosInEyeSpace, 0, mFigure.mViewMatrix, 0, mFigure.mLightPosInWorldSpace, 0);

        // Draw some cubes.
        Matrix.setIdentityM(mFigure.mModelMatrix, 0);
        Matrix.translateM(mFigure.mModelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);
        mFigure.drawCube();

        Matrix.setIdentityM(mFigure.mModelMatrix, 0);
        Matrix.translateM(mFigure.mModelMatrix, 0, -4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        mFigure.drawCube();

        Matrix.setIdentityM(mFigure.mModelMatrix, 0);
        Matrix.translateM(mFigure.mModelMatrix, 0, 0.0f, 4.0f, -7.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        mFigure.drawCube();

        Matrix.setIdentityM(mFigure.mModelMatrix, 0);
        Matrix.translateM(mFigure.mModelMatrix, 0, 0.0f, -4.0f, -7.0f);
        mFigure.drawCube();

        Matrix.setIdentityM(mFigure.mModelMatrix, 0);
        Matrix.translateM(mFigure.mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
        mFigure.drawCube();

        // Draw a point to indicate the light.
        GLES20.glUseProgram(mFigure.mPointProgramHandle);
        mFigure.drawLight();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mFigure.mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    /*public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }*/

    /**
    * Utility method for debugging OpenGL calls. Provide the name of the call
    * just after making it:
    *
    * <pre>
    * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
    *
    * If the operation is not successful, the check throws an error.
    *
    * @param glOperation - Name of the OpenGL call to check.
    */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getXAngle() {
        return mXAngle;
    }
    public float getYAngle() {
        return mYAngle;
    }


    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setXAngle(float angle) {
        mXAngle = angle;
    }
    public void setYAngle(float angle) {
        mYAngle = angle;
    }

}