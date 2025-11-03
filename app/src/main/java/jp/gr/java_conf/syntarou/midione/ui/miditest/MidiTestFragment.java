package jp.gr.java_conf.syntarou.midione.ui.miditest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import jp.gr.java_conf.syntarou.midione.MainActivity;
import jp.gr.java_conf.syntarou.midione.MXMidiStatic;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.databinding.FragmentMidiSelectorBinding;
import jp.gr.java_conf.syntarou.midione.util.PermissionHelper;
import jp.gr.java_conf.syntarou.midione.v1.IOneConnectionListener;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneDeviceStatus;
import jp.gr.java_conf.syntarou.midione.v1.OneInput;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;
import jp.gr.java_conf.syntarou.midione.ui.userchoice.IUserChoiceElement;
import jp.gr.java_conf.syntarou.midione.ui.userchoice.UserChoiceAdapter;
import jp.gr.java_conf.syntarou.midione.ui.userchoice.UserChoiceView;

import java.util.ArrayList;

public class MidiTestFragment extends Fragment {
    public MidiTestFragment() {
        super();
    }
    MidiOne _one;
    static int _targetBus = 0;
    private FragmentMidiSelectorBinding binding;

    IOneConnectionListener _listener = new IOneConnectionListener() {
        @Override
        public void onDeviceStatusChanged(OneDevice device, int newStatus) {
            if (newStatus == OneDeviceStatus.STATUS_CONNECTED) {
                if (device.countInput() > 0) {
                    OneInput in = device.getInput(0);
                    in.bindOnParsed(new IOneDispatcher() {
                        @Override
                        public boolean dispatchOne(OneMessage one) {
                            _logger.log(in.getNameText() + ">>>" + one);
                            return true;
                        }

                        @Override
                        public String getNameText() {
                            return "Logging";
                        }
                    });
                }
            }
            requestReloadList();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        binding.recyclerInput.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerOutput.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerReadyDevice.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerFoundDevice.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerFound2Device.setLayoutManager(new LinearLayoutManager(getContext()));

        for (OneDevice device : _one.listAllDevices()) {
            device.getDeviceStatus();
        }
        requestReloadList();
    }
    PermissionHelper _permission;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _one = MidiOne.getInstance();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    MessageLogger _logger;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        _permission = PermissionHelper.getInstance();
        binding = FragmentMidiSelectorBinding.inflate(getLayoutInflater());
        _one.addConnectionListener(_listener);

        binding.buttonAnotherBLE.setOnClickListener(this::startScanBle);
        binding.buttonAnotherBLE2.setOnClickListener(this::startScanBePeripheral);
        binding.buttonConnectDevice.setOnClickListener(this::startConnectDevice);
        binding.buttonCloseDevice.setOnClickListener(this::startCloseDevice);
        binding.buttonSendSignal.setOnClickListener(this::startSendSignal);

        changeBus(_targetBus);
        _logger = MessageLogger.getInstance();
        _logger.attachRecyclerView(binding.recyclerLogging);

        return binding.getRoot();
    }
    public void startCloseDevice(View v) {
        IUserChoiceElement e = binding.recyclerReadyDevice.getUserChoiceResult();
        if (e instanceof  OneDevice) {
            OneDevice device = (OneDevice)e;
            device.closeDevice();
        }
    }

