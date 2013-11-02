package com.logicify.apps.previewFilter;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Camera preview class.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
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
        List<Camera.Size> previewSizes = mParams.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(previewSizes.size() - 1);
        mParams.setPreviewSize(previewSize.width, previewSize.height);
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

}

