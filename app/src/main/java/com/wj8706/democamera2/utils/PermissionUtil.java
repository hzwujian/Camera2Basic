package com.wj8706.democamera2.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by wj8706 on 2018/5/7.
 */

public class PermissionUtil {
    private Activity activity;

    public PermissionUtil(Activity activity) {
        this.activity = activity;
    }

    //判断当前是否获取到了权限
    public boolean hasPermissionGtranted(String[] permissions){
        for (String permission : permissions) {
            if(ContextCompat.checkSelfPermission(activity,permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,permission)) {
                return false;
            }
        }
        return true;
    }

    public void requestRequiredPermissions(final String[] permissins, int message, final int requestCode) {
        if (shouldShowRequestPermissionRationale(permissins)) {
            new AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity,permissins,requestCode);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity,permissins,requestCode);
        }
    }


}