    public void startSendSignal(View v) {
        IUserChoiceElement e = binding.recyclerOutput.getUserChoiceResult();
        if (e instanceof OneOutput) {
            OneOutput output = (OneOutput)e;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 128; ++i) {
                        OneMessage on = OneMessage.thisCodes(0, MXMidiStatic.COMMAND_CH_NOTEON, i, 100);
                        OneMessage off = OneMessage.thisCodes(0, MXMidiStatic.COMMAND_CH_NOTEOFF, i, 0);
                        _logger.log(output.getNameText() + "<<<" + on);
                        output.dispatchOne(on);
                        OneHelper.Thread_sleep(200);
                        _logger.log(output.getNameText() + "<<<" + off);
                        output.dispatchOne(off);
                        OneHelper.Thread_sleep(100);
                    }
                }
            }).start();
        }
        else {
            Toast.makeText(getContext(), "Choice Output from UpRight", Toast.LENGTH_SHORT).show();
        }
    }
    public void startConnectDevice(View v) {
        IUserChoiceElement e = binding.recyclerFoundDevice.getUserChoiceResult();
        if (e instanceof  OneDevice) {
            OneDevice device = (OneDevice) e;
            device.openDevice();
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        _one.removeConnectionListener(_listener);
    }

    @SuppressLint("MissingPermission")
    public void startScanBle(View v) {
        if (_permission.isPermissoinGranted()) {
            _one.getDriverBluetooth().startScanDevices(getContext());
        } else {
            Toast.makeText(getContext(), "Need BLE Permission", Toast.LENGTH_SHORT).show();
            _permission.grantPermissionImpl();
        }
    }

    public void startScanBePeripheral(View v) {
        if (_permission.isPermissoinGranted()) {
            MainActivity main = (MainActivity) getActivity();
            main.getBleLauncher().launch(() -> {
                _one.getDriverPeripheral().startScanDevices(_one.getMidiContext());
            });
        } else {
            Toast.makeText(getContext(), "Need BLE Permission", Toast.LENGTH_SHORT).show();
            _permission.grantPermissionImpl();
        }
    }

    /*
    private final BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    ParcelUuid[] uuids = device.getUuids();
                    if (uuids != null) {
                        for (ParcelUuid seek : uuids) {
                            String text = seek.toString();
                            if (BleMidiDeviceUtils.isMidiUUID(context, text)) {
                                _one.getDriverBluetooth().addBluetoothDevice(device);
                                break;
                            }
                        }
                    }
                }
            }catch(Throwable ex) {
                Log.e(AppConstant.TAG, ex.getMessage(), ex);
            }
        }
    };*/

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void setAdapterWithRecycle(UserChoiceView view, ArrayList<IUserChoiceElement> list) {
        UserChoiceAdapter adapter = view.getAdapter();
        adapter.setData(list);
        adapter.setOnUserChoiceListener((adapter1, clicked) -> {
            Object obj = clicked;
            if (clicked != null) {
                adapter1.setSelection(clicked);
            }
        });
    }

    public ArrayList<IUserChoiceElement> createListInput() {
        ArrayList<IUserChoiceElement> result = new ArrayList<>();
        for (OneDevice device:  _one.listAllDevices()) {
            if (device.getDeviceStatus().isConnected()){
                for (int i = 0 ; i < device.countInput(); ++ i) {
                    OneInput input = device.getInput(i);
                    result.add(input);
                }
            }
        }
        return result;
    }
    public ArrayList<IUserChoiceElement> createListOutput() {
        ArrayList<IUserChoiceElement> result = new ArrayList<>();
        for (OneDevice device:  _one.listAllDevices()) {
            if (device.getDeviceStatus().isConnected()){
                for (int i = 0 ; i < device.countOutput(); ++ i) {
                    OneOutput output = device.getOutput(i);
                    result.add(output);
                }
            }
        }
        return result;
    }
    public ArrayList<IUserChoiceElement> createListDevice(int status) {
        ArrayList<IUserChoiceElement> list = new ArrayList<>();
        for (OneDevice seek : _one.listAllDevices()) {
            if (seek.getDeviceStatus().getStatus() == status) {
                list.add(seek);
            }
        }
        return list;
    }

    public void changeBus(int bus) {
        _targetBus =  bus;
        _reserved = 0;
        reloadList();
    }
    long _reserved = 0;
    public void requestReloadList() {
        _reserved = System.currentTimeMillis() + 190;
        OneHelper.runOnUiThread(this::reloadList, 200);
    }
    public void reloadList() {
        long past = System.currentTimeMillis() - _reserved;
        if (past < 0 && _reserved != 0) {
            return;
        }

        setAdapterWithRecycle(binding.recyclerInput, createListInput());
        setAdapterWithRecycle(binding.recyclerOutput, createListOutput());
        setAdapterWithRecycle(binding.recyclerReadyDevice, createListDevice(OneDeviceStatus.STATUS_CONNECTED));
        setAdapterWithRecycle(binding.recyclerFoundDevice, createListDevice(OneDeviceStatus.STATUS_JUST_AVAIL));
        setAdapterWithRecycle(binding.recyclerFound2Device, createListDevice(OneDeviceStatus.STATUS_START_CONNECT));
    }
}
