package com.iemike3.clovadeskapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.iemike3.clovadesk.ClovaDeskConnector;

public class MainActivity extends AppCompatActivity {

    public static ClovaDeskConnector clovaDeskConnector;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;

    private boolean icClovaDeskConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // BLEがサポートされてるかチェック
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
            Toast.makeText(this, "BLEがサポートされていません。", Toast.LENGTH_SHORT).show();
        }

        // Bluetoothが有効化されていなければ有効化するようにIntent
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 20);
        }

        clovaDeskConnector = new ClovaDeskConnector(MainActivity.this);

        BluetoothLeScanner bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanCallback scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        /* Permission Error */
                        return;
                    }
                    if (result.getDevice().getName() != null) {
                        if (result.getDevice().getName().startsWith("CLOVA-DESK-")) {
                            bleScanner.stopScan(this);
                            Toast.makeText(MainActivity.this, "Clova Deskを発見", Toast.LENGTH_SHORT).show();
                            clovaDeskConnector.setupConnection(result.getDevice(), bluetoothManager, () -> {
                                Log.i(this.getClass().getSimpleName(), "setupConnection Success!");
                                findViewById(R.id.disconnect_clovadesk).setEnabled(true);
                            });
                        }
                    }
                }
            }
        };

        findViewById(R.id.disconnect_clovadesk).setEnabled(false);

        //buildScanFilters(), buildScanSettings(),
        findViewById(R.id.connect_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 21);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
                    return;
                }

                findViewById(R.id.connect_clovadesk).setEnabled(false);

                Toast.makeText(MainActivity.this, "Clova Deskを検索中", Toast.LENGTH_SHORT).show();
                bleScanner.startScan(scanCallBack);
            }
        });
        findViewById(R.id.disconnect_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.disconnect();
                findViewById(R.id.connect_clovadesk).setEnabled(true);
                findViewById(R.id.disconnect_clovadesk).setEnabled(false);
                Toast.makeText(MainActivity.this, "切断しました", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.check_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.checkMyDevice();
            }
        });

        findViewById(R.id.firmware_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.getDetails();
            }
        });

        findViewById(R.id.wifilist_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.getWifiList();
            }
        });

        findViewById(R.id.setauth_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.setAuth();
            }
        });

        findViewById(R.id.protocolver_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.getProtocolVersion();
            }
        });

        findViewById(R.id.prepareinfo_clovadesk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clovaDeskConnector.getPrepareInfo();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode != RESULT_OK) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            }
        }
    }

}

