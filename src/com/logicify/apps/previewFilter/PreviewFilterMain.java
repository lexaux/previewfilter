package com.logicify.apps.previewFilter;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class PreviewFilterMain extends Activity implements SurfaceTexture.OnFrameAvailableListener {
    private Camera mCamera;
    private MyGLSurfaceView glSurfaceView;
    private SurfaceTexture surface;
    private boolean mCameraRunning = false;
    private MyGL20Renderer renderer;
    public static final int DEFAULT_CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new MyGLSurfaceView(this);
        renderer = glSurfaceView.getRenderer();

        setContentView(glSurfaceView);
    }

    public void startCamera(int texture) {
        try {
            surface = new SurfaceTexture(texture);
            renderer.setSurface(surface);

            mCamera = openRearCamera();
            surface.setOnFrameAvailableListener(this);

            Camera.Size selectedPreviewSize = tryStartPreview();
            renderer.setPreviewTextureAspectRatio(selectedPreviewSize.width / selectedPreviewSize.height);

            mCameraRunning = true;
        } catch (Exception e) {
            Log.e("Camera", "Could not open camera, exiting", e);
            Toast.makeText(this, "Could not open the camera, can not proceed further.", Toast.LENGTH_LONG).show();
        }
    }


    public void toggleCamera() throws Exception {

        if (!mCameraRunning) {
            tryStartPreview();
            mCameraRunning = true;
        } else {
            mCamera.setPreviewTexture(null);
            mCamera.stopPreview();
            mCameraRunning = false;
        }
    }


    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSurfaceView.requestRender();
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            System.exit(0);
        }
    }


    private Camera.Size tryStartPreview() throws Exception {
        // set preview size and make any resize, rotate or
        // reformatting changes here
        try {
            mCamera.stopPreview();

            Camera.Parameters mParams = mCamera.getParameters();
            Camera.Size bestSize = getBestPreviewSizeToScreen(mParams.getSupportedPreviewSizes());


            mParams.setPreviewSize(bestSize.width, bestSize.height);
            List<String> focusModes = mParams.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(mParams);

            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();

            return bestSize;
        } catch (IOException ioe) {
            throw new Exception(ioe);
        }
    }


    /**
     * Method tries to determine the most appropriate preview size for the current screen size. Since we'll be occupying
     * the whole device screen, we firstly search for exact pixel-match. If there isn't one, we search for the one with
     * the same ratio.
     *
     * @param sizes camera-supported preview sizes
     * @return the best-fit size
     */
    private Camera.Size getBestPreviewSizeToScreen(List<Camera.Size> sizes) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        //try to get screen-exact preview size
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            Log.d(Util.TAG, "Trying preview size " + size.width + "x" + size.height);

            if (size.width == metrics.widthPixels && size.height == metrics.heightPixels) {
                bestSize = size;
                break;
            }
        }

        // if not, just the first with the right ratio
        if (bestSize == null) {
            Log.d(Util.TAG, "best size == null; Using ratio to determine correct one");
            float ratio = (float) metrics.heightPixels / (float) metrics.widthPixels;
            Log.d(Util.TAG, "Screen ratio = " + ratio);
            for (Camera.Size size : sizes) {
                float sizeRatio = (float) size.height / (float) size.width;
                Log.d(Util.TAG, "Current size " + size.width + "x" + size.height + ", ratio = " + sizeRatio);
                if (Math.abs(sizeRatio - ratio) > 0.001f) {
                    bestSize = size;
                    break;
                }
            }
        }

        if (bestSize == null) {
            bestSize = sizes.get(0);
        }

        Log.d(Util.TAG, "Best size " + bestSize.width + "x" + bestSize.height);

        return bestSize;
    }

    /**
     * This method opens rear camera and sets it to the appropriate member variable.
     * In case rear camera is not available (Nexus 7, for instance), it would get any camera.
     */
    private Camera openRearCamera() throws Exception {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == DEFAULT_CAMERA_FACING) {
                return Camera.open(i);
            }
        }

        if (mCamera == null) {
            return Camera.open(0);
        }

        throw new Exception("No camera found!");
    }


}
