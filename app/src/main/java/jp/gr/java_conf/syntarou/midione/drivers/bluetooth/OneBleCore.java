package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import jp.gr.java_conf.syntarou.midione.common.RunQueueThread;
import jp.gr.java_conf.syntarou.midione.MidiOne;

import java.util.List;
import java.util.Set;

public class OneBleCore {
    BluetoothAdapter _bluetoothAdapter;
    BluetoothManager _bluetoothManager;
    private RunQueueThread _thread = new RunQueueThread();

    public RunQueueThread getThread() {
        return _thread;
    }

    private MidiOne _one;

    public OneBleCore(MidiOne one) {
        Context context = one.getMidiContext();
        _one = one;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //APIレベル22以前の機種の場合の処理
            _bluetoothManager = context.getSystemService(BluetoothManager.class);
        } else  {
            //APIレベル23以降の機種の場合の処理
            _bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        _bluetoothAdapter = _bluetoothManager.getAdapter();
    }

    public void addAllBondedDevices(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (isBleEnabled() == false) {
            return;
        }


        Set<BluetoothDevice> already = _bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : already) {
            ParcelUuid[] uuid = device.getUuids();
            if (uuid == null) {
                continue;
            }
            for (int i = 0; i < uuid.length; ++i) {
                if (MidiOne.getInstance().MIDI_SERVICE.equals(uuid[i])) {
                    _one.getDriverBluetooth().addBluetoothDevice(device);
                    break;
                }
            }
        }
        BluetoothManager manager = _one.getOneBleCore().getBluetoothManager();
        if (manager != null) {
            List<BluetoothDevice> already2  = manager.getConnectedDevices(BluetoothProfile.GATT);
            if (already2 != null) {
                for (BluetoothDevice device : already2) {
                    ParcelUuid[] uuid = device.getUuids();
                    if (uuid == null) {
                        continue;
                    }
                    for (int i = 0; i < uuid.length; ++i) {
                        if (MidiOne.getInstance().MIDI_SERVICE.equals(uuid[i])) {
                            _one.getDriverBluetooth().addBluetoothDevice(device);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void startScan() {

    }

    public void stopScanDevice() {

    }

    public boolean isBleSupported() {
        if (_bluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean isBleEnabled() {
        if (_bluetoothAdapter == null) {
            return false;
        }
        return _bluetoothAdapter.isEnabled();
    }

    public boolean isBlePeripheralSupported() {
        if (_bluetoothAdapter == null) {
            return false;
        }
        return _bluetoothAdapter.isMultipleAdvertisementSupported();
    }


    public static final int REQUEST_CODE_BLUETOOTH_ENABLE = 0xb1e;
    public static final int SELECT_DEVICE_REQUEST_CODE = 0x5e1e;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void launchEnableBluetooth(Activity activity) {
        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLUETOOTH_ENABLE);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return _bluetoothAdapter;
    }

    public BluetoothManager getBluetoothManager() { return _bluetoothManager; }

    public boolean isBleSupported(@NonNull final Context context) {
        try {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) == false) {
                return false;
            }

            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

            final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                return true;
            }
        } catch (final Throwable ignored) {
            // ignore exception
        }
        return false;
    }

    public boolean isBlePeripheralSupported(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        final BluetoothAdapter bluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isMultipleAdvertisementSupported();
    }

    public boolean isBluetoothEnabled(@NonNull final Context context) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            return false;
        }

        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void enableBluetooth(@NonNull final Activity activity) {
        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLUETOOTH_ENABLE);
    }
}
