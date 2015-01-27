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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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

    private float mXAngle = 0;
    private float mYAngle = -60;
    private float mScale = 12f;


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

        // Set our up vector
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix.
        Matrix.setLookAtM(mFigure.mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = mFigure.getVertexShader();
        final String fragmentShader = mFigure.getFragmentShader();

        final int vertexShaderHandle = mFigure.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = mFigure.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mFigure.mPerVertexProgramHandle = mFigure.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_TexCoordinate"});

        mFigure.mTextureDataHandle = loadTexture();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mFigure.mPerVertexProgramHandle);

        // Set program handles for cube drawing.
        mFigure.mMVPMatrixHandle = GLES20.glGetUniformLocation(mFigure.mPerVertexProgramHandle, "u_MVPMatrix");
        mFigure.mPositionHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_Position");
        mFigure.mColorHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_Color");
        mFigure.mTexCoordinateHandle = GLES20.glGetAttribLocation(mFigure.mPerVertexProgramHandle, "a_TexCoordinate");
        mFigure.mTextureUniformHandle = GLES20.glGetUniformLocation(mFigure.mPerVertexProgramHandle, "u_Texture");

        // Draw figure.
        Matrix.setIdentityM(mFigure.mModelMatrix, 0);

        Matrix.translateM(mFigure.mModelMatrix, 0, 0f, 0f, -5.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, mYAngle, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mFigure.mModelMatrix, 0, mXAngle, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(mFigure.mModelMatrix, 0, mScale,mScale,mScale);
        mFigure.drawFigure();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix.
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
    public void setScale(float scale) {
        mScale = scale;
    }

    private int loadTexture()
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;	// No pre-scaling


            // Read in the resource
            Bitmap bitmap = BitmapFactory.decodeStream(this.getClass().getClassLoader().getResourceAsStream("res/raw/helenstex2.bmp"));
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(180);
            bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);


            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

}