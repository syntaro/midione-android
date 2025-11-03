package jp.gr.java_conf.syntarou.midione.v1;

public interface IOneConnectionListener {
    void onDeviceStatusChanged(OneDevice device, int newStatus);
}
