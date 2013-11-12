package com.logicify.apps.previewFilter;

import android.content.Context;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Camera preview class.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context)
    {
        super(context); // Always necessary
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (null == mCamera)
        {
            mCamera = Camera.open();
        }

        try
        {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
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
        try
        {
            mCamera.stopPreview();
        } catch (Exception e)
        {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters mParams = mCamera.getParameters();

        Camera.Size bestSize = getBestPreviewSizeToScreen(mParams.getSupportedPreviewSizes());

        mParams.setPreviewSize(bestSize.width, bestSize.height);
        mCamera.setPreviewCallback(this);
        mCamera.setParameters(mParams);

        // start preview with new settings
        try
        {
            mCamera.startPreview();

        } catch (Exception e)
        {
            Log.e(Util.TAG, "Error starting camera preview: " + e.getMessage(), e);
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
        if (null == mCamera)
        {
            return;
        }
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        // do nothing for now
    }
}

