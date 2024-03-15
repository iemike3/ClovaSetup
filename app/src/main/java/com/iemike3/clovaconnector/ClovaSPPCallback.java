package com.iemike3.clovaconnector;

import android.bluetooth.BluetoothDevice;

public interface ClovaSPPCallback {
    void onConnected(String clovaDeviceName);
    void onDisconnected();
    void onError(int errorType, String errorMessage);
    void onMessage(BluetoothDevice clovaDevice, String type, String message);
}
