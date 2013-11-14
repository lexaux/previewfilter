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
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, TextureView.SurfaceTextureListener
{
    private SurfaceHolder mHolder;
    private Camera mCamera;

    //    private Camera.Size previewSize = null;
    private int previewSizePixels;
    private int[] previewBuffer;
    private TextureView internalTextureView;

    public CameraPreview(Context context, TextureView internalTextureView)
    {
        super(context); // Always necessary
        this.internalTextureView = internalTextureView;
        internalTextureView.setSurfaceTextureListener(this);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        synchronized (this)
        {
            this.setWillNotDraw(false);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null)
        {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
//        try
//        {
//            mCamera.stopPreview();
//        } catch (Exception e)
//        {
//            // ignore: tried to stop a non-existent preview
//        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
//
//        this.previewSize = bestSize;
//
//        mParams.setPreviewSize(bestSize.width, bestSize.height);
//        mCamera.setPreviewCallback(this);
//        mCamera.setParameters(mParams);
//
//        // start preview with new settings
//        try
//        {
//            mCamera.setPreviewDisplay(mHolder);
//            mCamera.startPreview();
//
//        } catch (Exception e)
//        {
//            Log.e(Util.TAG, "Error starting camera preview: " + e.getMessage(), e);
//        }
    }

    /**
     * Method tries to determine the most appropriate preview size for the current screen size. Since we'll be occupying
     * the whole device screen, we firstly search for exact pixel-match. If there isn't one, we search for the one with
     * the same ratio.
     *
     * @param sizes camera-supported preview sizes
     * @return the best-fit size
     */
    private Camera.Size getBestPreviewSizeToScreen(List<Camera.Size> sizes)
    {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        //try to get screen-exact preview size
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes)
        {
            if (size.width == metrics.widthPixels && size.height == metrics.heightPixels)
            {
                bestSize = size;
            }
        }
        // if not, just the first with the right ratio
        if (bestSize == null)
        {
            float ratio = metrics.heightPixels / metrics.widthPixels;
            for (Camera.Size size : sizes)
            {
                float sizeRatio = size.height / size.width;
                if (Math.abs(sizeRatio - ratio) > 0.001)
                {
                    bestSize = size;
                }
            }
        }
        if (bestSize == null)
        {
            bestSize = sizes.get(0);
        }
        return bestSize;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
//        synchronized (this)
//        {
//            if (null == mCamera)
//            {
//                return;
//            }
//            mCamera.stopPreview();
//            mCamera.setPreviewCallback(null);
//
//            mCamera.release();
//            mCamera = null;
//        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        Log.d("Camera", "Got a camera frame");

        Canvas c = null;

        if (mHolder == null)
        {
            return;
        }

        try
        {
            synchronized (mHolder)
            {
                c = mHolder.lockCanvas(null);
                if (c == null)
                {
                    return;
                }

                Camera.Size size = camera.getParameters().getPreviewSize();
                applyGrayScale(previewBuffer, data, size.width, size.height);
                c.drawBitmap(previewBuffer, 0, size.width, 0, 0, size.width, size.height, false, new Paint());

                Log.d("SOMETHING", "Got Bitmap");
            }
        } finally
        {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (c != null)
            {
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
    public static void applyGrayScale(int[] pixels, byte[] data, int width, int height)
    {
        int p;
        int size = width * height;
        for (int i = 0; i < size; i++)
        {
            p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p << 16;//p << 8;// | p;
        }
    }

    // Surface texture details.
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        mCamera = Camera.open();
        internalTextureView.setVisibility(View.INVISIBLE);

        try
        {
            Camera.Parameters mParams = mCamera.getParameters();
            Camera.Size bestSize = getBestPreviewSizeToScreen(mParams.getSupportedPreviewSizes());

            this.previewSizePixels = width * height;
            this.previewBuffer = new int[this.previewSizePixels];

            mParams.setPreviewSize(bestSize.width, bestSize.height);
            mCamera.setParameters(mParams);

            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (IOException ioe)
        {
            // Something bad happened
        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

