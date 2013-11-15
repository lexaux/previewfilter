package com.logicify.apps.previewFilter;

import android.app.Activity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.FrameLayout;

public class PreviewFilterMain extends Activity {
    /**
     * Called when the activity is first created.
     */
    private TextureView mTextureView;
    private CameraPreview preview;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);

        FrameLayout aLayout = new FrameLayout(this);
        aLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        aLayout.addView(mTextureView);

        preview = new CameraPreview(this, mTextureView);
        aLayout.addView(preview);

        setContentView(aLayout);
    }
}
