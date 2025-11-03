package jp.gr.java_conf.syntarou.midione.ui.miditest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class BleLauncher {
    ActivityResultLauncher<Intent> _launcher;
    boolean _closed = false;
    Runnable _onSuccess = null;

    public BleLauncher(AppCompatActivity activity) {
        _launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), (result) -> onActivityResult1(result));
    }

    public void launch(Runnable onSuccess) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        _closed = false;
        _onSuccess = onSuccess;
        _launcher.launch(intent);
    }

    public void onActivityResult1(ActivityResult result) {
        synchronized (this) {
            _closed = true;
            notifyAll();
        }
        if (result.getResultCode() != Activity.RESULT_CANCELED) {
            if (_onSuccess != null) {
                _onSuccess.run();
            }
        }
    }

    public void waitClose() {
        synchronized (this) {
            while (!_closed) {
                try {
                    wait(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
