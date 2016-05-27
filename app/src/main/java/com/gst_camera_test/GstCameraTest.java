/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.gst_camera_test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.freedesktop.gstreamer.GStreamer;

public class GstCameraTest extends Activity implements SurfaceHolder.Callback {
    private native void nativeInit();
    private native void nativeFinalize();
    private native void nativePlay();
    private native void nativePause();
    private static native boolean classInit();
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();
    private long native_custom_data;

    private boolean playing;
    private int position;
    private int duration;

    private Bundle initialization_data;

    /* Called when the activity is first created. 
    @Override */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); 
            return;
        }

        setContentView(R.layout.main);

        ImageButton play = (ImageButton) this.findViewById(R.id.button_play);
        play.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                nativePlay();
            }
        });

        ImageButton pause = (ImageButton) this.findViewById(R.id.button_stop);
        pause.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                nativePause();
            }
        });

        SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);


        initialization_data = savedInstanceState;

        nativeInit();
    }
    
    protected void onSaveInstanceState (Bundle outState) {
        Log.d ("GStreamer", "Saving state, playing:" + playing);
        outState.putBoolean("playing", playing);
    }

    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();
    }

    /* Called from native code */
    private void onGStreamerInitialized () {
        if (initialization_data != null) {
            boolean should_play = initialization_data.getBoolean("playing");
            Log.i ("GStreamer", "Restoring state, playing:" + should_play);
            if (should_play) {
                nativePlay();
            } else {
                nativePause();
            }
        } else {
            nativePause();
        }
    }

    /* Called from native code */
    private void setCurrentState (int state) {
        Log.d ("GStreamer", "State has changed to " + state);
        switch (state) {
        case 1:
            setMessage ("NULL");
            break;
        case 2:
            setMessage ("READY");
            break;
        case 3:
            setMessage ("PAUSED");
            break;
        case 4:
            setMessage ("PLAYING");
            break;
        }
        playing = (state == 4);
    }

    /* Called from native code */
    private void setMessage(final String message) {
        final TextView tv = (TextView) this.findViewById(R.id.textview_message);
        runOnUiThread (new Runnable() {
          public void run() {
            tv.setText(message);
          }
        });
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("camera_test");
        classInit();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d("GStreamer", "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit (holder.getSurface());
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("GStreamer", "Surface created: " + holder.getSurface());
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("GStreamer", "Surface destroyed");
        nativeSurfaceFinalize ();
    }
}
