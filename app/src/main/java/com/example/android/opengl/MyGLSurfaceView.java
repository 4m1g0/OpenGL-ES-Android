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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends GLSurfaceView  {
    private final MyGLRenderer mRenderer;
    private int rotate_threshold = 30;
    private ScaleGestureDetector mScaleDetector;



    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer();
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

   @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
       mScaleDetector.onTouchEvent(e);
        if (!mScaleDetector.isInProgress()) {
            float x = e.getX();
            float y = e.getY();

            float x_pos = mRenderer.getXAngle() % 360;
            if (x_pos < 0)
                x_pos = 360 + x_pos;
            float y_pos = mRenderer.getYAngle() % 360;
            if (y_pos < 0)
                y_pos = 360 + y_pos;

            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;
                    //dx = dx * -1;
                    dy = dy * -1;

                    // Establecemos un threshold en funcion de la posicion del cubo en el eje x
                    // Si se supera, hacemos que gire en función de si estamos tocando el cubo en
                    // la parte izquierda o derecha de la pantalla
                    if (x_pos > rotate_threshold && x_pos < 360 - rotate_threshold) {
                        if (x_pos < 180 - rotate_threshold) {
                            if (x < getWidth() / 2)
                                dy = dy * -1;
                        } else if (x_pos < 180 + rotate_threshold) {
                            dy = dy * -1;
                        } else {
                            if (x > getWidth() / 2)
                                dy = dy * -1;
                        }
                    }

                    //Establecemos el nuevo angulo de giro en el eje X y en el eje Y
                    mRenderer.setYAngle(
                            mRenderer.getYAngle() +
                                    //((dx + dy) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
                                    ((dy) * TOUCH_SCALE_FACTOR));

                    mRenderer.setXAngle(
                            mRenderer.getXAngle() +
                                    //((dx + dy) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
                                    ((dx) * TOUCH_SCALE_FACTOR));
                    requestRender();
            }

            mPreviousX = x;
            mPreviousY = y;
        }
        return true;
    }


    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            detector.getScaleFactor();
            return true;
        }
    }


}
