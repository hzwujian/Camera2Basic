package com.wj8706.democamera2.controler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import com.wj8706.democamera2.R;
import com.wj8706.democamera2.utils.FileUtil;
import com.wj8706.democamera2.utils.PermissionUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by wj8706 on 2018/5/7.
 */

public class CameraControler {
    private static String TAG = "wj-CameraControler";
    //屏幕方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static int SCREEN_ORIENTATION_0 = 0;
    private static int SCREEN_ORIENTATION_90 = 1;
    private static int SCREEN_ORIENTATION_180 = 2;
    private static int SCREEN_ORIENTATION_270 = 3;
    static {
        ORIENTATIONS.append(SCREEN_ORIENTATION_0,90);
        ORIENTATIONS.append(SCREEN_ORIENTATION_90,0);
        ORIENTATIONS.append(SCREEN_ORIENTATION_180,270);
        ORIENTATIONS.append(SCREEN_ORIENTATION_270,180);
    }
    //拍照权限请求码
    private static final int REQUEST_PICTURE_PERMISSION = 1;
    //拍照权限
    private static final String[] PICTURE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    private Activity mActivity;
    private Context mContext;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened! camera id = " + mCameraID);
            mCameraDevice = camera;
            startPreview();
            if (mOrientationListener != null){
                mOrientationListener.enable();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: error id = " + error);
        }
    };
    private CameraCaptureSession.CaptureCallback mCameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            Log.d(TAG, "onCaptureProgressed: ");
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            Log.d(TAG, "onCaptureCompleted: ");
            try {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            super.onCaptureCompleted(session, request, result);
        }
    };
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mCaptureRequest;
    //设置ImageReader的监听器，用于捕获capture图像帧
    private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: ");
            Image image = reader.acquireNextImage();
            mBackgroundHandler.post(new ImageSaver(image));
            mIsCaptureFinished = true;
        }
    };
    private OrientationEventListener mOrientationListener = null;
    private int mSreenRotation;
    private MediaRecorder mMediaRecorder = null;
    private ImageReader mImageReader;
    private Size mPreviewSize;
    private Size mVideoSize;
    private String[] mCameraIDs;
    private int mCameraID = -1;
    private int mWidth=0 , mHeight=0;
    private boolean mIsCaptureFinished = true;
    private boolean mIsRecordingVideo = false;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;


    public CameraControler(Context context, TextureView textureView, Activity activity) {
        this.mContext = context;
        this.mTextureView = textureView;
        this.mActivity = activity;
    }

    public boolean isCaptureFinished() {
        return mIsCaptureFinished;
    }

    public boolean isRecordingVideo() {
        return mIsRecordingVideo;
    }

    /**
     * 设置屏幕方向监听
     */
    public void setOrientationListener() {
        mOrientationListener = new OrientationEventListener(mActivity, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    //手机平放时，检测不到有效的角度
                    return;
                }
                //只检测是否有四个角度的改变
                if (orientation > 350 || orientation < 10) { //0度
                    mSreenRotation = SCREEN_ORIENTATION_0;
                } else if (orientation > 80 && orientation < 100) { //90度
                    mSreenRotation = SCREEN_ORIENTATION_270;
                } else if (orientation > 170 && orientation < 190) { //180度
                    mSreenRotation = SCREEN_ORIENTATION_180;
                } else if (orientation > 260 && orientation < 280) { //270度
                    mSreenRotation = SCREEN_ORIENTATION_90;
                } else {
                    return;
                }
            }
        };
    }

    /**
     * 获取拍照方向
     */
    private int getOrientation(int rotation) {
        return ORIENTATIONS.get(rotation);
    }


    /**
     * 初始化camera设备
     */
    private void initCamera() throws CameraAccessException {
        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(String.valueOf(mCameraID));
        Log.d(TAG, "initCamera, orientation = "+mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
        //获取摄像头支持的配置属性
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //获取摄像头支持的最大尺寸
        Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

        //创建一个ImageReader对象，用于获取摄像头的图像数据
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
        //设置获取图片的监听
        mImageReader.setOnImageAvailableListener(imageAvailableListener, null);
        //获取最佳的预览尺寸
        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), mWidth, mHeight, largest);
        //获取最佳的录像尺寸
        mVideoSize = getVideoSize(map.getOutputSizes(MediaRecorder.class));

        mMediaRecorder = new MediaRecorder();

        startBackgroundThread();
    }

    /**
     * 选择合适的preview尺寸
     *
     * @param choices
     * @param width
     * @param height
     * @param aspectRatio
     * @return
     */
    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        Log.d(TAG, "chooseOptimalSize: w = " + w + " h=" + h);
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new CompareSizesByArea());
        } else {
            //没有合适的预览尺寸
            return choices[0];
        }
    }

    // 为Size定义一个比较器Comparator
    class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * 判断传入的camera id是否合法
     *
     * @param id
     * @return
     */
    private boolean checkCameraID(int id) {
        for (String cameraStr : mCameraIDs) {
            if (Integer.parseInt(cameraStr) == id)
                return true;
        }
        return false;
    }

    /**
     * 打开camera
     *
     * @param cameraId
     */
    @SuppressLint("MissingPermission")
    public void openCamera(int cameraId) {
        //查看是否有相机权限，如果没有则请求int width, int heightcamera权限
        PermissionUtil permissionUtil = new PermissionUtil((Activity) mContext);
        if (!permissionUtil.hasPermissionGtranted(PICTURE_PERMISSIONS)) {
            permissionUtil.requestRequiredPermissions(PICTURE_PERMISSIONS, R.string.need_permissions, REQUEST_PICTURE_PERMISSION);
            return;
        }

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIDs = mCameraManager.getCameraIdList();
            //检查camera id是否合法
            if (!checkCameraID(cameraId)) {
                Log.e(TAG, "openCamera: illegal camera id!");
                showToast("不存在ID为" + cameraId + "的camera设备！");
                return;
            }
            mCameraID = cameraId;

            //初始化camera
            initCamera();
            mCameraManager.openCamera(String.valueOf(cameraId), mCameraStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动预览
     */
    public void startPreview() {
        Log.d(TAG, "startPreview: thread = " + mBackgroundThread + " handler = " + mBackgroundHandler);
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Log.e(TAG, "mCameraDevice == null or mMyTextureView is not available!");
            return;
        }
        closePreviewSession();
        mSurfaceTexture = mTextureView.getSurfaceTexture();
        //设置预览大小
        Log.d(TAG, "startPreview: width = " + mPreviewSize.getWidth() + " height = " + mPreviewSize.getHeight());
        mSurfaceTexture.setDefaultBufferSize(1280, 720);
        Surface surface = new Surface(mSurfaceTexture);
        List<Surface> surfaceList = new ArrayList<>();
        surfaceList.add(surface);
        surfaceList.add(mImageReader.getSurface());
        try {
            //设置ZSL拍照模式
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
            //mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置Preview的参数
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,0);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "startPreview onConfigured");
                    if (mCameraDevice == null) {
                        return;
                    }
                    try {
                        mPreviewSession = cameraCaptureSession;
                        mCaptureRequest = mPreviewRequestBuilder.build();
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    showToast("预览失败！");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * 停止预览session
     */
    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    /**
     * 关闭camera并释放资源
     */
    public void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mOrientationListener){
            mOrientationListener.disable();
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
        stopBackgroundThread();
    }

    /**
     * 获取当前打开的Camera ID
     */
    public int getOpenedCamera(){
        return mCameraID;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        mCameraID = mCameraID == 0 ? 1 : 0;
        closeCamera();
        openCamera(mCameraID);
    }

    /**
     * 拍照
     */
    public void takePicture() {
        Log.d(TAG, "enter takePicture()");
        if (null == mCameraDevice) {
            return;
        } else {
            try {
                Log.d(TAG, "takePicture start!");
                mIsCaptureFinished = false;
                //拍照参数设置
                CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //aptureRequestBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL,CaptureRequest.EDGE_MODE_ZERO_SHUTTER_LAG);
                captureRequestBuilder.addTarget(mImageReader.getSurface());

                //照片方向设置
                Log.d(TAG, "takePicture: mScreenRotation = " + mSreenRotation+" getOrientation = "+getOrientation(mSreenRotation));
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(mSreenRotation));

                //停止预览并拍照
                //mPreviewSession.stopRepeating();
                //mPreviewSession.abortCaptures();
                mPreviewSession.capture(captureRequestBuilder.build(), null, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *设置Video分辨率大小
     */
    private Size getVideoSize(Size[] sizes) {
        for (Size size : sizes) {
            Log.d(TAG, "getVideoSize: "+size.getWidth()+"x"+size.getHeight());
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {

                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return sizes[sizes.length - 1];
    }

    /**
     * 设置MediaRecorder
     */
    private void setUpMediaRecorder() throws IOException {
        Log.d(TAG, "setUpMediaRecorder!");
        //Video的设置必须遵循严格的顺序
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncodingBitRate(2500000);
        mMediaRecorder.setOrientationHint(getOrientation(mSreenRotation));
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(),mVideoSize.getHeight());
        //mMediaRecorder.setVideoSize(1280,720);
        Log.d(TAG, "setUpMediaRecorder, video size ="+mVideoSize.getWidth()+"x"+mVideoSize.getHeight());
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/"+"WJVID_"+System.currentTimeMillis();
        mMediaRecorder.setOutputFile(filePath + ".mp4");
        mMediaRecorder.prepare();
    }

    /**
     * Recording准备工作
     */
    private void prepareRecording() {
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(1280, 720);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

            List<Surface> surfaces = new ArrayList<>();
            Surface textureSurface = new Surface(surfaceTexture);
            surfaces.add(new Surface(surfaceTexture));
            mPreviewRequestBuilder.addTarget(textureSurface);

            Surface mediaSurface = mMediaRecorder.getSurface();
            surfaces.add(mediaSurface);
            mPreviewRequestBuilder.addTarget(mediaSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mPreviewSession = session;
                        mPreviewSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        showToast("创建RecordRequest失败！");
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 开始录像
     */
    public void startRecording() {
        prepareRecording();
        mMediaRecorder.start();
        mIsRecordingVideo = true;
    }

    /**
     * 停止录像
     */
    public void stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mIsRecordingVideo = false;
        startPreview();

    }

    /**
     * 显示toast信息
     */
    private void showToast(String text) {
        if (!text.equals("")) {
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 启动后台处理子线程线程（主要用于照片、视频保存等耗时操作）
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CAMERA_BACKGROUND");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台子线程
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 照片保存子线程
     */
    class ImageSaver implements Runnable {
        private Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            Log.d(TAG, "run: ");
            int saveResult = FileUtil.saveImage(image);
            if (saveResult == FileUtil.SAVE_SUCCESS) {
                showToast("Photo saved succeed!");
            }
        }
    }

}
