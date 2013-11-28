package com.logicify.apps.previewFilter;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class PreviewFilterMain extends Activity implements SurfaceTexture.OnFrameAvailableListener {
    private Camera mCamera;
    private MyGLSurfaceView glSurfaceView;
    private SurfaceTexture surface;
    private boolean mCameraRunning = false;
    MyGL20Renderer renderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new MyGLSurfaceView(this);
        renderer = glSurfaceView.getRenderer();

        setContentView(glSurfaceView);
    }

    public void startCamera(int texture) {
        surface = new SurfaceTexture(texture);
        renderer.setSurface(surface);

        mCamera = Camera.open();
        surface.setOnFrameAvailableListener(this);
        mCameraRunning = false;
        toggleCamera();
    }


    public void toggleCamera() {

        if (!mCameraRunning) {
            try {
                mCamera.setPreviewTexture(surface);
                mCamera.startPreview();
                mCameraRunning = true;
            } catch (IOException ioe) {
                Log.w("MainActivity", "CAM LAUNCH FAILED");
            }
        } else {
            try {
                mCamera.setPreviewTexture(null);
                mCamera.stopPreview();
                mCameraRunning = false;
            } catch (IOException ioe) {
                Log.w("MainActivity", "CAM LAUNCH FAILED");
            }
        }
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSurfaceView.requestRender();
    }

    @Override
    public void onPause() {
        mCamera.stopPreview();
        mCamera.release();
        System.exit(0);
    }

}
