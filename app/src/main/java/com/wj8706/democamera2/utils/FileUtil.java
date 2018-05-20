package com.wj8706.democamera2.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wj8706 on 2018/5/7.
 */

public class FileUtil {
    private static String TAG = "wj-FileUtil";
    public static int SAVE_SUCCESS = 0;
    public static int NOSDCARD_ERROR = 1;
    public static int SAVEFILE_ERROR = 2;

    /**
     * 保存照片数据到SD卡
     *
     * @param image
     */
    public static int saveImage(Image image) {
        //检查手机是否有sdcard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            return NOSDCARD_ERROR;
        }
        // 获取捕获的照片数据
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        //手机拍照都是存到这个路径
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/";
        String picturePath = "WJIMG_"+System.currentTimeMillis() + ".jpg";
        File file = new File(filePath, picturePath);
        try {
            //存到本地相册
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.close();
            return SAVE_SUCCESS;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return SAVEFILE_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            return SAVEFILE_ERROR;
        } finally {
            Log.d(TAG, "saveImage: finally");
            image.close();
        }
    }
}
