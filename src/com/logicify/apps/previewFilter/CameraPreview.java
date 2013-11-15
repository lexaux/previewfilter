package com.logicify.apps.previewFilter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;
import java.util.List;

/**
 * Camera preview class.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, TextureView.SurfaceTextureListener {

    private static final String TAG = "CameraPreview";

    public static final int DEFAULT_CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SurfaceHolder mHolder;
    private Camera mCamera;

    //    private Camera.Size previewSize = null;
    private int previewSizePixels;
    private int[] previewBuffer;
    private TextureView internalTextureView;

    private volatile boolean drawingSurfaceReady = false;
    private volatile boolean receivingSurfaceTextureReady = false;

    private int width;
    private int height;

    public CameraPreview(Context context, TextureView internalTextureView) {
        super(context); // Always necessary
        this.internalTextureView = internalTextureView;

        internalTextureView.setSurfaceTextureListener(this);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            this.setWillNotDraw(false);
            this.drawingSurfaceReady = true;
            openRearCamera();
        }
    }

    private void openRearCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == DEFAULT_CAMERA_FACING) {
                mCamera = Camera.open(i);
            }
        }

        if (mCamera == null) {
            mCamera = Camera.open(0);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        this.width = width;
        this.height = height;
        tryStartPreview();
    }

    private void tryStartPreview() {
        if (!(drawingSurfaceReady && receivingSurfaceTextureReady)) {
            Log.i(Util.TAG, "Not starting preview, either texture or surface not yet ready");
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        try {
            mCamera.stopPreview();

            Camera.Parameters mParams = mCamera.getParameters();
            Camera.Size bestSize = getBestPreviewSizeToScreen(mParams.getSupportedPreviewSizes());

            this.previewSizePixels = bestSize.width * bestSize.height;
            this.previewBuffer = new int[this.previewSizePixels + 1];

            mParams.setPreviewSize(bestSize.width, bestSize.height);
            List<String> focusModes = mParams.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(mParams);

            mCamera.setPreviewTexture(internalTextureView.getSurfaceTexture());
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
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
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        //try to get screen-exact preview size
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            Log.d(TAG, "Trying preview size " + size.width + "x" + size.height);

            if (size.width == metrics.widthPixels && size.height == metrics.heightPixels) {
                bestSize = size;
                break;
            }
        }

        // if not, just the first with the right ratio
        if (bestSize == null) {
            Log.d(TAG, "best size == null; Using ratio to determine correct one");
            float ratio = (float) metrics.heightPixels / (float) metrics.widthPixels;
            Log.d(TAG, "Screen ratio = " + ratio);
            for (Camera.Size size : sizes) {
                float sizeRatio = (float) size.height / (float) size.width;
                Log.d(TAG, "Current size " + size.width + "x" + size.height + ", ratio = " + sizeRatio);
                if (Math.abs(sizeRatio - ratio) > 0.001f) {
                    bestSize = size;
                    break;
                }
            }
        }

        if (bestSize == null) {
            bestSize = sizes.get(0);
        }

        Log.d(TAG, "Best size " + bestSize.width + "x" + bestSize.height);

        return bestSize;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (this) {
            if (null == mCamera) {
                return;
            }
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("Camera", "Got a camera frame");

        Canvas c = null;

        if (mHolder == null) {
            return;
        }

        try {
            synchronized (mHolder) {
                c = mHolder.lockCanvas(null);
                if (c == null) {
                    return;
                }

                Camera.Size size = camera.getParameters().getPreviewSize();
                applyGrayScale(previewBuffer, data, size.width, size.height);
                c.drawBitmap(previewBuffer, 0, size.width, 0, 0, size.width, size.height, false, new Paint());

                Log.d("SOMETHING", "Got Bitmap");
            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (c != null) {
                mHolder.unlockCanvasAndPost(c);
            }
        }
    }

    /**
     * Converts YUV420 NV21 to Y888 (RGB8888). The grayscale image still holds 3 bytes on the pixel.
     *
     * @param pixels output array with the converted array o grayscale pixels
     * @param data   byte array on YUV420 NV21 format.
     * @param width  pixels width
     * @param height pixels height
     */
    public static void applyGrayScale(int[] pixels, byte[] data, int width, int height) {
        int p;
        int size = width * height;
        for (int i = 0; i < size; i++) {
            p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p << 16 | p << 8 | p;
        }
    }

    // Surface texture details.
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        internalTextureView.setVisibility(View.INVISIBLE);
        tryStartPreview();
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

