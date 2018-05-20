package com.wj8706.democamera2;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;


import com.wj8706.democamera2.controler.CameraControler;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = "wj-CameraActivity";

    private CameraControler mCameraControler = null;
    private Activity mActivity = CameraActivity.this;

    private ImageButton mCaptureButton;
    private ImageButton mSwitchButton;
    private ImageButton mRecordButton;
    private TextureView mTextureView;
    private Chronometer mTimer;
    private LinearLayout mTimeLayout;

    private TextureView.SurfaceTextureListener mSurfaceListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (mTextureView.isAvailable()) {
                mCameraControler.openCamera(0);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView() {
        mCaptureButton = findViewById(R.id.cap_btn);
        mTextureView = findViewById(R.id.textureview);
        mSwitchButton = findViewById(R.id.switch_btn);
        mRecordButton = findViewById(R.id.record_btn);
        mTimeLayout = findViewById(R.id.time_layout);
        mTimer = findViewById(R.id.timer);

        mCaptureButton.setOnClickListener(this);
        mSwitchButton.setOnClickListener(this);
        mRecordButton.setOnClickListener(this);

        mRecordButton.setBackgroundResource(R.drawable.btn_shutter_video_default);
        mTimeLayout.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_btn:
                mCameraControler.switchCamera();
                if (mCameraControler.getOpenedCamera() == 0) {
                    mSwitchButton.setBackgroundResource(R.drawable.ic_switch_front);
                } else {
                    mSwitchButton.setBackgroundResource(R.drawable.ic_switch_back);
                }
                break;
            case R.id.cap_btn:
                if (mCameraControler.isCaptureFinished()) {
                    mCameraControler.takePicture();
                }
                break;

            case R.id.record_btn:
                if (!mCameraControler.isRecordingVideo()){
                    mCameraControler.startRecording();
                    mRecordButton.setBackgroundResource(R.drawable.btn_shutter_video_recording);
                    mCaptureButton.setBackgroundResource(R.drawable.btn_shutter_pressed_disabled);
                    mCaptureButton.setEnabled(false);
                    mSwitchButton.setEnabled(false);
                    mTimeLayout.setVisibility(View.VISIBLE);
                    mTimer.setBase(SystemClock.elapsedRealtime());
                    int hour = (int) ((SystemClock.elapsedRealtime()-mTimer.getBase()) / 1000 / 60);
                    mTimer.setFormat("0"+String.valueOf(hour)+":%s");
                    mTimer.start();
                } else {
                    mCameraControler.stopRecording();
                    mRecordButton.setBackgroundResource(R.drawable.btn_shutter_video_default);
                    mCaptureButton.setBackgroundResource(R.drawable.btn_shutter_default);
                    mCaptureButton.setEnabled(true);
                    mSwitchButton.setEnabled(true);
                    mTimer.stop();
                    mTimeLayout.setVisibility(View.GONE);
                }
        }

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "onResume: isAvailable!");
            mCameraControler.openCamera(0);
        } else {
            Log.d(TAG, "onResume: not available!");
            mCameraControler = new CameraControler(this,mTextureView,mActivity);
            mTextureView.setSurfaceTextureListener(mSurfaceListener);
            mCameraControler.setOrientationListener();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        if (mCameraControler.isRecordingVideo()) {
            mCameraControler.stopRecording();
            mRecordButton.setBackgroundResource(R.drawable.btn_shutter_video_default);
            mTimer.stop();
            mTimeLayout.setVisibility(View.GONE);
        }
        if (mCameraControler != null && mCameraControler.getOpenedCamera()!=-1) {
            mCameraControler.closeCamera();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraControler.closeCamera();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else {

        }
    }
}
