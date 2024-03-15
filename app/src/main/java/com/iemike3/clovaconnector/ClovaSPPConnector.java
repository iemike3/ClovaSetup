package com.iemike3.clovaconnector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class ClovaSPPConnector {

    public static int ERROR_NOT_CONNECTED = 0;
    public static int ERROR_FAILED_TO_CONNECT = 1;
    public static int  ERROR_FAILED_PARSE_RESPONSE = 2;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice clovaDevice;
    private BluetoothSocket clovaBluetoothSocket;
    private ClovaConnectedThread clovaConnectedThread;
    private ClovaSPPCallback clovaSPPCallback;

    public ClovaSPPConnector(Context context, BluetoothAdapter bluetoothAdapter, ClovaSPPCallback clovaSPPCallback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.clovaSPPCallback = clovaSPPCallback;
    }

    public void connect(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice.getName() != null && !(bluetoothDevice.getName().startsWith("WAVE-") || bluetoothDevice.getName().startsWith("CLOVA-"))) {
            return;
        }
        this.clovaDevice = bluetoothDevice;
        ClovaConnectThread clovaConnectThread = new ClovaConnectThread(this.clovaDevice);
        clovaConnectThread.start();
    }

    public void close() {
        if (clovaConnectedThread != null) {
            clovaConnectedThread.cancel();
        }
    }

    public void checkMyDevice() {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.checkMyDevice();
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    public void getDetails() {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.getDetails();
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    public void getWifiList() {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.getWifiList();
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    public void connectWifi(JSONObject wifiData) {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.connectWifi(wifiData);
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    public void setAuth() {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.setAuth();
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    public void enableBTPan() {
        if (clovaConnectedThread != null && clovaConnectedThread.isConnected()) {
            clovaConnectedThread.enableBTPan();
        } else {
            if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_NOT_CONNECTED, "");
            }
        }
    }

    private void onConnected(BluetoothSocket clovaBluetoothSocket) {
        Log.i("ta", "connected");
        clovaConnectedThread = new ClovaConnectedThread(clovaBluetoothSocket);
        clovaConnectedThread.start();
    }

    class ClovaConnectThread extends Thread {
        BluetoothDevice clovaDevice;
        BluetoothSocket clovaBluetoothSocket;

        ClovaConnectThread(BluetoothDevice bluetoothDevice) {
            this.clovaDevice = bluetoothDevice;
            try {
                this.clovaBluetoothSocket = this.clovaDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isConnected() {
            return this.clovaBluetoothSocket.isConnected();
        }

        public void cancel() {
            try {
                this.clovaBluetoothSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            Log.i("ClovaConnectThread", "Connecting...");
            if (ClovaSPPConnector.this.bluetoothAdapter.isDiscovering()) {
                ClovaSPPConnector.this.bluetoothAdapter.cancelDiscovery();
            }
            try {
                this.clovaBluetoothSocket.connect();
            } catch (IOException e) {
                //接続失敗
                if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                    ClovaSPPConnector.this.clovaSPPCallback.onError(ERROR_FAILED_TO_CONNECT, e.toString());
                }
                e.printStackTrace();
                //throw new RuntimeException(e);
            }
            if (this.clovaBluetoothSocket.isConnected()) {
                if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                    ClovaSPPConnector.this.clovaSPPCallback.onConnected(clovaDevice.getName());
                }
                ClovaSPPConnector.this.onConnected(this.clovaBluetoothSocket);
            }
        }
    }

    class ClovaConnectedThread extends Thread {
        BluetoothSocket clovaBluetoothSocket;
        InputStream inputStream;
        OutputStream outputStream;

        ClovaConnectedThread(BluetoothSocket bluetoothSocket) {
            this.clovaBluetoothSocket = bluetoothSocket;
            try {
                this.inputStream = this.clovaBluetoothSocket.getInputStream();
                this.outputStream = this.clovaBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public boolean isConnected() {
            return this.clovaBluetoothSocket != null && this.clovaBluetoothSocket.isConnected();
        }

        public void checkMyDevice() {
            try {
                this.outputStream.write("APCD".getBytes(StandardCharsets.UTF_8));
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void getDetails() {
            try {
                this.outputStream.write("APGF".getBytes(StandardCharsets.UTF_8));
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void getWifiList() {
            try {
                this.outputStream.write("APGN".getBytes(StandardCharsets.UTF_8));
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void connectWifi(JSONObject wifiData) {
            Log.d("ClovaSPPConnector (connectWifi)", wifiData.toString());
            JSONObject sendJSON = wifiData;
            byte[] bytes = sendJSON.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bArr = new byte[bytes.length + 8];
            bArr[0] = 65;
            bArr[1] = 80;
            bArr[2] = 83;
            bArr[3] = 78;
            bArr[4] = 82;
            bArr[5] = 81;
            if (bytes != null) {
                int length = bytes.length;
                System.arraycopy(new byte[]{(byte) (length >> 8), (byte) length}, 0, bArr, 6, 2);
                System.arraycopy(bytes, 0, bArr, 8, length);
            }
            try {
                this.outputStream.write(bArr);
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void setAuth() {
            JSONObject sendJSON = new JSONObject();
            try {
                sendJSON.put("clovaAuthCode", "dummy");
                sendJSON.put("clovaAuthState", "dummy");
                sendJSON.put("isClearUserData", false);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            byte[] bytes = sendJSON.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bArr = new byte[bytes.length + 8];
            bArr[0] = 65;
            bArr[1] = 80;
            bArr[2] = 83;
            bArr[3] = 80;
            bArr[4] = 82;
            bArr[5] = 81;
            if (bytes != null) {
                int length = bytes.length;
                System.arraycopy(new byte[]{(byte) (length >> 8), (byte) length}, 0, bArr, 6, 2);
                System.arraycopy(bytes, 0, bArr, 8, length);
            }
            try {
                this.outputStream.write(bArr);
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void enableBTPan() {
            JSONObject sendJSON = new JSONObject();
            try {
                sendJSON.put("tethering_enable", true);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            byte[] bytes = sendJSON.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bArr = new byte[bytes.length + 8];
            bArr[0] = 65;
            bArr[1] = 80;
            bArr[2] = 66;
            bArr[3] = 84;
            bArr[4] = 82;
            bArr[5] = 81;
            if (bytes != null) {
                int length = bytes.length;
                System.arraycopy(new byte[]{(byte) (length >> 8), (byte) length}, 0, bArr, 6, 2);
                System.arraycopy(bytes, 0, bArr, 8, length);
            }
            try {
                this.outputStream.write(bArr);
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void cancel() {
            try {
                this.clovaBluetoothSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            byte[] bArr = new byte[8];
            byte[] bArr2 = null;
            boolean z = false;
            int i2 = 0;
            while (true) {
                boolean z2 = true;
                if (!z) {
                    try {
                        if (inputStream.read(bArr) == 8 && bArr[0] == 65) {
                            int intValue = new BigInteger(new byte[]{bArr[6], bArr[7]}).intValue();
                            byte[] bArr3 = new byte[intValue];
                            i2 = inputStream.read(bArr3, 0, intValue);
                            if (i2 == intValue) {
                                int i3 = intValue + 8;
                                byte[] bArr4 = new byte[i3];
                                System.arraycopy(bArr, 0, bArr4, 0, 8);
                                System.arraycopy(bArr3, 0, bArr4, 8, intValue);

                                String data_type = new String(bArr4, 0, 6);
                                int data_length = new BigInteger(new byte[]{bArr4[6], bArr4[7]}).intValue();
                                String data = new String(bArr4, 8, data_length);
                                if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                                    ClovaSPPConnector.this.clovaSPPCallback.onMessage(clovaDevice, data_type, data);
                                }

                                z2 = false;
                            }
                            bArr2 = bArr3;
                            z = z2;
                        } else {
                            z = true;
                        }
                    } catch (IOException e2) {
                        Log.e("ClovaSPPConnector", "disconnected?: ", e2);
                        if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                            ClovaSPPConnector.this.clovaSPPCallback.onDisconnected();
                        }
                        return;
                    }
                } else if (bArr2 != null) {
                    int length = bArr2.length - i2;
                    byte[] bArr5 = new byte[length];
                    int read = 0;
                    try {
                        read = inputStream.read(bArr5, 0, length);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.arraycopy(bArr5, 0, bArr2, i2, length);
                    i2 += read;
                    if (read == length) {
                        int length2 = bArr2.length + 8;
                        byte[] bArr6 = new byte[length2];
                        System.arraycopy(bArr, 0, bArr6, 0, 8);
                        System.arraycopy(bArr2, 0, bArr6, 8, bArr2.length);

                        String data_type = new String(bArr6, 0, 6);
                        int data_length = new BigInteger(new byte[]{bArr6[6], bArr6[7]}).intValue();
                        String data = new String(bArr6, 8, data_length);
                        if (ClovaSPPConnector.this.clovaSPPCallback != null) {
                            ClovaSPPConnector.this.clovaSPPCallback.onMessage(clovaDevice, data_type, data);
                        }

                        z = false;
                    }
                }
            }

        }
    }

    public final String byteArrayToHexForLog(byte[] bArr) {
        StringBuilder sb2 = new StringBuilder();
        if (bArr != null) {
            int length = bArr.length;
            int i10 = 0;
            while (i10 < length) {
                byte b10 = bArr[i10];
                i10++;
                //q0 q0Var = q0.a;
                String format = String.format("%02x ", Arrays.copyOf(new Object[]{Integer.valueOf((b10 & 255))}, 1));
                //s.e(format, "format(format, *args)");
                sb2.append(format);
            }
        }
        String sb3 = sb2.toString();
        //s.e(sb3, "sb.toString()");
        return sb3;
    }
}
