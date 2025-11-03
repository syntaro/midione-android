package jp.gr.java_conf.syntarou.midione.util;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jp.gr.java_conf.syntarou.midione.R;


/**
 * Utility for finding BLE MIDI devices
 *
 * @author K.Shoji
 */
public final class BleMidiDeviceUtils {
    public static boolean isMidiUUID(Context context, String uuid) {
        String[] uuidStringArray = context.getResources().getStringArray(R.array.uuidListForService);
        for (String seek : uuidStringArray) {
            if (seek.equalsIgnoreCase(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtains BluetoothGattService for MIDI
     *
     * @param context       the context
     * @param bluetoothGatt the gatt of device
     * @return null if no service found
     */
    @Nullable
    public static BluetoothGattService getMidiService(@NonNull final Context context, @NonNull final BluetoothGatt bluetoothGatt) {
        List<BluetoothGattService> services = bluetoothGatt.getServices();
        String[] uuidStringArray = context.getResources().getStringArray(R.array.uuidListForService);

        for (BluetoothGattService service : services) {
            for (String uuidString : uuidStringArray) {
                UUID uuid = BleUuidUtils.fromString(uuidString);
                if (BleUuidUtils.matches(service.getUuid(), uuid)) {
                    return service;
                }
            }
        }

        /*
        UUID id2 = BleUuidUtils.fromShortValue(0x1801);
        for (BluetoothGattService service : services) {
            UUID id1 = service.getUuid();
            if (id1.equals(id2)) {
                return service;
            }
        }*/

        return null;
    }

    /**
     * Obtains BluetoothGattCharacteristic for MIDI Input
     *
     * @param context              the context
     * @param bluetoothGattService the gatt service of device
     * @return null if no characteristic found
     */
    @Nullable
    public static BluetoothGattCharacteristic getMidiInputCharacteristic(@NonNull final Context context, @NonNull final BluetoothGattService bluetoothGattService) {
        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
        String[] uuidStringArray = context.getResources().getStringArray(jp.gr.java_conf.syntarou.midione.R.array.uuidListForInputCharacteristic);

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            for (String uuidString : uuidStringArray) {
                UUID uuid = BleUuidUtils.fromString(uuidString);
                if (BleUuidUtils.matches(characteristic.getUuid(), uuid)) {
                    return characteristic;
                }
            }
        }

        return null;
    }

    /**
     * Obtains BluetoothGattCharacteristic for MIDI Output
     *
     * @param context              the context
     * @param bluetoothGattService the gatt service of device
     * @return null if no characteristic found
     */
    @Nullable
    public static BluetoothGattCharacteristic getMidiOutputCharacteristic(@NonNull final Context context, @NonNull final BluetoothGattService bluetoothGattService) {
        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
        String[] uuidStringArray = context.getResources().getStringArray(R.array.uuidListForOutputCharacteristic);

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            for (String uuidString : uuidStringArray) {
                UUID uuid = BleUuidUtils.fromString(uuidString);
                if (BleUuidUtils.matches(characteristic.getUuid(), uuid)) {
                    return characteristic;
                }
            }
        }

        return null;
    }

    /**
     * Obtains list of ScanFilter for BLE MIDI
     *
     * @param context the context
     * @return list of {@link android.bluetooth.le.ScanFilter} for BLE MIDI devices.
     */
    @NonNull
    public static List<ScanFilter> getBleMidiScanFilters(@NonNull final Context context) {
        List<ScanFilter> scanFilters = new ArrayList<>();

        String[] uuidStringArray = context.getResources().getStringArray(R.array.uuidListForService);
        for (String uuidString : uuidStringArray) {
            scanFilters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuidString)).build());
        }

        return scanFilters;
    }

    /**
     * Obtains AssociationRequest for BLE MIDI
     *
     * @param context the context
     * @return {@link AssociationRequest} for BLE MIDI devices.
     */
    @NonNull
    public static AssociationRequest getBleMidiAssociationRequest(@NonNull final Context context) {
        final AssociationRequest.Builder associationRequestBuilder = new AssociationRequest.Builder();

        String[] uuidStringArray = context.getResources().getStringArray(R.array.uuidListForService);
        for (String uuidString : uuidStringArray) {
            associationRequestBuilder.addDeviceFilter(
                    new BluetoothLeDeviceFilter.Builder().setScanFilter(
                            new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuidString)).build()
                    ).build()
            );
        }

        return associationRequestBuilder.build();
    }

    /**
     * Device Information Service Gatt Service
     */
    private static final ParcelUuid SERVICE_DEVICE_INFORMATION = BleUuidUtils.parcelFromShortValue(0x180A);

    /**
     * Obtains BluetoothGattService for Device Information
     *
     * @param bluetoothGatt the gatt of device
     * @return null if no service found
     */
    @Nullable
    public static BluetoothGattService getDeviceInformationService(@NonNull final BluetoothGatt bluetoothGatt) {
        List<BluetoothGattService> services = bluetoothGatt.getServices();

        for (BluetoothGattService service : services) {
            if (BleUuidUtils.matches(service.getUuid(), SERVICE_DEVICE_INFORMATION.getUuid())) {
                return service;
            }
        }

        return null;
    }

    /**
     * Device Information Service Gatt Characteristics
     */
    public static final UUID CHARACTERISTIC_MANUFACTURER_NAME = BleUuidUtils.fromShortValue(0x2A29);
    public static final UUID CHARACTERISTIC_MODEL_NUMBER = BleUuidUtils.fromShortValue(0x2A24);
/*
    public static final UUID CHARACTERISTIC_SERIAL_NUMBER = BleUuidUtils.fromShortValue(0x2A24);

    public static final UUID CHARACTERISTIC_FIRMWARE_REVISION = BleUuidUtils.fromShortValue(0x2A24);

    public static final UUID CHARACTERISTIC_HARDWARE_REVSIION = BleUuidUtils.fromShortValue(0x2A24);
*/

    /**
     * Obtains BluetoothGattCharacteristic for Device Information
     *
     * @param bluetoothGattService the gatt service of device
     * @return null if no characteristic found
     */
    @Nullable
    public static BluetoothGattCharacteristic getManufacturerCharacteristic(@NonNull final BluetoothGattService bluetoothGattService) {
        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (BleUuidUtils.matches(characteristic.getUuid(), CHARACTERISTIC_MANUFACTURER_NAME)) {
                return characteristic;
            }
        }

        return null;
    }

    /**
     * Obtains BluetoothGattCharacteristic for Device Information
     *
     * @param bluetoothGattService the gatt service of device
     * @return null if no characteristic found
     */
    @Nullable
    public static BluetoothGattCharacteristic getModelCharacteristic(@NonNull final BluetoothGattService bluetoothGattService) {
        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();

        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (BleUuidUtils.matches(characteristic.getUuid(), CHARACTERISTIC_MODEL_NUMBER)) {
                return characteristic;
            }
        }

        return null;
    }
}
