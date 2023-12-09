package com.iemike3.clovadesk;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.iemike3.clovadeskapp.ClovaDetails;
import com.iemike3.clovadeskapp.ClovaWiFiList;
import com.iemike3.clovadeskapp.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import ai.clova.cic.clientlib.device.crypto.AESCipher;
import ai.clova.cic.clientlib.device.crypto.KeyManager;

public class ClovaDeskConnector {

    //private ClovaDeskBTCallback clovaDeskBTCallback;
    private Context context;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    // clova関係
    private boolean needReconnect;
    private ArrayList<String> publicKeys = new ArrayList<>();

    private boolean waitingDescriptorCallback;
    private boolean writeEnable = false;
    private final int dataSplitMaxLength = 128;

    private final int gattSendDataMaxLength = 185;
    private HashMap<String, byte[]> gattResponseBufferQueueMap = new HashMap<>();
    private int connectionState = 0;
    private byte[] aesSecret;
    private byte[] aesIV;
    private byte[] gattSendDataBuffer;


    private final String clovaDesk_serviceID = "000001e0-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_FIRMWARE_INFO = "000001e1-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_NETWORK_STATUS = "000001e2-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_NETWORK_LIST = "000001e3-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_SET_NETWORK = "000001e4-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_PREPARE_INFO = "000001e5-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_SET_PREPARE_INFO = "000001e6-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_SET_PUBLIC_KEY = "000001e7-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_PROTOCOL_VERSION = "000001e8-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_SET_HOTSPOT_CONNECTION = "000001e9-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_END_PROTOCOL = "000001ea-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_CHECK_DEVICE = "000001ed-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_TETHERING_ENABLE = "000001ee-0000-1000-8000-00805f9b34fb";
    private final String clovaDesk_GET_NETWORK_CONNECTION_STEP = "000001ef-0000-1000-8000-00805f9b34fb";
    private final String clovaDeskSET_NOTIFICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private int gatt_state = 0;

    private String clova_scaned_wifilist_str;

    private Handler clova_setAESSecretHandler = null;


    public ClovaDeskConnector(Context context) {
        this.context = context;
    }

    /*
    public interface ClovaDeskBTCallback {
        void onInit();
        void onConnect();
        void onDisconnect();
        void onMessage();
    }

    public void setCallback(ClovaDeskBTCallback clovaDeskBTCallback) {
        this.clovaDeskBTCallback = clovaDeskBTCallback;
        clovaDeskBTCallback.onInit();
    }
     */

