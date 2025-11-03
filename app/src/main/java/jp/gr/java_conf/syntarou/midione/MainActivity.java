package jp.gr.java_conf.syntarou.midione;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;

import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.R;
import jp.gr.java_conf.syntarou.midione.databinding.ActivityMainBinding;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneBleCore;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneDeviceBle;
import jp.gr.java_conf.syntarou.midione.util.PermissionHelper;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;
import jp.gr.java_conf.syntarou.midione.ui.miditest.BleLauncher;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    BleLauncher _bleLauncher = null;
    MidiOne _midiOne;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _midiOne = MidiOne.getInstance();
        _midiOne.fullInitialize(this);

        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                doCommand(menuItem.getItemId());
                return false;
            }
        });
        try {
            PermissionHelper.initialize(this);
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }

        if (binding == null) {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setSupportActionBar(binding.myActionBar);
            setContentView(binding.getRoot());

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }
        try {
            _bleLauncher = new BleLauncher(this);
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if (_midiOne != null) {
            _midiOne.terminate();
            _midiOne = null;
        }
        super.onDestroy();
    }


    public BleLauncher getBleLauncher() {
        return _bleLauncher;
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onFoundBle(BluetoothDevice device) {
        try {
            OneDeviceBle deviceBle = _midiOne.getDriverBluetooth().addBluetoothDevice(device);
            deviceBle.openDevice();
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OneBleCore.SELECT_DEVICE_REQUEST_CODE:
                if (data == null) {
                    //cancel
                    return;
                }
                ScanResult result = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                if (result == null) {
                    return;
                }
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    onFoundBle(device);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean doCommand(int id) {
        if (id == android.R.id.home) {
            finish();
        }
        else if (id == R.id.menu_quit) {
            finish();
        }
        else if (id == R.id.menu_oss_license) {
            onOSSLicense();
        }
        else if (id == R.id.menu_license) {
            onLicense();
        }
        else {
            MessageLogger.getInstance().log( "Unknown menu");
            return false;
        }
        return true;
    }

    public void onOSSLicense() {
        //import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
        Intent intent =  new Intent(this, OssLicensesMenuActivity.class);
        startActivity(intent);
    }

    public void onLicense() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MidiOne-BLE License 0.1");
        builder.setMessage("Apache 2.0 License");
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}
