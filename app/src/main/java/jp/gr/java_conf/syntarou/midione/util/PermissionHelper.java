package jp.gr.java_conf.syntarou.midione.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PermissionHelper {

    static PermissionHelper _instance = null;

    public static PermissionHelper getInstance() {
        return _instance;
    }

    public static void initialize(AppCompatActivity activity) {
        if (_instance == null) {
            _instance = new PermissionHelper(activity);
        }
    }

    AppCompatActivity _activity;

    public PermissionHelper(AppCompatActivity activity) {
        _activity = activity;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permission = {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
            };
            _listPermission = permission;
        } else {
            String[] permission = {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
            };
            _listPermission = permission;
        }
    }

    public static boolean isPermissoinGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPermissoinGranted(Context context, String[] listPermission) {
        for (String seek : listPermission) {
            if (isPermissoinGranted(context, seek) == false) {
                return false;
            }
        }
        return true;
    }

    public boolean isPermissoinGranted() {
        return isPermissoinGranted(_activity, _listPermission);
    }

    final String[] _listPermission;
    final int REQUEST_DEVICE_ID = 1;

    public void grantPermissionImpl() {
        if (false) {
            ActivityCompat.requestPermissions(_activity, _listPermission, REQUEST_DEVICE_ID);
        } else {
            ArrayList<String> need = new ArrayList<>();
            boolean needAlert = false;
            for (String seek : _listPermission) {
                if (isPermissoinGranted(_activity, seek) == false) {
                    //ユーザが過去に許可しなかった場合
                    if (ActivityCompat.shouldShowRequestPermissionRationale(_activity, seek)) {
                        needAlert = true;
                    } else {
                        need.add(seek);
                    }
                }
            }
            if (needAlert) {
                AlertDialog.Builder builder =  new AlertDialog.Builder(_activity);
                builder.setMessage("Need Permission");
                builder.setPositiveButton("Open Setting", (dialog, which) -> {
                    showSettingIntent();
                });
                builder.setNegativeButton("Dont Open", (dialog, which) -> {
                });
                builder.show();
                return;
            }
            if (need.size() == 0) {
                return;
            }
            String[] neededPermission = new String[need.size()];
            need.toArray(neededPermission);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //TODO
                ActivityCompat.requestPermissions(_activity, neededPermission, REQUEST_DEVICE_ID);
            } else {
                ActivityCompat.requestPermissions(_activity, neededPermission, REQUEST_DEVICE_ID);
            }
        }
    }


    void showSettingIntent() {
        Intent settingsIntent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:jp.gr.java_conf.syntarou.midione")
        );
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _activity.startActivity(settingsIntent);
    }
}
