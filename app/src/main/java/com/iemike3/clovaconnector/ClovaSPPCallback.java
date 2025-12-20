package com.iemike3.clovaconnector;

import android.bluetooth.BluetoothDevice;

public interface ClovaSPPCallback {
    void onConnected(BluetoothDevice clovaDevice);
    void onDisconnected();
    void onError(int errorType, String errorMessage);
    void onClovaData(BluetoothDevice clovaDevice, String type, String message);
}