    public void setupConnection(BluetoothDevice clovaDeskBTDevice, BluetoothManager bluetoothManager, Runnable successFunction) {
        if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!clovaDeskBTDevice.getName().startsWith("CLOVA-DESK-")) {
            return;
        }
        bluetoothDevice = clovaDeskBTDevice;
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothGatt = bluetoothDevice.connectGatt(this.context, false, new clovaDeskBTCallback(), BluetoothDevice.TRANSPORT_LE);
        new Handler().post(successFunction);
    }

    public void disconnect() {
        if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (gatt_state != 0) {
            //this.needReconnect = false;
            bluetoothGatt.disconnect();
            bluetoothGatt = null;
            bluetoothDevice = null;
        }
    }

    public void checkMyDevice() {
        if (gatt_state != 2) {
            return;
        }
        /* check my device? */
        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_CHECK_DEVICE);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = null;
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
        /* end */
    }

    public void getDetails() {
        if (gatt_state != 2) {
            return;
        }
        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_GET_FIRMWARE_INFO);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = null;
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public void getWifiList() {
        if (gatt_state != 2) {
            return;
        }
        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_GET_NETWORK_LIST);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = null;
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public void setAuth() {
        if (gatt_state != 2) {
            return;
        }

        HashMap hashMap = new HashMap();
        hashMap.put("clovaAuthCode", "ClovaDymmyCOde");
        hashMap.put("clovaAuthState", "333");
        hashMap.put("isClearUserData", false);

        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_SET_PREPARE_INFO);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = new JSONObject(hashMap);
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public void getProtocolVersion() {
        if (gatt_state != 2) {
            return;
        }

        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_GET_PROTOCOL_VERSION);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = null;
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public void connectWifi(String wifiName, String password) throws JSONException {
        if (gatt_state != 2) {
            return;
        }

        Toast.makeText(context, wifiName + " / " + password, Toast.LENGTH_SHORT).show();
        JSONObject jsonObject = new JSONObject(clova_scaned_wifilist_str);
        JSONArray clova_scaned_wifilist = jsonObject.getJSONArray("wifi");
        HashMap hashMap = new HashMap();

        for (int i = 0; i < clova_scaned_wifilist.length(); i++) {
            JSONObject wifi = clova_scaned_wifilist.getJSONObject(i);
            if (wifiName.equals(wifi.getString("ssid"))) {
                Log.i("aujo2", "hit!!!!");
                hashMap.put("ssid", wifi.getString("ssid"));
                hashMap.put("hex_ssid", Utils.byteArrayToHex(wifi.getString("ssid").getBytes(StandardCharsets.UTF_8)));
                if (wifi.getInt("key_mgmt") != 0) {
                    if (password != null) {
                        hashMap.put("psk", password);
                        hashMap.put("hex_psk", Utils.byteArrayToHex(password.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
                    } else {
                        hashMap.put("psk", wifi.getString("psk"));
                        hashMap.put("hex_psk", Utils.byteArrayToHex(wifi.getString("psk").getBytes(StandardCharsets.UTF_8)));
                    }
                } else {
                    hashMap.put("psk", wifi.getString("psk"));
                    hashMap.put("hex_psk", Utils.byteArrayToHex(wifi.getString("psk").getBytes(StandardCharsets.UTF_8)));
                }
                hashMap.put("frequency", wifi.getInt("frequency"));
                hashMap.put("signal_level", wifi.getInt("signal_level"));
                hashMap.put("key_mgmt", wifi.getInt("key_mgmt"));
                hashMap.put("eap", wifi.getString("eap"));
                hashMap.put("id", wifi.getString("id"));
                hashMap.put("isConnected", wifi.getBoolean("isConnected"));
                hashMap.put("hidden", wifi.getBoolean("hidden"));
                hashMap.put("currentTime", Long.valueOf(System.currentTimeMillis()));
                hashMap.put("isNetworkOnly", wifi.getBoolean("isNetworkOnly"));
            }
        }

        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_SET_NETWORK);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = new JSONObject(hashMap);
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public void getPrepareInfo() {
        if (gatt_state != 2) {
            return;
        }

        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(clovaDesk_GET_PREPARE_INFO);
        if (gattCharacteristic != null) {
            setNotificationDescriptorIfNeeded(gattCharacteristic);
            Object obj = null;
            setGattSendData(String.valueOf(obj).getBytes(StandardCharsets.UTF_8), gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    private class clovaDeskBTCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            gatt_state = newState;
            Log.i(this.getClass().getSimpleName(), "onConnectionStateChange: " + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(ClovaDeskConnector.this.context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(this.getClass().getSimpleName(), "onServicesDiscovered");
            if (ActivityCompat.checkSelfPermission(ClovaDeskConnector.this.context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (status != 0) {
                // Error?
                Log.e("onServicesDiscovered", "Status Not 0");
                return;
            }
            boolean success = false;
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService bluetoothGattService : services) {
                Log.d("onServicesDiscovered [Service UUID]", bluetoothGattService.getUuid().toString());
                if (bluetoothGattService.getUuid().toString().equals(clovaDesk_serviceID)) {
                    List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                    for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                    }
                    success = true;
                }
            }
            if (success) {
                gatt.requestMtu(ClovaDeskConnector.this.gattSendDataMaxLength);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.i(this.getClass().getSimpleName(), "onMtuChanged");
            if (status == 0) {
                if (getNeedReconnect()) {
                    setNeedReconnect(false);
                    sendGattData(getGattCharacteristic(clovaDesk_SET_NETWORK));
                    return;
                }
                ClovaDeskConnector.this.waitingDescriptorCallback = true;
                if (setNotificationDescriptor(getGattCharacteristic(clovaDesk_SET_PUBLIC_KEY))) {
                    return;
                }
                ClovaDeskConnector.this.waitingDescriptorCallback = false;
                generateGattPublicKey();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(this.getClass().getSimpleName(), "onCharacteristicRead");

            ArrayList arrayList;
            ArrayList arrayList2;
            ArrayList arrayList3 = null;
            HashMap hashMap2;
            HashMap hashMap3;
            HashMap hashMap4;
            String decodeAES = null;
            byte[] bArr;
            byte[] bArr2;
            String str = null;

            if (status != 0) {
                Log.d(this.getClass().getSimpleName(), "onCharacteristicRead (status != 0) UUID: " + characteristic.getUuid().toString());
                return;
            }
            Log.d(this.getClass().getSimpleName(), "onCharacteristicRead UUID: " + characteristic.getUuid().toString());
            String uuid2 = characteristic.getUuid().toString();
            byte[] value = characteristic.getValue();
            HashMap hashMap = ClovaDeskConnector.this.gattResponseBufferQueueMap;
            byte[] bArr3 = (byte[]) hashMap.get(uuid2);
            if ((uuid2.length() > 0 ? (byte) 1 : (byte) 0) != 0) {
                if (((value.length == 0 ? 1 : 0) ^ 1) != 0 && !"ffffff".equals(Utils.INSTANCE.byteArrayToHex(value))) {
                    hashMap3 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                    if (hashMap3.containsKey(uuid2)) {
                        if (bArr3 != null) {
                            byte[] bArr4 = new byte[bArr3.length + value.length];
                            System.arraycopy(bArr3, 0, bArr4, 0, bArr3.length);
                            System.arraycopy(value, 0, bArr4, bArr3.length, value.length);
                            hashMap4 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                            hashMap4.put(uuid2, bArr4);
                        }
                        readCharacteristic(characteristic);
                        return;
                    }
                }
            }
            if (uuid2.equals(clovaDesk_SET_PUBLIC_KEY)) {
                Log.d(this.getClass().getSimpleName(), "server public key: " + Utils.INSTANCE.byteArrayToHexForLog(bArr3));
                /*
                arrayList = ClovaDeskConnector.this.publicKeys;
                if (arrayList == null) {
                    //t.x("publicKeys");
                    Log.e(this.getClass().getSimpleName(), "publickeys error?");
                    arrayList = null;
                }
                byte[] hexStringToByteArray = Utils.hexStringToByteArray((String) arrayList.get(0));
                arrayList2 = ClovaDeskConnector.this.publicKeys;
                if (arrayList2 == null) {
                    Log.e(this.getClass().getSimpleName(), "publickeys(2) error?");
                } else {
                    arrayList3 = arrayList2;
                }
                byte[] hexStringToByteArray2 = Utils.hexStringToByteArray((String) arrayList3.get(1));
                if (!Arrays.equals(bArr3, hexStringToByteArray) && !Arrays.equals(bArr3, hexStringToByteArray2)) {
                    //Error Handle?
                    //unused6 = GATTConnector.this.TAG;
                    //bluetoothResponseHandler4 = GATTConnector.this.bluetoothResponseHandler;
                    //if (bluetoothResponseHandler4 != null && (obtainMessage3 = bluetoothResponseHandler4.obtainMessage(BluetoothResponseHandlerMessage.ERROR.getType(), ClovaDeviceConnectError.ECDH_EXCHANGE_FAIL)) != null) {
                    //    obtainMessage3.sendToTarget();
                    //}
                    Log.e(this.getClass().getSimpleName(), "Array Error(equals) error?");
                    ClovaDeskConnector.this.disconnect();
                    return;
                }
                */
                if (bArr3 != null) {
                    try {
                        processEncryptionKey(bArr3);
                        Log.i("processEncryptionkey Success", "AESSecret: " + Utils.INSTANCE.byteArrayToHexForLog(ClovaDeskConnector.this.aesSecret));
                        Log.i("processEncryptionkey Success", "AESIV: " + Utils.INSTANCE.byteArrayToHexForLog(ClovaDeskConnector.this.aesIV));
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "AESSecret Success!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (NoSuchAlgorithmException e10) {
                        //unused7 = GATTConnector.this.TAG;
                        Log.e(this.getClass().getSimpleName(), "processEncryption (NoSuchAlgorithmException) error?");
                        e10.printStackTrace();
                        ClovaDeskConnector.this.disconnect();
                    }
                }
            } else {
                //Decrypted
                if (bArr3 == null) {
                    decodeAES = "";
                } else {
                    try {
                        AESCipher aESCipher = AESCipher.INSTANCE;
                        bArr = ClovaDeskConnector.this.aesSecret;
                        if (bArr == null) {
                            Log.e("onCharacteristicRead(encrypted)", "No aesSecret");
                            bArr = null;
                        }
                        bArr2 = ClovaDeskConnector.this.aesIV;
                        if (bArr2 == null) {
                            Log.e("onCharacteristicRead(encrypted)", "No aesIV");
                            bArr2 = null;
                        }
                        decodeAES = aESCipher.decodeAES(bArr, bArr2, bArr3);
                    } catch (Exception e11) {
                        if (e11 instanceof NoSuchAlgorithmException ? true : e11 instanceof InvalidAlgorithmParameterException ? true : e11 instanceof InvalidKeyException ? true : e11 instanceof NoSuchPaddingException ? true : e11 instanceof BadPaddingException ? true : e11 instanceof IllegalBlockSizeException) {
                            e11.printStackTrace();
                            if (uuid2.equals(clovaDesk_SET_PREPARE_INFO)) {
                                str = String.valueOf(bArr3);
                            }
                        }
                    }
                }
                str = decodeAES;

                Log.i("onCharacteristicRead (decrypted data)", "uuid: " + characteristic.getUuid().toString());
                Log.i("onCharacteristicRead (decrypted data)", "data: " + str);

                String finalStr = str;

                switch (characteristic.getUuid().toString()) {
                    case clovaDesk_GET_FIRMWARE_INFO:
                        Intent ClovaDetailsIntent = new Intent(context, ClovaDetails.class);
                        ClovaDetailsIntent.putExtra("clova_device_name", gatt.getDevice().getName().replace("CLOVA-", ""));
                        ClovaDetailsIntent.putExtra("clova_return_json", str);
                        context.startActivity(ClovaDetailsIntent);
                        break;
                    case clovaDesk_CHECK_DEVICE:
                        break;
                    case clovaDesk_GET_NETWORK_LIST:
                        clova_scaned_wifilist_str = str;
                        Intent ClovaWiFiListIntent = new Intent(context, ClovaWiFiList.class);
                        ClovaWiFiListIntent.putExtra("clova_device_name", gatt.getDevice().getName().replace("CLOVA-", ""));
                        ClovaWiFiListIntent.putExtra("clova_return_json", str);
                        context.startActivity(ClovaWiFiListIntent);
                        break;
                    default:
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(context)
                                        .setMessage(finalStr)
                                        .show();
                            }
                        });
                }
                /*
                unused8 = GATTConnector.this.TAG;
                s.n("content : ", str);
                if (str != null && bluetoothResponseHandler2 != null) {
                    Message obtainMessage4 = bluetoothResponseHandler2.obtainMessage(BluetoothResponseHandlerMessage.READ.getType(), uuid2 + "#!" + ((Object) str));
                    if (obtainMessage4 != null) {
                        obtainMessage4.sendToTarget();
                    }
                }
                 */
            }
            hashMap2 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
            hashMap2.remove(uuid2);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Log.i(this.getClass().getSimpleName(), "onCharacteristicWrite");
            if (status == 0) {
                boolean z10 = ClovaDeskConnector.this.writeEnable;
                if (z10) {
                    sendGattData(characteristic);
                    return;
                }
                Log.i(this.getClass().getSimpleName(), "onCharacteristicWrite: writeEnable False!");
                String uuid = characteristic.getUuid().toString();
                //gATTMessenger = GATTConnector.this.messenger;
                //if (t.b(uuid, gATTMessenger.getString(ClovaCharacteristicType.SET_NETWORK)) && GATTConnector.this.getNeedReconnect()) {
                //    h.b(i0.a(v0.a()), null, null, new GATTConnector$gattCallback$1$onCharacteristicWrite$1(GATTConnector.this, null), 3, null);
                //}
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            HashMap hashMap;
            HashMap hashMap2;
            HashMap hashMap3;
            HashMap hashMap4;
            HashMap hashMap5;
            String decodeAES = null;
            byte[] bArr;
            byte[] bArr2;


            Log.i(this.getClass().getSimpleName(), "onCharacteristicChanged");
            Log.d(this.getClass().getSimpleName(), "onCharacteristicChanged (UUID): " + characteristic.getUuid().toString());
            String uuid = characteristic.getUuid().toString();
            byte[] value = characteristic.getValue();
            if (uuid.length() > 0) {
                String str = null;
                if ((!(value.length == 0)) && !"ffffff".equals(Utils.byteArrayToHex(value))) {
                    Log.d(this.getClass().getSimpleName(), "value ffffff");
                    if (uuid.equals(clovaDesk_GET_NETWORK_CONNECTION_STEP)) {
                        hashMap5 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                        hashMap5.put(uuid, null);
                    }
                    hashMap3 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                    if (hashMap3.containsKey(uuid)) {
                        hashMap4 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                        hashMap4.put(uuid, value);
                        if (!uuid.equals(clovaDesk_SET_NETWORK)) {
                            //gATTMessenger4 = GATTConnector.this.messenger;
                        }
                        //h.b(i0.a(v0.a()), null, null, new GATTConnector$gattCallback$1$onCharacteristicChanged$1(GATTConnector.this, null), 3, null);
                        readCharacteristic(characteristic);
                        return;
                    }
                }
                hashMap = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                byte[] bArr3 = (byte[]) hashMap.get(uuid);
                if (bArr3 == null) {
                    decodeAES = "";
                } else {
                    try {
                        AESCipher aESCipher = AESCipher.INSTANCE;
                        bArr = ClovaDeskConnector.this.aesSecret;
                        if (bArr == null) {
                            Log.e("onCharacteristicChanged(encrypted)", "No aesSecret");
                            bArr = null;
                        }
                        bArr2 = ClovaDeskConnector.this.aesIV;
                        if (bArr2 == null) {
                            Log.e("onCharacteristicChanged(encrypted)", "No aesIV");
                            bArr2 = null;
                        }
                        decodeAES = aESCipher.decodeAES(bArr, bArr2, bArr3);
                    } catch (Exception e10) {
                        if (e10 instanceof NoSuchAlgorithmException ? true : e10 instanceof InvalidAlgorithmParameterException ? true : e10 instanceof InvalidKeyException ? true : e10 instanceof NoSuchPaddingException ? true : e10 instanceof BadPaddingException) {
                            e10.printStackTrace();
                            //gATTMessenger = GATTConnector.this.messenger;
                            if (uuid.equals(clovaDesk_SET_PREPARE_INFO) && bArr3 != null) {
                                str = bArr3.toString();
                            }
                        }
                    }
                }
                str = decodeAES;
                /*
                bluetoothResponseHandler = GATTConnector.this.bluetoothResponseHandler;
                if (bluetoothResponseHandler != null) {
                    Message obtainMessage = bluetoothResponseHandler.obtainMessage(BluetoothResponseHandlerMessage.READ.getType(), uuid + "#!" + ((Object) str));
                    if (obtainMessage != null) {
                        obtainMessage.sendToTarget();
                    }
                }
                 */
                hashMap2 = ClovaDeskConnector.this.gattResponseBufferQueueMap;
                hashMap2.remove(uuid);
            }
        }

    }


    public final boolean getNeedReconnect() {
        return ClovaDeskConnector.this.needReconnect;
    }

    public final void setNeedReconnect(boolean needReconnect) {
        ClovaDeskConnector.this.needReconnect = needReconnect;
    }

    public final void setPublicKeys(ArrayList<String> publicKeys) {
        //t.g(publicKeys, "publicKeys");
        Log.d(this.getClass().getSimpleName(), "connect public key: " + publicKeys);
        ClovaDeskConnector.this.publicKeys = publicKeys;
    }

    public final void setNotificationDescriptorIfNeeded(BluetoothGattCharacteristic characteristic) {
        //s.f(characteristic, "characteristic");
        String uuid = characteristic.getUuid().toString();
        //s.e(uuid, "characteristic.uuid.toString()");
        if (clovaDesk_SET_PUBLIC_KEY.equals(uuid)) {
            return;
        }
        setNotificationDescriptor(characteristic);
    }


    public final void readCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        BluetoothGatt bluetoothGatt;
        if (ClovaDeskConnector.this.bluetoothAdapter == null || (bluetoothGatt = ClovaDeskConnector.this.bluetoothGatt) == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }


    public final void generateGattPublicKey() {
        byte[] bArr = new byte[32];
        Utils utils = Utils.INSTANCE;
        Context context = ClovaDeskConnector.this.context;
        if (context == null) {
            //t.x("context");
            context = null;
        }
        String uuid = utils.getUUID(context);
        Charset charset = StandardCharsets.UTF_8;
        byte[] bytes = uuid.getBytes(charset);
        KeyManager.ecdhGenPublicKey(bytes, bArr);
        StringBuilder sb2 = new StringBuilder(32);
        int i10 = 0;
        while (i10 < 32) {
            byte b10 = bArr[i10];
            i10++;
            String format = String.format("%02X ", Arrays.copyOf(new Object[]{Byte.valueOf(b10)}, 1));
            sb2.append(format);
        }
        Log.i("genPublicKey : ", String.valueOf(sb2));
        sendPublicKey(bArr);
    }


    public final boolean setNotificationDescriptor(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (bluetoothGattCharacteristic == null) {
            return false;
        }
        BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(clovaDeskSET_NOTIFICATION_DESCRIPTOR));
        Log.i(this.getClass().getSimpleName(),"descriptor: " + descriptor);
        if (descriptor == null) {
            ClovaDeskConnector.this.needReconnect = true;
            Log.e("setNotificationDescriptor", "needReconnect is true");
            return false;
        }
        ClovaDeskConnector.this.needReconnect = false;
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        BluetoothGatt bluetoothGatt = ClovaDeskConnector.this.bluetoothGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.writeDescriptor(descriptor);
        }
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e10) {
            e10.printStackTrace();
        }
        return true;
    }

    private final void sendPublicKey(byte[] bArr) {
        String string = clovaDesk_SET_PUBLIC_KEY;
        BluetoothGattCharacteristic gattCharacteristic = getGattCharacteristic(string);
        if (gattCharacteristic != null) {
            String uuid = gattCharacteristic.getUuid().toString();
            Log.i(this.getClass().getSimpleName(), "sendPublicKey UUID: " + uuid);
            if (!string.equals(uuid)) {
                setNotificationDescriptor(gattCharacteristic);
            }
            setGattSendData(bArr, gattCharacteristic);
            sendGattData(gattCharacteristic);
        }
    }

    public final void setGattSendData(byte[] sendData, BluetoothGattCharacteristic writeCharacteristic) {
        Log.i(this.getClass().getSimpleName(),sendData + " : sendData");
        Log.i(this.getClass().getSimpleName(),writeCharacteristic + " : writeCharacteristic");
        try {
            if (!writeCharacteristic.getUuid().toString().equals(clovaDesk_SET_PUBLIC_KEY)) {
                if (!(sendData.length == 0)) {
                    AESCipher aESCipher = AESCipher.INSTANCE;
                    byte[] bArr = ClovaDeskConnector.this.aesSecret;
                    byte[] bArr2 = null;
                    if (bArr == null) {
                        Log.d("setGattSendData(encrypted?)", "aesSecret");
                        bArr = null;
                    }
                    byte[] bArr3 = ClovaDeskConnector.this.aesIV;
                    if (bArr3 == null) {
                        Log.d("setGattSendData(encrypted?)", "aesIV");
                    } else {
                        bArr2 = bArr3;
                    }
                    sendData = aESCipher.encodeAES(bArr, bArr2, sendData);
                }
            }
            ClovaDeskConnector.this.gattSendDataBuffer = sendData;
        } catch (Exception e10) {
            e10.printStackTrace();
        }
    }

    public final void processEncryptionKey(byte[] publicKey) throws   NoSuchAlgorithmException {
        byte[] bArr = new byte[32];
        KeyManager.ecdhSharedSecret(publicKey, bArr);
        Utils utils = Utils.INSTANCE;
        byte[] hexStringToByteArray = utils.hexStringToByteArray(KeyManager.dummyKey());
        byte[] bArr2 = new byte[hexStringToByteArray.length + 32];
        System.arraycopy(hexStringToByteArray, 0, bArr2, 0, hexStringToByteArray.length);
        System.arraycopy(bArr, 0, bArr2, hexStringToByteArray.length, 32);
        byte[] encryptSHA512 = utils.encryptSHA512(bArr2);
        byte[] aESKey = utils.getAESKey(encryptSHA512);
        ClovaDeskConnector.this.aesSecret = aESKey;
        byte[] bArr3 = null;
        if (aESKey == null) {
            //t.x("aesSecret");
            aESKey = null;
        }
        //t.o("aesSecret, size : ", Integer.valueOf(aESKey.length));
        byte[] bArr4 = ClovaDeskConnector.this.aesSecret;
        if (bArr4 == null) {
            //t.x("aesSecret");
            bArr4 = null;
        }
        //t.o("aesSecret : ", utils.byteArrayToHexForLog(bArr4));
        byte[] aesiv = utils.getAESIV(encryptSHA512);
        ClovaDeskConnector.this.aesIV = aesiv;
        if (aesiv == null) {
            //t.x("aesIV");
            aesiv = null;
        }
        //t.o("aesIV, size : ", Integer.valueOf(aesiv.length));
        byte[] bArr5 = ClovaDeskConnector.this.aesIV;
        if (bArr5 == null) {
            //t.x("aesIV");
        } else {
            bArr3 = bArr5;
        }
        //t.o("aesIV : ", utils.byteArrayToHexForLog(bArr3));
    }


    private Boolean writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z10) {
        BluetoothGatt bluetoothGatt;
        if (ClovaDeskConnector.this.bluetoothAdapter == null || (bluetoothGatt = ClovaDeskConnector.this.bluetoothGatt) == null) {
            ClovaDeskConnector.this.writeEnable = false;
            return false;
        }
        ClovaDeskConnector.this.writeEnable = z10;
        return Boolean.valueOf(bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic));
    }

    public final void sendGattData(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (bluetoothGattCharacteristic == null) {
            return;
        }
        byte[] gattSendData = getGattSendData();
        Log.d(this.getClass().getSimpleName(),"write sendData size : " + Integer.valueOf(gattSendData.length));
        Log.d(this.getClass().getSimpleName(),"setValue : " + Boolean.valueOf(bluetoothGattCharacteristic.setValue(gattSendData)));
        if (writeCharacteristic(bluetoothGattCharacteristic, !(gattSendData.length == 0))) {
            HashMap<String, byte[]> hashMap = ClovaDeskConnector.this.gattResponseBufferQueueMap;
            String uuid = bluetoothGattCharacteristic.getUuid().toString();
            Log.d(this.getClass().getSimpleName(),"writeCharacteristic OK: " + uuid);
            hashMap.put(uuid, null);
            if (bluetoothGattCharacteristic.getUuid().toString().equals(clovaDesk_SET_NETWORK)) {
                //h.b(i0.a(v0.a()), null, null, new GATTConnector$sendGattData$1(this, null), 3, null);
            }
        }
    }

    private final List<BluetoothGattService> getSupportedGattServices() {
        BluetoothGatt bluetoothGatt = ClovaDeskConnector.this.bluetoothGatt;
        if (bluetoothGatt == null) {
            return null;
        }
        return bluetoothGatt.getServices();
    }

    public final BluetoothGattCharacteristic getGattCharacteristic(String characteristicUuid) {
        List<BluetoothGattService> supportedGattServices;
        //Log.i(this.getClass().getSimpleName(),characteristicUuid + "characteristicUuid");
        if (ClovaDeskConnector.this.bluetoothGatt != null && (supportedGattServices = getSupportedGattServices()) != null) {
            for (BluetoothGattService bluetoothGattService : supportedGattServices) {
                if (clovaDesk_serviceID.equals(bluetoothGattService.getUuid().toString())) {
                    List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                    for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
                        if (bluetoothGattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
                            UUID uuid = bluetoothGattCharacteristic.getUuid();
                            //Log.i(this.getClass().getSimpleName(),"gattCharacteristic is " + uuid);
                            return bluetoothGattCharacteristic;
                        }
                    }
                    continue;
                }
            }
        }
        return null;
    }


    private final byte[] getGattSendData() {
        byte[] bArr;
        byte[] bArr2 = new byte[0];
        byte[] bArr3 = ClovaDeskConnector.this.gattSendDataBuffer;
        byte[] bArr4 = null;
        if (bArr3 == null) {
            Log.i("getGattSendData (null)", "bArr3");
            bArr3 = null;
        }
        if (!(bArr3.length == 0)) {
            byte[] bArr5 = ClovaDeskConnector.this.gattSendDataBuffer;
            if (bArr5 == null) {
                Log.i("getGattSendData (null)", "bArr5");
                bArr5 = null;
            }
            int length = bArr5.length;
            int i10 = ClovaDeskConnector.this.dataSplitMaxLength;
            if (length < i10) {
                byte[] bArr6 = ClovaDeskConnector.this.gattSendDataBuffer;
                if (bArr6 == null) {
                    Log.i("getGattSendData (null)", "bArr6");
                    bArr6 = null;
                }
                i10 = bArr6.length;
            }
            bArr2 = new byte[i10];
            byte[] bArr7 = ClovaDeskConnector.this.gattSendDataBuffer;
            if (bArr7 == null) {
                Log.i("getGattSendData (null)", "bArr7");
                bArr7 = null;
            }
            System.arraycopy(bArr7, 0, bArr2, 0, i10);
            byte[] bArr8 = ClovaDeskConnector.this.gattSendDataBuffer;
            if (bArr8 == null) {
                Log.i("getGattSendData (null)", "bArr8");
                bArr8 = null;
            }
            int length2 = bArr8.length - i10;
            if (length2 <= 0) {
                bArr = new byte[0];
            } else {
                byte[] bArr9 = new byte[length2];
                byte[] bArr10 = ClovaDeskConnector.this.gattSendDataBuffer;
                if (bArr10 == null) {
                    Log.i("getGattSendData (null)", "bArr10");
                } else {
                    bArr4 = bArr10;
                }
                System.arraycopy(bArr4, i10, bArr9, 0, length2);
                bArr = bArr9;
            }
            ClovaDeskConnector.this.gattSendDataBuffer = bArr;
        }
        Log.i(this.getClass().getSimpleName(), "retVal : " + new String(bArr2, StandardCharsets.UTF_8));
        return bArr2;
    }
}
