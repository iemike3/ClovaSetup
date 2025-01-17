package com.iemike3.clovadeskapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.iemike3.clovaconnector.ClovaSPPCallback;
import com.iemike3.clovaconnector.ClovaSPPConnector;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    public static ClovaSPPConnector clovaSPPConnector;;

    private BluetoothAdapter bluetoothAdapter;

    public int checkSelfPermission(final String permission) {
        return ((getPackageManager().checkPermission(permission, getPackageName()) == PackageManager.PERMISSION_GRANTED) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
    }

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

        // Bluetoothが有効化されていなければ有効化するようにIntent
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 20);
        }

        //buildScanFilters(), buildScanSettings(),
        findViewById(R.id.connect_clova).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 21);
                    return;
                }

                if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
                    return;
                }

                findViewById(R.id.connect_clova).setEnabled(false);

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                registerReceiver(new ClovaSearchBroadcastReceiver(), intentFilter);

                if (bluetoothAdapter.startDiscovery()) {
                    Toast.makeText(MainActivity.this, "Clovaを検索中", Toast.LENGTH_SHORT).show();
                } else {
                    findViewById(R.id.connect_clova).setEnabled(true);
                    Toast.makeText(MainActivity.this, "Clovaの検索に失敗しました。", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.disconnect_clova).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.connect_clova).setEnabled(true);
                findViewById(R.id.disconnect_clova).setEnabled(false);
                Toast.makeText(MainActivity.this, "切断しました。", Toast.LENGTH_SHORT).show();
                clovaSPPConnector.close();
            }
        });

        findViewById(R.id.clova_checkdevice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.checkMyDevice();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_firmware).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getDetails();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_wifilist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getWifiList();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_setauth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.setAuth();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_protocolver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getProtocolVersion();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_prepareinfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.getPrepareInfo();
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_enable_btpan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.toggleBTPan(true);
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_disable_btpan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    clovaSPPConnector.toggleBTPan(false);
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.clova_changeserver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clovaSPPConnector != null) {
                    View clova_changeserver = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_clova_changeserver, null);
                    LinearLayout clova_layout_custom_svurl = clova_changeserver.findViewById(R.id.clova_layout_custom_svurl);
                    EditText clova_edittext_svurl = clova_changeserver.findViewById(R.id.clova_edittext_svurl);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    adapter.add("LINE (Production)");
                    adapter.add("NAVER (Production)");
                    adapter.add("Custom");

                    Spinner spinner = (Spinner) clova_changeserver.findViewById(R.id.clova_connectto_server_spinner);
                    spinner.setAdapter(adapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            switch (position) {
                                case 0: // LINE (Production)
                                    clova_layout_custom_svurl.setVisibility(View.GONE);
                                    break;
                                case 1: // NAVER (Production)
                                    clova_layout_custom_svurl.setVisibility(View.GONE);
                                    break;
                                case 2: // Custom
                                    clova_layout_custom_svurl.setVisibility(View.VISIBLE);
                                    break;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                        }
                    });

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setView(clova_changeserver)
                            .setNegativeButton("キャンセル", null)
                            .setPositiveButton("サーバーを変更", null);

                    AlertDialog dialog = builder.show();
                    Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            switch (spinner.getSelectedItemPosition()) {
                                case 0: // LINE (Production)
                                    clovaSPPConnector.sendScheme(Uri.parse("clovadevice://server_setting/0"));
                                    dialog.dismiss();
                                    break;
                                case 1: // NAVER (Production)
                                    clovaSPPConnector.sendScheme(Uri.parse("clovadevice://server_setting/3"));
                                    dialog.dismiss();
                                    break;
                                case 2: // Custom
                                    if (!clova_edittext_svurl.getText().toString().startsWith("https://")) {
                                        Toast.makeText(MainActivity.this, "サーバーURLが無効です。", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    Uri.Builder uriBuilder = Uri.parse("clovadevice://server_setting/10").buildUpon();
                                    uriBuilder.appendQueryParameter("cic", clova_edittext_svurl.getText().toString());
                                    uriBuilder.appendQueryParameter("auth", clova_edittext_svurl.getText().toString());
                                    uriBuilder.appendQueryParameter("client_id", "fake_client_id");
                                    uriBuilder.appendQueryParameter("client_secret_id", "fake_secret_id");
                                    clovaSPPConnector.sendScheme(uriBuilder.build());
                                    dialog.dismiss();
                                    break;
                                default:
                                    return;
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    class ClovaSearchBroadcastReceiver extends BroadcastReceiver {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && (device.getName().startsWith("WAVE-") || device.getName().startsWith("CLOVA-")) && device.getType() != 2) {
                    // CLOVAを発見
                    unregisterReceiver(this);
                    clovaSPPConnector = new ClovaSPPConnector(MainActivity.this, bluetoothAdapter, new ClovaSPPCallback() {
                        @Override
                        public void onConnected(String clovaDeviceName) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.connect_clova).setEnabled(false);
                                    findViewById(R.id.disconnect_clova).setEnabled(true);
                                    Toast.makeText(MainActivity.this, clovaDeviceName + "に接続しました", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onDisconnected() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.connect_clova).setEnabled(true);
                                    findViewById(R.id.disconnect_clova).setEnabled(false);
                                }
                            });
                        }

                        @Override
                        public void onError(int errorType, String errorMessage) {
                            if (errorType == ClovaSPPConnector.ERROR_NOT_CONNECTED) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Clovaが接続されていません", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if (errorType == ClovaSPPConnector.ERROR_FAILED_TO_CONNECT) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.connect_clova).setEnabled(true);
                                        findViewById(R.id.disconnect_clova).setEnabled(false);

                                        new AlertDialog.Builder(MainActivity.this)
                                                .setMessage("Clovaへの接続に失敗しました")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onClovaData(BluetoothDevice clovaDevice, String type, String message) {
                            Log.d("onClovaData (" + type + ")", message);
                            if (type.contains("AMGFRQ")) {
                                try {
                                    JSONObject clova_details_json = new JSONObject(message);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            View clova_details = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_clova_details, null);
                                            ((TextView) clova_details.findViewById(R.id.clova_devicename_value)).setText(clovaDevice.getName());
                                            try {
                                                ((TextView) clova_details.findViewById(R.id.clova_macaddress_value)).setText(clova_details_json.getJSONObject("firmware").getString("wifimac"));
                                                ((TextView) clova_details.findViewById(R.id.clova_serial_value)).setText(clova_details_json.getJSONObject("firmware").getString("serial"));
                                                ((TextView) clova_details.findViewById(R.id.clova_firmwarever_value)).setText(clova_details_json.getJSONObject("firmware").getString("ver"));
                                                ((TextView) clova_details.findViewById(R.id.clova_hasclovatoken_value)).setText(String.valueOf(clova_details_json.getJSONObject("firmware").getBoolean("hasClovaToken")));
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setView(clova_details)
                                                    .setPositiveButton("閉じる", null)
                                                    .show();
                                        }
                                    });
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (type.contains("AMGNRQ")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startActivity(new Intent(MainActivity.this, ClovaWiFiList.class).putExtra("clovaWiFiJson", message));
                                    }
                                });
                            } else if (type.contains("AMPVRQ")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(message)
                                            .setPositiveButton("閉じる", null)
                                            .show();
                                    }
                                });
                            }
                        }

                    });
                    clovaSPPConnector.connect(device);
                }
            }
        }
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

