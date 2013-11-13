package com.logicify.apps.previewFilter;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;

public class PreviewFilterMain extends Activity implements TextureView.SurfaceTextureListener
{
    /**
     * Called when the activity is first created.
     */
    private Camera mCamera;
    private TextureView mTextureView;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        setContentView(mTextureView);
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        mCamera = Camera.open();
//        mTextureView.setVisibility(View.INVISIBLE);

        try
        {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe)
        {
            // Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        // Invoked every time there's a new Camera preview frame
    }

}
