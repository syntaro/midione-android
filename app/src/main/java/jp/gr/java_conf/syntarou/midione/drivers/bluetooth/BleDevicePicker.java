package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.companion.AssociatedDevice;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.MacAddress;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.MainActivity;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.util.BleMidiDeviceUtils;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;

public class BleDevicePicker {
    OneBleCore _oneBle;

    public BleDevicePicker(OneBleCore params) {
        _oneBle = params;
        _useCompanionDeviceSetup = true;
    }

    boolean _useCompanionDeviceSetup = false;

    public boolean launch(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && _useCompanionDeviceSetup) {
            final CompanionDeviceManager.Callback associationCallback = new CompanionDeviceManager.Callback() {
                @Override
                public void onAssociationPending(@NonNull IntentSender intentSender) {
                    MessageLogger.getInstance().log("onAssociationPending");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            Intent intent = new Intent();
                            BroadcastReceiver receiver = new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    BluetoothDevice device = intent.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                                    Log.e("**********1", "" + device);
                                }
                            };
                            IntentSender.OnFinished finished = new IntentSender.OnFinished() {
                                @Override
                                public void onSendFinished(IntentSender IntentSender, Intent intent,
                                                           int resultCode, String resultData, Bundle resultExtras) {
                                    BluetoothDevice device = intent.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                                    Log.e("**********2", "" + device);
                                }
                            };
                            /*
                            if (_oneBle.getBluetoothAdapter().isDiscovering()) { //ADDED 2025/4/25
                                _oneBle.getBluetoothAdapter().cancelDiscovery();
                            }*/
                            IntentFilter filter = new IntentFilter();
                            context.registerReceiver(receiver, filter);
                            intentSender.sendIntent(context, OneBleCore.SELECT_DEVICE_REQUEST_CODE, intent, finished, null);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(AppConstant.MidiOneTag, e.getMessage(), e);
                        }
                    } else {
                    }
                    // calls onDeviceFound
                    super.onAssociationPending(intentSender);
                }

                @Override
                @Deprecated
                public void onDeviceFound(final IntentSender intentSender) {
                    MessageLogger.getInstance().log("onDeviceFound " + intentSender);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        try {
                            ((Activity) context).startIntentSenderForResult(intentSender, OneBleCore.SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(AppConstant.MidiOneTag, e.getMessage(), e);
                        }
                    }
                }

                @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
                @Override
                public void onAssociationCreated(@NonNull AssociationInfo associationInfo) {
                    MessageLogger.getInstance().log("onAssociationCreated");
                    try {
                        String name = String.valueOf(associationInfo.getDisplayName());
                        MacAddress address = associationInfo.getDeviceMacAddress();
                        BluetoothDevice device = null;
                        if (address != null) {
                            device = _oneBle.getBluetoothAdapter().getRemoteDevice(address.toByteArray());
                        } else {
                            AssociatedDevice ascDevice = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                ascDevice = associationInfo.getAssociatedDevice();
                                ScanResult scanResult = ascDevice.getBleDevice();
                                device = scanResult.getDevice();
                            }
                        }

                        if (device == null) {
                            return;
                        }
                        if (name == null) {
                            name = device.getName();
                        }
                        if (_oneBle.getBluetoothAdapter().isDiscovering()) { //ADDED 2025/4/25
                            _oneBle.getBluetoothAdapter().cancelDiscovery();
                        }
                        if (context instanceof MainActivity) {
                            MainActivity activity = (MainActivity) context;
                            activity.onFoundBle(device);
                        } else {
                            MidiOne manager = MidiOne.getInstance();
                            OneDeviceBle info = new OneDeviceBle(manager.getDriverBluetooth(), device, name);
                            manager.getDriverBluetooth().addDevice(info);
                        }
                    } catch (Throwable ex) {
                        Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                    }
                }

                @Override
                public void onFailure(final CharSequence error) {
                    MessageLogger.getInstance().log("onFailure");
                    OneHelper.runOnUiThread(()->{
                        Toast.makeText(context, "Need Clear Cache", Toast.LENGTH_SHORT).show();
                    });
                }
            };

            try {
                final AssociationRequest associationRequest = BleMidiDeviceUtils.getBleMidiAssociationRequest(context);
                final CompanionDeviceManager deviceManager = context.getSystemService(CompanionDeviceManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    deviceManager.associate(associationRequest, command -> command.run(), associationCallback);
                } else {
                    deviceManager.associate(associationRequest, associationCallback, null);
                }
            } catch (IllegalStateException ignored) {
                Log.e(AppConstant.MidiOneTag, ignored.getMessage(), ignored);
                // Must declare uses-feature android.software.companion_device_setup in manifest to use this API
                // fallback to use BluetoothLeScanner
                _useCompanionDeviceSetup = false;
                return false;
            }
        }
        return true;
    }
}
